#!/usr/bin/env python3
"""
build-feed: step 14 of the EVE Ref Deep Archive workflow. Rebuilds feed.xml and torrents.json in full, from every
published .torrent in the bucket's deep-archive/ -- never from local state, so the bucket stays the single source
of truth (PLAN 2.5).

DRAFT. Shows the diff and asks first (lib/workflow.py, "Show, then ask"). Writes to the data bucket.

    13-build-feed.py

Needs 12-check-upload's completion file, and refuses to run twice (lib/workflow.py).

1. Lists deep-archive/*.torrent, downloads each with rclone, and reads it with libtorrent: name (the ID),
   creation date, comment (years/datasets, PLAN 2.4), v1/v2 info-hashes, data file count and bytes (the two root
   files excluded).
2. Builds torrents.json (the catalogue) and feed.xml (RSS 2.0, PLAN 2.7), magnet links with trackers.txt's core
   trackers, newest first.
3. Diffs both against what's currently published, shows it, and asks before uploading.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, DATA_BUCKET, RCLONE_CONFIG_EVEREF_ACCOUNT, RCLONE_CONFIG_EVEREF_KEY

Needs rclone and libtorrent 2.x.
"""

import datetime
import difflib
import json
import os
import re
import shutil
import sys
import time
import xml.sax.saxutils as xml_escape

import libtorrent as lt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DATA_BASE_URL,
    DEEP_ARCHIVE_DIR,
    FEED_CACHE_MAX_AGE_SECONDS,
    TORRENT_ID_RE,
    Workflow,
    data_remote,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    join,
    ok,
    rclone,
    section,
)

STEP = "build-feed"
TRACKERS_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")
COMMENT_RE = re.compile(r"^Data from (.+)\. Datasets: (.+)\.$", re.MULTILINE)


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    remote = data_remote()
    deep_archive = join(remote, DEEP_ARCHIVE_DIR)

    section("Published torrents")
    names = list_torrents(deep_archive)
    if torrent_id not in [n[: -len(".torrent")] for n in names]:
        fail(f"{torrent_id}.torrent isn't published yet in {deep_archive}/: run 11-upload-torrent first")
    print(f"{len(names)} published .torrent files")

    section("Reading torrents")
    tmp_dir = os.path.join(workflow.scratch_dir, "build-feed")
    if os.path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)
    os.makedirs(tmp_dir)
    try:
        entries = [read_torrent(deep_archive, tmp_dir, name) for name in names]
    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)
    entries.sort(key=lambda e: e["id"])
    print(f"Read {len(entries)} torrents")

    trackers = load_trackers(TRACKERS_FILE)
    torrents_json = build_catalogue(entries)
    feed_xml = build_feed(entries, trackers)

    section("Diff: torrents.json")
    old_json = fetch_existing(join(deep_archive, "torrents.json"))
    print_diff(old_json, torrents_json, "torrents.json")

    section("Diff: feed.xml")
    old_xml = fetch_existing(join(deep_archive, "feed.xml"))
    print_diff(old_xml, feed_xml, "feed.xml")

    typed = input(f"Type {torrent_id} to publish the feed and catalogue: ")
    if typed.strip() != torrent_id:
        fail("Confirmation didn't match, aborting")

    section("Uploading")
    upload(workflow.scratch_dir, deep_archive, "torrents.json", torrents_json, "application/json")
    upload(workflow.scratch_dir, deep_archive, "feed.xml", feed_xml, "application/rss+xml")

    workflow.complete(STEP, torrent_id, {"torrents": len(entries)})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Build feed OK for {torrent_id}")


def list_torrents(deep_archive):
    out = rclone("lsjson", "--files-only", "--no-mimetype", deep_archive)
    names = sorted(e["Path"] for e in json.loads(out) if e["Path"].endswith(".torrent"))
    if not names:
        fail(f"No .torrent files in {deep_archive}")
    return names


def read_torrent(deep_archive, tmp_dir, name):
    local = os.path.join(tmp_dir, name)
    rclone("copyto", join(deep_archive, name), local)
    size = os.path.getsize(local)
    ti = lt.torrent_info(local)
    torrent_id = ti.name()
    if not TORRENT_ID_RE.match(torrent_id):
        fail(f"{name}: torrent name {torrent_id!r} isn't a valid torrent ID")
    hashes = ti.info_hashes()
    storage = ti.files()
    root_names = {f"{torrent_id}.sha256", f"{torrent_id}-README.txt"}
    prefix = torrent_id + "/"
    files = 0
    data_bytes = 0
    for i in range(storage.num_files()):
        if storage.file_flags(i) & lt.file_storage.flag_pad_file:
            continue
        path = storage.file_path(i).replace(os.sep, "/")
        rel = path[len(prefix) :] if path.startswith(prefix) else path
        if rel in root_names:
            continue
        files += 1
        data_bytes += storage.file_size(i)
    match = COMMENT_RE.search(ti.comment() or "")
    years, datasets = (match.group(1), match.group(2)) if match else ("unknown years", "none")
    return {
        "id": torrent_id,
        "torrent_bytes": size,
        "v1_info_hash": str(hashes.v1) if hashes.has_v1() else None,
        "v2_info_hash": str(hashes.v2) if hashes.has_v2() else None,
        "pub_date": datetime.datetime.fromtimestamp(ti.creation_date(), datetime.timezone.utc),
        "files": files,
        "bytes": data_bytes,
        "years": years,
        "datasets": datasets,
        "comment": ti.comment() or "",
    }


def load_trackers(path):
    if not os.path.isfile(path):
        fail(f"Tracker list not found: {path}")
    with open(path, encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip() and not line.strip().startswith("#")]


def torrent_url(torrent_id, name):
    return f"{DATA_BASE_URL.rstrip('/')}/{DEEP_ARCHIVE_DIR}/{torrent_id}.{name}"


def build_catalogue(entries):
    catalogue = {
        "generated_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "torrents": [
            {
                "id": e["id"],
                "torrent_url": torrent_url(e["id"], "torrent"),
                "txt_url": torrent_url(e["id"], "txt"),
                "v1_info_hash": e["v1_info_hash"],
                "v2_info_hash": e["v2_info_hash"],
                "files": e["files"],
                "bytes": e["bytes"],
                "years": e["years"],
                "datasets": e["datasets"],
                "pub_date": e["pub_date"].strftime("%Y-%m-%dT%H:%M:%SZ"),
            }
            for e in entries
        ],
    }
    return json.dumps(catalogue, indent=2, sort_keys=True) + "\n"


def build_feed(entries, trackers):
    items = []
    for e in sorted(entries, key=lambda e: e["pub_date"], reverse=True):
        magnet = build_magnet(e["id"], e["v1_info_hash"], e["v2_info_hash"], trackers)
        items.append(
            f"""    <item>
      <title>{xml_escape.escape(e['id'])}</title>
      <link>{xml_escape.escape(torrent_url(e['id'], 'torrent'))}</link>
      <guid isPermaLink="false">{e['v1_info_hash']}</guid>
      <pubDate>{e['pub_date'].strftime('%a, %d %b %Y %H:%M:%S GMT')}</pubDate>
      <description>{xml_escape.escape(
          f"EVE Ref Deep Archive, {fmt_bytes(e['bytes'])}, {fmt_count(e['files'])} files. "
          f"Datasets: {e['datasets']}. File list: {torrent_url(e['id'], 'txt')}"
      )}</description>
      <enclosure url="{xml_escape.escape(torrent_url(e['id'], 'torrent'))}" length="{e['torrent_bytes']}"
                 type="application/x-bittorrent"/>
      <torrent:infoHash>{e['v1_info_hash']}</torrent:infoHash>
      <torrent:magnetURI><![CDATA[{magnet}]]></torrent:magnetURI>
      <torrent:contentLength>{e['bytes']}</torrent:contentLength>
    </item>"""
        )
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:torrent="http://xmlns.ezrss.it/0.1/">
  <channel>
    <title>EVE Ref Deep Archive</title>
    <link>{DATA_BASE_URL.rstrip('/')}/{DEEP_ARCHIVE_DIR}/</link>
    <description>Archived datasets from data.everef.net</description>
{chr(10).join(items)}
  </channel>
</rss>
"""


def build_magnet(torrent_id, v1_hash, v2_hash, trackers):
    params = [f"xt=urn:btih:{v1_hash}"]
    if v2_hash:
        params.append(f"xt=urn:btmh:1220{v2_hash}")
    params.append(f"dn={torrent_id}")
    params += [f"tr={t}" for t in trackers]
    return "magnet:?" + "&".join(params)


def fetch_existing(remote_path):
    folder, _, name = remote_path.rpartition("/")
    out, err = run_rclone_lsjson(folder, name)
    if err:
        return None
    return rclone("cat", remote_path)


def run_rclone_lsjson(folder, name):
    import subprocess

    result = subprocess.run(
        ["rclone", "lsjson", "--files-only", "--no-mimetype", folder], capture_output=True, text=True
    )
    if result.returncode != 0:
        return None, "listing failed"
    entries = json.loads(result.stdout)
    return (True, None) if any(e["Name"] == name for e in entries) else (None, "not found")


def print_diff(old_text, new_text, name):
    old_lines = (old_text or "").splitlines(keepends=True)
    new_lines = new_text.splitlines(keepends=True)
    diff = list(difflib.unified_diff(old_lines, new_lines, fromfile=f"published/{name}", tofile=f"new/{name}"))
    if not diff:
        print(f"No change to {name}")
    else:
        print("".join(diff), end="")


def upload(scratch_dir, deep_archive, name, text, content_type):
    tmp_path = os.path.join(scratch_dir, name)
    with open(tmp_path, "w", encoding="utf-8") as f:
        f.write(text)
    try:
        rclone(
            "copyto",
            tmp_path,
            join(deep_archive, name),
            "--header-upload",
            f"Content-Type: {content_type}",
            "--header-upload",
            f"Cache-Control: max-age={FEED_CACHE_MAX_AGE_SECONDS}",
        )
    finally:
        os.remove(tmp_path)
    print(f"Uploaded {join(deep_archive, name)}")


if __name__ == "__main__":
    main()
