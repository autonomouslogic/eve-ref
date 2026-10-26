"""
Tests for 05-make-torrent.py: the check that the content folder still matches the manifest before the torrent is
hashed (content_problems), the progress lines while it is (hashing_progress), reading a file's modification time
back out of a bencoded torrent (file_mtime_problems), and an end-to-end build-and-check (EndToEndTest) against a
real tiny torrent, exercising build_torrent, load_torrent and check_torrent together, the same as main() does.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import importlib.util
import io
import os
import sys
import tempfile
import unittest
from unittest.mock import patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("make_torrent", os.path.join(HERE, "..", "05-make-torrent.py"))
make_torrent = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(make_torrent)

try:
    import libtorrent as lt

    LIBTORRENT = True
except ImportError:
    LIBTORRENT = False

MTIME = 1_700_000_000
PATH = "market-orders/history/2023/2023-01-01/a.csv.bz2"


class ContentProblemsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.content_dir = self.tmp.name
        full = os.path.join(self.content_dir, PATH)
        os.makedirs(os.path.dirname(full))
        with open(full, "wb") as f:
            f.write(b"0123456789")
        os.utime(full, (MTIME, MTIME))

    def tearDown(self):
        self.tmp.cleanup()

    def problems(self, size=10, mtime=MTIME):
        return make_torrent.content_problems(self.content_dir, {PATH: size}, {PATH: mtime})

    def test_unchanged(self):
        self.assertEqual(self.problems(), [])

    def test_different_size(self):
        self.assertEqual(len(self.problems(size=11)), 1)

    def test_different_modification_time(self):
        self.assertEqual(len(self.problems(mtime=MTIME + 1)), 1)

    def test_missing(self):
        os.remove(os.path.join(self.content_dir, PATH))
        self.assertEqual(len(self.problems()), 1)


class HashingProgressTest(unittest.TestCase):
    def lines(self, num_pieces, calls, interval):
        out = io.StringIO()
        with patch.object(make_torrent, "PROGRESS_INTERVAL", interval), contextlib.redirect_stdout(out):
            progress = make_torrent.hashing_progress(num_pieces)
            for piece in calls:
                progress(piece)
        return [line.split(",")[0] for line in out.getvalue().splitlines()]

    def test_prints_once_all_pieces_are_done(self):
        # libtorrent hashes in parallel, so pieces come back out of order: they're counted, not read as an index.
        self.assertEqual(self.lines(3, [2, 0, 1], 3600), ["3/3 pieces"])

    def test_prints_every_interval(self):
        self.assertEqual(self.lines(3, [0, 1, 2], 0), ["1/3 pieces", "2/3 pieces", "3/3 pieces"])


@unittest.skipUnless(LIBTORRENT, "libtorrent Python bindings aren't installed")
class FileMtimeProblemsTest(unittest.TestCase):
    def build(self, files):
        """files: {path: (size, mtime)}. A tiny real torrent's bencoded bytes, v1 only + modification_time."""
        with tempfile.TemporaryDirectory() as parent:
            name = "t"
            fs = lt.file_storage()
            for path, (size, mtime) in files.items():
                full = os.path.join(parent, name, path)
                os.makedirs(os.path.dirname(full), exist_ok=True)
                with open(full, "wb") as f:
                    f.write(b"x" * size)
                fs.add_file(f"{name}/{path}", size, 0, mtime)
            ct = lt.create_torrent(fs, 16 * 1024, lt.create_torrent.v1_only | lt.create_torrent.modification_time)
            lt.set_piece_hashes(ct, parent)
            return lt.bencode(ct.generate())

    def test_matching_mtimes_have_no_problems(self):
        data = self.build({"a.bin": (10, MTIME), "b.bin": (20, MTIME + 1)})
        self.assertEqual(make_torrent.file_mtime_problems(data, {"a.bin": MTIME, "b.bin": MTIME + 1}), [])

    def test_a_different_expected_mtime_is_a_problem(self):
        data = self.build({"a.bin": (10, MTIME)})
        problems = make_torrent.file_mtime_problems(data, {"a.bin": MTIME + 1})
        self.assertEqual(len(problems), 1)
        self.assertIn("a.bin", problems[0])

    def test_a_path_not_in_expected_mtimes_is_ignored(self):
        data = self.build({"a.bin": (10, MTIME)})
        self.assertEqual(make_torrent.file_mtime_problems(data, {}), [])


@unittest.skipUnless(LIBTORRENT, "libtorrent Python bindings aren't installed")
class EndToEndTest(unittest.TestCase):
    """
    Builds a tiny content folder (a couple of data files, plus the three root files, stood in for by plain files:
    this doesn't need the signing pipeline), then runs it through build_torrent, load_torrent and check_torrent --
    the same path main() takes from "Torrent" through "Check" -- and checks the result comes back clean. A second
    pass changes a file afterwards and checks check_torrent catches it.
    """

    TORRENT_ID = "everef-eve-online-deep-archive-01"
    PIECE_SIZE = 16 * 1024
    TRACKERS = ["udp://tracker1.example.org:1337/announce", "udp://tracker2.example.org:1337/announce"]
    COMMENT = "test comment"
    CREATOR = "EVE Ref (libtorrent test)"

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.content_dir = os.path.join(self.tmp.name, self.TORRENT_ID)
        os.makedirs(self.content_dir)
        self.files = {}  # path -> size
        self.mtimes = {}  # path -> mtime
        for path, size, mtime in (
            ("a/one.csv.bz2", 100, MTIME),
            ("a/two.csv.bz2", self.PIECE_SIZE + 10, MTIME + 1),
        ):
            self._write(path, size, mtime)
            self.files[path] = size
            self.mtimes[path] = mtime
        self.root_names = {
            f"{self.TORRENT_ID}-README.md",
            f"{self.TORRENT_ID}.sha256",
            f"{self.TORRENT_ID}.sha256.minisig",
        }
        for name in self.root_names:
            self._write(name, 20, MTIME + 2)

    def tearDown(self):
        self.tmp.cleanup()

    def _write(self, path, size, mtime):
        full = os.path.join(self.content_dir, path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "wb") as f:
            f.write(b"x" * size)
        os.utime(full, (mtime, mtime))

    def build_and_check(self):
        """Runs main()'s "Torrent" through "Check" steps on self.content_dir; returns check_torrent's v1 hash."""
        root_mtimes = {name: int(os.path.getmtime(os.path.join(self.content_dir, name))) for name in self.root_names}
        expected_mtimes = {**self.mtimes, **root_mtimes}
        with patch.object(make_torrent, "PIECE_SIZE", self.PIECE_SIZE):
            # fs must stay referenced until generate(): create_torrent only keeps a reference to it, not a copy
            # (build_torrent's docstring).
            ct, fs, piece_size, num_pieces, _total_bytes = make_torrent.build_torrent(
                lt, self.content_dir, self.TORRENT_ID, self.files, self.root_names, expected_mtimes, self.TRACKERS,
                self.COMMENT, self.CREATOR,
            )
            lt.set_piece_hashes(ct, self.tmp.name)
            data = lt.bencode(ct.generate())
            del fs
            torrent_path = os.path.join(self.tmp.name, f"{self.TORRENT_ID}.torrent")
            with open(torrent_path, "wb") as f:
                f.write(data)
            ti = make_torrent.load_torrent(lt, torrent_path)
            txt_path = os.path.join(self.tmp.name, f"{self.TORRENT_ID}.txt")
            with open(txt_path, "w", encoding="utf-8") as f:
                f.writelines(p + "\n" for p in sorted(self.files))
            return make_torrent.check_torrent(
                ti, data, torrent_path, txt_path, self.TORRENT_ID, self.files, self.content_dir, self.root_names,
                self.TRACKERS, self.COMMENT, piece_size, expected_mtimes,
            )

    def test_a_clean_build_checks_out(self):
        with contextlib.redirect_stdout(io.StringIO()):
            v1_hash = self.build_and_check()
        self.assertTrue(v1_hash)
        self.assertEqual(len(v1_hash), 40)  # a SHA-1 hex digest

    def test_a_file_changed_on_disk_after_the_manifest_is_caught(self):
        with contextlib.redirect_stdout(io.StringIO()):
            self.build_and_check()
        # The file on disk changed size, but self.files (standing in for the manifest) still has the old one:
        # build_torrent embeds whatever's on disk now, but check_torrent compares it against self.files, not disk,
        # and must catch the mismatch -- the same protection content_problems gives earlier in main(), independently.
        self._write("a/one.csv.bz2", 999, MTIME)
        with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(SystemExit):
            self.build_and_check()


if __name__ == "__main__":
    unittest.main()
