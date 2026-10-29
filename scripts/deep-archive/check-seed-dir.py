#!/usr/bin/env python3
"""
check-seed-dir: not a workflow step -- no completion file, and safe to run at any time, independently of any
workflow in progress (it skips SEED_DIR/.staging/, where one might have files staged). Checks the merged SEED_DIR
tree (NOTES.md "Merged seed directory") against every signed torrent in SEED_DIR/.torrents/ (lib/seed_dir.py):

- every file on disk belongs to exactly one torrent, with the size and modification time it records, and no two
  torrents' paths collide once merged (lib/seed_dir.py's quick_check);
- qBittorrent is seeding each torrent cleanly: 100% with 0 bytes downloaded, a seeding state, the right save path
  (check_qbittorrent).

    check-seed-dir.py [--full]

--full additionally runs `sha256sum -c <ID>.sha256` from the SEED_DIR root, for every torrent (check_sha256): slow,
reads every byte of every torrent, where the rest of this script only reads file sizes and modification times.

Used by 00-preflight and 08-check-seed (without --full, quick mode, lib/seed_dir.py's quick_check and
check_qbittorrent only), and README Appendix C's monthly maintenance (--full, from the command line).

Environment (or torrents.env, see lib/workflow.py):
    SEED_DIR, QBITTORRENT_URL, QBITTORRENT_API_KEY

Needs libtorrent 2.x and Docker (minisign, for verify_signature).
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
import seed_dir  # noqa: E402
from qbittorrent import Client, QbittorrentError  # noqa: E402
from workflow import (  # noqa: E402
    TORRENTS_DIR_NAME,
    error,
    fail,
    fmt_count,
    load_env,
    ok,
    qbittorrent_save_path,
    qbittorrent_settings,
    required_dir,
    section,
)

LOG_SAMPLE = 20


def main():
    args = parse_args()
    load_env()
    seed = required_dir("SEED_DIR")
    torrents_dir = os.path.join(seed, TORRENTS_DIR_NAME)

    section("Torrents")
    try:
        torrents, disk, problems = seed_dir.quick_check(seed, torrents_dir)
    except seed_dir.SeedDirError as e:
        fail(str(e))
    print(f"{fmt_count(len(torrents))} signed torrents in {torrents_dir}, {fmt_count(len(disk))} files on disk")

    section("Ownership")
    if problems:
        report(problems)
    else:
        print("Every file is owned by exactly one torrent, no collisions once merged")

    section("qBittorrent")
    url, api_key = qbittorrent_settings()
    try:
        client = Client(url, api_key)
        client.version()
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent at {url}: {e}")
    expected_save_path = qbittorrent_save_path(seed)
    qbittorrent_problems = seed_dir.check_qbittorrent(client, torrents, expected_save_path)
    problems += qbittorrent_problems
    if qbittorrent_problems:
        report(qbittorrent_problems)
    else:
        print(f"All {fmt_count(len(torrents))} torrents seeding cleanly, save path {expected_save_path}")

    if args.full:
        section("sha256sum -c")
        seed_dir.check_sha256(torrents, seed)
        print(f"sha256sum -c passed for all {fmt_count(len(torrents))} torrents, from {seed}")

    if problems:
        fail(f"{fmt_count(len(problems))} problems with {seed}")
    ok(f"SEED_DIR OK: {fmt_count(len(torrents))} torrents, {fmt_count(len(disk))} files")


def report(problems):
    for problem in problems[:LOG_SAMPLE]:
        error(f"  {problem}")
    if len(problems) > LOG_SAMPLE:
        error(f"  ... and {fmt_count(len(problems) - LOG_SAMPLE)} more")


def parse_args():
    parser = argparse.ArgumentParser(description="Check the merged SEED_DIR tree against its signed torrents.")
    parser.add_argument("--full", action="store_true", help="also run sha256sum -c for every torrent (slow)")
    return parser.parse_args()


if __name__ == "__main__":
    main()
