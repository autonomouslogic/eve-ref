"""
Tests for 00-preflight.py: a missing tool aborts preflight right after the Tools section, before the later checks.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
import json
import os
import sys
import tempfile
import unittest
from unittest.mock import DEFAULT, Mock, patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("preflight", os.path.join(HERE, "..", "00-preflight.py"))
preflight = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(preflight)


class MainTest(unittest.TestCase):
    def run_main(self, check_tools):
        with tempfile.TemporaryDirectory() as tmp:
            env = {"SCRATCH_DIR": os.path.join(tmp, "scratch"), "SEED_DIR": os.path.join(tmp, "seed")}
            os.makedirs(env["SEED_DIR"])
            with (
                patch.dict(os.environ, env),
                patch.multiple(
                    preflight,
                    load_env=lambda: None,
                    lock=DEFAULT,
                    start_log=DEFAULT,
                    check_tools=check_tools,
                    check_trackers=DEFAULT,
                    check_remote=DEFAULT,
                    check_qbittorrent=DEFAULT,
                ) as mocks,
                self.assertRaises(SystemExit) as exit,
            ):
                preflight.main()
        return exit.exception, mocks

    def test_missing_tool_aborts_before_later_checks(self):
        def check_tools(checks):
            checks.fail("Missing required tool: sha256sum")

        exit, mocks = self.run_main(check_tools)
        self.assertEqual(exit.code, 1)
        mocks["check_trackers"].assert_not_called()
        mocks["check_remote"].assert_not_called()
        mocks["check_qbittorrent"].assert_not_called()


class CheckStagingTest(unittest.TestCase):
    def check(self, seed):
        checks = preflight.Checks()
        preflight.check_staging(checks, seed)
        return checks

    def test_missing_folders_are_not_created(self):
        with tempfile.TemporaryDirectory() as seed:
            checks = self.check(seed)
            self.assertEqual(checks.failed, 0)
            self.assertEqual(os.listdir(seed), [])

    def test_missing_torrents_folder_warns(self):
        with tempfile.TemporaryDirectory() as seed:
            os.makedirs(os.path.join(seed, ".staging"))
            with patch.object(preflight, "warning") as warn:
                checks = self.check(seed)
            self.assertEqual(checks.failed, 0)
            warn.assert_called_once()
            self.assertIn(".torrents", warn.call_args[0][0])

    def test_existing_torrents_folder_does_not_warn(self):
        with tempfile.TemporaryDirectory() as seed:
            os.makedirs(os.path.join(seed, ".staging"))
            os.makedirs(os.path.join(seed, ".torrents"))
            with patch.object(preflight, "warning") as warn:
                self.check(seed)
            warn.assert_not_called()

    def test_empty_staging_is_fine(self):
        with tempfile.TemporaryDirectory() as seed:
            os.makedirs(os.path.join(seed, ".staging"))
            os.makedirs(os.path.join(seed, ".torrents"))
            self.assertEqual(self.check(seed).failed, 0)

    def test_nonempty_staging_fails(self):
        with tempfile.TemporaryDirectory() as seed:
            staging = os.path.join(seed, ".staging", "everef-eve-online-deep-archive-00")
            os.makedirs(staging)
            with open(os.path.join(staging, "a.csv"), "w", encoding="utf-8") as f:
                f.write("x")
            self.assertEqual(self.check(seed).failed, 1)

    def test_staging_as_a_file_fails(self):
        with tempfile.TemporaryDirectory() as seed:
            with open(os.path.join(seed, ".staging"), "w", encoding="utf-8") as f:
                f.write("x")
            self.assertEqual(self.check(seed).failed, 1)


class PrepareDirTest(unittest.TestCase):
    def test_creates_missing_dir_by_default(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "a", "b")
            with patch.dict(os.environ, {"SCRATCH_DIR": path}):
                found, created, problem = preflight.prepare_dir("SCRATCH_DIR")
            self.assertEqual((found, created, problem), (os.path.realpath(path), True, None))
            self.assertTrue(os.path.isdir(path))

    def test_missing_dir_with_create_off_is_a_problem_and_not_created(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "seed")
            with patch.dict(os.environ, {"SEED_DIR": path}):
                found, created, problem = preflight.prepare_dir("SEED_DIR", create=False)
            self.assertIsNone(found)
            self.assertFalse(created)
            self.assertIn("does not exist", problem)
            self.assertFalse(os.path.exists(path))


class AfterLocalTorrentsTest(unittest.TestCase):
    """A torrent made here but not published yet doesn't show in the bucket: its ID must not be reused."""

    ID = "everef-eve-online-deep-archive-%02d"

    def after(self, local_sequences, next_sequence):
        with tempfile.TemporaryDirectory() as seed:
            os.makedirs(os.path.join(seed, ".torrents"))
            for sequence in local_sequences:
                with open(os.path.join(seed, ".torrents", f"{self.ID % sequence}.torrent"), "wb") as f:
                    f.write(b"")
            checks = preflight.Checks()
            return preflight.after_local_torrents(checks, seed, self.ID % next_sequence), checks

    def test_unpublished_local_torrent_moves_the_next_id_on(self):
        self.assertEqual(self.after([0], 0)[0], self.ID % 1)

    def test_newest_local_torrent_counts(self):
        self.assertEqual(self.after([0, 2, 1], 0)[0], self.ID % 3)

    def test_published_ahead_of_local_keeps_the_published_next_id(self):
        self.assertEqual(self.after([0], 5)[0], self.ID % 5)

    def test_no_local_torrents_or_folder(self):
        self.assertEqual(self.after([], 3)[0], self.ID % 3)
        checks = preflight.Checks()
        self.assertEqual(preflight.after_local_torrents(checks, "/nonexistent", self.ID % 3), self.ID % 3)


class CheckRemoteTest(unittest.TestCase):
    """check_remote also picks the next sequential torrent ID from the published .torrent files."""

    def check_remote(self, lsjson_results):
        """lsjson_results: queue of (stdout, error_line) for successive run_rclone('lsjson', ...) calls."""
        checks = preflight.Checks()
        env = {
            "DATA_BUCKET": "bucket",
            "RCLONE_CONFIG_EVEREF_ACCOUNT": "account",
            "RCLONE_CONFIG_EVEREF_KEY": "key",
        }
        calls = list(lsjson_results)
        with (
            patch.dict(os.environ, env),
            patch.object(preflight, "run_rclone", side_effect=lambda *a: calls.pop(0)),
        ):
            next_id = preflight.check_remote(checks)
        return next_id, checks

    def index_result(self):
        return (json.dumps([{"ModTime": "2024-01-01T00:00:00Z"}]), None)

    def test_no_deep_archive_folder_starts_at_00(self):
        next_id, checks = self.check_remote([self.index_result(), (None, "directory not found")])
        self.assertEqual(next_id, "everef-eve-online-deep-archive-00")
        self.assertEqual(checks.failed, 0)

    def test_empty_deep_archive_folder_starts_at_00(self):
        next_id, checks = self.check_remote([self.index_result(), ("[]", None)])
        self.assertEqual(next_id, "everef-eve-online-deep-archive-00")
        self.assertEqual(checks.failed, 0)

    def test_next_id_is_one_past_the_highest_published(self):
        listing = json.dumps(
            [
                {"Path": "everef-eve-online-deep-archive-00.torrent"},
                {"Path": "everef-eve-online-deep-archive-01.torrent"},
                {"Path": "everef-eve-online-deep-archive-00.txt"},
            ]
        )
        next_id, checks = self.check_remote([self.index_result(), (listing, None)])
        self.assertEqual(next_id, "everef-eve-online-deep-archive-02")
        self.assertEqual(checks.failed, 0)

    def test_invalid_torrent_id_fails(self):
        listing = json.dumps([{"Path": "everef-eve-online-deep-archive-2023-04-11.torrent"}])
        next_id, checks = self.check_remote([self.index_result(), (listing, None)])
        self.assertIsNone(next_id)
        self.assertEqual(checks.failed, 1)

    def test_sequence_exhausted_fails(self):
        listing = json.dumps([{"Path": "everef-eve-online-deep-archive-99.torrent"}])
        next_id, checks = self.check_remote([self.index_result(), (listing, None)])
        self.assertIsNone(next_id)
        self.assertEqual(checks.failed, 1)


class CheckQbittorrentTest(unittest.TestCase):
    """qBittorrent must run libtorrent 2.x (a failure); older than MIN_LIBTORRENT_VERSION is only a warning."""

    def check(self, libtorrent_version):
        checks = preflight.Checks()
        env = {"QBITTORRENT_URL": "http://localhost:8080"}
        client = Mock()
        client.version.return_value = "v5.0.0"
        client.build_info.return_value = {"libtorrent": libtorrent_version}
        with (
            patch.dict(os.environ, env),
            patch.object(preflight, "Client", return_value=client),
        ):
            preflight.check_qbittorrent(checks, seed=None)
        return checks

    def test_libtorrent_1_x_fails(self):
        checks = self.check("1.2.19.0")
        self.assertEqual(checks.failed, 1)

    def test_not_a_version_fails(self):
        checks = self.check("")
        self.assertEqual(checks.failed, 1)

    def test_recent_libtorrent_2_x_has_no_problems(self):
        checks = self.check("2.0.15.0")
        self.assertEqual(checks.failed, 0)

    def test_old_libtorrent_2_x_warns_but_does_not_fail(self):
        with patch.object(preflight, "warning") as warn:
            checks = self.check("2.0.10.0")
        self.assertEqual(checks.failed, 0)
        warn.assert_called_once()
        self.assertIn("2.0.10.0", warn.call_args[0][0])


class CheckTrackersTest(unittest.TestCase):
    def check(self, results, min_working=3):
        checks = preflight.Checks()
        with (
            patch.object(preflight, "check_tracker_connectivity", return_value=results),
            patch.object(preflight, "GOOD_TRACKERS", [r[0] for r in results]),
            patch.object(preflight, "MIN_WORKING_TRACKERS", min_working),
        ):
            preflight.check_trackers(checks)
        return checks

    def test_enough_working_passes(self):
        results = [("udp://a/announce", True, None), ("udp://b/announce", True, None), ("udp://c/announce", False, "timeout")]
        checks = self.check(results, min_working=2)
        self.assertEqual(checks.failed, 0)

    def test_too_few_working_fails(self):
        results = [("udp://a/announce", True, None), ("udp://b/announce", False, "timeout")]
        checks = self.check(results, min_working=2)
        self.assertEqual(checks.failed, 1)

    def test_unchecked_trackers_do_not_count_as_working(self):
        results = [("https://a/announce", None, "not UDP: not checked")]
        checks = self.check(results, min_working=1)
        self.assertEqual(checks.failed, 1)


if __name__ == "__main__":
    unittest.main()
