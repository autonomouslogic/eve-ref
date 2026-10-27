#!/usr/bin/env python3
"""
preflight: a step of the EVE Ref Deep Archive workflow. Basic checks before a workflow starts.

DRAFT. Read-only, apart from SCRATCH_DIR (its completion file and logs).

    00-preflight.py

Runs every check and reports each failure, then aborts if any failed; a missing tool aborts straight after the tool
checks, since the later checks need them. On success it writes
SCRATCH_DIR/00-preflight.done, which 01-find needs. It can't run twice in the same SCRATCH_DIR: the scratch
directory must be empty.

Checks:
- the settings are there: SCRATCH_DIR, SEED_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY
  (environment or torrents.env, see lib/workflow.py); settings the environment overrides are named (not a failure);
- SCRATCH_DIR exists (created, with its parents, if missing) and SEED_DIR exists, and both are writable: SCRATCH_DIR
  is all preflight writes to;
- SCRATCH_DIR is empty (log/ aside): one workflow per scratch directory;
- SEED_DIR has room for the largest possible torrent (MAX_BYTES);
- SCRATCH_DIR and SEED_DIR are separate;
- SEED_DIR/.staging/ is empty or doesn't exist: not an abandoned workflow's staged files, left outside any torrent in
  the merged tree (NOTES.md "Merged seed directory"); SEED_DIR/.torrents/ not existing is a warning, in case SEED_DIR
  is the wrong directory;
- the merged tree matches its signed torrents: every file on disk owned by exactly one torrent, no collisions once
  merged (lib/seed_dir.py's quick_check, check-seed-dir.py);
- rclone is installed;
- the data site's bucket is reachable (the site root's index.json), and deep-archive/ is listable; also picks the
  next torrent ID (one past the highest published one, or 00 if there are none), for 01-find to use;
- the other required tools: libtorrent 2.x, sha256sum (03-load uses it to verify the checksum file it just
  wrote), Docker (04-sign-sha256 and 06-sign-torrent run minisign through it);
- qBittorrent's Web API answers at QBITTORRENT_URL with QBITTORRENT_API_KEY, and runs libtorrent 2.x (a warning, not
  a failure, if it's older than 2.0.15: NOTES.md "libtorrent" lists the fixes since);
- at least MIN_WORKING_TRACKERS of the fixed trackers (lib/trackers.py's GOOD_TRACKERS) answer a BEP 15 connect
  request;
- LOCAL_COPY_DIR, if set, is a readable folder laid out like the bucket (index.json at the top), apart from
  SCRATCH_DIR and SEED_DIR.
"""

import glob
import json
import os
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
import seed_dir  # noqa: E402
from workflow import (  # noqa: E402
    DEEP_ARCHIVE_DIR,
    MAX_BYTES,
    MIN_LIBTORRENT_VERSION,
    STAGING_DIR_NAME,
    TORRENTS_DIR_NAME,
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
    parse_version,
    print_env_overrides,
    qbittorrent_save_path,
    qbittorrent_settings,
    section,
    start_log,
    torrent_sequence,
    warning,
)
from qbittorrent import Client, QbittorrentError  # noqa: E402
from trackers import GOOD_TRACKERS, MIN_WORKING_TRACKERS  # noqa: E402
from trackers import check_trackers as check_tracker_connectivity  # noqa: E402

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
    print_env_overrides()

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
    seed, _, seed_problem = prepare_dir("SEED_DIR", create=False)
    if seed is None:
        checks.fail(seed_problem)
    else:
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
    if seed:
        check_staging(checks, seed)
        check_seed_dir_inventory(checks, seed)

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
    if next_torrent_id and seed:
        next_torrent_id = after_local_torrents(checks, seed, next_torrent_id)

    if checks.failed:
        fail(f"Preflight found {checks.failed} problem{'s' if checks.failed != 1 else ''}, aborting")
    workflow = Workflow()
    workflow.complete(STEP, None, {"env_file": env_file, "next_torrent_id": next_torrent_id})
    ok("Preflight OK")


def prepare_dir(name, create=True):
    """
    (path, created, None) for the setting's directory, creating it and its parents if it doesn't exist and create is
    set; or (None, False, problem) if it isn't set, doesn't exist (create not set), can't be created, isn't a
    directory, or isn't writable.
    """
    value = os.environ.get(name)
    if not value:
        return None, False, f"{name} is not set (environment or torrents.env)"
    path = os.path.realpath(value)
    created = False
    if not os.path.exists(path):
        if not create:
            return None, False, f"{name} does not exist: {path}"
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
    print(f"LOCAL_COPY_DIR: {local}, load copies what it has, the bucket replaces any that don't match")


def check_staging(checks, seed):
    """
    SEED_DIR/.staging/ and SEED_DIR/.torrents/ (NOTES.md "Merged seed directory"), read-only: the later steps create
    them. A missing .torrents/ warns, in case SEED_DIR is the wrong directory. If .staging/ exists it must be empty --
    anything in it is an abandoned workflow's staged files, left outside any torrent in the merged tree, and must be
    resolved by hand before a new one starts.
    """
    staging = os.path.join(seed, STAGING_DIR_NAME)
    torrents = os.path.join(seed, TORRENTS_DIR_NAME)
    if not os.path.isdir(torrents):
        warning(f"SEED_DIR/{TORRENTS_DIR_NAME} not found in {seed}: is SEED_DIR the right directory?")
    for path, name in ((staging, STAGING_DIR_NAME), (torrents, TORRENTS_DIR_NAME)):
        if os.path.exists(path) and not os.path.isdir(path):
            checks.fail(f"SEED_DIR/{name} exists and isn't a directory: {path}")
            return
    entries = os.listdir(staging) if os.path.isdir(staging) else []
    if entries:
        checks.fail(
            f"SEED_DIR/{STAGING_DIR_NAME} isn't empty: {len(entries)} entries, e.g. {entries[0]} "
            "(an earlier workflow's staged files, left behind; resolve by hand)"
        )
    else:
        print(f"SEED_DIR/{STAGING_DIR_NAME}: empty")


def check_seed_dir_inventory(checks, seed):
    """
    The merged tree against its signed torrents (lib/seed_dir.py's quick_check, NOTES.md "Merged seed directory"):
    every file on disk owned by exactly one torrent, no collisions once merged. qBittorrent isn't checked here --
    check_qbittorrent above already did, for this run's own torrent, and check-seed-dir.py covers the rest.
    """
    torrents_dir = os.path.join(seed, TORRENTS_DIR_NAME)
    try:
        torrents, disk, problems = seed_dir.quick_check(seed, torrents_dir)
    except seed_dir.SeedDirError as e:
        checks.fail(str(e))
        return
    if problems:
        for problem in problems[:10]:
            error(f"  {problem}")
        checks.fail(f"SEED_DIR inventory: {len(problems)} problems (check-seed-dir.py for the full list)")
    else:
        print(f"SEED_DIR inventory: {len(torrents)} torrents, {len(disk)} files, all owned, no collisions")


def check_qbittorrent(checks, seed):
    """
    qBittorrent's Web API answers with the configured API key, and runs libtorrent 2.x (needed to seed v2, kept as a
    requirement even though make-torrent now makes v1-only torrents: a v1+v2 torrent made by hand, or by an earlier
    workflow, must still seed). A libtorrent older than MIN_LIBTORRENT_VERSION is a warning, not a failure: it seeds
    fine, but misses fixes that matter to re-seeding and rechecking (NOTES.md "libtorrent").
    """
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
    parsed = parse_version(libtorrent)
    if not parsed or parsed[0] < 2:
        checks.fail(f"qBittorrent must run libtorrent 2.x to seed hybrid v1+v2 torrents, found {libtorrent!r}")
    elif parsed < MIN_LIBTORRENT_VERSION:
        warning(
            f"qBittorrent's libtorrent {libtorrent} is older than "
            f"{'.'.join(str(p) for p in MIN_LIBTORRENT_VERSION)}: missing fixes that matter to re-seeding and "
            "rechecking (NOTES.md \"libtorrent\"). Not a failure, but worth upgrading"
        )
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
            checks.fail(f"libtorrent must be 2.x (make-torrent uses its v1_only and modification_time flags), "
                        f"found {version}")
        else:
            print(f"libtorrent {version}")
    except ImportError:
        checks.fail("Missing required tool: libtorrent Python bindings")
    if shutil.which("sha256sum") is None:
        checks.fail("Missing required tool: sha256sum")
    else:
        print("sha256sum found")
    docker = subprocess.run(["docker", "version"], capture_output=True, text=True) if shutil.which("docker") else None
    if docker is None or docker.returncode != 0:
        checks.fail("Missing required tool: Docker (04-sign-sha256 and 06-sign-torrent run minisign through it)")
    else:
        print("Docker found")


def check_trackers(checks):
    """
    GOOD_TRACKERS (lib/trackers.py), the fixed list make-torrent uses, answer a BEP 15 connect request: at least
    MIN_WORKING_TRACKERS of them, or this fails (the torrent would still be made with a mostly-dead tracker list).
    """
    results = check_tracker_connectivity(GOOD_TRACKERS)
    working = 0
    for url, reachable, reason in results:
        if reachable is None:
            print(f"{url}: not checked ({reason})")
        elif reachable:
            working += 1
            print(f"{url}: OK")
        else:
            print(f"{url}: not reachable ({reason})")
    if working < MIN_WORKING_TRACKERS:
        checks.fail(
            f"Only {working} of {len(GOOD_TRACKERS)} trackers answered, need at least {MIN_WORKING_TRACKERS}: "
            "review lib/trackers.py's GOOD_TRACKERS"
        )
    else:
        print(f"{working} of {len(GOOD_TRACKERS)} trackers answered (at least {MIN_WORKING_TRACKERS} needed)")


def after_local_torrents(checks, seed, next_torrent_id):
    """
    next_torrent_id, or the ID after the newest <ID>.torrent in SEED_DIR/.torrents/ if that's later: a torrent made
    here but not published yet (or not for good) doesn't show in the bucket, and its ID must not be reused.
    """
    torrents_dir = os.path.join(seed, TORRENTS_DIR_NAME)
    names = glob.glob(os.path.join(torrents_dir, "*.torrent"))
    sequences = [torrent_sequence(os.path.basename(n)[: -len(".torrent")]) for n in names]
    sequences = [s for s in sequences if s is not None]
    if not sequences or max(sequences) + 1 <= torrent_sequence(next_torrent_id):
        return next_torrent_id
    if max(sequences) + 1 > 99:
        checks.fail("All double-digit torrent IDs (00-99) are used; the ID scheme needs widening")
        return None
    later = format_torrent_id(max(sequences) + 1)
    newest = format_torrent_id(max(sequences))
    print(f"{len(sequences)} local torrents in {torrents_dir}, newest {newest}: next torrent ID {later}")
    return later


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
