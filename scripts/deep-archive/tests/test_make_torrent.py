"""
Tests for 05-make-torrent.py: the check that the content folder still matches the manifest before the torrent is
hashed (content_problems, PLAN Review notes R7).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
import os
import sys
import tempfile
import unittest

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


if __name__ == "__main__":
    unittest.main()
