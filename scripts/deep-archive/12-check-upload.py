#!/usr/bin/env python3
"""
check-upload: step 13 of the EVE Ref Deep Archive workflow. Checks what data.everef.net actually serves for the
uploaded files, over HTTPS, against the local copies -- never trusting 11-upload-torrent's own result.

DRAFT. Read-only, over HTTPS (the only public-HTTP use these scripts make; PLAN 2.21).

    12-check-upload.py

Needs 11-upload-torrent's completion file, and refuses to run twice (lib/workflow.py).

- https://data.everef.net/deep-archive/<ID>.torrent and <ID>.txt both return 200.
- Content-Type matches what was uploaded (application/x-bittorrent, text/plain; charset=utf-8).
- The bytes served are byte-for-byte the local files.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR
"""

import os
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DATA_BASE_URL,
    DEEP_ARCHIVE_DIR,
    Workflow,
    error,
    fail,
    fmt_duration,
    ok,
    section,
)

STEP = "check-upload"

CHECKS = (
    ("{id}.torrent", "application/x-bittorrent"),
    ("{id}.txt", "text/plain; charset=utf-8"),
)


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()

    problems = []
    for name_fmt, content_type in CHECKS:
        name = name_fmt.format(id=torrent_id)
        local = os.path.join(workflow.scratch_dir, name)
        url = DATA_BASE_URL.rstrip("/") + f"/{DEEP_ARCHIVE_DIR}/{name}"
        section(name)
        problems += check(url, local, content_type)

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the upload")

    workflow.complete(STEP, torrent_id, {})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check upload OK for {torrent_id}")


def check(url, local_path, expected_content_type):
    if not os.path.isfile(local_path):
        return [f"Local file not found: {local_path}"]
    with open(local_path, "rb") as f:
        local_bytes = f.read()
    print(f"GET {url}")
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            status = response.status
            headers = response.headers
            body = response.read()
    except urllib.error.HTTPError as e:
        return [f"{url}: HTTP {e.code}"]
    except urllib.error.URLError as e:
        return [f"{url}: {e.reason}"]
    problems = []
    print(f"  {status} {headers.get('Content-Type')}, {len(body)} bytes")
    if status != 200:
        problems.append(f"{url}: status {status}, expected 200")
    content_type = (headers.get("Content-Type") or "").strip()
    if content_type != expected_content_type:
        problems.append(f"{url}: Content-Type {content_type!r}, expected {expected_content_type!r}")
    if body != local_bytes:
        problems.append(f"{url}: served bytes don't match {local_path} ({len(body)} vs {len(local_bytes)} bytes)")
    return problems


if __name__ == "__main__":
    main()
