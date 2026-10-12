#!/usr/bin/env python3
"""
check-backup: step 20 of the EVE Ref Deep Archive workflow. Checks the Glacier backup from scratch, against S3
directly: never trusts 18-backup's own result (lib/workflow.py, "Every action has a separate check step").

DRAFT. Read-only on S3.

    19-check-backup.py

Needs 18-backup's completion file, and refuses to run twice (lib/workflow.py).

- The bundle (`<ID>.tar`) exists in the backup bucket with the size and SHA-256 recorded when it was built
  (18-backup's completion file, SCRATCH_DIR/backup/<ID>.tar.sha256), bigger than the plan's data, and with storage
  class DEEP_ARCHIVE.
- Every metadata file (manifest.json, the backup index, `<ID>.torrent`, `<ID>.txt`, `<ID>.sha256`,
  `<ID>-README.txt`) exists next to it with the local file's exact size.

A full restore isn't part of this check (restores from Deep Archive take up to 48 hours); see README, "Restore
drill" for the one-time manual check that reads the backup back.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, BACKUP_BUCKET, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_DEFAULT_REGION

Needs boto3.
"""

import json
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    BACKUP_PREFIX,
    STORAGE_CLASS,
    Workflow,
    backup_bucket,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "check-backup"


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    bucket = backup_bucket()
    prefix = f"{BACKUP_PREFIX}/{torrent_id}/"

    import boto3

    s3 = boto3.client("s3")

    section("Plan")
    plan_path = os.path.join(workflow.scratch_dir, "backup-plan.json")
    with open(plan_path, encoding="utf-8") as f:
        plan = json.load(f)
    bundle_name = plan["bundle"]
    data_bytes = plan["bytes"]
    # The tarball is bigger than the plan's data (a header per file, padding), and its exact size is only known once
    # built: 18-backup recorded it, like the SHA-256 below.
    bundle_bytes = workflow.read_done("backup")["summary"]["bundle_bytes"]
    sha_path = os.path.join(workflow.scratch_dir, "backup", bundle_name + ".sha256")
    if not os.path.isfile(sha_path):
        fail(f"No recorded SHA-256 for the bundle: {sha_path} (run 18-backup first)")
    expected_sha256 = open(sha_path, encoding="utf-8").read().strip()

    problems = []

    section("Bundle")
    bundle_key = prefix + bundle_name
    response = head(s3, bucket, bundle_key)
    if response is None:
        problems.append(f"missing: s3://{bucket}/{bundle_key}")
    else:
        size = response["ContentLength"]
        storage_class = response.get("StorageClass", "STANDARD")
        sha256 = response.get("Metadata", {}).get("sha256")
        print(f"s3://{bucket}/{bundle_key}: {fmt_bytes(size)}, {storage_class}, sha256 {sha256}")
        if size != bundle_bytes:
            problems.append(f"{bundle_key}: {fmt_count(size)} bytes, expected {fmt_count(bundle_bytes)}")
        if size <= data_bytes:
            problems.append(f"{bundle_key}: {fmt_count(size)} bytes, no bigger than the plan's data ({data_bytes})")
        if storage_class != STORAGE_CLASS:
            problems.append(f"{bundle_key}: storage class {storage_class}, expected {STORAGE_CLASS}")
        if sha256 != expected_sha256:
            problems.append(f"{bundle_key}: sha256 metadata {sha256}, expected {expected_sha256}")

    section("Metadata files")
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    metadata = {
        f"{torrent_id}.torrent": os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent"),
        f"{torrent_id}.txt": os.path.join(workflow.scratch_dir, f"{torrent_id}.txt"),
        f"{torrent_id}.sha256": os.path.join(content_dir, f"{torrent_id}.sha256"),
        f"{torrent_id}-README.txt": os.path.join(content_dir, f"{torrent_id}-README.txt"),
        "manifest.json": os.path.join(workflow.scratch_dir, "manifest.json"),
        f"{torrent_id}-backup-index.tsv": os.path.join(workflow.scratch_dir, f"{torrent_id}-backup-index.tsv"),
    }
    for name, local_path in metadata.items():
        key = prefix + name
        response = head(s3, bucket, key)
        if response is None:
            problems.append(f"missing: s3://{bucket}/{key}")
            continue
        local_size = os.path.getsize(local_path) if os.path.isfile(local_path) else None
        print(f"s3://{bucket}/{key}: {fmt_bytes(response['ContentLength'])}")
        if local_size is not None and response["ContentLength"] != local_size:
            problems.append(f"{key}: {response['ContentLength']} bytes, local file is {local_size}")

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the backup")

    workflow.complete(STEP, torrent_id, {"bucket": bucket, "prefix": prefix, "bundle_bytes": bundle_bytes})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check backup OK for {torrent_id}")


def head(s3, bucket, key):
    try:
        return s3.head_object(Bucket=bucket, Key=key)
    except Exception as e:
        if "404" in str(e) or "Not Found" in str(e):
            return None
        raise


if __name__ == "__main__":
    main()
