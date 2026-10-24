"""
Tests for lib/workflow.py's shared helpers: dataset/year classification (classify_path), the torrent comment
(render_comment), timestamp formatting (iso/parse_iso), and the file list format (file_list_problems,
site_path_problem). These are shared by 01-find, 03-load, 04-make-torrent, 07-publish, 09-check-before-delete and
10-delete, so a regression here would hit every one of them.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import gzip
import json
import os
import re
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
from workflow import (  # noqa: E402
    MANUAL_STEPS,
    STEPS,
    classify_path,
    file_list_problems,
    iso,
    parse_iso,
    read_listing_gz,
    render_comment,
    site_path_problem,
    step_name,
)


class ClassifyPathTest(unittest.TestCase):
    def test_dated_layout(self):
        self.assertEqual(
            classify_path("market-orders/history/2024/01/market-orders-2024-01-01.csv.bz2"),
            (2024, "market-orders"),
        )

    def test_backfills_suffix_is_stripped(self):
        self.assertEqual(
            classify_path("fuzzwork/ordersets/backfills/2021/orderset-21000.csv.gz"),
            (2021, "fuzzwork/ordersets"),
        )

    def test_history_suffix_is_stripped(self):
        self.assertEqual(
            classify_path("killmails/history/2020/killmail-1.json"),
            (2020, "killmails"),
        )

    def test_no_year_folder_falls_back_to_first_three_directories(self):
        year, dataset = classify_path("ccp/sde/older/sde-2019.zip")
        self.assertIsNone(year)
        self.assertEqual(dataset, "ccp/sde/older")

    def test_no_year_folder_still_strips_a_trailing_suffix(self):
        year, dataset = classify_path("fuzzwork/ordersets/backfills/orderset-21000.csv.gz")
        self.assertIsNone(year)
        self.assertEqual(dataset, "fuzzwork/ordersets")

    def test_directly_under_the_year_folder_has_no_dataset(self):
        self.assertEqual(classify_path("2024/root-file.json"), (2024, ""))

    def test_root_level_file_has_no_year_or_dataset(self):
        self.assertEqual(classify_path("robots.txt"), (None, ""))

    def test_first_year_folder_wins(self):
        # A year-looking directory inside the dataset name itself (not realistic, but the first match should win).
        self.assertEqual(classify_path("a/2020/b/2021/file.json"), (2020, "a"))


class RenderCommentTest(unittest.TestCase):
    def test_single_year_and_datasets_sorted(self):
        comment = render_comment("everef-deep-archive-2024-01-01", {2024}, {"b", "a"})
        self.assertIn("Data from 2024. Datasets: a, b.", comment)
        self.assertIn("everef-deep-archive-2024-01-01", comment)

    def test_year_range(self):
        comment = render_comment("id", {2022, 2020, 2021}, {"a"})
        self.assertIn("Data from 2020–2022.", comment)

    def test_no_years_or_datasets(self):
        comment = render_comment("id", set(), set())
        self.assertIn("Data from unknown years. Datasets: none.", comment)

    def test_long_dataset_list_is_truncated(self):
        datasets = {f"dataset-{i:03d}" for i in range(50)}
        comment = render_comment("id", {2024}, datasets)
        line = [line for line in comment.splitlines() if line.startswith("Data from")][0]
        self.assertTrue(line.endswith("...."))
        self.assertLessEqual(len(line), len("Data from 2024. Datasets: ") + 200 + len(", ..."))


class IsoTest(unittest.TestCase):
    def test_round_trip(self):
        value = "2024-06-15T12:34:56Z"
        self.assertEqual(iso(parse_iso(value)), value)

    def test_iso_format(self):
        self.assertEqual(iso(0), "1970-01-01T00:00:00Z")


class FileListProblemsTest(unittest.TestCase):
    def test_well_formed_list_has_no_problems(self):
        self.assertEqual(file_list_problems("a/b.txt\nc/d.txt\n"), [])

    def test_empty_file(self):
        self.assertEqual(file_list_problems(""), ["empty file"])

    def test_missing_trailing_newline(self):
        self.assertIn("doesn't end with \\n", file_list_problems("a/b.txt")[0])

    def test_carriage_return(self):
        problems = file_list_problems("a/b.txt\r\n")
        self.assertTrue(any("\\r" in p for p in problems))

    def test_not_sorted(self):
        problems = file_list_problems("b/b.txt\na/a.txt\n")
        self.assertTrue(any("not sorted" in p for p in problems))

    def test_duplicate(self):
        problems = file_list_problems("a/a.txt\na/a.txt\n")
        self.assertTrue(any("duplicate" in p for p in problems))

    def test_leading_slash(self):
        self.assertIn("starts with /", site_path_problem("/a.txt"))

    def test_root_level_path(self):
        self.assertIn("site root", site_path_problem("a.txt"))

    def test_torrent_root_folder_prefix(self):
        problem = site_path_problem("everef-deep-archive-2024-01-01/market-orders/a.csv")
        self.assertIn("torrent ID", problem)

    def test_deep_archive_path(self):
        self.assertIn("deep-archive/", site_path_problem("deep-archive/everef-deep-archive-2024-01-01.torrent"))

    def test_dot_segment(self):
        self.assertIn("segment", site_path_problem("a/../b.txt"))


class ReadListingGzTest(unittest.TestCase):
    def test_reads_paths_written_by_find(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "listing.jsonl.gz")
            with gzip.open(path, "wt", encoding="utf-8") as f:
                f.write(json.dumps({"path": "a/b.txt", "size": 1, "last_modified": "2024-01-01T00:00:00Z"}) + "\n")
                f.write(json.dumps({"path": "c/d.txt", "size": 2, "last_modified": "2024-01-02T00:00:00Z"}) + "\n")
            self.assertEqual(read_listing_gz(path), {"a/b.txt", "c/d.txt"})

    def test_skips_blank_lines(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "listing.jsonl.gz")
            with gzip.open(path, "wt", encoding="utf-8") as f:
                f.write(json.dumps({"path": "a/b.txt", "size": 1, "last_modified": "2024-01-01T00:00:00Z"}) + "\n")
                f.write("\n")
            self.assertEqual(read_listing_gz(path), {"a/b.txt"})


class StepsTest(unittest.TestCase):
    """STEPS numbers the scripts and their completion files; the two must never drift apart."""

    def test_every_scripted_step_has_its_script(self):
        for step in STEPS:
            if step not in MANUAL_STEPS:
                self.assertTrue(os.path.isfile(os.path.join(HERE, "..", f"{step_name(step)}.py")))

    def test_every_numbered_script_is_a_scripted_step(self):
        scripted = {f"{step_name(s)}.py" for s in STEPS if s not in MANUAL_STEPS}
        numbered = {name for name in os.listdir(os.path.join(HERE, "..")) if re.match(r"^\d\d-.*\.py$", name)}
        self.assertEqual(numbered, scripted)

    def test_manual_steps_are_steps(self):
        self.assertLessEqual(MANUAL_STEPS, set(STEPS))

    def test_step_names_are_unique(self):
        self.assertEqual(len(STEPS), len(set(STEPS)))


if __name__ == "__main__":
    unittest.main()
