"""
Tests for 00-preflight.py: a missing tool aborts preflight right after the Tools section, before the later checks.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
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


if __name__ == "__main__":
    unittest.main()
