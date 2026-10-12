#!/usr/bin/env python3
"""
check-backup-plan: step 18 of the EVE Ref Deep Archive workflow. Checks 16-prepare-backup's plan against the
manifest, from scratch.

DRAFT. Read-only.

    17-check-backup-plan.py

Needs 16-prepare-backup's completion file, and refuses to run twice (lib/workflow.py).

- Every manifest file is in the plan exactly once, with the same size and SHA-256.
- The plan has no file the manifest doesn't.
- The plan's "bytes" equals the sum of its files' sizes, which equals the manifest's total.
- `<ID>-backup-index.tsv` has exactly the same rows as the plan (PLAN 2.11, "path, bundle, size, sha256").

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR
"""

import json
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    Workflow,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "check-backup-plan"
LOG_SAMPLE = 20


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    manifest_files = read_manifest(manifest_path, torrent_id)
    manifest_total = sum(f["size"] for f in manifest_files.values())
    print(f"{fmt_count(len(manifest_files))} files ({fmt_bytes(manifest_total)}) in {manifest_path}")

    section("Plan")
    plan_path = os.path.join(workflow.scratch_dir, "backup-plan.json")
    if not os.path.isfile(plan_path):
        fail(f"Plan not found: {plan_path}")
    with open(plan_path, encoding="utf-8") as f:
        plan = json.load(f)
    if plan.get("torrent_id") != torrent_id:
        fail(f"{plan_path} is for {plan.get('torrent_id')!r}, not {torrent_id!r}")
    plan_files = plan.get("files") or []
    print(f"{fmt_count(len(plan_files))} files ({fmt_bytes(plan.get('bytes', 0))}) in {plan_path}")

    problems = []
    seen = set()
    for f in plan_files:
        path = f["path"]
        if path in seen:
            problems.append(f"duplicate in plan: {path}")
            continue
        seen.add(path)
        m = manifest_files.get(path)
        if m is None:
            problems.append(f"in plan but not in manifest: {path}")
        elif m["size"] != f["size"] or m["sha256"] != f["sha256"]:
            problems.append(f"differs from manifest: {path}")
    missing = sorted(set(manifest_files) - seen)
    for path in missing:
        problems.append(f"in manifest but not in plan: {path}")
    plan_total = sum(f["size"] for f in plan_files)
    if plan_total != plan.get("bytes"):
        problems.append(f"plan 'bytes' is {plan.get('bytes')}, files sum to {plan_total}")
    if plan_total != manifest_total:
        problems.append(f"plan totals {fmt_bytes(plan_total)}, manifest totals {fmt_bytes(manifest_total)}")

    report(problems, "plan")

    section("Backup index")
    bundle_name = plan.get("bundle")
    index_path = os.path.join(workflow.scratch_dir, f"{torrent_id}-backup-index.tsv")
    index_problems = check_index(index_path, bundle_name, plan_files)
    report(index_problems, "backup index")
    print(f"{fmt_count(len(plan_files))} rows match the plan")

    workflow.complete(STEP, torrent_id, {"files": len(plan_files), "bytes": plan_total})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check backup plan OK for {torrent_id}")


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
    return {f["path"]: f for f in files}


def check_index(path, bundle_name, plan_files):
    if not os.path.isfile(path):
        return [f"Backup index not found: {path}"]
    with open(path, encoding="utf-8") as f:
        lines = f.read().splitlines()
    if not lines or lines[0] != "path\tbundle\tsize\tsha256":
        return [f"{path}: missing or wrong header"]
    rows = {}
    problems = []
    for number, line in enumerate(lines[1:], 2):
        parts = line.split("\t")
        if len(parts) != 4:
            problems.append(f"{path}:{number}: not 4 tab-separated fields")
            continue
        path_, bundle, size, sha256 = parts
        if path_ in rows:
            problems.append(f"{path}:{number}: duplicate path {path_}")
            continue
        rows[path_] = (bundle, size, sha256)
    expected = {f["path"]: (bundle_name, str(f["size"]), f["sha256"]) for f in plan_files}
    for path_, values in expected.items():
        if path_ not in rows:
            problems.append(f"{path}: missing row for {path_}")
        elif rows[path_] != values:
            problems.append(f"{path}: row for {path_} doesn't match the plan")
    for path_ in set(rows) - set(expected):
        problems.append(f"{path}: row for {path_} isn't in the plan")
    return problems


def report(problems, title):
    if not problems:
        return
    error(f"{len(problems)} problems in the {title}, e.g.")
    for problem in problems[:LOG_SAMPLE]:
        error(f"  {problem}")
    fail(f"{len(problems)} problems in the {title}")


if __name__ == "__main__":
    main()
