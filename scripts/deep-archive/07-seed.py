#!/usr/bin/env python3
"""
seed: a step of the EVE Ref Deep Archive workflow. Adds the torrent to qBittorrent with SEED_DIR, as qBittorrent sees
it, as the save path, so it checks the files where they already are instead of downloading them.

DRAFT. Writes only to qBittorrent (via its Web API) and SCRATCH_DIR.

    07-seed.py

Needs 06-sign-torrent's completion file, and refuses to run twice (lib/workflow.py).

Adds $SCRATCH_DIR/<ID>.torrent to qBittorrent with save path QBITTORRENT_SAVE_PATH (SEED_DIR as qBittorrent sees
it; SEED_DIR itself if not set), automatic torrent management off and content layout Original (the torrent's own <ID>
root folder, whatever qBittorrent's default), so qBittorrent uses $SEED_DIR/<ID>/ (already there from 03-load) rather
than moving or re-downloading anything.
Waits for qBittorrent to report the torrent, turns on super seeding (NOTES.md "Super seeding": has no effect until
the recheck finishes and it's actually a seed, so the order doesn't matter), then triggers a recheck (qBittorrent
doesn't always check a torrent added from existing files without being asked).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR, QBITTORRENT_URL, QBITTORRENT_API_KEY
    QBITTORRENT_SAVE_PATH   optional: SEED_DIR as qBittorrent sees it, if different (e.g. qBittorrent in Docker)
"""

import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from qbittorrent import Client, QbittorrentError  # noqa: E402
from qbittorrent import torrent_id as qbittorrent_torrent_id  # noqa: E402
from workflow import (  # noqa: E402
    Workflow,
    fail,
    fmt_duration,
    ok,
    qbittorrent_save_path,
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
    qbittorrent_id = qbittorrent_torrent_id(v1_hash, make_torrent["summary"]["v2_info_hash"])
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")

    section("qBittorrent")
    url, api_key = qbittorrent_settings()
    try:
        client = Client(url, api_key)
        version = client.version()
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent at {url}: {e}")
    print(f"{url}: qBittorrent {version}")

    print(f"qBittorrent's torrent ID: {qbittorrent_id} (the v2 info-hash, truncated)")
    try:
        existing = client.info(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't read the torrents in qBittorrent: {e}")
    if existing is not None:
        fail(f"Torrent {qbittorrent_id} is already in qBittorrent: remove it by hand first if this is a redo")

    section("Add torrent")
    if not os.path.isfile(torrent_path):
        fail(f"Torrent not found: {torrent_path}")
    save_path = qbittorrent_save_path(workflow.seed_dir)
    print(f"Adding {torrent_path}, save path {save_path}")
    try:
        client.add_torrent_file(torrent_path, save_path)
    except QbittorrentError as e:
        fail(f"Adding the torrent failed: {e}")

    try:
        info = wait_for(client, qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't read the torrent back from qBittorrent: {e}")
    if info is None:
        fail(f"qBittorrent didn't report {qbittorrent_id} within {WAIT_TIMEOUT}s")
    print(f"Added: state {info['state']}, {info['num_seeds']} seeds reported")

    section("Super seeding")
    try:
        client.set_super_seeding(qbittorrent_id, True)
        info = client.info(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Turning on super seeding failed: {e}")
    if info is None or not info.get("super_seeding"):
        fail(f"qBittorrent doesn't report super seeding as on for {qbittorrent_id}")
    print(
        "Super seeding on: has no effect until the recheck below finishes and it's actually a seed. Turn it off "
        "by hand (WebUI) once the torrent has attracted enough peers and seeders (NOTES.md 'Super seeding')"
    )

    section("Recheck")
    try:
        client.recheck(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Triggering the recheck failed: {e}")
    print("Recheck triggered. Run 08-check-seed once it reaches 100%.")

    workflow.complete(
        STEP,
        torrent_id,
        {"info_hash": v1_hash, "qbittorrent_id": qbittorrent_id, "save_path": save_path, "super_seeding": True},
    )
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
