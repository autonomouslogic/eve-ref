#!/usr/bin/env python3
"""
load: a step of the EVE Ref Deep Archive workflow. Copies exactly the files find selected into the torrent's content
folder, SEED_DIR/<ID>/, from the local copy (LOCAL_COPY_DIR, optional) where it has them and from the data site's
bucket otherwise, keeping each file's modification time. Then checks the result and computes the SHA-256 manifest the
rest of the workflow is built on, and the standard `sha256sum`-format checksum file that ends up inside the torrent
(04-sign-sha256 signs it; 05-make-torrent copies both into the content folder).

DRAFT. Reads the bucket and the local copy; writes only to SEED_DIR/<ID>/ and SCRATCH_DIR.

    03-load.py

Needs 01-find's completion file, and refuses to run twice (lib/workflow.py).

1. Read the selection, SCRATCH_DIR/find/files.jsonl, and check it: well-formed site paths, no duplicates, and the
   file count find recorded in its completion file.
2. Compare it with the content folder, if it exists from an earlier attempt: files already there with the listed size
   and modification time are kept, and rclone skips them. Anything in the folder that isn't in the selection aborts
   the step before downloading: it needs looking at by hand.
3. Check there's room in SEED_DIR for what's left to copy.
4. If LOCAL_COPY_DIR is set: take the files still to copy that are there with the selected size and modification
   time, rclone check them against the bucket (SHA-1, where B2 has one; size only where it doesn't), and rclone copy
   --files-from-raw those that pass into SEED_DIR/<ID>/. Every other file is listed and left for the download.
5. rclone copy --files-from-raw the rest from the bucket into SEED_DIR/<ID>/. rclone keeps the modification times,
   skips files already there, and checks each download's SHA-1 where B2 has one.
6. Walk the content folder again and check every listed file is on disk with the listed size and modification time,
   and nothing else is. Any problem aborts the step without a completion file; running it again carries on where it
   stopped.
7. Read every selected file once, computing its SHA-256 and, for .bz2 and .gz files, testing that the compressed
   stream decompresses cleanly (tolerating concatenated streams). An archive test failure is reported but doesn't
   fail the step: corrupt files are archived as they are (README, "Load"); the operator decides whether to accept it
   in NOTES.md before going on.
8. Write SCRATCH_DIR/manifest.json: the selection plus a SHA-256 for every file. 05-make-torrent and
   11-check-before-delete use it.
9. Write SCRATCH_DIR/<ID>.sha256 from the manifest: standard `sha256sum` format ("<hex>  <path>", sorted by path,
   "\n" line endings), one line per file. Stays in SCRATCH_DIR for now; 04-sign-sha256 signs it with minisign, and
   05-make-torrent copies both into the torrent's content folder, so they end up inside the torrent, next to the
   README. Neither is published standalone: checking a file found another way (e.g. a mirror) needs the torrent.
   A SCRATCH_DIR/<ID>.sha256.minisig left by an earlier run is removed first: it signs the old file, so
   04-sign-sha256 has to sign the new one (05-make-torrent refuses to run without it).
10. Run `sha256sum -c` on the file just written, from the content folder: an independent check, with a different
    tool, of the hashes just computed in Python.

Writes, in SCRATCH_DIR:

    load/
      local-candidates.txt  the paths rclone check checks in the local copy (rclone's --files-from-raw list)
      local-check.txt       rclone check's --combined result: "= path" for a match
      local-files-from.txt  the paths copied from the local copy
      files-from.txt        the paths downloaded from the bucket
    manifest.json           the selection plus a SHA-256 for every file (torrent_id, generated_at, files)
    <ID>.sha256             the same hashes, standard sha256sum format
    03-load.done            completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR
    DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY   the bucket and B2 key (lib/workflow.py)
    LOCAL_COPY_DIR   optional: a local copy of the data site, laid out like the bucket

Needs rclone and sha256sum.
"""

import bz2
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import sys
import time
import zlib
from stat import S_ISREG

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MIB,
    Workflow,
    data_remote,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    iso,
    local_copy_dir,
    ok,
    parse_iso,
    rclone_run,
    section,
    site_path_problem,
)

STEP = "load"

# rclone parallelism: the selection is tens of thousands of files, mostly small.
TRANSFERS = 16
CHECKERS = 32
STATS_INTERVAL = "30s"

# Hashing.
CHUNK_SIZE = 4 * MIB
PROGRESS_INTERVAL = 30  # seconds

LOG_SAMPLE = 20

# rclone check --combined: the first character of each line, for the files that don't match.
CHECK_STATUS = {
    "*": "different content from the bucket",
    "+": "not on the bucket",
    "-": "not in the local copy",
    "!": "error while checking",
}


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

    local_dir = local_copy_dir()

    section("Settings")
    print(f"Source: {remote}")
    if local_dir is not None:
        if not os.path.isdir(local_dir):
            fail(f"LOCAL_COPY_DIR does not exist: {local_dir}")
        print(f"Local copy, tried first: {local_dir}")
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
    print(f"To copy: {fmt_count(len(to_copy))} files ({fmt_bytes(to_copy_bytes)}), {fmt_bytes(free)} free")
    if to_copy_bytes > free:
        fail(f"Not enough free space in SEED_DIR: {fmt_bytes(to_copy_bytes)} to copy, {fmt_bytes(free)} free")

    load_dir = os.path.join(workflow.scratch_dir, "load")
    from_local = []
    if local_dir is not None and to_copy:
        section("Local copy")
        os.makedirs(load_dir, exist_ok=True)
        from_local = check_local_copy(local_dir, to_copy, remote, load_dir)
        if from_local:
            os.makedirs(content_dir, exist_ok=True)
            files_from = write_files_from(os.path.join(load_dir, "local-files-from.txt"), from_local)
            print(f"rclone copy {local_dir} -> {content_dir}, {TRANSFERS} transfers, list in {files_from}")
            copy_started = time.monotonic()
            rclone_copy(local_dir, content_dir, files_from)
            print(f"rclone finished in {fmt_duration(time.monotonic() - copy_started)}")
    from_local_paths = {f.path for f in from_local}
    to_download = [f for f in to_copy if f.path not in from_local_paths]

    if to_download:
        section("Download")
        os.makedirs(content_dir, exist_ok=True)
        os.makedirs(load_dir, exist_ok=True)
        files_from = write_files_from(os.path.join(load_dir, "files-from.txt"), to_download)
        print(
            f"rclone copy {remote} -> {content_dir}: {fmt_count(len(to_download))} files "
            f"({fmt_bytes(sum(f.size for f in to_download))}), {TRANSFERS} transfers, list in {files_from}"
        )
        download_started = time.monotonic()
        rclone_copy(remote, content_dir, files_from)
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

    section("Hashing")
    manifest_files = []
    archive_failures = []
    hashing_started = time.monotonic()
    last_progress = hashing_started
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
                f"{fmt_bytes(hashed_bytes)}/{fmt_bytes(total)}, {fmt_duration(now - hashing_started)} elapsed"
            )
            last_progress = now

    if archive_failures:
        section("Archive test failures")
        report_problems("files that failed their archive integrity test", archive_failures)
        print(
            f"{fmt_count(len(archive_failures))} archive test failures. Corrupt files are archived as they are "
            f"(README, 'Load'): accept each one in NOTES.md before going on, or investigate first"
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

    section("Checksum file")
    sha256_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.sha256")
    # A signature from an earlier run is of the old checksum file: remove it, so 04-sign-sha256 has to sign this one.
    minisig_path = sha256_path + ".minisig"
    if os.path.exists(minisig_path):
        os.remove(minisig_path)
        print(f"Removed the earlier run's signature, sign again with 04-sign-sha256: {minisig_path}")
    write_sha256sums(sha256_path, manifest_files)
    print(f"{fmt_count(len(manifest_files))} files in {sha256_path}")
    verify_sha256sums(sha256_path, content_dir)
    print(f"sha256sum -c: all {fmt_count(len(manifest_files))} files OK")

    workflow.complete(
        STEP,
        torrent_id,
        {
            "content_dir": content_dir,
            "manifest": manifest_path,
            "sha256": sha256_path,
            "files": len(files),
            "bytes": total,
            "local_copy_files": len(from_local),
            "downloaded_files": len(to_download),
            "archive_test_failures": len(archive_failures),
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Load OK for {torrent_id}")


def check_local_copy(local_dir, files, remote, load_dir):
    """
    The files to copy from the local copy instead of the bucket: those there with the selected size and modification
    time, whose content rclone check confirms against the bucket's SHA-1 (where B2 has one). Every other file is left
    for the download.
    """
    candidates = []
    missing = []
    different = []
    for f in files:
        try:
            stat = os.lstat(os.path.join(local_dir, f.path))
        except FileNotFoundError:
            missing.append(f.path)
            continue
        if not S_ISREG(stat.st_mode):
            different.append(f"{f.path}: not a regular file")
        elif stat.st_size != f.size:
            different.append(f"{f.path}: {fmt_count(stat.st_size)} bytes, selected {fmt_count(f.size)}")
        elif int(stat.st_mtime) != f.mtime:
            different.append(f"{f.path}: modified {iso(stat.st_mtime)}, selected {iso(f.mtime)}")
        else:
            candidates.append(f)
    print(
        f"{fmt_count(len(candidates))} files there with the selected size and modification time, "
        f"{fmt_count(len(missing))} missing, {fmt_count(len(different))} different"
    )
    if different:
        report_problems("files in the local copy that differ from the selection, downloaded instead", different)
    if not candidates:
        return []

    candidates_from = write_files_from(os.path.join(load_dir, "local-candidates.txt"), candidates)
    combined = os.path.join(load_dir, "local-check.txt")
    print(f"rclone check {local_dir} against {remote}, list in {candidates_from}")
    check_started = time.monotonic()
    code = rclone_run(
        "check",
        local_dir,
        remote,
        "--one-way",
        "--files-from-raw",
        candidates_from,
        "--combined",
        combined,
        "--checkers",
        str(CHECKERS),
        check=False,
    )
    print(f"rclone finished in {fmt_duration(time.monotonic() - check_started)}")
    status = {}
    if os.path.isfile(combined):
        with open(combined, encoding="utf-8") as f:
            for line in f:
                line = line.rstrip("\n")
                if len(line) > 2 and line[1] == " ":
                    status[line[2:]] = line[0]
    verified = []
    rejected = []
    for f in candidates:
        state = status.get(f.path)
        if state == "=":
            verified.append(f)
        else:
            rejected.append(f"{f.path}: {CHECK_STATUS.get(state, 'not checked')}")
    if code != 0 and not rejected:
        fail(f"rclone check failed with exit code {code}, but reported no differences")
    if rejected:
        report_problems("files in the local copy that failed rclone check, downloaded instead", rejected)
    print(f"From the local copy: {fmt_count(len(verified))} files ({fmt_bytes(sum(f.size for f in verified))})")
    return verified


def write_sha256sums(path, manifest_files):
    """Writes the standard sha256sum format: "<hex>  <path>", sorted by path, one line per file."""
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        for entry in sorted(manifest_files, key=lambda e: e["path"]):
            f.write(f"{entry['sha256']}  {entry['path']}\n")
    os.rename(tmp, path)


def verify_sha256sums(sha256_path, content_dir):
    """Runs sha256sum -c on the just-written checksum file, from content_dir (the paths are relative to it). Fails
    with sha256sum's own output on any mismatch: a second, independent check of the hashes just computed in Python."""
    result = subprocess.run(
        ["sha256sum", "-c", "--strict", sha256_path], cwd=content_dir, capture_output=True, text=True
    )
    if result.returncode != 0:
        for line in result.stdout.splitlines() + result.stderr.splitlines():
            if line.strip():
                error(f"  {line}")
        fail(f"sha256sum -c failed for {sha256_path}")


def write_files_from(path, files):
    """Writes an rclone --files-from-raw list of the files' paths, and returns its path."""
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(o.path + "\n" for o in files)
    return path


def rclone_copy(source, content_dir, files_from):
    """rclone copy of exactly the listed files, keeping modification times and skipping files already there."""
    rclone_run(
        "copy",
        source,
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


if __name__ == "__main__":
    main()
