"""
Tests for 09-check-before-delete.py: which of the torrent's files make the delete set (build_delete_set), and that the
docs page lists the torrent before anything is deleted (docs_page_problems).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
import os
import sys
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location(
    "check_before_delete", os.path.join(HERE, "..", "09-check-before-delete.py")
)
check_before_delete = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(check_before_delete)

MTIME = 1_700_000_000
PATH = "market-orders/history/2023/2023-01-01/a.csv.bz2"


class BuildDeleteSetTest(unittest.TestCase):
    def build(self, torrent_size=10, manifest=True, selected=(10, MTIME), fresh=(10, MTIME)):
        return check_before_delete.build_delete_set(
            {PATH: torrent_size},
            {PATH: "sha"} if manifest else {},
            {PATH: selected} if selected else {},
            {PATH: fresh} if fresh else {},
        )

    def test_confirmed(self):
        confirmed, failed = self.build()
        self.assertEqual(confirmed, [{"path": PATH, "size": 10, "last_modified": "2023-11-14T22:13:20Z"}])
        self.assertEqual(failed, [])

    def test_torrent_size_differs_from_selection(self):
        confirmed, failed = self.build(torrent_size=11)
        self.assertEqual(confirmed, [])
        self.assertEqual([f["path"] for f in failed], [PATH])

    def test_not_in_manifest(self):
        confirmed, failed = self.build(manifest=False)
        self.assertEqual(confirmed, [])
        self.assertEqual(len(failed), 1)

    def test_changed_on_bucket(self):
        confirmed, failed = self.build(fresh=(10, MTIME + 1))
        self.assertEqual(confirmed, [])
        self.assertEqual(len(failed), 1)

    def test_gone_from_bucket(self):
        confirmed, failed = self.build(fresh=None)
        self.assertEqual(confirmed, [])
        self.assertEqual(len(failed), 1)


class DocsPageProblemsTest(unittest.TestCase):
    ID = "everef-eve-online-deep-archive-2024-01-01"
    V1 = "0123456789abcdef0123456789abcdef01234567"

    def test_lists_torrent(self):
        text = f"<td>{self.ID}</td><td><code>{self.V1.upper()}</code></td>"
        self.assertEqual(check_before_delete.docs_page_problems(text, self.ID, self.V1), [])

    def test_missing_torrent(self):
        self.assertEqual(len(check_before_delete.docs_page_problems("<p>nothing</p>", self.ID, self.V1)), 2)

    def test_missing_info_hash(self):
        self.assertEqual(len(check_before_delete.docs_page_problems(self.ID, self.ID, self.V1)), 1)


if __name__ == "__main__":
    unittest.main()
