#!/usr/bin/env python3
"""
delete: a step of the EVE Ref Deep Archive workflow. The permanent step: deletes exactly the confirmed delete
set from 12-check-before-delete, then cleans up index files left in folders that delete emptied.

DRAFT. Dry run by default; only --execute writes to the bucket, and only after typing the torrent ID back.

    13-delete.py              # dry run: shows the delete set and rclone's dry run of it, changes nothing, doesn't
                              # complete the step
    13-delete.py --execute    # the same, then asks, re-checks and deletes

Needs 12-check-before-delete's completion file, and refuses to run once the step completed (lib/workflow.py), dry run
included. The dry run takes the lock and writes a log like any step, and can be run any number of times; only
--execute completes the step.

1. Re-checks every file in delete-set.json against a **fresh** listing: a file that changed since
   12-check-before-delete ran is dropped from this run (it stays on the site; Appendix E), not re-included. A file
   already gone (an earlier --execute that failed part way) counts as deleted, so rerunning finishes the job.
2. Runs `rclone delete --dry-run` on the files still to delete, and checks from rclone's JSON log that it would
   delete exactly those files: none missing, nothing else. Without --execute, the step ends here.
3. Asks for the torrent ID, then re-checks against another fresh listing, since the answer can come much later.
   Only files that passed both re-checks are deleted.
4. Deletes them with `rclone delete --files-from-raw`, an explicit list -- never by prefix or pattern -- after
   another dry run of that same list matched it exactly, and with --max-delete set to its length, so rclone itself
   refuses to delete more. B2 soft-deletes (hard_delete is fixed to false, lib/workflow.py): treat this as permanent
   anyway (NOTES.md "Deletion").
5. Cleans up `index.html`/`index.json` from every folder a delete left holding nothing else, and parents left the
   same way (NOTES.md "Stale index files"), deleted like the data in step 4. Never the site root, never under
   `deep-archive/`.

Writes, in SCRATCH_DIR:

    deleted.jsonl           every deleted path, with its size and modification time, kind "data" or "index"
    13-delete.done          completion file (only with --execute)

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone.
"""

import argparse
import contextlib
import json
import os
import subprocess
import sys
import tempfile
import time
from collections import defaultdict

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DEEP_ARCHIVE_DIR,
    Workflow,
    data_remote,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    iso,
    lsjson,
    ok,
    parse_iso,
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
    workflow.begin(STEP, dry_run=not args.execute)

    section("Delete set")
    delete_set_path = os.path.join(workflow.scratch_dir, "delete-set.json")
    with open(delete_set_path, encoding="utf-8") as f:
        delete_set = json.load(f)
    if delete_set.get("torrent_id") != torrent_id:
        fail(f"{delete_set_path} is for {delete_set.get('torrent_id')!r}, not {torrent_id!r}")
    planned = delete_set["files"]
    print(f"{fmt_count(len(planned))} files ({fmt_bytes(sum(f['size'] for f in planned))}) in {delete_set_path}")

    section("Fresh re-check")
    to_delete, gone = fresh_recheck(remote, planned)

    section("rclone dry run")
    dry_run(remote, [f["path"] for f in to_delete])

    if not args.execute:
        print("Dry run: nothing deleted. Run with --execute to delete the files above.")
        return

    typed = input(f"Type {torrent_id} to permanently delete {fmt_count(len(to_delete))} files: ")
    if typed.strip() != torrent_id:
        fail("Confirmation didn't match, aborting")

    section("Fresh re-check, after confirming")
    confirmed = {f["path"] for f in to_delete}
    to_delete, gone = fresh_recheck(remote, planned)
    dropped = sorted(confirmed - {f["path"] for f in to_delete})
    if dropped:
        print(f"Changed or gone since the dry run, not deleted by this run: {fmt_count(len(dropped))}")
        for path in dropped[:LOG_SAMPLE]:
            print(f"  {path}")
    to_delete = [f for f in to_delete if f["path"] in confirmed]
    total_bytes = sum(f["size"] for f in to_delete)

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


def fresh_recheck(remote, planned):
    """Re-checks the planned files against a fresh listing (recheck), printing the result. Returns (delete, gone)."""
    fresh = {path: (size, mtime) for path, size, mtime in lsjson(remote, recursive=True)}
    to_delete, gone, skipped = recheck(planned, fresh)
    print(f"Still matching: {fmt_count(len(to_delete))} files, {fmt_bytes(sum(f['size'] for f in to_delete))}")
    if gone:
        # Usually an earlier --execute that failed part way: they still count as deleted, for the index cleanup and
        # deleted.jsonl.
        print(f"Already gone from the bucket (an earlier interrupted run?): {fmt_count(len(gone))}")
        for g in gone[:LOG_SAMPLE]:
            print(f"  {g['path']}")
    if skipped:
        print(f"Changed since 12-check-before-delete, staying on site: {fmt_count(len(skipped))}")
        for s in skipped[:LOG_SAMPLE]:
            print(f"  {s['path']}: {s['reason']}")
    return to_delete, gone


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


@contextlib.contextmanager
def files_from(paths):
    """A temporary rclone --files-from-raw list of the paths, one per line."""
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".txt", delete=False) as f:
        f.writelines(p + "\n" for p in paths)
    try:
        yield f.name
    finally:
        os.remove(f.name)


def dry_run(remote, paths):
    """rclone's dry run of deleting exactly these paths, checked (check_dry_run). Deletes nothing."""
    if not paths:
        print("Nothing to delete")
        return
    with files_from(paths) as list_file:
        check_dry_run(remote, list_file, paths)


def delete_files(remote, paths):
    """
    Deletes exactly these paths, never by prefix or pattern: rclone delete --files-from-raw, only after a dry run of
    the same list matched it exactly (check_dry_run), and with --max-delete at its length, so rclone itself refuses
    to delete more.
    """
    if not paths:
        return
    with files_from(paths) as list_file:
        check_dry_run(remote, list_file, paths)
        # --files-from-raw: every line is a path as it is (--files-from would skip lines starting with # or ;).
        rclone_run("delete", remote, "--files-from-raw", list_file, "--max-delete", str(len(paths)))


def check_dry_run(remote, list_file, paths):
    """
    Runs rclone delete --dry-run with the list file, the same arguments as the delete itself, and fails unless rclone
    would delete exactly these paths: none missing, nothing else.
    """
    args = ["rclone", "delete", remote, "--files-from-raw", list_file, "--max-delete", str(len(paths))]
    try:
        result = subprocess.run([*args, "--dry-run", "--use-json-log"], capture_output=True, text=True)
    except FileNotFoundError:
        fail("rclone is not installed")
    would, messages = parse_dry_run_log(result.stderr)
    for message in messages:
        print(f"rclone: {message}", flush=True)
    if result.returncode != 0:
        fail(f"rclone delete --dry-run failed: {messages[-1] if messages else f'exit code {result.returncode}'}")
    expected = set(paths)
    missing = sorted(expected - would)
    extra = sorted(would - expected)
    for title, items in (
        ("listed, but not in rclone's dry run", missing),
        ("in rclone's dry run, but not listed", extra),
    ):
        if items:
            error(f"{title}: {fmt_count(len(items))}, e.g.")
            for item in items[:LOG_SAMPLE]:
                error(f"  {item}")
    if missing or extra:
        fail("rclone's dry run doesn't match the list exactly: nothing deleted")
    print(f"rclone's dry run would delete exactly the {fmt_count(len(paths))} files listed, nothing else")


def parse_dry_run_log(text):
    """
    (paths, other messages) from rclone's JSON log (--use-json-log) of a delete --dry-run: each file the dry run skips
    is an entry with "skipped": "delete" and the file's path as "object". Other entries give their "msg"; lines that
    aren't JSON are kept as they are.
    """
    paths = set()
    messages = []
    for line in text.splitlines():
        if not line.strip():
            continue
        try:
            entry = json.loads(line)
        except ValueError:
            entry = None
        if not isinstance(entry, dict):
            messages.append(line)
        elif entry.get("skipped") == "delete" and isinstance(entry.get("object"), str):
            paths.add(entry["object"])
        else:
            messages.append(str(entry.get("msg", line)))
    return paths, messages


def clean_stale_indexes(remote, deleted_paths):
    """
    Deletes the stale index files in a fresh listing (stale_index_files) with delete_files. Returns them, as
    deleted.jsonl records them.
    """
    stale = stale_index_files(lsjson(remote, recursive=True), deleted_paths)
    delete_files(remote, [f["path"] for f in stale])
    return stale


def stale_index_files(listing, deleted_paths):
    """
    The index.html/index.json files ({path, size, last_modified}) of every folder holding a deleted path, or above
    one, with nothing but index files left anywhere beneath it, deepest folders first. Never the site root or
    deep-archive/.
    """
    non_index = set()  # folders with a non-index file anywhere beneath them
    index_files = defaultdict(list)  # folder -> [(name, size, mtime)]
    for path, size, mtime in listing:
        parts = path.split("/")
        if parts[-1] in INDEX_NAMES:
            index_files["/".join(parts[:-1])].append((parts[-1], size, mtime))
        else:
            non_index.update("/".join(parts[:i]) for i in range(len(parts)))
    candidates = set()
    for path in deleted_paths:
        parts = path.split("/")[:-1]
        candidates.update("/".join(parts[:i]) for i in range(1, len(parts) + 1))
    stale = []
    for folder in sorted(candidates, key=lambda f: (-f.count("/"), f)):
        if folder.split("/")[0] == DEEP_ARCHIVE_DIR or folder in non_index:
            continue
        for name, size, mtime in sorted(index_files.get(folder, [])):
            stale.append({"path": f"{folder}/{name}", "size": size, "last_modified": iso(mtime)})
    return stale


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
