"""
Tests for 05-make-torrent.py: the check that the content folder still matches the manifest before the torrent is
hashed (content_problems), and the progress lines while it is (hashing_progress).

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


if __name__ == "__main__":
    unittest.main()
