#!/usr/bin/env python3
"""
prepare-backup: step 17 of the EVE Ref Deep Archive workflow. Plans the Glacier backup: one uncompressed tarball
per torrent, holding every file in the manifest, plus the index that will travel with it.

DRAFT. Writes only SCRATCH_DIR. Doesn't touch S3 or the data bucket.

    16-prepare-backup.py

Needs the previous step's completion file, and refuses to run twice (lib/workflow.py).

The backup is **one tarball**, not many bundles: `<ID>.tar`, uncompressed (the data inside is already compressed),
files in sorted path order, under the torrent's name like the torrent itself. It's uploaded straight to AWS S3 with
storage class DEEP_ARCHIVE (18-backup) -- real AWS Glacier Deep Archive, the one place these scripts use AWS
instead of rclone/B2 (lib/workflow.py).

Writes, in SCRATCH_DIR:

    backup-plan.json          torrent_id, bundle name, total bytes, and every manifest file (path, size, sha256)
    <ID>-backup-index.tsv     path, bundle, size, sha256 for every data file (travels with the backup, PLAN 2.11)
    16-prepare-backup.done    completion file

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
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "prepare-backup"


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    bundle_name = f"{torrent_id}.tar"

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    files = read_manifest(manifest_path, torrent_id)
    total = sum(f["size"] for f in files)
    print(f"{fmt_count(len(files))} files ({fmt_bytes(total)}) in {manifest_path}")

    section("Plan")
    plan = {
        "torrent_id": torrent_id,
        "bundle": bundle_name,
        "bytes": total,
        "files": files,
    }
    plan_path = os.path.join(workflow.scratch_dir, "backup-plan.json")
    write_json(plan_path, plan)
    print(f"One bundle, {bundle_name}: {fmt_count(len(files))} files, {fmt_bytes(total)}")
    print(f"Plan written to {plan_path}")

    section("Backup index")
    index_path = os.path.join(workflow.scratch_dir, f"{torrent_id}-backup-index.tsv")
    write_index(index_path, bundle_name, files)
    print(f"{fmt_count(len(files))} rows in {index_path}")

    workflow.complete(
        STEP, torrent_id, {"bundle": bundle_name, "files": len(files), "bytes": total, "plan": plan_path}
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Prepare backup OK for {torrent_id}")


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
    return sorted(
        ({"path": f["path"], "size": f["size"], "sha256": f["sha256"]} for f in files), key=lambda f: f["path"]
    )


def write_json(path, data):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2, sort_keys=True) + "\n")
    os.rename(tmp, path)


def write_index(path, bundle_name, files):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write("path\tbundle\tsize\tsha256\n")
        for file in files:
            f.write(f"{file['path']}\t{bundle_name}\t{file['size']}\t{file['sha256']}\n")
    os.rename(tmp, path)


if __name__ == "__main__":
    main()
