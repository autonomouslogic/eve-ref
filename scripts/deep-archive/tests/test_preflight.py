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
from unittest.mock import DEFAULT, patch

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


if __name__ == "__main__":
    unittest.main()
