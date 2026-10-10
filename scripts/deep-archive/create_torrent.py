#!/usr/bin/env python3
"""
create_torrent: make a hybrid v1+v2 .torrent for a directory with Python libtorrent 2.x, to measure .torrent sizes.

EXPERIMENT (PLAN 2.1, 2.4, Appendix B).

    create_torrent.py --source <dir> [--output <file.torrent>] [--fake-hashes]

The torrent's name is the basename of <dir>, normally the torrent ID. The .torrent goes in SCRATCH_DIR by default
(environment or torrents.env, see lib/workflow.py), or beside <dir> if SCRATCH_DIR isn't set.

Pieces are PIECE_SIZE (lib/workflow.py), the size find's limits are worked out for.

Trackers always come from deep-archive/trackers.txt: one per line, each in its own tier.

The comment follows the PLAN 2.4 template. Years and datasets come from the file paths (the first YYYY directory,
or the modification year for files without one), not from a manifest.

--fake-hashes skips reading the data and fills in dummy piece hashes. Hashes have a fixed size, so the .torrent has
the same size as a real one, in minutes instead of hours. The result is useless for seeding.
"""

import argparse
import datetime
import math
import os
import re
import signal
import sys
import time

import libtorrent as lt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import PIECE_SIZE, fail, load_env, ok, section  # noqa: E402

TRACKERS_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")
TRACKER_SCHEMES = ("udp://", "http://", "https://")
YEAR_DIR_RE = re.compile(r"^(19|20)\d{2}$")
DATASET_SUFFIXES = {"history", "backfills"}
DATASETS_MAX_LENGTH = 200
CREATOR = "EVE Ref"

# PLAN 2.4. {ID} is the torrent name.
COMMENT_TEMPLATE = """EVE Ref Deep Archive: {id}
Data from {years}. Datasets: {datasets}.
What this is, how to use and verify it, and how to help seed: https://docs.everef.net/datasets/deep-archive.html
File list: https://data.everef.net/deep-archive/{id}.txt"""

KIB = 1024
MIB = 1024 * KIB
GIB = 1024 * MIB



def main():
    # libtorrent's long calls (set_piece_hashes, generate) run in C++ and never return to Python to raise
    # KeyboardInterrupt, so let Ctrl+C kill the process directly.
    signal.signal(signal.SIGINT, signal.SIG_DFL)
    args = parse_args()
    started = time.monotonic()

    source = os.path.realpath(args.source)
    if not os.path.isdir(source):
        fail(f"--source does not exist: {source}")
    name = os.path.basename(source)
    parent = os.path.dirname(source)
    load_env()
    output_dir = os.environ.get("SCRATCH_DIR") or parent
    output = os.path.realpath(args.output) if args.output else os.path.join(output_dir, f"{name}.torrent")
    if output.startswith(source + os.sep):
        fail("--output must not be inside --source")

    print(f"# 🚀 create_torrent: {name}")
    print(f"libtorrent {lt.__version__}")
    print(f"Source: {source}")
    print(f"Output: {output}")

    section("Trackers")
    trackers = load_trackers(TRACKERS_FILE)

    section("Files")
    fs, files, total, years, datasets = add_files(source, name)
    if not files:
        fail("No files to add")
    comment = render_comment(name, years, datasets)

    section("Comment")
    print(comment)

    section("Torrent")

    ct = lt.create_torrent(fs, PIECE_SIZE)
    piece_size = ct.piece_length()
    for tier, tracker in enumerate(trackers):
        ct.add_tracker(tracker, tier)
    ct.set_comment(comment)
    ct.set_creator(CREATOR)
    num_pieces = ct.num_pieces()
    if args.fake_hashes:
        print(f"Setting fake hashes for {fmt_count(num_pieces)} pieces")
        set_fake_hashes(ct, piece_size)
    else:
        print(f"Hashing {fmt_count(num_pieces)} pieces ({fmt_bytes(total)})")
        lt.set_piece_hashes(ct, parent)

    print("Generating .torrent")
    data = lt.bencode(ct.generate())
    tmp = output + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.rename(tmp, output)

    loads, error = try_load(output)
    section("Summary")
    print(f"Torrent:   {output}")
    print(f"Files:     {fmt_count(files)} ({fmt_bytes(total)})")
    print(f"Pieces:    {fmt_count(num_pieces)} of {fmt_bytes(piece_size)}")
    print(f"Trackers:  {len(trackers)}, one per tier")
    print(f".torrent:  {fmt_bytes(len(data))} ({fmt_count(len(data))} bytes)")
    print(f"Per file:  {len(data) / files:.0f} bytes")
    print(f"Loads with libtorrent default limits: {'yes' if loads else f'NO ({error})'}")
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Created {os.path.basename(output)}")


def parse_args():
    parser = argparse.ArgumentParser(description="Make a hybrid v1+v2 .torrent for a directory, to measure its size.")
    parser.add_argument("--source", required=True, help="directory to make the torrent from")
    parser.add_argument("--output", help="the .torrent to write, default <name>.torrent in SCRATCH_DIR")
    parser.add_argument("--fake-hashes", action="store_true", help="don't read the data; size measurement only")
    return parser.parse_args()


def load_trackers(path):
    """One tracker URL per line; blank lines and # comments are ignored."""
    path = os.path.realpath(path)
    if not os.path.isfile(path):
        fail(f"Tracker list not found: {path}")
    trackers = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if not line.startswith(TRACKER_SCHEMES):
                fail(f"Not a UDP or HTTP(S) tracker in {path}: {line}")
            if line in trackers:
                fail(f"Duplicate tracker in {path}: {line}")
            trackers.append(line)
    if not trackers:
        fail(f"No trackers in {path}")
    print(f"{len(trackers)} trackers from {path}")
    return trackers


def add_files(source, name):
    """
    Walks the source in sorted order and adds every file, printing each one. Symlinks are skipped.
    Also collects the years and datasets for the comment.
    """
    fs = lt.file_storage()
    files = 0
    total = 0
    years = set()
    datasets = set()
    for dirpath, dirnames, filenames in os.walk(source, followlinks=False):
        dirnames.sort()
        rel_dir = os.path.relpath(dirpath, source)
        rel_dir = "" if rel_dir == "." else rel_dir.replace(os.sep, "/")
        for filename in sorted(filenames):
            full = os.path.join(dirpath, filename)
            if os.path.islink(full):
                continue
            path = f"{rel_dir}/{filename}" if rel_dir else filename
            stat = os.stat(full)
            fs.add_file(f"{name}/{path}", stat.st_size)
            files += 1
            total += stat.st_size
            classify(path, stat.st_mtime, years, datasets)
            print(f"Added {path} ({fmt_bytes(stat.st_size)})")
    print(f"Added {fmt_count(files)} files ({fmt_bytes(total)})")
    return fs, files, total, years, datasets


def classify(path, mtime, years, datasets):
    """
    Year: the first YYYY directory, or the modification year without one. Dataset: the directories before the year
    (or the first three without one), minus a trailing history/backfills. Root-level files (.sha256, README) count
    for neither.
    """
    dirs = path.split("/")[:-1]
    if not dirs:
        return
    year_index = next((i for i, d in enumerate(dirs) if YEAR_DIR_RE.match(d)), None)
    if year_index is None:
        years.add(datetime.datetime.fromtimestamp(mtime, datetime.timezone.utc).year)
        dataset = dirs[:3]
    else:
        years.add(int(dirs[year_index]))
        dataset = dirs[:year_index]
    while len(dataset) > 1 and dataset[-1] in DATASET_SUFFIXES:
        dataset = dataset[:-1]
    if dataset:
        datasets.add("/".join(dataset))


def render_comment(name, years, datasets):
    if not years:
        years_text = "unknown years"
    elif min(years) == max(years):
        years_text = str(min(years))
    else:
        years_text = f"{min(years)}–{max(years)}"
    datasets_text = ", ".join(sorted(datasets)) or "none"
    if len(datasets_text) > DATASETS_MAX_LENGTH:
        datasets_text = datasets_text[:DATASETS_MAX_LENGTH].rsplit(", ", 1)[0] + ", ..."
    return COMMENT_TEMPLATE.format(id=name, years=years_text, datasets=datasets_text)


def set_fake_hashes(ct, piece_size):
    """Dummy v1 piece hashes and v2 per-file piece hashes, so generate() works without reading any data."""
    for piece in range(ct.num_pieces()):
        ct.set_hash(piece, b"\0" * 20)
    storage = ct.files()
    for index in range(storage.num_files()):
        if storage.file_flags(index) & lt.file_storage.flag_pad_file:
            continue
        for piece in range(math.ceil(storage.file_size(index) / piece_size)):
            ct.set_hash2(index, piece, b"\0" * 32)


def try_load(path):
    """Loads the .torrent the way a client with libtorrent's default limits would."""
    try:
        lt.torrent_info(path)
        return True, None
    except RuntimeError as e:
        return False, str(e)


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
