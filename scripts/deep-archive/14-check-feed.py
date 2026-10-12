#!/usr/bin/env python3
"""
check-feed: step 15 of the EVE Ref Deep Archive workflow. Checks what data.everef.net actually serves for the
feed and catalogue, over HTTPS -- never trusting 13-build-feed's own result.

DRAFT. Read-only, over HTTPS.

    14-check-feed.py

Needs 13-build-feed's completion file, and refuses to run twice (lib/workflow.py).

- feed.xml is served with Content-Type application/rss+xml, is valid XML, and has an <item> for this torrent with
  the right v1 info-hash as its <guid>.
- torrents.json is served with Content-Type application/json, is valid JSON, and lists this torrent with the
  right v1/v2 info-hashes (07-check-torrent).

On the first workflow, also add the feed to a qBittorrent RSS reader and check the item shows up (manual, README).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DATA_BASE_URL,
    DEEP_ARCHIVE_DIR,
    Workflow,
    error,
    fail,
    fmt_duration,
    ok,
    section,
)


STEP = "check-feed"


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    check_torrent = workflow.read_done("check-torrent")["summary"]
    v1_hash, v2_hash = check_torrent["v1_info_hash"], check_torrent["v2_info_hash"]
    base = DATA_BASE_URL.rstrip("/") + f"/{DEEP_ARCHIVE_DIR}"

    problems = []

    section("feed.xml")
    status, headers, body = fetch(f"{base}/feed.xml")
    problems += check_status(status, headers, body, "feed.xml", "application/rss+xml")
    if body:
        problems += check_feed_item(body, torrent_id, v1_hash)

    section("torrents.json")
    status, headers, body = fetch(f"{base}/torrents.json")
    problems += check_status(status, headers, body, "torrents.json", "application/json")
    if body:
        problems += check_catalogue_entry(body, torrent_id, v1_hash, v2_hash)

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the feed")

    workflow.complete(STEP, torrent_id, {})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check feed OK for {torrent_id}")


def fetch(url):
    print(f"GET {url}")
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.status, response.headers, response.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers, None
    except urllib.error.URLError as e:
        fail(f"{url}: {e.reason}")


def check_status(status, headers, body, name, expected_content_type):
    problems = []
    if status != 200:
        return [f"{name}: HTTP {status}"]
    content_type = (headers.get("Content-Type") or "").strip()
    print(f"  200 {content_type}, {len(body)} bytes")
    if content_type != expected_content_type:
        problems.append(f"{name}: Content-Type {content_type!r}, expected {expected_content_type!r}")
    return problems


def check_feed_item(body, torrent_id, v1_hash):
    try:
        root = ET.fromstring(body)
    except ET.ParseError as e:
        return [f"feed.xml: not valid XML: {e}"]
    guids = {item.findtext("guid") for item in root.iter("item")}
    if v1_hash not in guids:
        return [f"feed.xml: no <item> with guid {v1_hash} ({torrent_id})"]
    print(f"  <item> with guid {v1_hash} found")
    return []


def check_catalogue_entry(body, torrent_id, v1_hash, v2_hash):
    try:
        catalogue = json.loads(body)
    except ValueError as e:
        return [f"torrents.json: not valid JSON: {e}"]
    entries = {e["id"]: e for e in catalogue.get("torrents", [])}
    entry = entries.get(torrent_id)
    if entry is None:
        return [f"torrents.json: no entry for {torrent_id}"]
    problems = []
    if entry.get("v1_info_hash") != v1_hash:
        problems.append(f"torrents.json: v1_info_hash {entry.get('v1_info_hash')}, expected {v1_hash}")
    if entry.get("v2_info_hash") != v2_hash:
        problems.append(f"torrents.json: v2_info_hash {entry.get('v2_info_hash')}, expected {v2_hash}")
    if not problems:
        print(f"  entry for {torrent_id} matches")
    return problems


if __name__ == "__main__":
    main()
