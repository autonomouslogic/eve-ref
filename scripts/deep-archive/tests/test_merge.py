"""
Tests for lib/merge.py: moving a torrent's staged files into the merged SEED_DIR tree (build_plan, link_all,
verify_all, unlink_staging, remove_empty_staging_dirs), and that every step is safe to run again partway through.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
import merge  # noqa: E402

MTIME = 1_700_000_000


class MergeTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.staging_dir = os.path.join(self.tmp.name, "staging")
        self.seed_dir = os.path.join(self.tmp.name, "seed")
        os.makedirs(self.staging_dir)
        os.makedirs(self.seed_dir)

    def tearDown(self):
        self.tmp.cleanup()

    def write_staging(self, rel_path, content=b"x", mtime=MTIME):
        full = os.path.join(self.staging_dir, *rel_path.split("/"))
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "wb") as f:
            f.write(content)
        os.utime(full, (mtime, mtime))
        return full


class BuildPlanTest(MergeTestCase):
    def test_plan_has_one_entry_per_file(self):
        self.write_staging("a/b.csv", b"0123456789")
        plan = merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 10, MTIME)])
        self.assertEqual(len(plan), 1)
        self.assertEqual(plan[0]["path"], "a/b.csv")
        self.assertEqual(plan[0]["target"], os.path.join(self.seed_dir, "a", "b.csv"))

    def test_missing_staging_file_fails(self):
        with self.assertRaises(merge.MergeError):
            merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 10, MTIME)])

    def test_size_mismatch_fails(self):
        self.write_staging("a/b.csv", b"0123456789")
        with self.assertRaises(merge.MergeError):
            merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 999, MTIME)])

    def test_mtime_mismatch_fails(self):
        self.write_staging("a/b.csv", b"0123456789", mtime=MTIME)
        with self.assertRaises(merge.MergeError):
            merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 10, MTIME + 1)])


class LinkAllTest(MergeTestCase):
    def plan_for(self, rel_path, content=b"0123456789"):
        full = self.write_staging(rel_path, content)
        return merge.build_plan(self.staging_dir, self.seed_dir, [(rel_path, len(content), MTIME)]), full

    def test_links_into_place(self):
        plan, staging_full = self.plan_for("a/b.csv")
        merge.link_all(plan)
        target = plan[0]["target"]
        self.assertTrue(os.path.isfile(target))
        self.assertEqual(os.stat(target).st_ino, os.stat(staging_full).st_ino)

    def test_creates_parent_folders(self):
        plan, _ = self.plan_for("a/b/c/d.csv")
        merge.link_all(plan)
        self.assertTrue(os.path.isfile(plan[0]["target"]))

    def test_resuming_an_already_linked_file_is_a_noop(self):
        plan, _ = self.plan_for("a/b.csv")
        merge.link_all(plan)
        merge.link_all(plan)  # doesn't raise

    def test_a_real_collision_fails(self):
        plan, _ = self.plan_for("a/b.csv")
        os.makedirs(os.path.dirname(plan[0]["target"]), exist_ok=True)
        with open(plan[0]["target"], "w", encoding="utf-8") as f:
            f.write("someone else's file")
        with self.assertRaises(merge.MergeError):
            merge.link_all(plan)


class VerifyAllTest(MergeTestCase):
    def test_clean_plan_has_no_problems(self):
        full = self.write_staging("a/b.csv")
        plan = merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 1, MTIME)])
        merge.link_all(plan)
        self.assertEqual(merge.verify_all(plan), [])
        del full

    def test_missing_target_is_a_problem(self):
        self.write_staging("a/b.csv")
        plan = merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 1, MTIME)])
        problems = merge.verify_all(plan)
        self.assertEqual(len(problems), 1)
        self.assertIn("missing", problems[0])

    def test_wrong_inode_is_a_problem(self):
        self.write_staging("a/b.csv")
        plan = merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 1, MTIME)])
        os.makedirs(os.path.dirname(plan[0]["target"]), exist_ok=True)
        with open(plan[0]["target"], "w", encoding="utf-8") as f:
            f.write("x")
        os.utime(plan[0]["target"], (MTIME, MTIME))
        problems = merge.verify_all(plan)
        self.assertEqual(len(problems), 1)
        self.assertIn("different inode", problems[0])


class UnlinkStagingTest(MergeTestCase):
    def linked_plan(self, rel_path="a/b.csv"):
        self.write_staging(rel_path)
        plan = merge.build_plan(self.staging_dir, self.seed_dir, [(rel_path, 1, MTIME)])
        merge.link_all(plan)
        return plan

    def test_removes_staging_file(self):
        plan = self.linked_plan()
        removed = merge.unlink_staging(plan)
        self.assertEqual(removed, [plan[0]["staging"]])
        self.assertFalse(os.path.exists(plan[0]["staging"]))
        self.assertTrue(os.path.isfile(plan[0]["target"]))  # the target survives

    def test_already_removed_staging_file_is_skipped(self):
        plan = self.linked_plan()
        merge.unlink_staging(plan)
        self.assertEqual(merge.unlink_staging(plan), [])  # doesn't raise on the second run

    def test_never_unlinks_the_only_copy(self):
        self.write_staging("a/b.csv")
        plan = merge.build_plan(self.staging_dir, self.seed_dir, [("a/b.csv", 1, MTIME)])
        # Not linked: the staging file has only one link to itself.
        with self.assertRaises(merge.MergeError):
            merge.unlink_staging(plan)
        self.assertTrue(os.path.exists(plan[0]["staging"]))

    def test_target_replaced_by_something_else_refuses_to_unlink(self):
        plan = self.linked_plan()
        os.unlink(plan[0]["target"])
        with open(plan[0]["target"], "w", encoding="utf-8") as f:
            f.write("not the staged file")
        with self.assertRaises(merge.MergeError):
            merge.unlink_staging(plan)
        self.assertTrue(os.path.exists(plan[0]["staging"]))  # left alone


class RemoveEmptyStagingDirsTest(MergeTestCase):
    def test_removes_empty_nested_folders_and_itself(self):
        os.makedirs(os.path.join(self.staging_dir, "a", "b", "c"))
        merge.remove_empty_staging_dirs(self.staging_dir)
        self.assertFalse(os.path.exists(self.staging_dir))

    def test_keeps_folders_with_files(self):
        self.write_staging("a/b/c.csv")
        merge.remove_empty_staging_dirs(self.staging_dir)
        self.assertTrue(os.path.isfile(os.path.join(self.staging_dir, "a", "b", "c.csv")))


if __name__ == "__main__":
    unittest.main()
