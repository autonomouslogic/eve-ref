#!/usr/bin/env python3
"""
check-torrent: step 8 of the EVE Ref Deep Archive workflow. Checks the .torrent from scratch: reloads it from
disk and re-derives everything from the manifest, trackers.txt and the .txt file list, never trusting
06-make-torrent's own result.

DRAFT. Read-only.

    07-check-torrent.py

Needs 06-make-torrent's completion file, and refuses to run twice (lib/workflow.py).

- Files = the manifest plus <ID>.sha256 and <ID>-README.txt; bytes match.
- Hybrid (v1 and v2 info-hashes both present), name is <ID>, not private, no web seeds.
- Trackers equal trackers.txt, one per tier, in order.
- The comment equals the PLAN 2.4 template rendered from the manifest.
- <ID>.txt matches the torrent's data files, passes file_list_problems, and has no market-history/, killmails/ or
  ccp/ paths.
- The .torrent loads with libtorrent's default limits (size and token checks; lib/workflow.py's MAX_TORRENT_BYTES).

Prints the v1 and v2 info-hashes: copy them into NOTES.md (README, "06: Make torrent").

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR

Needs libtorrent 2.x.
"""

import json
import os
import re
import sys
import time

import libtorrent as lt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MAX_TORRENT_BYTES,
    TORRENT_EXCLUDE,
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

STEP = "check-torrent"
TRACKERS_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")
TRACKER_SCHEMES = ("udp://", "http://", "https://")
LOG_SAMPLE = 20


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")
    txt_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.txt")

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    manifest = read_manifest(manifest_path, torrent_id)
    sha_name = f"{torrent_id}.sha256"
    readme_name = f"{torrent_id}-README.txt"
    print(f"{fmt_count(len(manifest))} files in {manifest_path}")

    problems = []

    section("Loading")
    if not os.path.isfile(torrent_path):
        fail(f"Torrent not found: {torrent_path}")
    size = os.path.getsize(torrent_path)
    print(f"{torrent_path}: {fmt_bytes(size)}")
    if size > MAX_TORRENT_BYTES:
        problems.append(f".torrent is {fmt_count(size)} bytes, over MAX_TORRENT_BYTES ({fmt_count(MAX_TORRENT_BYTES)})")
    try:
        ti = lt.torrent_info(torrent_path)
    except RuntimeError as e:
        fail(f"Doesn't load with libtorrent's default limits: {e}")
    print("Loads with libtorrent's default limits: yes")

    section("Identity")
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

    section("Trackers")
    expected_trackers = load_trackers(TRACKERS_FILE)
    actual = sorted(ti.trackers(), key=lambda t: t.tier)
    actual_urls = [t.url for t in actual]
    actual_tiers = [t.tier for t in actual]
    if actual_urls != expected_trackers:
        problems.append(f"trackers don't match {TRACKERS_FILE}: {actual_urls} != {expected_trackers}")
    if actual_tiers != list(range(len(expected_trackers))):
        problems.append(f"trackers aren't one per tier in order: tiers {actual_tiers}")
    else:
        print(f"{len(actual_urls)} trackers, one per tier, matching {TRACKERS_FILE}")

    section("Files")
    data_paths, data_bytes = torrent_files(ti, torrent_id, {sha_name, readme_name})
    manifest_total = sum(manifest.values())
    if data_paths != set(manifest):
        extra = sorted(data_paths - set(manifest))
        missing = sorted(set(manifest) - data_paths)
        if extra:
            problems.append(f"files in the torrent but not the manifest: {len(extra)}, e.g. {extra[:LOG_SAMPLE]}")
        if missing:
            problems.append(f"manifest files missing from the torrent: {len(missing)}, e.g. {missing[:LOG_SAMPLE]}")
    root_sizes = sum(os.path.getsize(os.path.join(content_dir, name)) for name in (sha_name, readme_name))
    if data_bytes != manifest_total + root_sizes:
        problems.append(
            f"torrent data bytes {fmt_count(data_bytes)}, expected {fmt_count(manifest_total + root_sizes)} "
            "(manifest + root files)"
        )
    print(f"{fmt_count(len(data_paths))} data files ({fmt_bytes(data_bytes)}, including the 2 root files)")

    section("Comment")
    expected_comment = expected_comment_text(torrent_id, manifest)
    if ti.comment() != expected_comment:
        problems.append(f"comment doesn't match the PLAN 2.4 template rendered from the manifest:\n{ti.comment()}")
    else:
        print("Comment matches the PLAN 2.4 template")

    section("File list (<ID>.txt)")
    if not os.path.isfile(txt_path):
        problems.append(f"{txt_path} not found")
    else:
        with open(txt_path, encoding="utf-8") as f:
            text = f.read()
        list_problems = file_list_problems(text)
        if list_problems:
            problems.append(f"{txt_path} fails file_list_problems: {list_problems[0]}")
        listed = set(text.splitlines())
        if listed != set(manifest):
            problems.append(f"{txt_path} doesn't match the manifest's paths")
        excluded_hits = [p for p in listed if any(p == e or p.startswith(e + "/") for e in TORRENT_EXCLUDE)]
        if excluded_hits:
            problems.append(f"{txt_path} has excluded paths: {excluded_hits[:LOG_SAMPLE]}")
        print(f"{fmt_count(len(listed))} lines, well-formed, matching the manifest")

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the torrent")

    workflow.complete(
        STEP,
        torrent_id,
        {
            "v1_info_hash": v1_hash,
            "v2_info_hash": v2_hash,
            "torrent_bytes": size,
            "data_files": len(data_paths),
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    print(f"Copy into NOTES.md: v1 {v1_hash}, v2 {v2_hash}")
    ok(f"Check torrent OK for {torrent_id}")


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


def torrent_files(ti, torrent_id, root_names):
    """(data paths without the torrent-name prefix, total bytes), pad files excluded."""
    storage = ti.files()
    prefix = torrent_id + "/"
    paths = set()
    total = 0
    for i in range(storage.num_files()):
        if storage.file_flags(i) & lt.file_storage.flag_pad_file:
            continue
        path = storage.file_path(i).replace(os.sep, "/")
        if not path.startswith(prefix):
            fail(f"File outside the torrent's root folder: {path}")
        rel = path[len(prefix) :]
        total += storage.file_size(i)
        if rel in root_names:
            continue
        paths.add(rel)
    return paths, total


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
            trackers.append(line)
    if not trackers:
        fail(f"No trackers in {path}")
    return trackers


YEAR_DIR_RE_SOURCE = r"^(19|20)\d{2}$"


def expected_comment_text(torrent_id, manifest):
    year_re = re.compile(YEAR_DIR_RE_SOURCE)
    dataset_suffixes = {"history", "backfills"}
    years = set()
    datasets = set()
    for path in manifest:
        dirs = path.split("/")[:-1]
        if not dirs:
            continue
        year_index = next((i for i, d in enumerate(dirs) if year_re.match(d)), None)
        if year_index is None:
            dataset = dirs[:3]
        else:
            years.add(int(dirs[year_index]))
            dataset = dirs[:year_index]
        while len(dataset) > 1 and dataset[-1] in dataset_suffixes:
            dataset = dataset[:-1]
        if dataset:
            datasets.add("/".join(dataset))
    years_text = "unknown years" if not years else (
        str(min(years)) if min(years) == max(years) else f"{min(years)}–{max(years)}"
    )
    datasets_text = ", ".join(sorted(datasets)) or "none"
    if len(datasets_text) > 200:
        datasets_text = datasets_text[:200].rsplit(", ", 1)[0] + ", ..."
    return (
        f"EVE Ref Deep Archive: {torrent_id}\n"
        f"Data from {years_text}. Datasets: {datasets_text}.\n"
        "What this is, how to use it and how to help seed: https://docs.everef.net/datasets/deep-archive.html\n"
        f"File list: https://data.everef.net/deep-archive/{torrent_id}.txt"
    )


if __name__ == "__main__":
    main()
