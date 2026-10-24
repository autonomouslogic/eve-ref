"""
Tests for lib/qbittorrent.py: authenticating with the Web API key (Authorization: Bearer on every request).

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import os
import sys
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


if __name__ == "__main__":
    unittest.main()
