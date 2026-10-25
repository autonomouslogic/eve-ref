#!/usr/bin/env python3
"""
make-torrent: a step of the EVE Ref Deep Archive workflow. Writes the torrent's README, copies in the signed
checksum file, then makes the hybrid v1+v2 .torrent and its file list, and checks both.

DRAFT. Writes $SEED_DIR/<ID>/<ID>-README.md, <ID>.sha256 and <ID>.sha256.minisig (copied in), and
SCRATCH_DIR/<ID>.torrent, SCRATCH_DIR/<ID>.txt.

    05-make-torrent.py

Needs 04-sign-sha256's completion file, and refuses to run twice (lib/workflow.py). Slow: hashes every piece of every
file (hours for a full torrent).

First checks every manifest file in $SEED_DIR/<ID>/ still has the manifest's size and modification time
(content_problems; PLAN Review notes, R7), so a file changed on disk since 03-load hashed it doesn't go into the
torrent unnoticed. Then:

1. Render lib/README-template.md (ID, its two-digit number, cutoff) into $SEED_DIR/<ID>/<ID>-README.md.
2. Verify SCRATCH_DIR/<ID>.sha256.minisig against SCRATCH_DIR/<ID>.sha256 (03-load, 04-sign-sha256) with minisign
   and the public key in this checkout (lib/workflow.py's verify_signature), then copy both into $SEED_DIR/<ID>/, so
   they end up inside the torrent too: neither is published standalone, so this is the only way to check a file
   against them without a torrent client.
3. Download the tracker lists (lib/trackers.py): the union of both "best" lists, UDP and HTTP(S) only. Fails if a
   list can't be downloaded or has no usable trackers, or if the union has fewer than MIN_TRACKERS.
4. Remove a SCRATCH_DIR/<ID>.torrent.minisig left by an earlier run (it signs the old .torrent, so 06-sign-torrent
   has to sign the new one), then build the torrent from exactly the manifest's files plus the root files (README,
   .sha256, .sha256.minisig), sorted by path, with Python libtorrent 2.x: hybrid v1+v2, PIECE_SIZE pieces, the
   downloaded trackers (one per tier, in order), the PLAN 2.4 comment (lib/comment-template.txt with the ID's
   sequence number filled in), creator "EVE Ref", no private flag, no web seed.
5. Generate SCRATCH_DIR/<ID>.txt from the torrent's own file list (the data files only, without the root folder).
6. Reload the .torrent and the .txt from disk and check them: loads with libtorrent's default limits and within
   MAX_TORRENT_BYTES; hybrid (v1 and v2 info-hashes), name <ID>, not private, no web seeds; trackers equal the
   downloaded ones, one per tier, in order; the comment; files = the manifest plus the root files, bytes match; the
   .txt passes file_list_problems, matches the manifest and has no TORRENT_EXCLUDE paths (and, with TORRENT_INCLUDE
   set, none outside it). Prints the v1 and v2 info-hashes (copy them into NOTES.md) and records them in the
   completion file.

Writes:
    $SEED_DIR/<ID>/<ID>-README.md, <ID>.sha256, <ID>.sha256.minisig
    SCRATCH_DIR/<ID>.torrent
    SCRATCH_DIR/<ID>.txt
    SCRATCH_DIR/trackers/     each tracker list as downloaded (<name>.txt), and trackers.txt: the torrent's trackers
    05-make-torrent.done     completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR

Needs libtorrent 2.x, Docker (minisign), and HTTPS access to raw.githubusercontent.com for the tracker lists.
"""

import json
import os
import shutil
import signal
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MAX_TORRENT_BYTES,
    PIECE_SIZE,
    TORRENT_EXCLUDE,
    TORRENT_INCLUDE,
    Workflow,
    error,
    fail,
    file_list_problems,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    in_folders,
    iso,
    ok,
    parse_iso,
    render_comment,
    root_file_names,
    section,
    torrent_sequence,
    verify_signature,
)
from trackers import TRACKER_SOURCES, TrackersError, combine, fetch_tracker_lists  # noqa: E402

STEP = "make-torrent"
README_TEMPLATE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib", "README-template.md")
CREATOR = "EVE Ref"
LOG_SAMPLE = 20


def main():
    # libtorrent's long calls (set_piece_hashes, generate) run in C++ and never return to Python to raise
    # KeyboardInterrupt, so let Ctrl+C kill the process directly.
    signal.signal(signal.SIGINT, signal.SIG_DFL)
    import libtorrent as lt

    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    cutoff = workflow.read_done("find")["summary"]["cutoff"]
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")
    txt_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.txt")

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    files, mtimes = read_manifest(manifest_path, torrent_id)
    print(f"{fmt_count(len(files))} files in {manifest_path}")

    section("Content folder")
    problems = content_problems(content_dir, files, mtimes)
    if problems:
        for problem in problems[:LOG_SAMPLE]:
            error(f"  {problem}")
        fail(f"{fmt_count(len(problems))} files in {content_dir} changed since 03-load hashed them: investigate")
    print(f"All {fmt_count(len(files))} files in {content_dir} have the manifest's size and modification time")

    section("README")
    readme_name = f"{torrent_id}-README.md"
    write_readme(os.path.join(content_dir, readme_name), torrent_id, cutoff)
    print(f"Written: {os.path.join(content_dir, readme_name)}")

    section("Checksum file and signature")
    sha256_name, minisig_name = copy_checksum_files(workflow.scratch_dir, content_dir, torrent_id)
    print(f"Copied into {content_dir}: {sha256_name}, {minisig_name}")

    section("Trackers")
    trackers = download_trackers(os.path.join(workflow.scratch_dir, "trackers"))

    section("Torrent")
    # A signature from an earlier run is of the old .torrent: remove it, so 06-sign-torrent has to sign this one.
    minisig_path = torrent_path + ".minisig"
    if os.path.exists(minisig_path):
        os.remove(minisig_path)
        print(f"Removed the earlier run's signature, sign again with 06-sign-torrent: {minisig_path}")
    fs = lt.file_storage()
    root_names = root_file_names(torrent_id)
    for path in sorted(files) + sorted(root_names):
        full = os.path.join(content_dir, path)
        if not os.path.isfile(full):
            fail(f"Missing file for the torrent: {full}")
        fs.add_file(f"{torrent_id}/{path}", os.path.getsize(full))
    comment = render_comment(torrent_id)
    print(comment)

    ct = lt.create_torrent(fs, PIECE_SIZE)
    piece_size = ct.piece_length()
    for tier, tracker in enumerate(trackers):
        ct.add_tracker(tracker, tier)
    ct.set_comment(comment)
    ct.set_creator(CREATOR)
    num_pieces = ct.num_pieces()
    total_bytes = sum(files.values()) + sum(
        os.path.getsize(os.path.join(content_dir, name)) for name in root_names
    )
    print(f"Hashing {fmt_count(num_pieces)} pieces ({fmt_bytes(total_bytes)})")
    lt.set_piece_hashes(ct, workflow.seed_dir)
    data = lt.bencode(ct.generate())
    tmp = torrent_path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.rename(tmp, torrent_path)
    print(f"Written: {torrent_path} ({fmt_bytes(len(data))})")

    section("File list")
    root_entries = {f"{torrent_id}/{name}" for name in root_names}
    paths = sorted(
        name[len(torrent_id) + 1 :] for name in storage_paths(fs) if name not in root_entries
    )
    problems = file_list_problems("".join(p + "\n" for p in paths))
    if problems:
        fail(f"Generated file list is malformed: {problems[0]}")
    tmp = txt_path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.writelines(p + "\n" for p in paths)
    os.rename(tmp, txt_path)
    print(f"Written: {txt_path} ({fmt_count(len(paths))} data files)")

    section("Check")
    v1_hash, v2_hash = check_torrent(
        lt, torrent_path, txt_path, torrent_id, files, content_dir, root_names, trackers, comment
    )

    workflow.complete(
        STEP,
        torrent_id,
        {
            "torrent": torrent_path,
            "txt": txt_path,
            "readme": os.path.join(content_dir, readme_name),
            "pieces": num_pieces,
            "piece_size": piece_size,
            "torrent_bytes": len(data),
            "data_files": len(paths),
            "v1_info_hash": v1_hash,
            "v2_info_hash": v2_hash,
            "trackers": len(trackers),
            "tracker_sources": [url for _, url in TRACKER_SOURCES],
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    print(f"Copy into NOTES.md: v1 {v1_hash}, v2 {v2_hash}")
    ok(f"Make torrent OK for {torrent_id}")


def check_torrent(lt, torrent_path, txt_path, torrent_id, files, content_dir, root_names, trackers, comment):
    """
    Reloads the .torrent and the .txt from disk and checks them against the manifest, the trackers and the comment.
    Fails listing every problem; returns (v1 info-hash, v2 info-hash).
    """
    problems = []

    size = os.path.getsize(torrent_path)
    if size > MAX_TORRENT_BYTES:
        problems.append(f".torrent is {fmt_count(size)} bytes, over MAX_TORRENT_BYTES ({fmt_count(MAX_TORRENT_BYTES)})")
    try:
        ti = lt.torrent_info(torrent_path)
    except RuntimeError as e:
        fail(f"{torrent_path} doesn't load with libtorrent's default limits: {e}")
    print(f"{torrent_path}: {fmt_bytes(size)}, loads with libtorrent's default limits")

    if ti.name() != torrent_id:
        problems.append(f"name is {ti.name()!r}, expected {torrent_id!r}")
    if ti.priv():
        problems.append("torrent is private")
    if list(ti.web_seeds()):
        problems.append(f"has web seeds: {ti.web_seeds()}")
    hashes = ti.info_hashes()
    v1_hash = str(hashes.v1) if hashes.has_v1() else None
    v2_hash = str(hashes.v2) if hashes.has_v2() else None
    if not v1_hash or not v2_hash:
        problems.append(f"not hybrid: v1={v1_hash}, v2={v2_hash}")
    print(f"v1 info-hash: {v1_hash}")
    print(f"v2 info-hash: {v2_hash}")

    actual = sorted(ti.trackers(), key=lambda t: t.tier)
    if [t.url for t in actual] != trackers:
        problems.append(f"trackers don't match the downloaded lists: {[t.url for t in actual]} != {trackers}")
    if [t.tier for t in actual] != list(range(len(trackers))):
        problems.append(f"trackers aren't one per tier in order: tiers {[t.tier for t in actual]}")
    if ti.comment() != comment:
        problems.append(f"comment doesn't match comment-template.txt rendered with the sequence number:\n{ti.comment()}")

    prefix = torrent_id + "/"
    storage = ti.files()
    data_paths = set()
    torrent_bytes = 0
    for i in range(storage.num_files()):
        if storage.file_flags(i) & lt.file_storage.flag_pad_file:
            continue
        path = storage.file_path(i).replace(os.sep, "/")
        if not path.startswith(prefix):
            problems.append(f"file outside the torrent's root folder: {path}")
            continue
        torrent_bytes += storage.file_size(i)
        if path[len(prefix) :] not in root_names:
            data_paths.add(path[len(prefix) :])
    if data_paths != set(files):
        extra = sorted(data_paths - set(files))
        missing = sorted(set(files) - data_paths)
        if extra:
            problems.append(f"files in the torrent but not the manifest: {len(extra)}, e.g. {extra[:LOG_SAMPLE]}")
        if missing:
            problems.append(f"manifest files missing from the torrent: {len(missing)}, e.g. {missing[:LOG_SAMPLE]}")
    expected_bytes = sum(files.values()) + sum(
        os.path.getsize(os.path.join(content_dir, name)) for name in root_names
    )
    if torrent_bytes != expected_bytes:
        problems.append(
            f"torrent data bytes {fmt_count(torrent_bytes)}, expected {fmt_count(expected_bytes)} "
            "(manifest + root files)"
        )
    print(f"{fmt_count(len(data_paths))} data files plus {fmt_count(len(root_names))} root files, "
          f"{fmt_bytes(torrent_bytes)}")

    with open(txt_path, encoding="utf-8") as f:
        text = f.read()
    list_problems = file_list_problems(text)
    if list_problems:
        problems.append(f"{txt_path} fails file_list_problems: {list_problems[0]}")
    listed = set(text.splitlines())
    if listed != set(files):
        problems.append(f"{txt_path} doesn't match the manifest's paths")
    excluded = sorted(p for p in listed if in_folders(p, TORRENT_EXCLUDE))
    if excluded:
        problems.append(f"{txt_path} has excluded paths: {excluded[:LOG_SAMPLE]}")
    if TORRENT_INCLUDE is not None:
        not_included = sorted(p for p in listed if not in_folders(p, TORRENT_INCLUDE))
        if not_included:
            problems.append(f"{txt_path} has paths outside TORRENT_INCLUDE: {not_included[:LOG_SAMPLE]}")
    print(f"{txt_path}: {fmt_count(len(listed))} lines")

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the torrent")
    print("Hybrid, not private, no web seeds, trackers one per tier, comment, files and file list all as expected")
    return v1_hash, v2_hash


def storage_paths(fs):
    """The torrent's file paths, without pad files: create_torrent adds .pad/<n> entries to fs for a hybrid torrent."""
    import libtorrent as lt

    return [
        fs.file_path(i).replace(os.sep, "/")
        for i in range(fs.num_files())
        if not fs.file_flags(i) & lt.file_storage.flag_pad_file
    ]


def read_manifest(path, torrent_id):
    """({path: size}, {path: modification time}) for the manifest's files; modification times as POSIX seconds."""
    if not os.path.isfile(path):
        fail(f"Manifest not found: {path}")
    with open(path, encoding="utf-8") as f:
        manifest = json.load(f)
    if manifest.get("torrent_id") != torrent_id:
        fail(f"{path} is for {manifest.get('torrent_id')!r}, not {torrent_id!r}")
    files = manifest.get("files")
    if not files:
        fail(f"No files in {path}")
    return {f["path"]: f["size"] for f in files}, {f["path"]: parse_iso(f["last_modified"]) for f in files}


def content_problems(content_dir, files, mtimes):
    """
    Manifest files missing from the content folder, or with another size or modification time than the manifest
    records: a cheap check, before hashing, that nothing changed on disk since 03-load hashed it (PLAN Review notes,
    R7). Doesn't read the data.
    """
    problems = []
    for path in sorted(files):
        try:
            stat = os.stat(os.path.join(content_dir, path))
        except FileNotFoundError:
            problems.append(f"{path}: missing")
            continue
        if stat.st_size != files[path]:
            problems.append(f"{path}: {fmt_count(stat.st_size)} bytes, manifest {fmt_count(files[path])}")
        elif int(stat.st_mtime) != mtimes[path]:
            problems.append(f"{path}: modified {iso(stat.st_mtime)}, manifest {iso(mtimes[path])}")
    return problems


def write_readme(path, torrent_id, cutoff):
    """lib/README-template.md with {id}, {sequence} (the ID's two-digit number, e.g. 00) and {cutoff} filled in."""
    if not os.path.isfile(README_TEMPLATE):
        fail(f"README template not found: {README_TEMPLATE}")
    with open(README_TEMPLATE, encoding="utf-8") as f:
        template = f.read()
    text = template.format(id=torrent_id, sequence=f"{torrent_sequence(torrent_id):02d}", cutoff=cutoff)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(text)
    os.rename(tmp, path)


def copy_checksum_files(scratch_dir, content_dir, torrent_id):
    """
    Copies SCRATCH_DIR/<ID>.sha256 and its .minisig (03-load, 04-sign-sha256) into the torrent's content folder, so
    they end up inside the torrent as root files. Returns (sha256_name, minisig_name). Fails if either is missing
    (04-sign-sha256 needs to run first), or if the signature doesn't verify against the checksum file.
    """
    sha256_name = f"{torrent_id}.sha256"
    minisig_name = f"{sha256_name}.minisig"
    for name in (sha256_name, minisig_name):
        src = os.path.join(scratch_dir, name)
        if not os.path.isfile(src):
            fail(f"Missing {src}: run 03-load and 04-sign-sha256 first")
    verify_signature(os.path.join(scratch_dir, sha256_name))
    for name in (sha256_name, minisig_name):
        shutil.copyfile(os.path.join(scratch_dir, name), os.path.join(content_dir, name))
    return sha256_name, minisig_name


def download_trackers(trackers_dir):
    """
    Downloads the tracker lists (lib/trackers.py) and returns their union, one tracker per tier in order. Saves each
    list as downloaded, and the union, in trackers_dir, so the torrent's trackers can be reviewed later.
    """
    try:
        lists = fetch_tracker_lists()
        trackers, counts = combine([(name, text) for name, _, text in lists])
    except TrackersError as e:
        fail(str(e))
    os.makedirs(trackers_dir, exist_ok=True)
    for name, url, text in lists:
        path = os.path.join(trackers_dir, f"{name}.txt")
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)
        print(f"{name}: {url}, saved to {path}")
    for name, usable, skipped in counts:
        others = f", {len(skipped)} others skipped: {skipped}" if skipped else ""
        print(f"{name}: {usable} UDP or HTTP(S) trackers{others}")
    path = os.path.join(trackers_dir, "trackers.txt")
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(t + "\n" for t in trackers)
    print(f"{len(trackers)} trackers together, one per tier, in {path}")
    return trackers


if __name__ == "__main__":
    main()
