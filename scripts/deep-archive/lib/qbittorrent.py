"""
qbittorrent: a tiny client for qBittorrent's Web API (https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API),
stdlib only (urllib + http.cookiejar), for 08-seed, 09-check-seed and 10-test-download.
"""

import http.cookiejar
import json
import urllib.error
import urllib.parse
import urllib.request


class QbittorrentError(Exception):
    pass


class Client:
    def __init__(self, url, username, password):
        self.url = url
        self._cookies = http.cookiejar.CookieJar()
        self._opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self._cookies))
        if username:
            self._login(username, password)

    def _login(self, username, password):
        body = urllib.parse.urlencode({"username": username, "password": password}).encode()
        text = self._request("POST", "/api/v2/auth/login", data=body)
        if text.strip() != "Ok.":
            raise QbittorrentError(f"Login failed: {text.strip()!r}")

    def _request(self, method, path, data=None, params=None, headers=None):
        url = self.url + path
        if params:
            url += "?" + urllib.parse.urlencode(params)
        request = urllib.request.Request(url, data=data, method=method, headers=headers or {})
        try:
            with self._opener.open(request, timeout=60) as response:
                return response.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as e:
            raise QbittorrentError(f"{method} {path} failed: HTTP {e.code} {e.read().decode(errors='replace')}")
        except urllib.error.URLError as e:
            raise QbittorrentError(f"{method} {path} failed: {e.reason}")

    def version(self):
        return self._request("GET", "/api/v2/app/version")

    def add_torrent_file(self, torrent_path, save_path, category=None):
        """Adds a .torrent by multipart upload, with autoTMM off so save_path is used (checks the files in place)."""
        boundary = "----evereef-deep-archive"
        with open(torrent_path, "rb") as f:
            torrent_bytes = f.read()
        fields = [("savepath", save_path), ("autoTMM", "false"), ("skip_checking", "false")]
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
        text = self._request("POST", "/api/v2/torrents/add", data=body, headers=headers)
        if text.strip() not in ("Ok.", ""):
            raise QbittorrentError(f"Add torrent failed: {text.strip()!r}")

    def add_torrent_magnet(self, magnet_uri, save_path, category=None):
        fields = {"urls": magnet_uri, "savepath": save_path, "autoTMM": "false"}
        if category:
            fields["category"] = category
        body = urllib.parse.urlencode(fields).encode()
        text = self._request("POST", "/api/v2/torrents/add", data=body)
        if text.strip() not in ("Ok.", ""):
            raise QbittorrentError(f"Add magnet failed: {text.strip()!r}")

    def info(self, info_hash):
        """The torrent's info dict (hashes, progress, state, …), or None if it isn't added."""
        text = self._request("GET", "/api/v2/torrents/info", params={"hashes": info_hash})
        torrents = json.loads(text)
        return torrents[0] if torrents else None

    def properties(self, info_hash):
        text = self._request("GET", "/api/v2/torrents/properties", params={"hash": info_hash})
        return json.loads(text)

    def trackers(self, info_hash):
        text = self._request("GET", "/api/v2/torrents/trackers", params={"hash": info_hash})
        return json.loads(text)

    def recheck(self, info_hash):
        self._request("POST", "/api/v2/torrents/recheck", data=urllib.parse.urlencode({"hashes": info_hash}).encode())

    def delete(self, info_hash, delete_files=False):
        body = urllib.parse.urlencode({"hashes": info_hash, "deleteFiles": "true" if delete_files else "false"})
        self._request("POST", "/api/v2/torrents/delete", data=body.encode())
