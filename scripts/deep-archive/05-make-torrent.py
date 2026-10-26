#!/usr/bin/env python3
"""
make-torrent: a step of the EVE Ref Deep Archive workflow. Writes the torrent's README, copies in the signed
checksum file, then makes the v1 .torrent and its file list, and checks both.

DRAFT. Writes $SEED_DIR/<ID>/<ID>-README.md, <ID>.sha256 and <ID>.sha256.minisig (copied in), and
SCRATCH_DIR/<ID>.torrent, SCRATCH_DIR/<ID>.txt.

    05-make-torrent.py

Needs 04-sign-sha256's completion file, and refuses to run twice (lib/workflow.py). Slow: hashes every piece of every
file (hours for a full torrent), with a progress line every PROGRESS_INTERVAL seconds.

First checks every manifest file in $SEED_DIR/<ID>/ still has the manifest's size and modification time
(content_problems; NOTES.md "Known limitations"), so a file changed on disk since 03-load hashed it doesn't go into the
torrent unnoticed. Then runs sha256sum -c on a random sample of SHA256_SAMPLE_SIZE files from the checksum file
03-load wrote (lib/workflow.py's sample_sha256sums): a smoke test, not a full re-read, since 03-load's own
sha256sum -c already checked every byte once and libtorrent's piece hashing below checks it all again. Then:

1. Render lib/README-template.md (ID, its two-digit number, cutoff) into $SEED_DIR/<ID>/<ID>-README.md.
2. Verify SCRATCH_DIR/<ID>.sha256.minisig against SCRATCH_DIR/<ID>.sha256 (03-load, 04-sign-sha256) with minisign
   and the public key in this checkout (lib/workflow.py's verify_signature), then copy both into $SEED_DIR/<ID>/, so
   they end up inside the torrent too: neither is published standalone, so this is the only way to check a file
   against them without a torrent client.
3. Build the torrent from exactly the manifest's files plus the root files (README, .sha256, .sha256.minisig),
   sorted by path, with Python libtorrent 2.x: v1 only, PIECE_SIZE pieces, each file's modification time included
   (the manifest's for a data file, the file's own for a root file), lib/trackers.py's fixed GOOD_TRACKERS (one per
   tier, in order; 00-preflight already checked they answer), the comment (lib/comment-template.txt with the ID's
   sequence number filled in), creator "EVE Ref (libtorrent <version>)", no private flag, no web seed. v1 has no
   piece alignment, so there are no pad files (NOTES.md "Piece alignment").
4. Generate SCRATCH_DIR/<ID>.txt from the torrent's own file list, reloaded from the .torrent just written (not the
   file_storage object that built it), the data files only, without the root folder.
5. Reload the .torrent and the .txt from disk and check them: loads with libtorrent's default limits and within
   MAX_TORRENT_BYTES; piece length PIECE_SIZE; v1 only (no v2 info-hash); name <ID>, not private, no web seeds;
   trackers equal GOOD_TRACKERS, one per tier, in order; the comment; files = the manifest plus the root files, each
   file's size and modification time, not just the total bytes. Prints the v1 info-hash (copy it into
   $SCRATCH_DIR/NOTES.md) and records it, and the libtorrent version, in the completion file.

Writes:
    $SEED_DIR/<ID>/<ID>-README.md, <ID>.sha256, <ID>.sha256.minisig
    SCRATCH_DIR/<ID>.torrent
    SCRATCH_DIR/<ID>.txt
    05-make-torrent.done     completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR

Needs libtorrent 2.x and Docker (minisign).
"""

import json
import os
import shutil
import signal
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from trackers import GOOD_TRACKERS  # noqa: E402
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
    sample_sha256sums,
    section,
    torrent_sequence,
    verify_signature,
)

STEP = "make-torrent"
README_TEMPLATE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib", "README-template.md")
CREATOR_PREFIX = "EVE Ref"
LOG_SAMPLE = 20
PROGRESS_INTERVAL = 30  # seconds between hashing progress lines
SHA256_SAMPLE_SIZE = 1000  # smoke test, not a full check: 03-load already read every byte once


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

    section("Checksum sample")
    sha256_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.sha256")
    sampled = sample_sha256sums(sha256_path, content_dir, SHA256_SAMPLE_SIZE)
    print(f"sha256sum -c against {sampled} of {fmt_count(len(files))} files, picked at random: OK")

    section("README")
    readme_name = f"{torrent_id}-README.md"
    write_readme(os.path.join(content_dir, readme_name), torrent_id, cutoff)
    print(f"Written: {os.path.join(content_dir, readme_name)}")

    section("Checksum file and signature")
    sha256_name, minisig_name = copy_checksum_files(workflow.scratch_dir, content_dir, torrent_id)
    print(f"Copied into {content_dir}: {sha256_name}, {minisig_name}")

    section("Trackers")
    trackers = list(GOOD_TRACKERS)
    for tracker in trackers:
        print(f"  {tracker}")
    print(f"{len(trackers)} trackers (lib/trackers.py's GOOD_TRACKERS; 00-preflight already checked they answer)")

    section("Torrent")
    # A signature from an earlier run is of the old .torrent: remove it, so 06-sign-torrent has to sign this one.
    minisig_path = torrent_path + ".minisig"
    if os.path.exists(minisig_path):
        os.remove(minisig_path)
        print(f"Removed the earlier run's signature, sign again with 06-sign-torrent: {minisig_path}")
    root_names = root_file_names(torrent_id)
    root_mtimes = {name: int(os.path.getmtime(os.path.join(content_dir, name))) for name in root_names}
    expected_mtimes = {**mtimes, **root_mtimes}
    comment = render_comment(torrent_id)
    print(comment)
    creator = f"{CREATOR_PREFIX} (libtorrent {lt.__version__})"

    # fs (the file_storage) is unused from here on, but must stay referenced until generate(): create_torrent only
    # keeps a reference to it, not a copy (build_torrent's docstring).
    ct, fs, piece_size, num_pieces, total_bytes = build_torrent(
        lt, content_dir, torrent_id, files, root_names, expected_mtimes, trackers, comment, creator
    )
    print(f"Hashing {fmt_count(num_pieces)} pieces ({fmt_bytes(total_bytes)})")
    lt.set_piece_hashes(ct, workflow.seed_dir, hashing_progress(num_pieces))
    data = lt.bencode(ct.generate())
    del fs
    tmp = torrent_path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.rename(tmp, torrent_path)
    print(f"Written: {torrent_path} ({fmt_bytes(len(data))})")

    section("Reloading")
    # Everything from here on reads back the .torrent just written, rather than the file_storage object that built
    # it: that's what a downloader sees, and the only copy that's safe to rely on if libtorrent ever changes what it
    # does to file_storage internally (e.g. reordering files; NOTES.md "libtorrent" on 2.1's changes).
    ti = load_torrent(lt, torrent_path)

    section("File list")
    root_entries = {f"{torrent_id}/{name}" for name in root_names}
    storage = ti.files()
    paths = []
    for i in range(storage.num_files()):
        path = storage.file_path(i).replace(os.sep, "/")
        if path not in root_entries:
            paths.append(path[len(torrent_id) + 1 :])
    paths.sort()
    problems = file_list_problems("".join(p + "\n" for p in paths))
    if problems:
        fail(f"Generated file list is malformed: {problems[0]}")
    tmp = txt_path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.writelines(p + "\n" for p in paths)
    os.rename(tmp, txt_path)
    print(f"Written: {txt_path} ({fmt_count(len(paths))} data files)")

    section("Check")
    v1_hash = check_torrent(
        ti, data, torrent_path, txt_path, torrent_id, files, content_dir, root_names, trackers, comment,
        piece_size, expected_mtimes,
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
            "v2_info_hash": None,
            "trackers": trackers,
            "creator": creator,
            "libtorrent_version": lt.__version__,
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    print(f"Copy into $SCRATCH_DIR/NOTES.md: v1 {v1_hash}")
    ok(f"Make torrent OK for {torrent_id}")


def hashing_progress(num_pieces):
    """
    A set_piece_hashes callback, which libtorrent calls once per piece hashed (not in piece order): prints the count
    every PROGRESS_INTERVAL seconds, and once all are done.
    """
    started = time.monotonic()
    state = {"done": 0, "printed": started}

    def progress(piece):
        state["done"] += 1
        now = time.monotonic()
        if now - state["printed"] >= PROGRESS_INTERVAL or state["done"] == num_pieces:
            state["printed"] = now
            print(
                f"{fmt_count(state['done'])}/{fmt_count(num_pieces)} pieces, {fmt_duration(now - started)} elapsed",
                flush=True,
            )

    return progress


def build_torrent(lt, content_dir, torrent_id, files, root_names, expected_mtimes, trackers, comment, creator):
    """
    The create_torrent, set up and ready for set_piece_hashes: every manifest file plus the root files, sorted by
    path, each with its modification time (expected_mtimes, keyed by path without the torrent's root folder); v1
    only, PIECE_SIZE pieces, trackers one per tier, comment, creator, no private flag. Returns
    (create_torrent, file_storage, piece_size, num_pieces, total_bytes).

    create_torrent only keeps a reference to the file_storage, not a copy (libtorrent's docs: "The file_storage and
    torrent_info objects must stay alive for the entire duration of the create_torrent object"), so the caller must
    keep the returned file_storage referenced for as long as it uses the create_torrent -- through set_piece_hashes
    and generate() -- or the create_torrent is left holding a dangling reference.
    """
    fs = lt.file_storage()
    for path in sorted(files) + sorted(root_names):
        full = os.path.join(content_dir, path)
        if not os.path.isfile(full):
            fail(f"Missing file for the torrent: {full}")
        fs.add_file(f"{torrent_id}/{path}", os.path.getsize(full), 0, expected_mtimes[path])
    ct = lt.create_torrent(fs, PIECE_SIZE, lt.create_torrent.v1_only | lt.create_torrent.modification_time)
    for tier, tracker in enumerate(trackers):
        ct.add_tracker(tracker, tier)
    ct.set_comment(comment)
    ct.set_creator(creator)
    total_bytes = sum(files.values()) + sum(
        os.path.getsize(os.path.join(content_dir, name)) for name in root_names
    )
    return ct, fs, ct.piece_length(), ct.num_pieces(), total_bytes


def load_torrent(lt, torrent_path):
    """Loads the .torrent with libtorrent's default limits, failing within MAX_TORRENT_BYTES check_torrent also
    makes. Separate from check_torrent so the file list can be built from the same loaded torrent."""
    size = os.path.getsize(torrent_path)
    if size > MAX_TORRENT_BYTES:
        fail(f".torrent is {fmt_count(size)} bytes, over MAX_TORRENT_BYTES ({fmt_count(MAX_TORRENT_BYTES)})")
    try:
        ti = lt.torrent_info(torrent_path)
    except RuntimeError as e:
        fail(f"{torrent_path} doesn't load with libtorrent's default limits: {e}")
    print(f"{torrent_path}: {fmt_bytes(size)}, loads with libtorrent's default limits")
    return ti


def check_torrent(
    ti, data, torrent_path, txt_path, torrent_id, files, content_dir, root_names, trackers, comment, piece_size,
    expected_mtimes,
):
    """
    Checks the reloaded .torrent and the .txt against the manifest, the trackers, the comment and the piece size.
    Fails listing every problem; returns the v1 info-hash.
    """
    problems = []

    if ti.name() != torrent_id:
        problems.append(f"name is {ti.name()!r}, expected {torrent_id!r}")
    if ti.priv():
        problems.append("torrent is private")
    if list(ti.web_seeds()):
        problems.append(f"has web seeds: {ti.web_seeds()}")
    if ti.piece_length() != piece_size:
        problems.append(f"piece length is {ti.piece_length()}, expected {piece_size}")
    hashes = ti.info_hashes()
    v1_hash = str(hashes.v1) if hashes.has_v1() else None
    if not v1_hash:
        problems.append("no v1 info-hash")
    if hashes.has_v2():
        problems.append(f"has a v2 info-hash ({hashes.v2}): should be v1 only")
    print(f"v1 info-hash: {v1_hash}")

    actual = sorted(ti.trackers(), key=lambda t: t.tier)
    if [t.url for t in actual] != trackers:
        problems.append(f"trackers don't match GOOD_TRACKERS: {[t.url for t in actual]} != {trackers}")
    if [t.tier for t in actual] != list(range(len(trackers))):
        problems.append(f"trackers aren't one per tier in order: tiers {[t.tier for t in actual]}")
    if ti.comment() != comment:
        problems.append(f"comment doesn't match comment-template.txt rendered with the sequence number:\n{ti.comment()}")

    prefix = torrent_id + "/"
    storage = ti.files()
    data_paths = set()
    torrent_bytes = 0
    size_mismatches = []
    expected_file_bytes = {**files, **{name: os.path.getsize(os.path.join(content_dir, name)) for name in root_names}}
    for i in range(storage.num_files()):
        path = storage.file_path(i).replace(os.sep, "/")
        if not path.startswith(prefix):
            problems.append(f"file outside the torrent's root folder: {path}")
            continue
        rel = path[len(prefix) :]
        torrent_bytes += storage.file_size(i)
        expected_size = expected_file_bytes.get(rel)
        if expected_size is not None and storage.file_size(i) != expected_size:
            size_mismatches.append(f"{rel}: {storage.file_size(i)} bytes in the torrent, expected {expected_size}")
        if rel not in root_names:
            data_paths.add(rel)
    if size_mismatches:
        problems.append(f"files with the wrong size: {len(size_mismatches)}, e.g. {size_mismatches[:LOG_SAMPLE]}")
    mtime_mismatches = file_mtime_problems(data, expected_mtimes)
    if mtime_mismatches:
        problems.append(
            f"files with the wrong modification time: {len(mtime_mismatches)}, e.g. {mtime_mismatches[:LOG_SAMPLE]}"
        )
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
    print("v1 only, piece length, not private, no web seeds, trackers one per tier, comment, files, sizes and "
          "modification times all as expected")
    return v1_hash


def file_mtime_problems(data, expected_mtimes):
    """
    "<path>: mtime <got>, expected <want>" for every file in the bencoded torrent bytes whose "mtime" differs from
    expected_mtimes (keyed by path without the torrent's root folder), or is missing. Reads the raw bencoded
    dictionary, not libtorrent's typed API, which doesn't expose a file's mtime once loaded.
    """
    import libtorrent as lt

    top = lt.bdecode(data)
    info = top[b"info"]
    problems = []
    for file_e in info.get(b"files", []):
        path = "/".join(part.decode("utf-8") for part in file_e[b"path"])
        want = expected_mtimes.get(path)
        if want is None:
            continue
        got = file_e.get(b"mtime")
        if got != want:
            problems.append(f"{path}: mtime {got}, expected {want}")
    return problems


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
    records: a cheap check, before hashing, that nothing changed on disk since 03-load hashed it. Doesn't read the data.
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


if __name__ == "__main__":
    main()
