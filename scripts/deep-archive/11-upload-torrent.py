#!/usr/bin/env python3
"""
upload-torrent: step 12 of the EVE Ref Deep Archive workflow. Uploads <ID>.torrent and <ID>.txt to the bucket's
deep-archive/, with the right Content-Type. Never overwrites.

DRAFT. Shows the targets and asks first (lib/workflow.py, "Show, then ask"). Writes to the data bucket.

    11-upload-torrent.py

Needs 10-test-download's completion file, and refuses to run twice (lib/workflow.py).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone.
"""

import json
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DEEP_ARCHIVE_DIR,
    Workflow,
    data_remote,
    fail,
    fmt_bytes,
    fmt_duration,
    join,
    ok,
    rclone,
    section,
)

STEP = "upload-torrent"

UPLOADS = (
    ("{id}.torrent", "application/x-bittorrent"),
    ("{id}.txt", "text/plain; charset=utf-8"),
)


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    remote = data_remote()

    section("Targets")
    uploads = []
    for name_fmt, content_type in UPLOADS:
        name = name_fmt.format(id=torrent_id)
        local = os.path.join(workflow.scratch_dir, name)
        if not os.path.isfile(local):
            fail(f"Not found: {local}")
        remote_path = join(remote, f"{DEEP_ARCHIVE_DIR}/{name}")
        uploads.append((local, remote_path, content_type))
        print(f"{local} -> {remote_path} ({content_type}, {fmt_bytes(os.path.getsize(local))})")

    section("Checking for existing files")
    existing = existing_paths(remote, [u[1] for u in uploads])
    if existing:
        fail(f"Already published, never overwritten: {', '.join(existing)}")
    print("Neither file is published yet")

    typed = input(f"Type {torrent_id} to upload: ")
    if typed.strip() != torrent_id:
        fail("Confirmation didn't match, aborting")

    section("Uploading")
    for local, remote_path, content_type in uploads:
        rclone(
            "copyto",
            local,
            remote_path,
            "--header-upload",
            f"Content-Type: {content_type}",
        )
        print(f"Uploaded {remote_path}")

    workflow.complete(STEP, torrent_id, {"uploaded": [u[1] for u in uploads]})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Upload torrent OK for {torrent_id}")


def existing_paths(remote, remote_paths):
    """remote_paths that already exist in the bucket, checked with lsjson so 'never overwrites' is enforced."""
    found = []
    for remote_path in remote_paths:
        folder, _, name = remote_path.rpartition("/")
        out, err = run_rclone("lsjson", "--files-only", "--no-mimetype", folder)
        if err:
            if "directory not found" in err:
                continue
            fail(f"Can't list {folder}: {err}")
        if any(entry["Name"] == name for entry in json.loads(out)):
            found.append(remote_path)
    return found


def run_rclone(*args):
    import subprocess

    try:
        result = subprocess.run(["rclone", *args], capture_output=True, text=True)
    except FileNotFoundError:
        fail("rclone is not installed")
    if result.returncode != 0:
        lines = [line for line in result.stderr.splitlines() if line.strip()]
        return None, lines[-1] if lines else f"exit code {result.returncode}"
    return result.stdout, None


if __name__ == "__main__":
    main()
