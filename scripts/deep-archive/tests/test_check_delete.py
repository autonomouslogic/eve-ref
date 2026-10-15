"""
Tests for 11-check-delete.py: catching a find-time path that disappeared without being part of the confirmed
delete set (find_unexpectedly_gone), and the stale-index-folder scan (find_index_only_folders).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
import os
import sys
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("check_delete", os.path.join(HERE, "..", "11-check-delete.py"))
check_delete = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(check_delete)


class FindUnexpectedlyGoneTest(unittest.TestCase):
    def test_nothing_gone(self):
        original = {"a.csv", "b.csv"}
        fresh = {"a.csv", "b.csv"}
        self.assertEqual(check_delete.find_unexpectedly_gone(original, set(), set(), fresh), [])

    def test_deleted_paths_are_allowed_to_be_gone(self):
        original = {"a.csv", "b.csv"}
        fresh = {"b.csv"}
        self.assertEqual(check_delete.find_unexpectedly_gone(original, {"a.csv"}, set(), fresh), [])

    def test_failed_paths_are_allowed_to_be_gone(self):
        # 11-check-delete's own "Failed paths untouched" section already reports this case; find_unexpectedly_gone
        # doesn't double-report it.
        original = {"a.csv", "b.csv"}
        fresh = {"b.csv"}
        self.assertEqual(check_delete.find_unexpectedly_gone(original, set(), {"a.csv"}, fresh), [])

    def test_catches_a_path_removed_outside_the_delete_set(self):
        # Simulates the bug this check exists for: 10-delete's rclone call only ever takes an explicit file list, so
        # this is what would catch it deleting (or something else removing) a path that was never confirmed.
        original = {"a.csv", "b.csv", "c.csv"}
        fresh = {"b.csv"}  # a.csv and c.csv are both gone
        self.assertEqual(check_delete.find_unexpectedly_gone(original, {"a.csv"}, set(), fresh), ["c.csv"])

    def test_new_uploads_since_find_are_not_a_problem(self):
        # fresh can hold paths find never saw: that's normal site activity, not checked here.
        original = {"a.csv"}
        fresh = {"a.csv", "new.csv"}
        self.assertEqual(check_delete.find_unexpectedly_gone(original, set(), set(), fresh), [])

    def test_result_is_sorted(self):
        original = {"c.csv", "a.csv", "b.csv"}
        fresh = set()
        self.assertEqual(check_delete.find_unexpectedly_gone(original, set(), set(), fresh), ["a.csv", "b.csv", "c.csv"])


class FindIndexOnlyFoldersTest(unittest.TestCase):
    def test_no_stale_folders(self):
        fresh = {"a/index.json": None, "a/data.csv": None}
        self.assertEqual(check_delete.find_index_only_folders(fresh), [])

    def test_folder_with_only_index_files_is_stale(self):
        fresh = {"a/index.json": None, "a/index.html": None}
        self.assertEqual(check_delete.find_index_only_folders(fresh), ["a"])

    def test_a_non_index_file_deeper_down_keeps_the_folder_alive(self):
        fresh = {"a/index.json": None, "a/b/data.csv": None}
        self.assertEqual(check_delete.find_index_only_folders(fresh), [])

    def test_site_root_is_never_reported(self):
        fresh = {"index.json": None}
        self.assertEqual(check_delete.find_index_only_folders(fresh), [])

    def test_deep_archive_is_never_reported(self):
        fresh = {"deep-archive/index.json": None}
        self.assertEqual(check_delete.find_index_only_folders(fresh), [])


if __name__ == "__main__":
    unittest.main()
