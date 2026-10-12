#!/usr/bin/env python3
"""
make-torrent: step 6 of the EVE Ref Deep Archive workflow. Writes the torrent's README, then makes the hybrid
v1+v2 .torrent and its file list, and checks both.

DRAFT. Writes $SEED_DIR/<ID>/<ID>-README.txt and SCRATCH_DIR/<ID>.torrent, SCRATCH_DIR/<ID>.txt.

    05-make-torrent.py

Needs 04-load's completion file, and refuses to run twice (lib/workflow.py). Slow: hashes every piece
of every file (hours for a full torrent).

1. Render README-template.txt (ID, cutoff, datasets) into $SEED_DIR/<ID>/<ID>-README.txt.
2. Build the torrent from exactly the manifest's files plus the root README (<ID>-README.txt), sorted
   by path, with Python libtorrent 2.x: hybrid v1+v2, PIECE_SIZE pieces, trackers from trackers.txt (one
   per tier), the PLAN 2.4 comment (years and datasets from the manifest), creator "EVE Ref", no private flag,
   no web seed.
3. Generate SCRATCH_DIR/<ID>.txt from the torrent's own file list (the data files only, without the root folder).
4. Reload the .torrent and the .txt from disk and check them: loads with libtorrent's default limits and within
   MAX_TORRENT_BYTES; hybrid (v1 and v2 info-hashes), name <ID>, not private, no web seeds; trackers equal
   trackers.txt, one per tier, in order; the comment; files = the manifest plus <ID>-README.txt, bytes match; the .txt
   passes file_list_problems, matches the manifest and has no market-history/, killmails/ or ccp/ paths. Prints the
   v1 and v2 info-hashes (copy them into NOTES.md) and records them in the completion file.

Writes:
    $SEED_DIR/<ID>/<ID>-README.txt
    SCRATCH_DIR/<ID>.torrent
    SCRATCH_DIR/<ID>.txt
    05-make-torrent.done     completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR

Needs libtorrent 2.x.
"""

import json
import os
import re
import signal
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MAX_TORRENT_BYTES,
    PIECE_SIZE,
    TORRENT_EXCLUDE,
    TORRENT_ID_PREFIX,
    Workflow,
    error,
    fail,
    file_list_problems,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "make-torrent"
TRACKERS_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")
README_TEMPLATE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "README-template.txt")
TRACKER_SCHEMES = ("udp://", "http://", "https://")
YEAR_DIR_RE = re.compile(r"^(19|20)\d{2}$")
DATASET_SUFFIXES = {"history", "backfills"}
DATASETS_MAX_LENGTH = 200
CREATOR = "EVE Ref"
LOG_SAMPLE = 20

COMMENT_TEMPLATE = """EVE Ref Deep Archive: {id}
Data from {years}. Datasets: {datasets}.
What this is, how to use it and how to help seed: https://docs.everef.net/datasets/deep-archive.html
File list: https://data.everef.net/deep-archive/{id}.txt"""


def main():
    # libtorrent's long calls (set_piece_hashes, generate) run in C++ and never return to Python to raise
    # KeyboardInterrupt, so let Ctrl+C kill the process directly.
    signal.signal(signal.SIGINT, signal.SIG_DFL)
    import libtorrent as lt

    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")
    txt_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.txt")

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    files = read_manifest(manifest_path, torrent_id)
    print(f"{fmt_count(len(files))} files in {manifest_path}")

    section("README")
    readme_name = f"{torrent_id}-README.txt"
    years, datasets = classify_all(files)
    write_readme(os.path.join(content_dir, readme_name), torrent_id, files, years, datasets)
    print(f"Written: {os.path.join(content_dir, readme_name)}")

    section("Trackers")
    trackers = load_trackers(TRACKERS_FILE)

    section("Torrent")
    fs = lt.file_storage()
    root_names = {readme_name}
    for path in sorted(files) + sorted(root_names):
        full = os.path.join(content_dir, path)
        if not os.path.isfile(full):
            fail(f"Missing file for the torrent: {full}")
        fs.add_file(f"{torrent_id}/{path}", os.path.getsize(full))
    comment = render_comment(torrent_id, years, datasets)
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
    print(f"Hashing {fmt_count(num_pieces)} pieces ({fmt_bytes(total_bytes)}) -- this takes hours for a full torrent")
    lt.set_piece_hashes(ct, workflow.seed_dir)
    data = lt.bencode(ct.generate())
    tmp = torrent_path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.rename(tmp, torrent_path)
    print(f"Written: {torrent_path} ({fmt_bytes(len(data))})")

    section("File list")
    root_entries = {f"{torrent_id}/{readme_name}"}
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
        lt, torrent_path, txt_path, torrent_id, files, content_dir, readme_name, trackers, comment
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
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    print(f"Copy into NOTES.md: v1 {v1_hash}, v2 {v2_hash}")
    ok(f"Make torrent OK for {torrent_id}")


def check_torrent(lt, torrent_path, txt_path, torrent_id, files, content_dir, readme_name, trackers, comment):
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
        problems.append(f"trackers don't match {TRACKERS_FILE}: {[t.url for t in actual]} != {trackers}")
    if [t.tier for t in actual] != list(range(len(trackers))):
        problems.append(f"trackers aren't one per tier in order: tiers {[t.tier for t in actual]}")
    if ti.comment() != comment:
        problems.append(f"comment doesn't match the PLAN 2.4 template rendered from the manifest:\n{ti.comment()}")

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
        if path[len(prefix) :] != readme_name:
            data_paths.add(path[len(prefix) :])
    if data_paths != set(files):
        extra = sorted(data_paths - set(files))
        missing = sorted(set(files) - data_paths)
        if extra:
            problems.append(f"files in the torrent but not the manifest: {len(extra)}, e.g. {extra[:LOG_SAMPLE]}")
        if missing:
            problems.append(f"manifest files missing from the torrent: {len(missing)}, e.g. {missing[:LOG_SAMPLE]}")
    expected_bytes = sum(files.values()) + os.path.getsize(os.path.join(content_dir, readme_name))
    if torrent_bytes != expected_bytes:
        problems.append(
            f"torrent data bytes {fmt_count(torrent_bytes)}, expected {fmt_count(expected_bytes)} (manifest + README)"
        )
    print(f"{fmt_count(len(data_paths))} data files plus the README, {fmt_bytes(torrent_bytes)}")

    with open(txt_path, encoding="utf-8") as f:
        text = f.read()
    list_problems = file_list_problems(text)
    if list_problems:
        problems.append(f"{txt_path} fails file_list_problems: {list_problems[0]}")
    listed = set(text.splitlines())
    if listed != set(files):
        problems.append(f"{txt_path} doesn't match the manifest's paths")
    excluded = [p for p in listed if any(p == e or p.startswith(e + "/") for e in TORRENT_EXCLUDE)]
    if excluded:
        problems.append(f"{txt_path} has excluded paths: {excluded[:LOG_SAMPLE]}")
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
    if not os.path.isfile(path):
        fail(f"Manifest not found: {path}")
    with open(path, encoding="utf-8") as f:
        manifest = json.load(f)
    if manifest.get("torrent_id") != torrent_id:
        fail(f"{path} is for {manifest.get('torrent_id')!r}, not {torrent_id!r}")
    files = manifest.get("files")
    if not files:
        fail(f"No files in {path}")
    return {f["path"]: f["size"] for f in files}


def classify_all(files):
    """Distinct years and dataset folders across the manifest paths (PLAN 2.4's comment)."""
    years = set()
    datasets = set()
    for path in files:
        dirs = path.split("/")[:-1]
        if not dirs:
            continue
        year_index = next((i for i, d in enumerate(dirs) if YEAR_DIR_RE.match(d)), None)
        if year_index is None:
            dataset = dirs[:3]
        else:
            years.add(int(dirs[year_index]))
            dataset = dirs[:year_index]
        while len(dataset) > 1 and dataset[-1] in DATASET_SUFFIXES:
            dataset = dataset[:-1]
        if dataset:
            datasets.add("/".join(dataset))
    return years, datasets


def render_comment(torrent_id, years, datasets):
    if not years:
        years_text = "unknown years"
    elif min(years) == max(years):
        years_text = str(min(years))
    else:
        years_text = f"{min(years)}–{max(years)}"
    datasets_text = ", ".join(sorted(datasets)) or "none"
    if len(datasets_text) > DATASETS_MAX_LENGTH:
        datasets_text = datasets_text[:DATASETS_MAX_LENGTH].rsplit(", ", 1)[0] + ", ..."
    return COMMENT_TEMPLATE.format(id=torrent_id, years=years_text, datasets=datasets_text)


def write_readme(path, torrent_id, files, years, datasets):
    if not os.path.isfile(README_TEMPLATE):
        fail(f"README template not found: {README_TEMPLATE}")
    with open(README_TEMPLATE, encoding="utf-8") as f:
        template = f.read()
    cutoff = torrent_id[len(TORRENT_ID_PREFIX) :]
    text = template.format(id=torrent_id, cutoff=cutoff, datasets=", ".join(sorted(datasets)) or "none")
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(text)
    os.rename(tmp, path)


def load_trackers(path):
    path = os.path.realpath(path)
    if not os.path.isfile(path):
        fail(f"Tracker list not found: {path}")
    trackers = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if not line.startswith(TRACKER_SCHEMES):
                fail(f"Not a UDP or HTTP(S) tracker in {path}: {line}")
            if line in trackers:
                fail(f"Duplicate tracker in {path}: {line}")
            trackers.append(line)
    if not trackers:
        fail(f"No trackers in {path}")
    print(f"{len(trackers)} trackers from {path}")
    return trackers


if __name__ == "__main__":
    main()
