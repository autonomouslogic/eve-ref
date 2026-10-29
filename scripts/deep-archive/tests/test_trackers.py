"""
Tests for lib/trackers.py: the BEP 15 connect check and the BEP 3 announce check (check_tracker, check_trackers)
against fake trackers on localhost. No real network access.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import os
import socket
import struct
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
from trackers import GOOD_TRACKERS, check_tracker, check_trackers, load_good_trackers  # noqa: E402

TIMEOUT = 0.5  # short, so a non-responding test doesn't slow the suite down


class FakeTracker:
    """A UDP socket on localhost that answers (or doesn't) a single BEP 15 connect request, in a background thread."""

    def __init__(self, respond=True, wrong_action=False, wrong_transaction_id=False, short=False):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(("127.0.0.1", 0))
        self.sock.settimeout(TIMEOUT * 4)
        self.respond = respond
        self.wrong_action = wrong_action
        self.wrong_transaction_id = wrong_transaction_id
        self.short = short
        self.thread = threading.Thread(target=self._serve, daemon=True)
        self.thread.start()

    @property
    def url(self):
        return f"udp://127.0.0.1:{self.sock.getsockname()[1]}/announce"

    def _serve(self):
        try:
            packet, addr = self.sock.recvfrom(16)
        except OSError:
            return
        if not self.respond:
            return
        _magic, _action, transaction_id = struct.unpack(">QII", packet)
        action = 1 if self.wrong_action else 0
        reply_id = transaction_id + 1 if self.wrong_transaction_id else transaction_id
        response = struct.pack(">IIQ", action, reply_id, 0)
        self.sock.sendto(response[:8] if self.short else response, addr)

    def close(self):
        self.sock.close()
        self.thread.join(timeout=TIMEOUT * 4)


class FakeHttpTracker:
    """An HTTP server on localhost that answers announces with a fixed status and body, in a background thread."""

    def __init__(self, status=200, body=b"d8:intervali1800e5:peers0:e"):
        outer = self
        outer.requests = []

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                outer.requests.append(self.path)
                self.send_response(status)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, *args):
                pass

        self.server = HTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    @property
    def url(self):
        return f"http://127.0.0.1:{self.server.server_address[1]}/announce"

    def close(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=TIMEOUT * 4)


class CheckHttpTrackerTest(unittest.TestCase):
    def check(self, **kwargs):
        tracker = FakeHttpTracker(**kwargs)
        try:
            return check_tracker(tracker.url, timeout=TIMEOUT), tracker.requests
        finally:
            tracker.close()

    def test_a_working_tracker_answers(self):
        result, requests = self.check()
        self.assertEqual(result, (True, None))
        self.assertEqual(len(requests), 1)
        self.assertTrue(requests[0].startswith("/announce?info_hash="))

    def test_failure_reason_still_counts_as_alive(self):
        result, _ = self.check(body=b"d14:failure reason7:unknowne")
        self.assertEqual(result, (True, None))

    def test_http_error_fails(self):
        result, _ = self.check(status=503, body=b"")
        self.assertEqual(result, (False, "HTTP 503"))

    def test_non_bencoded_response_fails(self):
        result, _ = self.check(body=b"<html>hello</html>")
        self.assertEqual(result, (False, "response isn't a bencoded dictionary"))

    def test_nothing_listening_fails(self):
        tracker = FakeHttpTracker()
        url = tracker.url
        tracker.close()
        ok, reason = check_tracker(url, timeout=TIMEOUT)
        self.assertFalse(ok)
        self.assertTrue(reason)


class CheckTrackerTest(unittest.TestCase):
    def test_a_working_tracker_answers(self):
        tracker = FakeTracker()
        try:
            self.assertEqual(check_tracker(tracker.url, timeout=TIMEOUT), (True, None))
        finally:
            tracker.close()

    def test_no_response_times_out(self):
        tracker = FakeTracker(respond=False)
        try:
            ok, reason = check_tracker(tracker.url, timeout=TIMEOUT)
            self.assertFalse(ok)
            self.assertTrue(reason)
        finally:
            tracker.close()

    def test_wrong_action_fails(self):
        tracker = FakeTracker(wrong_action=True)
        try:
            self.assertEqual(check_tracker(tracker.url, timeout=TIMEOUT)[0], False)
        finally:
            tracker.close()

    def test_wrong_transaction_id_fails(self):
        tracker = FakeTracker(wrong_transaction_id=True)
        try:
            self.assertEqual(check_tracker(tracker.url, timeout=TIMEOUT)[0], False)
        finally:
            tracker.close()

    def test_short_response_fails(self):
        tracker = FakeTracker(short=True)
        try:
            self.assertEqual(check_tracker(tracker.url, timeout=TIMEOUT)[0], False)
        finally:
            tracker.close()

    def test_nothing_listening_fails(self):
        # A closed socket's port: nothing answers, and most systems reply ICMP port-unreachable (an OSError), rather
        # than a timeout.
        tracker = FakeTracker()
        url = tracker.url
        tracker.close()
        ok, reason = check_tracker(url, timeout=TIMEOUT)
        self.assertFalse(ok)
        self.assertTrue(reason)

    def test_other_scheme_is_unchecked(self):
        self.assertEqual(
            check_tracker("wss://example.org/announce"), (None, "not UDP, HTTP or HTTPS: not checked")
        )

    def test_no_port_fails(self):
        ok, reason = check_tracker("udp://example.org/announce")
        self.assertFalse(ok)
        self.assertTrue(reason)


class CheckTrackersTest(unittest.TestCase):
    def test_checks_each_in_order(self):
        good = FakeTracker()
        bad = FakeTracker(respond=False)
        try:
            results = check_trackers([good.url, bad.url], timeout=TIMEOUT)
            self.assertEqual([url for url, _, _ in results], [good.url, bad.url])
            self.assertEqual([ok for _, ok, _ in results], [True, False])
        finally:
            good.close()
            bad.close()


class GoodTrackersTest(unittest.TestCase):
    def test_schemes(self):
        for url in GOOD_TRACKERS:
            self.assertTrue(url.startswith(("udp://", "http://")), url)

    def test_has_udp_and_http(self):
        self.assertTrue(any(url.startswith("udp://") for url in GOOD_TRACKERS))
        self.assertTrue(any(url.startswith("http://") for url in GOOD_TRACKERS))

    def test_no_duplicates(self):
        self.assertEqual(len(GOOD_TRACKERS), len(set(GOOD_TRACKERS)))

    def test_not_empty(self):
        self.assertGreater(len(GOOD_TRACKERS), 0)


class LoadGoodTrackersTest(unittest.TestCase):
    def load(self, text):
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as f:
            f.write(text)
            path = f.name
        try:
            return load_good_trackers(path)
        finally:
            os.remove(path)

    def test_ignores_blank_lines_and_comments(self):
        trackers = self.load("# a comment\n\nudp://a.example.org:1337/announce\n\nudp://b.example.org:6969/announce\n")
        self.assertEqual(trackers, ["udp://a.example.org:1337/announce", "udp://b.example.org:6969/announce"])

    def test_strips_whitespace(self):
        self.assertEqual(self.load("  udp://a.example.org:1337/announce  \n"), ["udp://a.example.org:1337/announce"])

    def test_the_committed_file_loads(self):
        # GOOD_TRACKERS is loaded from lib/trackers.txt at import time: it must actually parse.
        self.assertEqual(GOOD_TRACKERS, load_good_trackers())


if __name__ == "__main__":
    unittest.main()
