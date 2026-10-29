"""
Tests for 13-delete.py, the permanent step: re-checking the delete set against a fresh listing (recheck), reading
rclone's dry run (parse_dry_run_log), finding stale index files (stale_index_files), and whole runs against a fake
bucket: a dry run, a delete, a dry run that doesn't match, a file that changes while the operator confirms, and a rerun
after a delete that failed part way.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import importlib.util
import io
import json
import os
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
import workflow  # noqa: E402
from workflow import iso  # noqa: E402

SPEC = importlib.util.spec_from_file_location("delete", os.path.join(HERE, "..", "13-delete.py"))
delete = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(delete)

ID = "everef-eve-online-deep-archive-01"
MTIME = 1_700_000_000
DAY = "market-orders/history/2023/2023-01-01"
A = f"{DAY}/market-orders-2023-01-01_00-00-00.v3.csv.bz2"
B = f"{DAY}/market-orders-2023-01-01_12-00-00.v3.csv.bz2"
LATER = "market-orders/history/2023/2023-01-02/market-orders-2023-01-02_00-00-00.v3.csv.bz2"


def planned(path, size=10, mtime=MTIME):
    return {"path": path, "size": size, "last_modified": iso(mtime)}


class RecheckTest(unittest.TestCase):
    def test_still_matching(self):
        to_delete, gone, skipped = delete.recheck([planned(A)], {A: (10, MTIME + 0.5)})
        self.assertEqual((to_delete, gone, skipped), ([planned(A)], [], []))

    def test_other_size_stays(self):
        to_delete, gone, skipped = delete.recheck([planned(A)], {A: (11, MTIME)})
        self.assertEqual((to_delete, gone, [s["path"] for s in skipped]), ([], [], [A]))

    def test_other_modification_time_stays(self):
        to_delete, gone, skipped = delete.recheck([planned(A)], {A: (10, MTIME + 1)})
        self.assertEqual((to_delete, gone, [s["path"] for s in skipped]), ([], [], [A]))

    def test_already_gone(self):
        self.assertEqual(delete.recheck([planned(A)], {}), ([], [planned(A)], []))


class ParseDryRunLogTest(unittest.TestCase):
    def test_rclone_1_60(self):
        # logrus: NOTICE is logged as "warning".
        line = json.dumps(
            {
                "level": "warning",
                "msg": "Skipped delete as --dry-run is set (size 10)",
                "object": A,
                "objectType": "*b2.Object",
                "size": 10,
                "skipped": "delete",
                "source": "operations/operations.go:2404",
                "time": "2026-10-26T12:00:00.000000+00:00",
            }
        )
        self.assertEqual(delete.parse_dry_run_log(line + "\n"), ({A}, []))

    def test_rclone_1_68_and_later(self):
        # slog: the same fields, with level "notice".
        line = json.dumps(
            {
                "time": "2026-10-26T12:00:00.000000+00:00",
                "level": "notice",
                "msg": "Skipped delete as --dry-run is set (size 10)",
                "skipped": "delete",
                "size": 10,
                "object": A,
                "objectType": "*b2.Object",
                "source": "operations/operations.go:2724",
            }
        )
        self.assertEqual(delete.parse_dry_run_log(line + "\n"), ({A}, []))

    def test_other_messages(self):
        text = json.dumps({"level": "error", "msg": "Failed to delete: boom"}) + "\nnot json\n\n"
        self.assertEqual(delete.parse_dry_run_log(text), (set(), ["Failed to delete: boom", "not json"]))

    def test_other_skips_are_not_deletes(self):
        line = json.dumps({"level": "notice", "msg": "Skipped copy", "object": A, "skipped": "copy"})
        self.assertEqual(delete.parse_dry_run_log(line), (set(), ["Skipped copy"]))


class StaleIndexFilesTest(unittest.TestCase):
    def stale(self, paths, deleted):
        return [f["path"] for f in delete.stale_index_files([(p, 1, MTIME) for p in paths], deleted)]

    def test_emptied_folder(self):
        paths = [f"{DAY}/index.html", f"{DAY}/index.json"]
        self.assertEqual(self.stale(paths, [A]), paths)

    def test_emptied_parents_too_deepest_first(self):
        paths = [f"{DAY}/index.json", "market-orders/history/2023/index.json", "market-orders/history/index.json"]
        self.assertEqual(self.stale(paths, [A]), paths)

    def test_folder_with_files_left(self):
        self.assertEqual(self.stale([f"{DAY}/index.json", B], [A]), [])

    def test_a_file_deeper_down_keeps_a_parent(self):
        paths = [f"{DAY}/index.json", "market-orders/history/2023/index.json", LATER]
        self.assertEqual(self.stale(paths, [A]), [f"{DAY}/index.json"])

    def test_never_the_site_root(self):
        self.assertEqual(self.stale(["index.json", "a/index.json"], ["a/data.csv"]), ["a/index.json"])

    def test_never_deep_archive(self):
        self.assertEqual(self.stale(["deep-archive/old/index.json"], ["deep-archive/old/data.csv"]), [])

    def test_never_a_folder_the_delete_never_touched(self):
        self.assertEqual(self.stale(["other/index.json"], [A]), [])


class FakeBucket:
    """
    The bucket as 13-delete sees it: lsjson lists it, subprocess.run is rclone delete --dry-run, rclone_run is rclone
    delete itself.
    """

    def __init__(self, files, dry_run_extra=(), fail_after=None):
        self.files = dict(files)  # path -> (size, mtime)
        self.dry_run_extra = list(dry_run_extra)  # paths a wrong dry run also reports
        self.fail_after = fail_after  # deletes before rclone fails, if it does
        self.deletes = []  # args of every real delete

    def lsjson(self, remote, recursive=False):
        for path, (size, mtime) in sorted(self.files.items()):
            yield path, size, mtime

    @staticmethod
    def listed(args):
        with open(args[args.index("--files-from-raw") + 1], encoding="utf-8") as f:
            return f.read().splitlines()

    def run(self, args, **kwargs):
        if args[:2] != ["rclone", "delete"] or "--dry-run" not in args:
            raise ValueError(f"unexpected command: {args}")
        lines = [
            json.dumps({"level": "notice", "msg": "Skipped delete", "object": p, "skipped": "delete"})
            for p in self.listed(args) + self.dry_run_extra
            if p in self.files
        ]
        return subprocess.CompletedProcess(args, 0, "", "".join(line + "\n" for line in lines))

    def rclone_run(self, *args, check=True):
        self.deletes.append(args)
        for count, path in enumerate(self.listed(args)):
            if count == self.fail_after:
                raise SystemExit(1)
            self.files.pop(path, None)


class RunTest(unittest.TestCase):
    """Whole runs of 13-delete.py against a FakeBucket."""

    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.scratch = os.path.join(tmp.name, "scratch")
        seed = os.path.join(tmp.name, "seed")
        os.makedirs(self.scratch)
        os.makedirs(seed)
        for step in ("01-find", "12-check-before-delete"):
            with open(os.path.join(self.scratch, f"{step}.done"), "w", encoding="utf-8") as f:
                json.dump({"step": step, "torrent_id": ID, "completed_at": "2026-10-26T00:00:00Z", "summary": {}}, f)
        with open(os.path.join(self.scratch, "delete-set.json"), "w", encoding="utf-8") as f:
            json.dump({"torrent_id": ID, "files": [planned(A), planned(B)], "failed": []}, f)
        self.env = {
            "SCRATCH_DIR": self.scratch,
            "SEED_DIR": seed,
            "DATA_BUCKET": "bucket",
            "TORRENTS_ENV": os.path.join(tmp.name, "no-torrents.env"),
        }

    def bucket(self, **kwargs):
        files = {path: (10, MTIME) for path in (A, B, LATER)}
        files.update({f"{DAY}/index.html": (1, MTIME), f"{DAY}/index.json": (1, MTIME)})
        files["market-orders/history/2023/index.json"] = (1, MTIME)
        return FakeBucket(files, **kwargs)

    def run_delete(self, bucket, *args, typed=ID):
        """
        Runs main() with these arguments, typing typed at the prompt. Returns the exit code; the output is in
        self.output.
        """
        self.output = io.StringIO()
        with (
            patch.dict(os.environ, self.env),
            patch.object(sys, "argv", ["13-delete.py", *args]),
            patch.multiple(workflow, lock=lambda scratch_dir: None, start_log=lambda *a: None),
            patch.multiple(delete, lsjson=bucket.lsjson, rclone_run=bucket.rclone_run),
            patch.object(delete.subprocess, "run", bucket.run),
            patch("builtins.input", side_effect=typed if callable(typed) else lambda prompt: typed),
            contextlib.redirect_stdout(self.output),
            contextlib.redirect_stderr(self.output),
        ):
            try:
                delete.main()
            except SystemExit as e:
                return e.code
        return 0

    def deleted_log(self):
        with open(os.path.join(self.scratch, "deleted.jsonl"), encoding="utf-8") as f:
            return [(entry["path"], entry["kind"]) for entry in map(json.loads, f)]

    def completed(self):
        return os.path.isfile(os.path.join(self.scratch, "13-delete.done"))

    def test_dry_run_deletes_nothing(self):
        bucket = self.bucket()
        self.assertEqual(self.run_delete(bucket), 0)
        self.assertEqual(bucket.deletes, [])
        self.assertEqual(len(bucket.files), 6)
        self.assertFalse(self.completed())

    def test_execute(self):
        bucket = self.bucket()
        self.assertEqual(self.run_delete(bucket, "--execute"), 0)
        self.assertEqual([d[-2:] for d in bucket.deletes], [("--max-delete", "2"), ("--max-delete", "2")])
        self.assertEqual(sorted(bucket.files), [LATER, "market-orders/history/2023/index.json"])
        self.assertEqual(
            self.deleted_log(),
            [(A, "data"), (B, "data"), (f"{DAY}/index.html", "index"), (f"{DAY}/index.json", "index")],
        )
        self.assertTrue(self.completed())

    def test_dry_run_that_does_not_match_deletes_nothing(self):
        bucket = self.bucket(dry_run_extra=[LATER])
        self.assertEqual(self.run_delete(bucket, "--execute"), 1)
        self.assertIn(f"in rclone's dry run, but not listed: 1, e.g.\n🔴   {LATER}", self.output.getvalue())
        self.assertEqual(bucket.deletes, [])
        self.assertFalse(self.completed())

    def test_wrong_confirmation_deletes_nothing(self):
        bucket = self.bucket()
        self.assertEqual(self.run_delete(bucket, "--execute", typed="everef-eve-online-deep-archive-00"), 1)
        self.assertIn("Confirmation didn't match", self.output.getvalue())
        self.assertEqual(bucket.deletes, [])

    def test_file_changed_while_confirming_stays(self):
        bucket = self.bucket()

        def confirm(prompt):
            bucket.files[B] = (10, MTIME + 60)  # re-uploaded while the operator was away
            return ID

        self.assertEqual(self.run_delete(bucket, "--execute", typed=confirm), 0)
        self.assertIn(B, bucket.files)
        self.assertNotIn(A, bucket.files)
        self.assertEqual(self.deleted_log(), [(A, "data")])

    def test_rerun_after_a_partial_delete(self):
        bucket = self.bucket(fail_after=1)
        self.assertEqual(self.run_delete(bucket, "--execute"), 1)
        self.assertNotIn(A, bucket.files)
        self.assertIn(B, bucket.files)
        self.assertFalse(self.completed())

        bucket.fail_after = None
        self.assertEqual(self.run_delete(bucket, "--execute"), 0)
        self.assertEqual(sorted(bucket.files), [LATER, "market-orders/history/2023/index.json"])
        self.assertEqual(
            self.deleted_log(),
            [(A, "data"), (B, "data"), (f"{DAY}/index.html", "index"), (f"{DAY}/index.json", "index")],
        )
        self.assertTrue(self.completed())


if __name__ == "__main__":
    unittest.main()
