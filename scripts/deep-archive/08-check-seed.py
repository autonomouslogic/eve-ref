#!/usr/bin/env python3
"""
check-seed: a step of the EVE Ref Deep Archive workflow. Checks qBittorrent's own state for the torrent, from
scratch, never trusting 07-seed's own result, then downloads the torrent's README through BitTorrent with aria2c,
as a separate client, to show the torrent can actually be downloaded.

DRAFT. Read-only on qBittorrent. Writes only a throwaway folder, SCRATCH_DIR/test-download/, deleted at the end.

    08-check-seed.py

Needs 07-seed's completion file, and refuses to run twice (lib/workflow.py).

qBittorrent (Web API):
- 100% progress with **0 bytes downloaded** (any download means the files on disk don't match the torrent: stop
  and investigate, don't just rerun).
- State is a seeding state (not still checking, not an error).
- At least one tracker reports as working.

Test download (aria2c), only once qBittorrent checks out:
- aria2c gets SCRATCH_DIR/<ID>.torrent (no magnet link) and downloads only <ID>-README.md into a fresh folder,
  finding peers through the trackers and DHT. aria2c doesn't support BitTorrent v2, so it joins the hybrid
  torrent's v1 swarm; the README's index is taken from aria2c --show-files, where the v1 pad files count as files.
- aria2c checks the piece hashes itself; the downloaded README must also be byte-identical to SEED_DIR's copy.
- Gives up after STALL_TIMEOUT seconds without download progress (aria2c --bt-stop-timeout).

It runs on the workflow host, so it proves the torrent can be downloaded, not that the seeder is reachable from
outside. If the seeder is on the same host, aria2c may have to reach qBittorrent through the host's own public address
(NAT hairpinning), which some routers don't allow (NOTES.md "Known limitations").

If the recheck is still running, this fails with the current progress; run it again once it finishes.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR, QBITTORRENT_URL, QBITTORRENT_API_KEY

Needs aria2c.
"""

import filecmp
import os
import re
import shutil
import subprocess
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from qbittorrent import Client, QbittorrentError  # noqa: E402
from trackers import TRACKER_SCHEMES  # noqa: E402
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
STALL_TIMEOUT = 300  # seconds without download progress before aria2c gives up
ARIA2C_SUMMARY_INTERVAL = 30  # seconds between aria2c's progress lines


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    seed_done = workflow.read_done("seed")
    info_hash = seed_done["summary"]["info_hash"]
    qbittorrent_id = seed_done["summary"]["qbittorrent_id"]

    section("qBittorrent")
    url, api_key = qbittorrent_settings()
    client = Client(url, api_key)
    try:
        info = client.info(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent at {url}: {e}")
    if info is None:
        fail(f"Torrent {qbittorrent_id} isn't in qBittorrent")

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
        trackers = client.trackers(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't read trackers: {e}")
    working = working_trackers(trackers)
    for t in trackers:
        if t["url"].startswith(TRACKER_SCHEMES):
            print(f"  {t['url']}: status {t['status']} ({t.get('msg') or 'ok'}), {t.get('num_seeds', 0)} seeds")
    if not working:
        problems.append("no tracker reports as working yet (give it a few minutes and rerun)")
    else:
        print(f"{len(working)} tracker(s) working")

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the seed")

    section("Test download")
    readme_bytes = test_download(workflow, torrent_id)

    workflow.complete(
        STEP,
        torrent_id,
        {
            "info_hash": info_hash,
            "progress": info["progress"],
            "state": info["state"],
            "working_trackers": len(working),
            "test_download_bytes": readme_bytes,
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check seed OK for {torrent_id}")


def working_trackers(trackers):
    """
    The torrent's trackers that qBittorrent's torrents/trackers reports as working. Only real trackers count:
    torrents/trackers also lists DHT, PeX and LSD as "** [DHT] **" etc., working whenever they're enabled.
    """
    return [t for t in trackers if t["url"].startswith(TRACKER_SCHEMES) and t.get("status") == TRACKER_WORKING]


def test_download(workflow, torrent_id):
    """
    Downloads only the README through the torrent with aria2c, into a throwaway folder, and compares it with the seed
    directory's copy. Returns its size.
    """
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")
    readme_name = f"{torrent_id}-README.md"
    seed_copy = os.path.join(workflow.seed_dir, torrent_id, readme_name)
    if not os.path.isfile(torrent_path):
        fail(f"Torrent not found: {torrent_path}")
    index = aria2c_file_index(torrent_path, f"{torrent_id}/{readme_name}")
    print(f"aria2c file {index}: {readme_name}, from {torrent_path}")

    download_dir = os.path.join(workflow.scratch_dir, "test-download")
    if os.path.exists(download_dir):
        shutil.rmtree(download_dir)
    os.makedirs(download_dir)
    download_started = time.monotonic()
    try:
        run_aria2c(
            f"--dir={download_dir}",
            f"--select-file={index}",
            "--seed-time=0",
            f"--bt-stop-timeout={STALL_TIMEOUT}",
            "--file-allocation=none",
            "--enable-color=false",
            f"--summary-interval={ARIA2C_SUMMARY_INTERVAL}",
            "--show-console-readout=false",
            "--console-log-level=warn",
            torrent_path,
        )
        downloaded = os.path.join(download_dir, torrent_id, readme_name)
        if not os.path.isfile(downloaded):
            fail(f"aria2c finished but {downloaded} isn't there")
        if not filecmp.cmp(downloaded, seed_copy, shallow=False):
            fail(f"Downloaded {readme_name} differs from {seed_copy}")
        size = os.path.getsize(downloaded)
    finally:
        shutil.rmtree(download_dir, ignore_errors=True)
    print(
        f"Downloaded {readme_name} ({fmt_bytes(size)}) through the torrent in "
        f"{fmt_duration(time.monotonic() - download_started)}, identical to the seed copy"
    )
    return size


ARIA2C_FILE_RE = re.compile(r"^\s*(\d+)\|(.+)$")


def aria2c_file_index(torrent_path, wanted):
    """
    aria2c's 1-based index of a file (path under the torrent's root folder) in its --show-files listing: the v1 file
    list, pad files included, which is what --select-file counts.
    """
    try:
        result = subprocess.run(["aria2c", "--show-files", torrent_path], capture_output=True, text=True)
    except FileNotFoundError:
        fail("aria2c is not installed")
    if result.returncode != 0:
        lines = [line for line in (result.stderr + result.stdout).splitlines() if line.strip()]
        fail(f"aria2c --show-files failed: {lines[-1] if lines else f'exit code {result.returncode}'}")
    for line in result.stdout.splitlines():
        match = ARIA2C_FILE_RE.match(line)
        if match and match.group(2).strip().endswith("/" + wanted):
            return int(match.group(1))
    fail(f"{wanted} isn't in aria2c's file list for {torrent_path}")


def run_aria2c(*args):
    """Runs aria2c, printing its output as it comes. Fails with its last output line if it exits with an error."""
    try:
        process = subprocess.Popen(["aria2c", *args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    except FileNotFoundError:
        fail("aria2c is not installed")
    last = None
    for line in process.stdout:
        line = line.rstrip("\n")
        if line.strip():
            print(f"aria2c: {line}", flush=True)
            last = line
    process.wait()
    if process.returncode != 0:
        fail(f"aria2c failed (exit code {process.returncode}): {last or 'no output'}")


if __name__ == "__main__":
    main()
