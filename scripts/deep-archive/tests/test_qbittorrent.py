"""
Tests for lib/qbittorrent.py: authenticating with the Web API key (Authorization: Bearer on every request), adding a
torrent, and qBittorrent's torrent ID.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import os
import sys
import tempfile
import unittest
from unittest.mock import patch

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
import qbittorrent  # noqa: E402


class ClientAuthTest(unittest.TestCase):
    def requests(self, api_key, url="http://localhost:8080"):
        with patch.object(qbittorrent.urllib.request, "urlopen") as urlopen:
            urlopen.return_value.__enter__.return_value.read.return_value = b"v5.2.0"
            client = qbittorrent.Client(url, api_key)
            client.version()
            client.recheck("abc")
        return [call.args[0] for call in urlopen.call_args_list]

    def test_api_key_sends_bearer_header_on_every_request(self):
        requests = self.requests("qbt_key")
        self.assertEqual(
            [r.full_url for r in requests],
            ["http://localhost:8080/api/v2/app/version", "http://localhost:8080/api/v2/torrents/recheck"],
        )
        self.assertEqual([r.get_header("Authorization") for r in requests], ["Bearer qbt_key", "Bearer qbt_key"])

    def test_no_api_key_sends_no_auth(self):
        requests = self.requests("")
        self.assertEqual([r.get_header("Authorization") for r in requests], [None, None])

    def test_default_port_left_out_of_host(self):
        # qBittorrent's host header validation rejects "Host: example.lan:80" with 401, and urllib sends the port
        # whenever the URL has one.
        for url, host in (
            ("http://example.lan:80", "example.lan"),
            ("https://example.lan:443", "example.lan"),
            ("http://example.lan:8080", "example.lan:8080"),
            ("https://example.lan:80", "example.lan:80"),
        ):
            with self.subTest(url=url):
                self.assertEqual([r.host for r in self.requests("qbt_key", url)], [host, host])


class AddTorrentTest(unittest.TestCase):
    def add(self, response):
        """Adds a torrent, qBittorrent answering with response. Returns the request's body."""
        with tempfile.NamedTemporaryFile(suffix=".torrent") as torrent:
            with patch.object(qbittorrent.urllib.request, "urlopen") as urlopen:
                urlopen.return_value.__enter__.return_value.read.return_value = response
                qbittorrent.Client("http://localhost:8080", "qbt_key").add_torrent_file(torrent.name, "/downloads")
        return urlopen.call_args.args[0].data

    def test_keeps_the_torrents_root_folder(self):
        # Whatever qBittorrent's default content layout, the files are looked for in /downloads/<torrent name>/.
        self.assertIn(b'name="contentLayout"\r\n\r\nOriginal\r\n', self.add(b"Ok."))

    def test_ok_text(self):
        # qBittorrent before 5.2.
        self.add(b"Ok.")

    def test_json_success(self):
        # qBittorrent 5.2+.
        self.add(b'{"added_torrent_ids":["64d67e474d13c31da81afecb310bac2082dd416e"],"failure_count":0,'
                 b'"pending_count":0,"success_count":1}')

    def test_json_failure(self):
        with self.assertRaises(qbittorrent.QbittorrentError):
            self.add(b'{"added_torrent_ids":[],"failure_count":1,"pending_count":0,"success_count":0}')

    def test_fails_text(self):
        with self.assertRaises(qbittorrent.QbittorrentError):
            self.add(b"Fails.")


class SetSuperSeedingTest(unittest.TestCase):
    def set_super_seeding(self, info_hash, enable):
        with patch.object(qbittorrent.urllib.request, "urlopen") as urlopen:
            urlopen.return_value.__enter__.return_value.read.return_value = b""
            qbittorrent.Client("http://localhost:8080", "qbt_key").set_super_seeding(info_hash, enable)
        return urlopen.call_args.args[0]

    def test_enabling_posts_hashes_and_value_true(self):
        request = self.set_super_seeding("abc", True)
        self.assertEqual(request.full_url, "http://localhost:8080/api/v2/torrents/setSuperSeeding")
        self.assertEqual(request.data, b"hashes=abc&value=true")

    def test_disabling_posts_value_false(self):
        request = self.set_super_seeding("abc", False)
        self.assertEqual(request.data, b"hashes=abc&value=false")


class TorrentIdTest(unittest.TestCase):
    V1 = "a" * 40
    V2 = "0123456789abcdef" * 4

    def test_hybrid_is_truncated_v2(self):
        # qBittorrent's torrent ID is libtorrent's info_hash_t::get_best(): the v2 hash, truncated to 20 bytes.
        # torrents/info?hashes= matches only that ID, so a hybrid torrent isn't found by its v1 hash.
        self.assertEqual(qbittorrent.torrent_id(self.V1, self.V2), self.V2[:40])

    def test_v1_only_is_v1(self):
        self.assertEqual(qbittorrent.torrent_id(self.V1, None), self.V1)


if __name__ == "__main__":
    unittest.main()
