"""
Tests for 03-load.py: comparing the content folder with the selection (compare), where index.json files are only
checked for being there, since DataIndex rewrites them on every run.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("load", os.path.join(HERE, "..", "03-load.py"))
load = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(load)

MTIME = 1_700_000_000
DATA = "wars/history/wars-2003.tar.bz2"
INDEX = "wars/history/index.json"


class CompareTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.content_dir = self.tmp.name
        for path in (DATA, INDEX):
            full = os.path.join(self.content_dir, path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "wb") as f:
                f.write(b"0123456789")
            os.utime(full, (MTIME, MTIME))

    def tearDown(self):
        self.tmp.cleanup()

    def compare(self, path, size=10, mtime=MTIME):
        files = [load.Selected(p, size, mtime) if p == path else load.Selected(p, 10, MTIME) for p in (DATA, INDEX)]
        return load.compare(self.content_dir, files)

    def test_all_as_selected(self):
        state = self.compare(DATA)
        self.assertEqual(state.matching, {DATA, INDEX})
        self.assertEqual(state.different, [])

    def test_data_file_with_other_mtime_is_different(self):
        state = self.compare(DATA, mtime=MTIME + 86400)
        self.assertEqual(state.matching, {INDEX})
        self.assertEqual(len(state.different), 1)

    def test_data_file_with_other_size_is_different(self):
        state = self.compare(DATA, size=11)
        self.assertEqual(len(state.different), 1)

    def test_index_json_with_other_mtime_matches(self):
        state = self.compare(INDEX, mtime=MTIME + 86400)
        self.assertEqual(state.matching, {DATA, INDEX})
        self.assertEqual(state.different, [])

    def test_index_json_with_other_size_matches(self):
        state = self.compare(INDEX, size=11)
        self.assertEqual(state.matching, {DATA, INDEX})
        self.assertEqual(state.different, [])

    def test_missing_index_json_is_missing(self):
        os.remove(os.path.join(self.content_dir, INDEX))
        state = self.compare(DATA)
        self.assertEqual(state.missing, [INDEX])


if __name__ == "__main__":
    unittest.main()
