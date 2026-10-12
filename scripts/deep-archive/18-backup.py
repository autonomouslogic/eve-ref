#!/usr/bin/env python3
"""
backup: step 19 of the EVE Ref Deep Archive workflow. Builds the torrent's backup tarball and uploads it, with its
metadata, to AWS S3 Glacier Deep Archive.

DRAFT. Shows the target and asks before uploading (lib/workflow.py, "Show, then ask"). Reads SEED_DIR/<ID>/ and
SCRATCH_DIR; writes SCRATCH_DIR/backup/ (deleted once uploaded) and the backup bucket (BACKUP_BUCKET).

    18-backup.py

Needs 17-check-backup-plan's completion file, and refuses to run twice (lib/workflow.py).

This is real AWS S3, not B2 (lib/workflow.py): Glacier Deep Archive is an AWS storage class, so the backup is the
one place these scripts use AWS credentials instead of rclone. Standard boto3 environment variables configure the
client (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_DEFAULT_REGION, or a profile); only the bucket is a setting
here (BACKUP_BUCKET).

1. Build $SCRATCH_DIR/backup/<ID>.tar from the backup plan: one uncompressed tarball, files in sorted path order,
   under "<ID>/" like the torrent itself. Record its SHA-256 next to it (<ID>.tar.sha256) as it's built, so a
   resumed run that finds both doesn't rebuild it.
2. Upload it to s3://BACKUP_BUCKET/everef-deep-archive/<ID>/<ID>.tar with storage class DEEP_ARCHIVE and the
   SHA-256 as object metadata. Skipped if it's already there with the same size and hash. Then delete the local
   tarball: SCRATCH_DIR holds at most one tarball at a time.
3. Upload the small metadata files next to it, in the bucket's default storage class (readable without a restore):
   manifest.json, the backup index, and the torrent's own <ID>.torrent, <ID>.txt, <ID>.sha256, <ID>-README.txt.

Writes, in SCRATCH_DIR:

    backup/<ID>.tar          deleted once uploaded
    backup/<ID>.tar.sha256   its SHA-256, kept for 19-check-backup
    18-backup.done           completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR, BACKUP_BUCKET, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_DEFAULT_REGION

Needs boto3.
"""

import hashlib
import json
import os
import shutil
import sys
import tarfile
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    BACKUP_PREFIX,
    MIB,
    STORAGE_CLASS,
    Workflow,
    backup_bucket,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "backup"
CHUNK_SIZE = 8 * MIB
MULTIPART_THRESHOLD = 256 * MIB
PROGRESS_INTERVAL = 30  # seconds

METADATA_CONTENT_TYPES = {
    ".torrent": "application/x-bittorrent",
    ".txt": "text/plain; charset=utf-8",
    ".sha256": "text/plain; charset=utf-8",
    ".json": "application/json",
}


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    bucket = backup_bucket()
    prefix = f"{BACKUP_PREFIX}/{torrent_id}/"

    section("Plan")
    plan_path = os.path.join(workflow.scratch_dir, "backup-plan.json")
    plan = read_plan(plan_path, torrent_id)
    files = plan["files"]
    total_bytes = plan["bytes"]
    bundle_name = plan["bundle"]
    print(f"{fmt_count(len(files))} files ({fmt_bytes(total_bytes)}) into {bundle_name}")

    section("Target")
    bundle_key = prefix + bundle_name
    print(f"Bucket: {bucket}")
    print(f"Bundle: s3://{bucket}/{bundle_key} (storage class {STORAGE_CLASS})")
    metadata_paths = metadata_files(workflow, content_dir, torrent_id)
    for name in metadata_paths:
        print(f"Metadata: s3://{bucket}/{prefix}{name}")
    typed = input(f"Type {torrent_id} to upload the backup: ")
    if typed.strip() != torrent_id:
        fail("Confirmation didn't match, aborting")

    import boto3

    s3 = boto3.client("s3")

    section("Bundle")
    backup_dir = os.path.join(workflow.scratch_dir, "backup")
    os.makedirs(backup_dir, exist_ok=True)
    tar_path = os.path.join(backup_dir, bundle_name)
    sha_path = tar_path + ".sha256"
    recorded = open(sha_path, encoding="utf-8").read().strip() if os.path.isfile(sha_path) else None
    existing = head_object(s3, bucket, bundle_key)
    if not os.path.isfile(tar_path) and recorded and existing and existing.get("sha256") == recorded:
        # An earlier attempt uploaded the tarball and removed it, then failed on the metadata.
        size, digest = existing["ContentLength"], recorded
        print(f"Already uploaded by an earlier attempt: s3://{bucket}/{bundle_key} ({fmt_bytes(size)}, sha256 {digest})")
    else:
        if os.path.isfile(tar_path) and recorded:
            digest = recorded
            size = os.path.getsize(tar_path)
            print(f"Reusing {tar_path} from an earlier attempt: {fmt_bytes(size)}, sha256 {digest}")
        else:
            # Data plus a 512-byte header per file, padding and long-name headers: a rough upper bound.
            needed = total_bytes + len(files) * 3 * 512
            free = shutil.disk_usage(backup_dir).free
            if free < needed:
                fail(f"Not enough free space in {backup_dir} for the tarball: {fmt_bytes(free)}, need {fmt_bytes(needed)}")
            size, digest = build_tar(tar_path, content_dir, torrent_id, files, started)
            with open(sha_path + ".tmp", "w", encoding="utf-8") as f:
                f.write(digest + "\n")
            os.rename(sha_path + ".tmp", sha_path)
            print(f"Built {tar_path}: {fmt_bytes(size)}, sha256 {digest}")
        print(f"Plan data alone: {fmt_bytes(total_bytes)} (the tarball is bigger: tar headers per file)")

        section("Upload bundle")
        if existing and existing.get("ContentLength") == size and existing.get("sha256") == digest:
            print(f"Already uploaded: s3://{bucket}/{bundle_key} ({fmt_bytes(size)}, sha256 matches)")
        else:
            upload_bundle(s3, tar_path, bucket, bundle_key, digest, size)
            print(f"Uploaded s3://{bucket}/{bundle_key}")
        os.remove(tar_path)
        print(f"Removed local {tar_path}")

    section("Upload metadata")
    for name, local_path in metadata_paths.items():
        key = prefix + name
        upload_metadata(s3, local_path, bucket, key)
        print(f"Uploaded s3://{bucket}/{key}")

    workflow.complete(
        STEP,
        torrent_id,
        {
            "bucket": bucket,
            "prefix": prefix,
            "bundle_key": bundle_key,
            "bundle_bytes": size,
            "bundle_sha256": digest,
            "metadata_files": list(metadata_paths),
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Backup OK for {torrent_id}")


def read_plan(path, torrent_id):
    if not os.path.isfile(path):
        fail(f"Plan not found: {path}")
    with open(path, encoding="utf-8") as f:
        plan = json.load(f)
    if plan.get("torrent_id") != torrent_id:
        fail(f"{path} is for {plan.get('torrent_id')!r}, not {torrent_id!r}")
    if not plan.get("files"):
        fail(f"No files in {path}")
    return plan


def metadata_files(workflow, content_dir, torrent_id):
    """name -> local path, for the small files uploaded next to the bundle. Fails if any is missing."""
    candidates = {
        f"{torrent_id}.torrent": os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent"),
        f"{torrent_id}.txt": os.path.join(workflow.scratch_dir, f"{torrent_id}.txt"),
        f"{torrent_id}.sha256": os.path.join(content_dir, f"{torrent_id}.sha256"),
        f"{torrent_id}-README.txt": os.path.join(content_dir, f"{torrent_id}-README.txt"),
        "manifest.json": os.path.join(workflow.scratch_dir, "manifest.json"),
        f"{torrent_id}-backup-index.tsv": os.path.join(workflow.scratch_dir, f"{torrent_id}-backup-index.tsv"),
    }
    missing = [path for path in candidates.values() if not os.path.isfile(path)]
    if missing:
        fail(f"Missing metadata file(s): {', '.join(missing)}")
    return candidates


def build_tar(tar_path, content_dir, torrent_id, files, started):
    """Builds the tarball (uncompressed, sorted path order) and returns (size, sha256)."""
    tmp = tar_path + ".tmp"
    sha256 = hashlib.sha256()
    last_progress = time.monotonic()
    written_bytes = 0
    total_bytes = sum(f["size"] for f in files)
    with open(tmp, "wb") as raw:
        tee = _TeeWriter(raw, sha256)
        with tarfile.open(fileobj=tee, mode="w") as tar:
            for index, f in enumerate(files, 1):
                full = os.path.join(content_dir, f["path"])
                arcname = f"{torrent_id}/{f['path']}"
                tar.add(full, arcname=arcname, recursive=False)
                written_bytes += f["size"]
                now = time.monotonic()
                if now - last_progress >= PROGRESS_INTERVAL or index == len(files):
                    print(
                        f"{fmt_count(index)}/{fmt_count(len(files))} files, "
                        f"{fmt_bytes(written_bytes)}/{fmt_bytes(total_bytes)}, "
                        f"{fmt_duration(now - started)} elapsed"
                    )
                    last_progress = now
    os.rename(tmp, tar_path)
    return os.path.getsize(tar_path), sha256.hexdigest()


class _TeeWriter:
    """A writable file object that also feeds every write to a hashlib digest. tarfile only needs write/tell."""

    def __init__(self, fileobj, digest):
        self._fileobj = fileobj
        self._digest = digest

    def write(self, data):
        self._digest.update(data)
        return self._fileobj.write(data)

    def tell(self):
        return self._fileobj.tell()

    def flush(self):
        self._fileobj.flush()


def head_object(s3, bucket, key):
    try:
        response = s3.head_object(Bucket=bucket, Key=key)
    except Exception as e:  # botocore.exceptions.ClientError, but avoid importing botocore just for the type
        if "404" in str(e) or "Not Found" in str(e):
            return None
        raise
    return {"ContentLength": response["ContentLength"], "sha256": response.get("Metadata", {}).get("sha256")}


def upload_bundle(s3, path, bucket, key, digest, size):
    from boto3.s3.transfer import TransferConfig

    config = TransferConfig(multipart_threshold=MULTIPART_THRESHOLD, multipart_chunksize=MULTIPART_THRESHOLD)
    last_progress = time.monotonic()
    uploaded = [0]

    def progress(bytes_sent):
        uploaded[0] += bytes_sent
        nonlocal last_progress
        now = time.monotonic()
        if now - last_progress >= PROGRESS_INTERVAL:
            last_progress = now
            print(f"Uploading: {fmt_bytes(uploaded[0])}/{fmt_bytes(size)}")

    s3.upload_file(
        path,
        bucket,
        key,
        ExtraArgs={"StorageClass": "DEEP_ARCHIVE", "Metadata": {"sha256": digest}},
        Config=config,
        Callback=progress,
    )


def upload_metadata(s3, local_path, bucket, key):
    suffix = next((s for s in METADATA_CONTENT_TYPES if local_path.endswith(s)), None)
    content_type = METADATA_CONTENT_TYPES.get(suffix, "application/octet-stream")
    s3.upload_file(local_path, bucket, key, ExtraArgs={"ContentType": content_type})


if __name__ == "__main__":
    main()
