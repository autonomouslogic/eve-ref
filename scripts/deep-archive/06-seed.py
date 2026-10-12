#!/usr/bin/env python3
"""
seed: step 7 of the EVE Ref Deep Archive workflow. Adds the torrent to qBittorrent, save path $SEED_DIR, so it
checks the files where they already are instead of downloading them.

DRAFT. Writes only to qBittorrent (via its Web API) and SCRATCH_DIR.

    06-seed.py

Needs 05-make-torrent's completion file, and refuses to run twice (lib/workflow.py).

Adds $SCRATCH_DIR/<ID>.torrent to qBittorrent with save path $SEED_DIR and automatic torrent management off, so
qBittorrent uses $SEED_DIR/<ID>/ (already there from 04-load) rather than moving or re-downloading anything.
Waits for qBittorrent to report the torrent, then triggers a recheck (qBittorrent doesn't always check a torrent
added from existing files without being asked).

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
    fail,
    fmt_duration,
    ok,
    qbittorrent_settings,
    section,
)

STEP = "seed"
WAIT_TIMEOUT = 60  # seconds, for qBittorrent to report the torrent after adding it
WAIT_INTERVAL = 2


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    make_torrent = workflow.read_done("make-torrent")
    v1_hash = make_torrent["summary"]["v1_info_hash"]
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")

    section("qBittorrent")
    url, username, password = qbittorrent_settings()
    try:
        client = Client(url, username, password)
        version = client.version()
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent at {url}: {e}")
    print(f"{url}: qBittorrent {version}")

    if client.info(v1_hash) is not None:
        fail(f"Torrent {v1_hash} is already in qBittorrent: remove it by hand first if this is a redo")

    section("Add torrent")
    if not os.path.isfile(torrent_path):
        fail(f"Torrent not found: {torrent_path}")
    print(f"Adding {torrent_path}, save path {workflow.seed_dir}")
    try:
        client.add_torrent_file(torrent_path, workflow.seed_dir)
    except QbittorrentError as e:
        fail(f"Adding the torrent failed: {e}")

    info = wait_for(client, v1_hash)
    if info is None:
        fail(f"qBittorrent didn't report {v1_hash} within {WAIT_TIMEOUT}s")
    print(f"Added: state {info['state']}, {info['num_seeds']} seeds reported")

    section("Recheck")
    client.recheck(v1_hash)
    print("Recheck triggered. Run 07-check-seed once it reaches 100%.")

    workflow.complete(STEP, torrent_id, {"info_hash": v1_hash, "save_path": workflow.seed_dir})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Seed OK for {torrent_id}")


def wait_for(client, info_hash):
    deadline = time.monotonic() + WAIT_TIMEOUT
    while time.monotonic() < deadline:
        info = client.info(info_hash)
        if info is not None:
            return info
        time.sleep(WAIT_INTERVAL)
    return None


if __name__ == "__main__":
    main()
