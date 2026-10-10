#!/usr/bin/env python3
"""
find: step 2 of the EVE Ref Deep Archive workflow. Selects the files for the torrent: the oldest files on
data.everef.net that aren't archived yet, by modification date, up to the last date that keeps the torrent as big as
the size limits allow.

DRAFT. Lists the data site's bucket on B2 with rclone (DATA_BUCKET, see lib/workflow.py). Read-only: it only lists
files and reads published file lists.

    01-find.py

Needs 00-preflight's completion file, and refuses to run twice (lib/workflow.py).

How the cutoff is chosen:
1. List the entire bucket, with each file's size and modification time (rclone's ModTime: on B2, the upload time).
2. Drop what is never archived (see Skipped below), and files a published torrent already holds (see Published
   torrents below). Set index.json files aside: they aren't selected by their own date (see index.json below).
3. Drop files modified after the newest allowed date (MIN_AGE_YEARS before today). The rest are candidates.
4. Group the candidates by modification date (UTC). Find the first date where including every candidate on or
   before it, plus the index.json of every folder they're in, goes over MAX_BYTES of data or a .torrent estimate
   (lib/torrent_size.py) over MAX_TORRENT_BYTES (lib/workflow.py). The cutoff is the day before that date. If the
   limits are never reached, the cutoff is the newest allowed date.
5. Select every candidate modified on or before the cutoff, and the index.json of every folder they're in.

index.json: a folder's index.json is selected when any file directly in that folder is, whatever its own
modification date (DataIndex rewrites it on every run, so its date says nothing about the data). It isn't selected
otherwise. Only the folder's own index.json; parent folders' aren't added.

Published torrents: a path listed in a published deep-archive/<ID>.txt is skipped only if its modification date is on
or before that torrent's cutoff (the date in the ID): it's the version the torrent holds, still on the site because
its delete didn't happen (flagged). A later date means the file was uploaded again after it was archived (it
changed), so it's a candidate like any other and can land in a new torrent. The same path can be in several torrents,
one version each.

The torrent ID is everef-deep-archive-YYYY-MM-DD, with the cutoff date. The torrent holds everything not yet archived
that was modified on or before that date. Writes, in SCRATCH_DIR:

    find/
      files.jsonl          the selection, index.json files included: one file per line: path, size, last_modified, url
      listing.jsonl.gz     the full bucket listing the selection was made from
      report.txt           the printed report
      summary.json         everything in the report, machine-readable
      find.log             details: flagged files and folders
    01-find.done           completion file, with the torrent ID

The torrent content folder, SEED_DIR/<ID>/, must not exist yet. find doesn't create it; load does.

Skipped:
- index.html files (index.json files are selected with their folder, see above);
- datasets in TORRENT_EXCLUDE (lib/workflow.py), *-latest.* files, hidden files, files at the root of the site,
  everything under deep-archive/;
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
    GIB,
    KIB,
    DATA_BASE_URL,
    DEEP_ARCHIVE_DIR,
    MAX_BYTES,
    MAX_TORRENT_BYTES,
    MIB,
    PIECE_SIZE,
    TIB,
    TORRENT_EXCLUDE,
    TORRENT_ID_PREFIX,
    TORRENT_ID_RE,
    Workflow,
    data_remote,
    error,
    fail,
    file_list_problems,
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
YEAR_DIR_RE = re.compile(r"^(19|20)\d{2}$")

# Files modified less than this many years before today are never selected.
MIN_AGE_YEARS = 2

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

LIMIT_NAMES = {"torrent": "the .torrent size limit", "bytes": "the data size limit", "both": "both limits"}

class Obj:
    """One file in the bucket. path is relative to the site root; mtime is a POSIX timestamp."""

    __slots__ = ("path", "size", "mtime")

    def __init__(self, path, size, mtime):
        self.path = path
        self.size = size
        self.mtime = mtime

    @property
    def date(self):
        return datetime.datetime.fromtimestamp(self.mtime, datetime.timezone.utc).date()


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
    print(f"Excluded datasets: {', '.join(excludes) or '(none)'}")
    print(
        f"Limits: .torrent {fmt_size(MAX_TORRENT_BYTES)}, data {fmt_bytes(MAX_BYTES)}, "
        f"nothing modified after {newest_allowed}"
    )

    section("Listing")
    listing = list_remote(remote, args.progress_seconds)
    if not any(o.path == INDEX_JSON for o in listing):
        fail(f"No {INDEX_JSON} at the root of {remote}")

    section("Published file lists")
    taken = load_published_lists(remote, listing)

    section("Selection")
    scan = classify_all(listing, excludes, taken)
    cut = choose_cutoff(scan.candidates, scan.indexes, newest_allowed)
    if cut.cutoff is None:
        fail(render_no_cutoff(cut, scan))
    data = [o for o in scan.candidates if o.date <= cut.cutoff]
    selected = data + folder_indexes(data, scan.indexes)
    torrent_id = f"{TORRENT_ID_PREFIX}{cut.cutoff.isoformat()}"
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
    Path -> torrent ID for every path in a published torrent, from the deep-archive/<ID>.txt file lists in the
    bucket. A path in several lists maps to the newest torrent. Fails if any list isn't in the PLAN 2.5 format: a list
    find can't read correctly would let the same version of a file land in two torrents.
    """
    published = {}
    lists = {}  # path -> torrent ID
    for o in listing:
        folder, _, name = o.path.rpartition("/")
        if folder == DEEP_ARCHIVE_DIR and name.endswith(".txt"):
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
        self.candidates = []  # [Obj], sorted by mtime then path; no index.json
        self.indexes = {}  # folder -> its index.json Obj, if not in a published torrent
        self.taken_indexes = set()  # folders whose index.json is the version a published torrent holds
        self.taken = defaultdict(lambda: [0, 0])  # <ID> -> [files, bytes]: the version that torrent holds
        self.changed = defaultdict(lambda: [0, 0])  # <ID> -> [files, bytes]: changed since that torrent, candidates
        self.changed_paths = set()
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
        if archived_in is not None:
            if o.date <= torrent_cutoff(archived_in):
                add(scan.taken[archived_in], o.size)
                if is_index:
                    scan.taken_indexes.add(folder_of(o.path))
                continue
            if not is_index:  # DataIndex rewrites them, so they always look changed
                add(scan.changed[archived_in], o.size)
                scan.changed_paths.add(o.path)
        if is_index:
            scan.indexes[folder_of(o.path)] = o
            continue
        if o.size == 0:
            scan.empty.append(o.path)
        scan.candidates.append(o)
    scan.candidates.sort(key=lambda o: (o.mtime, o.path))
    print(f"{fmt_count(len(scan.candidates))} candidates, {fmt_count(len(scan.indexes))} index.json files")
    return scan


def is_torrent_id(name):
    if not TORRENT_ID_RE.match(name):
        return False
    try:
        torrent_cutoff(name)
    except ValueError:
        return False
    return True


def torrent_cutoff(torrent_id):
    """The cutoff date in a torrent ID: the torrent holds nothing modified after it."""
    return datetime.date.fromisoformat(torrent_id[len(TORRENT_ID_PREFIX) :])


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
    if parts[0] == DEEP_ARCHIVE_DIR:
        return ("ignored", f"{DEEP_ARCHIVE_DIR}/")
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
        self.reason = None  # "torrent", "bytes", "both" or "age"
        self.over_date = None  # first date that went over a limit
        self.over_files = None  # cumulative files including over_date
        self.over_bytes = None
        self.over_torrent_bytes = None  # .torrent estimate including over_date
        self.over_day = None  # (files, bytes) on over_date alone
        self.too_new = [0, 0]  # candidates modified after newest_allowed


def choose_cutoff(candidates, indexes, newest_allowed):
    """
    Last date such that every candidate on or before it, with the index.json of their folders, fits within MAX_BYTES
    of data and a .torrent estimate of MAX_TORRENT_BYTES. A folder's index.json counts on the first date one of its
    files is included. Prints each date considered, with that date's files and bytes and the running totals.
    """
    cut = Cutoff()
    days = defaultdict(list)  # date -> [Obj], index.json included
    counted = set()  # folders whose index.json is counted
    for o in candidates:  # sorted by mtime, so dates come in order
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
        over_torrent = torrent.bytes > MAX_TORRENT_BYTES
        over_bytes = size > MAX_BYTES
        reason = (
            "both" if over_torrent and over_bytes else "torrent" if over_torrent else "bytes" if over_bytes else None
        )
        print(
            f"  {date}  {fmt_count(day_files):>9} {fmt_bytes(day_bytes):>10}"
            f"  total {fmt_count(files):>9} {fmt_bytes(size):>10}  .torrent {fmt_bytes(torrent.bytes):>9}"
            f"{'  over ' + LIMIT_NAMES[reason] if reason else ''}"
        )
        if reason:
            cut.reason = reason
            cut.over_date = date
            cut.over_files = files
            cut.over_bytes = size
            cut.over_torrent_bytes = torrent.bytes
            cut.over_day = (day_files, day_bytes)
            if files == day_files:
                # The first date alone is over a limit: it can't be split by date.
                return cut
            cut.cutoff = date - datetime.timedelta(days=1)
            return cut
    if days:
        cut.reason = "age"
        cut.cutoff = newest_allowed
    return cut


def add(counter, size):
    counter[0] += 1
    counter[1] += size


def dataset_of(path):
    """The path before the year folder, without a trailing history; or the first three folders if there's no year."""
    dirs = path.split("/")[:-1]
    year_index = next((i for i, d in enumerate(dirs) if YEAR_DIR_RE.match(d)), None)
    if year_index is None:
        return "/".join(dirs[:3]) + " (no year folder)"
    dataset_parts = dirs[:year_index]
    if dataset_parts and dataset_parts[-1] == "history":
        dataset_parts = dataset_parts[:-1]
    return "/".join(dataset_parts) or "(root)"


def path_year(path):
    return next((d for d in path.split("/")[:-1] if YEAR_DIR_RE.match(d)), "(none)")


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
    """Folders of selected files with no index.json to select: (missing, already in a published torrent)."""
    folders = {folder_of(o.path) for o in data}
    missing = sorted(f for f in folders if f not in scan.indexes and f not in scan.taken_indexes)
    taken = sorted(f for f in folders if f in scan.taken_indexes)
    return missing, taken


def build_summary(listing, scan, cut, data, selected, torrent_id, data_remote, excludes, taken, newest_allowed):
    """data: the selected files without index.json, in mtime order; selected: data plus their folders' index.json."""
    now = datetime.datetime.now(datetime.timezone.utc)
    datasets = defaultdict(lambda: [0, 0])
    years = defaultdict(lambda: [0, 0])
    months = defaultdict(lambda: [0, 0])
    for o in selected:
        add(datasets[dataset_of(o.path)], o.size)
        add(years[path_year(o.path)], o.size)
    for o in data:
        # By the data's dates only: an index.json's date is the last DataIndex run.
        add(months[o.date.strftime("%Y-%m")], o.size)
    total = sum(o.size for o in selected)
    estimate = estimate_torrent(selected)
    index_files = len(selected) - len(data)
    index_bytes = total - sum(o.size for o in data)
    partial = partial_index_dirs(listing, selected)
    scan.partial_index = partial
    missing_index, taken_index = unindexed_folders(data, scan)
    scan.missing_index = missing_index
    scan.taken_index = taken_index

    flags = []
    if cut.reason == "age":
        flags.append(
            f"limits not reached: everything modified up to {newest_allowed} fits. The torrent is smaller than it "
            "could be; consider waiting"
        )
    if scan.empty:
        flags.append(f"empty files: {len(scan.empty)}")
    if partial:
        flags.append(f"selected index.json files that list files staying on the site: {len(partial)} (see find.log)")
    if missing_index:
        flags.append(f"folders with selected files but no index.json: {len(missing_index)} (see find.log)")
    if taken_index:
        flags.append(
            f"folders with selected files whose index.json, unchanged, is in a published torrent: {len(taken_index)} "
            "(see find.log)"
        )
    if scan.taken:
        flags.append(
            "files a published torrent holds are still on the site, unchanged: its delete didn't remove them "
            "(see Skipped)"
        )
    changed_selected = sum(1 for o in data if o.path in scan.changed_paths)
    if changed_selected:
        flags.append(f"selected files that changed after an earlier torrent archived them: {changed_selected}")
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
            "max_torrent_bytes": MAX_TORRENT_BYTES,
            "piece_size": PIECE_SIZE,
            "min_age_years": MIN_AGE_YEARS,
            "newest_allowed": newest_allowed.isoformat(),
        },
        "source": data_remote,
        "run_at": iso(now.timestamp()),
        "exclude": excludes,
        "published": sorted(set(taken.values())),
        "changed_since": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.changed.items())},
        "listed": {"files": len(listing), "bytes": sum(o.size for o in listing)},
        "candidates": {"files": len(scan.candidates), "bytes": sum(o.size for o in scan.candidates)},
        "too_new": {"files": cut.too_new[0], "bytes": cut.too_new[1]},
        "selected": {
            "files": len(selected),
            "bytes": total,
            "index_json": index_files,
            "index_json_bytes": index_bytes,
            "oldest_last_modified": iso(data[0].mtime) if data else None,
            "newest_last_modified": iso(data[-1].mtime) if data else None,
        },
        "torrent_estimate": estimate,
        "datasets": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(datasets.items())},
        "path_years": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(years.items())},
        "modified_months": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(months.items())},
        "flags": flags,
        "skipped": {
            "excluded": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.excluded.items())},
            "still_on_site": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.taken.items())},
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
        f"modified on or before {limits['newest_allowed']}"
    )
    listed = summary["listed"]
    candidates = summary["candidates"]
    too_new = summary["too_new"]
    lines.append(f"Listed:     {fmt_count(listed['files'])} files, {fmt_bytes(listed['bytes'])}")
    lines.append(
        f"Candidates: {fmt_count(candidates['files'])} files, {fmt_bytes(candidates['bytes'])} "
        f"({fmt_count(too_new['files'])} files, {fmt_bytes(too_new['bytes'])} too new)"
    )
    changed = summary["changed_since"]
    if changed:
        lines.append(
            f"Changed:    {fmt_count(sum(c['files'] for c in changed.values()))} files, "
            f"{fmt_bytes(sum(c['bytes'] for c in changed.values()))}, changed after a published torrent archived them "
            "(candidates again)"
        )
    lines.append("")
    lines.append(
        f"Selected:   {fmt_count(selected['files'])} files, {fmt_bytes(selected['bytes'])} "
        f"(incl. {fmt_count(selected['index_json'])} index.json, {fmt_bytes(selected['index_json_bytes'])})"
    )
    if selected["oldest_last_modified"]:
        lines.append(f"Modified:   {selected['oldest_last_modified']} to {selected['newest_last_modified']}")
    lines.append(f"Cutoff:     {summary['cutoff']}, {render_cutoff_reason(summary)}")
    lines.append(
        f"Estimate:   .torrent {fmt_size(est['bytes'])}, {fmt_count(est['tokens'])} tokens, "
        f"{fmt_count(est['pieces'])} pieces of {fmt_bytes(est['piece_size'])}"
    )
    lines.append("")
    lines.append("Modified (month, UTC, without index.json):")
    lines.extend(render_rows([(name, s["files"], s["bytes"]) for name, s in summary["modified_months"].items()]))
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
    rows += [(f"unchanged, in {label}", s["files"], s["bytes"]) for label, s in skipped["still_on_site"].items()]
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
    limit = LIMIT_NAMES[summary["cutoff_reason"]]
    return (
        f"the day before {summary['next_date']}, which goes over {limit} "
        f"({fmt_count(summary['next_date_files'])} files, {fmt_bytes(summary['next_date_bytes'])} that day, "
        f".torrent {fmt_bytes(summary['next_date_torrent_bytes'])} with it)"
    )


def render_no_cutoff(cut, scan):
    if cut.over_date is None:
        return (
            f"No candidates modified on or before the newest allowed date. "
            f"{fmt_count(len(scan.candidates))} candidates, all newer."
        )
    files, size = cut.over_day
    return (
        f"The oldest date, {cut.over_date}, alone has {fmt_count(files)} files ({fmt_bytes(size)}, .torrent "
        f"{fmt_size(cut.over_torrent_bytes)}), over the limit of a {fmt_size(MAX_TORRENT_BYTES)} .torrent or "
        f"{fmt_bytes(MAX_BYTES)} of data. A single date can't be split: this needs resolving by hand."
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
    sections = [
        ("selected files with no year folder in the path", no_year),
        ("selected index.json files that list files staying on the site", scan.partial_index),
        ("folders with selected files but no index.json", scan.missing_index),
        ("folders with selected files whose index.json, unchanged, is in a published torrent", scan.taken_index),
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


def iso(timestamp):
    return datetime.datetime.fromtimestamp(timestamp, datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


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


def fmt_size(n):
    """Bytes with the exact count, for .torrent sizes: limits are in decimal bytes, fmt_bytes rounds to binary units."""
    return f"{fmt_bytes(n)} ({fmt_count(n)} bytes)"


def fmt_bytes(n):
    for unit, factor in (("TiB", TIB), ("GiB", GIB), ("MiB", MIB), ("KiB", KIB)):
        if n >= factor:
            return f"{n / factor:.1f} {unit}"
    return f"{n} B"


if __name__ == "__main__":
    main()
