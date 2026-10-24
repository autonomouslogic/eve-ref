#!/usr/bin/env python3
"""
check-before-delete: step 10 of the EVE Ref Deep Archive workflow. The last check before anything is deleted:
re-verifies the docs page and seeding, confirms the published torrent matches the local one, and builds the
delete set from the torrent against a fresh listing of the bucket (PLAN 2.14).

DRAFT. Read-only on the bucket and qBittorrent.

    09-check-before-delete.py

Needs 07-publish's completion file, and refuses to run twice (lib/workflow.py).

A data file is in the delete set only if all of these hold:
- it's a data file in the published torrent (not index.json or the README);
- it's in the manifest (so its SHA-256 was checked against the torrent at 100%, PLAN 2.14);
- it's on the bucket right now, in a **fresh** listing, with the same size and modification time recorded when
  it was selected (01-find's files.jsonl).

A file that fails stays on the site (Appendix E); it's listed and doesn't stop the step. Many failures point at a
problem; look before going on (README, "09: Check before delete").

Writes, in SCRATCH_DIR:

    delete-set.json              torrent_id, confirmed files (path, size, last_modified), bytes, datasets, failed
    09-check-before-delete.done  completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY,
    QBITTORRENT_URL, QBITTORRENT_USERNAME, QBITTORRENT_PASSWORD

Needs rclone and libtorrent 2.x.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request
from collections import defaultdict

import libtorrent as lt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from qbittorrent import Client, QbittorrentError  # noqa: E402
from workflow import (  # noqa: E402
    DEEP_ARCHIVE_DIR,
    Workflow,
    classify_path,
    data_remote,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    iso,
    join,
    lsjson,
    ok,
    parse_iso,
    qbittorrent_settings,
    rclone,
    section,
)

STEP = "check-before-delete"
DOCS_PAGE = "https://docs.everef.net/datasets/deep-archive.html"
LOG_SAMPLE = 20


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    remote = data_remote()

    section("Docs")
    check_docs_live()

    section("Seeder")
    check_seeder(workflow, torrent_id)

    section("Published torrent")
    data_paths = check_published_torrent(workflow, remote, torrent_id)

    section("Manifest")
    manifest = read_manifest(os.path.join(workflow.scratch_dir, "manifest.json"), torrent_id)

    section("Selection")
    selection = read_selection(os.path.join(workflow.scratch_dir, "find", "files.jsonl"))

    section("Fresh listing")
    fresh = {path: (size, mtime) for path, size, mtime in lsjson(remote, recursive=True)}
    print(f"{fmt_count(len(fresh))} files on the bucket right now")

    section("Delete set")
    confirmed, failed = build_delete_set(data_paths, manifest, selection, fresh)
    total_bytes = sum(f["size"] for f in confirmed)
    datasets = defaultdict(lambda: [0, 0])
    for f in confirmed:
        d = dataset_of(f["path"])
        datasets[d][0] += 1
        datasets[d][1] += f["size"]
    print(f"Confirmed: {fmt_count(len(confirmed))} files, {fmt_bytes(total_bytes)}")
    for name, (count, size) in sorted(datasets.items()):
        print(f"  {name:<40} {fmt_count(count):>10} {fmt_bytes(size):>12}")
    if failed:
        print(f"FAILED (stay on site): {fmt_count(len(failed))}, e.g.")
        for f in failed[:LOG_SAMPLE]:
            error(f"  {f['path']}: {f['reason']}")
        if len(failed) > len(confirmed) // 2:
            fail(f"{len(failed)} failures is a lot relative to {len(confirmed)} confirmed: look before going on")

    delete_set = {
        "torrent_id": torrent_id,
        "files": confirmed,
        "bytes": total_bytes,
        "datasets": {k: {"files": v[0], "bytes": v[1]} for k, v in datasets.items()},
        "failed": failed,
    }
    path = os.path.join(workflow.scratch_dir, "delete-set.json")
    with open(path + ".tmp", "w", encoding="utf-8") as f:
        f.write(json.dumps(delete_set, indent=2, sort_keys=True) + "\n")
    os.rename(path + ".tmp", path)
    print(f"Written: {path}")

    workflow.complete(
        STEP, torrent_id, {"files": len(confirmed), "bytes": total_bytes, "failed": len(failed)}
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check before delete OK for {torrent_id}")


def check_docs_live():
    try:
        with urllib.request.urlopen(DOCS_PAGE, timeout=60) as response:
            status = response.status
    except urllib.error.HTTPError as e:
        status = e.code
    except urllib.error.URLError as e:
        fail(f"{DOCS_PAGE}: {e.reason}")
    if status != 200:
        fail(f"{DOCS_PAGE}: HTTP {status}")
    print(f"{DOCS_PAGE}: 200")


def check_seeder(workflow, torrent_id):
    url, username, password = qbittorrent_settings()
    seed_done = workflow.read_done("seed")
    info_hash = seed_done["summary"]["info_hash"]
    try:
        client = Client(url, username, password)
        info = client.info(info_hash)
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent: {e}")
    if info is None:
        fail(f"Torrent {info_hash} isn't in qBittorrent")
    if info["progress"] != 1.0 or info["downloaded"] != 0:
        fail(f"Seeder isn't clean: progress {info['progress'] * 100:.2f}%, downloaded {info['downloaded']} bytes")
    print(f"qBittorrent: {info['state']}, 100%, 0 bytes downloaded")


def check_published_torrent(workflow, remote, torrent_id):
    make_torrent = workflow.read_done("make-torrent")["summary"]
    local_v1_hash = make_torrent["v1_info_hash"]
    tmp = os.path.join(workflow.scratch_dir, f"{torrent_id}.published.torrent")
    rclone("copyto", join(remote, f"{DEEP_ARCHIVE_DIR}/{torrent_id}.torrent"), tmp)
    try:
        ti = lt.torrent_info(tmp)
    finally:
        os.remove(tmp)
    published_v1_hash = str(ti.info_hashes().v1)
    if published_v1_hash != local_v1_hash:
        fail(
            f"Published torrent's v1 info-hash {published_v1_hash} != local {local_v1_hash}: "
            "something republished it differently"
        )
    print(f"Published torrent matches: v1 info-hash {published_v1_hash}")
    root_names = {f"{torrent_id}-README.md"}
    prefix = torrent_id + "/"
    storage = ti.files()
    data_paths = set()
    for i in range(storage.num_files()):
        if storage.file_flags(i) & lt.file_storage.flag_pad_file:
            continue
        path = storage.file_path(i).replace(os.sep, "/")
        rel = path[len(prefix) :] if path.startswith(prefix) else path
        if rel in root_names or rel.rsplit("/", 1)[-1] == "index.json":
            continue
        data_paths.add(rel)
    return data_paths


def read_manifest(path, torrent_id):
    if not os.path.isfile(path):
        fail(f"Manifest not found: {path}")
    with open(path, encoding="utf-8") as f:
        manifest = json.load(f)
    if manifest.get("torrent_id") != torrent_id:
        fail(f"{path} is for {manifest.get('torrent_id')!r}, not {torrent_id!r}")
    return {f["path"]: f["sha256"] for f in manifest["files"]}


def read_selection(path):
    if not os.path.isfile(path):
        fail(f"Selection not found: {path}")
    selection = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            record = json.loads(line)
            mtime = parse_iso(record["last_modified"])
            selection[record["path"]] = (record["size"], mtime)
    return selection


def build_delete_set(data_paths, manifest, selection, fresh):
    confirmed = []
    failed = []
    for path in sorted(data_paths):
        if path not in manifest:
            failed.append({"path": path, "reason": "not in the manifest"})
            continue
        selected = selection.get(path)
        if selected is None:
            failed.append({"path": path, "reason": "not in the selection (files.jsonl)"})
            continue
        current = fresh.get(path)
        if current is None:
            failed.append({"path": path, "reason": "no longer on the bucket"})
            continue
        if current[0] != selected[0] or int(current[1]) != selected[1]:
            failed.append({"path": path, "reason": "changed since it was selected: not the archived version"})
            continue
        confirmed.append({"path": path, "size": selected[0], "last_modified": iso(selected[1])})
    return confirmed, failed


def dataset_of(path):
    year, dataset = classify_path(path)
    if year is None:
        return f"{dataset} (no year folder)"
    return dataset or "(root)"


if __name__ == "__main__":
    main()
