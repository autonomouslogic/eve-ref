"""
Tests for check-seed-dir.py: the --full flag, and reporting a sample of problems (report).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import importlib.util
import io
import os
import sys
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("check_seed_dir", os.path.join(HERE, "..", "check-seed-dir.py"))
check_seed_dir = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(check_seed_dir)


class ParseArgsTest(unittest.TestCase):
    def parse(self, argv):
        old = sys.argv
        sys.argv = ["check-seed-dir.py", *argv]
        try:
            return check_seed_dir.parse_args()
        finally:
            sys.argv = old

    def test_default_is_not_full(self):
        self.assertFalse(self.parse([]).full)

    def test_full_flag(self):
        self.assertTrue(self.parse(["--full"]).full)


class ReportTest(unittest.TestCase):
    def report(self, problems):
        out = io.StringIO()
        with contextlib.redirect_stderr(out):
            check_seed_dir.report(problems)
        return out.getvalue()

    def test_reports_every_problem_under_the_sample_size(self):
        text = self.report(["a", "b"])
        self.assertIn("a", text)
        self.assertIn("b", text)
        self.assertNotIn("more", text)

    def test_truncates_past_the_sample_size(self):
        problems = [f"problem {i}" for i in range(check_seed_dir.LOG_SAMPLE + 5)]
        text = self.report(problems)
        self.assertIn("5 more", text)


if __name__ == "__main__":
    unittest.main()
