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
            checks.fail("Missing required tool: aria2c")

        exit, mocks = self.run_main(check_tools)
        self.assertEqual(exit.code, 1)
        mocks["check_trackers"].assert_not_called()
        mocks["check_remote"].assert_not_called()
        mocks["check_qbittorrent"].assert_not_called()


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
