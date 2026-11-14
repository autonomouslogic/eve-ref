"""
Tests for the purge (lib/purge.py and d00-preflight.py to d04-delete.py): what may be deleted (blockers), the plan
and the check of a reviewed list (plan_purge, verify_list), reading rclone's dry run, index-only folders, the
published-torrent check, the step rules, and d03-dryrun and d04-delete against a fake site.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import datetime
import importlib.util
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
import purge  # noqa: E402
from purge import (  # noqa: E402
    ArchivedFile,
    CHANGED,
    GONE,
    NO_SEED_COPY,
    NOT_ARCHIVED,
    PROTECTED,
    RECENT,
    blockers,
)
from workflow import iso  # noqa: E402


def load_script(name, file):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, "..", file))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


preflight = load_script("d00_preflight", "d00-preflight.py")
find = load_script("d01_find", "d01-find.py")
dryrun = load_script("d03_dryrun", "d03-dryrun.py")
delete = load_script("d04_delete", "d04-delete.py")

ID = "everef-eve-online-deep-archive-00"
OLD = int(datetime.datetime(2023, 5, 1, tzinfo=datetime.timezone.utc).timestamp())
LAST_YEAR = int(datetime.datetime(2025, 5, 1, tzinfo=datetime.timezone.utc).timestamp())
THIS_YEAR = int(datetime.datetime(2026, 5, 1, tzinfo=datetime.timezone.utc).timestamp())
FIRST_KEPT_YEAR = 2025
TODAY = datetime.date(2026, 11, 14)

OLD_FILE = "market-orders/history/2023/2023-05-01/market-orders-2023-05-01_00-00-00.v3.csv.bz2"
OLD_FILE_2 = "public-contracts/history/2023/2023-05-01/public-contracts-2023-05-01_00-00-00.v2.dat.bz2"
LAST_YEAR_FILE = "market-orders/history/2025/2025-05-01/market-orders-2025-05-01_00-00-00.v3.csv.bz2"
THIS_YEAR_FILE = "market-orders/history/2026/2026-05-01/market-orders-2026-05-01_00-00-00.v3.csv.bz2"


def quietly(function, *args, **kwargs):
    with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
        return function(*args, **kwargs)


class Fixture(unittest.TestCase):
    """A SEED_DIR holding the archived files, an archive, and a site listing that matches them."""

    FILES = {OLD_FILE: OLD, OLD_FILE_2: OLD, LAST_YEAR_FILE: LAST_YEAR, THIS_YEAR_FILE: THIS_YEAR}

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.seed = os.path.join(self.temp.name, "seed")
        self.purge_dir = os.path.join(self.temp.name, "purge")
        os.makedirs(self.seed)
        os.makedirs(self.purge_dir)
        self.archived = {}
        self.site = {}
        for path, mtime in self.FILES.items():
            self.add_file(path, mtime)

    def add_file(self, path, mtime, content=b"0123456789"):
        full = os.path.join(self.seed, *path.split("/"))
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "wb") as f:
            f.write(content)
        os.utime(full, (mtime, mtime))
        self.archived[path] = ArchivedFile(ID, len(content), mtime)
        self.site[path] = (len(content), mtime + 0.25)

    def blockers(self, path):
        return blockers(path, self.archived, self.site, lambda p: purge.seed_copy_stat(self.seed, p), FIRST_KEPT_YEAR)


class BlockersTest(Fixture):
    def test_old_unchanged_copied_file_may_go(self):
        self.assertEqual(self.blockers(OLD_FILE), [])

    def test_last_year_stays(self):
        self.assertEqual(self.blockers(LAST_YEAR_FILE), [RECENT])

    def test_this_year_stays(self):
        self.assertEqual(self.blockers(THIS_YEAR_FILE), [RECENT])

    def test_the_last_second_of_the_year_before_last_may_go(self):
        UTC = datetime.timezone.utc
        path = "x/boundary.json"
        self.add_file(path, int(datetime.datetime(2024, 12, 31, 23, 59, 59, tzinfo=UTC).timestamp()))
        self.assertEqual(self.blockers(path), [])

    def test_the_first_second_of_last_year_stays(self):
        UTC = datetime.timezone.utc
        path = "x/boundary.json"
        self.add_file(path, int(datetime.datetime(2025, 1, 1, tzinfo=UTC).timestamp()))
        self.assertEqual(self.blockers(path), [RECENT])

    def test_a_year_in_the_name_dates_an_old_upload_old(self):
        path = "ccp/other/backfill-2019.zip"
        self.add_file(path, THIS_YEAR)
        self.assertEqual(self.blockers(path), [])

    def test_a_date_in_the_name_never_makes_an_old_file_recent(self):
        path = "x/report-2026-01-01.json"
        self.add_file(path, OLD)
        self.assertEqual(self.blockers(path), [])

    def test_not_in_a_torrent(self):
        self.site["a/b.json"] = (1, OLD)
        self.assertEqual(self.blockers("a/b.json"), [NOT_ARCHIVED])

    def test_gone_from_the_site(self):
        del self.site[OLD_FILE]
        self.assertEqual(self.blockers(OLD_FILE), [GONE])

    def test_other_size_on_the_site(self):
        self.site[OLD_FILE] = (11, OLD)
        self.assertEqual(self.blockers(OLD_FILE), [CHANGED])

    def test_other_modification_time_on_the_site(self):
        self.site[OLD_FILE] = (10, OLD + 1)
        self.assertEqual(self.blockers(OLD_FILE), [CHANGED])

    def test_no_copy_in_seed_dir(self):
        os.remove(os.path.join(self.seed, *OLD_FILE.split("/")))
        self.assertEqual(self.blockers(OLD_FILE), [NO_SEED_COPY])

    def test_other_size_in_seed_dir(self):
        with open(os.path.join(self.seed, *OLD_FILE.split("/")), "wb") as f:
            f.write(b"short")
        os.utime(os.path.join(self.seed, *OLD_FILE.split("/")), (OLD, OLD))
        self.assertEqual(self.blockers(OLD_FILE), [NO_SEED_COPY])

    def test_other_modification_time_in_seed_dir(self):
        os.utime(os.path.join(self.seed, *OLD_FILE.split("/")), (OLD + 5, OLD + 5))
        self.assertEqual(self.blockers(OLD_FILE), [NO_SEED_COPY])

    def test_symlink_in_seed_dir_isnt_a_copy(self):
        full = os.path.join(self.seed, *OLD_FILE.split("/"))
        target = os.path.join(self.temp.name, "elsewhere")
        os.rename(full, target)
        os.symlink(target, full)
        self.assertEqual(self.blockers(OLD_FILE), [NO_SEED_COPY])

    def test_never_purged_paths(self):
        for path in ("ccp/csm/2019/csm.pdf", "ccp/mer/2019-01/mer.zip", "market-history/2019/2019-01-01/x.csv.bz2",
                     "killmails/2019/2019-01-01/k.tar.bz2"):
            self.add_file(path, OLD)
            self.assertEqual(self.blockers(path), [PROTECTED], path)

    def test_ccp_next_to_csm_and_mer_isnt_protected(self):
        path = "ccp/sde/sde-2019.zip"
        self.add_file(path, OLD)
        self.assertEqual(self.blockers(path), [])

    def test_index_files_and_deep_archive_are_never_deletable(self):
        for path in ("market-orders/index.json", "deep-archive/x.torrent", "index.json"):
            self.add_file(path, OLD)
            self.assertNotEqual(self.blockers(path), [], path)

    def test_seed_dir_isnt_looked_at_for_files_that_stay_anyway(self):
        calls = []
        blockers(LAST_YEAR_FILE, self.archived, self.site, lambda p: calls.append(p), FIRST_KEPT_YEAR)
        self.assertEqual(calls, [])


class PlanTest(Fixture):
    def plan(self):
        return purge.plan_purge(self.archived, self.site, self.seed, FIRST_KEPT_YEAR)

    def test_deletes_only_the_old_files(self):
        plan = self.plan()
        self.assertEqual([r["path"] for r in plan.delete], sorted([OLD_FILE, OLD_FILE_2]))
        self.assertEqual(sorted(plan.stays[RECENT]), sorted([LAST_YEAR_FILE, THIS_YEAR_FILE]))

    def test_records(self):
        record = self.plan().delete[0]
        self.assertEqual(
            record,
            {
                "path": OLD_FILE,
                "size": 10,
                "last_modified": iso(OLD),
                "date": "2023-05-01",
                "dated_by": "modified",
                "torrent_id": ID,
            },
        )

    def test_gone_and_changed_are_reported_not_deleted(self):
        del self.site[OLD_FILE]
        self.site[OLD_FILE_2] = (99, OLD)
        plan = self.plan()
        self.assertEqual((plan.delete, plan.gone, plan.stays[CHANGED]), ([], [OLD_FILE], [OLD_FILE_2]))

    def test_site_files_in_no_torrent_are_never_considered(self):
        self.site["fuzzwork/ordersets/2019/x.csv.gz"] = (5, OLD)
        paths = [r["path"] for r in self.plan().delete]
        self.assertNotIn("fuzzwork/ordersets/2019/x.csv.gz", paths)


class VerifyListTest(Fixture):
    def verify(self, paths):
        return quietly(purge.verify_list, paths, self.archived, self.site, self.seed, FIRST_KEPT_YEAR)

    def test_a_good_list(self):
        records, gone = self.verify([OLD_FILE, OLD_FILE_2])
        self.assertEqual(([r["path"] for r in records], gone), ([OLD_FILE, OLD_FILE_2], []))

    def test_a_trimmed_list_is_fine(self):
        records, _ = self.verify([OLD_FILE])
        self.assertEqual([r["path"] for r in records], [OLD_FILE])

    def test_gone_paths_are_dropped(self):
        del self.site[OLD_FILE_2]
        records, gone = self.verify([OLD_FILE, OLD_FILE_2])
        self.assertEqual(([r["path"] for r in records], gone), ([OLD_FILE], [OLD_FILE_2]))

    def test_a_recent_file_on_the_list_fails_everything(self):
        with self.assertRaises(SystemExit):
            self.verify([OLD_FILE, LAST_YEAR_FILE])

    def test_a_path_in_no_torrent_fails(self):
        self.site["a/b.json"] = (1, OLD)
        with self.assertRaises(SystemExit):
            self.verify(["a/b.json"])

    def test_a_changed_file_fails(self):
        self.site[OLD_FILE] = (10, OLD + 60)
        with self.assertRaises(SystemExit):
            self.verify([OLD_FILE])

    def test_a_missing_seed_copy_fails(self):
        os.remove(os.path.join(self.seed, *OLD_FILE.split("/")))
        with self.assertRaises(SystemExit):
            self.verify([OLD_FILE])

    def test_a_path_in_no_torrent_and_off_the_site_still_fails(self):
        with self.assertRaises(SystemExit):
            self.verify(["never/archived.json"])


class StaleYearTest(unittest.TestCase):
    def test_same_year_is_fine(self):
        purge.check_not_stale_year(2025, datetime.date(2026, 12, 31))

    def test_the_new_year_needs_a_new_list(self):
        with self.assertRaises(SystemExit):
            quietly(purge.check_not_stale_year, 2025, datetime.date(2027, 1, 1))

    def test_first_kept_year(self):
        self.assertEqual(purge.keep_from_year(datetime.date(2026, 1, 1)), 2025)
        self.assertEqual(purge.keep_from_year(datetime.date(2026, 12, 31)), 2025)


class ArchivedDataFilesTest(unittest.TestCase):
    def torrents(self, **by_id):
        return {tid: {"files": files, "v1_hash": "0" * 40} for tid, files in by_id.items()}

    def test_root_files_are_left_out(self):
        files = {OLD_FILE: (10, OLD), f"{ID}-README.md": (5, OLD), f"{ID}.sha256": (5, OLD),
                 f"{ID}-index.tar.xz": (5, OLD)}
        archived, problems = purge.archived_data_files(self.torrents(**{ID: files}))
        self.assertEqual((list(archived), problems), ([OLD_FILE], []))
        self.assertEqual(archived[OLD_FILE], ArchivedFile(ID, 10, OLD))

    def test_odd_paths_are_problems(self):
        files = {"index.json": (1, OLD), "a/index.json": (1, OLD), "deep-archive/x.txt": (1, OLD), "../x/y": (1, OLD)}
        archived, problems = purge.archived_data_files(self.torrents(**{ID: files}))
        self.assertEqual((archived, len(problems)), ({}, 4))

    def test_a_path_in_two_torrents_is_a_problem(self):
        other = "everef-eve-online-deep-archive-01"
        archived, problems = purge.archived_data_files(
            self.torrents(**{ID: {OLD_FILE: (10, OLD)}, other: {OLD_FILE: (10, OLD)}})
        )
        self.assertEqual(len(problems), 1)
        self.assertIn(OLD_FILE, problems[0])


class DryRunLogTest(unittest.TestCase):
    def test_reads_the_skipped_deletes(self):
        text = "\n".join(
            [
                json.dumps({"level": "notice", "msg": "Skipped delete as --dry-run is set", "object": "a/b.txt",
                            "skipped": "delete"}),
                json.dumps({"level": "notice", "msg": "Not deleting anything else"}),
                "not json",
            ]
        )
        paths, messages = purge.parse_dry_run_log(text)
        self.assertEqual((paths, messages), ({"a/b.txt"}, ["Not deleting anything else", "not json"]))


class CheckDryRunTest(unittest.TestCase):
    def run_with(self, would, returncode=0):
        stderr = "\n".join(json.dumps({"msg": "x", "object": p, "skipped": "delete"}) for p in would)
        result = type("R", (), {"stderr": stderr, "returncode": returncode})()
        with patch.object(purge.subprocess, "run", return_value=result) as run:
            quietly(purge.check_dry_run, "everef:bucket", "list.txt", ["a/1", "a/2"])
        return run

    def test_exact_match(self):
        run = self.run_with(["a/1", "a/2"])
        args = run.call_args[0][0]
        self.assertIn("--dry-run", args)
        self.assertEqual(args[args.index("--max-delete") + 1], "2")

    def test_extra_file_fails(self):
        with self.assertRaises(SystemExit):
            self.run_with(["a/1", "a/2", "a/3"])

    def test_missing_file_fails(self):
        with self.assertRaises(SystemExit):
            self.run_with(["a/1"])

    def test_rclone_error_fails(self):
        with self.assertRaises(SystemExit):
            self.run_with(["a/1", "a/2"], returncode=1)


class IndexOnlyFoldersTest(unittest.TestCase):
    def test_finds_touched_folders_left_with_only_index_files(self):
        site = [
            "a/2020/index.json",
            "a/2020/index.html",
            "a/index.json",
            "b/2020/index.json",
            "b/2021/x.json",
            "b/index.json",
            "deep-archive/index.json",
            "index.json",
        ]
        deleted = {"a/2020/x.json", "b/2020/x.json", "deep-archive/y.txt"}
        self.assertEqual(purge.index_only_folders(site, deleted), ["a", "a/2020", "b/2020"])

    def test_untouched_folders_are_left_alone(self):
        self.assertEqual(purge.index_only_folders(["c/index.json", "index.json"], {"a/x.json"}), [])

    def test_never_the_site_root(self):
        self.assertEqual(purge.index_only_folders(["index.json"], {"x.json"}), [])


class PublishedProblemsTest(unittest.TestCase):
    PATHS = ["a/1", "a/2"]

    def problems(self, torrent=b"t", published=b"t", listing="a/1\na/2\n"):
        return purge.published_problems(ID, b"t", published, listing, self.PATHS)

    def test_matching(self):
        self.assertEqual(self.problems(), [])

    def test_different_torrent(self):
        self.assertEqual(len(self.problems(published=b"other")), 1)

    def test_unpublished(self):
        self.assertEqual(len(purge.published_problems(ID, b"t", None, None, self.PATHS)), 2)

    def test_list_with_extra_and_missing_paths(self):
        problems = self.problems(listing="a/1\na/3\n")
        self.assertEqual(len(problems), 1)
        self.assertIn("1 extra, 1 missing", problems[0])


class DocsPageTest(unittest.TestCase):
    TORRENTS = {ID: {"v1_hash": "ABCDEF" + "0" * 34}}

    def test_lists_the_torrent(self):
        text = f"| {ID} | {'abcdef' + '0' * 34} |"
        self.assertEqual(preflight.docs_page_problems(text, self.TORRENTS), [])

    def test_missing_id(self):
        self.assertEqual(len(preflight.docs_page_problems("nothing", self.TORRENTS)), 1)

    def test_missing_hash(self):
        self.assertEqual(len(preflight.docs_page_problems(ID, self.TORRENTS)), 1)


class StepsTest(unittest.TestCase):
    def test_every_script_is_a_scripted_step_and_back(self):
        names = [n for n in os.listdir(os.path.join(HERE, "..")) if re.match(r"^d\d\d-.*\.py$", n)]
        scripted = {purge.purge_step_name(s) + ".py" for s in purge.PURGE_STEPS if s not in purge.PURGE_MANUAL_STEPS}
        self.assertEqual(set(names), scripted)

    def test_numbers(self):
        self.assertEqual(
            [purge.purge_step_name(s) for s in purge.PURGE_STEPS],
            ["d00-preflight", "d01-find", "d02-review", "d03-dryrun", "d04-delete"],
        )

    def test_review_is_manual_and_dryrun_follows_find(self):
        self.assertEqual(purge.previous_purge_step("dryrun"), "find")
        self.assertEqual(purge.previous_purge_step("preflight"), None)


class PurgeWorkflowTest(Fixture):
    def workflow(self):
        env = {"PURGE_DIR": self.purge_dir, "SEED_DIR": self.seed, "TORRENTS_ENV": os.path.join(self.temp.name, "none")}
        with patch.dict(os.environ, env):
            return purge.PurgeWorkflow()

    def begin(self, workflow, step):
        with patch.object(purge, "lock"), patch.object(purge, "start_log"), patch.object(purge, "print_env_overrides"):
            workflow.begin(step)

    def test_a_step_needs_the_previous_one(self):
        with self.assertRaises(SystemExit):
            quietly(self.begin, self.workflow(), "find")

    def test_a_step_that_completed_wont_rerun(self):
        workflow = self.workflow()
        workflow.complete("preflight", {})
        with self.assertRaises(SystemExit):
            quietly(self.begin, workflow, "preflight")

    def test_the_next_step_runs_after_the_previous(self):
        workflow = self.workflow()
        workflow.complete("preflight", {})
        self.begin(workflow, "find")

    def test_dryrun_needs_find_not_the_manual_review(self):
        workflow = self.workflow()
        workflow.complete("preflight", {})
        workflow.complete("find", {})
        self.begin(workflow, "dryrun")

    def test_manual_steps_cant_begin(self):
        with self.assertRaises(ValueError):
            self.begin(self.workflow(), "review")

    def test_seed_dir_must_be_separate(self):
        env = {"PURGE_DIR": os.path.join(self.seed, "purge"), "SEED_DIR": self.seed}
        os.makedirs(env["PURGE_DIR"])
        with patch.dict(os.environ, env), self.assertRaises(SystemExit):
            quietly(purge.PurgeWorkflow)


class RunTest(Fixture):
    """d01-find, d03-dryrun and d04-delete end to end against a fake site, with rclone and the torrents patched."""

    def setUp(self):
        super().setUp()
        self.env = {
            "PURGE_DIR": self.purge_dir,
            "SEED_DIR": self.seed,
            "DATA_BUCKET": "bucket",
            "TORRENTS_ENV": os.path.join(self.temp.name, "none"),
        }
        self.deleted = []
        self.patches = [
            patch.dict(os.environ, self.env),
            patch.object(purge, "lock"),
            patch.object(purge, "print_env_overrides"),
            patch.object(purge.seed_dir, "quick_check", self._quick_check),
            patch.object(purge, "check_published", lambda *a: []),
            patch.object(purge, "lsjson", self._lsjson),
            patch.object(purge, "start_log", lambda *a, **k: None),
            patch.object(purge, "delete_files", self._delete_files),
            patch.object(purge, "dry_run", lambda remote, paths: None),
        ]
        for p in self.patches:
            p.start()
            self.addCleanup(p.stop)
        for module, name, replacement in (
            (dryrun, "dry_run", purge.dry_run),
            (delete, "dry_run", purge.dry_run),
            (delete, "delete_files", purge.delete_files),
            (delete, "lsjson", purge.lsjson),
        ):
            patcher = patch.object(module, name, replacement)
            patcher.start()
            self.addCleanup(patcher.stop)

    def _quick_check(self, seed, torrents_dir):
        return {ID: {"files": {}, "v1_hash": "0" * 40}}, {}, []

    def _lsjson(self, remote, recursive=False):
        for path, (size, mtime) in sorted(self.site.items()):
            yield path, size, mtime

    def _delete_files(self, remote, paths):
        self.deleted += paths
        for path in paths:
            del self.site[path]

    def run_step(self, module, answer=None):
        with patch.object(purge, "archived_data_files", lambda torrents: (self.archived, [])):
            with patch("builtins.input", return_value=answer):
                quietly(module.main)

    def fake_today(self, *modules):
        class FakeDatetime(datetime.datetime):
            @classmethod
            def now(cls, tz=None):
                return datetime.datetime(2026, 11, 14, 12, 0, tzinfo=tz)

        for module in modules:
            patcher = patch.object(module.datetime, "datetime", FakeDatetime)
            patcher.start()
            self.addCleanup(patcher.stop)

    def run_find(self):
        self.fake_today(find)
        workflow = purge.PurgeWorkflow()
        workflow.complete("preflight", {})
        self.run_step(find)
        return workflow

    def read(self, *parts):
        with open(os.path.join(self.purge_dir, *parts), encoding="utf-8") as f:
            return f.read()

    def test_find_lists_only_old_files_and_writes_the_report(self):
        workflow = self.run_find()
        self.assertEqual(self.read("find", "delete.txt").split(), sorted([OLD_FILE, OLD_FILE_2]))
        self.assertEqual(workflow.read_done("find")["summary"]["first_kept_year"], 2025)
        self.assertIn("this or the previous year: 2 files", self.read("find", "report.txt"))
        self.assertEqual(self.deleted, [])

    def test_find_fails_when_seed_dir_lacks_an_old_file(self):
        os.remove(os.path.join(self.seed, *OLD_FILE.split("/")))
        self.fake_today(find)
        purge.PurgeWorkflow().complete("preflight", {})
        with self.assertRaises(SystemExit):
            self.run_step(find)
        self.assertFalse(os.path.exists(os.path.join(self.purge_dir, "find")))

    def test_find_fails_with_nothing_to_purge(self):
        for path in (OLD_FILE, OLD_FILE_2):
            del self.site[path]
        self.fake_today(find)
        purge.PurgeWorkflow().complete("preflight", {})
        with self.assertRaises(SystemExit):
            self.run_step(find)

    def test_dryrun_then_delete(self):
        workflow = self.run_find()
        self.fake_today(dryrun, delete)
        self.run_step(dryrun)
        self.assertEqual(self.read("dryrun", "verified.txt").split(), sorted([OLD_FILE, OLD_FILE_2]))
        self.assertEqual(self.deleted, [])
        self.run_step(delete, answer="2")
        self.assertEqual(sorted(self.deleted), sorted([OLD_FILE, OLD_FILE_2]))
        self.assertEqual(sorted(self.site), sorted([LAST_YEAR_FILE, THIS_YEAR_FILE]))
        self.assertEqual(workflow.read_done("delete")["summary"]["deleted_files"], 2)
        self.assertEqual(len(self.read("deleted.jsonl").splitlines()), 2)

    def test_a_trimmed_review_list_is_all_that_goes(self):
        self.run_find()
        with open(os.path.join(self.purge_dir, "find", "delete.txt"), "w", encoding="utf-8") as f:
            f.write(OLD_FILE + "\n")
        self.fake_today(dryrun, delete)
        self.run_step(dryrun)
        self.run_step(delete, answer="1")
        self.assertEqual(self.deleted, [OLD_FILE])

    def test_a_recent_file_added_to_the_review_list_stops_the_dry_run(self):
        self.run_find()
        with open(os.path.join(self.purge_dir, "find", "delete.txt"), "a", encoding="utf-8") as f:
            f.write(LAST_YEAR_FILE + "\n")
        self.fake_today(dryrun)
        with self.assertRaises(SystemExit):
            self.run_step(dryrun)
        self.assertFalse(os.path.exists(os.path.join(self.purge_dir, "dryrun")))

    def test_the_new_year_stops_the_dry_run(self):
        self.run_find()

        class NewYear(datetime.datetime):
            @classmethod
            def now(cls, tz=None):
                return datetime.datetime(2027, 1, 1, tzinfo=tz)

        with patch.object(dryrun.datetime, "datetime", NewYear), self.assertRaises(SystemExit):
            self.run_step(dryrun)

    def test_a_file_changing_before_the_delete_stops_it(self):
        self.run_find()
        self.fake_today(dryrun, delete)
        self.run_step(dryrun)
        self.site[OLD_FILE] = (10, OLD + 100)
        with self.assertRaises(SystemExit):
            self.run_step(delete, answer="2")
        self.assertEqual(self.deleted, [])

    def test_an_edited_verified_list_stops_the_delete(self):
        self.run_find()
        self.fake_today(dryrun, delete)
        self.run_step(dryrun)
        with open(os.path.join(self.purge_dir, "dryrun", "verified.txt"), "a", encoding="utf-8") as f:
            f.write(THIS_YEAR_FILE + "\n")
        with self.assertRaises(SystemExit):
            self.run_step(delete, answer="3")
        self.assertEqual(self.deleted, [])

    def test_a_wrong_confirmation_deletes_nothing(self):
        self.run_find()
        self.fake_today(dryrun, delete)
        self.run_step(dryrun)
        with self.assertRaises(SystemExit):
            self.run_step(delete, answer="yes")
        self.assertEqual(self.deleted, [])

    def test_files_already_gone_are_skipped_on_a_rerun(self):
        self.run_find()
        self.fake_today(dryrun, delete)
        self.run_step(dryrun)
        del self.site[OLD_FILE]
        self.run_step(delete, answer="1")
        self.assertEqual(self.deleted, [OLD_FILE_2])


if __name__ == "__main__":
    unittest.main()
