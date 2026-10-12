#!/usr/bin/env python3
"""
make-checksums: step 5 of the EVE Ref Deep Archive workflow. Writes the torrent's checksum file from the manifest.

DRAFT. Writes only $SEED_DIR/<ID>/<ID>.sha256.

    04-make-checksums.py

Needs 03-check-load's completion file, and refuses to run twice (lib/workflow.py).

Standard sha256sum format (PLAN 2.6): "<64 hex chars>  <path>\\n", one line per data file, sorted by path, paths
relative to the torrent root (the site paths). Doesn't list itself or the README.

Writes:
    $SEED_DIR/<ID>/<ID>.sha256
    04-make-checksums.done     completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
"""

import json
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    Workflow,
    fail,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "make-checksums"


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    content_dir = os.path.join(workflow.seed_dir, torrent_id)

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    files = read_manifest(manifest_path, torrent_id)
    print(f"{fmt_count(len(files))} files in {manifest_path}")

    section("Checksums")
    if not os.path.isdir(content_dir):
        fail(f"{content_dir} does not exist: run 02-load and 03-check-load first")
    sha_path = os.path.join(content_dir, f"{torrent_id}.sha256")
    tmp = sha_path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        for path in sorted(files):
            f.write(f"{files[path]}  {path}\n")
    os.rename(tmp, sha_path)
    print(f"{fmt_count(len(files))} lines in {sha_path}")

    workflow.complete(STEP, torrent_id, {"sha256_file": sha_path, "files": len(files)})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Make checksums OK for {torrent_id}")


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


if __name__ == "__main__":
    main()
