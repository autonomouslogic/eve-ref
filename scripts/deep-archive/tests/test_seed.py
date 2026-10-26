"""
Tests for 07-seed.py: copying the signed torrent into SEED_DIR/.torrents/ (copy_torrent_files), reading a torrent's
own files back from its raw bencoded info dict (read_torrent_entries), checking its name and info-hash against
05-make-torrent's (check_torrent_identity), the merge plan cache (load_or_build_plan), and the no-subfolder read-back
check (check_no_subfolder).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import importlib.util
import io
import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("seed", os.path.join(HERE, "..", "07-seed.py"))
seed = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(seed)

try:
    import libtorrent as lt

    LIBTORRENT = True
except ImportError:
    LIBTORRENT = False

MTIME = 1_700_000_000


def quietly(function, *args, **kwargs):
    with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
        return function(*args, **kwargs)


class CopyTorrentFilesTest(unittest.TestCase):
    def test_copies_both_files(self):
        with tempfile.TemporaryDirectory() as scratch, tempfile.TemporaryDirectory() as torrents_dir:
            for name in ("x.torrent", "x.torrent.minisig"):
                with open(os.path.join(scratch, name), "w", encoding="utf-8") as f:
                    f.write(name)
            seed.copy_torrent_files(torrents_dir, scratch, "x")
            for name in ("x.torrent", "x.torrent.minisig"):
                with open(os.path.join(torrents_dir, name), encoding="utf-8") as f:
                    self.assertEqual(f.read(), name)

    def test_missing_source_fails(self):
        with tempfile.TemporaryDirectory() as scratch, tempfile.TemporaryDirectory() as torrents_dir:
            with self.assertRaises(SystemExit):
                quietly(seed.copy_torrent_files, torrents_dir, scratch, "x")

    def test_identical_existing_copy_is_fine(self):
        with tempfile.TemporaryDirectory() as scratch, tempfile.TemporaryDirectory() as torrents_dir:
            for name in ("x.torrent", "x.torrent.minisig"):
                with open(os.path.join(scratch, name), "w", encoding="utf-8") as f:
                    f.write(name)
                with open(os.path.join(torrents_dir, name), "w", encoding="utf-8") as f:
                    f.write(name)
            seed.copy_torrent_files(torrents_dir, scratch, "x")  # doesn't raise

    def test_different_existing_copy_fails(self):
        with tempfile.TemporaryDirectory() as scratch, tempfile.TemporaryDirectory() as torrents_dir:
            for name in ("x.torrent", "x.torrent.minisig"):
                with open(os.path.join(scratch, name), "w", encoding="utf-8") as f:
                    f.write(name)
            with open(os.path.join(torrents_dir, "x.torrent"), "w", encoding="utf-8") as f:
                f.write("different content")
            with self.assertRaises(SystemExit):
                quietly(seed.copy_torrent_files, torrents_dir, scratch, "x")


@unittest.skipUnless(LIBTORRENT, "libtorrent Python bindings aren't installed")
class RealTorrentTestCase(unittest.TestCase):
    """Builds a tiny real v1-only torrent with modification times, the same way 05-make-torrent does."""

    TORRENT_ID = "everef-eve-online-deep-archive-01"

    def build_torrent(self, files):
        """files: {path: (size, mtime)}. Writes the content under self.content_dir and returns the .torrent path."""
        self.tmp = tempfile.TemporaryDirectory()
        self.content_dir = os.path.join(self.tmp.name, self.TORRENT_ID)
        fs = lt.file_storage()
        for path, (size, mtime) in sorted(files.items()):
            full = os.path.join(self.content_dir, path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "wb") as f:
                f.write(b"x" * size)
            fs.add_file(f"{self.TORRENT_ID}/{path}", size, 0, mtime)
        ct = lt.create_torrent(fs, 16 * 1024, lt.create_torrent.v1_only | lt.create_torrent.modification_time)
        lt.set_piece_hashes(ct, self.tmp.name)
        data = lt.bencode(ct.generate())
        torrent_path = os.path.join(self.tmp.name, f"{self.TORRENT_ID}.torrent")
        with open(torrent_path, "wb") as f:
            f.write(data)
        del fs
        self.v1_hash = str(lt.torrent_info(torrent_path).info_hashes().v1)
        return torrent_path

    def tearDown(self):
        self.tmp.cleanup()


class ReadTorrentEntriesTest(RealTorrentTestCase):
    def test_reads_path_size_and_mtime(self):
        torrent_path = self.build_torrent(
            {"a/b.csv": (10, MTIME), f"{self.TORRENT_ID}-README.md": (5, MTIME + 1)}
        )
        entries = seed.read_torrent_entries(torrent_path)
        self.assertEqual(
            sorted(entries), sorted([("a/b.csv", 10, MTIME), (f"{self.TORRENT_ID}-README.md", 5, MTIME + 1)])
        )


class CheckTorrentIdentityTest(RealTorrentTestCase):
    def test_matching_name_and_hash_is_fine(self):
        torrent_path = self.build_torrent({"a.csv": (1, MTIME)})
        quietly(seed.check_torrent_identity, torrent_path, self.TORRENT_ID, self.v1_hash)  # doesn't raise

    def test_wrong_name_fails(self):
        torrent_path = self.build_torrent({"a.csv": (1, MTIME)})
        with self.assertRaises(SystemExit):
            quietly(seed.check_torrent_identity, torrent_path, "everef-eve-online-deep-archive-99", self.v1_hash)

    def test_wrong_hash_fails(self):
        torrent_path = self.build_torrent({"a.csv": (1, MTIME)})
        with self.assertRaises(SystemExit):
            quietly(seed.check_torrent_identity, torrent_path, self.TORRENT_ID, "0" * 40)


class LoadOrBuildPlanTest(unittest.TestCase):
    def test_builds_and_saves_a_plan(self):
        with tempfile.TemporaryDirectory() as tmp:
            content_dir = os.path.join(tmp, "staging")
            seed_dir = os.path.join(tmp, "seed")
            os.makedirs(content_dir)
            os.makedirs(seed_dir)
            full = os.path.join(content_dir, "a.csv")
            with open(full, "wb") as f:
                f.write(b"x")
            os.utime(full, (MTIME, MTIME))
            plan_path = os.path.join(tmp, "merge-plan.json")
            plan = quietly(seed.load_or_build_plan, plan_path, content_dir, seed_dir, [("a.csv", 1, MTIME)])
            self.assertEqual(len(plan), 1)
            self.assertTrue(os.path.isfile(plan_path))

    def test_loads_an_existing_plan_without_rebuilding(self):
        with tempfile.TemporaryDirectory() as tmp:
            plan_path = os.path.join(tmp, "merge-plan.json")
            saved = [{"path": "a.csv", "size": 1, "mtime": MTIME, "staging": "/gone", "target": "/also-gone",
                      "dev": 1, "ino": 2}]
            with open(plan_path, "w", encoding="utf-8") as f:
                json.dump(saved, f)
            # content_dir doesn't exist: if this tried to rebuild, build_plan would fail on the missing staging file.
            plan = seed.load_or_build_plan(plan_path, "/nonexistent", "/nonexistent", [("a.csv", 1, MTIME)])
            self.assertEqual(plan, saved)

    def test_missing_staging_file_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            content_dir = os.path.join(tmp, "staging")
            os.makedirs(content_dir)
            plan_path = os.path.join(tmp, "merge-plan.json")
            with self.assertRaises(SystemExit):
                quietly(seed.load_or_build_plan, plan_path, content_dir, tmp, [("a.csv", 1, MTIME)])


class CheckNoSubfolderTest(unittest.TestCase):
    class FakeClient:
        def __init__(self, files):
            self._files = files

        def files(self, info_hash):
            return self._files

    def test_matching_names_is_fine(self):
        client = self.FakeClient([{"name": "a/b.csv"}, {"name": "c.csv"}])
        entries = [("a/b.csv", 1, MTIME), ("c.csv", 2, MTIME)]
        seed.check_no_subfolder(client, "abc", entries)  # doesn't raise

    def test_a_root_folder_in_the_names_fails(self):
        torrent_id = "everef-eve-online-deep-archive-00"
        client = self.FakeClient([{"name": f"{torrent_id}/a/b.csv"}])
        entries = [("a/b.csv", 1, MTIME)]
        with self.assertRaises(SystemExit):
            quietly(seed.check_no_subfolder, client, "abc", entries)

    def test_backslash_names_are_normalised(self):
        client = self.FakeClient([{"name": "a\\b.csv"}])
        entries = [("a/b.csv", 1, MTIME)]
        seed.check_no_subfolder(client, "abc", entries)  # doesn't raise


if __name__ == "__main__":
    unittest.main()
