"""
Tests for 08-check-seed.py: which rows of qBittorrent's torrents/trackers count as a working tracker
(working_trackers), and which of its torrents/files are still under a root folder (files_with_root_folder,
NOTES.md "Merged seed directory").

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import importlib.util
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


if __name__ == "__main__":
    unittest.main()
