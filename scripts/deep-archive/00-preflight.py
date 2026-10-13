#!/usr/bin/env python3
"""
preflight: step 1 of the EVE Ref Deep Archive workflow. Basic checks before a workflow starts.

DRAFT. Read-only, apart from its own completion file.

    00-preflight.py

Runs every check and reports each failure, then aborts if any failed. On success it writes
SCRATCH_DIR/00-preflight.done, which 01-find needs. It can't run twice in the same SCRATCH_DIR: the scratch
directory must be empty.

Checks:
- the settings are there: SCRATCH_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY
  (environment or torrents.env, see lib/workflow.py);
- SCRATCH_DIR and SEED_DIR exist (created, with their parents, if missing) and are writable;
- SCRATCH_DIR is empty (log/ aside): one workflow per scratch directory;
- SEED_DIR has room for the largest possible torrent (MAX_BYTES);
- SCRATCH_DIR and SEED_DIR are separate;
- rclone is installed;
- the data site's bucket is reachable (the site root's index.json), and deep-archive/ is listable;
- the tools later steps need: libtorrent 2.x, aria2c (06-check-seed's test download);
- the tracker lists make-torrent downloads (lib/trackers.py) are reachable, each has UDP or HTTP(S) trackers, and
  together they have at least MIN_TRACKERS;
- LOCAL_COPY_DIR, if set, is a readable folder laid out like the bucket (index.json at the top), apart from
  SCRATCH_DIR and SEED_DIR;
- qBittorrent's Web API answers at QBITTORRENT_URL with the configured login, and runs libtorrent 2.x.
"""

import json
import os
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MAX_BYTES,
    Workflow,
    data_remote,
    error,
    fail,
    fmt_bytes,
    is_inside,
    join,
    load_env,
    local_copy_dir,
    ok,
    qbittorrent_save_path,
    qbittorrent_settings,
    section,
    start_log,
)
from qbittorrent import Client, QbittorrentError  # noqa: E402
from trackers import MIN_TRACKERS, TrackersError, combine, fetch_tracker_lists  # noqa: E402

STEP = "preflight"
DEEP_ARCHIVE_DIR = "deep-archive"


class Checks:
    """Counts failed checks, so every problem is reported before aborting."""

    def __init__(self):
        self.failed = 0

    def fail(self, message):
        self.failed += 1
        error(message)


def main():
    checks = Checks()
    env_file = load_env()

    # The scratch directory must be empty, apart from the logs of earlier preflight attempts that failed.
    scratch, scratch_created, scratch_problem = prepare_dir("SCRATCH_DIR")
    scratch_entries = sorted(e for e in os.listdir(scratch) if e != "log") if scratch else None
    if scratch:
        start_log(scratch, STEP)
    else:
        print(f"# 🚀 00-{STEP}")

    section("Settings")
    print(f"Settings from {env_file}" if env_file else "Settings from the environment only (no torrents.env)")

    section("Directories")
    if scratch is None:
        checks.fail(scratch_problem)
    else:
        if scratch_created:
            print(f"Created SCRATCH_DIR: {scratch}")
        if scratch_entries:
            checks.fail(f"SCRATCH_DIR isn't empty: {len(scratch_entries)} entries, e.g. {scratch_entries[0]}")
        else:
            print(f"SCRATCH_DIR: {scratch}, empty")
    seed, seed_created, seed_problem = prepare_dir("SEED_DIR")
    if seed is None:
        checks.fail(seed_problem)
    else:
        if seed_created:
            print(f"Created SEED_DIR: {seed}")
        free = shutil.disk_usage(seed).free
        if free < MAX_BYTES:
            checks.fail(
                f"Not enough free space in SEED_DIR: {fmt_bytes(free)}, a torrent can be up to {fmt_bytes(MAX_BYTES)}"
            )
        else:
            print(f"SEED_DIR: {seed}, {fmt_bytes(free)} free (a torrent can be up to {fmt_bytes(MAX_BYTES)})")
    if scratch and seed:
        if is_inside(scratch, seed) or is_inside(seed, scratch):
            checks.fail("SCRATCH_DIR and SEED_DIR must not be inside each other")
        else:
            print("SCRATCH_DIR and SEED_DIR are separate")
    check_local_copy(checks, scratch, seed)

    section("Tools")
    check_tools(checks)

    section("Trackers")
    check_trackers(checks)

    section("Remote")
    check_remote(checks)

    section("qBittorrent")
    check_qbittorrent(checks, seed)

    if checks.failed:
        fail(f"Preflight found {checks.failed} problem{'s' if checks.failed != 1 else ''}, aborting")
    workflow = Workflow()
    workflow.complete(STEP, None, {"env_file": env_file})
    ok("Preflight OK")


def prepare_dir(name):
    """
    (path, created, None) for the setting's directory, creating it and its parents if it doesn't exist; or
    (None, False, problem) if it isn't set, can't be created, isn't a directory, or isn't writable.
    """
    value = os.environ.get(name)
    if not value:
        return None, False, f"{name} is not set (environment or torrents.env)"
    path = os.path.realpath(value)
    created = False
    if not os.path.exists(path):
        try:
            os.makedirs(path)
            created = True
        except OSError as e:
            return None, False, f"Couldn't create {name} {path}: {e.strerror}"
    if not os.path.isdir(path):
        return None, False, f"{name} is not a directory: {path}"
    if not os.access(path, os.W_OK | os.X_OK):
        return None, False, f"{name} is not writable: {path}"
    return path, created, None


def check_local_copy(checks, scratch, seed):
    """LOCAL_COPY_DIR, if set: a folder laid out like the bucket (index.json at the top), apart from the other two."""
    local = local_copy_dir()
    if local is None:
        print("LOCAL_COPY_DIR: not set, load downloads everything from the bucket")
        return
    if not os.path.isdir(local):
        checks.fail(f"LOCAL_COPY_DIR is not a directory: {local}")
        return
    if not os.access(local, os.R_OK | os.X_OK):
        checks.fail(f"LOCAL_COPY_DIR is not readable: {local}")
        return
    if not os.path.isfile(os.path.join(local, "index.json")):
        checks.fail(f"LOCAL_COPY_DIR has no index.json at the top: {local} isn't laid out like the bucket")
        return
    for name, path in (("SCRATCH_DIR", scratch), ("SEED_DIR", seed)):
        if path and (is_inside(local, path) or is_inside(path, local)):
            checks.fail(f"LOCAL_COPY_DIR and {name} must not be inside each other")
            return
    print(f"LOCAL_COPY_DIR: {local}, load copies matching files from it")


def check_qbittorrent(checks, seed):
    """qBittorrent's Web API answers with the configured login, and runs libtorrent 2.x (needed to seed v2)."""
    if not os.environ.get("QBITTORRENT_URL", "").strip():
        checks.fail("QBITTORRENT_URL is not set (environment or torrents.env)")
        return
    url, username, password = qbittorrent_settings()
    try:
        client = Client(url, username, password)
        version = client.version()
        build = client.build_info()
    except (QbittorrentError, ValueError) as e:
        checks.fail(f"Can't use qBittorrent's Web API at {url}: {e}")
        return
    libtorrent = str(build.get("libtorrent", ""))
    print(f"{url}: qBittorrent {version}, libtorrent {libtorrent}")
    if not libtorrent.split(".")[0].isdigit() or int(libtorrent.split(".")[0]) < 2:
        checks.fail(f"qBittorrent must run libtorrent 2.x to seed hybrid v1+v2 torrents, found {libtorrent!r}")
    if seed:
        print(f"Save path for new torrents: {qbittorrent_save_path(seed)} (QBITTORRENT_SAVE_PATH, or SEED_DIR)")


def run_rclone(*args):
    """(stdout, None) or (None, rclone's last error line)."""
    try:
        result = subprocess.run(["rclone", *args], capture_output=True, text=True)
    except FileNotFoundError:
        return None, "rclone is not installed"
    if result.returncode != 0:
        lines = [line for line in result.stderr.splitlines() if line.strip()]
        return None, lines[-1] if lines else f"exit code {result.returncode}"
    return result.stdout, None


def check_tools(checks):
    out, error_line = run_rclone("version")
    if error_line:
        checks.fail(f"Missing required tool: rclone ({error_line})")
    else:
        print(out.splitlines()[0])
    try:
        import libtorrent

        version = libtorrent.__version__
        if int(version.split(".")[0]) < 2:
            checks.fail(f"libtorrent must be 2.x for hybrid v1+v2 torrents, found {version}")
        else:
            print(f"libtorrent {version}")
    except ImportError:
        checks.fail("Missing required tool: libtorrent Python bindings (needed by make-torrent)")
    aria2c = subprocess.run(["aria2c", "--version"], capture_output=True, text=True) if shutil.which("aria2c") else None
    if aria2c is None or aria2c.returncode != 0:
        checks.fail("Missing required tool: aria2c (needed by 06-check-seed's test download)")
    else:
        print(aria2c.stdout.splitlines()[0] if aria2c.stdout else "aria2c")


def check_trackers(checks):
    """The tracker lists make-torrent downloads are reachable and give enough trackers today (lib/trackers.py)."""
    try:
        lists = fetch_tracker_lists()
        trackers, counts = combine([(name, text) for name, _, text in lists])
    except TrackersError as e:
        checks.fail(f"{e} (make-torrent would fail)")
        return
    for name, usable, _ in counts:
        print(f"{name}: {usable} UDP or HTTP(S) trackers")
    print(f"{len(trackers)} trackers together (at least {MIN_TRACKERS} needed)")


def check_remote(checks):
    settings = ("DATA_BUCKET", "RCLONE_CONFIG_EVEREF_ACCOUNT", "RCLONE_CONFIG_EVEREF_KEY")
    missing = [name for name in settings if not os.environ.get(name, "").strip()]
    if missing:
        checks.fail(f"{', '.join(missing)} not set (environment or torrents.env)")
        return
    remote = data_remote()
    print(f"Bucket: {remote}")
    print(f"B2 key: {os.environ['RCLONE_CONFIG_EVEREF_ACCOUNT']}")

    out, error_line = run_rclone("lsjson", "--no-mimetype", join(remote, "index.json"))
    entries = json.loads(out) if out else []
    if error_line or not entries:
        checks.fail(f"Can't read {join(remote, 'index.json')}: {error_line or 'not found'}")
        return
    print(f"Site root: index.json modified {entries[0]['ModTime'][:19]}")

    out, error_line = run_rclone("lsjson", "--files-only", "--no-mimetype", join(remote, DEEP_ARCHIVE_DIR))
    if error_line and "directory not found" in error_line:
        print(f"{DEEP_ARCHIVE_DIR}/ doesn't exist yet: no published torrents")
    elif error_line:
        checks.fail(f"Can't list {join(remote, DEEP_ARCHIVE_DIR)}: {error_line}")
    else:
        torrents = sorted(e["Path"] for e in json.loads(out) if e["Path"].endswith(".torrent"))
        if torrents:
            print(f"{DEEP_ARCHIVE_DIR}/: {len(torrents)} published torrents, newest {torrents[-1]}")
        else:
            print(f"{DEEP_ARCHIVE_DIR}/: no published torrents yet")


if __name__ == "__main__":
    main()
