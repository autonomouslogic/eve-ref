#!/usr/bin/env python3
"""
d03-dryrun: a step of the EVE Ref Deep Archive purge (README "Purge"). Checks the reviewed delete list from scratch
against fresh data, then has rclone do a dry run of deleting exactly it, and compares the two. Deletes nothing.

    d03-dryrun.py

Needs d00-preflight's and d01-find's completion files (d02 is the manual review), and refuses to run twice
(lib/purge.py).

1. Reads find/delete.txt as the operator left it. It may have had lines removed; every path in it is then checked
   again, against the signed torrents, a fresh listing of the site and SEED_DIR, by the same rules d01-find used
   (lib/purge.py): a path that is blocked now -- not in a torrent, changed since it was archived, dated this or the
   previous year, no copy in SEED_DIR, a year later than d01-find ran -- fails the step. Paths already gone from the
   site are dropped.
2. `rclone delete --dry-run --files-from-raw` of the remaining list. rclone's own JSON log of what it would delete must
   be exactly that list: none missing, nothing else.

Writes, in PURGE_DIR:

    dryrun/verified.txt     the list d04-delete deletes, sorted
    dryrun/verified.jsonl   the same, with size, last_modified, date, dated_by and torrent_id
    d03-dryrun.done         completion file, with the list's SHA-256 (d04-delete checks it hasn't changed)

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
    describe,
    dry_run,
    fail_on,
    load_state,
    read_path_list,
    sha256_of,
    total_bytes,
    verify_list,
    write_text,
)
from workflow import data_remote, fail, fmt_count, fmt_duration, ok, section, site_path_problem  # noqa: E402

STEP = "dryrun"


def main():
    started = time.monotonic()
    workflow = PurgeWorkflow()
    workflow.begin(STEP)
    remote = data_remote()
    today = datetime.datetime.now(datetime.timezone.utc).date()
    first_kept_year = workflow.read_done("find")["summary"]["first_kept_year"]
    check_not_stale_year(first_kept_year, today)

    section("Reviewed list")
    list_path = os.path.join(workflow.purge_dir, "find", "delete.txt")
    if not os.path.isfile(list_path):
        fail(f"Not found: {list_path}")
    reviewed = read_path_list(list_path)
    fail_on([f"{p!r}: {site_path_problem(p)}" for p in reviewed if site_path_problem(p)], "Not paths of data files")
    print(f"{fmt_count(len(reviewed))} paths in {list_path}")
    if not reviewed:
        fail("The list is empty: nothing to delete")

    state = load_state(workflow, remote)

    section("Verifying the list")
    records, gone = verify_list(sorted(reviewed), state.archived, state.site, workflow.seed_dir, first_kept_year)
    if gone:
        print(f"Already gone from the site, left out: {fmt_count(len(gone))}, e.g.")
        for path in gone[:LOG_SAMPLE]:
            print(f"  {path}")
    if not records:
        fail("Nothing left to delete")
    print(f"Every one of the {describe(records)} is in a torrent, unchanged, old enough and in SEED_DIR")

    section("rclone dry run")
    dry_run(remote, [r["path"] for r in records])

    section("Writing results")
    out_dir = os.path.join(workflow.purge_dir, "dryrun")
    os.makedirs(out_dir, exist_ok=True)
    verified = os.path.join(out_dir, "verified.txt")
    write_text(verified, "".join(r["path"] + "\n" for r in records))
    write_text(os.path.join(out_dir, "verified.jsonl"), "".join(json.dumps(r) + "\n" for r in records))
    print(f"{fmt_count(len(records))} files in {verified}")

    workflow.complete(
        STEP,
        {"files": len(records), "bytes": total_bytes(records), "sha256": sha256_of(verified)},
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Dry run OK: {describe(records)} can be deleted. Nothing was deleted")


if __name__ == "__main__":
    main()
