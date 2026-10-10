#!/usr/bin/env python3
"""
load: step 3 of the EVE Ref Deep Archive workflow. Downloads exactly the files find selected from the data site's
bucket into the torrent's content folder, SEED_DIR/<ID>/, keeping each file's modification time.

DRAFT. Reads the bucket; writes only to SEED_DIR/<ID>/ and SCRATCH_DIR.

    02-load.py

Needs 01-find's completion file, and refuses to run twice (lib/workflow.py).

1. Read the selection, SCRATCH_DIR/find/files.jsonl, and check it: well-formed site paths, no duplicates, and the
   file count find recorded in its completion file.
2. Compare it with the content folder, if it exists from an earlier attempt: files already there with the listed size
   and modification time are kept, and rclone skips them. Anything in the folder that isn't in the selection aborts
   the step before downloading: it needs looking at by hand.
3. Check there's room in SEED_DIR for what's left to download.
4. rclone copy --files-from-raw from the bucket into SEED_DIR/<ID>/. rclone keeps the modification times, skips files
   already there, and checks each download's SHA-1 where B2 has one.
5. Check every listed file is on disk with the listed size and modification time, and nothing else is. Any problem
   aborts the step without a completion file; running it again carries on where it stopped.

03-check-load checks the result again from scratch, and computes the SHA-256 manifest.

Writes, in SCRATCH_DIR:

    load/
      files-from.txt       the selected paths, one per line: rclone's --files-from-raw list
    02-load.done           completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
    DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY   the bucket and B2 key (lib/workflow.py)

Needs rclone.
"""

import datetime
import json
import os
import shutil
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    GIB,
    KIB,
    MIB,
    TIB,
    Workflow,
    data_remote,
    error,
    fail,
    ok,
    rclone_run,
    section,
    site_path_problem,
)

STEP = "load"

# rclone parallelism: the selection is tens of thousands of files, mostly small.
TRANSFERS = 16
CHECKERS = 32
STATS_INTERVAL = "30s"

LOG_SAMPLE = 20


class Selected:
    """One selected file. mtime is whole seconds, as files.jsonl records it."""

    __slots__ = ("path", "size", "mtime")

    def __init__(self, path, size, mtime):
        self.path = path
        self.size = size
        self.mtime = mtime


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    remote = data_remote()
    find_dir = os.path.join(workflow.scratch_dir, "find")
    content_dir = os.path.join(workflow.seed_dir, torrent_id)

    section("Settings")
    print(f"Source: {remote}")
    print(f"Content folder: {content_dir}")

    section("Selection")
    selection_path = os.path.join(find_dir, "files.jsonl")
    files = read_selection(selection_path, workflow.read_done("find"))
    total = sum(f.size for f in files)
    print(f"{fmt_count(len(files))} files ({fmt_bytes(total)}) in {selection_path}")

    section("Content folder")
    if os.path.isdir(content_dir):
        state = compare(content_dir, files)
        print(
            f"Exists from an earlier attempt: {fmt_count(len(state.matching))} files already there, "
            f"{fmt_count(len(state.missing))} missing, {fmt_count(len(state.different))} different"
        )
        if state.extra:
            report_problems("files that aren't in the selection", state.extra)
            fail(f"{content_dir} holds {fmt_count(len(state.extra))} files that aren't selected: resolve by hand")
        to_copy = [f for f in files if f.path not in state.matching]
    elif os.path.exists(content_dir):
        fail(f"{content_dir} exists and isn't a folder")
    else:
        print("New")
        to_copy = files
    to_copy_bytes = sum(f.size for f in to_copy)
    free = shutil.disk_usage(workflow.seed_dir).free
    print(f"To download: {fmt_count(len(to_copy))} files ({fmt_bytes(to_copy_bytes)}), {fmt_bytes(free)} free")
    if to_copy_bytes > free:
        fail(f"Not enough free space in SEED_DIR: {fmt_bytes(to_copy_bytes)} to download, {fmt_bytes(free)} free")

    if to_copy:
        section("Download")
        os.makedirs(content_dir, exist_ok=True)
        load_dir = os.path.join(workflow.scratch_dir, "load")
        os.makedirs(load_dir, exist_ok=True)
        files_from = os.path.join(load_dir, "files-from.txt")
        with open(files_from, "w", encoding="utf-8") as f:
            f.writelines(o.path + "\n" for o in files)
        print(f"rclone copy {remote} -> {content_dir}, {TRANSFERS} transfers, list in {files_from}")
        download_started = time.monotonic()
        rclone_run(
            "copy",
            remote,
            content_dir,
            "--files-from-raw",
            files_from,
            "--transfers",
            str(TRANSFERS),
            "--checkers",
            str(CHECKERS),
            "--stats",
            STATS_INTERVAL,
            "--stats-one-line",
            "--stats-log-level",
            "NOTICE",
        )
        print(f"rclone finished in {fmt_duration(time.monotonic() - download_started)}")

    section("Result")
    state = compare(content_dir, files)
    problems = 0
    for title, items in (
        ("missing files", state.missing),
        ("files with a different size or modification time", state.different),
        ("files that aren't in the selection", state.extra),
    ):
        if items:
            problems += len(items)
            report_problems(title, items)
    if problems:
        fail(f"{fmt_count(problems)} problems in {content_dir}. Run load again to retry, or resolve by hand")
    print(f"{fmt_count(len(state.matching))} files ({fmt_bytes(total)}) in {content_dir}, all as selected")

    workflow.complete(
        STEP,
        torrent_id,
        {"content_dir": content_dir, "files": len(files), "bytes": total, "downloaded_files": len(to_copy)},
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Load OK for {torrent_id}")


def read_selection(path, find_done):
    """The selected files from files.jsonl, checked. Fails on any problem, listing every one."""
    if not os.path.isfile(path):
        fail(f"Selection not found: {path}")
    files = []
    seen = set()
    problems = []
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            try:
                record = json.loads(line)
                file_path = record["path"]
                size = record["size"]
                mtime = parse_iso(record["last_modified"])
            except (ValueError, KeyError, TypeError) as e:
                problems.append(f"line {number}: not a selection record ({e})")
                continue
            problem = site_path_problem(file_path)
            if problem is None and file_path in seen:
                problem = "duplicate"
            if problem is None and (not isinstance(size, int) or size < 0):
                problem = f"bad size {size!r}"
            if problem is not None:
                problems.append(f"line {number}: {problem}: {file_path!r}")
                continue
            seen.add(file_path)
            files.append(Selected(file_path, size, mtime))
    expected = find_done["summary"]["files"]
    if not problems and len(files) != expected:
        problems.append(f"{fmt_count(len(files))} files, but find selected {fmt_count(expected)}")
    if not problems and not files:
        problems.append("no files")
    if problems:
        for problem in problems[:LOG_SAMPLE]:
            error(f"  {problem}")
        fail(f"{len(problems)} problems in {path}")
    return files


def parse_iso(value):
    """files.jsonl's last_modified, YYYY-MM-DDTHH:MM:SSZ, as a POSIX timestamp."""
    parsed = datetime.datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")
    return int(parsed.replace(tzinfo=datetime.timezone.utc).timestamp())


class State:
    def __init__(self):
        self.matching = set()  # paths on disk with the listed size and modification time
        self.missing = []
        self.different = []  # "path: why"
        self.extra = []  # paths on disk that aren't selected, symlinks and other non-files included


def compare(content_dir, files):
    """The content folder against the selection, by size and modification time (to the second)."""
    state = State()
    selected = {f.path: f for f in files}
    on_disk = set()
    for dirpath, dirnames, filenames in os.walk(content_dir, followlinks=False):
        rel_dir = os.path.relpath(dirpath, content_dir)
        rel_dir = "" if rel_dir == "." else rel_dir.replace(os.sep, "/")
        # Symlinks to folders are listed in dirnames, and never walked into.
        for name in sorted(filenames + [d for d in dirnames if os.path.islink(os.path.join(dirpath, d))]):
            path = f"{rel_dir}/{name}" if rel_dir else name
            full = os.path.join(dirpath, name)
            f = selected.get(path)
            if f is None or os.path.islink(full) or not os.path.isfile(full):
                state.extra.append(path)
                continue
            on_disk.add(path)
            stat = os.stat(full)
            if stat.st_size != f.size:
                state.different.append(f"{path}: {fmt_count(stat.st_size)} bytes, selected {fmt_count(f.size)}")
            elif int(stat.st_mtime) != f.mtime:
                state.different.append(f"{path}: modified {iso(stat.st_mtime)}, selected {iso(f.mtime)}")
            else:
                state.matching.add(path)
    state.missing = [f.path for f in files if f.path not in on_disk]
    return state


def report_problems(title, items):
    error(f"{title}: {fmt_count(len(items))}, e.g.")
    for item in items[:LOG_SAMPLE]:
        error(f"  {item}")


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
