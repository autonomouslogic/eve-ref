#!/usr/bin/env python3
"""
find: select the files on data.everef.net for one EVE Ref Deep Archive torrent: everything up to and including a
year that isn't already archived.

DRAFT. Reads a LOCAL copy of data.everef.net (an rclone mirror or a backup) instead of the public index.json files.
The source is only read, never written.

    find.py --dir <work dir> --source <local copy of data.everef.net> --year <YYYY>

<work dir> is the working directory, which is also the directory torrents are seeded from. For year YYYY the torrent
ID is everef-deep-archive-YYYY, and the script creates, inside <work dir>:

    everef-deep-archive-YYYY/          torrent content folder, created empty if missing (filled later by load)
    everef-deep-archive-YYYY.work/
      find/                            this script's output, replaced on every run:
        files.jsonl                    one selected file per line: path, size, last_modified, url, selected_by
        report.txt                     the printed report
        summary.json                   everything in the report, machine-readable
        find.log                       details: flagged files, symlinks, ...

Nothing is ever written inside the content folder: it must hold exactly what goes into the torrent.

Rules (PLAN 2.2, 2.8):
- --year Y selects files from Y and every year before it, except files already in a published torrent or in
  another frozen selection. The first torrent catches up on all old years; later ones usually add one year.
- The year of a file comes from the first YYYY directory in its path (selected_by "path").
- Files without a year in the path are selected when their modification time is in Y or before
  (selected_by "mtime"), e.g. fuzzwork/ordersets/backfills/. Files at the root of the site are never selected
  this way.
- Which years to archive is the operator's decision (policy: years <= current year - 2). The script doesn't check.
- index.json files are selected when their path has a year (the year's directory listings go into the torrent).
  index.json files without a year in the path list live directories, so they're never selected by mtime.
- Skipped: datasets in TORRENT_EXCLUDE, *-latest.* files, index.html, totals.json, hidden files,
  everything under deep-archive/, files listed in a published deep-archive/<ID>.txt in the source, and files
  in another year's frozen selection in <work dir> (<ID>.work/files.jsonl), so no file lands in two torrents.

Last-modified is the local file's mtime. That's only meaningful if the copy kept the site's timestamps (rclone does).

Environment:
    TORRENT_EXCLUDE         comma-separated dataset paths, default "market-history,killmails"
    DATA_BASE_URL           default "https://data.everef.net/"
"""

import argparse
import datetime
import json
import logging
import math
import os
import re
import shutil
import sys
import time
import urllib.parse
from collections import defaultdict

TORRENT_ID_PREFIX = "everef-deep-archive-"
INDEX_JSON = "index.json"
IGNORED_NAMES = {"index.html", "totals.json"}
DEEP_ARCHIVE_DIR = "deep-archive"
LATEST_RE = re.compile(r"-latest\.")
YEAR_DIR_RE = re.compile(r"^(19|20)\d{2}$")
DASHED_DATE_RE = re.compile(r"(?<!\d)((?:19|20)\d{2})-(0[1-9]|1[0-2])(?:-(0[1-9]|[12]\d|3[01]))?(?!\d)")

KIB = 1024
MIB = 1024 * KIB
GIB = 1024 * MIB

# Limits a .torrent should stay within (PLAN 2.4 "Creating the torrent").
# libtorrent defaults from its documentation; the qBittorrent values are assumed and need checking.
LIMITS = [
    ("libtorrent .torrent size (max_buffer_size)", "bytes", 10_000_000),
    ("libtorrent decode tokens (max_decode_tokens)", "tokens", 3_000_000),
    ("libtorrent magnet metadata (max_metadata_size)", "bytes", 3 * 1024 * 10240),
    ("qBittorrent .torrent size (assumed default, check)", "bytes", 100 * MIB),
    ("qBittorrent decode tokens (assumed default, check)", "tokens", 10_000_000),
]

LOG_SAMPLE = 20

log = logging.getLogger("find")


def main():
    args = parse_args()
    logging.basicConfig(
        stream=sys.stderr, level=logging.INFO, format="%(asctime)s %(message)s", datefmt="%Y-%m-%d %H:%M:%S"
    )
    started = time.monotonic()
    excludes = [e.strip().strip("/") for e in os.environ.get("TORRENT_EXCLUDE", "market-history,killmails").split(",")]
    excludes = [e for e in excludes if e]
    base_url = os.environ.get("DATA_BASE_URL", "https://data.everef.net/")
    if not base_url.endswith("/"):
        base_url += "/"
    year = args.year

    work_dir = os.path.realpath(args.dir)
    source = os.path.realpath(args.source)
    if not os.path.isdir(work_dir):
        fail(f"--dir does not exist: {work_dir}")
    if not os.path.isdir(source):
        fail(f"--source does not exist: {source}")
    if is_inside(work_dir, source):
        fail("--dir must not be inside --source")
    if not YEAR_DIR_RE.match(str(year)):
        fail(f"--year doesn't look like a year: {year}")
    if args.limit is not None and args.limit < 1:
        fail("--limit must be at least 1")

    torrent_id = f"{TORRENT_ID_PREFIX}{year}"
    content_dir = os.path.join(work_dir, torrent_id)
    find_dir = os.path.join(work_dir, f"{torrent_id}.work", "find")
    log.info(f"Selecting everything up to and including {year} ({torrent_id})")
    log.info(f"Source:   {source}")
    log.info(f"Work dir: {work_dir}")
    log.info(f"Excluded datasets: {', '.join(excludes) or '(none)'}")

    taken = load_published_lists(source)
    taken.update(load_frozen_selections(work_dir, torrent_id))
    scan = scan_source(source, year, excludes, taken, args.progress_seconds, args.limit)

    if os.path.isdir(content_dir):
        log.info(f"Content folder exists: {content_dir}")
    else:
        log.info(f"Creating content folder: {content_dir}")
        os.makedirs(content_dir)
    tmp_dir = find_dir + ".tmp"
    if os.path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)
    os.makedirs(tmp_dir)

    log.info("Summarising and estimating the .torrent size")
    summary = build_summary(scan, torrent_id, year, source, work_dir, excludes, taken, args.recent_days)
    log.info(f"Writing results to {tmp_dir}")
    write_file_list(os.path.join(tmp_dir, "files.jsonl"), scan, base_url)
    report = render_report(summary)
    write_text(os.path.join(tmp_dir, "summary.json"), json.dumps(summary, indent=2, sort_keys=True) + "\n")
    write_text(os.path.join(tmp_dir, "report.txt"), report)
    write_text(os.path.join(tmp_dir, "find.log"), render_log(scan))
    log.info(f"Wrote {fmt_count(len(scan.selected))} records to files.jsonl, plus report.txt, summary.json, find.log")

    if os.path.exists(find_dir):
        if not os.path.isfile(os.path.join(find_dir, "summary.json")):
            fail(f"{find_dir} exists but doesn't look like find output; not replacing it")
        log.info(f"Replacing previous output in {find_dir}")
        shutil.rmtree(find_dir)
    os.rename(tmp_dir, find_dir)
    log.info(f"Done in {fmt_duration(time.monotonic() - started)}")

    print(report, end="")
    print(f"Content folder: {content_dir}")
    print(f"Output:         {find_dir}")
    print("OK (debug: --limit used, selection incomplete)" if scan.limited else "OK")


def parse_args():
    parser = argparse.ArgumentParser(description="Select files up to a year for an EVE Ref Deep Archive torrent.")
    parser.add_argument("--dir", required=True, help="working directory: the directory torrents are seeded from")
    parser.add_argument("--source", required=True, help="local copy of data.everef.net, with the site's layout")
    parser.add_argument("--year", required=True, type=int, help="select this year and every year before it, e.g. 2025")
    parser.add_argument("--recent-days", type=int, default=365, help="flag files modified this recently")
    parser.add_argument(
        "--limit", type=int, help="debug: stop the scan once this many files are selected (incomplete selection)"
    )
    parser.add_argument(
        "--progress-seconds", type=float, default=10, help="log scan progress this often (0 to turn it off)"
    )
    return parser.parse_args()


def fail(reason):
    print(f"FAILED: {reason}")
    sys.exit(1)


def is_inside(path, parent):
    return path == parent or path.startswith(parent + os.sep)


def load_published_lists(source):
    """Paths already in a published torrent, from deep-archive/*.txt in the source."""
    published = {}
    deep_archive = os.path.join(source, DEEP_ARCHIVE_DIR)
    if not os.path.isdir(deep_archive):
        log.info(f"No published file lists: {deep_archive} doesn't exist")
        return published
    log.info(f"Loading published file lists from {deep_archive}")
    lists = 0
    for name in sorted(os.listdir(deep_archive)):
        if not name.endswith(".txt"):
            continue
        torrent_id = name[: -len(".txt")]
        lists += 1
        with open(os.path.join(deep_archive, name), encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    published[line] = f"published {torrent_id}"
    log.info(f"Loaded {fmt_count(len(published))} paths from {lists} published file lists")
    return published


def load_frozen_selections(work_dir, torrent_id):
    """Paths in other years' frozen selections (<ID>.work/files.jsonl) in the work dir."""
    frozen = {}
    selections = []
    for name in sorted(os.listdir(work_dir)):
        if not name.startswith(TORRENT_ID_PREFIX) or not name.endswith(".work") or name == f"{torrent_id}.work":
            continue
        files = os.path.join(work_dir, name, "files.jsonl")
        if not os.path.isfile(files):
            continue
        other_id = name[: -len(".work")]
        selections.append(other_id)
        with open(files, encoding="utf-8") as f:
            for line in f:
                if line.strip():
                    frozen[json.loads(line)["path"]] = f"frozen selection {other_id}"
    if selections:
        log.info(f"Loaded {fmt_count(len(frozen))} paths from frozen selections: {', '.join(selections)}")
    else:
        log.info("No frozen selections of other years in the work dir")
    return frozen


class Scan:
    def __init__(self):
        self.scanned = [0, 0]  # [files, bytes], everything looked at
        self.selected = []  # [(path, size, mtime, dataset, selected_by)]
        self.taken = defaultdict(lambda: [0, 0])  # "published <ID>" or "frozen selection <ID>" -> [files, bytes]
        self.excluded = defaultdict(lambda: [0, 0])  # dataset -> [files, bytes]
        self.ignored = defaultdict(lambda: [0, 0])  # reason -> [files, bytes], all years
        self.root_files = []  # root-level files modified in the year or before: never selected by mtime
        self.year_mismatch = []  # date in path disagrees with the year folder
        self.empty = []
        self.symlinks = []
        self.limited = None  # --limit, if the scan stopped early


def scan_source(source, year, excludes, taken, progress_seconds, limit=None):
    log.info(f"Scanning {source}" + (f" (debug: stopping at {fmt_count(limit)} selected files)" if limit else ""))
    scan = Scan()
    started = time.monotonic()
    last_progress = started
    for dirpath, dirnames, filenames in os.walk(source, followlinks=False):
        rel_dir = os.path.relpath(dirpath, source)
        rel_dir = "" if rel_dir == "." else rel_dir.replace(os.sep, "/")
        if rel_dir and "/" not in rel_dir:
            log.info(f"Scanning {rel_dir}/")
        if rel_dir == "" and DEEP_ARCHIVE_DIR in dirnames:
            dirnames.remove(DEEP_ARCHIVE_DIR)
        dirnames.sort()
        for name in sorted(filenames):
            path = f"{rel_dir}/{name}" if rel_dir else name
            full = os.path.join(dirpath, name)
            if os.path.islink(full):
                scan.symlinks.append(path)
                continue
            stat = os.stat(full)
            add(scan.scanned, stat.st_size)
            classify(scan, path, name, stat.st_size, stat.st_mtime, year, excludes, taken)
            if limit and len(scan.selected) >= limit:
                scan.limited = limit
                log.warning(f"Debug limit reached: stopping the scan at {fmt_count(limit)} selected files")
                log.info(f"Scan stopped in {fmt_duration(time.monotonic() - started)}: {progress(scan)}")
                return scan
        now = time.monotonic()
        if progress_seconds and now - last_progress >= progress_seconds:
            last_progress = now
            log.info(f"  {progress(scan)}, in {rel_dir or '/'}")
    log.info(f"Scan done in {fmt_duration(time.monotonic() - started)}: {progress(scan)}")
    return scan


def progress(scan):
    selected_bytes = sum(f[1] for f in scan.selected)
    return (
        f"scanned {fmt_count(scan.scanned[0])} files ({fmt_bytes(scan.scanned[1])}), "
        f"selected {fmt_count(len(scan.selected))} ({fmt_bytes(selected_bytes)})"
    )


def classify(scan, path, name, size, mtime, year, excludes, taken):
    if name in IGNORED_NAMES:
        add(scan.ignored["index.html/totals.json files"], size)
        return
    if LATEST_RE.search(name):
        add(scan.ignored["latest files"], size)
        return
    if name.startswith("."):
        add(scan.ignored["hidden files"], size)
        return

    parts = path.split("/")
    dirs = parts[:-1]
    year_index = next((i for i, d in enumerate(dirs) if YEAR_DIR_RE.match(d)), None)
    if year_index is None:
        # No year in the URL: selected if modified in the year or before.
        file_year = datetime.datetime.fromtimestamp(mtime, datetime.timezone.utc).year
        if file_year > year:
            return
        if not dirs:
            # Root-level files are site files, not datasets: never selected by mtime.
            scan.root_files.append(path)
            return
        if name == INDEX_JSON:
            # Listings of directories that stay on the site.
            return
        dataset = "/".join(dirs[:3])
        selected_by = "mtime"
    else:
        # Year in the URL: selected if it's the year or before.
        file_year = int(dirs[year_index])
        if file_year > year:
            return
        dataset_parts = dirs[:year_index]
        if dataset_parts and dataset_parts[-1] == "history":
            dataset_parts = dataset_parts[:-1]
        dataset = "/".join(dataset_parts) or "(root)"
        selected_by = "path"

    for exclude in excludes:
        if path == exclude or path.startswith(exclude + "/"):
            add(scan.excluded[exclude], size)
            return
    if path in taken:
        add(scan.taken[taken[path]], size)
        return

    if selected_by == "path":
        for match in DASHED_DATE_RE.finditer("/".join(parts[year_index + 1 :])):
            if int(match.group(1)) != file_year:
                scan.year_mismatch.append(path)
                break
    if size == 0:
        scan.empty.append(path)
    scan.selected.append((path, size, mtime, dataset, selected_by, file_year))


def add(counter, size):
    counter[0] += 1
    counter[1] += size


def build_summary(scan, torrent_id, year, source, work_dir, excludes, taken, recent_days):
    now = datetime.datetime.now(datetime.timezone.utc)
    files = scan.selected
    datasets = defaultdict(lambda: [0, 0])
    years = defaultdict(lambda: [0, 0])
    for _, size, _, dataset, selected_by, file_year in files:
        add(datasets[dataset if selected_by == "path" else f"{dataset} (by mtime)"], size)
        add(years[file_year], size)
    total = sum(f[1] for f in files)
    estimate = estimate_torrent(files, total)

    flags = []
    if scan.limited:
        flags.append(f"DEBUG: scan stopped at --limit {fmt_count(scan.limited)}: the selection is INCOMPLETE")
    if not files:
        flags.append("nothing selected")
    recent_after = now.timestamp() - recent_days * 86400
    data_files = [f for f in files if f[0].rsplit("/", 1)[-1] != INDEX_JSON]
    recent = sum(1 for f in data_files if f[2] >= recent_after)
    if recent:
        flags.append(f"recently modified: {recent} files in the last {recent_days} days (index.json not counted)")
    index_files = sum(1 for f in files if f[0].rsplit("/", 1)[-1] == INDEX_JSON)
    by_mtime = sum(1 for f in files if f[4] == "mtime")
    if by_mtime:
        flags.append(f"selected by modification time (no year in path, {year} or before): {by_mtime} files, check them")
    if scan.year_mismatch:
        flags.append(f"date in path doesn't match year folder: {len(scan.year_mismatch)} files")
    if scan.empty:
        flags.append(f"empty files: {len(scan.empty)}")
    if scan.taken:
        flags.append("some files are already in another torrent or frozen selection (see Skipped)")
    if scan.root_files:
        flags.append(f"root-level files modified in {year} or before, not selected: {len(scan.root_files)}")
    if os.path.exists(os.path.join(work_dir, f"{torrent_id}.torrent")):
        flags.append(f"{torrent_id}.torrent already exists in --dir")
    for limit_name, unit, limit in LIMITS:
        value = estimate[unit]
        if value > limit:
            flags.append(f"torrent estimate over {limit_name}: {fmt_count(value)} > {fmt_count(limit)} {unit}")

    return {
        "mode": "local",
        "limit": scan.limited,
        "torrent_id": torrent_id,
        "year": year,
        "source": source,
        "run_at": iso(now.timestamp()),
        "exclude": excludes,
        "taken_by": sorted(set(taken.values())),
        "scanned": {"files": scan.scanned[0], "bytes": scan.scanned[1]},
        "selected": {
            "files": len(files),
            "bytes": total,
            "by_mtime": by_mtime,
            "index_json": index_files,
            "newest_last_modified": iso(max(f[2] for f in data_files)) if data_files else None,
            "oldest_last_modified": iso(min(f[2] for f in data_files)) if data_files else None,
        },
        "torrent_estimate": estimate,
        "datasets": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(datasets.items())},
        "years": {str(k): {"files": v[0], "bytes": v[1]} for k, v in sorted(years.items())},
        "flags": flags,
        "skipped": {
            "excluded": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.excluded.items())},
            "already_taken": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.taken.items())},
            "root_files": len(scan.root_files),
            "ignored_all_years": {k: {"files": v[0], "bytes": v[1]} for k, v in sorted(scan.ignored.items())},
            "symlinks": len(scan.symlinks),
        },
    }


def piece_size_for(total):
    """PLAN 2.4: about 4 MiB for ~10 GB, 8 MiB for ~100 GB, 16 MiB for >= 500 GB."""
    if total <= 20 * GIB:
        return 4 * MIB
    if total <= 200 * GIB:
        return 8 * MIB
    return 16 * MIB


def estimate_torrent(files, total):
    """
    Rough size of a hybrid v1+v2 .torrent for these files, and its bdecode token count.

    Hybrid torrents align every file to a piece, so each file gets its own pieces, a pad file in the v1 list, and an
    entry in the v2 file tree. Trackers, comment and the two root files are ignored (small). This is an estimate
    for spotting problems, not an exact number.
    """
    piece = piece_size_for(total)
    size = 1024
    tokens = 50
    pieces = 0
    directories = set()
    for path, file_size, *_ in files:
        parts = path.split("/")
        path_bytes = sum(len(p.encode("utf-8")) + len(str(len(p.encode("utf-8")))) + 1 for p in parts)
        file_pieces = math.ceil(file_size / piece)
        pieces += file_pieces
        # v1 file entry: d6:lengthi<n>e4:pathl...ee
        size += 20 + len(str(file_size)) + path_bytes
        tokens += 5 + len(parts)
        # v1 pad file entry: d4:attr1:p6:lengthi<n>e4:pathl4:.pad<n>:<n>ee
        if file_size % piece:
            size += 50
            tokens += 9
        # v1 piece hashes
        size += 20 * file_pieces
        # v2 file tree leaf: <name>d0:d6:lengthi<n>e11:pieces root32:...ee
        size += 60 + len(str(file_size)) + len(parts[-1].encode("utf-8"))
        tokens += 8
        # v2 piece layers, only for files bigger than one piece
        if file_size > piece:
            size += 36 + 32 * file_pieces
            tokens += 2
        for i in range(1, len(parts)):
            directories.add("/".join(parts[:i]))
    for directory in directories:
        size += len(directory.rsplit("/", 1)[-1].encode("utf-8")) + 4
        tokens += 2
    return {"piece_size": piece, "pieces": pieces, "bytes": size, "tokens": tokens}


def write_file_list(path, scan, base_url):
    with open(path, "w", encoding="utf-8") as f:
        for file_path, size, mtime, _, selected_by, _ in sorted(scan.selected):
            record = {
                "path": file_path,
                "size": size,
                "last_modified": iso(mtime),
                "url": base_url + urllib.parse.quote(file_path),
                "selected_by": selected_by,
            }
            f.write(json.dumps(record) + "\n")


def render_report(summary):
    selected = summary["selected"]
    est = summary["torrent_estimate"]
    lines = []
    lines.append(f"EVE Ref Deep Archive — find up to and including {summary['year']} (local source)")
    lines.append(f"Torrent ID: {summary['torrent_id']}")
    lines.append(f"Source:     {summary['source']}")
    lines.append(f"Exclude:    {', '.join(summary['exclude']) or '(none)'}")
    scanned = summary["scanned"]
    lines.append(f"Scanned:    {fmt_count(scanned['files'])} files, {fmt_bytes(scanned['bytes'])}")
    lines.append("")
    lines.append(
        f"Selected:   {fmt_count(selected['files'])} files, {fmt_bytes(selected['bytes'])} "
        f"(incl. {fmt_count(selected['index_json'])} index.json)"
    )
    if selected["oldest_last_modified"]:
        lines.append(
            f"Modified:   {selected['oldest_last_modified']} to {selected['newest_last_modified']} "
            "(index.json not counted)"
        )
    lines.append(
        f"Estimate:   .torrent {fmt_bytes(est['bytes'])}, {fmt_count(est['tokens'])} tokens, "
        f"{fmt_count(est['pieces'])} pieces of {fmt_bytes(est['piece_size'])}"
    )
    lines.append("")
    lines.append("Years (from the path, or the modification time for files without a year in the path):")
    lines.extend(render_rows([(name, s["files"], s["bytes"]) for name, s in summary["years"].items()]))
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
        rows.append(("root-level files (see find.log)", skipped["root_files"], None))
    rows += [(f"{name}, all years", s["files"], s["bytes"]) for name, s in skipped["ignored_all_years"].items()]
    if skipped["symlinks"]:
        rows.append(("symlinks (not followed), all years", skipped["symlinks"], None))
    if rows:
        lines.append("Skipped:")
        lines.extend(render_rows(rows))
        lines.append("")
    return "\n".join(lines) + "\n"


def render_rows(rows):
    """Label, file count and bytes, aligned."""
    width = max((len(label) for label, _, _ in rows), default=0) + 2
    return [
        f"  {label:<{width}}{fmt_count(files):>12}{'' if size is None else fmt_bytes(size):>12}".rstrip()
        for label, files, size in rows
    ]


def render_log(scan):
    lines = []
    by_mtime = [f[0] for f in scan.selected if f[4] == "mtime"]
    sections = [
        ("selected by modification time (no year in path, the year or before)", by_mtime),
        ("date in path doesn't match year folder", scan.year_mismatch),
        ("empty files", scan.empty),
        ("root-level files modified in the year or before, not selected", scan.root_files),
        ("symlinks not followed (all years)", scan.symlinks),
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
    for unit, factor in (("TiB", 1024**4), ("GiB", GIB), ("MiB", MIB), ("KiB", KIB)):
        if n >= factor:
            return f"{n / factor:.1f} {unit}"
    return f"{n} B"


if __name__ == "__main__":
    main()
