#!/usr/bin/env python3
"""
d00-preflight: a step of the EVE Ref Deep Archive purge, the deletion of archived data from data.everef.net
(README "Purge"). Checks before anything else runs. Read-only, apart from PURGE_DIR (its completion file and logs).

    d00-preflight.py [--full]

Reports every problem, then aborts if any failed. On success it writes PURGE_DIR/d00-preflight.done, which d01-find
needs. It can't run twice in the same PURGE_DIR: the directory must be empty (log/ aside).

Checks:
- the settings are there: PURGE_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY,
  QBITTORRENT_URL (environment or torrents.env, lib/workflow.py); PURGE_DIR is created if missing, and is separate
  from SEED_DIR;
- rclone, the libtorrent bindings and Docker (minisign verifies the torrents' signatures) are there;
- SEED_DIR/.torrents/ holds signed torrents, and SEED_DIR matches them: every file on disk owned by exactly one
  torrent, with the size and modification time it records (lib/seed_dir.py's quick_check). After the purge this tree
  is the only copy of what was deleted;
- qBittorrent seeds every one of them cleanly: 100%, 0 bytes downloaded, a seeding state;
- the bucket is reachable, and every torrent is published: its .torrent on the site is the signed one in SEED_DIR,
  and its .txt lists exactly its data files;
- the docs page lists every torrent's ID and info-hash (the people downloading the archive are told where it is);
- --full: `sha256sum -c` of every torrent from SEED_DIR, reading every byte (slow; README Appendix C).

Environment (or torrents.env, see lib/workflow.py):
    PURGE_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY,
    QBITTORRENT_URL, QBITTORRENT_API_KEY, QBITTORRENT_SAVE_PATH

Needs rclone, libtorrent 2.x and Docker.
"""

import argparse
import os
import shutil
import subprocess
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
import seed_dir  # noqa: E402
from purge import PurgeWorkflow, archived_data_files, check_published, fail_on, purge_step_name  # noqa: E402
from qbittorrent import Client, QbittorrentError  # noqa: E402
from workflow import (  # noqa: E402
    data_remote,
    fail,
    fmt_count,
    is_inside,
    load_env,
    lock,
    ok,
    print_env_overrides,
    qbittorrent_save_path,
    qbittorrent_settings,
    section,
    start_log,
)

STEP = "preflight"
DOCS_PAGE = "https://docs.everef.net/datasets/deep-archive.html"


def main():
    args = parse_args()
    env_file = load_env()
    purge_dir = prepare_purge_dir()
    lock(purge_dir)
    start_log(purge_dir, STEP, label=purge_step_name(STEP))
    print(f"Settings from {env_file}" if env_file else "Settings from the environment only (no torrents.env)")
    print_env_overrides()
    workflow = PurgeWorkflow()
    remote = data_remote()
    problems = []

    section("Directories")
    entries = sorted(e for e in os.listdir(purge_dir) if e != "log")
    if entries:
        problems.append(f"PURGE_DIR isn't empty: {len(entries)} entries, e.g. {entries[0]}")
    print(f"PURGE_DIR: {purge_dir}")
    print(f"SEED_DIR: {workflow.seed_dir}")
    torrents_dir = workflow.torrents_dir()
    if not os.path.isdir(torrents_dir):
        fail(f"{torrents_dir} not found: is SEED_DIR the right directory?")

    section("Tools")
    check_tools()

    section("SEED_DIR")
    try:
        torrents, disk, seed_problems = seed_dir.quick_check(workflow.seed_dir, torrents_dir)
    except seed_dir.SeedDirError as e:
        fail(str(e))
    if not torrents:
        fail(f"No torrents in {torrents_dir}: nothing is archived")
    print(f"{len(torrents)} signed torrents, {fmt_count(len(disk))} files on disk")
    problems += seed_problems
    archived, archive_problems = archived_data_files(torrents)
    problems += archive_problems
    print(f"{fmt_count(len(archived))} data files in the torrents")

    section("qBittorrent")
    problems += check_qbittorrent(workflow.seed_dir, torrents)

    section("Site")
    problems += check_published(remote, torrents_dir, torrents, archived, purge_dir)
    problems += check_docs_page(torrents)
    print(f"All {len(torrents)} published .torrent and .txt files match")

    if args.full:
        section("sha256sum -c")
        seed_dir.check_sha256(torrents, workflow.seed_dir)
        print(f"sha256sum -c passed for all {len(torrents)} torrents, from {workflow.seed_dir}")

    fail_on(problems, "Preflight found problems")
    workflow.complete(
        STEP,
        {"torrents": sorted(torrents), "files": len(archived), "full": args.full},
    )
    ok("Purge preflight OK")


def parse_args():
    parser = argparse.ArgumentParser(description="Checks before an EVE Ref Deep Archive purge.")
    parser.add_argument("--full", action="store_true", help="also run sha256sum -c for every torrent (slow)")
    return parser.parse_args()


def prepare_purge_dir():
    """PURGE_DIR, created with its parents if missing."""
    value = os.environ.get("PURGE_DIR", "").strip()
    if not value:
        fail("PURGE_DIR is not set (environment or torrents.env)")
    path = os.path.realpath(value)
    seed = os.environ.get("SEED_DIR", "").strip()
    if seed and (is_inside(path, os.path.realpath(seed)) or is_inside(os.path.realpath(seed), path)):
        fail("PURGE_DIR and SEED_DIR must be separate, neither inside the other")
    os.makedirs(path, exist_ok=True)
    if not os.access(path, os.W_OK | os.X_OK):
        fail(f"PURGE_DIR is not writable: {path}")
    return path


def check_tools():
    for tool in ("rclone", "docker"):
        if shutil.which(tool) is None:
            fail(f"Missing required tool: {tool}")
    docker = subprocess.run(["docker", "version"], capture_output=True, text=True)
    if docker.returncode != 0:
        fail("Docker isn't running: minisign runs through it")
    try:
        import libtorrent
    except ImportError:
        fail("Missing required tool: libtorrent Python bindings")
    if int(libtorrent.__version__.split(".")[0]) < 2:
        fail(f"libtorrent must be 2.x, found {libtorrent.__version__}")
    print(f"rclone, Docker and libtorrent {libtorrent.__version__} found")


def check_qbittorrent(seed, torrents):
    url, api_key = qbittorrent_settings()
    try:
        client = Client(url, api_key)
        version = client.version()
    except (QbittorrentError, ValueError) as e:
        return [f"Can't use qBittorrent's Web API at {url}: {e}"]
    save_path = qbittorrent_save_path(seed)
    problems = seed_dir.check_qbittorrent(client, torrents, save_path)
    if not problems:
        print(f"{url}: qBittorrent {version}, all {len(torrents)} torrents seeding cleanly, save path {save_path}")
    return problems


def check_docs_page(torrents):
    """The docs page is live and lists every torrent's ID and v1 info-hash."""
    try:
        with urllib.request.urlopen(DOCS_PAGE, timeout=60) as response:
            text = response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return [f"{DOCS_PAGE}: HTTP {e.code}"]
    except urllib.error.URLError as e:
        return [f"{DOCS_PAGE}: {e.reason}"]
    problems = docs_page_problems(text, torrents)
    if not problems:
        print(f"{DOCS_PAGE}: lists all {len(torrents)} torrents with their v1 info-hashes")
    return problems


def docs_page_problems(text, torrents):
    """The torrents whose ID or v1 info-hash the docs page text doesn't have."""
    problems = []
    lower = text.lower()
    for torrent_id, info in sorted(torrents.items()):
        if torrent_id not in text:
            problems.append(f"The docs page doesn't mention {torrent_id}")
        elif info["v1_hash"].lower() not in lower:
            problems.append(f"The docs page doesn't have {torrent_id}'s v1 info-hash {info['v1_hash']}")
    return problems


if __name__ == "__main__":
    main()
