#!/usr/bin/env python3
"""
d01-find: a step of the EVE Ref Deep Archive purge (README "Purge"). Lists the whole data site and everything in the
signed torrents, and works out which files on the site may be deleted. Read-only on the bucket; deletes nothing.

    d01-find.py

Needs d00-preflight's completion file, and refuses to run twice (lib/purge.py).

A file is on the list only if it passes every rule in lib/purge.py's docstring: a data file of a signed torrent, still
on the site as archived (size and modification time as the torrent records them), not under NEVER_PURGE, dated before
the start of the previous year by 01-find's own dating (lib/dating.py: modification time, dates and years in the
file name), and present in SEED_DIR. So when this runs in 2026, every file dated 2026 or 2025 stays.

Fails, writing nothing, if SEED_DIR doesn't match its torrents, a published torrent isn't the signed one, a torrent
holds an odd path, or a file that would otherwise go has no matching copy in SEED_DIR.

Writes, in PURGE_DIR:

    find/
      delete.txt         the paths to delete, sorted, one per line: review it (d02), and remove lines to keep files
      delete.jsonl       the same, with size, last_modified, date, dated_by and torrent_id
      report.txt         the printed report
      summary.json       the report, machine-readable
      listing.jsonl.gz   the whole site listing this was made from
    d01-find.done        completion file

Environment (or torrents.env, see lib/workflow.py):
    PURGE_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone and libtorrent 2.x.
"""

import datetime
import gzip
import json
import os
import shutil
import sys
import time
from collections import defaultdict

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from purge import (  # noqa: E402
    LOG_SAMPLE,
    NO_SEED_COPY,
    NEVER_PURGE,
    PurgeWorkflow,
    describe,
    keep_from_year,
    load_state,
    plan_purge,
    total_bytes,
    write_text,
)
from workflow import (  # noqa: E402
    classify_path,
    data_remote,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    iso,
    ok,
    section,
)

STEP = "find"


def main():
    started = time.monotonic()
    workflow = PurgeWorkflow()
    workflow.begin(STEP)
    remote = data_remote()
    today = datetime.datetime.now(datetime.timezone.utc).date()
    first_kept_year = keep_from_year(today)
    print(f"Today: {today}, keeping everything dated {first_kept_year} or later")
    print(f"Never purged: {', '.join(NEVER_PURGE)}")

    state = load_state(workflow, remote)

    section("Plan")
    plan = plan_purge(state.archived, state.site, workflow.seed_dir, first_kept_year)
    no_copy = plan.stays.pop(NO_SEED_COPY, [])
    if no_copy:
        error(f"{fmt_count(len(no_copy))} old files have no matching copy in SEED_DIR, e.g.")
        for path in no_copy[:LOG_SAMPLE]:
            error(f"  {path}")
        fail("SEED_DIR must hold every file before it's deleted from the site: fix SEED_DIR first")
    if not plan.delete:
        fail("Nothing to purge")
    newest = max(r["date"] for r in plan.delete)
    if int(newest[:4]) >= first_kept_year:
        fail(f"Bug: a file dated {newest} is on the delete list")
    summary = build_summary(plan, state, today, first_kept_year)
    report = render_report(summary)
    print(report, end="")

    section("Writing results")
    find_dir = os.path.join(workflow.purge_dir, "find")
    tmp_dir = find_dir + ".tmp"
    if os.path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)
    os.makedirs(tmp_dir)
    write_text(os.path.join(tmp_dir, "delete.txt"), "".join(r["path"] + "\n" for r in plan.delete))
    write_text(os.path.join(tmp_dir, "delete.jsonl"), "".join(json.dumps(r) + "\n" for r in plan.delete))
    write_text(os.path.join(tmp_dir, "summary.json"), json.dumps(summary, indent=2, sort_keys=True) + "\n")
    write_text(os.path.join(tmp_dir, "report.txt"), report)
    with gzip.open(os.path.join(tmp_dir, "listing.jsonl.gz"), "wt", encoding="utf-8") as f:
        for path, (size, mtime) in sorted(state.site.items()):
            f.write(json.dumps({"path": path, "size": size, "last_modified": iso(mtime)}) + "\n")
    if os.path.exists(find_dir):
        print(f"Replacing incomplete output in {find_dir}")
        shutil.rmtree(find_dir)
    os.rename(tmp_dir, find_dir)
    print(f"Review {os.path.join(find_dir, 'report.txt')} and {os.path.join(find_dir, 'delete.txt')}")

    workflow.complete(
        STEP,
        {
            "first_kept_year": first_kept_year,
            "files": len(plan.delete),
            "bytes": total_bytes(plan.delete),
            "newest_date": newest,
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Find OK: {describe(plan.delete)} to delete")


def build_summary(plan, state, today, first_kept_year):
    datasets = defaultdict(lambda: [0, 0])
    for record in plan.delete:
        key = f"{classify_path(record['path'])[1] or '(root)'}, dated {record['date'][:4]}"
        datasets[key][0] += 1
        datasets[key][1] += record["size"]
    stays = {}
    for reason, paths in sorted(plan.stays.items()):
        stays[reason] = {
            "files": len(paths),
            "bytes": sum(state.archived[p].size for p in paths),
            "examples": paths[:LOG_SAMPLE],
        }
    return {
        "today": today.isoformat(),
        "first_kept_year": first_kept_year,
        "torrents": sorted(state.torrents),
        "archived_files": len(state.archived),
        "site_files": len(state.site),
        "gone": len(plan.gone),
        "delete": {
            "files": len(plan.delete),
            "bytes": total_bytes(plan.delete),
            "oldest_date": min(r["date"] for r in plan.delete),
            "newest_date": max(r["date"] for r in plan.delete),
            "datasets": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(datasets.items())},
        },
        "stays": stays,
    }


def render_report(summary):
    delete = summary["delete"]
    lines = [
        f"Purge as of {summary['today']}: everything dated {summary['first_kept_year']} or later stays",
        f"{len(summary['torrents'])} torrents hold {fmt_count(summary['archived_files'])} data files; "
        f"the site has {fmt_count(summary['site_files'])} files",
        f"Already gone from the site: {fmt_count(summary['gone'])}",
        "",
        f"To delete: {fmt_count(delete['files'])} files, {fmt_bytes(delete['bytes'])}, "
        f"dated {delete['oldest_date']} to {delete['newest_date']}",
    ]
    for name, row in delete["datasets"].items():
        lines.append(f"  {name:<55} {fmt_count(row['files']):>10} {fmt_bytes(row['bytes']):>12}")
    lines += ["", "Archived, still on the site, and staying:"]
    if not summary["stays"]:
        lines.append("  (none)")
    for reason, row in summary["stays"].items():
        lines.append(f"  {reason}: {fmt_count(row['files'])} files, {fmt_bytes(row['bytes'])}")
        if reason not in ("this or the previous year",):
            lines += [f"    {path}" for path in row["examples"]]
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    main()
