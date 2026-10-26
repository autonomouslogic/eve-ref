"""
Tests for 03-load.py: comparing the staging folder with the selection (compare), the index snapshot (read_index_list,
check_index_coverage, write_index_tar/check_index_tar), and the checksum file (write_sha256sums, sha256_of).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
import io
import json
import os
import sys
import tarfile
import tempfile
import unittest
from contextlib import redirect_stdout
from unittest.mock import patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
SPEC = importlib.util.spec_from_file_location("load", os.path.join(HERE, "..", "03-load.py"))
load = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(load)

MTIME = 1_700_000_000
DATA = "wars/history/wars-2003.tar.bz2"
DATA2 = "wars/history/wars-2004.tar.bz2"


class CompareTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.content_dir = self.tmp.name
        for path in (DATA, DATA2):
            full = os.path.join(self.content_dir, path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "wb") as f:
                f.write(b"0123456789")
            os.utime(full, (MTIME, MTIME))

    def tearDown(self):
        self.tmp.cleanup()

    def compare(self, path, size=10, mtime=MTIME, root_names=frozenset()):
        files = [load.Selected(p, size, mtime) if p == path else load.Selected(p, 10, MTIME) for p in (DATA, DATA2)]
        return load.compare(self.content_dir, files, root_names)

    def test_all_as_selected(self):
        state = self.compare(DATA)
        self.assertEqual(state.matching, {DATA, DATA2})
        self.assertEqual(state.different, [])

    def test_data_file_with_other_mtime_is_different(self):
        state = self.compare(DATA, mtime=MTIME + 86400)
        self.assertEqual(state.matching, {DATA2})
        self.assertEqual(len(state.different), 1)

    def test_data_file_with_other_size_is_different(self):
        state = self.compare(DATA, size=11)
        self.assertEqual(len(state.different), 1)

    def test_missing_file_is_missing(self):
        os.remove(os.path.join(self.content_dir, DATA2))
        state = self.compare(DATA)
        self.assertEqual(state.missing, [DATA2])

    def test_root_file_skipped_entirely(self):
        root_name = "everef-eve-online-deep-archive-00-README.md"
        with open(os.path.join(self.content_dir, root_name), "w", encoding="utf-8") as f:
            f.write("x")
        state = self.compare(DATA, root_names={root_name})
        self.assertEqual(state.extra, [])

    def test_unselected_file_is_extra_without_root_names(self):
        with open(os.path.join(self.content_dir, "stray.txt"), "w", encoding="utf-8") as f:
            f.write("x")
        state = self.compare(DATA)
        self.assertEqual(state.extra, ["stray.txt"])


class ReflinkCopyTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.source = os.path.join(self.tmp.name, "source")
        self.dest = os.path.join(self.tmp.name, "dest")
        os.makedirs(self.dest)
        self.files = [load.Selected(DATA, 10, MTIME), load.Selected(DATA2, 10, MTIME)]
        for f in self.files:
            full = os.path.join(self.source, f.path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "wb") as out:
                out.write(b"0123456789")

    def tearDown(self):
        self.tmp.cleanup()

    def test_runs_cp_with_reflink_always_from_the_source_dir(self):
        with patch.object(load.subprocess, "run") as run:
            run.return_value.returncode = 0
            copied = load.reflink_copy(self.source, self.dest, self.files)
        self.assertEqual(copied, 2)
        run.assert_called_once()
        args, kwargs = run.call_args
        self.assertEqual(
            args[0], ["cp", "-p", "--reflink=always", "--parents", "-t", self.dest, "--", DATA, DATA2]
        )
        self.assertEqual(kwargs["cwd"], self.source)

    def test_batches(self):
        files = [load.Selected(f"f{i}", 1, MTIME) for i in range(load.REFLINK_BATCH + 1)]
        with patch.object(load.subprocess, "run") as run:
            run.return_value.returncode = 0
            copied = load.reflink_copy(self.source, self.dest, files)
        self.assertEqual(copied, len(files))
        self.assertEqual(run.call_count, 2)

    def test_prints_status_after_every_batch(self):
        files = [load.Selected(f"f{i}", 1, MTIME) for i in range(load.REFLINK_BATCH + 1)]
        out = io.StringIO()
        with patch.object(load.subprocess, "run") as run, redirect_stdout(out):
            run.return_value.returncode = 0
            load.reflink_copy(self.source, self.dest, files)
        lines = out.getvalue().splitlines()
        self.assertEqual(len(lines), 2)
        self.assertIn(f"{load.REFLINK_BATCH}/{load.REFLINK_BATCH + 1} files", lines[0])
        self.assertIn("elapsed", lines[0])
        self.assertIn("files/s", lines[0])
        self.assertIn("ETA", lines[0])
        self.assertIn(f"{load.REFLINK_BATCH + 1}/{load.REFLINK_BATCH + 1} files", lines[1])
        self.assertIn("ETA 0s", lines[1])

    def test_nothing_to_copy_runs_nothing(self):
        with patch.object(load.subprocess, "run") as run:
            self.assertEqual(load.reflink_copy(self.source, self.dest, []), 0)
        run.assert_not_called()

    def test_failure_returns_none(self):
        with patch.object(load.subprocess, "run") as run, patch.object(load, "warning") as warn:
            run.return_value.returncode = 1
            run.return_value.stderr = "cp: failed to clone: Invalid cross-device link\n"
            self.assertIsNone(load.reflink_copy(self.source, self.dest, self.files))
        warn.assert_called_once()


class FormatProgressTest(unittest.TestCase):
    def test_rate_and_eta(self):
        line = load.format_progress(500, 2000, 5 * load.MIB, 20 * load.MIB, 10)
        self.assertEqual(
            line, "500/2,000 files (5.0 MiB/20.0 MiB), elapsed 10s, 50 files/s, ETA 30s"
        )

    def test_no_elapsed_time_has_unknown_eta(self):
        self.assertIn("ETA unknown", load.format_progress(0, 10, 0, 10, 0))


class ReadIndexListTest(unittest.TestCase):
    def test_reads_records(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "indexes.jsonl")
            with open(path, "w", encoding="utf-8") as f:
                f.write(json.dumps({"path": "a/index.json", "size": 5, "last_modified": "2024-01-01T00:00:00Z"}) + "\n")
            files = load.read_index_list(path)
            self.assertEqual(len(files), 1)
            self.assertEqual(files[0].path, "a/index.json")
            self.assertEqual(files[0].size, 5)

    def test_missing_file_fails(self):
        with self.assertRaises(SystemExit):
            load.read_index_list("/nonexistent/indexes.jsonl")

    def test_blank_lines_skipped(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "indexes.jsonl")
            with open(path, "w", encoding="utf-8") as f:
                f.write("\n")
            self.assertEqual(load.read_index_list(path), [])


class CheckIndexCoverageTest(unittest.TestCase):
    def write_index(self, index_dir, folder, files_field):
        full = os.path.join(index_dir, *folder.split("/")) if folder else index_dir
        os.makedirs(full, exist_ok=True)
        with open(os.path.join(full, "index.json"), "w", encoding="utf-8") as f:
            json.dump({"path": folder, "files": files_field}, f)

    def test_covered_file_has_no_problem(self):
        with tempfile.TemporaryDirectory() as index_dir:
            self.write_index(
                index_dir, "a/b", [{"name": "c.csv", "size": 10, "last_modified": "2024-01-01T00:00:00Z"}]
            )
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, load.parse_iso("2024-01-01T00:00:00Z"))]}
            self.assertEqual(load.check_index_coverage(index_dir, by_folder), [])

    def test_covered_with_fractional_seconds(self):
        with tempfile.TemporaryDirectory() as index_dir:
            self.write_index(
                index_dir, "a/b", [{"name": "c.csv", "size": 10, "last_modified": "2024-01-01T00:00:00.555Z"}]
            )
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, load.parse_iso("2024-01-01T00:00:00Z"))]}
            self.assertEqual(load.check_index_coverage(index_dir, by_folder), [])

    def test_missing_index_json_is_a_problem(self):
        with tempfile.TemporaryDirectory() as index_dir:
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, 0)]}
            problems = load.check_index_coverage(index_dir, by_folder)
            self.assertEqual(len(problems), 1)
            self.assertIn("no index.json", problems[0])

    def test_file_not_listed_is_a_problem(self):
        with tempfile.TemporaryDirectory() as index_dir:
            self.write_index(index_dir, "a/b", [])
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, 0)]}
            problems = load.check_index_coverage(index_dir, by_folder)
            self.assertEqual(len(problems), 1)
            self.assertIn("not listed", problems[0])

    def test_size_mismatch_is_a_problem(self):
        with tempfile.TemporaryDirectory() as index_dir:
            self.write_index(
                index_dir, "a/b", [{"name": "c.csv", "size": 999, "last_modified": "2024-01-01T00:00:00Z"}]
            )
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, load.parse_iso("2024-01-01T00:00:00Z"))]}
            problems = load.check_index_coverage(index_dir, by_folder)
            self.assertEqual(len(problems), 1)
            self.assertIn("size", problems[0])

    def test_mtime_mismatch_is_a_problem(self):
        with tempfile.TemporaryDirectory() as index_dir:
            self.write_index(
                index_dir, "a/b", [{"name": "c.csv", "size": 10, "last_modified": "2024-01-02T00:00:00Z"}]
            )
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, load.parse_iso("2024-01-01T00:00:00Z"))]}
            problems = load.check_index_coverage(index_dir, by_folder)
            self.assertEqual(len(problems), 1)
            self.assertIn("last_modified", problems[0])

    def test_invalid_json_is_a_problem(self):
        with tempfile.TemporaryDirectory() as index_dir:
            os.makedirs(os.path.join(index_dir, "a", "b"), exist_ok=True)
            with open(os.path.join(index_dir, "a", "b", "index.json"), "w", encoding="utf-8") as f:
                f.write("not json")
            by_folder = {"a/b": [load.Selected("a/b/c.csv", 10, 0)]}
            problems = load.check_index_coverage(index_dir, by_folder)
            self.assertEqual(len(problems), 1)
            self.assertIn("invalid JSON", problems[0])


class IndexTarTest(unittest.TestCase):
    """write_index_tar then check_index_tar: the tarball holds exactly the downloaded index.json files, named by their
    site paths so that extracting it into the site's root puts them back in place, and the two round-trip."""

    def test_round_trip(self):
        with tempfile.TemporaryDirectory() as index_dir, tempfile.TemporaryDirectory() as out_dir:
            for folder, content in (("a", b"{}"), ("a/b", b'{"x": 1}')):
                full = os.path.join(index_dir, folder, "index.json")
                os.makedirs(os.path.dirname(full), exist_ok=True)
                with open(full, "wb") as f:
                    f.write(content)
                os.utime(full, (MTIME, MTIME))
            indexes = [
                load.Selected("a/index.json", 2, MTIME),
                load.Selected("a/b/index.json", 8, MTIME),
            ]
            tar_path = os.path.join(out_dir, "everef-eve-online-deep-archive-00-index.tar.xz")
            load.write_index_tar(tar_path, index_dir, indexes)
            load.check_index_tar(tar_path, index_dir, indexes)  # doesn't fail

            with tarfile.open(tar_path, "r:xz") as tar:
                names = sorted(m.name for m in tar.getmembers())
            self.assertEqual(names, ["a/b/index.json", "a/index.json"])

    def test_deterministic(self):
        with tempfile.TemporaryDirectory() as index_dir, tempfile.TemporaryDirectory() as out_dir:
            full = os.path.join(index_dir, "a", "index.json")
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "wb") as f:
                f.write(b"{}")
            os.utime(full, (MTIME, MTIME))
            indexes = [load.Selected("a/index.json", 2, MTIME)]
            tar1 = os.path.join(out_dir, "1.tar.xz")
            tar2 = os.path.join(out_dir, "2.tar.xz")
            load.write_index_tar(tar1, index_dir, indexes)
            load.write_index_tar(tar2, index_dir, indexes)
            with open(tar1, "rb") as f1, open(tar2, "rb") as f2:
                self.assertEqual(f1.read(), f2.read())

    def test_check_fails_on_missing_member(self):
        with tempfile.TemporaryDirectory() as index_dir, tempfile.TemporaryDirectory() as out_dir:
            full = os.path.join(index_dir, "a", "index.json")
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "wb") as f:
                f.write(b"{}")
            os.utime(full, (MTIME, MTIME))
            indexes = [load.Selected("a/index.json", 2, MTIME)]
            tar_path = os.path.join(out_dir, "x.tar.xz")
            load.write_index_tar(tar_path, index_dir, indexes)
            extra = load.Selected("b/index.json", 2, MTIME)
            with self.assertRaises(SystemExit):
                load.check_index_tar(tar_path, index_dir, [*indexes, extra])


class WriteSha256sumsTest(unittest.TestCase):
    def test_sorted_and_formatted(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "x.sha256")
            load.write_sha256sums(path, [{"path": "b.txt", "sha256": "bb"}, {"path": "a.txt", "sha256": "aa"}])
            with open(path, encoding="utf-8") as f:
                self.assertEqual(f.read(), "aa  a.txt\nbb  b.txt\n")


class Sha256OfTest(unittest.TestCase):
    def test_known_hash(self):
        import hashlib

        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "f")
            with open(path, "wb") as f:
                f.write(b"hello")
            self.assertEqual(load.sha256_of(path), hashlib.sha256(b"hello").hexdigest())


if __name__ == "__main__":
    unittest.main()
