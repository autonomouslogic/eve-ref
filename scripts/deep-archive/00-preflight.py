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
- the tools later steps need: libtorrent 2.x, the tracker list.
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
    is_inside,
    join,
    load_env,
    ok,
    section,
    start_log,
)

STEP = "preflight"
TRACKERS_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")
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

    section("Tools")
    check_tools(checks)

    section("Remote")
    check_remote(checks)

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
    path = os.path.realpath(TRACKERS_FILE)
    if not os.path.isfile(path):
        checks.fail(f"Tracker list not found: {path}")
        return
    with open(path, encoding="utf-8") as f:
        trackers = [line.strip() for line in f if line.strip() and not line.strip().startswith("#")]
    if not trackers:
        checks.fail(f"No trackers in {path}")
    else:
        print(f"{len(trackers)} trackers in {path}")


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


def fmt_bytes(n):
    for unit, factor in (("TiB", 1024**4), ("GiB", 1024**3), ("MiB", 1024**2), ("KiB", 1024)):
        if n >= factor:
            return f"{n / factor:.1f} {unit}"
    return f"{n} B"


if __name__ == "__main__":
    main()
