#!/usr/bin/env python3
"""
check-checksums: step 6 of the EVE Ref Deep Archive workflow. Checks <ID>.sha256 against the manifest, then
re-reads every data file and verifies it with sha256sum.

DRAFT. Read-only.

    05-check-checksums.py

Needs 04-make-checksums's completion file, and refuses to run twice (lib/workflow.py). Slow: re-reads all the
data (PLAN 2.6), hours for a full torrent.

- The `.sha256` has exactly the manifest's files, same hashes, sorted, "sha256sum" format.
- `sha256sum -c` over the content folder reports 0 failures.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
"""

import json
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    Workflow,
    error,
    fail,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "check-checksums"
LOG_SAMPLE = 20


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    content_dir = os.path.join(workflow.seed_dir, torrent_id)

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    manifest = read_manifest(manifest_path, torrent_id)
    print(f"{fmt_count(len(manifest))} files in {manifest_path}")

    section("Checksum file")
    sha_path = os.path.join(content_dir, f"{torrent_id}.sha256")
    problems = check_sha_file(sha_path, manifest)
    if problems:
        for problem in problems[:LOG_SAMPLE]:
            error(f"  {problem}")
        fail(f"{len(problems)} problems in {sha_path}")
    print(f"{fmt_count(len(manifest))} lines in {sha_path} match the manifest")

    section("sha256sum -c")
    print(f"Re-reading every file under {content_dir} (this is slow; hours for a full torrent)")
    run_sha256sum_check(content_dir, f"{torrent_id}.sha256")

    workflow.complete(STEP, torrent_id, {"files": len(manifest)})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check checksums OK for {torrent_id}")


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
    return {f["path"]: f["sha256"] for f in files}


def check_sha_file(path, manifest):
    if not os.path.isfile(path):
        return [f"Checksum file not found: {path}"]
    problems = []
    seen = set()
    previous = None
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if len(line) < 66 or line[64:66] != "  ":
                problems.append(f"line {number}: not \"<64 hex>  <path>\": {line!r}")
                continue
            digest, path_ = line[:64], line[66:]
            if digest != digest.lower() or len(digest.strip("0123456789abcdef")) != 0:
                problems.append(f"line {number}: not a lowercase hex digest: {digest!r}")
            if previous is not None and path_ <= previous:
                problems.append(f"line {number}: not sorted (after {previous!r}): {path_!r}")
            previous = path_
            if path_ in seen:
                problems.append(f"line {number}: duplicate path: {path_!r}")
                continue
            seen.add(path_)
            expected = manifest.get(path_)
            if expected is None:
                problems.append(f"line {number}: not in the manifest: {path_!r}")
            elif expected != digest:
                problems.append(f"line {number}: sha256 {digest} doesn't match the manifest ({expected}): {path_!r}")
    for path_ in sorted(set(manifest) - seen):
        problems.append(f"in the manifest but not in the checksum file: {path_!r}")
    return problems


def run_sha256sum_check(content_dir, sha_name):
    """Runs sha256sum -c from inside content_dir, so the checksum file's relative paths resolve."""
    try:
        result = subprocess.run(
            ["sha256sum", "-c", sha_name],
            cwd=content_dir,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        fail("sha256sum is not installed")
    failures = [line for line in result.stdout.splitlines() if not line.endswith(": OK")]
    if result.returncode != 0 or failures:
        for line in failures[:LOG_SAMPLE]:
            error(f"  {line}")
        if result.stderr.strip():
            error(result.stderr.strip().splitlines()[-1])
        fail(f"sha256sum -c reported {len(failures)} failure(s) (exit code {result.returncode})")
    print("sha256sum -c: 0 failures")


if __name__ == "__main__":
    main()
