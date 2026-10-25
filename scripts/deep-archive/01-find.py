#!/usr/bin/env python3
"""
find: a step of the EVE Ref Deep Archive workflow. Selects the files for the torrent: the oldest files on
data.everef.net that aren't archived yet, by date (see Dates below), up to the last date that keeps the torrent as big
as the size limits allow.

DRAFT. Lists the data site's bucket on B2 with rclone (DATA_BUCKET, see lib/workflow.py). Read-only: it only lists
files and reads published file lists.

    01-find.py

Needs 00-preflight's completion file, and refuses to run twice (lib/workflow.py).

How the cutoff is chosen:
1. List the entire bucket, with each file's size and modification time (rclone's ModTime: on B2, the upload time).
2. Drop what is never archived (see Skipped below), and files a published torrent already holds (see Published
   torrents below). Set index.json files aside: they aren't selected by their own date (see index.json below).
3. Drop files dated after the newest allowed date (MIN_AGE_YEARS before today). The rest are candidates.
4. Group the candidates by date and add them oldest first, each with the index.json of every folder they're in
   (lib/workflow.py has the limits):
   - MAX_TORRENT_BYTES is hard: the first date that takes the .torrent estimate (lib/torrent_size.py) over it is
     left out, and the cutoff is the day before.
   - MAX_BYTES is soft: the first date that takes the data to it or over is the cutoff. The torrent can be over it.
   If neither is reached, the cutoff is the newest allowed date.
5. Select every candidate dated on or before the cutoff, and the index.json of every folder they're in.
6. Abort if the selection holds less than MIN_BYTES (lib/workflow.py): not enough data for a torrent yet.

Dates: a file's date is the earliest of:
- its modification date (UTC);
- any ISO date (YYYY-MM-DD) in its file name;
- 31 December of any year (a 4-digit number from FIRST_YEAR on) in its file name.
Only the file name counts, not the folders. A 4-digit ID in a name looks like a year and dates the file too early;
the report counts the files dated each way, and find.log lists samples.

index.json: a folder's index.json is selected when any file directly in that folder is, whatever its own
modification date (DataIndex rewrites it on every run, so its date says nothing about the data). It isn't selected
otherwise. Only the folder's own index.json; parent folders' aren't added.

Published torrents: a non-index.json path listed in a published deep-archive/<ID>.txt is simply never a candidate
again -- it's already archived. If it's still a candidate by the current scan (old enough, not excluded) and also
still on the site, that's a bug (its delete after an earlier torrent didn't happen, or it reappeared): find aborts
listing every such path, rather than guessing whether it changed and silently re-selecting it. index.json is the
exception: DataIndex rewrites it on every run regardless of the underlying data, so the same folder's index.json can
legitimately appear in several torrents' file lists over time (one partial archive after another) without anything
being wrong.

Torrent ID: sequential (everef-eve-online-deep-archive-NN, two digits, PLAN 2.3), chosen by 00-preflight (the
completion file find needs) rather than from the cutoff date. The torrent holds everything not yet archived that
was dated on or before the cutoff this run chose, picked fresh from this run's own candidates -- not from any
earlier torrent's cutoff. Writes, in SCRATCH_DIR:

    find/
      files.jsonl          the selection, index.json files included: one file per line: path, size, last_modified,
                           date, dated_by, url
      listing.jsonl.gz     the full bucket listing the selection was made from
      report.txt           the printed report
      summary.json         everything in the report, machine-readable
      find.log             details: flagged files and folders
    01-find.done           completion file, with the torrent ID

The torrent content folder, SEED_DIR/<ID>/, must not exist yet. find doesn't create it; load does.

Skipped:
- index.html files (index.json files are selected with their folder, see above);
- folders in TORRENT_EXCLUDE (lib/workflow.py), *-latest.* files, hidden files, files at the root of the site;
- files a published torrent already holds (see Published torrents above).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
    DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY   the bucket and B2 key (lib/workflow.py)

Needs rclone.
"""
import argparse
import datetime
import gzip
import json
import os
import re
import shutil
import sys
import time
import urllib.parse
from collections import defaultdict

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from torrent_size import TorrentSize  # noqa: E402
from workflow import (  # noqa: E402
    DATA_BASE_URL,
    DEEP_ARCHIVE_DIR,
    MAX_BYTES,
    MAX_TORRENT_BYTES,
    MIB,
    MIN_AGE_YEARS,
    MIN_BYTES,
    PIECE_SIZE,
    TORRENT_EXCLUDE,
    TORRENT_ID_RE,
    Workflow,
    classify_path,
    data_remote,
    error,
    fail,
    file_list_problems,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    fmt_size,
    iso,
    join,
    lsjson,
    ok,
    rclone,
    section,
)

STEP = "find"

INDEX_JSON = "index.json"
IGNORED_NAMES = {"index.html"}
LATEST_RE = re.compile(r"-latest\.")
NAME_DATE_RE = re.compile(r"(?<![0-9])([0-9]{4})-([0-9]{2})-([0-9]{2})(?![0-9])")
NAME_YEAR_RE = re.compile(r"(?<![0-9])([0-9]{4})(?![0-9])")
# Earlier numbers in a file name aren't years: EVE Online was released in 2003.
FIRST_YEAR = 2003

# Limits a .torrent should stay within (PLAN 2.4 "Creating the torrent"), checked against the estimate.
# libtorrent defaults from its documentation; the qBittorrent values are assumed and need checking.
LIMITS = [
    ("libtorrent .torrent size (max_buffer_size)", "bytes", 10_000_000),
    ("libtorrent decode tokens (max_decode_tokens)", "tokens", 3_000_000),
    ("libtorrent magnet metadata (max_metadata_size)", "bytes", 3 * 1024 * 10240),
    ("qBittorrent .torrent size (assumed default, check)", "bytes", 100 * MIB),
    ("qBittorrent decode tokens (assumed default, check)", "tokens", 10_000_000),
]

LOG_SAMPLE = 20

LIMIT_NAMES = {
    "torrent": "over the .torrent size limit: left out",
    "bytes": "reached the data size limit: last date taken",
}

class Obj:
    """
    One file in the bucket. path is relative to the site root; mtime is a POSIX timestamp. date is what the file is
    selected by, and dated_by where it came from (file_date).
    """

    __slots__ = ("path", "size", "mtime", "date", "dated_by")

    def __init__(self, path, size, mtime):
        self.path = path
        self.size = size
        self.mtime = mtime
        self.date, self.dated_by = file_date(path, self.modified)

    @property
    def modified(self):
        return datetime.datetime.fromtimestamp(self.mtime, datetime.timezone.utc).date()


def file_date(path, modified):
    """
    (date, dated_by): the earliest of the modification date ("modified"), any ISO date in the file name ("name date"),
    and 31 December of any year in the file name ("name year"). On a tie, the first of those.
    """
    name = file_name(path)
    dates = [(modified, "modified")]
    for match in NAME_DATE_RE.finditer(name):
        try:
            date = datetime.date(int(match[1]), int(match[2]), int(match[3]))
        except ValueError:
            continue
        if date.year >= FIRST_YEAR:
            dates.append((date, "name date"))
    for match in NAME_YEAR_RE.finditer(name):
        year = int(match[1])
        if year >= FIRST_YEAR:
            dates.append((datetime.date(year, 12, 31), "name year"))
    return min(dates, key=lambda d: d[0])


def main():
    args = parse_args()
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    excludes = TORRENT_EXCLUDE
    remote = data_remote()
    today = datetime.datetime.now(datetime.timezone.utc).date()
    newest_allowed = years_before(today, MIN_AGE_YEARS)

    section("Settings")
    print(f"Source: {remote}")
    print(f"Seed dir: {workflow.seed_dir}")
    print(f"Excluded folders: {', '.join(excludes) or '(none)'}")
    print(
        f"Limits: .torrent {fmt_size(MAX_TORRENT_BYTES)}, data {fmt_bytes(MIN_BYTES)} to {fmt_bytes(MAX_BYTES)}, "
        f"nothing dated after {newest_allowed}"
    )

    section("Listing")
    listing = list_remote(remote, args.progress_seconds)
    if not any(o.path == INDEX_JSON for o in listing):
        fail(f"No {INDEX_JSON} at the root of {remote}")

    section("Published file lists")
    taken = load_published_lists(remote, listing)

    section("Selection")
    scan = classify_all(listing, excludes, taken)
    if scan.already_archived:
        error(f"{fmt_count(len(scan.already_archived))} paths already archived but still on the site, e.g.")
        for path in scan.already_archived[:LOG_SAMPLE]:
            error(f"  {path}")
        fail(
            f"{fmt_count(len(scan.already_archived))} paths a published torrent already holds are still on the "
            "site: an earlier delete didn't happen, or the path reappeared. Resolve by hand before running find again"
        )
    cut = choose_cutoff(scan.candidates, scan.indexes, newest_allowed)
    if cut.cutoff is None:
        fail(render_no_cutoff(cut, scan))
    data = [o for o in scan.candidates if o.date <= cut.cutoff]
    selected = data + folder_indexes(data, scan.indexes)
    selected_bytes = sum(o.size for o in selected)
    if selected_bytes < MIN_BYTES:
        fail(
            f"Only {fmt_bytes(selected_bytes)} ({fmt_count(len(selected))} files, up to {cut.cutoff}) to archive, "
            f"less than MIN_BYTES ({fmt_bytes(MIN_BYTES)}): not enough data for a torrent yet"
        )
    preflight = workflow.read_done("preflight")["summary"]
    torrent_id = preflight.get("next_torrent_id")
    if not torrent_id:
        fail("00-preflight.done has no next_torrent_id: rerun preflight")
    print(f"Cutoff: {cut.cutoff}")
    print(f"Torrent ID: {torrent_id}")
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    if os.path.exists(content_dir):
        fail(f"{content_dir} already exists: a torrent with this ID was started before")

    summary = build_summary(listing, scan, cut, data, selected, torrent_id, remote, excludes, taken, newest_allowed)
    print(f"Estimated .torrent: {fmt_size(summary['torrent_estimate']['bytes'])}, max {fmt_size(MAX_TORRENT_BYTES)}")
    report = render_report(summary)
    section("Report")
    print(report, end="")

    section("Writing results")
    find_dir = os.path.join(workflow.scratch_dir, "find")
    tmp_dir = find_dir + ".tmp"
    if os.path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)
    os.makedirs(tmp_dir)
    write_file_list(os.path.join(tmp_dir, "files.jsonl"), selected, DATA_BASE_URL)
    write_listing(os.path.join(tmp_dir, "listing.jsonl.gz"), listing)
    write_text(os.path.join(tmp_dir, "summary.json"), json.dumps(summary, indent=2, sort_keys=True) + "\n")
    write_text(os.path.join(tmp_dir, "report.txt"), report)
    write_text(os.path.join(tmp_dir, "find.log"), render_log(scan, data))
    if os.path.exists(find_dir):
        # Left by an earlier attempt that failed before completing.
        print(f"Replacing incomplete output in {find_dir}")
        shutil.rmtree(find_dir)
    os.rename(tmp_dir, find_dir)
    print(f"{fmt_count(len(selected))} files in {os.path.join(find_dir, 'files.jsonl')}")
    print(f"Listing, report, summary and details in {find_dir}")
    print(f"Content folder, for load: {content_dir}")

    workflow.complete(
        STEP,
        torrent_id,
        {
            "cutoff": summary["cutoff"],
            "files": summary["selected"]["files"],
            "bytes": summary["selected"]["bytes"],
            "torrent_bytes": summary["torrent_estimate"]["bytes"],
            "flags": summary["flags"],
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Find OK for {torrent_id}")


def parse_args():
    parser = argparse.ArgumentParser(description="Select the files for the next EVE Ref Deep Archive torrent.")
    parser.add_argument(
        "--progress-seconds", type=float, default=10, help="log listing progress this often (0 to turn it off)"
    )
    return parser.parse_args()


def years_before(date, years):
    try:
        return date.replace(year=date.year - years)
    except ValueError:  # 29 February
        return date.replace(year=date.year - years, day=28)


def list_remote(data_remote, progress_seconds):
    print(f"Listing {data_remote}")
    started = time.monotonic()
    last_progress = started
    listing = []
    total = 0
    for path, size, mtime in lsjson(data_remote, recursive=True):
        listing.append(Obj(path, size, mtime))
        total += size
        now = time.monotonic()
        if progress_seconds and now - last_progress >= progress_seconds:
            last_progress = now
            print(f"{fmt_count(len(listing))} files ({fmt_bytes(total)}) so far, at {path}")
    print(
        f"Listed {fmt_count(len(listing))} files ({fmt_bytes(total)}) in {fmt_duration(time.monotonic() - started)}"
    )
    return listing


def load_published_lists(data_remote, listing):
    """
    published maps path -> torrent ID for every path in a published torrent, from the deep-archive/<ID>.txt file
    lists in the bucket (a path in several lists maps to the newest torrent). Fails if any list isn't in the
    PLAN 2.5 format: full site paths, sorted, one per line.
    """
    published = {}
    lists = {}  # path -> torrent ID
    for o in listing:
        folder, _, name = o.path.rpartition("/")
        if folder != DEEP_ARCHIVE_DIR:
            continue
        if name.endswith(".txt"):
            torrent_id = name[: -len(".txt")]
            if is_torrent_id(torrent_id):
                lists[o.path] = torrent_id
            else:
                print(f"Not a file list (no torrent ID in the name), ignored: {o.path}")
    if not lists:
        print(f"No published file lists in {DEEP_ARCHIVE_DIR}/")
        return published
    print(f"Loading {len(lists)} published file lists from {DEEP_ARCHIVE_DIR}/")
    bad = 0
    for path, torrent_id in sorted(lists.items()):
        text = rclone("cat", join(data_remote, path))
        problems = file_list_problems(text)
        if problems:
            bad += 1
            error(f"{path}: {len(problems)} problems, e.g.")
            for problem in problems[:LOG_SAMPLE]:
                error(f"  {problem}")
            continue
        for line in text.splitlines():
            published[line] = torrent_id  # sorted by ID, so the newest torrent wins
    if bad:
        fail(f"{bad} published file lists aren't in the PLAN 2.5 format: full site paths, sorted, one per line")
    print(f"Loaded {fmt_count(len(published))} paths from {len(lists)} published file lists")
    return published


class Scan:
    def __init__(self):
        self.candidates = []  # [Obj], sorted by date, mtime, then path; no index.json
        self.indexes = {}  # folder -> its index.json Obj
        self.already_archived = []  # "path (<ID>)" for a non-index path a published torrent already holds
        self.excluded = defaultdict(lambda: [0, 0])  # dataset -> [files, bytes]
        self.ignored = defaultdict(lambda: [0, 0])  # reason -> [files, bytes]
        self.root_files = []
        self.empty = []


def classify_all(listing, excludes, taken):
    scan = Scan()
    for o in listing:
        reason = skip_reason(o.path, excludes)
        if reason is not None:
            kind, label = reason
            if kind == "excluded":
                add(scan.excluded[label], o.size)
            elif kind == "root":
                scan.root_files.append(o.path)
            else:
                add(scan.ignored[label], o.size)
            continue
        is_index = file_name(o.path) == INDEX_JSON
        archived_in = taken.get(o.path)
        if archived_in is not None and not is_index:
            # index.json is exempt: DataIndex rewrites it every run, so the same folder's index.json legitimately
            # shows up in several torrents' file lists over time (one partial archive after another).
            scan.already_archived.append(f"{o.path} ({archived_in})")
            continue
        if is_index:
            scan.indexes[folder_of(o.path)] = o
            continue
        if o.size == 0:
            scan.empty.append(o.path)
        scan.candidates.append(o)
    scan.candidates.sort(key=lambda o: (o.date, o.mtime, o.path))
    print(f"{fmt_count(len(scan.candidates))} candidates, {fmt_count(len(scan.indexes))} index.json files")
    return scan


def is_torrent_id(name):
    return bool(TORRENT_ID_RE.match(name))


def file_name(path):
    return path.rsplit("/", 1)[-1]


def folder_of(path):
    return path.rsplit("/", 1)[0] if "/" in path else ""


def folder_indexes(files, indexes):
    """The index.json of every folder the files are directly in, where there is one."""
    folders = {folder_of(o.path) for o in files}
    return [indexes[f] for f in sorted(folders) if f in indexes]


def skip_reason(path, excludes):
    """(kind, label) if the path is never archived, else None."""
    parts = path.split("/")
    name = parts[-1]
    if name in IGNORED_NAMES:
        return ("ignored", "index.html files")
    if LATEST_RE.search(name):
        return ("ignored", "latest files")
    if any(p.startswith(".") for p in parts):
        return ("ignored", "hidden files")
    for exclude in excludes:
        if path == exclude or path.startswith(exclude + "/"):
            return ("excluded", exclude)
    if len(parts) == 1:
        # Root-level files are site files, not datasets.
        return ("root", None)
    return None


class Cutoff:
    def __init__(self):
        self.cutoff = None  # last date included
        self.reason = None  # "torrent", "bytes" or "age"
        # The date that stopped the selection: for "torrent", the first date left out (it went over the hard
        # .torrent limit); for "bytes", the last date included (it reached the soft data limit).
        self.over_date = None
        self.over_files = None  # cumulative files including over_date
        self.over_bytes = None
        self.over_torrent_bytes = None  # .torrent estimate including over_date
        self.over_day = None  # (files, bytes) on over_date alone
        self.too_new = [0, 0]  # candidates dated after newest_allowed


def choose_cutoff(candidates, indexes, newest_allowed):
    """
    The cutoff date: every candidate on or before it is selected, with the index.json of their folders. A folder's
    index.json counts on the first date one of its files is included. Dates are added oldest first, and never split:
    - MAX_TORRENT_BYTES is a hard limit: the first date that takes the .torrent estimate over it is left out, and the
      cutoff is the day before. If that's the oldest date, there is no cutoff.
    - MAX_BYTES is a soft limit: the first date that takes the data to it or over is included, and the selection
      stops there. The torrent can end up over MAX_BYTES.
    - Neither reached: the cutoff is newest_allowed.
    Prints each date considered, with that date's files and bytes and the running totals.
    """
    cut = Cutoff()
    days = defaultdict(list)  # date -> [Obj], index.json included
    counted = set()  # folders whose index.json is counted
    for o in candidates:  # sorted by date, so dates come in order
        date = o.date
        if date > newest_allowed:
            add(cut.too_new, o.size)
            continue
        days[date].append(o)
        folder = folder_of(o.path)
        if folder not in counted:
            counted.add(folder)
            if folder in indexes:
                days[date].append(indexes[folder])
    files = 0
    size = 0
    torrent = TorrentSize(PIECE_SIZE)
    if days:
        print(
            "Dates considered (files and bytes that date, then the totals up to it and the .torrent estimate, "
            "index.json included):"
        )
    for date in sorted(days):
        day_files = len(days[date])
        day_bytes = sum(o.size for o in days[date])
        for o in days[date]:
            torrent.add(o.path, o.size)
        files += day_files
        size += day_bytes
        reason = "torrent" if torrent.bytes > MAX_TORRENT_BYTES else "bytes" if size >= MAX_BYTES else None
        print(
            f"  {date}  {fmt_count(day_files):>9} {fmt_bytes(day_bytes):>10}"
            f"  total {fmt_count(files):>9} {fmt_bytes(size):>10}  .torrent {fmt_bytes(torrent.bytes):>9}"
            f"{'  ' + LIMIT_NAMES[reason] if reason else ''}"
        )
        if reason:
            cut.reason = reason
            cut.over_date = date
            cut.over_files = files
            cut.over_bytes = size
            cut.over_torrent_bytes = torrent.bytes
            cut.over_day = (day_files, day_bytes)
            if reason == "bytes":
                cut.cutoff = date
            elif files != day_files:
                cut.cutoff = date - datetime.timedelta(days=1)
            # else: the oldest date alone is over the .torrent limit, and a date can't be split: no cutoff.
            return cut
    if days:
        cut.reason = "age"
        cut.cutoff = newest_allowed
    return cut


def add(counter, size):
    counter[0] += 1
    counter[1] += size


def dataset_of(path):
    """The path's dataset (classify_path), flagged when it has no year folder."""
    year, dataset = classify_path(path)
    if year is None:
        return f"{dataset} (no year folder)"
    return dataset or "(root)"


def path_year(path):
    year, _ = classify_path(path)
    return str(year) if year is not None else "(none)"


def partial_index_dirs(listing, selected):
    """Folders whose selected index.json lists something that isn't selected (it stays on the site)."""
    selected_paths = {o.path for o in selected}
    remaining = set()
    for o in listing:
        name = file_name(o.path)
        if o.path in selected_paths or name in IGNORED_NAMES or name == INDEX_JSON:
            continue
        parts = o.path.split("/")[:-1]
        for i in range(len(parts) + 1):
            remaining.add("/".join(parts[:i]))
    partial = []
    for o in selected:
        if file_name(o.path) == INDEX_JSON and folder_of(o.path) in remaining:
            partial.append(o.path)
    return partial


def unindexed_folders(data, scan):
    """Folders of selected files with no index.json to select."""
    folders = {folder_of(o.path) for o in data}
    return sorted(f for f in folders if f not in scan.indexes)


def build_summary(listing, scan, cut, data, selected, torrent_id, data_remote, excludes, taken, newest_allowed):
    """data: the selected files without index.json, in date order; selected: data plus their folders' index.json."""
    now = datetime.datetime.now(datetime.timezone.utc)
    datasets = defaultdict(lambda: [0, 0])
    years = defaultdict(lambda: [0, 0])
    months = defaultdict(lambda: [0, 0])
    dated_by = defaultdict(lambda: [0, 0])
    for o in selected:
        add(datasets[dataset_of(o.path)], o.size)
        add(years[path_year(o.path)], o.size)
    for o in data:
        # By the data's dates only: an index.json's date is the last DataIndex run.
        add(months[o.date.strftime("%Y-%m")], o.size)
        add(dated_by[o.dated_by], o.size)
    total = sum(o.size for o in selected)
    estimate = estimate_torrent(selected)
    index_files = len(selected) - len(data)
    index_bytes = total - sum(o.size for o in data)
    partial = partial_index_dirs(listing, selected)
    scan.partial_index = partial
    missing_index = unindexed_folders(data, scan)
    scan.missing_index = missing_index

    flags = []
    if cut.reason == "age":
        flags.append(
            f"limits not reached: everything dated up to {newest_allowed} fits. The torrent is smaller than it "
            "could be; consider waiting"
        )
    if scan.empty:
        flags.append(f"empty files: {len(scan.empty)}")
    if partial:
        flags.append(f"selected index.json files that list files staying on the site: {len(partial)} (see find.log)")
    if missing_index:
        flags.append(f"folders with selected files but no index.json: {len(missing_index)} (see find.log)")
    for limit_name, unit, limit in LIMITS:
        value = estimate[unit]
        if value > limit:
            flags.append(f"torrent estimate over {limit_name}: {fmt_count(value)} > {fmt_count(limit)} {unit}")

    return {
        "torrent_id": torrent_id,
        "cutoff": cut.cutoff.isoformat(),
        "cutoff_reason": cut.reason,
        "next_date": cut.over_date.isoformat() if cut.over_date else None,
        "next_date_files": cut.over_day[0] if cut.over_day else None,
        "next_date_bytes": cut.over_day[1] if cut.over_day else None,
        "next_date_torrent_bytes": cut.over_torrent_bytes,
        "limits": {
            "max_bytes": MAX_BYTES,
            "min_bytes": MIN_BYTES,
            "max_torrent_bytes": MAX_TORRENT_BYTES,
            "piece_size": PIECE_SIZE,
            "min_age_years": MIN_AGE_YEARS,
            "newest_allowed": newest_allowed.isoformat(),
        },
        "source": data_remote,
        "run_at": iso(now.timestamp()),
        "exclude": excludes,
        "published": sorted(set(taken.values())),
        "listed": {"files": len(listing), "bytes": sum(o.size for o in listing)},
        "candidates": {"files": len(scan.candidates), "bytes": sum(o.size for o in scan.candidates)},
        "too_new": {"files": cut.too_new[0], "bytes": cut.too_new[1]},
        "selected": {
            "files": len(selected),
            "bytes": total,
            "index_json": index_files,
            "index_json_bytes": index_bytes,
            "oldest_date": data[0].date.isoformat() if data else None,
            "newest_date": data[-1].date.isoformat() if data else None,
            "oldest_last_modified": iso(min(o.mtime for o in data)) if data else None,
            "newest_last_modified": iso(max(o.mtime for o in data)) if data else None,
        },
        "dated_by": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(dated_by.items())},
        "torrent_estimate": estimate,
        "datasets": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(datasets.items())},
        "path_years": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(years.items())},
        "date_months": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(months.items())},
        "flags": flags,
        "skipped": {
            "excluded": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.excluded.items())},
            "root_files": len(scan.root_files),
            "ignored": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.ignored.items())},
        },
    }


def estimate_torrent(files):
    """The .torrent estimate for these files at PIECE_SIZE (lib/torrent_size.py)."""
    torrent = TorrentSize(PIECE_SIZE)
    for o in files:
        torrent.add(o.path, o.size)
    return {"piece_size": PIECE_SIZE, "pieces": torrent.pieces, "bytes": torrent.bytes, "tokens": torrent.tokens}


def write_file_list(path, selected, base_url):
    with open(path, "w", encoding="utf-8") as f:
        for o in sorted(selected, key=lambda o: o.path):
            record = {
                "path": o.path,
                "size": o.size,
                "last_modified": iso(o.mtime),
                "date": o.date.isoformat(),
                "dated_by": o.dated_by,
                "url": base_url + urllib.parse.quote(o.path),
            }
            f.write(json.dumps(record) + "\n")


def write_listing(path, listing):
    with gzip.open(path, "wt", encoding="utf-8") as f:
        for o in listing:
            f.write(json.dumps({"path": o.path, "size": o.size, "last_modified": iso(o.mtime)}) + "\n")


def render_report(summary):
    selected = summary["selected"]
    est = summary["torrent_estimate"]
    limits = summary["limits"]
    lines = []
    lines.append(f"EVE Ref Deep Archive find, cutoff {summary['cutoff']}")
    lines.append(f"Torrent ID: {summary['torrent_id']}")
    lines.append(f"Source:     {summary['source']}")
    lines.append(f"Exclude:    {', '.join(summary['exclude']) or '(none)'}")
    lines.append(
        f"Limits:     .torrent {fmt_size(limits['max_torrent_bytes'])}, data {fmt_bytes(limits['max_bytes'])}, "
        f"dated on or before {limits['newest_allowed']}"
    )
    listed = summary["listed"]
    candidates = summary["candidates"]
    too_new = summary["too_new"]
    lines.append(f"Listed:     {fmt_count(listed['files'])} files, {fmt_bytes(listed['bytes'])}")
    lines.append(
        f"Candidates: {fmt_count(candidates['files'])} files, {fmt_bytes(candidates['bytes'])} "
        f"({fmt_count(too_new['files'])} files, {fmt_bytes(too_new['bytes'])} too new)"
    )
    lines.append("")
    lines.append(
        f"Selected:   {fmt_count(selected['files'])} files, {fmt_bytes(selected['bytes'])} "
        f"(incl. {fmt_count(selected['index_json'])} index.json, {fmt_bytes(selected['index_json_bytes'])})"
    )
    if selected["oldest_date"]:
        lines.append(f"Dated:      {selected['oldest_date']} to {selected['newest_date']}")
        lines.append(f"Modified:   {selected['oldest_last_modified']} to {selected['newest_last_modified']}")
    lines.append(f"Cutoff:     {summary['cutoff']}, {render_cutoff_reason(summary)}")
    lines.append(
        f"Estimate:   .torrent {fmt_size(est['bytes'])}, {fmt_count(est['tokens'])} tokens, "
        f"{fmt_count(est['pieces'])} pieces of {fmt_bytes(est['piece_size'])}"
    )
    lines.append("")
    lines.append("Dated by (the earliest of the three, without index.json):")
    lines.extend(render_rows([(name, s["files"], s["bytes"]) for name, s in summary["dated_by"].items()]))
    lines.append("")
    lines.append("Date (month, without index.json):")
    lines.extend(render_rows([(name, s["files"], s["bytes"]) for name, s in summary["date_months"].items()]))
    lines.append("")
    lines.append("Year folder in the path:")
    lines.extend(render_rows([(name, s["files"], s["bytes"]) for name, s in summary["path_years"].items()]))
    lines.append("")
    lines.append("Datasets:")
    lines.extend(render_rows([(name, s["files"], s["bytes"]) for name, s in summary["datasets"].items()]))
    lines.append("")
    if summary["flags"]:
        lines.extend(f"FLAG: {flag}" for flag in summary["flags"])
        lines.append("")

    skipped = summary["skipped"]
    rows = []
    rows += [(f"excluded {name}", s["files"], s["bytes"]) for name, s in skipped["excluded"].items()]
    if skipped["root_files"]:
        rows.append(("root-level files", skipped["root_files"], None))
    rows += [(name, s["files"], s["bytes"]) for name, s in skipped["ignored"].items()]
    if rows:
        lines.append("Skipped:")
        lines.extend(render_rows(rows))
        lines.append("")
    return "\n".join(lines) + "\n"


def render_cutoff_reason(summary):
    if summary["cutoff_reason"] == "age":
        return "the newest allowed date (limits not reached)"
    day = (
        f"({fmt_count(summary['next_date_files'])} files, {fmt_bytes(summary['next_date_bytes'])} that day, "
        f".torrent {fmt_bytes(summary['next_date_torrent_bytes'])} with it)"
    )
    if summary["cutoff_reason"] == "bytes":
        return f"the date that reached the data size limit (soft: the torrent may go over it) {day}"
    return f"the day before {summary['next_date']}, which goes over the .torrent size limit (hard) {day}"


def render_no_cutoff(cut, scan):
    if cut.over_date is None:
        return (
            f"No candidates dated on or before the newest allowed date. "
            f"{fmt_count(len(scan.candidates))} candidates, all newer."
        )
    files, size = cut.over_day
    return (
        f"The oldest date, {cut.over_date}, alone has {fmt_count(files)} files ({fmt_bytes(size)}, .torrent "
        f"{fmt_size(cut.over_torrent_bytes)}), over the limit of a {fmt_size(MAX_TORRENT_BYTES)} .torrent. A single "
        "date can't be split: this needs resolving by hand."
    )


def render_rows(rows):
    """Label, file count and bytes, aligned."""
    width = max((len(label) for label, _, _ in rows), default=0) + 2
    return [
        f"  {label:<{width}}{fmt_count(files):>12}{'' if size is None else fmt_bytes(size):>12}".rstrip()
        for label, files, size in rows
    ]


def render_log(scan, data):
    lines = []
    no_year = [o.path for o in data if path_year(o.path) == "(none)"]
    by_name_date = [f"{o.path} ({o.date}, modified {o.modified})" for o in data if o.dated_by == "name date"]
    by_name_year = [f"{o.path} ({o.date}, modified {o.modified})" for o in data if o.dated_by == "name year"]
    sections = [
        ("selected files dated by a year in the name (check it isn't an ID)", by_name_year),
        ("selected files dated by a date in the name, earlier than modified", by_name_date),
        ("selected files with no year folder in the path", no_year),
        ("selected index.json files that list files staying on the site", scan.partial_index),
        ("folders with selected files but no index.json", scan.missing_index),
        ("empty files", scan.empty),
        ("root-level files, never selected", scan.root_files),
    ]
    for title, paths in sections:
        if paths:
            lines.append(f"{title}: {len(paths)}, e.g.")
            lines.extend(f"  {p}" for p in paths[:LOG_SAMPLE])
    return "\n".join(lines) + "\n"


def write_text(path, text):
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


if __name__ == "__main__":
    main()
