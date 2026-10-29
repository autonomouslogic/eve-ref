#!/usr/bin/env python3
"""
check-before-delete: a step of the EVE Ref Deep Archive workflow. The last check before anything is deleted:
re-verifies the docs page and seeding, confirms the published torrent matches the local one, and builds the
delete set from the torrent against a fresh listing of the bucket (NOTES.md "Deletion").

DRAFT. Read-only on the bucket and qBittorrent.

    12-check-before-delete.py

Needs 09-publish's completion file (update-docs and index-deep-archive are manual), and refuses to run twice (lib/workflow.py). Fails
if the docs page doesn't list the torrent's ID and v1 info-hash: update-docs must come first.

A data file is in the delete set only if all of these hold:
- it's a data file in the published torrent (not the README, checksum file, its signature or the index tarball at
  the torrent's root; index.json is never in a torrent at all, NOTES.md "Index snapshot");
- it's in the manifest (so its SHA-256 was checked against the torrent at 100%);
- it's on the bucket right now, in a **fresh** listing, with the size the torrent gives it, and the same size and
  modification time recorded when it was selected (01-find's files.jsonl).

A file that fails stays on the site (Appendix E); it's listed and doesn't stop the step. Many failures point at a
problem; look before going on (README, "Check before delete").

Writes, in SCRATCH_DIR:

    delete-set.json              torrent_id, confirmed files (path, size, last_modified), bytes, datasets, failed
                                 files (path, reason, on_bucket)
    12-check-before-delete.done  completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY,
    QBITTORRENT_URL, QBITTORRENT_API_KEY

Needs rclone and libtorrent 2.x.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request
from collections import defaultdict

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from qbittorrent import Client, QbittorrentError  # noqa: E402
from workflow import (  # noqa: E402
    refuse_to_run,
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
    root_file_names,
    section,
    warning,
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
    check_docs_page(torrent_id, workflow.read_done("make-torrent")["summary"]["v1_info_hash"])

    section("Seeder")
    check_seeder(workflow, torrent_id)

    section("Published torrent")
    data_files = check_published_torrent(workflow, remote, torrent_id)

    section("Manifest")
    manifest = read_manifest(os.path.join(workflow.scratch_dir, "manifest.json"), torrent_id)

    section("Selection")
    selection = read_selection(os.path.join(workflow.scratch_dir, "find", "files.jsonl"))

    section("Fresh listing")
    fresh = {path: (size, mtime) for path, size, mtime in lsjson(remote, recursive=True)}
    print(f"{fmt_count(len(fresh))} files on the bucket right now")

    section("Delete set")
    confirmed, failed = build_delete_set(data_files, manifest, selection, fresh)
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
        too_many = len(failed) > len(confirmed) // 2
        report = error if too_many else warning
        report(f"FAILED (stay on site): {fmt_count(len(failed))}, e.g.")
        for f in failed[:LOG_SAMPLE]:
            report(f"  {f['path']}: {f['reason']}")
        if too_many:
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


def check_docs_page(torrent_id, v1_hash):
    """The docs page is live and lists this torrent (update-docs), before anything is deleted."""
    try:
        with urllib.request.urlopen(DOCS_PAGE, timeout=60) as response:
            status = response.status
            text = response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        status, text = e.code, ""
    except urllib.error.URLError as e:
        fail(f"{DOCS_PAGE}: {e.reason}")
    if status != 200:
        fail(f"{DOCS_PAGE}: HTTP {status}")
    problems = docs_page_problems(text, torrent_id, v1_hash)
    if problems:
        fail(f"{DOCS_PAGE}: {'; '.join(problems)}: add the torrent to the page first (update-docs)")
    print(f"{DOCS_PAGE}: 200, lists {torrent_id} with its v1 info-hash")


def docs_page_problems(text, torrent_id, v1_hash):
    """What the docs page is missing for this torrent: its ID and its v1 info-hash."""
    problems = []
    if torrent_id not in text:
        problems.append(f"doesn't mention {torrent_id}")
    if v1_hash.lower() not in text.lower():
        problems.append(f"doesn't have the v1 info-hash {v1_hash}")
    return problems


def check_seeder(workflow, torrent_id):
    url, api_key = qbittorrent_settings()
    seed_done = workflow.read_done("seed")
    qbittorrent_id = seed_done["summary"]["qbittorrent_id"]
    try:
        client = Client(url, api_key)
        info = client.info(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent: {e}")
    if info is None:
        fail(f"Torrent {qbittorrent_id} isn't in qBittorrent")
    if info["progress"] != 1.0 or info["downloaded"] != 0:
        fail(f"Seeder isn't clean: progress {info['progress'] * 100:.2f}%, downloaded {info['downloaded']} bytes")
    print(f"qBittorrent: {info['state']}, 100%, 0 bytes downloaded")


def check_published_torrent(workflow, remote, torrent_id):
    """The published torrent's data files, as {site path: size in the torrent}, after checking its info-hash."""
    import libtorrent as lt

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
    storage = ti.files()
    files = [
        (storage.file_path(i).replace(os.sep, "/"), storage.file_size(i))
        for i in range(storage.num_files())
        if not storage.file_flags(i) & lt.file_storage.flag_pad_file
    ]
    return torrent_data_files(files, torrent_id)


def torrent_data_files(files, torrent_id):
    """
    {site path: size} for the torrent's data files, from its (path, size) files without pad files: the root files
    (root_file_names: README, checksum file, its signature, the index tarball) left out. index.json is never in a
    torrent at all any more (NOTES.md "Index snapshot"), so there's nothing left to exclude for it specifically.
    """
    root_names = root_file_names(torrent_id)
    prefix = torrent_id + "/"
    data_files = {}
    for path, size in files:
        rel = path[len(prefix) :] if path.startswith(prefix) else path
        if rel in root_names:
            continue
        data_files[rel] = size
    return data_files


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


def build_delete_set(data_files, manifest, selection, fresh):
    """
    (confirmed, failed) for the torrent's data files ({path: size in the torrent}): confirmed if the file is in the
    manifest and the selection, the bucket has it right now with the torrent's size, and with the selected size and
    modification time. A failed file's on_bucket says whether the bucket has it right now: 14-check-delete expects
    those still there after the delete.
    """
    confirmed = []
    failed = []
    for path, torrent_size in sorted(data_files.items()):
        selected = selection.get(path)
        current = fresh.get(path)
        if path not in manifest:
            reason = "not in the manifest"
        elif selected is None:
            reason = "not in the selection (files.jsonl)"
        elif current is None:
            reason = "no longer on the bucket"
        elif torrent_size != current[0]:
            reason = f"{torrent_size} bytes in the torrent, {current[0]} on the bucket"
        elif current[0] != selected[0] or int(current[1]) != selected[1]:
            reason = "changed since it was selected: not the archived version"
        else:
            confirmed.append({"path": path, "size": selected[0], "last_modified": iso(selected[1])})
            continue
        failed.append({"path": path, "reason": reason, "on_bucket": current is not None})
    return confirmed, failed


def dataset_of(path):
    year, dataset = classify_path(path)
    if year is None:
        return f"{dataset} (no year folder)"
    return dataset or "(root)"


if __name__ == "__main__":
    refuse_to_run("check-before-delete")
    main()
