#!/usr/bin/env python3
"""
test-download: step 11 of the EVE Ref Deep Archive workflow. Downloads a sample of the torrent through its
magnet link, in a throwaway libtorrent session separate from the seeding one in qBittorrent, and verifies it.

DRAFT. Writes only a throwaway folder under SCRATCH_DIR, deleted at the end.

    10-test-download.py

Needs 09-check-seed's completion file, and refuses to run twice (lib/workflow.py).

Open question (PLAN 6, #6): where this runs decides what it actually proves. Run it from another machine, ideally
on another network, for a real test of reachability; run from the seeder host itself, it only proves the swarm
and the local qBittorrent agree.

1. Builds a magnet link from the torrent's v1/v2 info-hashes (07-check-torrent) and trackers.txt. Only the magnet
   link: no local .torrent file is given to the session, so metadata must come from peers.
2. Starts a libtorrent session in $SCRATCH_DIR/test-download/ (not $SEED_DIR), waits for metadata, then downloads
   just <ID>.sha256, <ID>-README.txt and a sample of data files (every Nth path, capped at SAMPLE_SIZE).
3. Verifies every downloaded file's SHA-256 against the manifest (data files) or the seed directory's own copy
   (the two root files).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR

Needs libtorrent 2.x.
"""

import hashlib
import json
import os
import shutil
import sys
import time

import libtorrent as lt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    Workflow,
    error,
    fail,
    fmt_duration,
    ok,
    section,
)

STEP = "test-download"
TRACKERS_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")
SAMPLE_SIZE = 20
METADATA_TIMEOUT = 120  # seconds
DOWNLOAD_TIMEOUT = 600  # seconds
POLL_INTERVAL = 2


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    check_torrent = workflow.read_done("check-torrent")["summary"]
    v1_hash, v2_hash = check_torrent["v1_info_hash"], check_torrent["v2_info_hash"]
    sha_name, readme_name = f"{torrent_id}.sha256", f"{torrent_id}-README.txt"

    section("Manifest")
    manifest = read_manifest(os.path.join(workflow.scratch_dir, "manifest.json"), torrent_id)

    section("Magnet link")
    trackers = load_trackers(TRACKERS_FILE)
    magnet = build_magnet(torrent_id, v1_hash, v2_hash, trackers)
    print(magnet)

    download_dir = os.path.join(workflow.scratch_dir, "test-download")
    if os.path.exists(download_dir):
        shutil.rmtree(download_dir)
    os.makedirs(download_dir)

    section("Session")
    session = lt.session({"listen_interfaces": "0.0.0.0:0"})
    params = lt.parse_magnet_uri(magnet)
    params.save_path = download_dir
    # Every file at priority 0 once metadata arrives, so nothing but the sample starts downloading before
    # set_priorities: a partly downloaded file would show up as a false hash mismatch.
    params.flags |= lt.torrent_flags.default_dont_download
    handle = session.add_torrent(params)
    print("Added by magnet link only (no local .torrent file); waiting for metadata")

    try:
        wait_for_metadata(handle)
        print("Metadata received from peers")

        sample = pick_sample(handle, torrent_id, sha_name, readme_name)
        print(f"Downloading {len(sample)} files: {sha_name}, {readme_name}, and {len(sample) - 2} data files")
        set_priorities(handle, sample)
        wait_for_download(handle, sample)

        section("Verifying")
        sample_paths = sample_rel_paths(handle, torrent_id, sample)
        problems = verify(download_dir, torrent_id, manifest, content_dir, sha_name, readme_name, sample_paths)
    finally:
        session.remove_torrent(handle)
        shutil.rmtree(download_dir, ignore_errors=True)

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} hash mismatches in the test download")
    print(f"{len(sample)} files downloaded through the magnet link, 0 hash mismatches")

    workflow.complete(STEP, torrent_id, {"sampled_files": len(sample)})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Test download OK for {torrent_id}")


def read_manifest(path, torrent_id):
    if not os.path.isfile(path):
        fail(f"Manifest not found: {path}")
    with open(path, encoding="utf-8") as f:
        manifest = json.load(f)
    if manifest.get("torrent_id") != torrent_id:
        fail(f"{path} is for {manifest.get('torrent_id')!r}, not {torrent_id!r}")
    return {f["path"]: f["sha256"] for f in manifest["files"]}


def load_trackers(path):
    if not os.path.isfile(path):
        fail(f"Tracker list not found: {path}")
    with open(path, encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip() and not line.strip().startswith("#")]


def build_magnet(torrent_id, v1_hash, v2_hash, trackers):
    params = [f"xt=urn:btih:{v1_hash}"]
    if v2_hash:
        params.append(f"xt=urn:btmh:1220{v2_hash}")
    params.append(f"dn={torrent_id}")
    params += [f"tr={t}" for t in trackers]
    return "magnet:?" + "&".join(params)


def wait_for_metadata(handle):
    deadline = time.monotonic() + METADATA_TIMEOUT
    while time.monotonic() < deadline:
        if handle.status().has_metadata:
            return
        time.sleep(POLL_INTERVAL)
    fail(f"No metadata from peers within {METADATA_TIMEOUT}s")


def pick_sample(handle, torrent_id, sha_name, readme_name):
    """File indices for the two root files plus every Nth data file, capped at SAMPLE_SIZE."""
    ti = handle.torrent_file()
    storage = ti.files()
    prefix = torrent_id + "/"
    root, data = [], []
    for i in range(storage.num_files()):
        if storage.file_flags(i) & lt.file_storage.flag_pad_file:
            continue
        name = storage.file_path(i).replace(os.sep, "/")
        if not name.startswith(prefix):
            continue
        rel = name[len(prefix) :]
        if rel in (sha_name, readme_name):
            root.append(i)
        else:
            data.append((i, rel))
    data.sort(key=lambda pair: pair[1])
    stride = max(1, len(data) // max(1, SAMPLE_SIZE - len(root)))
    sampled = [i for i, _ in data[::stride]][: max(0, SAMPLE_SIZE - len(root))]
    return root + sampled


def set_priorities(handle, sample):
    ti = handle.torrent_file()
    priorities = [0] * ti.files().num_files()
    for i in sample:
        priorities[i] = 4
    handle.prioritize_files(priorities)


def wait_for_download(handle, sample):
    ti = handle.torrent_file()
    sizes = {i: ti.files().file_size(i) for i in sample}
    deadline = time.monotonic() + DOWNLOAD_TIMEOUT
    last_print = time.monotonic()
    while time.monotonic() < deadline:
        progress = handle.file_progress()
        done = sum(1 for i in sample if progress[i] >= sizes[i])
        if done == len(sample):
            return
        now = time.monotonic()
        if now - last_print >= 10:
            last_print = now
            print(f"  {done}/{len(sample)} sampled files complete, {handle.status().num_peers} peers")
        time.sleep(POLL_INTERVAL)
    fail(f"Sample didn't finish downloading within {DOWNLOAD_TIMEOUT}s")


def sample_rel_paths(handle, torrent_id, sample):
    storage = handle.torrent_file().files()
    prefix = torrent_id + "/"
    return [storage.file_path(i).replace(os.sep, "/")[len(prefix) :] for i in sample]


def verify(download_dir, torrent_id, manifest, content_dir, sha_name, readme_name, sample_paths):
    problems = []
    root = os.path.join(download_dir, torrent_id)
    on_disk = set(downloaded_paths(download_dir, torrent_id))
    extra = on_disk - set(sample_paths)
    if extra:
        print(f"  {len(extra)} unsampled files also on disk (pieces shared with the sample), not checked")
    for rel in sample_paths:
        if rel not in on_disk:
            problems.append(f"missing after download: {rel}")
            continue
        full = os.path.join(root, rel)
        if not os.path.isfile(full):
            problems.append(f"missing after download: {rel}")
            continue
        digest = sha256_of(full)
        if rel in (sha_name, readme_name):
            expected = sha256_of(os.path.join(content_dir, rel))
        else:
            expected = manifest.get(rel)
        if expected is None:
            problems.append(f"no expected hash for {rel}")
        elif digest != expected:
            problems.append(f"{rel}: downloaded sha256 {digest} != expected {expected}")
    return problems


def downloaded_paths(download_dir, torrent_id):
    """Re-derives the downloaded paths by walking the folder, independent of the session's own bookkeeping."""
    root = os.path.join(download_dir, torrent_id)
    paths = []
    if not os.path.isdir(root):
        return paths
    for dirpath, _, filenames in os.walk(root):
        rel_dir = os.path.relpath(dirpath, root)
        rel_dir = "" if rel_dir == "." else rel_dir.replace(os.sep, "/")
        for name in filenames:
            paths.append(f"{rel_dir}/{name}" if rel_dir else name)
    return paths


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(4 * 1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


if __name__ == "__main__":
    main()
