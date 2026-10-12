#!/usr/bin/env python3
"""
check-load: step 4 of the EVE Ref Deep Archive workflow. Checks 02-load's result again from scratch, then computes
the SHA-256 manifest the rest of the workflow is built on.

DRAFT. Read-only on SEED_DIR/<ID>/; writes only manifest.json and SCRATCH_DIR.

    03-check-load.py

Needs 02-load's completion file, and refuses to run twice (lib/workflow.py).

1. Read the selection, SCRATCH_DIR/find/files.jsonl (the same source 02-load used), and check it.
2. Walk SEED_DIR/<ID>/ itself (never trusting 02-load's own result) and compare it with the selection, by size and
   modification time. Any file missing, different, or on disk but not selected, fails the step.
3. Read every selected file once, computing its SHA-256 and, for .bz2 and .gz files, testing that the compressed
   stream decompresses cleanly (tolerating concatenated streams). An archive test failure is reported but doesn't
   fail the step: corrupt files are archived as they are (README, "Check load"); the operator decides whether to
   accept it in NOTES.md before going on.
4. Write SCRATCH_DIR/manifest.json: the selection plus a SHA-256 for every file. 04-make-checksums builds <ID>.sha256
   from it; 05-check-checksums re-reads all the data again to confirm it.

Writes, in SCRATCH_DIR:

    manifest.json           the selection plus a SHA-256 for every file (torrent_id, generated_at, files)
    03-check-load.done      completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
"""

import bz2
import datetime
import hashlib
import json
import os
import sys
import time
import zlib

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MIB,
    Workflow,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
    site_path_problem,
)

STEP = "check-load"

CHUNK_SIZE = 4 * MIB
PROGRESS_INTERVAL = 30  # seconds
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
    content_dir = os.path.join(workflow.seed_dir, torrent_id)
    find_dir = os.path.join(workflow.scratch_dir, "find")

    section("Settings")
    print(f"Content folder: {content_dir}")

    section("Selection")
    selection_path = os.path.join(find_dir, "files.jsonl")
    files = read_selection(selection_path, workflow.read_done("find"))
    total_bytes = sum(f.size for f in files)
    print(f"{fmt_count(len(files))} files ({fmt_bytes(total_bytes)}) in {selection_path}")

    section("Content folder")
    if not os.path.isdir(content_dir):
        fail(f"{content_dir} does not exist: run 02-load first")
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
        fail(f"{fmt_count(problems)} problems in {content_dir}. Run 02-load again, or resolve by hand")
    print(f"{fmt_count(len(state.matching))} files ({fmt_bytes(total_bytes)}) match the selection")

    section("Hashing")
    manifest_files = []
    archive_failures = []
    last_progress = started
    hashed_bytes = 0
    for index, f in enumerate(sorted(files, key=lambda s: s.path), 1):
        full = os.path.join(content_dir, f.path)
        digest, archive_error = hash_and_test(full)
        manifest_files.append({"path": f.path, "size": f.size, "last_modified": iso(f.mtime), "sha256": digest})
        hashed_bytes += f.size
        if archive_error is not None:
            archive_failures.append(f"{f.path}: {archive_error}")
        now = time.monotonic()
        if now - last_progress >= PROGRESS_INTERVAL or index == len(files):
            print(
                f"{fmt_count(index)}/{fmt_count(len(files))} files, "
                f"{fmt_bytes(hashed_bytes)}/{fmt_bytes(total_bytes)}, {fmt_duration(now - started)} elapsed"
            )
            last_progress = now

    if archive_failures:
        section("Archive test failures")
        report_problems("files that failed their archive integrity test", archive_failures)
        print(
            f"{fmt_count(len(archive_failures))} archive test failures. Corrupt files are archived as they are "
            f"(README, 'Check load'): accept each one in NOTES.md before going on, or investigate first"
        )

    section("Manifest")
    manifest_path = os.path.join(workflow.scratch_dir, "manifest.json")
    manifest = {
        "torrent_id": torrent_id,
        "generated_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "files": manifest_files,
    }
    tmp = manifest_path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as out:
        out.write(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    os.rename(tmp, manifest_path)
    print(f"{fmt_count(len(manifest_files))} files in {manifest_path}")

    workflow.complete(
        STEP,
        torrent_id,
        {
            "content_dir": content_dir,
            "manifest": manifest_path,
            "files": len(manifest_files),
            "bytes": total_bytes,
            "archive_test_failures": len(archive_failures),
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check load OK for {torrent_id}")


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
    """
    The content folder against the selection, by size and modification time (to the second). Re-walks the folder
    from scratch: never trusts 02-load's own result (lib/workflow.py, "Every action has a separate check step").
    """
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


def hash_and_test(path):
    """
    SHA-256 of the file and, for .bz2/.gz files, an archive test error (or None): reads the file once, feeding
    every chunk to both the digest and the decompression test.
    """
    sha256 = hashlib.sha256()
    tester = _archive_tester(path)
    archive_error = None
    with open(path, "rb") as f:
        while True:
            chunk = f.read(CHUNK_SIZE)
            if not chunk:
                break
            sha256.update(chunk)
            if tester is not None and archive_error is None:
                try:
                    tester.feed(chunk)
                except Exception as e:
                    archive_error = str(e) or type(e).__name__
    if tester is not None and archive_error is None and not tester.complete():
        archive_error = "truncated: the compressed stream doesn't end"
    return sha256.hexdigest(), archive_error


def _archive_tester(path):
    """A streaming decompression test for the file's extension, or None if it isn't one we test."""
    if path.endswith(".bz2"):
        return _Bz2Tester()
    if path.endswith(".gz"):
        return _GzipTester()
    return None


class _Bz2Tester:
    """Feeds raw bytes through bz2 decompression, tolerating concatenated streams."""

    def __init__(self):
        self._decompressor = bz2.BZ2Decompressor()

    def feed(self, chunk):
        while chunk:
            self._decompressor.decompress(chunk)
            if not self._decompressor.eof:
                break
            chunk = self._decompressor.unused_data
            if chunk:
                self._decompressor = bz2.BZ2Decompressor()

    def complete(self):
        return self._decompressor.eof


class _GzipTester:
    """Feeds raw bytes through gzip (zlib) decompression, tolerating concatenated members."""

    def __init__(self):
        self._decompressor = zlib.decompressobj(zlib.MAX_WBITS | 16)

    def feed(self, chunk):
        while chunk:
            self._decompressor.decompress(chunk)
            if not self._decompressor.eof:
                break
            chunk = self._decompressor.unused_data
            if chunk:
                self._decompressor = zlib.decompressobj(zlib.MAX_WBITS | 16)

    def complete(self):
        return self._decompressor.eof


def report_problems(title, items):
    error(f"{title}: {fmt_count(len(items))}, e.g.")
    for item in items[:LOG_SAMPLE]:
        error(f"  {item}")


def iso(timestamp):
    return datetime.datetime.fromtimestamp(timestamp, datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


if __name__ == "__main__":
    main()
