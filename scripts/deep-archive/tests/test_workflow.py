"""
Tests for lib/workflow.py's shared helpers: dataset/year classification (classify_path), the torrent comment
(render_comment), timestamp formatting (iso/parse_iso), and the file list format (file_list_problems,
site_path_problem). These are shared by 01-find, 03-load, 05-make-torrent, 09-publish and the purge scripts, so a
regression here would hit every one of them.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import gzip
import io
import json
import os
import re
import sys
import tempfile
import unittest
from unittest.mock import patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
import workflow  # noqa: E402
from workflow import (  # noqa: E402
    MANUAL_STEPS,
    MINISIGN_IMAGE,
    RCLONE_FIXED,
    STEPS,
    classify_path,
    file_list_problems,
    format_torrent_id,
    index_tar_name,
    iso,
    load_env,
    match_paths,
    merge_conflicts,
    parse_iso,
    previous_scripted_step,
    print_env_overrides,
    read_listing_gz,
    render_comment,
    root_file_names,
    site_path_problem,
    step_name,
    torrent_excluded,
    torrent_sequence,
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


class MatchPathsTest(unittest.TestCase):
    def test_the_folder_and_inside_it(self):
        self.assertEqual(match_paths("ccp", ["ccp"]), "ccp")
        self.assertEqual(match_paths("ccp/sde/a.zip", ["killmails", "ccp"]), "ccp")

    def test_by_whole_folder_name_not_prefix(self):
        self.assertIsNone(match_paths("ccp-other/a.json", ["ccp"]))

    def test_no_patterns(self):
        self.assertIsNone(match_paths("ccp/a.json", []))

    def test_first_matching_pattern(self):
        self.assertEqual(match_paths("a/b/c.json", ["x", "a/*", "a"]), "a/*")

    def test_star_stays_within_a_folder(self):
        self.assertEqual(match_paths("a/history/2024/x.json", ["*/history"]), "*/history")
        self.assertIsNone(match_paths("a/b/history/2024/x.json", ["*/history"]))
        self.assertIsNone(match_paths("a/b.json", ["a*b.json"]))

    def test_double_star_matches_any_number_of_folders(self):
        self.assertEqual(match_paths("a/history/x.json", ["**/history"]), "**/history")
        self.assertEqual(match_paths("a/b/c/history/x.json", ["**/history"]), "**/history")
        self.assertEqual(match_paths("history/x.json", ["**/history"]), "**/history")
        self.assertEqual(match_paths("a/b/c.json", ["a/**/c.json"]), "a/**/c.json")
        self.assertIsNone(match_paths("a/b/d.json", ["a/**/c.json"]))

    def test_character_classes_and_question_mark(self):
        pattern = "*/history/[0-9][0-9][0-9][0-9]"
        self.assertEqual(match_paths("a/history/2024/2024-01-01/x.json", [pattern]), pattern)
        self.assertIsNone(match_paths("a/history/backfills/x.json", [pattern]))
        self.assertIsNone(match_paths("a/history/a-2023.tar.xz", [pattern]))
        self.assertEqual(match_paths("a/b1.json", ["a/b?.json"]), "a/b?.json")

    def test_case_sensitive(self):
        self.assertIsNone(match_paths("CCP/a.json", ["ccp"]))


class TorrentExcludedTest(unittest.TestCase):
    def test_excluded_unless_included(self):
        self.assertEqual(torrent_excluded("a/history/2024/x.json", ["*/history"], []), "*/history")
        self.assertIsNone(torrent_excluded("a/history/2024/x.json", ["*/history"], ["a/history"]))
        self.assertEqual(torrent_excluded("b/history/2024/x.json", ["*/history"], ["a/history"]), "*/history")

    def test_include_alone_adds_nothing(self):
        self.assertIsNone(torrent_excluded("a/x.json", [], ["b"]))

    def excluded(self, path):
        return torrent_excluded(path) is not None

    def test_config_rolled_up_history(self):
        self.assertTrue(self.excluded("structures/history/2026/2026-01-01/structures-2026-01-01_00-00-00.v2.json.bz2"))
        self.assertFalse(self.excluded("structures/history/structures-2023.tar.xz"))
        # Too new to be rolled up yet: excluded until it is.
        self.assertTrue(self.excluded("military-campaigns/history/2026/2026-01-01/a.json.bz2"))

    def test_config_history_backfills(self):
        self.assertFalse(self.excluded("incursions/history/backfills/a.json.bz2"))
        self.assertFalse(self.excluded("public-contracts/history/backfills/a.tar.bz2"))

    def test_config_never_rolled_up(self):
        self.assertFalse(self.excluded("market-orders/history/2024/2024-01-01/market-orders-2024-01-01.v3.csv.bz2"))
        self.assertFalse(self.excluded("public-contracts/history/2024/2024-01-01/a.v2.tar.bz2"))
        self.assertFalse(self.excluded("esi-scrape/history/2024/eve-ref-esi-scrape-2024-01-01.tar.xz"))
        self.assertFalse(self.excluded("reference-data/history/2024/reference-data-2024-01-01.tar.xz"))
        self.assertFalse(self.excluded("hoboleaks-sde/history/2024/hoboleaks-sde-2024-01-01.tar.xz"))

    def test_config_wars_history(self):
        self.assertTrue(self.excluded("wars/history/2024/wars-2024-01-01.tar.bz2"))
        self.assertTrue(self.excluded("wars/history/wars-2003.tar.bz2"))

    def test_config_ccp(self):
        self.assertFalse(self.excluded("ccp/csm/a.pdf"))
        self.assertFalse(self.excluded("ccp/mer/2019/a.zip"))
        self.assertFalse(self.excluded("ccp/sde/2019/sde-20190101-TRANQUILITY.zip"))

    def test_config_current_files(self):
        self.assertTrue(self.excluded("wars/wars-current.json"))
        self.assertFalse(self.excluded("wars/current-thing.json"))

    def test_config_databases_and_deep_archive(self):
        self.assertTrue(self.excluded("market-history/2019/market-history-2019-01-01.csv.bz2"))
        self.assertTrue(self.excluded("killmails/2019/killmails-2019-01-01.tar.bz2"))
        self.assertTrue(self.excluded("deep-archive/everef-eve-online-deep-archive-00.txt"))


class RenderCommentTest(unittest.TestCase):
    def test_includes_the_two_digit_sequence_number(self):
        comment = render_comment(format_torrent_id(7))
        self.assertIn("07", comment)
        self.assertNotIn(format_torrent_id(7), comment)

    def test_is_a_single_line(self):
        comment = render_comment(format_torrent_id(0))
        self.assertEqual(len(comment.splitlines()), 1)

    def test_fails_for_a_name_that_is_not_a_torrent_id(self):
        with self.assertRaises(SystemExit):
            render_comment("everef-eve-online-deep-archive-2023-04-11")


class TorrentIdTest(unittest.TestCase):
    def test_format_is_two_digit_zero_padded(self):
        self.assertEqual(format_torrent_id(0), "everef-eve-online-deep-archive-00")
        self.assertEqual(format_torrent_id(9), "everef-eve-online-deep-archive-09")
        self.assertEqual(format_torrent_id(42), "everef-eve-online-deep-archive-42")

    def test_sequence_round_trips(self):
        self.assertEqual(torrent_sequence(format_torrent_id(7)), 7)

    def test_sequence_none_for_old_date_based_ids(self):
        self.assertIsNone(torrent_sequence("everef-eve-online-deep-archive-2023-04-11"))

    def test_sequence_none_for_non_ids(self):
        self.assertIsNone(torrent_sequence("feed"))


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
        problem = site_path_problem("everef-eve-online-deep-archive-2024-01-01/market-orders/a.csv")
        self.assertIn("torrent ID", problem)

    def test_deep_archive_path(self):
        self.assertIn("deep-archive/", site_path_problem("deep-archive/everef-eve-online-deep-archive-2024-01-01.torrent"))

    def test_dot_segment(self):
        self.assertIn("segment", site_path_problem("a/../b.txt"))

    def test_index_json(self):
        self.assertIn("index file", site_path_problem("a/b/index.json"))

    def test_index_html(self):
        self.assertIn("index file", site_path_problem("a/index.html"))


class RootFileNamesTest(unittest.TestCase):
    def test_includes_index_tarball(self):
        torrent_id = "everef-eve-online-deep-archive-00"
        names = root_file_names(torrent_id)
        self.assertIn(f"{torrent_id}-index.tar.xz", names)
        self.assertEqual(index_tar_name(torrent_id), f"{torrent_id}-index.tar.xz")

    def test_all_four_root_files(self):
        names = root_file_names("everef-eve-online-deep-archive-00")
        self.assertEqual(len(names), 4)


class MergeConflictsTest(unittest.TestCase):
    def test_no_conflicts(self):
        self.assertEqual(merge_conflicts(["a/b.txt", "c/d.txt"], ["e/f.txt"]), [])

    def test_case_fold_collision_within_new(self):
        problems = merge_conflicts(["a/B.txt", "a/b.txt"])
        self.assertEqual(len(problems), 1)
        self.assertIn("collide", problems[0])

    def test_unicode_normalisation_collision(self):
        # "é" (e + combining acute) vs "é" (é precomposed): same text, different bytes.
        problems = merge_conflicts(["a/café.txt", "a/café.txt"])
        self.assertEqual(len(problems), 1)

    def test_case_fold_collision_against_existing(self):
        problems = merge_conflicts(["a/B.txt"], ["a/b.txt"])
        self.assertIn("already-archived", problems[0])

    def test_identical_path_in_both_is_not_a_conflict(self):
        # Exact duplicates are the caller's own problem (already_archived), not merge_conflicts'.
        self.assertEqual(merge_conflicts(["a/b.txt"], ["a/b.txt"]), [])

    def test_file_folder_clash_new_is_the_file(self):
        problems = merge_conflicts(["a/b"], ["a/b/c"])
        self.assertEqual(len(problems), 1)
        self.assertIn("a/b", problems[0])
        self.assertIn("a/b/c", problems[0])

    def test_file_folder_clash_new_is_the_folder_contents(self):
        problems = merge_conflicts(["a/b/c"], ["a/b"])
        self.assertEqual(len(problems), 1)

    def test_file_folder_clash_within_new(self):
        problems = merge_conflicts(["a/b", "a/b/c"])
        self.assertEqual(len(problems), 1)

    def test_clash_between_two_existing_paths_is_not_reported(self):
        # Already-archived paths are assumed clean with each other; only new paths matter here.
        self.assertEqual(merge_conflicts([], ["a/b", "a/b/c"]), [])

    def test_unrelated_paths_are_not_folder_conflicts(self):
        self.assertEqual(merge_conflicts(["a/b", "a/bc"]), [])


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


class LoadEnvTest(unittest.TestCase):
    """
    Of all RCLONE_* settings, load_env keeps only the B2 key, so nothing left in the operator's shell (a filter, a
    flag, another option for the remote) changes what rclone lists or deletes.
    """

    def load(self, environ, env_file=None):
        """The RCLONE_* settings after load_env, with this environment and torrents.env (None: no file)."""
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "torrents.env")
            if env_file is not None:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(env_file)
            with patch.dict(os.environ, {**environ, "TORRENTS_ENV": path}, clear=True):
                load_env()
                return {k: v for k, v in os.environ.items() if k.startswith("RCLONE_")}

    def test_keeps_the_b2_key(self):
        env = self.load({"RCLONE_CONFIG_EVEREF_ACCOUNT": "account"}, "RCLONE_CONFIG_EVEREF_KEY=key\n")
        self.assertEqual(
            env, {**RCLONE_FIXED, "RCLONE_CONFIG_EVEREF_ACCOUNT": "account", "RCLONE_CONFIG_EVEREF_KEY": "key"}
        )

    def test_drops_other_rclone_settings_from_the_environment(self):
        env = self.load({"RCLONE_EXCLUDE": "*.bz2", "RCLONE_DRY_RUN": "true", "RCLONE_CONFIG_EVEREF_VERSIONS": "true"})
        self.assertEqual(env, RCLONE_FIXED)

    def test_drops_other_rclone_settings_from_torrents_env(self):
        self.assertEqual(self.load({}, "RCLONE_MAX_AGE=1y\n"), RCLONE_FIXED)

    def test_drops_them_without_a_torrents_env(self):
        self.assertEqual(self.load({"RCLONE_EXCLUDE": "*.bz2"}), RCLONE_FIXED)

    def test_fixed_settings_win(self):
        env = self.load({"RCLONE_CONFIG_EVEREF_HARD_DELETE": "true", "RCLONE_CONFIG": "/home/me/rclone.conf"})
        self.assertEqual(env, RCLONE_FIXED)

    def test_names_what_it_dropped(self):
        self.load({"RCLONE_EXCLUDE": "*.bz2", "RCLONE_CONFIG_EVEREF_HARD_DELETE": "true"})
        self.assertEqual(workflow.RCLONE_IGNORED, ["RCLONE_CONFIG_EVEREF_HARD_DELETE", "RCLONE_EXCLUDE"])
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            print_env_overrides()
        self.assertIn("RCLONE_CONFIG_EVEREF_HARD_DELETE, RCLONE_EXCLUDE", out.getvalue())


class MinisignImageTest(unittest.TestCase):
    """minisign's Docker image is pinned by digest, the same for verifying (lib/workflow.py) and signing (sign.sh)."""

    def test_pinned_by_digest(self):
        self.assertRegex(MINISIGN_IMAGE, r"^jedisct1/minisign@sha256:[0-9a-f]{64}$")

    def test_sign_sh_pins_the_same_image(self):
        with open(os.path.join(HERE, "..", "lib", "sign.sh"), encoding="utf-8") as f:
            pins = re.findall(r'^MINISIGN_IMAGE="([^"]*)"$', f.read(), re.MULTILINE)
        self.assertEqual(pins, [MINISIGN_IMAGE])


class StepsTest(unittest.TestCase):
    """STEPS numbers the scripts and their completion files; the two must never drift apart."""

    def scripts(self):
        """step_name -> script file name, for every numbered .py and .sh script."""
        names = os.listdir(os.path.join(HERE, ".."))
        return {name.rsplit(".", 1)[0]: name for name in names if re.match(r"^\d\d-.*\.(py|sh)$", name)}

    def test_every_numbered_script_is_a_scripted_step(self):
        scripted = {step_name(s) for s in STEPS if s not in MANUAL_STEPS}
        self.assertEqual(set(self.scripts()), scripted)

    def test_no_step_has_both_a_py_and_a_sh_script(self):
        names = os.listdir(os.path.join(HERE, ".."))
        self.assertEqual(len(self.scripts()), len([n for n in names if re.match(r"^\d\d-.*\.(py|sh)$", n)]))

    def test_bash_steps_wait_for_the_previous_scripted_step(self):
        # lib/sign.sh can't read STEPS: each bash step names its previous step itself, in its sign_step call.
        for name, script in self.scripts().items():
            if not script.endswith(".sh"):
                continue
            with open(os.path.join(HERE, "..", script), encoding="utf-8") as f:
                calls = re.findall(r"^sign_step (\S+) ", f.read(), re.MULTILINE)
            step = name.split("-", 1)[1]
            self.assertEqual(calls, [step_name(previous_scripted_step(step))])

    def test_manual_steps_are_steps(self):
        self.assertLessEqual(MANUAL_STEPS, set(STEPS))

    def test_step_names_are_unique(self):
        self.assertEqual(len(STEPS), len(set(STEPS)))


if __name__ == "__main__":
    unittest.main()
