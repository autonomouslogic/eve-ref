"""
workflow: settings and completion files shared by the EVE Ref Deep Archive step scripts.

One workflow makes one torrent, from preflight to check-site, in SCRATCH_DIR. Every step writes a completion file,
SCRATCH_DIR/<NN>-<step>.done, as the last thing it does. A step refuses to start if its own completion file
exists (no accidental double execution), or if the previous step's doesn't. To redo a step on purpose, delete its
completion file, and every later one, by hand.

Output follows the roll-up scripts (scripts/rollup on the rollup-scripts branch): "🚀 <step>" once,
"🔵 <section>" headings, plain lines for results, "🟡 <message>" on stderr for problems the step carries on past,
"🔴 <message>" on stderr for errors (always followed by exit 1), and "✅ ..." as the last line on success. Everything a step prints, stdout and stderr, is also written to
SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log.

Settings come from the environment, then from deep-archive/torrents.env (or the file named by TORRENTS_ENV) for
anything the environment doesn't set:
    SCRATCH_DIR  work files for the current workflow; deleted by hand once the workflow is complete
    SEED_DIR     torrent content, one folder per torrent ID, kept for seeding (qBittorrent save path)
    RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY
                 the B2 key for the "everef" rclone remote; the rest of the remote is fixed here, and every other
                 RCLONE_* setting is dropped (RCLONE_FIXED)
    QBITTORRENT_URL, QBITTORRENT_API_KEY
                 qBittorrent's Web API and its API key (qBittorrent 5.2+, qbittorrent_settings)
    QBITTORRENT_SAVE_PATH  optional: SEED_DIR as qBittorrent sees it, if different (qbittorrent_save_path)
    DATA_BUCKET  the data site's bucket on B2; its root is the site root (data_remote)
    LOCAL_COPY_DIR  optional: a local copy of the data site, same layout as the bucket (local_copy_dir); load copies
                 what it has, unchecked, then the bucket copy replaces any that don't match (a shortcut, not trusted)
    MINISIGN_KEY_PATH  the minisign private key, for the bash signing steps (lib/sign.sh)

The scripts reach the data bucket only through rclone (data_remote).
"""

import datetime
import json
import os
import re
import subprocess
import sys
import threading

ENV_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..", "torrents.env")

# rclone is configured only from these env vars. RCLONE_CONFIG=/dev/null keeps the operator's own rclone.conf, and any
# remotes defined in it, out of the picture entirely, and load_env drops every other RCLONE_* variable but the B2 key
# (RCLONE_ALLOWED), from the environment or torrents.env: a filter or flag left in the shell (RCLONE_EXCLUDE,
# RCLONE_DRY_RUN, ...) would change what a listing or a delete sees. The rest of the "everef" remote is fixed here.
# hard_delete must never be true: deletes must stay soft, so the bucket lifecycle rules provide the undo window.
RCLONE_FIXED = {
    "RCLONE_CONFIG": "/dev/null",
    "RCLONE_CONFIG_EVEREF_TYPE": "b2",
    "RCLONE_CONFIG_EVEREF_HARD_DELETE": "false",
}
RCLONE_ALLOWED = {"RCLONE_CONFIG_EVEREF_ACCOUNT", "RCLONE_CONFIG_EVEREF_KEY"}

# The rclone remote for the data site's bucket; the bucket itself is the DATA_BUCKET setting.
REMOTE_NAME = "everef"

# The site folder holding the published torrents and their file lists (NOTES.md "Published files").
DEEP_ARCHIVE_DIR = "deep-archive"

# Folders never archived (NOTES.md "Never archived"): paths from the site root. market-history and killmails work
# as databases, with past files modified in place, which a torrent can't follow. deep-archive holds the torrents themselves.
# ccp (CCP's own data: SDE, MER, image collections, CSM results, ...) has to stay excluded for now: the docs link its
# old files and folders directly (sde.md links ccp/sde/older/, the datasets index links ccp/ccp_quant/, ccp/iec/ and
# ccp/csm/), and those links would break once a torrent took them. Revisit later, together with those docs.
TORRENT_EXCLUDE = ["market-history", "killmails", "ccp", DEEP_ARCHIVE_DIR]

# Optional allowlist: paths from the site root. None selects every folder not in TORRENT_EXCLUDE; a list selects only
# files in these folders (TORRENT_EXCLUDE still applies within them). Used to give the first torrents only the biggest
# datasets, to get them out of the way. Set it back to None once they're archived.
TORRENT_INCLUDE = None


def in_folders(path, folders):
    """Whether the path is one of the folders or inside one, by whole folder name (ccp-other isn't in ccp)."""
    return any(path == f or path.startswith(f + "/") for f in folders)

# A path's year folder, for classify_path: the first 4-digit 19xx/20xx directory.
YEAR_DIR_RE = re.compile(r"^(19|20)\d{2}$")

# Dataset folder suffixes stripped by classify_path: "history" (e.g. market-orders/history) and "backfills"
# (e.g. fuzzwork/ordersets/backfills) aren't their own datasets.
DATASET_SUFFIXES = {"history", "backfills"}

# Files modified less than this many years before today are never selected (01-find).
MIN_AGE_YEARS = 2

# Public URL of the data site.
DATA_BASE_URL = "https://data.everef.net/"

# Torrent IDs: everef-eve-online-deep-archive-<two-digit sequence number>, starting at 00. Sequential, not
# date-based: 00-preflight picks the next one from the published torrents' IDs.
TORRENT_ID_PREFIX = "everef-eve-online-deep-archive-"
TORRENT_ID_RE = re.compile(r"^everef-eve-online-deep-archive-(\d{2})$")


def format_torrent_id(sequence):
    """The torrent ID for a sequence number, e.g. 0 -> everef-eve-online-deep-archive-00."""
    return f"{TORRENT_ID_PREFIX}{sequence:02d}"


def torrent_sequence(torrent_id):
    """The sequence number in a torrent ID (TORRENT_ID_RE), or None if it doesn't match."""
    m = TORRENT_ID_RE.match(torrent_id)
    return int(m[1]) if m else None


def root_file_names(torrent_id):
    """
    The files at the root of a torrent, next to the data: its README, checksum file and the checksum file's
    signature. Not data files: never in the manifest, the .txt file list or the delete set.
    """
    return {f"{torrent_id}-README.md", f"{torrent_id}.sha256", f"{torrent_id}.sha256.minisig"}


# Cache-Control max-age for everef-eve-online-deep-archive.xml: short, like DataIndex's index pages
# (Configs.DATA_INDEX_CACHE_CONTROL_MAX_AGE, 2 minutes), so a new torrent shows up almost at once.
FEED_CACHE_MAX_AGE_SECONDS = 120

# A torrent's comment, built by render_comment and read back by 09-publish's read_torrent. {sequence} is
# the torrent ID's two-digit sequence number, e.g. 00. The template file itself must be a single line
# (load_comment_template): the comment is kept deliberately short.
COMMENT_TEMPLATE_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)), "comment-template.txt")


def load_comment_template():
    """comment-template.txt, trimmed. Fails if what's left has more than one line."""
    with open(COMMENT_TEMPLATE_PATH, encoding="utf-8") as f:
        text = f.read().strip()
    if "\n" in text:
        fail(f"{COMMENT_TEMPLATE_PATH} must be a single line, found {text.count(chr(10)) + 1}")
    return text

# The workflow's steps, in order (README), numbered by their place here. Each scripted step's script is named after
# its number and name, e.g. 01-find.py; the signing steps are bash (04-sign-sha256.sh, 06-sign-torrent.sh, through
# lib/sign.sh), with the same completion files and rules. Manual steps (MANUAL_STEPS) are numbered too, but have no
# script and no completion file: a script waits for the nearest earlier scripted step.
STEPS = [
    "preflight",
    "find",
    "review",
    "load",
    "sign-sha256",
    "make-torrent",
    "sign-torrent",
    "seed",
    "check-seed",
    "publish",
    "update-docs",
    "index-deep-archive",
    "check-before-delete",
    "delete",
    "check-delete",
    "data-index",
    "check-site",
    "delete-scratch",
]
MANUAL_STEPS = {
    "review",
    "update-docs",
    "index-deep-archive",
    "data-index",
    "delete-scratch",
}

KIB = 1024
MIB = 1024 * KIB
GIB = 1024 * MIB
TIB = 1024 * GIB


def fmt_count(n):
    return f"{n:,}"


def fmt_duration(seconds):
    minutes, seconds = divmod(int(seconds), 60)
    hours, minutes = divmod(minutes, 60)
    if hours:
        return f"{hours}h {minutes}m {seconds}s"
    if minutes:
        return f"{minutes}m {seconds}s"
    return f"{seconds}s"


def fmt_bytes(n):
    for unit, factor in (("TiB", TIB), ("GiB", GIB), ("MiB", MIB), ("KiB", KIB)):
        if n >= factor:
            return f"{n / factor:.1f} {unit}"
    return f"{n} B"


def fmt_size(n):
    """Bytes with the exact count, for .torrent sizes: limits are in decimal bytes, fmt_bytes rounds to binary units."""
    return f"{fmt_bytes(n)} ({fmt_count(n)} bytes)"


# Size limits for one torrent (find chooses the selection by them; make-torrent uses PIECE_SIZE).

# Piece size the limits are worked out for.
PIECE_SIZE = 32 * MIB

# Data in one torrent, a soft limit: 01-find stops at the first date that reaches it, and takes that whole date, so a
# torrent can be somewhat over. Roughly what the seeder holds per torrent and what a full download is.
MAX_BYTES = 990 * GIB

# Least data in one torrent: 01-find aborts if its selection (index.json included) is smaller, so no workflow makes a
# small torrent. Rerun it once more data has passed MIN_AGE_YEARS.
MIN_BYTES = 900 * GIB

# Largest .torrent, as estimated from the selection's paths and sizes (lib/torrent_size.py): libtorrent's default
# max_buffer_size, so any libtorrent client opens it unchanged. The estimate runs up to about 12 KB high (its
# FIXED_BYTES allowance), so a .torrent at the limit still loads. This is what limits the file count. A hard limit:
# 01-find leaves out the date that would go over it.
MAX_TORRENT_BYTES = 10_000_000

# qBittorrent's Web API (00-preflight, 07-seed, 08-check-seed, 12-check-before-delete). Torrents are added with save path
# QBITTORRENT_SAVE_PATH (qbittorrent_save_path), SEED_DIR as qBittorrent sees it.
def qbittorrent_settings():
    """(url, api_key) for qBittorrent's Web API (QBITTORRENT_* settings, environment or torrents.env)."""
    url = os.environ.get("QBITTORRENT_URL", "").strip().rstrip("/")
    if not url:
        fail("QBITTORRENT_URL is not set (environment or torrents.env)")
    return url, os.environ.get("QBITTORRENT_API_KEY", "").strip()


def qbittorrent_save_path(seed_dir):
    """
    The save path given to qBittorrent: QBITTORRENT_SAVE_PATH, or SEED_DIR if it isn't set. The same folder as SEED_DIR,
    as qBittorrent sees it (e.g. its mount point when it runs in Docker), so it isn't resolved or checked here.
    """
    return os.environ.get("QBITTORRENT_SAVE_PATH", "").strip() or seed_dir


def step_name(step):
    """
    The step with its number, e.g. 01-find: the name of its script (.py) and its completion file (.done).
    """
    return f"{STEPS.index(step):02d}-{step}"


def previous_scripted_step(step):
    """The nearest scripted step before this one, skipping manual steps; None for the first step."""
    earlier = [s for s in STEPS[: STEPS.index(step)] if s not in MANUAL_STEPS]
    return earlier[-1] if earlier else None


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
    print(f"🚀 {step_name(step)}{': ' + subject if subject else ''}")
    return path


def section(title):
    sys.stdout.flush()
    print(f"🔵 {title}")


def error(message):
    """
    Reports a problem that stops the step, without stopping yet: for steps that list every problem before aborting.
    Always followed by fail. A problem the step carries on past is a warning.
    """
    sys.stdout.flush()
    print(f"🔴 {message}", file=sys.stderr)
    sys.stderr.flush()


def warning(message):
    """Reports a problem the step carries on past (e.g. a file downloaded instead of copied from the local copy)."""
    sys.stdout.flush()
    print(f"🟡 {message}", file=sys.stderr)
    sys.stderr.flush()


def fail(message):
    error(message)
    sys.exit(1)


def ok(message):
    print(f"✅ {message}")


# Settings in torrents.env that the environment already set to something else, filled in by load_env. Every step
# names them (print_env_overrides), so a stale shell export doesn't silently override the file.
ENV_OVERRIDES = []

# RCLONE_* settings load_env dropped (see RCLONE_FIXED), named by every step like ENV_OVERRIDES.
RCLONE_IGNORED = []


def print_env_overrides():
    """
    Names the settings the environment overrides torrents.env with (ENV_OVERRIDES), and the RCLONE_* settings dropped
    (RCLONE_IGNORED): names only, some are secrets.
    """
    if ENV_OVERRIDES:
        print(f"Note: set in the environment, overriding torrents.env: {', '.join(sorted(ENV_OVERRIDES))}")
    if RCLONE_IGNORED:
        print(f"Note: ignored, rclone only takes the B2 key from the settings: {', '.join(RCLONE_IGNORED)}")


def _dropped_rclone_setting(key):
    return key.startswith("RCLONE_") and key not in RCLONE_ALLOWED


def _apply_rclone_settings():
    """Drops every RCLONE_* setting but RCLONE_ALLOWED, naming them in RCLONE_IGNORED, then applies RCLONE_FIXED."""
    RCLONE_IGNORED.clear()
    for key in sorted(os.environ):
        if _dropped_rclone_setting(key):
            if os.environ[key] != RCLONE_FIXED.get(key):
                RCLONE_IGNORED.append(key)
            del os.environ[key]
    os.environ.update(RCLONE_FIXED)


def load_env():
    """
    Reads torrents.env into os.environ, without overriding anything already set, then drops every RCLONE_* setting but
    the B2 key and applies RCLONE_FIXED. Returns the file's path, or None if there isn't one. Settings the environment
    overrides go in ENV_OVERRIDES, the RCLONE_* settings dropped in RCLONE_IGNORED.
    """
    path = os.path.realpath(os.environ.get("TORRENTS_ENV") or ENV_FILE)
    ENV_OVERRIDES.clear()
    if not os.path.isfile(path):
        _apply_rclone_settings()
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
            if key in os.environ and os.environ[key] != value and not _dropped_rclone_setting(key):
                ENV_OVERRIDES.append(key)
            os.environ.setdefault(key, value)
    _apply_rclone_settings()
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


def lock(scratch_dir):
    """
    Takes SCRATCH_DIR/log/.lock for as long as the process runs, so only one step runs at a time: two steps started
    by mistake could otherwise both pass the completion-file checks. In log/, which 00-preflight allows in an
    otherwise empty SCRATCH_DIR.
    """
    import fcntl

    log_dir = os.path.join(scratch_dir, "log")
    os.makedirs(log_dir, exist_ok=True)
    path = os.path.join(log_dir, ".lock")
    lock_file = open(path, "w")
    try:
        fcntl.flock(lock_file, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        fail(f"Another step is running in this SCRATCH_DIR ({path} is locked)")
    _LOCKS.append(lock_file)


# Open lock files, kept so they stay locked until the process exits.
_LOCKS = []


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

    def begin(self, step, dry_run=False):
        """
        Takes the lock, and refuses to start a step that already completed, or whose previous step hasn't. Then starts
        the step's log and prints its header, with the torrent ID once find has chosen it, and names any setting the
        environment overrides torrents.env with. dry_run only marks the header: the checks are the same.
        """
        if step not in STEPS or step in MANUAL_STEPS:
            raise ValueError(f"unknown scripted step: {step}")
        lock(self.scratch_dir)
        done = self.read_done(step)
        if done is not None:
            fail(
                f"{step_name(step)} already completed at {done.get('completed_at')}, refusing to rerun. "
                f"To redo it on purpose, delete {self.done_path(step)} and every later step's by hand"
            )
        previous = previous_scripted_step(step)
        if previous is not None:
            if self.read_done(previous) is None:
                fail(f"Earlier step {step_name(previous)} has not completed yet ({self.done_path(previous)} missing)")
        find = self.read_done("find")
        subject = find["torrent_id"] if find else None
        if dry_run:
            subject = f"{subject} (dry run)" if subject else "(dry run)"
        start_log(self.scratch_dir, step, subject)
        print_env_overrides()

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


def local_copy_dir():
    """
    LOCAL_COPY_DIR, resolved: a local copy of the data site, laid out like the bucket (index.json at the top). None if
    it isn't set.
    """
    value = os.environ.get("LOCAL_COPY_DIR", "").strip()
    return os.path.realpath(value) if value else None


def data_remote():
    """The rclone path of the data site's root: the "everef" remote and DATA_BUCKET. Call after load_env."""
    bucket = os.environ.get("DATA_BUCKET", "").strip().rstrip("/")
    if not bucket:
        fail("DATA_BUCKET is not set (environment or torrents.env)")
    return f"{REMOTE_NAME}:{bucket}"


def file_list_problems(text):
    """
    Problems with a <ID>.txt file list, as "line N: ..." strings; empty if it's well-formed. The format:
    UTF-8, "\\n" line endings, one data file per line as its full path from the site root (the same path inside the
    torrent, without the torrent's root folder), sorted by code point, no duplicates. The torrent's root files
    (root_file_names) aren't listed.
    """
    if not text:
        return ["empty file"]
    problems = []
    if "\r" in text:
        problems.append("contains \\r: line endings must be \\n")
    if text and not text.endswith("\n"):
        problems.append("doesn't end with \\n")
    lines = text.split("\n")
    if text.endswith("\n"):
        lines.pop()
    previous = None
    for number, line in enumerate(lines, 1):
        line = line.rstrip("\r")  # reported once above
        problem = site_path_problem(line)
        if problem is None and previous is not None and line <= previous:
            problem = "duplicate" if line == previous else f"not sorted (after {previous!r})"
        if problem is not None:
            problems.append(f"line {number}: {problem}: {line!r}")
        previous = line
    return problems


def site_path_problem(path):
    """Why a path isn't a data file's full path from the site root, or None."""
    if not path:
        return "empty line"
    if path != path.strip():
        return "leading or trailing whitespace"
    if path.startswith("/"):
        return "starts with /"
    if "\\" in path:
        return "contains a backslash"
    parts = path.split("/")
    if any(part in ("", ".", "..") for part in parts):
        return "empty, . or .. path segment"
    if len(parts) == 1:
        return "at the site root: data files never are, and the torrent's root files aren't listed"
    if parts[0].startswith(TORRENT_ID_PREFIX):
        return "starts with a torrent ID: the torrent's root folder isn't part of the path"
    if parts[0] == DEEP_ARCHIVE_DIR:
        return f"under {DEEP_ARCHIVE_DIR}/"
    return None


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


def rclone_run(*args, check=True):
    """
    Runs rclone for a long transfer, printing its output (progress, retries, errors) as it comes. Fails with its last
    output line if it exits with an error; with check=False, returns the exit code instead.
    """
    try:
        process = subprocess.Popen(["rclone", *args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    except FileNotFoundError:
        fail("rclone is not installed")
    last = None
    for line in process.stdout:
        line = line.rstrip("\n")
        if line.strip():
            print(f"rclone: {line}", flush=True)
            last = line
    process.wait()
    if not check:
        return process.returncode
    if process.returncode != 0:
        fail(f"rclone {args[0]} failed: {last or f'exit code {process.returncode}'}")


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
    # Drain stderr while stdout is read: if it's only read at the end, rclone blocks once the pipe buffer fills (e.g.
    # with retry messages on a long listing) and the listing hangs. Printing it also shows retries as they happen.
    errors = []
    drain = threading.Thread(target=_drain_stderr, args=(process.stderr, errors), daemon=True)
    drain.start()
    for line in process.stdout:
        line = line.strip().rstrip(",")
        if not line or line in ("[", "]"):
            continue
        entry = json.loads(line)
        yield entry["Path"], entry["Size"], parse_modtime(entry["ModTime"])
    process.wait()
    drain.join()
    if process.returncode != 0:
        fail(f"rclone lsjson {path} failed: {errors[-1] if errors else f'exit code {process.returncode}'}")


def _drain_stderr(stream, errors):
    """Prints rclone's stderr line by line, and keeps the last non-empty line in errors."""
    for line in stream:
        line = line.rstrip("\n")
        if line.strip():
            print(f"rclone: {line}", flush=True)
            errors[:] = [line]


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


def iso(timestamp):
    """A POSIX timestamp as files.jsonl/manifest.json record it: YYYY-MM-DDTHH:MM:SSZ, whole seconds, UTC."""
    return datetime.datetime.fromtimestamp(timestamp, datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_iso(value):
    """The inverse of iso(): YYYY-MM-DDTHH:MM:SSZ as a POSIX timestamp."""
    parsed = datetime.datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")
    return int(parsed.replace(tzinfo=datetime.timezone.utc).timestamp())


def classify_path(path):
    """
    (year, dataset) for a site path (file name included, as files.jsonl and the manifest record it):
    - year: the path's first YYYY directory (YEAR_DIR_RE), as an int; None if there isn't one. Callers that need a
      fallback (01-find's "(no year folder)" flag) check for None themselves.
    - dataset: the directories before that year folder, "/"-joined, with a trailing "history" or "backfills"
      (DATASET_SUFFIXES) removed; the first three directories if there's no year folder. "" at the site root (never
      reached: root-level files are skipped before classification) or directly under a year folder.
    Used by 01-find's and 12-check-before-delete's dataset breakdowns.
    """
    dirs = path.split("/")[:-1]
    year_index = next((i for i, d in enumerate(dirs) if YEAR_DIR_RE.match(d)), None)
    if year_index is None:
        year, dataset_parts = None, dirs[:3]
    else:
        year, dataset_parts = int(dirs[year_index]), dirs[:year_index]
    while len(dataset_parts) > 1 and dataset_parts[-1] in DATASET_SUFFIXES:
        dataset_parts = dataset_parts[:-1]
    return year, "/".join(dataset_parts)


# The minisign public key published at https://everef.net/.well-known/minisign.pub, from this checkout.
MINISIGN_PUBLIC_KEY = os.path.join(
    os.path.dirname(os.path.realpath(__file__)), "..", "..", "..", "ui", "public", ".well-known", "minisign.pub"
)

# minisign's Docker image, pinned by digest (latest as of 2026-10-26), the same as lib/sign.sh's for 04-sign-sha256.sh
# and 06-sign-torrent.sh: a swapped image could pass any signature here. Always run with no network.
MINISIGN_IMAGE = "jedisct1/minisign@sha256:ee60186c5d05e342e66cbc81133aa744d026752d909d8ea021a15c4d16b1193b"


def verify_signature(path):
    """
    Verifies path against path.minisig with minisign, run through Docker, and MINISIGN_PUBLIC_KEY. Fails if the file,
    its signature or the key is missing, or the signature doesn't verify (e.g. it was left by an earlier run, before
    the file was remade). For 05-make-torrent (<ID>.sha256) and 09-publish (<ID>.torrent), before they use a signature.
    """
    signature = path + ".minisig"
    name = os.path.basename(path)
    if not os.path.isfile(path):
        fail(f"Not found: {path}")
    if not os.path.isfile(signature):
        fail(f"Not found: {signature}: sign {name} first")
    if not os.path.isfile(MINISIGN_PUBLIC_KEY):
        fail(f"Public key not found: {MINISIGN_PUBLIC_KEY}")
    folder = os.path.dirname(os.path.realpath(path))
    key_dir, key_name = os.path.split(os.path.realpath(MINISIGN_PUBLIC_KEY))
    args = [
        "docker",
        "run",
        "--rm",
        "--network",
        "none",
        "-v",
        f"{folder}:/data:ro",
        "-v",
        f"{key_dir}:/pub:ro",
        MINISIGN_IMAGE,
        "-V",
        "-p",
        f"/pub/{key_name}",
        "-m",
        f"/data/{name}",
        "-x",
        f"/data/{name}.minisig",
    ]
    try:
        result = subprocess.run(args, capture_output=True, text=True)
    except FileNotFoundError:
        fail("Docker is not installed")
    output = [line for line in (result.stdout + result.stderr).splitlines() if line.strip()]
    if result.returncode != 0:
        fail(f"{signature} doesn't verify: {output[-1] if output else f'exit code {result.returncode}'}")
    print(f"Signature verified: {signature}, against {os.path.realpath(MINISIGN_PUBLIC_KEY)}")
    for line in output:
        print(f"  {line}")


def render_comment(torrent_id):
    """The torrent's comment: comment-template.txt with {sequence} (the ID's two-digit number) filled in."""
    sequence = torrent_sequence(torrent_id)
    if sequence is None:
        fail(f"Not a torrent ID: {torrent_id}")
    return load_comment_template().format(sequence=f"{sequence:02d}")


def read_listing_gz(path):
    """The paths in a gzip jsonl listing written by 01-find's write_listing (find/listing.jsonl.gz)."""
    import gzip

    paths = set()
    with gzip.open(path, "rt", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                paths.add(json.loads(line)["path"])
    return paths
