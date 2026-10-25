#!/usr/bin/env python3
"""
preflight: step 00 of the EVE Ref Deep Archive workflow. Basic checks before a workflow starts.

DRAFT. Read-only, apart from its own completion file.

    00-preflight.py

Runs every check and reports each failure, then aborts if any failed; a missing tool aborts straight after the tool
checks, since the later checks need them. On success it writes
SCRATCH_DIR/00-preflight.done, which 01-find needs. It can't run twice in the same SCRATCH_DIR: the scratch
directory must be empty.

Checks:
- the settings are there: SCRATCH_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY
  (environment or torrents.env, see lib/workflow.py); settings the environment overrides are named (not a failure);
- SCRATCH_DIR and SEED_DIR exist (created, with their parents, if missing) and are writable;
- SCRATCH_DIR is empty (log/ aside): one workflow per scratch directory;
- SEED_DIR has room for the largest possible torrent (MAX_BYTES);
- SCRATCH_DIR and SEED_DIR are separate;
- rclone is installed;
- the data site's bucket is reachable (the site root's index.json), and deep-archive/ is listable; also picks the
  next torrent ID (one past the highest published one, or 00 if there are none), for 01-find to use;
- the other required tools: libtorrent 2.x, aria2c, sha256sum (03-load uses it to verify the checksum file it just
  wrote), Docker (04-sign runs minisign through it);
- qBittorrent's Web API answers at QBITTORRENT_URL with QBITTORRENT_API_KEY, and runs libtorrent 2.x;
- the tracker lists make-torrent downloads (lib/trackers.py) are reachable, each has UDP or HTTP(S) trackers, and
  together they have at least MIN_TRACKERS;
- LOCAL_COPY_DIR, if set, is a readable folder laid out like the bucket (index.json at the top), apart from
  SCRATCH_DIR and SEED_DIR.
"""

import json
import os
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DEEP_ARCHIVE_DIR,
    ENV_OVERRIDES,
    MAX_BYTES,
    Workflow,
    data_remote,
    error,
    fail,
    fmt_bytes,
    format_torrent_id,
    is_inside,
    join,
    load_env,
    local_copy_dir,
    lock,
    ok,
    qbittorrent_save_path,
    qbittorrent_settings,
    section,
    start_log,
    torrent_sequence,
)
from qbittorrent import Client, QbittorrentError  # noqa: E402
from trackers import MIN_TRACKERS, TrackersError, combine, fetch_tracker_lists  # noqa: E402

STEP = "preflight"


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
        lock(scratch)
        start_log(scratch, STEP)
    else:
        print(f"🚀 00-{STEP}")

    section("Settings")
    print(f"Settings from {env_file}" if env_file else "Settings from the environment only (no torrents.env)")
    if ENV_OVERRIDES:
        # Names only: some of them are secrets.
        print(f"Note: set in the environment, overriding torrents.env: {', '.join(sorted(ENV_OVERRIDES))}")

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
    failed_before_tools = checks.failed
    check_tools(checks)
    if checks.failed > failed_before_tools:
        fail("Required tools are missing, aborting before the remaining checks")

    # qBittorrent first: it answers much faster than the tracker lists and the bucket.
    section("qBittorrent")
    check_qbittorrent(checks, seed)

    section("Trackers")
    check_trackers(checks)

    section("Remote")
    next_torrent_id = check_remote(checks)

    if checks.failed:
        fail(f"Preflight found {checks.failed} problem{'s' if checks.failed != 1 else ''}, aborting")
    workflow = Workflow()
    workflow.complete(STEP, None, {"env_file": env_file, "next_torrent_id": next_torrent_id})
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
    """qBittorrent's Web API answers with the configured API key, and runs libtorrent 2.x (needed to seed v2)."""
    if not os.environ.get("QBITTORRENT_URL", "").strip():
        checks.fail("QBITTORRENT_URL is not set (environment or torrents.env)")
        return
    url, api_key = qbittorrent_settings()
    try:
        client = Client(url, api_key)
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
        checks.fail("Missing required tool: libtorrent Python bindings")
    aria2c = subprocess.run(["aria2c", "--version"], capture_output=True, text=True) if shutil.which("aria2c") else None
    if aria2c is None or aria2c.returncode != 0:
        checks.fail("Missing required tool: aria2c")
    else:
        print(aria2c.stdout.splitlines()[0] if aria2c.stdout else "aria2c")
    if shutil.which("sha256sum") is None:
        checks.fail("Missing required tool: sha256sum")
    else:
        print("sha256sum found")
    docker = subprocess.run(["docker", "version"], capture_output=True, text=True) if shutil.which("docker") else None
    if docker is None or docker.returncode != 0:
        checks.fail("Missing required tool: Docker (04-sign runs minisign through it)")
    else:
        print("Docker found")


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
    """Also picks the next torrent ID (sequential, TORRENT_ID_RE): one past the highest published one, or 00 if
    there are none. Returns it, or None if it couldn't be determined."""
    settings = ("DATA_BUCKET", "RCLONE_CONFIG_EVEREF_ACCOUNT", "RCLONE_CONFIG_EVEREF_KEY")
    missing = [name for name in settings if not os.environ.get(name, "").strip()]
    if missing:
        checks.fail(f"{', '.join(missing)} not set (environment or torrents.env)")
        return None
    remote = data_remote()
    print(f"Bucket: {remote}")
    print(f"B2 key: {os.environ['RCLONE_CONFIG_EVEREF_ACCOUNT']}")

    out, error_line = run_rclone("lsjson", "--no-mimetype", join(remote, "index.json"))
    entries = json.loads(out) if out else []
    if error_line or not entries:
        checks.fail(f"Can't read {join(remote, 'index.json')}: {error_line or 'not found'}")
        return None
    print(f"Site root: index.json modified {entries[0]['ModTime'][:19]}")

    out, error_line = run_rclone("lsjson", "--files-only", "--no-mimetype", join(remote, DEEP_ARCHIVE_DIR))
    if error_line and "directory not found" in error_line:
        print(f"{DEEP_ARCHIVE_DIR}/ doesn't exist yet: no published torrents")
        return format_torrent_id(0)
    if error_line:
        checks.fail(f"Can't list {join(remote, DEEP_ARCHIVE_DIR)}: {error_line}")
        return None

    torrents = sorted(e["Path"] for e in json.loads(out) if e["Path"].endswith(".torrent"))
    if not torrents:
        print(f"{DEEP_ARCHIVE_DIR}/: no published torrents yet")
        return format_torrent_id(0)

    sequences = []
    for path in torrents:
        torrent_id = path[: -len(".torrent")]
        sequence = torrent_sequence(torrent_id)
        if sequence is None:
            checks.fail(f"{DEEP_ARCHIVE_DIR}/{path}: {torrent_id!r} isn't a valid torrent ID")
        else:
            sequences.append(sequence)
    if not sequences:
        return None
    newest = format_torrent_id(max(sequences))
    print(f"{DEEP_ARCHIVE_DIR}/: {len(torrents)} published torrents, newest {newest}")
    next_sequence = max(sequences) + 1
    if next_sequence > 99:
        checks.fail("All double-digit torrent IDs (00-99) are used; the ID scheme needs widening")
        return None
    next_torrent_id = format_torrent_id(next_sequence)
    print(f"Next torrent ID: {next_torrent_id}")
    return next_torrent_id


if __name__ == "__main__":
    main()
