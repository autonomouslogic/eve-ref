"""
workflow: settings and completion files shared by the EVE Ref Deep Archive step scripts.

One workflow makes one torrent, from preflight to check-site, in SCRATCH_DIR. Every step writes a completion file,
SCRATCH_DIR/<NN>-<step>.done, as the last thing it does. A step refuses to start if its own completion file
exists (no accidental double execution), or if the previous step's doesn't. To redo a step on purpose, delete its
completion file, and every later one, by hand.

Output follows the roll-up scripts (scripts/rollup on the rollup-scripts branch): "# 🚀 <step>" once,
"## 🔵 <section>" headings, plain lines for results, "🔴 <message>" on stderr for errors (then exit 1), and "✅ ..."
as the last line on success. Everything a step prints, stdout and stderr, is also written to
SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log.

Settings come from the environment, then from deep-archive/torrents.env (or the file named by TORRENTS_ENV) for
anything the environment doesn't set:
    SCRATCH_DIR  work files for the current workflow; deleted by hand once the workflow is complete
    SEED_DIR     torrent content, one folder per torrent ID, kept for seeding (qBittorrent save path)
    RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY
                 the B2 key for the "everef" rclone remote; the rest of the remote is fixed here (RCLONE_FIXED)

The scripts reach the storage only through rclone (DATA_REMOTE), never through the AWS CLI or an S3 library.
"""

import datetime
import json
import os
import re
import subprocess
import sys

ENV_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..", "torrents.env")

# rclone is configured only from RCLONE_CONFIG_* env vars. This keeps the operator's own rclone.conf, and any remotes
# defined in it, out of the picture entirely. Only the account and key are secrets, so only those come from
# torrents.env; the rest of the "everef" remote is fixed here, and overrides the environment. hard_delete must never be
# true: deletes must stay soft, so the bucket lifecycle rules provide the undo window.
RCLONE_FIXED = {
    "RCLONE_CONFIG": "/dev/null",
    "RCLONE_CONFIG_EVEREF_TYPE": "b2",
    "RCLONE_CONFIG_EVEREF_HARD_DELETE": "false",
}

# The data site's root: the data.everef.net bucket on B2.
DATA_REMOTE = "everef:data-everef-net-425eb511"

# Datasets never archived (PLAN 2.2): paths from the site root. market-history and killmails work as databases, with
# past files modified in place, which a torrent can't follow.
TORRENT_EXCLUDE = ["market-history", "killmails"]

# Public URL of the data site.
DATA_BASE_URL = "https://data.everef.net/"

# The workflow's scripted steps, in order (README). Each step's script is named after its number and name, e.g.
# 01-find.py. Manual steps in between have no script and no completion file.
STEPS = [
    "preflight",
    "find",
    "load",
    "check-load",
    "make-checksums",
    "check-checksums",
    "make-torrent",
    "check-torrent",
    "seed",
    "check-seed",
    "test-download",
    "upload-torrent",
    "check-upload",
    "build-feed",
    "check-feed",
    "check-docs",
    "prepare-backup",
    "check-backup-plan",
    "backup",
    "check-backup",
    "check-before-delete",
    "delete",
    "check-delete",
    "check-site",
]

KIB = 1024
MIB = 1024 * KIB
GIB = 1024 * MIB
TIB = 1024 * GIB

# Size limits for one torrent (find chooses the selection by them; make-torrent uses PIECE_SIZE).

# Piece size the limits are worked out for.
PIECE_SIZE = 32 * MIB

# Most data in one torrent: what the seeder holds per torrent and what a full download is.
MAX_BYTES = 1 * TIB

# Most files in one torrent, worked out from a target .torrent size:
# - TARGET_TORRENT_BYTES: ~8 MB, under libtorrent's 10 MB default (max_buffer_size) with a 20% margin, so any
#   libtorrent client opens it unchanged, and below the 43,620 files (10.8 MiB) that hung qBittorrent's WebUI (PLAN
#   Appendix B).
# - PIECE_HASH_BYTES: piece hashes for MAX_BYTES of data, 20 bytes (v1) + 32 bytes (v2) per piece:
#   1 TiB / 32 MiB = 32,768 pieces * 52 bytes = 1,703,936 bytes.
# - BYTES_PER_FILE: measured 261 bytes per file for a hybrid torrent (Appendix B), rounded up to 300 for longer real
#   site paths.
# (8,000,000 - 1,703,936) / 300 = 20,986 files.
TARGET_TORRENT_BYTES = 8_000_000
PIECE_HASH_BYTES = (MAX_BYTES // PIECE_SIZE) * (20 + 32)
BYTES_PER_FILE = 300
MAX_FILES = (TARGET_TORRENT_BYTES - PIECE_HASH_BYTES) // BYTES_PER_FILE


def step_name(step):
    """The step with its number, e.g. 01-find: the name of its script (.py) and its completion file (.done)."""
    return f"{STEPS.index(step):02d}-{step}"


class _Tee:
    """Writes to a stream and a log file."""

    def __init__(self, stream, log_file):
        self.stream = stream
        self.log_file = log_file

    def write(self, text):
        self.stream.write(text)
        self.log_file.write(text)
        return len(text)

    def flush(self):
        self.stream.flush()
        self.log_file.flush()

    def isatty(self):
        return self.stream.isatty()


def start_log(scratch_dir, step, subject=None):
    """Tees stdout and stderr to SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log, and prints the step's header."""
    log_dir = os.path.join(scratch_dir, "log")
    os.makedirs(log_dir, exist_ok=True)
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    path = os.path.join(log_dir, f"{step_name(step)}-{stamp}.log")
    log_file = open(path, "a", encoding="utf-8", buffering=1)
    sys.stdout = _Tee(sys.stdout, log_file)
    sys.stderr = _Tee(sys.stderr, log_file)
    print(f"# 🚀 {step_name(step)}{': ' + subject if subject else ''}")
    return path


def section(title):
    sys.stdout.flush()
    print(f"## 🔵 {title}")


def error(message):
    """Reports a problem without stopping, for steps that list every problem before aborting."""
    sys.stdout.flush()
    print(f"🔴 {message}", file=sys.stderr)
    sys.stderr.flush()


def fail(message):
    error(message)
    sys.exit(1)


def ok(message):
    print(f"✅ {message}")


def load_env():
    """
    Reads torrents.env into os.environ, without overriding anything already set, then applies RCLONE_FIXED.
    Returns the file's path, or None if there isn't one.
    """
    path = os.path.realpath(os.environ.get("TORRENTS_ENV") or ENV_FILE)
    if not os.path.isfile(path):
        os.environ.update(RCLONE_FIXED)
        return None
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("export "):
                line = line[len("export ") :].lstrip()
            key, sep, value = line.partition("=")
            key = key.strip()
            if not sep or not key:
                fail(f"{path}:{number}: not KEY=VALUE")
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
                value = value[1:-1]
            os.environ.setdefault(key, value)
    os.environ.update(RCLONE_FIXED)
    return path


def required_dir(name):
    value = os.environ.get(name)
    if not value:
        fail(f"{name} is not set (environment or torrents.env)")
    path = os.path.realpath(value)
    if not os.path.isdir(path):
        fail(f"{name} does not exist: {path}")
    return path


def is_inside(path, parent):
    return path == parent or path.startswith(parent + os.sep)


class Workflow:
    """The current workflow's directories and completion files."""

    def __init__(self):
        self.env_file = load_env()
        self.scratch_dir = required_dir("SCRATCH_DIR")
        self.seed_dir = required_dir("SEED_DIR")
        if is_inside(self.scratch_dir, self.seed_dir) or is_inside(self.seed_dir, self.scratch_dir):
            fail("SCRATCH_DIR and SEED_DIR must be separate, neither inside the other")

    def done_path(self, step):
        return os.path.join(self.scratch_dir, f"{step_name(step)}.done")

    def read_done(self, step):
        """The completion record of a step, or None if it hasn't completed."""
        path = self.done_path(step)
        if not os.path.isfile(path):
            return None
        with open(path, encoding="utf-8") as f:
            return json.load(f)

    def begin(self, step):
        """
        Refuses to start a step that already completed, or whose previous step hasn't. Then starts the step's log and
        prints its header, with the torrent ID once find has chosen it.
        """
        if step not in STEPS:
            raise ValueError(f"unknown step: {step}")
        done = self.read_done(step)
        if done is not None:
            fail(
                f"{step_name(step)} already completed at {done.get('completed_at')}, refusing to rerun. "
                f"To redo it on purpose, delete {self.done_path(step)} and every later step's by hand"
            )
        index = STEPS.index(step)
        if index > 0:
            previous = STEPS[index - 1]
            if self.read_done(previous) is None:
                fail(f"Earlier step {step_name(previous)} has not completed yet ({self.done_path(previous)} missing)")
        find = self.read_done("find")
        start_log(self.scratch_dir, step, find["torrent_id"] if find else None)

    def complete(self, step, torrent_id, summary):
        """Writes the step's completion file. Call it last, only after the step succeeded."""
        record = {
            "step": step,
            "torrent_id": torrent_id,
            "completed_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "summary": summary,
        }
        path = self.done_path(step)
        tmp = path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            f.write(json.dumps(record, indent=2, sort_keys=True) + "\n")
        os.rename(tmp, path)
        return path

    def torrent_id(self):
        """The workflow's torrent ID, chosen by find."""
        done = self.read_done("find")
        if done is None:
            fail(f"{step_name('find')} hasn't completed: no torrent ID yet")
        return done["torrent_id"]


def join(base, path):
    return f"{base}/{path}" if path else base


def rclone(*args):
    """Runs rclone and returns its stdout. Fails with rclone's last error line."""
    try:
        result = subprocess.run(["rclone", *args], capture_output=True, text=True)
    except FileNotFoundError:
        fail("rclone is not installed")
    if result.returncode != 0:
        lines = [line for line in result.stderr.splitlines() if line.strip()]
        fail(f"rclone {args[0]} failed: {lines[-1] if lines else f'exit code {result.returncode}'}")
    return result.stdout


def lsjson(path, recursive=False):
    """
    Yields (path, size, mtime) for every file under an rclone path, streaming rclone lsjson's output (one entry per
    line). mtime is a POSIX timestamp: on B2, the upload time unless the uploader set src_last_modified_millis.
    """
    args = ["rclone", "lsjson", "--files-only", "--no-mimetype"]
    if recursive:
        args += ["--recursive", "--fast-list"]
    try:
        process = subprocess.Popen([*args, path], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    except FileNotFoundError:
        fail("rclone is not installed")
    for line in process.stdout:
        line = line.strip().rstrip(",")
        if not line or line in ("[", "]"):
            continue
        entry = json.loads(line)
        yield entry["Path"], entry["Size"], parse_modtime(entry["ModTime"])
    stderr = process.stderr.read()
    if process.wait() != 0:
        lines = [line for line in stderr.splitlines() if line.strip()]
        fail(f"rclone lsjson {path} failed: {lines[-1] if lines else f'exit code {process.returncode}'}")


MODTIME_RE = re.compile(r"^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)(?:\.(\d+))?(Z|[+-]\d\d:\d\d)$")


def parse_modtime(value):
    """rclone's ModTime (RFC 3339, up to nanoseconds) as a POSIX timestamp."""
    match = MODTIME_RE.match(value)
    if not match:
        raise ValueError(f"unexpected ModTime from rclone: {value!r}")
    seconds, fraction, zone = match.groups()
    zone = "+00:00" if zone == "Z" else zone
    parsed = datetime.datetime.fromisoformat(seconds + zone)
    return parsed.timestamp() + (int(fraction[:9].ljust(9, "0")) / 1e9 if fraction else 0)
