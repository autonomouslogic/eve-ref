#!/usr/bin/env python3
"""
check-delete: step 23 of the EVE Ref Deep Archive workflow. Checks the delete from scratch, against a fresh
listing of the bucket -- never trusting 21-delete's own result.

DRAFT. Read-only on the bucket.

    22-check-delete.py

Needs 21-delete's completion file, and refuses to run twice (lib/workflow.py).

- Every path in deleted.jsonl (data and index cleanup) is gone from a fresh listing.
- Every path that failed the pre-delete check (delete-set.json's "failed" list) is still there, untouched.
- No folder holds nothing but index.html/index.json, anywhere beneath it (the cleanup fully worked).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone.
"""

import json
import os
import sys
import time
from collections import defaultdict

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DEEP_ARCHIVE_DIR,
    Workflow,
    data_remote,
    error,
    fail,
    fmt_count,
    fmt_duration,
    lsjson,
    ok,
    section,
)

STEP = "check-delete"
LOG_SAMPLE = 20
INDEX_NAMES = {"index.html", "index.json"}


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    remote = data_remote()

    section("Loading records")
    deleted_log = os.path.join(workflow.scratch_dir, "deleted.jsonl")
    deleted = read_jsonl(deleted_log)
    delete_set_path = os.path.join(workflow.scratch_dir, "delete-set.json")
    with open(delete_set_path, encoding="utf-8") as f:
        delete_set = json.load(f)
    failed = delete_set.get("failed", [])
    print(f"{fmt_count(len(deleted))} deleted entries in {deleted_log}")
    print(f"{fmt_count(len(failed))} failed-at-step-20 entries, should still be present")

    section("Fresh listing")
    fresh = {path: (size, mtime) for path, size, mtime in lsjson(remote, recursive=True)}
    print(f"{fmt_count(len(fresh))} files on the bucket right now")

    problems = []

    section("Deleted paths gone")
    still_there = [d["path"] for d in deleted if d["path"] in fresh]
    if still_there:
        problems.append(f"{len(still_there)} deleted path(s) still on the bucket, e.g. {still_there[:LOG_SAMPLE]}")
    else:
        print(f"All {fmt_count(len(deleted))} deleted paths are gone")

    section("Failed paths untouched")
    missing_failed = [f["path"] for f in failed if "path" in f and f["path"] not in fresh]
    if missing_failed:
        problems.append(f"{len(missing_failed)} failed-at-step-20 path(s) are gone too: {missing_failed[:LOG_SAMPLE]}")
    else:
        print(f"All {fmt_count(len(failed))} failed-at-step-20 paths are still there")

    section("Stale indexes")
    stale = find_index_only_folders(fresh)
    if stale:
        problems.append(f"{len(stale)} folder(s) still hold only index files: {stale[:LOG_SAMPLE]}")
    else:
        print("No folder holds only index.html/index.json")

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems after the delete")

    workflow.complete(STEP, torrent_id, {"deleted": len(deleted), "failed_untouched": len(failed)})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check delete OK for {torrent_id}")


def read_jsonl(path):
    if not os.path.isfile(path):
        return []
    with open(path, encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


def find_index_only_folders(fresh):
    non_index_under = defaultdict(set)
    index_folders = set()
    for path in fresh:
        parts = path.split("/")
        name = parts[-1]
        ancestors = ["/".join(parts[:i]) for i in range(len(parts))]
        if name in INDEX_NAMES:
            index_folders.add("/".join(parts[:-1]))
        else:
            for ancestor in ancestors:
                non_index_under[ancestor].add(path)
    stale = []
    for folder in sorted(index_folders):
        if folder == "" or folder.split("/")[0] == DEEP_ARCHIVE_DIR:
            continue
        if not non_index_under.get(folder):
            stale.append(folder)
    return stale


if __name__ == "__main__":
    main()
