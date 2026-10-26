"""
Tests for 01-find.py's selection logic: how a file is dated (file_date, Obj), what is skipped (skip_reason), how
published torrents and index.json files are handled (classify_all, folder_indexes), how the cutoff is chosen
(choose_cutoff), and the merged SEED_DIR tree (existing_seed_paths).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import datetime
import importlib.util
import io
import os
import tempfile
import unittest
from unittest import mock

HERE = os.path.dirname(os.path.realpath(__file__))
SPEC = importlib.util.spec_from_file_location("find", os.path.join(HERE, "..", "01-find.py"))
find = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(find)

date = datetime.date


def ts(year, month, day, hour=12, minute=0):
    """A POSIX timestamp for a UTC date and time."""
    return datetime.datetime(year, month, day, hour, minute, tzinfo=datetime.timezone.utc).timestamp()


def obj(path, size=100, modified=(2024, 6, 1), hour=12):
    return find.Obj(path, size, ts(*modified, hour=hour))


def quietly(function, *args):
    """Calls function, swallowing what it prints."""
    with contextlib.redirect_stdout(io.StringIO()):
        return function(*args)


class FileDateTest(unittest.TestCase):
    MODIFIED = date(2024, 6, 1)

    def file_date(self, path):
        return find.file_date(path, self.MODIFIED)

    def test_no_date_in_name(self):
        self.assertEqual(self.file_date("structures/structures.json"), (self.MODIFIED, "modified"))

    def test_iso_date_earlier_than_modified(self):
        self.assertEqual(
            self.file_date("market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-00-00.v3.csv.bz2"),
            (date(2023, 1, 1), "name date"),
        )

    def test_iso_date_later_than_modified(self):
        self.assertEqual(self.file_date("x/report-2025-01-01.json"), (self.MODIFIED, "modified"))

    def test_earliest_of_several_iso_dates(self):
        self.assertEqual(self.file_date("x/range-2022-05-01-to-2021-03-04.json"), (date(2021, 3, 4), "name date"))

    def test_year_in_name(self):
        self.assertEqual(self.file_date("ccp/sde/sde-2019.zip"), (date(2019, 12, 31), "name year"))

    def test_year_later_than_modified(self):
        self.assertEqual(self.file_date("x/plan-2030.json"), (self.MODIFIED, "modified"))

    def test_iso_date_beats_its_own_year(self):
        # The year in "2020-03-01" also matches as a year, 2020-12-31; the full date is earlier.
        self.assertEqual(self.file_date("x/snapshot-2020-03-01.json"), (date(2020, 3, 1), "name date"))

    def test_tie_between_iso_date_and_year_goes_to_iso_date(self):
        self.assertEqual(self.file_date("x/snapshot-2020-12-31.json"), (date(2020, 12, 31), "name date"))

    def test_tie_with_modified_goes_to_modified(self):
        self.assertEqual(self.file_date("x/snapshot-2024-06-01.json"), (self.MODIFIED, "modified"))

    def test_invalid_iso_date_falls_back_to_its_year(self):
        self.assertEqual(self.file_date("x/snapshot-2023-13-45.json"), (date(2023, 12, 31), "name year"))

    def test_leap_day(self):
        self.assertEqual(self.file_date("x/snapshot-2024-02-29.json"), (date(2024, 2, 29), "name date"))

    def test_leap_day_in_a_common_year_falls_back_to_its_year(self):
        self.assertEqual(self.file_date("x/snapshot-2023-02-29.json"), (date(2023, 12, 31), "name year"))

    def test_years_before_eve_online_are_not_years(self):
        self.assertEqual(self.file_date("x/type-1999.json"), (self.MODIFIED, "modified"))
        self.assertEqual(self.file_date("x/type-2002.json"), (self.MODIFIED, "modified"))

    def test_first_year(self):
        self.assertEqual(self.file_date("x/type-2003.json"), (date(2003, 12, 31), "name year"))

    def test_iso_date_before_eve_online_is_ignored(self):
        self.assertEqual(self.file_date("x/snapshot-1999-01-01.json"), (self.MODIFIED, "modified"))

    def test_four_digit_id_dates_the_file_as_a_year(self):
        # Known limitation (01-find.py, "Dates"): a 4-digit ID can't be told from a year.
        self.assertEqual(self.file_date("x/type-2010.json"), (date(2010, 12, 31), "name year"))

    def test_longer_numbers_are_not_years(self):
        self.assertEqual(
            self.file_date("fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-21000.csv.gz"),
            (self.MODIFIED, "modified"),
        )
        self.assertEqual(self.file_date("x/id-123456789.json"), (self.MODIFIED, "modified"))

    def test_compact_dates_are_not_dates(self):
        self.assertEqual(self.file_date("x/snapshot-20230101.json"), (self.MODIFIED, "modified"))

    def test_iso_date_followed_by_a_digit_is_not_a_date(self):
        # "2023-01-012": no date; "2023" is still a year.
        self.assertEqual(self.file_date("x/snapshot-2023-01-012.json"), (date(2023, 12, 31), "name year"))

    def test_year_between_letters(self):
        self.assertEqual(self.file_date("x/abc2015def.json"), (date(2015, 12, 31), "name year"))

    def test_folders_are_ignored(self):
        self.assertEqual(
            self.file_date("market-history/2019/2019-01-01/market-history.csv.bz2"), (self.MODIFIED, "modified")
        )


class ObjTest(unittest.TestCase):
    def test_modified_is_the_utc_date(self):
        o = find.Obj("x/a.json", 1, ts(2023, 1, 1, hour=23, minute=59))
        self.assertEqual(o.modified, date(2023, 1, 1))
        self.assertEqual((o.date, o.dated_by), (date(2023, 1, 1), "modified"))

    def test_date_from_name(self):
        o = obj("x/a-2021-02-03.json", modified=(2023, 1, 1))
        self.assertEqual((o.date, o.dated_by), (date(2021, 2, 3), "name date"))


class SkipReasonTest(unittest.TestCase):
    EXCLUDES = find.TORRENT_EXCLUDE

    def skip(self, path):
        return find.skip_reason(path, self.EXCLUDES)

    def test_data_file(self):
        self.assertIsNone(self.skip("market-orders/history/2023/2023-01-01/market-orders-2023-01-01.v3.csv.bz2"))

    def test_index_json_is_not_skipped(self):
        self.assertIsNone(self.skip("market-orders/history/2023/2023-01-01/index.json"))

    def test_index_html(self):
        self.assertEqual(self.skip("market-orders/index.html"), ("ignored", "index.html files"))

    def test_deep_archive(self):
        self.assertEqual(self.skip("deep-archive/everef-eve-online-deep-archive-00.txt"), ("excluded", "deep-archive"))

    def test_latest_files(self):
        self.assertEqual(self.skip("market-orders/market-orders-latest.v3.csv.bz2"), ("ignored", "latest files"))

    def test_hidden_files_and_folders(self):
        self.assertEqual(self.skip("market-orders/.hidden"), ("ignored", "hidden files"))
        self.assertEqual(self.skip(".well-known/x/y"), ("ignored", "hidden files"))

    def test_excluded_dataset(self):
        self.assertEqual(self.skip("market-history/2019/a.csv.bz2"), ("excluded", "market-history"))
        self.assertEqual(self.skip("ccp/sde/sde-2019.zip"), ("excluded", "ccp"))

    def test_excluded_dataset_by_folder_not_prefix(self):
        self.assertIsNone(self.skip("ccp-other/a.json"))
        self.assertIsNone(self.skip("killmails-summary/2020/a.json"))

    def test_root_files(self):
        self.assertEqual(self.skip("robots.txt"), ("root", None))
        self.assertEqual(self.skip("index.json"), ("root", None))

    def test_no_includes_selects_everything(self):
        self.assertIsNone(find.skip_reason("fuzzwork/ordersets/2020/a.csv.gz", self.EXCLUDES, None))

    def test_not_included_by_top_folder(self):
        includes = ["market-orders", "fuzzwork/ordersets"]
        self.assertIsNone(find.skip_reason("market-orders/history/2020/a.csv.bz2", self.EXCLUDES, includes))
        self.assertIsNone(find.skip_reason("fuzzwork/ordersets/2020/a.csv.gz", self.EXCLUDES, includes))
        self.assertEqual(
            find.skip_reason("fuzzwork/other/2020/a.csv.gz", self.EXCLUDES, includes), ("not_included", "fuzzwork")
        )
        self.assertEqual(
            find.skip_reason("market-orders-x/2020/a.json", self.EXCLUDES, includes),
            ("not_included", "market-orders-x"),
        )

    def test_excludes_apply_within_includes(self):
        self.assertEqual(find.skip_reason("ccp/sde/sde-2019.zip", self.EXCLUDES, ["ccp"]), ("excluded", "ccp"))

    def test_root_files_with_includes(self):
        self.assertEqual(find.skip_reason("robots.txt", self.EXCLUDES, ["market-orders"]), ("root", None))


class TorrentIdTest(unittest.TestCase):
    def test_is_torrent_id(self):
        self.assertTrue(find.is_torrent_id("everef-eve-online-deep-archive-00"))
        self.assertTrue(find.is_torrent_id("everef-eve-online-deep-archive-99"))
        self.assertFalse(find.is_torrent_id("everef-eve-online-deep-archive-2023-04-11"))
        self.assertFalse(find.is_torrent_id("everef-eve-online-deep-archive-latest"))
        self.assertFalse(find.is_torrent_id("feed"))


class LoadPublishedListsTest(unittest.TestCase):
    """published maps path -> torrent ID, from each torrent's published .txt file list. No README is read: 01-find
    never needs an earlier torrent's cutoff back (it picks its own fresh from this run's candidates)."""

    def load(self, listing, texts):
        with mock.patch.object(find, "rclone", side_effect=lambda *args: texts[args[-1]]):
            return quietly(find.load_published_lists, "remote", listing)

    def test_reads_published_paths(self):
        torrent_id = "everef-eve-online-deep-archive-00"
        listing = [obj(f"deep-archive/{torrent_id}.txt")]
        texts = {f"remote/deep-archive/{torrent_id}.txt": "a/b.json\n"}
        published = self.load(listing, texts)
        self.assertEqual(published, {"a/b.json": torrent_id})

    def test_malformed_list_fails(self):
        torrent_id = "everef-eve-online-deep-archive-00"
        listing = [obj(f"deep-archive/{torrent_id}.txt")]
        texts = {f"remote/deep-archive/{torrent_id}.txt": "not sorted\na/b.json\n"}
        with self.assertRaises(SystemExit):
            self.load(listing, texts)

    def test_no_published_lists(self):
        self.assertEqual(self.load([], {}), {})


class YearsBeforeTest(unittest.TestCase):
    def test_years_before(self):
        self.assertEqual(find.years_before(date(2026, 10, 12), 2), date(2024, 10, 12))

    def test_leap_day(self):
        self.assertEqual(find.years_before(date(2024, 2, 29), 2), date(2022, 2, 28))


class ClassifyAllTest(unittest.TestCase):
    EXCLUDES = ["market-history"]

    def classify(self, listing, taken=None):
        return quietly(find.classify_all, listing, self.EXCLUDES, taken or {})

    def test_candidates_sorted_by_date_then_modification_time_then_path(self):
        listing = [
            obj("d/c-2022-01-01.json", modified=(2023, 1, 1)),
            obj("d/b.json", modified=(2021, 6, 1), hour=13),
            obj("d/a.json", modified=(2021, 6, 1), hour=13),
            obj("d/z.json", modified=(2021, 6, 1), hour=1),
        ]
        scan = self.classify(listing)
        self.assertEqual([o.path for o in scan.candidates], ["d/z.json", "d/a.json", "d/b.json", "d/c-2022-01-01.json"])

    def test_index_json_set_aside_by_folder(self):
        index = obj("d/index.json")
        scan = self.classify([obj("d/a.json"), index])
        self.assertEqual([o.path for o in scan.candidates], ["d/a.json"])
        self.assertIs(scan.indexes["d"], index)

    def test_skipped_files_counted(self):
        scan = self.classify(
            [
                obj("market-history/2019/a.csv.bz2", size=10),
                obj("d/index.html", size=5),
                obj("robots.txt"),
                obj("d/a.json"),
            ]
        )
        self.assertEqual([o.path for o in scan.candidates], ["d/a.json"])
        self.assertEqual(scan.excluded["market-history"], [1, 10])
        self.assertEqual(scan.ignored["index.html files"], [1, 5])
        self.assertEqual(scan.root_files, ["robots.txt"])

    def test_not_included_files_counted(self):
        listing = [obj("d/a.json", size=3), obj("e/f/a.json", size=4), obj("e/g/a.json", size=5)]
        scan = quietly(find.classify_all, listing, self.EXCLUDES, {}, ["d", "e/f"])
        self.assertEqual([o.path for o in scan.candidates], ["d/a.json", "e/f/a.json"])
        self.assertEqual(scan.not_included, {"e": [1, 5]})

    def test_empty_files_are_candidates_and_flagged(self):
        scan = self.classify([obj("d/a.json", size=0)])
        self.assertEqual([o.path for o in scan.candidates], ["d/a.json"])
        self.assertEqual(scan.empty, ["d/a.json"])

    def test_published_path_still_on_site_is_an_error(self):
        # No cutoff cleverness: a non-index path a published torrent already holds, still a candidate, is a bug.
        torrent_id = "everef-eve-online-deep-archive-00"
        scan = self.classify([obj("d/a.json", size=7)], {"d/a.json": torrent_id})
        self.assertEqual(scan.candidates, [])
        self.assertEqual(scan.already_archived, [f"d/a.json ({torrent_id})"])

    def test_published_index_json_is_not_an_error(self):
        # DataIndex rewrites index.json every run, so the same folder's index.json legitimately recurs across
        # several torrents' file lists as it's archived in parts: not a bug.
        torrent_id = "everef-eve-online-deep-archive-00"
        taken = {"d/index.json": torrent_id}
        scan = self.classify([obj("d/index.json"), obj("d/a.json")], taken)
        self.assertEqual(scan.already_archived, [])
        self.assertIn("d", scan.indexes)
        self.assertEqual([o.path for o in scan.candidates], ["d/a.json"])


class FolderIndexesTest(unittest.TestCase):
    def test_index_of_each_folder_once(self):
        indexes = {"d": obj("d/index.json"), "e": obj("e/index.json"), "f": obj("f/index.json")}
        files = [obj("d/a.json"), obj("d/b.json"), obj("e/c.json"), obj("g/d.json")]
        self.assertEqual([o.path for o in find.folder_indexes(files, indexes)], ["d/index.json", "e/index.json"])


class MinBytesReachedTest(unittest.TestCase):
    def reached(self, selected_bytes, cutoff_reason):
        with mock.patch.object(find, "MIN_BYTES", 1000):
            return find.min_bytes_reached(selected_bytes, cutoff_reason)

    def test_enough_data_is_always_reached(self):
        self.assertTrue(self.reached(1000, "age"))
        self.assertTrue(self.reached(1000, "bytes"))
        self.assertTrue(self.reached(1000, "torrent"))

    def test_not_enough_data_fails_on_age(self):
        self.assertFalse(self.reached(500, "age"))

    def test_the_torrent_size_limit_is_exempt(self):
        # A lot of small files can hit MAX_TORRENT_BYTES well under MIN_BYTES: that's the size limit working as
        # intended, not a sign there isn't enough data yet.
        self.assertTrue(self.reached(500, "torrent"))


class ChooseCutoffTest(unittest.TestCase):
    NEWEST_ALLOWED = date(2024, 10, 12)

    def choose(self, candidates, max_bytes=10_000, max_torrent_bytes=10_000_000):
        with mock.patch.object(find, "MAX_BYTES", max_bytes), mock.patch.object(
            find, "MAX_TORRENT_BYTES", max_torrent_bytes
        ):
            candidates = sorted(candidates, key=lambda o: (o.date, o.mtime, o.path))
            return quietly(find.choose_cutoff, candidates, self.NEWEST_ALLOWED)

    def test_everything_fits(self):
        cut = self.choose([obj("d/a.json", modified=(2022, 1, 1)), obj("d/b.json", modified=(2022, 1, 2))])
        self.assertEqual(cut.cutoff, self.NEWEST_ALLOWED)
        self.assertEqual(cut.reason, "age")
        self.assertIsNone(cut.over_date)

    def test_too_new_files_are_left_out(self):
        cut = self.choose(
            [obj("d/a.json", size=10, modified=(2022, 1, 1)), obj("d/b.json", size=20, modified=(2024, 10, 13))]
        )
        self.assertEqual(cut.cutoff, self.NEWEST_ALLOWED)
        self.assertEqual(cut.too_new, [1, 20])

    def test_newest_allowed_date_is_included(self):
        cut = self.choose([obj("d/a.json", size=10, modified=(2024, 10, 12))])
        self.assertEqual(cut.too_new, [0, 0])
        self.assertEqual(cut.reason, "age")

    def test_dated_by_name_not_modification(self):
        # Modified recently, but its name dates it in 2020: a candidate.
        cut = self.choose([obj("d/a-2020-01-01.json", size=10, modified=(2026, 1, 1))])
        self.assertEqual(cut.too_new, [0, 0])
        self.assertEqual(cut.reason, "age")

    def test_byte_limit_is_soft_the_date_reaching_it_is_included(self):
        candidates = [
            obj("d/a.json", size=400, modified=(2022, 1, 1)),
            obj("d/b.json", size=400, modified=(2022, 1, 5)),
            obj("d/c.json", size=400, modified=(2022, 1, 9)),
            obj("d/d.json", size=400, modified=(2022, 1, 12)),
        ]
        cut = self.choose(candidates, max_bytes=1000)
        self.assertEqual(cut.reason, "bytes")
        self.assertEqual(cut.over_date, date(2022, 1, 9))
        self.assertEqual(cut.cutoff, date(2022, 1, 9))
        self.assertEqual((cut.over_files, cut.over_bytes), (3, 1200))
        self.assertEqual(cut.over_day, (1, 400))

    def test_byte_limit_reached_exactly(self):
        candidates = [
            obj("d/a.json", size=500, modified=(2022, 1, 1)),
            obj("d/b.json", size=500, modified=(2022, 1, 2)),
            obj("d/c.json", size=500, modified=(2022, 1, 3)),
        ]
        cut = self.choose(candidates, max_bytes=1000)
        self.assertEqual(cut.reason, "bytes")
        self.assertEqual(cut.cutoff, date(2022, 1, 2))

    def test_byte_limit_takes_the_whole_date(self):
        candidates = [
            obj("d/a.json", size=400, modified=(2022, 1, 1)),
            obj("d/b.json", size=400, modified=(2022, 1, 2)),
            obj("d/c.json", size=400, modified=(2022, 1, 2)),
        ]
        cut = self.choose(candidates, max_bytes=1000)
        self.assertEqual(cut.cutoff, date(2022, 1, 2))
        self.assertEqual(cut.over_day, (2, 800))

    def test_first_date_alone_over_the_byte_limit_is_taken(self):
        cut = self.choose([obj("d/a.json", size=2000, modified=(2022, 1, 1))], max_bytes=1000)
        self.assertEqual(cut.cutoff, date(2022, 1, 1))
        self.assertEqual(cut.reason, "bytes")

    def test_first_date_alone_over_the_torrent_limit_has_no_cutoff(self):
        cut = self.choose([obj("d/a.json", size=1, modified=(2022, 1, 1))], max_torrent_bytes=1)
        self.assertIsNone(cut.cutoff)
        self.assertEqual(cut.reason, "torrent")
        self.assertEqual(cut.over_date, date(2022, 1, 1))

    def test_torrent_size_limit_is_hard_the_date_over_it_is_left_out(self):
        candidates = [obj(f"d/file-{i:03d}.json", size=1, modified=(2022, 1, 1 + i)) for i in range(20)]
        cut = self.choose(candidates, max_torrent_bytes=find.TorrentSize(find.PIECE_SIZE).bytes + 1000)
        self.assertEqual(cut.reason, "torrent")
        self.assertEqual(cut.cutoff, cut.over_date - datetime.timedelta(days=1))
        self.assertGreater(cut.over_date, date(2022, 1, 1))

    def test_torrent_limit_wins_over_byte_limit_on_the_same_date(self):
        candidates = [
            obj("d/a.json", size=400, modified=(2022, 1, 1)),
            obj("d/b.json", size=800, modified=(2022, 1, 5)),
        ]
        one_file = find.TorrentSize(find.PIECE_SIZE)
        one_file.add("d/a.json", 400)
        cut = self.choose(candidates, max_bytes=1000, max_torrent_bytes=one_file.bytes)
        self.assertEqual(cut.reason, "torrent")
        self.assertEqual(cut.cutoff, date(2022, 1, 4))

    def test_no_candidates(self):
        cut = self.choose([])
        self.assertIsNone(cut.cutoff)
        self.assertIsNone(cut.reason)

    def test_only_too_new_candidates(self):
        cut = self.choose([obj("d/a.json", size=10, modified=(2025, 1, 1))])
        self.assertIsNone(cut.cutoff)
        self.assertEqual(cut.too_new, [1, 10])


class ExistingSeedPathsTest(unittest.TestCase):
    """existing_seed_paths: every data path already under SEED_DIR, for the merge check against a new selection."""

    def write(self, root, rel):
        path = os.path.join(root, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write("x")

    def test_data_paths_found(self):
        with tempfile.TemporaryDirectory() as seed_dir:
            self.write(seed_dir, "market-orders/history/2022/a.csv.bz2")
            self.write(seed_dir, "fuzzwork/ordersets/b.csv.gz")
            self.assertEqual(
                sorted(find.existing_seed_paths(seed_dir)),
                ["fuzzwork/ordersets/b.csv.gz", "market-orders/history/2022/a.csv.bz2"],
            )

    def test_staging_and_torrents_folders_skipped(self):
        with tempfile.TemporaryDirectory() as seed_dir:
            self.write(seed_dir, ".staging/everef-eve-online-deep-archive-00/a.csv.bz2")
            self.write(seed_dir, ".torrents/everef-eve-online-deep-archive-00.torrent")
            self.write(seed_dir, "market-orders/a.csv.bz2")
            self.assertEqual(find.existing_seed_paths(seed_dir), ["market-orders/a.csv.bz2"])

    def test_root_files_skipped(self):
        with tempfile.TemporaryDirectory() as seed_dir:
            self.write(seed_dir, "everef-eve-online-deep-archive-00-README.md")
            self.write(seed_dir, "everef-eve-online-deep-archive-00.sha256")
            self.write(seed_dir, "market-orders/a.csv.bz2")
            self.assertEqual(find.existing_seed_paths(seed_dir), ["market-orders/a.csv.bz2"])

    def test_empty_seed_dir(self):
        with tempfile.TemporaryDirectory() as seed_dir:
            self.assertEqual(find.existing_seed_paths(seed_dir), [])


if __name__ == "__main__":
    unittest.main()
