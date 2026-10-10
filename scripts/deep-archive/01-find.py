#!/usr/bin/env python3
"""
find: step 2 of the EVE Ref Deep Archive workflow. Selects the files for the torrent: the oldest files on
data.everef.net that aren't archived yet, by modification date, up to the last date that keeps the torrent as big as
the size limits allow.

DRAFT. Lists the data site's bucket on B2 with rclone (DATA_REMOTE in lib/workflow.py). Read-only: it only lists files
and reads published file lists.

    01-find.py

Needs 00-preflight's completion file, and refuses to run twice (lib/workflow.py).

How the cutoff is chosen:
1. List the entire bucket, with each file's size and modification time (rclone's ModTime: on B2, the upload time).
2. Drop what is never archived (see Skipped below), and files already in a published torrent.
3. Drop files modified after the newest allowed date (MIN_AGE_YEARS before today). The rest are candidates.
4. Group the candidates by modification date (UTC). Find the first date where including every candidate on or
   before it goes over MAX_FILES or MAX_BYTES (lib/workflow.py). The cutoff is the day before that date. If the limits
   are never reached, the cutoff is the newest allowed date.
5. Select every candidate modified on or before the cutoff.

The torrent ID is everef-deep-archive-YYYY-MM-DD, with the cutoff date. The torrent holds everything not yet archived
that was modified on or before that date. Writes, in SCRATCH_DIR:

    find/
      files.jsonl          the selection: one file per line: path, size, last_modified, url
      listing.jsonl.gz     the full bucket listing the selection was made from
      report.txt           the printed report
      summary.json         everything in the report, machine-readable
      find.log             details: flagged files
    01-find.done           completion file, with the torrent ID

The torrent content folder, SEED_DIR/<ID>/, must not exist yet. find doesn't create it; load does.

Skipped:
- index.html files (index.json files are selected like any other file);
- datasets in TORRENT_EXCLUDE (lib/workflow.py), *-latest.* files, hidden files, files at the root of the site,
  everything under deep-archive/;
- files listed in a published deep-archive/<ID>.txt, so no file lands in two torrents.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
    RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY   the B2 key (lib/workflow.py)

Needs rclone.
"""
import argparse
import datetime
import gzip
import json
import math
import os
import re
import shutil
import sys
import time
import urllib.parse
from collections import defaultdict

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    GIB,
    KIB,
    DATA_BASE_URL,
    DATA_REMOTE,
    MAX_BYTES,
    MAX_FILES,
    MIB,
    PIECE_SIZE,
    TARGET_TORRENT_BYTES,
    TIB,
    TORRENT_EXCLUDE,
    Workflow,
    fail,
    join,
    lsjson,
    ok,
    rclone,
    section,
)

STEP = "find"

TORRENT_ID_PREFIX = "everef-deep-archive-"
INDEX_JSON = "index.json"
IGNORED_NAMES = {"index.html"}
DEEP_ARCHIVE_DIR = "deep-archive"
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
    data_remote = DATA_REMOTE
    today = datetime.datetime.now(datetime.timezone.utc).date()
    newest_allowed = years_before(today, MIN_AGE_YEARS)

    section("Settings")
    print(f"Source: {data_remote}")
    print(f"Seed dir: {workflow.seed_dir}")
    print(f"Excluded datasets: {', '.join(excludes) or '(none)'}")
    print(f"Limits: {fmt_count(MAX_FILES)} files, {fmt_bytes(MAX_BYTES)}, nothing modified after {newest_allowed}")

    section("Listing")
    listing = list_remote(data_remote, args.progress_seconds)
    if not any(o.path == INDEX_JSON for o in listing):
        fail(f"No {INDEX_JSON} at the root of {data_remote}")

    section("Published file lists")
    taken = load_published_lists(data_remote, listing)

    section("Selection")
    scan = classify_all(listing, excludes, taken)
    cut = choose_cutoff(scan.candidates, newest_allowed)
    if cut.cutoff is None:
        fail(render_no_cutoff(cut, scan))
    selected = [o for o in scan.candidates if o.date <= cut.cutoff]
    torrent_id = f"{TORRENT_ID_PREFIX}{cut.cutoff.isoformat()}"
    print(f"Cutoff: {cut.cutoff}")
    print(f"Torrent ID: {torrent_id}")
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    if os.path.exists(content_dir):
        fail(f"{content_dir} already exists: a torrent with this ID was started before")

    summary = build_summary(listing, scan, cut, selected, torrent_id, data_remote, excludes, taken, newest_allowed)
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
    write_text(os.path.join(tmp_dir, "find.log"), render_log(scan, selected))
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
    """Paths already in a published torrent, from deep-archive/*.txt in the bucket."""
    published = {}
    lists = [o.path for o in listing if o.path.startswith(DEEP_ARCHIVE_DIR + "/") and o.path.endswith(".txt")]
    lists = [p for p in lists if "/" not in p[len(DEEP_ARCHIVE_DIR) + 1 :]]
    if not lists:
        print(f"No published file lists in {DEEP_ARCHIVE_DIR}/")
        return published
    print(f"Loading {len(lists)} published file lists from {DEEP_ARCHIVE_DIR}/")
    for path in sorted(lists):
        torrent_id = path.rsplit("/", 1)[-1][: -len(".txt")]
        for line in rclone("cat", join(data_remote, path)).splitlines():
            line = line.strip()
            if line:
                published[line] = f"published {torrent_id}"
    print(f"Loaded {fmt_count(len(published))} paths from {len(lists)} published file lists")
    return published


class Scan:
    def __init__(self):
        self.candidates = []  # [Obj], sorted by mtime then path
        self.taken = defaultdict(lambda: [0, 0])  # "published <ID>" -> [files, bytes]
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
        if o.path in taken:
            add(scan.taken[taken[o.path]], o.size)
            continue
        if o.size == 0:
            scan.empty.append(o.path)
        scan.candidates.append(o)
    scan.candidates.sort(key=lambda o: (o.mtime, o.path))
    print(f"{fmt_count(len(scan.candidates))} candidates")
    return scan


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
        self.reason = None  # "files", "bytes", "both" or "age"
        self.over_date = None  # first date that went over a limit
        self.over_files = None  # cumulative files including over_date
        self.over_bytes = None
        self.over_day = None  # (files, bytes) on over_date alone
        self.too_new = [0, 0]  # candidates modified after newest_allowed


def choose_cutoff(candidates, newest_allowed):
    """Last date such that every candidate on or before it fits within MAX_FILES and MAX_BYTES."""
    cut = Cutoff()
    days = defaultdict(lambda: [0, 0])
    for o in candidates:
        date = o.date
        if date > newest_allowed:
            add(cut.too_new, o.size)
        else:
            add(days[date], o.size)
    files = 0
    size = 0
    for date in sorted(days):
        day_files, day_bytes = days[date]
        files += day_files
        size += day_bytes
        over_files = files > MAX_FILES
        over_bytes = size > MAX_BYTES
        if over_files or over_bytes:
            cut.reason = "both" if over_files and over_bytes else "files" if over_files else "bytes"
            cut.over_date = date
            cut.over_files = files
            cut.over_bytes = size
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
        name = o.path.rsplit("/", 1)[-1]
        if o.path in selected_paths or name in IGNORED_NAMES or name == INDEX_JSON:
            continue
        parts = o.path.split("/")[:-1]
        for i in range(len(parts) + 1):
            remaining.add("/".join(parts[:i]))
    partial = []
    for o in selected:
        if o.path.rsplit("/", 1)[-1] == INDEX_JSON:
            folder = o.path.rsplit("/", 1)[0] if "/" in o.path else ""
            if folder in remaining:
                partial.append(o.path)
    return partial


def build_summary(listing, scan, cut, selected, torrent_id, data_remote, excludes, taken, newest_allowed):
    now = datetime.datetime.now(datetime.timezone.utc)
    datasets = defaultdict(lambda: [0, 0])
    years = defaultdict(lambda: [0, 0])
    months = defaultdict(lambda: [0, 0])
    for o in selected:
        add(datasets[dataset_of(o.path)], o.size)
        add(years[path_year(o.path)], o.size)
        add(months[o.date.strftime("%Y-%m")], o.size)
    total = sum(o.size for o in selected)
    estimate = estimate_torrent(selected)
    index_files = sum(1 for o in selected if o.path.rsplit("/", 1)[-1] == INDEX_JSON)
    partial = partial_index_dirs(listing, selected)
    scan.partial_index = partial

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
    if scan.taken:
        flags.append("some files are already in a published torrent but still on the site (see Skipped)")
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
        "limits": {
            "max_files": MAX_FILES,
            "max_bytes": MAX_BYTES,
            "target_torrent_bytes": TARGET_TORRENT_BYTES,
            "piece_size": PIECE_SIZE,
            "min_age_years": MIN_AGE_YEARS,
            "newest_allowed": newest_allowed.isoformat(),
        },
        "source": data_remote,
        "run_at": iso(now.timestamp()),
        "exclude": excludes,
        "taken_by": sorted(set(taken.values())),
        "listed": {"files": len(listing), "bytes": sum(o.size for o in listing)},
        "candidates": {"files": len(scan.candidates), "bytes": sum(o.size for o in scan.candidates)},
        "too_new": {"files": cut.too_new[0], "bytes": cut.too_new[1]},
        "selected": {
            "files": len(selected),
            "bytes": total,
            "index_json": index_files,
            "oldest_last_modified": iso(selected[0].mtime) if selected else None,
            "newest_last_modified": iso(selected[-1].mtime) if selected else None,
        },
        "torrent_estimate": estimate,
        "datasets": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(datasets.items())},
        "path_years": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(years.items())},
        "modified_months": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(months.items())},
        "flags": flags,
        "skipped": {
            "excluded": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.excluded.items())},
            "already_taken": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.taken.items())},
            "root_files": len(scan.root_files),
            "ignored": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.ignored.items())},
        },
    }


def estimate_torrent(files):
    """
    Rough size of a hybrid v1+v2 .torrent for these files at PIECE_SIZE, and its bdecode token count.

    Hybrid torrents align every file to a piece, so each file gets its own pieces, a pad file in the v1 list, and an
    entry in the v2 file tree. Trackers, comment and the two root files are ignored (small). This is an estimate
    for spotting problems, not an exact number.
    """
    piece = PIECE_SIZE
    size = 1024
    tokens = 50
    pieces = 0
    directories = set()
    for o in files:
        parts = o.path.split("/")
        path_bytes = sum(len(p.encode("utf-8")) + len(str(len(p.encode("utf-8")))) + 1 for p in parts)
        file_pieces = max(1, math.ceil(o.size / piece))
        pieces += file_pieces
        # v1 file entry: d6:lengthi<n>e4:pathl...ee
        size += 20 + len(str(o.size)) + path_bytes
        tokens += 5 + len(parts)
        # v1 pad file entry: d4:attr1:p6:lengthi<n>e4:pathl4:.pad<n>:<n>ee
        if o.size % piece:
            size += 50
            tokens += 9
        # v1 piece hashes
        size += 20 * file_pieces
        # v2 file tree leaf: <name>d0:d6:lengthi<n>e11:pieces root32:...ee
        size += 60 + len(str(o.size)) + len(parts[-1].encode("utf-8"))
        tokens += 8
        # v2 piece layers, only for files bigger than one piece
        if o.size > piece:
            size += 36 + 32 * file_pieces
            tokens += 2
        for i in range(1, len(parts)):
            directories.add("/".join(parts[:i]))
    for directory in directories:
        size += len(directory.rsplit("/", 1)[-1].encode("utf-8")) + 4
        tokens += 2
    return {"piece_size": piece, "pieces": pieces, "bytes": size, "tokens": tokens}


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
    lines.append(f"EVE Ref Deep Archive — find, cutoff {summary['cutoff']}")
    lines.append(f"Torrent ID: {summary['torrent_id']}")
    lines.append(f"Source:     {summary['source']}")
    lines.append(f"Exclude:    {', '.join(summary['exclude']) or '(none)'}")
    lines.append(
        f"Limits:     {fmt_count(limits['max_files'])} files, {fmt_bytes(limits['max_bytes'])}, "
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
    lines.append("")
    lines.append(
        f"Selected:   {fmt_count(selected['files'])} files, {fmt_bytes(selected['bytes'])} "
        f"(incl. {fmt_count(selected['index_json'])} index.json)"
    )
    if selected["oldest_last_modified"]:
        lines.append(f"Modified:   {selected['oldest_last_modified']} to {selected['newest_last_modified']}")
    lines.append(f"Cutoff:     {summary['cutoff']}, {render_cutoff_reason(summary)}")
    lines.append(
        f"Estimate:   .torrent {fmt_bytes(est['bytes'])}, {fmt_count(est['tokens'])} tokens, "
        f"{fmt_count(est['pieces'])} pieces of {fmt_bytes(est['piece_size'])}"
    )
    lines.append("")
    lines.append("Modified (month, UTC):")
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
    rows += [(f"already in {label}", s["files"], s["bytes"]) for label, s in skipped["already_taken"].items()]
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
    limit = {"files": "the file limit", "bytes": "the size limit", "both": "both limits"}[summary["cutoff_reason"]]
    return (
        f"the day before {summary['next_date']}, which goes over {limit} "
        f"({fmt_count(summary['next_date_files'])} files, {fmt_bytes(summary['next_date_bytes'])} that day)"
    )


def render_no_cutoff(cut, scan):
    if cut.over_date is None:
        return (
            f"No candidates modified on or before the newest allowed date. "
            f"{fmt_count(len(scan.candidates))} candidates, all newer."
        )
    files, size = cut.over_day
    return (
        f"The oldest date, {cut.over_date}, alone has {fmt_count(files)} files ({fmt_bytes(size)}), over the limit of "
        f"{fmt_count(MAX_FILES)} files or {fmt_bytes(MAX_BYTES)}. A single date can't be split: this needs resolving "
        "by hand."
    )


def render_rows(rows):
    """Label, file count and bytes, aligned."""
    width = max((len(label) for label, _, _ in rows), default=0) + 2
    return [
        f"  {label:<{width}}{fmt_count(files):>12}{'' if size is None else fmt_bytes(size):>12}".rstrip()
        for label, files, size in rows
    ]


def render_log(scan, selected):
    lines = []
    no_year = [o.path for o in selected if path_year(o.path) == "(none)"]
    sections = [
        ("selected files with no year folder in the path", no_year),
        ("selected index.json files that list files staying on the site", scan.partial_index),
        ("empty files", scan.empty),
        ("root-level files, never selected", scan.root_files),
    ]
    for title, paths in sections:
        if paths:
            lines.append(f"{title}: {len(paths)} files, e.g.")
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


def fmt_bytes(n):
    for unit, factor in (("TiB", TIB), ("GiB", GIB), ("MiB", MIB), ("KiB", KIB)):
        if n >= factor:
            return f"{n / factor:.1f} {unit}"
    return f"{n} B"


if __name__ == "__main__":
    main()
