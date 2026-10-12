#!/usr/bin/env python3
"""
check-seed: step 10 of the EVE Ref Deep Archive workflow. Checks qBittorrent's own state for the torrent, from
scratch, never trusting 08-seed's own result.

DRAFT. Read-only on qBittorrent.

    09-check-seed.py

Needs 08-seed's completion file, and refuses to run twice (lib/workflow.py).

- 100% progress with **0 bytes downloaded** (any download means the files on disk don't match the torrent: stop
  and investigate, don't just rerun).
- State is a seeding state (not still checking, not an error).
- At least one tracker reports as working.

If the recheck is still running, this fails with the current progress; run it again once it finishes.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, QBITTORRENT_URL, QBITTORRENT_USERNAME, QBITTORRENT_PASSWORD
"""

import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from qbittorrent import Client, QbittorrentError  # noqa: E402
from workflow import (  # noqa: E402
    Workflow,
    error,
    fail,
    fmt_bytes,
    fmt_duration,
    ok,
    qbittorrent_settings,
    section,
)

STEP = "check-seed"
SEEDING_STATES = {"uploading", "stalledUP", "queuedUP", "forcedUP"}
CHECKING_STATES = {"checkingUP", "checkingDL", "checkingResumeData", "allocating", "queuedForChecking"}
TRACKER_WORKING = 2  # qBittorrent's torrents/trackers status: 2 == "Working"


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    seed_done = workflow.read_done("seed")
    info_hash = seed_done["summary"]["info_hash"]

    section("qBittorrent")
    url, username, password = qbittorrent_settings()
    try:
        client = Client(url, username, password)
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent at {url}: {e}")

    info = client.info(info_hash)
    if info is None:
        fail(f"Torrent {info_hash} isn't in qBittorrent")

    problems = []
    progress_pct = info["progress"] * 100
    print(f"State: {info['state']}, progress {progress_pct:.2f}%, downloaded {fmt_bytes(info['downloaded'])}")
    if info["state"] in CHECKING_STATES:
        fail(f"Still checking ({info['state']}): run this again once it finishes")
    if info["progress"] != 1.0:
        problems.append(f"progress is {progress_pct:.2f}%, not 100%")
    if info["downloaded"] != 0:
        problems.append(
            f"{fmt_bytes(info['downloaded'])} downloaded: the files on disk didn't match the torrent. "
            "Investigate before re-seeding -- don't just add it again"
        )
    if info["state"] not in SEEDING_STATES:
        problems.append(f"state is {info['state']!r}, not a seeding state ({sorted(SEEDING_STATES)})")

    section("Trackers")
    try:
        trackers = client.trackers(info_hash)
    except QbittorrentError as e:
        fail(f"Can't read trackers: {e}")
    working = [t for t in trackers if t.get("status") == TRACKER_WORKING]
    for t in trackers:
        if t["url"].startswith(("udp://", "http://", "https://")):
            print(f"  {t['url']}: status {t['status']} ({t.get('msg') or 'ok'}), {t.get('num_seeds', 0)} seeds")
    if not working:
        problems.append("no tracker reports as working yet (give it a few minutes and rerun)")
    else:
        print(f"{len(working)} tracker(s) working")

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the seed")

    workflow.complete(
        STEP,
        torrent_id,
        {"info_hash": info_hash, "progress": info["progress"], "state": info["state"], "working_trackers": len(working)},
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check seed OK for {torrent_id}")


if __name__ == "__main__":
    main()
