#!/usr/bin/env python3
"""
load: a step of the EVE Ref Deep Archive workflow. Copies exactly the files find selected into the torrent's staging
folder, SEED_DIR/.staging/<ID>/, from the local copy (LOCAL_COPY_DIR, optional) where it has them and from the data
site's bucket otherwise, keeping each file's modification time. Then checks the result, snapshots the selected
folders' index.json into <ID>-index.tar.xz, and computes the SHA-256 manifest the rest of the workflow is built on,
and the standard `sha256sum`-format checksum file that ends up inside the torrent (04-sign-sha256 signs it;
05-make-torrent copies both into the staging folder).

DRAFT. Reads the bucket and the local copy; writes only to SEED_DIR/.staging/<ID>/ and SCRATCH_DIR.

    03-load.py

Needs 01-find's completion file, and refuses to run twice (lib/workflow.py).

1. Read the selection, SCRATCH_DIR/find/files.jsonl, and check it: well-formed site paths, no duplicates, and the
   file count find recorded in its completion file.
2. Compare it with the staging folder, if it exists from an earlier attempt: files already there with the listed size
   and modification time are kept. Anything in the folder that isn't in the selection aborts the step before
   downloading: it needs looking at by hand.
3. Check there's room in SEED_DIR for what's left to copy.
4. If LOCAL_COPY_DIR is set, copy every file still to copy that the local copy has into SEED_DIR/.staging/<ID>/, as
   it is, unchecked. The local copy is only a shortcut, never trusted: old, changed or missing files in it are
   expected, and step 5 replaces any that don't match the bucket. Two passes:
   a. `cp -p --reflink=always` of the files not yet in the staging folder. When the local copy is on the same
      BtrFS filesystem as SEED_DIR, that clones the extents instead of copying the data: almost no space or time.
      If cp fails (another filesystem, no reflink support) it's a warning, and the next pass copies the data.
   b. rclone copy --files-from-raw of the same files: skips what the reflink copy already put there (same size and
      modification time), and copies the rest.
5. rclone copy --files-from-raw SCRATCH_DIR/find/files.txt (the whole selection, written by 01-find) from the
   bucket into SEED_DIR/.staging/<ID>/: rclone compares every file already there with the bucket by size and
   modification time, and downloads those that differ or are missing, keeping the bucket's modification times.
6. Walk the staging folder again and check every listed file is on disk with the listed size and modification time,
   and nothing else is. Any problem aborts the step without a completion file; running it again carries on where it
   stopped.
7. Read every selected file once, computing its SHA-256 and, for .bz2 and .gz files, testing that the compressed
   stream decompresses cleanly (tolerating concatenated streams). HASH_WORKERS files at a time: hashlib releases the
   GIL during update(), so threads parallelize the CPU work (NOTES.md "Torrent size": ~800 MB/s at 6 threads against
   186 MB/s on one). An archive test failure is reported but doesn't fail the step: corrupt files are archived as
   they are (README, "Load"); the operator decides whether to accept it before going on.
8. Download find/indexes.jsonl's files fresh from the bucket into $SCRATCH_DIR/load/index/, check each one covers
   every selected file in its folder with the selected size and modification time, and tar+xz them into the staging
   folder's <ID>-index.tar.xz (NOTES.md "Index snapshot"), each member named by its site path. A file the
   snapshot doesn't cover fails the step: run DataIndex for that prefix, then load again (resumable).
9. Write SCRATCH_DIR/manifest.json: the selection plus a SHA-256 for every file, with each file's size and
   modification time as on disk. 05-make-torrent and 12-check-before-delete use it. The index tarball isn't a data
   file and isn't in the manifest.
10. Write SCRATCH_DIR/<ID>.sha256 from the manifest plus the index tarball: standard `sha256sum` format
    ("<hex>  <path>", sorted by path, "\n" line endings), one line per file. Stays in SCRATCH_DIR for now;
    04-sign-sha256 signs it with minisign, and 05-make-torrent copies both into the torrent's staging folder, so
    they end up inside the torrent, next to the README. Neither is published standalone: checking a file found
    another way (e.g. a mirror) needs the torrent. A SCRATCH_DIR/<ID>.sha256.minisig left by an earlier run is
    removed first: it signs the old file, so 04-sign-sha256 has to sign the new one (05-make-torrent refuses to run
    without it).
11. Run `sha256sum -c` on the file just written, from the staging folder: an independent check, with a different
    tool, of the hashes just computed in Python.

Writes, in SCRATCH_DIR:

    load/
      local-files-from.txt  the paths copied from the local copy
      index/                 the downloaded index.json snapshot, before it's tarred up
    manifest.json           the selection plus a SHA-256 for every file (torrent_id, generated_at, files)
    <ID>.sha256             the same hashes, standard sha256sum format, index tarball included
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
import tarfile
import time
import zlib
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor

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
    index_tar_name,
    iso,
    local_copy_dir,
    ok,
    parse_iso,
    parse_modtime,
    rclone_run,
    root_file_names,
    section,
    site_path_problem,
    verify_sha256sums,
    warning,
)

STEP = "load"

# rclone parallelism: the selection is tens of thousands of files, mostly small.
TRANSFERS = 16
CHECKERS = 32
STATS_INTERVAL = "30s"

# Hashing.
CHUNK_SIZE = 4 * MIB
HASH_WORKERS = os.cpu_count() or 4  # hashlib releases the GIL; NOTES.md "Torrent size" measured ~4.3x at 6 threads
PROGRESS_INTERVAL = 30  # seconds

# Files per cp call, to stay well under the argument length limit.
REFLINK_BATCH = 500

LOG_SAMPLE = 20


def folder_of(path):
    return path.rsplit("/", 1)[0] if "/" in path else ""


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
    content_dir = workflow.content_dir(torrent_id)
    root_names = root_file_names(torrent_id)

    local_dir = local_copy_dir()

    section("Settings")
    print(f"Source: {remote}")
    if local_dir is not None:
        if not os.path.isdir(local_dir):
            fail(f"LOCAL_COPY_DIR does not exist: {local_dir}")
        print(f"Local copy, tried first: {local_dir}")
    print(f"Staging folder: {content_dir}")

    section("Selection")
    selection_path = os.path.join(find_dir, "files.jsonl")
    files = read_selection(selection_path, workflow.read_done("find"))
    total = sum(f.size for f in files)
    print(f"{fmt_count(len(files))} files ({fmt_bytes(total)}) in {selection_path}")

    section("Staging folder")
    if os.path.isdir(content_dir):
        state = compare(content_dir, files, root_names)
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
    os.makedirs(load_dir, exist_ok=True)
    os.makedirs(content_dir, exist_ok=True)
    from_local = []
    if local_dir is not None and to_copy:
        section("Local copy")
        from_local = [f for f in to_copy if os.path.isfile(os.path.join(local_dir, f.path))]
        print(
            f"{fmt_count(len(from_local))} files ({fmt_bytes(sum(f.size for f in from_local))}) in the local copy, "
            f"copied unchecked: the bucket copy replaces any that don't match"
        )
        if from_local:
            reflink_started = time.monotonic()
            reflinked = reflink_copy(local_dir, content_dir, [f for f in from_local if not exists_in(content_dir, f)])
            if reflinked is not None:
                took = fmt_duration(time.monotonic() - reflink_started)
                print(f"cp --reflink=always: {fmt_count(reflinked)} files in {took}")
            files_from = write_files_from(os.path.join(load_dir, "local-files-from.txt"), from_local)
            print(f"rclone copy {local_dir} -> {content_dir}, {TRANSFERS} transfers, list in {files_from}")
            copy_started = time.monotonic()
            rclone_copy(local_dir, content_dir, files_from)
            print(f"rclone finished in {fmt_duration(time.monotonic() - copy_started)}")

    section("Bucket")
    files_from = os.path.join(find_dir, "files.txt")
    print(
        f"rclone copy {remote} -> {content_dir}: all {fmt_count(len(files))} files, "
        f"size and mtime, {TRANSFERS} transfers, list in {files_from}"
    )
    download_started = time.monotonic()
    rclone_copy(remote, content_dir, files_from)
    print(f"rclone finished in {fmt_duration(time.monotonic() - download_started)}")

    section("Result")
    state = compare(content_dir, files, root_names)
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

    section("Index snapshot")
    index_list_path = os.path.join(find_dir, "indexes.jsonl")
    indexes = read_index_list(index_list_path)
    index_dir = os.path.join(load_dir, "index")
    os.makedirs(index_dir, exist_ok=True)
    if indexes:
        index_files_from = write_files_from(os.path.join(load_dir, "index-files-from.txt"), indexes)
        print(f"rclone copy {remote} -> {index_dir}: {fmt_count(len(indexes))} index.json files, fresh from the bucket")
        rclone_copy(remote, index_dir, index_files_from)
    else:
        print("No index.json files to snapshot")
    by_folder = defaultdict(list)
    for f in files:
        by_folder[folder_of(f.path)].append(f)
    coverage_problems = check_index_coverage(index_dir, by_folder)
    if coverage_problems:
        report_problems("selected files not covered by the downloaded index.json snapshot", coverage_problems)
        fail(
            f"{fmt_count(len(coverage_problems))} selected files aren't covered by their folder's index.json: run "
            "DataIndex for that prefix, then run load again (resumable)"
        )
    print(f"All {fmt_count(len(files))} selected files covered by {fmt_count(len(indexes))} index.json files")

    tar_name = index_tar_name(torrent_id)
    tar_path = os.path.join(content_dir, tar_name)
    write_index_tar(tar_path, index_dir, indexes)
    print(f"Written: {tar_path} ({fmt_count(len(indexes))} files)")
    check_index_tar(tar_path, index_dir, indexes)
    print(f"{tar_path}: members, sizes and modification times match the downloaded index.json files")

    section("Hashing")
    manifest_files = []
    archive_failures = []
    hashing_started = time.monotonic()
    last_progress = hashing_started
    hashed_bytes = 0
    sorted_files = sorted(files, key=lambda s: s.path)
    print(f"{HASH_WORKERS} threads")
    with ThreadPoolExecutor(max_workers=HASH_WORKERS) as executor:
        for index, (f, digest, archive_error) in enumerate(
            executor.map(lambda f: (f, *hash_and_test(os.path.join(content_dir, f.path))), sorted_files), 1
        ):
            full = os.path.join(content_dir, f.path)
            # As on disk: the same as selected, except for an index.json that changed since (compare).
            stat = os.stat(full)
            manifest_files.append(
                {"path": f.path, "size": stat.st_size, "last_modified": iso(stat.st_mtime), "sha256": digest}
            )
            hashed_bytes += stat.st_size
            if archive_error is not None:
                archive_failures.append(f"{f.path}: {archive_error}")
            now = time.monotonic()
            if now - last_progress >= PROGRESS_INTERVAL or index == len(sorted_files):
                elapsed = now - hashing_started
                speed = hashed_bytes / elapsed if elapsed > 0 else 0
                eta = (total - hashed_bytes) / speed if speed > 0 else 0
                print(
                    f"{fmt_count(index)}/{fmt_count(len(sorted_files))} files, "
                    f"{fmt_bytes(hashed_bytes)}/{fmt_bytes(total)}, {fmt_duration(elapsed)} elapsed, "
                    f"{fmt_bytes(speed)}/s, ETA {fmt_duration(eta)}"
                )
                last_progress = now

    if archive_failures:
        section("Archive test failures")
        report_problems("files that failed their archive integrity test", archive_failures, warning)
        warning(
            f"{fmt_count(len(archive_failures))} archive test failures. Corrupt files are archived as they are "
            f"(README, 'Load'): accept each one before going on, or investigate first"
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
    tar_entry = {"path": tar_name, "sha256": sha256_of(tar_path)}
    write_sha256sums(sha256_path, manifest_files + [tar_entry])
    print(f"{fmt_count(len(manifest_files) + 1)} files in {sha256_path}") # +1 because it includes the index tar
    verify_sha256sums(sha256_path, content_dir)
    print(f"sha256sum -c: all {fmt_count(len(manifest_files) + 1)} files OK")

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
            "archive_test_failures": len(archive_failures),
            "index_snapshot_files": len(indexes),
            "index_tar": tar_path,
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Load OK for {torrent_id}")


def sha256_of(path):
    """SHA-256 of a file, read in CHUNK_SIZE pieces (for the index tarball, outside the manifest's own hashing)."""
    sha256 = hashlib.sha256()
    with open(path, "rb") as f:
        while True:
            chunk = f.read(CHUNK_SIZE)
            if not chunk:
                break
            sha256.update(chunk)
    return sha256.hexdigest()


def read_index_list(path):
    """find/indexes.jsonl: the index.json files to snapshot, same record shape as files.jsonl (Selected)."""
    if not os.path.isfile(path):
        fail(f"Index list not found: {path}")
    files = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            record = json.loads(line)
            files.append(Selected(record["path"], record["size"], parse_iso(record["last_modified"])))
    return files


def check_index_coverage(index_dir, by_folder):
    """
    "<path>: <why>" for every selected file (by_folder: folder -> [Selected]) that the downloaded index.json snapshot
    (index_dir, laid out like the bucket) doesn't cover with its own size and modification time (to the second):
    missing index.json, invalid JSON, the file not listed, or a size/last_modified mismatch. Matches by file name
    within the folder's "files" list (DataIndex's IndexFileEntry: name, size, last_modified).
    """
    problems = []
    for folder, selected_files in sorted(by_folder.items()):
        index_path = os.path.join(index_dir, *folder.split("/"), "index.json") if folder else os.path.join(
            index_dir, "index.json"
        )
        if not os.path.isfile(index_path):
            problems.extend(f"{f.path}: no index.json downloaded for its folder" for f in selected_files)
            continue
        with open(index_path, encoding="utf-8") as fh:
            try:
                data = json.load(fh)
            except ValueError as e:
                problems.extend(f"{f.path}: {folder}/index.json is invalid JSON ({e})" for f in selected_files)
                continue
        entries = {e["name"]: e for e in data.get("files", [])}
        for f in selected_files:
            name = f.path.rsplit("/", 1)[-1]
            entry = entries.get(name)
            if entry is None:
                problems.append(f"{f.path}: not listed in {folder}/index.json")
                continue
            if entry.get("size") != f.size:
                problems.append(f"{f.path}: index.json size {entry.get('size')}, selected {f.size}")
                continue
            try:
                entry_mtime = int(parse_modtime(entry["last_modified"]))
            except (KeyError, ValueError):
                problems.append(f"{f.path}: index.json has no usable last_modified")
                continue
            if entry_mtime != f.mtime:
                problems.append(
                    f"{f.path}: index.json last_modified {entry.get('last_modified')}, selected {iso(f.mtime)}"
                )
    return problems


def write_index_tar(tar_path, index_dir, indexes):
    """
    Writes the index snapshot tarball (NOTES.md "Index snapshot"): every downloaded index.json, named by its site
    path so extracting the tarball into the site's root puts each one back in place, PAX format, members in path
    order, each with the downloaded file's own modification time, uid/gid 0, no uname/gname, mode 0644. The same
    inputs give the same bytes.
    """
    tmp = tar_path + ".tmp"
    with tarfile.open(tmp, "w:xz", format=tarfile.PAX_FORMAT) as tar:
        for entry in sorted(indexes, key=lambda o: o.path):
            full = os.path.join(index_dir, *entry.path.split("/"))
            info = tar.gettarinfo(full, arcname=entry.path)
            info.uid = 0
            info.gid = 0
            info.uname = ""
            info.gname = ""
            info.mode = 0o644
            info.mtime = int(os.stat(full).st_mtime)
            with open(full, "rb") as f:
                tar.addfile(info, f)
    os.rename(tmp, tar_path)


def check_index_tar(tar_path, index_dir, indexes):
    """Reopens the tarball and checks its members' names, sizes and modification times against the downloaded files."""
    with tarfile.open(tar_path, "r:xz") as tar:
        members = {m.name: m for m in tar.getmembers()}
    expected_names = {entry.path for entry in indexes}
    problems = []
    if set(members) != expected_names:
        extra = sorted(set(members) - expected_names)
        missing = sorted(expected_names - set(members))
        if extra:
            problems.append(f"extra members: {extra[:LOG_SAMPLE]}")
        if missing:
            problems.append(f"missing members: {missing[:LOG_SAMPLE]}")
    for entry in indexes:
        name = entry.path
        member = members.get(name)
        if member is None:
            continue
        full = os.path.join(index_dir, *entry.path.split("/"))
        stat = os.stat(full)
        if member.size != stat.st_size:
            problems.append(f"{name}: tar size {member.size}, file {stat.st_size}")
        if member.mtime != int(stat.st_mtime):
            problems.append(f"{name}: tar mtime {member.mtime}, file {int(stat.st_mtime)}")
    if problems:
        for p in problems[:LOG_SAMPLE]:
            error(f"  {p}")
        fail(f"{tar_path} doesn't match the downloaded index.json files: {len(problems)} problems")


def write_sha256sums(path, manifest_files):
    """Writes the standard sha256sum format: "<hex>  <path>", sorted by path, one line per file."""
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        for entry in sorted(manifest_files, key=lambda e: e["path"]):
            f.write(f"{entry['sha256']}  {entry['path']}\n")
    os.rename(tmp, path)


def write_files_from(path, files):
    """Writes an rclone --files-from-raw list of the files' paths, and returns its path."""
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(o.path + "\n" for o in files)
    return path


def exists_in(content_dir, selected):
    return os.path.lexists(os.path.join(content_dir, selected.path))


def reflink_copy(source_dir, content_dir, files):
    """
    Copies the files from source_dir into content_dir with `cp -p --reflink=always`, keeping their relative paths and
    modification times. On BtrFS (or any reflink filesystem) with both on the same filesystem, that clones the extents
    instead of copying the data. Returns the number of files copied, or None, with a warning, if cp failed: the
    rclone copy after it then copies whatever is missing the slow way, and replaces anything cp left half-done.
    """
    copied = 0
    copied_bytes = 0
    total_bytes = sum(f.size for f in files)
    started = time.monotonic()
    for start in range(0, len(files), REFLINK_BATCH):
        batch = files[start : start + REFLINK_BATCH]
        result = subprocess.run(
            ["cp", "-p", "--reflink=always", "--parents", "-t", content_dir, "--", *[f.path for f in batch]],
            cwd=source_dir,
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            last = result.stderr.strip().splitlines()[-1:] or [f"exit code {result.returncode}"]
            warning(
                f"cp --reflink=always failed after {fmt_count(copied)} files, copying the data with rclone: {last[0]}"
            )
            return None
        copied += len(batch)
        copied_bytes += sum(f.size for f in batch)
        print(
            "cp --reflink=always: "
            + format_progress(copied, len(files), copied_bytes, total_bytes, time.monotonic() - started),
            flush=True,
        )
    return copied


def format_progress(done, total, done_bytes, total_bytes, elapsed):
    """
    One status line: files and bytes done, elapsed time, rate, and ETA. The ETA is worked out from the file rate, since
    a reflink costs per file rather than per byte.
    """
    rate = done / elapsed if elapsed > 0 else 0
    eta = fmt_duration((total - done) / rate) if rate > 0 else "unknown"
    return (
        f"{fmt_count(done)}/{fmt_count(total)} files ({fmt_bytes(done_bytes)}/{fmt_bytes(total_bytes)}), "
        f"elapsed {fmt_duration(elapsed)}, {rate:,.0f} files/s, ETA {eta}"
    )


def rclone_copy(source, content_dir, files_from, *args):
    """rclone copy of exactly the listed files, keeping modification times and skipping files already there."""
    rclone_run(
        "copy",
        source,
        content_dir,
        "--files-from-raw",
        files_from,
        *args,
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


def compare(content_dir, files, root_names=frozenset()):
    """
    The staging folder against the selection, by size and modification time (to the second), every file strictly
    checked (no more index.json exemption: it's never selected, NOTES.md "Index snapshot"). root_names (the README,
    checksum file, its signature and the index tarball) are skipped entirely, neither required nor flagged as extra:
    05-make-torrent and this step's own index snapshot section write them before the torrent is made, and a rerun of
    load must not trip over them.
    """
    state = State()
    selected = {f.path: f for f in files}
    on_disk = set()
    for dirpath, dirnames, filenames in os.walk(content_dir, followlinks=False):
        rel_dir = os.path.relpath(dirpath, content_dir)
        rel_dir = "" if rel_dir == "." else rel_dir.replace(os.sep, "/")
        # Symlinks to folders are listed in dirnames, and never walked into.
        for name in sorted(filenames + [d for d in dirnames if os.path.islink(os.path.join(dirpath, d))]):
            if rel_dir == "" and name in root_names:
                continue
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


def report_problems(title, items, report=error):
    """Lists a sample of the items, as errors (the step then stops) or, with report=warning, as warnings."""
    report(f"{title}: {fmt_count(len(items))}, e.g.")
    for item in items[:LOG_SAMPLE]:
        report(f"  {item}")


if __name__ == "__main__":
    main()
