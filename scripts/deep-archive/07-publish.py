#!/usr/bin/env python3
"""
publish: step 8 of the EVE Ref Deep Archive workflow. Uploads <ID>.torrent and <ID>.txt to the bucket's
deep-archive/, rebuilds feed.xml and torrents.json from every published .torrent, and checks what data.everef.net
serves for all four.

DRAFT. Shows the targets and asks before each upload (lib/workflow.py, "Show, then ask"). Writes to the data bucket.

    07-publish.py

Needs 06-check-seed's completion file, and refuses to run twice (lib/workflow.py).

1. Upload <ID>.torrent and <ID>.txt to deep-archive/ with the right Content-Type. Never overwrites: a file already
   published must be byte-identical to the local one (an earlier attempt that failed later), and is then skipped.
2. Rebuild torrents.json (the catalogue) and feed.xml (RSS 2.0, PLAN 2.7) in full, from every published .torrent in
   deep-archive/ -- never from local state, so the bucket stays the single source of truth (PLAN 2.5). Each torrent
   is read with libtorrent: name (the ID), creation date, comment (years/datasets, PLAN 2.4), v1/v2 info-hashes, data
   file count and bytes (the README excluded), trackers. Magnet links carry the torrent's first MAGNET_TRACKERS
   trackers; newest first.
3. Show the diff against what's published, and ask. Before uploading, save the published feed.xml to
   SCRATCH_DIR/feed-backup/feed-<UTC time>.xml.
4. Check over HTTPS, never trusting the uploads' own result: <ID>.torrent and <ID>.txt return 200 with the right
   Content-Type and exactly the local bytes; feed.xml is served as application/rss+xml, is valid XML and has an
   <item> with this torrent's v1 info-hash as its <guid>; torrents.json is served as application/json, is valid JSON
   and lists this torrent with the right v1/v2 info-hashes (04-make-torrent). Cloudflare may serve the old feed for
   up to FEED_CACHE_MAX_AGE_SECONDS, so the checks retry until CHECK_TIMEOUT.

On the first workflow, also add the feed to a qBittorrent RSS reader and check the item shows up (manual, README).

Writes, in SCRATCH_DIR:

    feed-backup/feed-<UTC time>.xml   the feed.xml published before this run (none on the first workflow)
    07-publish.done                   completion file

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
import subprocess
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
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
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    join,
    ok,
    rclone,
    section,
)

STEP = "publish"
# Magnet links carry the first trackers of the torrent's own list (ngosang's ranked best first, lib/trackers.py), to
# keep them short.
MAGNET_TRACKERS = 10
COMMENT_RE = re.compile(r"^Data from (.+)\. Datasets: (.+)\.$", re.MULTILINE)

UPLOADS = (
    ("{id}.torrent", "application/x-bittorrent"),
    ("{id}.txt", "text/plain; charset=utf-8"),
)

# The HTTPS checks retry until they pass or this many seconds have gone by: long enough for Cloudflare's copy of the
# old feed.xml to expire.
CHECK_TIMEOUT = FEED_CACHE_MAX_AGE_SECONDS + 60
CHECK_INTERVAL = 15  # seconds


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    remote = data_remote()
    deep_archive = join(remote, DEEP_ARCHIVE_DIR)
    make_torrent = workflow.read_done("make-torrent")["summary"]
    v1_hash, v2_hash = make_torrent["v1_info_hash"], make_torrent["v2_info_hash"]

    uploaded = upload_torrent(workflow, torrent_id, deep_archive)
    torrents, feed_backup = publish_feed(workflow, torrent_id, deep_archive)
    check_published(workflow, torrent_id, v1_hash, v2_hash)

    workflow.complete(
        STEP,
        torrent_id,
        {"uploaded": uploaded, "torrents_in_feed": torrents, "feed_backup": feed_backup},
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Publish OK for {torrent_id}")


# Upload <ID>.torrent and <ID>.txt.


def upload_torrent(workflow, torrent_id, deep_archive):
    """Uploads the .torrent and .txt, never overwriting. Returns the remote paths uploaded by this run."""
    section("Torrent and file list: targets")
    uploads = []
    for name_fmt, content_type in UPLOADS:
        name = name_fmt.format(id=torrent_id)
        local = os.path.join(workflow.scratch_dir, name)
        if not os.path.isfile(local):
            fail(f"Not found: {local}")
        remote_path = join(deep_archive, name)
        print(f"{local} -> {remote_path} ({content_type}, {fmt_bytes(os.path.getsize(local))})")
        if is_published(remote_path):
            with open(local, "rb") as f:
                local_bytes = f.read()
            if rclone_cat_bytes(remote_path) != local_bytes:
                fail(f"Already published with different content, never overwritten: {remote_path}")
            print("  already published by an earlier attempt, identical: skipped")
            continue
        uploads.append((local, remote_path, content_type))

    if uploads:
        typed = input(f"Type {torrent_id} to upload: ")
        if typed.strip() != torrent_id:
            fail("Confirmation didn't match, aborting")
        section("Torrent and file list: uploading")
        for local, remote_path, content_type in uploads:
            rclone("copyto", local, remote_path, "--header-upload", f"Content-Type: {content_type}")
            print(f"Uploaded {remote_path}")
    return [u[1] for u in uploads]


def is_published(remote_path):
    folder, _, name = remote_path.rpartition("/")
    args = ["rclone", "lsjson", "--files-only", "--no-mimetype", folder]
    result = subprocess.run(args, capture_output=True, text=True)
    if result.returncode != 0:
        lines = [line for line in result.stderr.splitlines() if line.strip()]
        if lines and "directory not found" in lines[-1]:
            return False
        fail(f"Can't list {folder}: {lines[-1] if lines else f'exit code {result.returncode}'}")
    return any(entry["Name"] == name for entry in json.loads(result.stdout))


def rclone_cat_bytes(remote_path):
    result = subprocess.run(["rclone", "cat", remote_path], capture_output=True)
    if result.returncode != 0:
        fail(f"rclone cat {remote_path} failed: exit code {result.returncode}")
    return result.stdout


# Rebuild and upload feed.xml and torrents.json.


def publish_feed(workflow, torrent_id, deep_archive):
    """Rebuilds the feed and catalogue from the bucket and uploads them. Returns (torrent count, feed backup path)."""
    section("Feed: published torrents")
    names = list_torrents(deep_archive)
    if torrent_id not in [n[: -len(".torrent")] for n in names]:
        fail(f"{torrent_id}.torrent isn't in {deep_archive}/ after uploading it")
    print(f"{len(names)} published .torrent files")

    tmp_dir = os.path.join(workflow.scratch_dir, "publish")
    if os.path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)
    os.makedirs(tmp_dir)
    try:
        entries = [read_torrent(deep_archive, tmp_dir, name) for name in names]
    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)
    entries.sort(key=lambda e: e["id"])
    print(f"Read {len(entries)} torrents")

    torrents_json = build_catalogue(entries)
    feed_xml = build_feed(entries)

    section("Feed: diff of torrents.json")
    old_json = fetch_existing(deep_archive, "torrents.json")
    print_diff(old_json, torrents_json, "torrents.json")

    section("Feed: diff of feed.xml")
    old_xml = fetch_existing(deep_archive, "feed.xml")
    print_diff(old_xml, feed_xml, "feed.xml")

    typed = input(f"Type {torrent_id} to publish the feed and catalogue: ")
    if typed.strip() != torrent_id:
        fail("Confirmation didn't match, aborting")

    section("Feed: backup")
    feed_backup = backup_feed(workflow.scratch_dir, old_xml)

    section("Feed: uploading")
    upload_text(workflow.scratch_dir, deep_archive, "torrents.json", torrents_json, "application/json")
    upload_text(workflow.scratch_dir, deep_archive, "feed.xml", feed_xml, "application/rss+xml")
    return len(entries), feed_backup


def backup_feed(scratch_dir, old_xml):
    """Saves the published feed.xml as SCRATCH_DIR/feed-backup/feed-<UTC time>.xml. Returns its path, or None."""
    if old_xml is None:
        print("No feed.xml published yet: nothing to back up")
        return None
    backup_dir = os.path.join(scratch_dir, "feed-backup")
    os.makedirs(backup_dir, exist_ok=True)
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    path = os.path.join(backup_dir, f"feed-{stamp}.xml")
    with open(path, "w", encoding="utf-8") as f:
        f.write(old_xml)
    print(f"Saved the published feed.xml to {path}")
    return path


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
    root_names = {f"{torrent_id}-README.txt"}
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
        "trackers": [t.url for t in sorted(ti.trackers(), key=lambda t: t.tier)],
    }


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


def build_feed(entries):
    items = []
    for e in sorted(entries, key=lambda e: e["pub_date"], reverse=True):
        magnet = build_magnet(e["id"], e["v1_info_hash"], e["v2_info_hash"], e["trackers"][:MAGNET_TRACKERS])
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


def fetch_existing(deep_archive, name):
    """The published file's text, or None if there isn't one."""
    remote_path = join(deep_archive, name)
    if not is_published(remote_path):
        return None
    return rclone("cat", remote_path)


def print_diff(old_text, new_text, name):
    old_lines = (old_text or "").splitlines(keepends=True)
    new_lines = new_text.splitlines(keepends=True)
    diff = list(difflib.unified_diff(old_lines, new_lines, fromfile=f"published/{name}", tofile=f"new/{name}"))
    if not diff:
        print(f"No change to {name}")
    else:
        print("".join(diff), end="")


def upload_text(scratch_dir, deep_archive, name, text, content_type):
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


# Check what data.everef.net serves.


def check_published(workflow, torrent_id, v1_hash, v2_hash):
    """Checks all four files over HTTPS, retrying until they pass or CHECK_TIMEOUT. Fails listing what's still wrong."""
    section("Checking what data.everef.net serves")
    base = DATA_BASE_URL.rstrip("/") + f"/{DEEP_ARCHIVE_DIR}"
    deadline = time.monotonic() + CHECK_TIMEOUT
    while True:
        problems = []
        for name_fmt, content_type in UPLOADS:
            name = name_fmt.format(id=torrent_id)
            problems += check_file(f"{base}/{name}", os.path.join(workflow.scratch_dir, name), content_type)
        status, headers, body = fetch(f"{base}/feed.xml")
        problems += check_status(status, headers, body, "feed.xml", "application/rss+xml")
        if body:
            problems += check_feed_item(body, torrent_id, v1_hash)
        status, headers, body = fetch(f"{base}/torrents.json")
        problems += check_status(status, headers, body, "torrents.json", "application/json")
        if body:
            problems += check_catalogue_entry(body, torrent_id, v1_hash, v2_hash)
        if not problems:
            print("All four files are served as published")
            return
        if time.monotonic() >= deadline:
            for problem in problems:
                error(f"  {problem}")
            fail(f"{len(problems)} problems with what data.everef.net serves, after {CHECK_TIMEOUT}s")
        print(f"{len(problems)} problems, e.g. {problems[0]}; retrying in {CHECK_INTERVAL}s (Cloudflare cache)")
        time.sleep(CHECK_INTERVAL)


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
    if status != 200:
        return [f"{name}: HTTP {status}"]
    content_type = (headers.get("Content-Type") or "").strip()
    print(f"  200 {content_type}, {len(body)} bytes")
    if content_type != expected_content_type:
        return [f"{name}: Content-Type {content_type!r}, expected {expected_content_type!r}"]
    return []


def check_file(url, local_path, expected_content_type):
    with open(local_path, "rb") as f:
        local_bytes = f.read()
    status, headers, body = fetch(url)
    name = url.rsplit("/", 1)[-1]
    problems = check_status(status, headers, body, name, expected_content_type)
    if body is not None and body != local_bytes:
        problems.append(f"{name}: served bytes don't match {local_path} ({len(body)} vs {len(local_bytes)} bytes)")
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
    entry = {e["id"]: e for e in catalogue.get("torrents", [])}.get(torrent_id)
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
