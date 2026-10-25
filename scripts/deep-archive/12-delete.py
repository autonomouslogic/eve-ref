#!/usr/bin/env python3
"""
delete: step 12 of the EVE Ref Deep Archive workflow. The permanent step: deletes exactly the confirmed delete
set from 11-check-before-delete, then cleans up index files left in folders that delete emptied.

DRAFT. Dry run by default; only --execute writes to the bucket, and only after typing the torrent ID back.

    12-delete.py              # dry run: shows the delete set, changes nothing, doesn't complete the step
    12-delete.py --execute    # re-checks against a fresh listing, asks, then deletes

Needs 11-check-before-delete's completion file. The dry run can be run any number of times; only --execute
completes the step, and it refuses to run twice (lib/workflow.py).

1. Re-checks every file in delete-set.json against a **fresh** listing (PLAN 2.14): a file that changed since
   11-check-before-delete ran is dropped from this run (it stays on the site; Appendix E), not re-included. A file
   already gone (an earlier --execute that failed part way) counts as deleted, so rerunning finishes the job.
2. `rclone delete --files-from` with the confirmed list -- never by prefix or pattern. B2 soft-deletes
   (hard_delete is fixed to false, lib/workflow.py): treat this as permanent anyway (PLAN 2.14).
3. Cleans up `index.html`/`index.json` from every folder a delete left holding nothing else, and parents left the
   same way, deepest first (PLAN 2.15). Never the site root, never under `deep-archive/`.

Writes, in SCRATCH_DIR:

    deleted.jsonl           every deleted path, with its size and modification time, kind "data" or "index"
    12-delete.done          completion file (only with --execute)

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone.
"""

import argparse
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
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    iso,
    lsjson,
    ok,
    parse_iso,
    rclone,
    rclone_run,
    section,
)

STEP = "delete"
LOG_SAMPLE = 20
INDEX_NAMES = {"index.html", "index.json"}


def main():
    args = parse_args()
    started = time.monotonic()
    workflow = Workflow()
    torrent_id = workflow.torrent_id()
    remote = data_remote()
    if args.execute:
        workflow.begin(STEP)
    else:
        print(f"\U0001f680 (dry run) {STEP}: {torrent_id}")

    section("Delete set")
    delete_set_path = os.path.join(workflow.scratch_dir, "delete-set.json")
    with open(delete_set_path, encoding="utf-8") as f:
        delete_set = json.load(f)
    if delete_set.get("torrent_id") != torrent_id:
        fail(f"{delete_set_path} is for {delete_set.get('torrent_id')!r}, not {torrent_id!r}")
    planned = delete_set["files"]
    print(f"{fmt_count(len(planned))} files ({fmt_bytes(sum(f['size'] for f in planned))}) in {delete_set_path}")

    section("Fresh re-check")
    fresh = {path: (size, mtime) for path, size, mtime in lsjson(remote, recursive=True)}
    to_delete, gone, skipped = recheck(planned, fresh)
    total_bytes = sum(f["size"] for f in to_delete)
    print(f"Still matching: {fmt_count(len(to_delete))} files, {fmt_bytes(total_bytes)}")
    if gone:
        # Usually an earlier --execute that failed part way: they still count as deleted, for the index cleanup and
        # deleted.jsonl.
        print(f"Already gone from the bucket (an earlier interrupted run?): {fmt_count(len(gone))}")
        for g in gone[:LOG_SAMPLE]:
            print(f"  {g['path']}")
    if skipped:
        print(f"Changed since 11-check-before-delete, staying on site: {fmt_count(len(skipped))}")
        for s in skipped[:LOG_SAMPLE]:
            print(f"  {s['path']}: {s['reason']}")

    if not args.execute:
        print("Dry run: nothing deleted. Run with --execute to delete the files above.")
        return

    typed = input(f"Type {torrent_id} to permanently delete {fmt_count(len(to_delete))} files: ")
    if typed.strip() != torrent_id:
        fail("Confirmation didn't match, aborting")

    section("Deleting data")
    deleted_log = os.path.join(workflow.scratch_dir, "deleted.jsonl")
    delete_files(remote, [f["path"] for f in to_delete])
    # Rewritten in full on every run: an interrupted run's data deletes are in gone.
    write_deleted(deleted_log, gone + to_delete, "data")
    print(f"Deleted {fmt_count(len(to_delete))} data files")

    section("Index cleanup")
    cleaned = clean_stale_indexes(remote, [f["path"] for f in gone + to_delete])
    append_deleted(deleted_log, cleaned, "index")
    print(f"Removed {fmt_count(len(cleaned))} stale index.html/index.json files")

    workflow.complete(
        STEP, torrent_id, {"deleted_files": len(to_delete), "deleted_bytes": total_bytes, "index_files": len(cleaned)}
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Delete OK for {torrent_id}")


def parse_args():
    parser = argparse.ArgumentParser(description="Delete the confirmed EVE Ref Deep Archive delete set.")
    parser.add_argument("--execute", action="store_true", help="actually delete; default is a dry run")
    return parser.parse_args()


def recheck(planned, fresh):
    """(still matching: delete, already gone, changed: stay on the site)."""
    to_delete, gone, skipped = [], [], []
    for f in planned:
        current = fresh.get(f["path"])
        expected_mtime = parse_iso(f["last_modified"])
        if current is None:
            gone.append(f)
        elif current[0] != f["size"] or int(current[1]) != expected_mtime:
            skipped.append({"path": f["path"], "reason": "changed since the pre-delete check"})
        else:
            to_delete.append(f)
    return to_delete, gone, skipped


def delete_files(remote, paths):
    if not paths:
        return
    import tempfile

    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as f:
        f.writelines(p + "\n" for p in paths)
        list_file = f.name
    try:
        # --files-from-raw: every line is a path as it is (--files-from would skip lines starting with # or ;).
        rclone_run("delete", remote, "--files-from-raw", list_file)
    finally:
        os.remove(list_file)


def clean_stale_indexes(remote, deleted_paths):
    """
    Folders holding nothing but index.html/index.json, anywhere beneath them, after the delete: their own
    index.html/index.json are removed too, deepest first. Never the site root or deep-archive/.
    """
    fresh = list(lsjson(remote, recursive=True))
    non_index_under = defaultdict(set)  # folder -> set of non-index files anywhere beneath it
    index_files = defaultdict(dict)  # folder -> {name: (size, mtime)}
    for path, size, mtime in fresh:
        parts = path.split("/")
        folder = "/".join(parts[:-1])
        name = parts[-1]
        ancestors = ["/".join(parts[:i]) for i in range(len(parts))]
        if name in INDEX_NAMES:
            index_files[folder][name] = (size, mtime)
        else:
            for ancestor in ancestors:
                non_index_under[ancestor].add(path)
    # Candidate folders: those that touched a just-deleted path, and their ancestors, deepest first.
    candidates = set()
    for path in deleted_paths:
        parts = path.split("/")[:-1]
        for i in range(len(parts), -1, -1):
            candidates.add("/".join(parts[:i]))
    candidates.discard("")
    candidates = {c for c in candidates if c.split("/")[0] != DEEP_ARCHIVE_DIR}
    ordered = sorted(candidates, key=lambda f: f.count("/"), reverse=True)

    cleaned = []
    for folder in ordered:
        if non_index_under.get(folder):
            continue
        for name, (size, mtime) in list(index_files.get(folder, {}).items()):
            remote_path = f"{remote}/{folder}/{name}"
            rclone("deletefile", remote_path)
            cleaned.append({"path": f"{folder}/{name}", "size": size, "last_modified": iso(mtime)})
            index_files[folder].pop(name, None)
    return cleaned


def write_deleted(path, files, kind):
    """Rewrites the log with these files, keeping the index files an earlier interrupted run already removed."""
    earlier_indexes = []
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            earlier_indexes = [line for line in f if line.strip() and json.loads(line).get("kind") == "index"]
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(earlier_indexes)
        for item in files:
            f.write(json.dumps({**item, "kind": kind}) + "\n")


def append_deleted(path, files, kind):
    with open(path, "a", encoding="utf-8") as f:
        for item in files:
            f.write(json.dumps({**item, "kind": kind}) + "\n")


if __name__ == "__main__":
    main()
