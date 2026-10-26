"""
qbittorrent: a tiny client for qBittorrent's Web API (https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API),
stdlib only (urllib), for 00-preflight, 07-seed, 08-check-seed and 12-check-before-delete.

No delete or stop, on purpose, and never add either: nothing may remove a torrent from qBittorrent, with or without
its files, or stop it seeding. After 13-delete, $SEED_DIR holds EVE Ref's only copy of the archived data (NOTES.md
"Script principles").
"""

import json
import urllib.error
import urllib.parse
import urllib.request


class QbittorrentError(Exception):
    pass


def torrent_id(v1_hash, v2_hash):
    """
    qBittorrent's ID for a torrent, what the Web API's hash/hashes parameters take: libtorrent's info_hash_t::get_best(),
    the v2 info-hash truncated to 20 bytes if there is one, otherwise the v1 info-hash. torrents/info matches only
    this ID, so a hybrid torrent isn't found by its v1 info-hash.
    """
    return v2_hash[:40] if v2_hash else v1_hash


def check_added(text):
    """
    Raises QbittorrentError unless torrents/add's response says the torrent was added: "Ok." (or nothing) before
    qBittorrent 5.2, JSON with success_count and failure_count from 5.2 on.
    """
    text = text.strip()
    if text in ("Ok.", ""):
        return
    try:
        result = json.loads(text)
    except ValueError:
        raise QbittorrentError(f"Add torrent failed: {text!r}")
    if not isinstance(result, dict) or result.get("failure_count") != 0 or result.get("success_count") != 1:
        raise QbittorrentError(f"Add torrent failed: {text!r}")


def without_default_port(url):
    """
    The URL without an explicit default port (:80 for http, :443 for https). urllib sends the port in the Host header
    whenever the URL has one, and qBittorrent's host header validation rejects "example.lan:80" with 401.
    """
    parts = urllib.parse.urlsplit(url)
    if (parts.scheme, parts.port) in (("http", 80), ("https", 443)):
        parts = parts._replace(netloc=parts.netloc.rsplit(":", 1)[0])
    return urllib.parse.urlunsplit(parts)


class Client:
    def __init__(self, url, api_key):
        """
        Authenticates with the Web API key (qBittorrent 5.2+), sent as an Authorization: Bearer header on every
        request. No key sends no auth, for a Web API that bypasses it.
        """
        self.url = without_default_port(url)
        self._auth_headers = {"Authorization": f"Bearer {api_key}"} if api_key else {}

    def _request(self, method, path, data=None, params=None, headers=None):
        url = self.url + path
        if params:
            url += "?" + urllib.parse.urlencode(params)
        headers = {**self._auth_headers, **(headers or {})}
        request = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as e:
            raise QbittorrentError(f"{method} {path} failed: HTTP {e.code} {e.read().decode(errors='replace')}")
        except urllib.error.URLError as e:
            raise QbittorrentError(f"{method} {path} failed: {e.reason}")

    def version(self):
        return self._request("GET", "/api/v2/app/version")

    def build_info(self):
        """Versions qBittorrent was built with: qt, libtorrent, boost, openssl, bitness."""
        return json.loads(self._request("GET", "/api/v2/app/buildInfo"))

    def add_torrent_file(self, torrent_path, save_path, category=None):
        """
        Adds a .torrent by multipart upload, with autoTMM off so save_path is used (checks the files in place), and
        content layout Original, whatever qBittorrent's default: the files go under save_path/<torrent name>/.
        """
        boundary = "----everef-deep-archive"
        with open(torrent_path, "rb") as f:
            torrent_bytes = f.read()
        fields = [
            ("savepath", save_path),
            ("autoTMM", "false"),
            ("skip_checking", "false"),
            ("contentLayout", "Original"),
        ]
        if category:
            fields.append(("category", category))
        parts = []
        for name, value in fields:
            parts.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n{value}\r\n")
        filename = torrent_path.rsplit("/", 1)[-1]
        parts.append(
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"torrents\"; filename=\"{filename}\"\r\n"
            "Content-Type: application/x-bittorrent\r\n\r\n"
        )
        body = "".join(parts).encode("utf-8") + torrent_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")
        headers = {"Content-Type": f"multipart/form-data; boundary={boundary}"}
        check_added(self._request("POST", "/api/v2/torrents/add", data=body, headers=headers))

    def info(self, info_hash):
        """The torrent's info dict (hashes, progress, state, …), or None if it isn't added."""
        text = self._request("GET", "/api/v2/torrents/info", params={"hashes": info_hash})
        torrents = json.loads(text)
        return torrents[0] if torrents else None

    def trackers(self, info_hash):
        text = self._request("GET", "/api/v2/torrents/trackers", params={"hash": info_hash})
        return json.loads(text)

    def recheck(self, info_hash):
        self._request("POST", "/api/v2/torrents/recheck", data=urllib.parse.urlencode({"hashes": info_hash}).encode())

    def set_super_seeding(self, info_hash, enable):
        """
        Turns super seeding on or off. Only takes effect once the torrent is a seed (libtorrent: "If the torrent is
        not a seed, this flag has no effect."), so it's safe to call right after adding a torrent that still has to
        be (re)checked.
        """
        self._request(
            "POST",
            "/api/v2/torrents/setSuperSeeding",
            data=urllib.parse.urlencode({"hashes": info_hash, "value": "true" if enable else "false"}).encode(),
        )
