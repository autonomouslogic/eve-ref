"""
Tests for lib/seed_dir.py: the merged SEED_DIR tree against its signed torrents (disk_paths, check_ownership,
check_merge_conflicts, quick_check), and qBittorrent state (check_qbittorrent).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import os
import sys
import tempfile
import unittest
from unittest.mock import patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
import seed_dir  # noqa: E402

MTIME = 1_700_000_000


def torrents(**by_id):
    """{torrent_id: {"files": {path: (size, mtime)}, "v1_hash": "<40 hex digits>"}} for test data."""
    return {tid: {"files": files, "v1_hash": f"{i:040x}"} for i, (tid, files) in enumerate(by_id.items())}


class DiskPathsTest(unittest.TestCase):
    def write(self, root, rel, mtime=MTIME):
        full = os.path.join(root, *rel.split("/"))
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "wb") as f:
            f.write(b"0123456789")
        os.utime(full, (mtime, mtime))

    def test_reads_size_and_mtime(self):
        with tempfile.TemporaryDirectory() as seed:
            self.write(seed, "market-orders/a.csv.bz2")
            paths = seed_dir.disk_paths(seed)
            self.assertEqual(paths, {"market-orders/a.csv.bz2": (10, MTIME)})

    def test_staging_and_torrents_folders_skipped(self):
        with tempfile.TemporaryDirectory() as seed:
            self.write(seed, ".staging/everef-eve-online-deep-archive-00/a.csv")
            self.write(seed, ".torrents/everef-eve-online-deep-archive-00.torrent")
            self.write(seed, "market-orders/a.csv")
            paths = seed_dir.disk_paths(seed)
            self.assertEqual(list(paths), ["market-orders/a.csv"])

    def test_root_files_are_included(self):
        # Unlike 01-find's existing_seed_paths, every file here is tracked, root files included: ownership covers
        # them too.
        with tempfile.TemporaryDirectory() as seed:
            self.write(seed, "everef-eve-online-deep-archive-00-README.md")
            paths = seed_dir.disk_paths(seed)
            self.assertEqual(list(paths), ["everef-eve-online-deep-archive-00-README.md"])


class CheckOwnershipTest(unittest.TestCase):
    def test_clean_tree_has_no_problems(self):
        t = torrents(t00={"a.csv": (10, MTIME)})
        disk = {"a.csv": (10, MTIME)}
        self.assertEqual(seed_dir.check_ownership(t, disk), [])

    def test_unowned_file_is_a_problem(self):
        t = torrents(t00={"a.csv": (10, MTIME)})
        disk = {"a.csv": (10, MTIME), "stray.csv": (1, MTIME)}
        problems = seed_dir.check_ownership(t, disk)
        self.assertEqual(len(problems), 1)
        self.assertIn("stray.csv", problems[0])

    def test_missing_file_is_a_problem(self):
        t = torrents(t00={"a.csv": (10, MTIME)})
        problems = seed_dir.check_ownership(t, {})
        self.assertEqual(len(problems), 1)
        self.assertIn("missing from disk", problems[0])

    def test_size_mismatch_is_a_problem(self):
        t = torrents(t00={"a.csv": (10, MTIME)})
        disk = {"a.csv": (999, MTIME)}
        problems = seed_dir.check_ownership(t, disk)
        self.assertEqual(len(problems), 1)

    def test_owned_by_two_torrents_is_a_problem(self):
        t = torrents(t00={"a.csv": (10, MTIME)}, t01={"a.csv": (10, MTIME)})
        disk = {"a.csv": (10, MTIME)}
        problems = seed_dir.check_ownership(t, disk)
        self.assertEqual(len(problems), 1)
        self.assertIn("owned by 2 torrents", problems[0])


class CheckMergeConflictsTest(unittest.TestCase):
    def test_no_conflicts(self):
        t = torrents(t00={"a/b.csv": (1, MTIME)}, t01={"c/d.csv": (1, MTIME)})
        self.assertEqual(seed_dir.check_merge_conflicts(t), [])

    def test_case_fold_collision_across_torrents(self):
        t = torrents(t00={"a/B.csv": (1, MTIME)}, t01={"a/b.csv": (1, MTIME)})
        self.assertEqual(len(seed_dir.check_merge_conflicts(t)), 1)


class QuickCheckTest(unittest.TestCase):
    def test_empty_tree_is_clean(self):
        with tempfile.TemporaryDirectory() as seed, tempfile.TemporaryDirectory() as torrents_dir:
            found, disk, problems = seed_dir.quick_check(seed, torrents_dir)
            self.assertEqual(found, {})
            self.assertEqual(disk, {})
            self.assertEqual(problems, [])


class CheckQbittorrentTest(unittest.TestCase):
    class FakeClient:
        def __init__(self, by_hash):
            self._by_hash = by_hash

        def info(self, info_hash):
            return self._by_hash.get(info_hash)

    def clean_info(self, save_path="/seed"):
        return {"progress": 1.0, "downloaded": 0, "state": "uploading", "save_path": save_path}

    def test_clean_torrent_has_no_problems(self):
        t = torrents(t00={"a.csv": (1, MTIME)})
        v1_hash = t["t00"]["v1_hash"]
        client = self.FakeClient({v1_hash: self.clean_info()})
        self.assertEqual(seed_dir.check_qbittorrent(client, t, "/seed"), [])

    def test_missing_from_qbittorrent_is_a_problem(self):
        t = torrents(t00={"a.csv": (1, MTIME)})
        client = self.FakeClient({})
        problems = seed_dir.check_qbittorrent(client, t, "/seed")
        self.assertEqual(len(problems), 1)
        self.assertIn("not in qBittorrent", problems[0])

    def test_partial_download_is_a_problem(self):
        t = torrents(t00={"a.csv": (1, MTIME)})
        v1_hash = t["t00"]["v1_hash"]
        info = self.clean_info()
        info["downloaded"] = 100
        client = self.FakeClient({v1_hash: info})
        problems = seed_dir.check_qbittorrent(client, t, "/seed")
        self.assertEqual(len(problems), 1)

    def test_wrong_save_path_is_a_problem(self):
        t = torrents(t00={"a.csv": (1, MTIME)})
        v1_hash = t["t00"]["v1_hash"]
        client = self.FakeClient({v1_hash: self.clean_info(save_path="/elsewhere")})
        problems = seed_dir.check_qbittorrent(client, t, "/seed")
        self.assertEqual(len(problems), 1)
        self.assertIn("save path", problems[0])

    def test_not_seeding_state_is_a_problem(self):
        t = torrents(t00={"a.csv": (1, MTIME)})
        v1_hash = t["t00"]["v1_hash"]
        info = self.clean_info()
        info["state"] = "error"
        client = self.FakeClient({v1_hash: info})
        problems = seed_dir.check_qbittorrent(client, t, "/seed")
        self.assertEqual(len(problems), 1)


class LoadTorrentsTest(unittest.TestCase):
    def test_empty_torrents_dir(self):
        with tempfile.TemporaryDirectory() as torrents_dir:
            self.assertEqual(seed_dir.load_torrents(torrents_dir), {})

    def test_name_mismatch_fails(self):
        with tempfile.TemporaryDirectory() as torrents_dir:
            torrent_path = os.path.join(torrents_dir, "everef-eve-online-deep-archive-00.torrent")
            with open(torrent_path, "wb") as f:
                f.write(b"placeholder")
            with patch.object(seed_dir, "verify_signature", return_value=None), patch.object(
                seed_dir, "read_torrent_entries", return_value=("everef-eve-online-deep-archive-99", {})
            ):
                with self.assertRaises(seed_dir.SeedDirError):
                    seed_dir.load_torrents(torrents_dir)


if __name__ == "__main__":
    unittest.main()
