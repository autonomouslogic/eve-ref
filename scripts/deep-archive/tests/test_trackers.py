"""
Tests for lib/trackers.py: how a tracker list is parsed (parse_tracker_list) and how the lists are combined, with the
errors make-torrent stops on (combine). No network.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import os
import sys
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
from trackers import MIN_TRACKERS, TrackersError, combine, parse_tracker_list  # noqa: E402


def tracker_list(*urls):
    """A list as ngosang publishes it: one URL per line, a blank line between them."""
    return "\n\n".join(urls) + "\n"


def udp(n):
    return f"udp://tracker{n}.example.org:1337/announce"


class ParseTrackerListTest(unittest.TestCase):
    def test_keeps_udp_and_http_in_order(self):
        text = tracker_list(udp(1), "http://a.example.org/announce", "https://b.example.org/announce")
        trackers, skipped = parse_tracker_list(text)
        self.assertEqual(trackers, [udp(1), "http://a.example.org/announce", "https://b.example.org/announce"])
        self.assertEqual(skipped, [])

    def test_skips_other_schemes(self):
        trackers, skipped = parse_tracker_list(tracker_list(udp(1), "wss://c.example.org/announce"))
        self.assertEqual(trackers, [udp(1)])
        self.assertEqual(skipped, ["wss://c.example.org/announce"])

    def test_ignores_blank_lines_comments_and_whitespace(self):
        trackers, _ = parse_tracker_list(f"# comment\r\n  {udp(1)}  \r\n\r\n{udp(2)}")
        self.assertEqual(trackers, [udp(1), udp(2)])

    def test_drops_duplicates(self):
        trackers, _ = parse_tracker_list(tracker_list(udp(1), udp(2), udp(1)))
        self.assertEqual(trackers, [udp(1), udp(2)])


class CombineTest(unittest.TestCase):
    def test_union_in_list_order(self):
        first = tracker_list(*[udp(n) for n in range(6)])
        second = tracker_list(*[udp(n) for n in range(3, 12)])
        trackers, counts = combine([("first", first), ("second", second)])
        self.assertEqual(trackers, [udp(n) for n in range(12)])
        self.assertEqual([(name, usable) for name, usable, _ in counts], [("first", 6), ("second", 9)])

    def test_fails_on_an_empty_list(self):
        with self.assertRaises(TrackersError):
            combine([("first", tracker_list(*[udp(n) for n in range(MIN_TRACKERS)])), ("second", "")])

    def test_fails_on_a_list_with_only_unusable_trackers(self):
        with self.assertRaises(TrackersError):
            combine([("first", tracker_list(*[udp(n) for n in range(MIN_TRACKERS)])), ("second", "wss://c/announce")])

    def test_fails_under_the_minimum(self):
        second = tracker_list(*[udp(n) for n in range(MIN_TRACKERS - 2)])
        with self.assertRaises(TrackersError):
            combine([("first", tracker_list(udp(1))), ("second", second)])

    def test_passes_at_the_minimum(self):
        trackers, _ = combine([("first", tracker_list(*[udp(n) for n in range(MIN_TRACKERS)])), ("second", udp(0))])
        self.assertEqual(len(trackers), MIN_TRACKERS)


if __name__ == "__main__":
    unittest.main()
