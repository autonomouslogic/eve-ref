"""
Tests for 08-check-seed.py: which rows of qBittorrent's torrents/trackers count as a working tracker
(working_trackers), and which of its torrents/files are still under a root folder (files_with_root_folder,
NOTES.md "Merged seed directory"), and waiting out qBittorrent's checking with progress (wait_while_checking).

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
SPEC = importlib.util.spec_from_file_location("check_seed", os.path.join(HERE, "..", "08-check-seed.py"))
check_seed = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(check_seed)

# torrents/trackers lists DHT, PeX and LSD before the torrent's own trackers, with status 2 ("working") whenever
# they're enabled, which they always are for a public torrent.
STICKY = [
    {"url": "** [DHT] **", "tier": -1, "status": 2, "msg": ""},
    {"url": "** [PeX] **", "tier": -1, "status": 2, "msg": ""},
    {"url": "** [LSD] **", "tier": -1, "status": 2, "msg": ""},
]


def tracker(url, status, tier=0):
    return {"url": url, "tier": tier, "status": status, "msg": ""}


class WorkingTrackersTest(unittest.TestCase):
    def test_dht_pex_and_lsd_are_not_trackers(self):
        trackers = STICKY + [tracker("udp://tracker.example.com:1337/announce", 4)]
        self.assertEqual(check_seed.working_trackers(trackers), [])

    def test_a_working_udp_tracker(self):
        working = tracker("udp://tracker.example.com:1337/announce", 2)
        self.assertEqual(check_seed.working_trackers(STICKY + [working]), [working])

    def test_working_http_and_https_trackers(self):
        trackers = [tracker("http://a.example.com/announce", 2), tracker("https://b.example.com/announce", 2, 1)]
        self.assertEqual(check_seed.working_trackers(STICKY + trackers), trackers)

    def test_trackers_not_working_yet(self):
        # 1: not contacted yet, 3: updating, 4: not working.
        trackers = [tracker(f"udp://t{status}.example.com:1337/announce", status) for status in (1, 3, 4)]
        self.assertEqual(check_seed.working_trackers(STICKY + trackers), [])


class FilesWithRootFolderTest(unittest.TestCase):
    TORRENT_ID = "everef-eve-online-deep-archive-00"

    def test_merged_files_have_no_root_folder(self):
        files = [{"name": "market-orders/a.csv"}, {"name": f"{self.TORRENT_ID}-README.md"}]
        self.assertEqual(check_seed.files_with_root_folder(files, self.TORRENT_ID), [])

    def test_a_file_still_under_the_root_folder_is_flagged(self):
        files = [{"name": f"{self.TORRENT_ID}/market-orders/a.csv"}]
        self.assertEqual(
            check_seed.files_with_root_folder(files, self.TORRENT_ID), [f"{self.TORRENT_ID}/market-orders/a.csv"]
        )

    def test_backslash_names_are_normalised(self):
        files = [{"name": f"{self.TORRENT_ID}\\market-orders\\a.csv"}]
        self.assertEqual(len(check_seed.files_with_root_folder(files, self.TORRENT_ID)), 1)

    def test_a_different_torrents_prefix_is_not_a_false_positive(self):
        files = [{"name": "everef-eve-online-deep-archive-001-not-really-an-id/a.csv"}]
        self.assertEqual(check_seed.files_with_root_folder(files, self.TORRENT_ID), [])


class FakeClock:
    """A clock that only moves when the fake sleep is called."""

    def __init__(self):
        self.now = 0.0
        self.sleeps = []

    def monotonic(self):
        return self.now

    def sleep(self, seconds):
        self.sleeps.append(seconds)
        self.now += seconds


class FakeClient:
    """info() returns the next of the given infos, repeating the last."""

    def __init__(self, infos):
        self.infos = list(infos)
        self.calls = 0

    def info(self, _info_hash):
        info = self.infos[min(self.calls, len(self.infos) - 1)]
        self.calls += 1
        return info


GIB = 1024**3


def info(state, progress, size=300 * GIB):
    return {"state": state, "progress": progress, "size": size}


class WaitWhileCheckingTest(unittest.TestCase):
    def wait(self, infos, interval=10, report_interval=30):
        clock = FakeClock()
        client = FakeClient(infos)
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            result = check_seed.wait_while_checking(
                client, "hash", interval=interval, report_interval=report_interval, sleep=clock.sleep,
                monotonic=clock.monotonic,
            )
        return result, client, clock, out.getvalue()

    def test_not_checking_returns_at_once_without_waiting(self):
        done = info("uploading", 1.0)
        result, client, clock, out = self.wait([done])
        self.assertEqual(result, done)
        self.assertEqual(clock.sleeps, [])
        self.assertEqual(out, "")

    def test_waits_until_checking_finishes(self):
        done = info("uploading", 1.0)
        infos = [info("checkingUP", 0.1), info("checkingUP", 0.5), done]
        result, client, clock, _out = self.wait(infos)
        self.assertEqual(result, done)
        self.assertEqual(clock.sleeps, [10, 10])

    def test_prints_the_reported_state_and_progress(self):
        infos = [info("checkingUP", 0.0), info("checkingUP", 0.25), info("checkingUP", 0.5), info("uploading", 1.0)]
        _result, _client, _clock, out = self.wait(infos, interval=10, report_interval=10)
        self.assertEqual(
            out.splitlines(),
            ["Checking: 0.00% (checkingUP)", "Checking: 25.00% (checkingUP)", "Checking: 50.00% (checkingUP)"],
        )

    def test_reports_no_more_often_than_the_report_interval(self):
        infos = [info("checkingUP", p / 100) for p in range(1, 20)] + [info("uploading", 1.0)]
        _result, _client, clock, out = self.wait(infos, interval=10, report_interval=60)
        self.assertEqual(out.count("Checking:"), clock.now // 60 + 1)

    def test_queued_for_checking_waits_too(self):
        done = info("stalledUP", 1.0)
        result, _client, clock, _out = self.wait([info("queuedForChecking", 0.0), done])
        self.assertEqual(result, done)
        self.assertEqual(len(clock.sleeps), 1)

    def test_torrent_removed_while_checking_returns_none(self):
        result, _client, _clock, _out = self.wait([info("checkingUP", 0.3), None])
        self.assertIsNone(result)


if __name__ == "__main__":
    unittest.main()
