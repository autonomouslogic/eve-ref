#!/usr/bin/env python3
"""
d04-delete: a step of the EVE Ref Deep Archive purge (README "Purge"). The permanent one: deletes the verified list
from d03-dryrun from the data site, then checks what's left.

    d04-delete.py

Needs d03-dryrun's completion file, and refuses to run twice once it completes (lib/purge.py). If it fails part way,
running it again finishes the job: files already gone are skipped.

1. Checks dryrun/verified.txt is the file d03-dryrun wrote (its SHA-256), and verifies every path in it again
   against the signed torrents, a fresh listing of the site and SEED_DIR (lib/purge.py): a path that is blocked now
   fails the step; one already gone is skipped.
2. rclone's dry run of the list must match it exactly.
3. Asks for the number of files typed back, then verifies against another fresh listing, since the answer can
   come much later.
4. Deletes with `rclone delete --files-from-raw`, an explicit list -- never a prefix or a pattern -- after another
   dry run of that list matched it exactly, and with --max-delete set to its length, so rclone itself refuses to
   delete more. B2 soft-deletes (hard_delete is fixed to false, lib/workflow.py), but treat this as permanent
   (NOTES.md "Deletion").
5. Lists the site again: every deleted path must be gone, and every other file that was on the site must still be.
   Folders the delete left holding only index.html/index.json are listed in index-only-folders.txt: they aren't
   deleted (only files in a torrent ever are), DataIndex has to be run for them (README "Purge").

Writes, in PURGE_DIR:

    deleted.jsonl            every deleted path, with size, last_modified, date, dated_by and torrent_id
    index-only-folders.txt   folders the delete left with nothing but index files (if any)
    d04-delete.done          completion file

Environment (or torrents.env, see lib/workflow.py):
    PURGE_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone and libtorrent 2.x.
"""

import datetime
import json
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from purge import (  # noqa: E402
    LOG_SAMPLE,
    PurgeWorkflow,
    check_not_stale_year,
    delete_files,
    describe,
    dry_run,
    fail_on,
    index_only_folders,
    load_state,
    read_path_list,
    sha256_of,
    total_bytes,
    verify_list,
    write_text,
)
from workflow import (  # noqa: E402
    data_remote,
    fail,
    fmt_count,
    fmt_duration,
    lsjson,
    ok,
    section,
    warning,
)

STEP = "delete"


def main():
    started = time.monotonic()
    workflow = PurgeWorkflow()
    workflow.begin(STEP)
    remote = data_remote()
    today = datetime.datetime.now(datetime.timezone.utc).date()
    first_kept_year = workflow.read_done("find")["summary"]["first_kept_year"]
    check_not_stale_year(first_kept_year, today)

    section("Verified list")
    list_path = os.path.join(workflow.purge_dir, "dryrun", "verified.txt")
    if not os.path.isfile(list_path):
        fail(f"Not found: {list_path}")
    expected_hash = workflow.read_done("dryrun")["summary"]["sha256"]
    if sha256_of(list_path) != expected_hash:
        fail(f"{list_path} has changed since d03-dryrun wrote it: run d03-dryrun again")
    listed = read_path_list(list_path)
    print(f"{fmt_count(len(listed))} paths in {list_path}, as d03-dryrun wrote them")

    state = load_state(workflow, remote)
    section("Verifying the list")
    records, gone = verify_list(listed, state.archived, state.site, workflow.seed_dir, first_kept_year)
    report_gone(gone)
    if not records:
        print("Everything on the list is already gone")
    print(f"To delete: {describe(records)}")

    section("rclone dry run")
    dry_run(remote, [r["path"] for r in records])

    if records:
        typed = input(f"Type {len(records)} to permanently delete {fmt_count(len(records))} files from the site: ")
        if typed.strip() != str(len(records)):
            fail("Confirmation didn't match, aborting")

        section("Verifying again, after confirming")
        state = load_state(workflow, remote)
        again, gone = verify_list([r["path"] for r in records], state.archived, state.site, workflow.seed_dir,
                                  first_kept_year)
        report_gone(gone)
        records = again

        section("Deleting")
        delete_files(remote, [r["path"] for r in records])
        print(f"Deleted {describe(records)}")

    section("Checking the site")
    before = set(state.site)
    after = {path for path, _size, _mtime in lsjson(remote, recursive=True)}
    all_deleted = {r["path"] for r in json_lines(os.path.join(workflow.purge_dir, "dryrun", "verified.jsonl"))}
    problems = [f"{p}: still on the site" for p in sorted(all_deleted & after)]
    problems += [f"{p}: vanished, but wasn't on the list" for p in sorted(before - after - all_deleted)]
    fail_on(problems, "The site isn't as expected after the delete")
    print(f"{fmt_count(len(all_deleted))} listed files are gone, the other {fmt_count(len(after - all_deleted))} "
          "files are still there")

    stale = index_only_folders(after, all_deleted)
    if stale:
        write_text(os.path.join(workflow.purge_dir, "index-only-folders.txt"), "".join(f + "\n" for f in stale))
        warning(f"{fmt_count(len(stale))} folders hold only index files now: run DataIndex (README \"Purge\")")
        for folder in stale[:LOG_SAMPLE]:
            warning(f"  {folder}")

    deleted = json_lines(os.path.join(workflow.purge_dir, "dryrun", "verified.jsonl"))
    write_text(os.path.join(workflow.purge_dir, "deleted.jsonl"), "".join(json.dumps(r) + "\n" for r in deleted))
    workflow.complete(
        STEP, {"deleted_files": len(deleted), "deleted_bytes": total_bytes(deleted), "index_only_folders": len(stale)}
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Delete OK: {describe(deleted)} are no longer on the site")


def report_gone(gone):
    if gone:
        print(f"Already gone from the site (an earlier interrupted run?), skipped: {fmt_count(len(gone))}, e.g.")
        for path in gone[:LOG_SAMPLE]:
            print(f"  {path}")


def json_lines(path):
    with open(path, encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


if __name__ == "__main__":
    main()
