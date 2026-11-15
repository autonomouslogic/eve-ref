"""
purge: what the purge scripts (d00-preflight, d01-find, d03-dryrun, d04-delete) share -- the rules for what may be
deleted from data.everef.net, and the workflow plumbing around them (README "Purge", NOTES.md "Purging").

A file on the site may be deleted only if every one of these holds (blockers returns what stops it):
- it is a data file of a signed torrent in SEED_DIR/.torrents/ (never a root file, an index file, or anything under
  deep-archive/), at that path in exactly one torrent;
- the site has it right now with the size and modification time the torrent records: the archived version, not a
  newer upload to the same path;
- it isn't under NEVER_PURGE;
- its date (lib/dating.py, the same code 01-find selects by) is before the start of the previous year;
- SEED_DIR holds a regular file at its path, with the torrent's size and modification time.
Anything else stays. Every script applies these rules to fresh data, so none trusts an earlier one's list.

The purge keeps its own completion files, d<NN>-<step>.done, in PURGE_DIR, next to the torrent workflow's
SCRATCH_DIR rather than in it (that one is wiped after every torrent), with the same rules as lib/workflow.py: a step
refuses to rerun, or to start before the previous scripted step completed.
"""

import contextlib
import datetime
import hashlib
import json
import os
import stat
import subprocess
import tempfile
from collections import namedtuple

import seed_dir
from dating import file_date
from workflow import (
    DEEP_ARCHIVE_DIR,
    TORRENTS_DIR_NAME,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    iso,
    is_inside,
    join,
    load_env,
    lock,
    lsjson,
    match_paths,
    print_env_overrides,
    rclone,
    rclone_run,
    required_dir,
    root_file_names,
    section,
    site_path_problem,
    start_log,
)

LOG_SAMPLE = 20

PURGE_STEPS = ["preflight", "find", "review", "dryrun", "delete"]
PURGE_MANUAL_STEPS = {"review"}

# Never deleted from the site, whatever a torrent holds (glob patterns from the site root, workflow.match_paths).
# ccp/csm and ccp/mer: the syncs wouldn't re-upload them (NOTES.md "Never archived"). The rest are never in a torrent
# anyway: this is a second line of defence.
NEVER_PURGE = ["ccp/csm", "ccp/mer", DEEP_ARCHIVE_DIR, "market-history", "killmails"]

# Reasons a file stays (blockers).
NOT_ARCHIVED = "not in a torrent"
BAD_PATH = "not a data file path"
PROTECTED = "never purged"
GONE = "not on the site"
CHANGED = "not the archived version"
RECENT = "this or the previous year"
NO_SEED_COPY = "no matching copy in SEED_DIR"

ArchivedFile = namedtuple("ArchivedFile", "torrent_id size mtime")


def purge_step_name(step):
    """The step with its number, e.g. d01-find: the name of its script (.py) and its completion file (.done)."""
    return f"d{PURGE_STEPS.index(step):02d}-{step}"


def previous_purge_step(step):
    """The nearest scripted step before this one, skipping manual steps; None for the first step."""
    earlier = [s for s in PURGE_STEPS[: PURGE_STEPS.index(step)] if s not in PURGE_MANUAL_STEPS]
    return earlier[-1] if earlier else None


class PurgeWorkflow:
    """The purge's directories and completion files."""

    def __init__(self):
        self.env_file = load_env()
        self.purge_dir = required_dir("PURGE_DIR")
        self.seed_dir = required_dir("SEED_DIR")
        if is_inside(self.purge_dir, self.seed_dir) or is_inside(self.seed_dir, self.purge_dir):
            fail("PURGE_DIR and SEED_DIR must be separate, neither inside the other")

    def done_path(self, step):
        return os.path.join(self.purge_dir, f"{purge_step_name(step)}.done")

    def read_done(self, step):
        """The completion record of a step, or None if it hasn't completed."""
        path = self.done_path(step)
        if not os.path.isfile(path):
            return None
        with open(path, encoding="utf-8") as f:
            return json.load(f)

    def begin(self, step):
        """Takes the lock, refuses a step that completed or whose previous step hasn't, and starts the log."""
        if step not in PURGE_STEPS or step in PURGE_MANUAL_STEPS:
            raise ValueError(f"unknown scripted purge step: {step}")
        lock(self.purge_dir)
        done = self.read_done(step)
        if done is not None:
            fail(
                f"{purge_step_name(step)} already completed at {done.get('completed_at')}, refusing to rerun. "
                f"To redo it on purpose, delete {self.done_path(step)} and every later step's by hand"
            )
        previous = previous_purge_step(step)
        if previous is not None and self.read_done(previous) is None:
            fail(f"Earlier step {purge_step_name(previous)} has not completed yet ({self.done_path(previous)} missing)")
        start_log(self.purge_dir, step, label=purge_step_name(step))
        print_env_overrides()

    def complete(self, step, summary):
        """Writes the step's completion file. Call it last, only after the step succeeded."""
        record = {
            "step": step,
            "completed_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "summary": summary,
        }
        path = self.done_path(step)
        write_text(path, json.dumps(record, indent=2, sort_keys=True) + "\n")
        return path

    def torrents_dir(self):
        return os.path.join(self.seed_dir, TORRENTS_DIR_NAME)


def write_text(path, text):
    """Writes the file whole, or not at all."""
    with open(path + ".tmp", "w", encoding="utf-8") as f:
        f.write(text)
    os.rename(path + ".tmp", path)


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def read_path_list(path):
    """The paths in a one-per-line list file, in order. Fails on a blank line or a duplicate."""
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    seen = set()
    for number, line in enumerate(lines, 1):
        if not line.strip():
            fail(f"{path}:{number}: blank line")
        if line in seen:
            fail(f"{path}:{number}: duplicate path {line!r}")
        seen.add(line)
    return lines


def keep_from_year(today):
    """The first year whose files are always kept: this year's and the previous year's are."""
    return today.year - 1


def modified_date(mtime):
    return datetime.datetime.fromtimestamp(mtime, datetime.timezone.utc).date()


def archived_data_files(torrents):
    """
    ({site path: ArchivedFile}, problems) for every data file in the torrents ({torrent_id: {"files": {path: (size,
    mtime)}, ...}}, seed_dir.load_torrents): the root files (root_file_names) left out. Problems: a path that isn't
    a data file's site path (site_path_problem), or one that several torrents hold.
    """
    archived = {}
    problems = []
    for torrent_id, info in sorted(torrents.items()):
        root_names = root_file_names(torrent_id)
        for path, (size, mtime) in sorted(info["files"].items()):
            if path in root_names:
                continue
            problem = site_path_problem(path)
            if problem:
                problems.append(f"{torrent_id}: {path!r}: {problem}")
            elif path in archived:
                problems.append(f"{path}: in {archived[path].torrent_id} and in {torrent_id}")
            else:
                archived[path] = ArchivedFile(torrent_id, size, mtime)
    return archived, problems


def seed_copy_stat(seed, path):
    """(size, whole-second modification time) of the regular file at path under SEED_DIR, or None (not a symlink)."""
    try:
        st = os.lstat(os.path.join(seed, *path.split("/")))
    except OSError:
        return None
    if not stat.S_ISREG(st.st_mode):
        return None
    return st.st_size, int(st.st_mtime)


def file_dating(path, archived_file):
    """(date, dated_by) as 01-find dated the file when it was archived: from the torrent's modification time."""
    return file_date(path, modified_date(archived_file.mtime))


def blockers(path, archived, site, seed_stat, first_kept_year):
    """
    Why this path must not be deleted, as the constants above (a list); empty means it may be. archived: {path:
    ArchivedFile}; site: {path: (size, mtime)} from a listing of the site; seed_stat: path -> (size, mtime) or None,
    called only once nothing else stands in the way; first_kept_year: keep_from_year(today).
    """
    archived_file = archived.get(path)
    if archived_file is None:
        return [NOT_ARCHIVED]
    if site_path_problem(path):
        return [BAD_PATH]
    if match_paths(path, NEVER_PURGE) is not None:
        return [PROTECTED]
    current = site.get(path)
    if current is None:
        return [GONE]
    if current[0] != archived_file.size or int(current[1]) != archived_file.mtime:
        return [CHANGED]
    if file_dating(path, archived_file)[0].year >= first_kept_year:
        return [RECENT]
    if seed_stat(path) != (archived_file.size, archived_file.mtime):
        return [NO_SEED_COPY]
    return []


def delete_record(path, archived_file):
    date, dated_by = file_dating(path, archived_file)
    return {
        "path": path,
        "size": archived_file.size,
        "last_modified": iso(archived_file.mtime),
        "date": date.isoformat(),
        "dated_by": dated_by,
        "torrent_id": archived_file.torrent_id,
    }


class Plan:
    """
    What a purge would do: delete, one record per file (delete_record); stays, {reason: [paths]} for every archived
    file that's still on the site and stays; gone, archived paths no longer on the site.
    """

    def __init__(self):
        self.delete = []
        self.stays = {}
        self.gone = []


def plan_purge(archived, site, seed, first_kept_year):
    """The Plan for every archived data file (blockers for each)."""
    plan = Plan()
    for path in sorted(archived):
        found = blockers(path, archived, site, lambda p: seed_copy_stat(seed, p), first_kept_year)
        if not found:
            plan.delete.append(delete_record(path, archived[path]))
        elif found[0] == GONE:
            plan.gone.append(path)
        else:
            plan.stays.setdefault(found[0], []).append(path)
    return plan


def verify_list(paths, archived, site, seed, first_kept_year):
    """
    (records, gone) for a list of paths to delete (a reviewed delete.txt, a verified list), checked from scratch
    against fresh data: records for those that may be deleted, the paths no longer on the site left out. Fails if
    any other path is blocked, listing them: nothing that isn't an archived, old, unchanged, copied file is ever
    deleted, and a list that says otherwise is stale or wrong.
    """
    records = []
    gone = []
    blocked = []
    for path in paths:
        found = blockers(path, archived, site, lambda p: seed_copy_stat(seed, p), first_kept_year)
        if not found:
            records.append(delete_record(path, archived[path]))
        elif found[0] == GONE:
            gone.append(path)
        else:
            blocked.append(f"{path}: {found[0]}")
    if blocked:
        error(f"{fmt_count(len(blocked))} listed paths must not be deleted, e.g.")
        for line in blocked[:LOG_SAMPLE]:
            error(f"  {line}")
        fail("The list holds paths that must stay: nothing deleted. Run d01-find again for a fresh list")
    return records, gone


def check_not_stale_year(done_first_kept_year, today):
    """Fails if the list was made for an earlier year than today's rule: its kept years would be too few."""
    if done_first_kept_year < keep_from_year(today):
        fail(
            f"d01-find kept files from {done_first_kept_year} on, but it's now {today.year}: files from "
            f"{keep_from_year(today)} on must be kept. Delete the completion files and run d01-find again"
        )


def published_problems(torrent_id, local_torrent, published_torrent, published_list, data_paths):
    """
    Why the torrent on the site isn't this one: the .torrent bytes differ from SEED_DIR/.torrents's, or the
    published <ID>.txt isn't exactly its data files. local_torrent, published_torrent: bytes; published_list: the
    .txt's text (or None if missing); data_paths: the torrent's data file paths.
    """
    problems = []
    if published_torrent is None:
        problems.append(f"{torrent_id}.torrent isn't published")
    elif published_torrent != local_torrent:
        problems.append(f"{torrent_id}.torrent on the site differs from the signed one in SEED_DIR")
    if published_list is None:
        problems.append(f"{torrent_id}.txt isn't published")
    elif sorted(published_list.splitlines()) != sorted(data_paths):
        listed = set(published_list.splitlines())
        extra = len(listed - set(data_paths))
        missing = len(set(data_paths) - listed)
        problems.append(
            f"{torrent_id}.txt on the site isn't the torrent's file list ({extra} extra, {missing} missing paths)"
        )
    return problems


def check_published(remote, torrents_dir, torrents, archived, scratch_dir):
    """
    published_problems for every torrent, read from the site's deep-archive/. Returns them all.
    """
    problems = []
    by_torrent = {}
    for path, archived_file in archived.items():
        by_torrent.setdefault(archived_file.torrent_id, []).append(path)
    on_site = {path for path, _size, _mtime in lsjson(join(remote, DEEP_ARCHIVE_DIR))}
    for torrent_id in sorted(torrents):
        with open(os.path.join(torrents_dir, f"{torrent_id}.torrent"), "rb") as f:
            local = f.read()
        published_torrent = published_list = None
        if f"{torrent_id}.torrent" in on_site:
            tmp = os.path.join(scratch_dir, f"{torrent_id}.published.torrent")
            rclone("copyto", join(remote, f"{DEEP_ARCHIVE_DIR}/{torrent_id}.torrent"), tmp)
            try:
                with open(tmp, "rb") as f:
                    published_torrent = f.read()
            finally:
                os.remove(tmp)
        if f"{torrent_id}.txt" in on_site:
            published_list = rclone("cat", join(remote, f"{DEEP_ARCHIVE_DIR}/{torrent_id}.txt"))
        problems += published_problems(
            torrent_id, local, published_torrent, published_list, by_torrent.get(torrent_id, [])
        )
    return problems


State = namedtuple("State", "torrents archived site")


def load_state(workflow, remote):
    """
    Everything a purge decision rests on, read fresh: the signed torrents (signatures verified), the data files they
    hold (archived_data_files), SEED_DIR's tree against them (seed_dir.quick_check), the published torrents against
    them (check_published), and a listing of the whole site. Fails on any problem, listing them.
    """
    section("Torrents")
    torrents_dir = workflow.torrents_dir()
    try:
        torrents, disk, problems = seed_dir.quick_check(workflow.seed_dir, torrents_dir)
    except seed_dir.SeedDirError as e:
        fail(str(e))
    if not torrents:
        fail(f"No torrents in {torrents_dir}: nothing is archived")
    fail_on(problems, "SEED_DIR doesn't match its signed torrents")
    archived, problems = archived_data_files(torrents)
    fail_on(problems, "The torrents hold paths that can't be purged safely")
    print(f"{len(torrents)} signed torrents, {fmt_count(len(archived))} data files, {fmt_count(len(disk))} in SEED_DIR")

    section("Published torrents")
    fail_on(
        check_published(remote, torrents_dir, torrents, archived, workflow.purge_dir),
        "The published torrents aren't the ones in SEED_DIR",
    )
    print(f"All {len(torrents)} published .torrent and .txt files match")

    section("Site listing")
    site = {}
    for path, size, mtime in lsjson(remote, recursive=True):
        site[path] = (size, mtime)
    print(f"{fmt_count(len(site))} files on the site right now")
    return State(torrents, archived, site)


def fail_on(problems, message):
    """Reports up to LOG_SAMPLE problems, then fails with the message, if there are any."""
    if not problems:
        return
    for problem in problems[:LOG_SAMPLE]:
        error(f"  {problem}")
    if len(problems) > LOG_SAMPLE:
        error(f"  ... and {fmt_count(len(problems) - LOG_SAMPLE)} more")
    fail(f"{message} ({fmt_count(len(problems))} problems)")


def total_bytes(records):
    return sum(r["size"] for r in records)


def describe(records):
    return f"{fmt_count(len(records))} files, {fmt_bytes(total_bytes(records))}"


@contextlib.contextmanager
def files_from(paths):
    """A temporary rclone --files-from-raw list of the paths, one per line."""
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".txt", delete=False) as f:
        f.writelines(p + "\n" for p in paths)
    try:
        yield f.name
    finally:
        os.remove(f.name)


def dry_run(remote, paths):
    """rclone's dry run of deleting exactly these paths, checked (check_dry_run). Deletes nothing."""
    if not paths:
        print("Nothing to delete")
        return
    with files_from(paths) as list_file:
        check_dry_run(remote, list_file, paths)


def delete_files(remote, paths):
    """
    Deletes exactly these paths, never by prefix or pattern: rclone delete --files-from-raw, only after a dry run of
    the same list matched it exactly (check_dry_run), and with --max-delete at its length, so rclone itself refuses
    to delete more. B2 soft-deletes (hard_delete is fixed to false, lib/workflow.py): treat this as permanent anyway.
    """
    if not paths:
        return
    with files_from(paths) as list_file:
        check_dry_run(remote, list_file, paths)
        # --files-from-raw: every line is a path as it is (--files-from would skip lines starting with # or ;).
        rclone_run("delete", remote, "--files-from-raw", list_file, "--max-delete", str(len(paths)))


def check_dry_run(remote, list_file, paths):
    """
    Runs rclone delete --dry-run with the list file, the same arguments as the delete itself, and fails unless rclone
    would delete exactly these paths: none missing, nothing else.
    """
    args = ["rclone", "delete", remote, "--files-from-raw", list_file, "--max-delete", str(len(paths))]
    try:
        result = subprocess.run([*args, "--dry-run", "--use-json-log"], capture_output=True, text=True)
    except FileNotFoundError:
        fail("rclone is not installed")
    would, messages = parse_dry_run_log(result.stderr)
    for message in messages:
        print(f"rclone: {message}", flush=True)
    if result.returncode != 0:
        fail(f"rclone delete --dry-run failed: {messages[-1] if messages else f'exit code {result.returncode}'}")
    expected = set(paths)
    missing = sorted(expected - would)
    extra = sorted(would - expected)
    for title, items in (
        ("listed, but not in rclone's dry run", missing),
        ("in rclone's dry run, but not listed", extra),
    ):
        if items:
            error(f"{title}: {fmt_count(len(items))}, e.g.")
            for item in items[:LOG_SAMPLE]:
                error(f"  {item}")
    if missing or extra:
        fail("rclone's dry run doesn't match the list exactly: nothing deleted")
    print(f"rclone's dry run would delete exactly the {fmt_count(len(paths))} files listed, nothing else")


def parse_dry_run_log(text):
    """
    (paths, other messages) from rclone's JSON log (--use-json-log) of a delete --dry-run: each file the dry run skips
    is an entry with "skipped": "delete" and the file's path as "object". Other entries give their "msg"; lines that
    aren't JSON are kept as they are.
    """
    paths = set()
    messages = []
    for line in text.splitlines():
        if not line.strip():
            continue
        try:
            entry = json.loads(line)
        except ValueError:
            entry = None
        if not isinstance(entry, dict):
            messages.append(line)
        elif entry.get("skipped") == "delete" and isinstance(entry.get("object"), str):
            paths.add(entry["object"])
        else:
            messages.append(str(entry.get("msg", line)))
    return paths, messages


def index_only_folders(site_paths, deleted_paths):
    """
    The folders holding or above a deleted path where nothing but index.html/index.json is left anywhere beneath,
    in a listing of the site afterwards, never the site root or deep-archive/. DataIndex never removes the index
    files of a folder it no longer lists (NOTES.md "Stale index files"), so these still link deleted files.
    """
    non_index = set()
    index_folders = set()
    for path in site_paths:
        parts = path.split("/")
        if parts[-1] in ("index.html", "index.json"):
            index_folders.add("/".join(parts[:-1]))
        else:
            non_index.update("/".join(parts[:i]) for i in range(len(parts)))
    touched = set()
    for path in deleted_paths:
        parts = path.split("/")[:-1]
        touched.update("/".join(parts[:i]) for i in range(1, len(parts) + 1))
    return sorted(
        f
        for f in index_folders & touched
        if f and f.split("/")[0] != DEEP_ARCHIVE_DIR and f not in non_index
    )

