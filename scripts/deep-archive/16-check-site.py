#!/usr/bin/env python3
"""
check-site: a step of the EVE Ref Deep Archive workflow, and the last scripted one. Checks what data.everef.net actually
serves after the delete and the full DataIndex run, over HTTPS (PLAN 2.16).

DRAFT. Read-only, over HTTPS.

    16-check-site.py

Needs 14-check-delete's completion file, and refuses to run twice (lib/workflow.py). Run this only after the
manual "Full DataIndex" step (README) has finished.

Per sampled URL (a spread of deleted files across datasets and months, every deleted index.json, PLAN 2.16):
- 404: pass.
- 200 with cf-cache-status: HIT: pass, reported as "still cached" (up to 30 days, DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE;
  accepted).
- Anything else (200 from the origin, 5xx, ...): fail.

Also checks deep-archive/ lists the torrent, the .txt, the torrent's signature and the feed, and that a sample of
parent folders' index.json no longer mention a deleted file.

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR
"""

import json
import os
import random
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    DATA_BASE_URL,
    DEEP_ARCHIVE_DIR,
    Workflow,
    error,
    fail,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "check-site"
SAMPLE_SIZE = 50
PARENT_SAMPLE_SIZE = 10
RANDOM_SEED = 0  # deterministic sample, reproducible between runs of the same deleted.jsonl


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    base = DATA_BASE_URL.rstrip("/")

    section("Loading deleted.jsonl")
    deleted_log = os.path.join(workflow.scratch_dir, "deleted.jsonl")
    deleted = read_jsonl(deleted_log)
    data_paths = [d["path"] for d in deleted if d.get("kind") == "data"]
    index_paths = [d["path"] for d in deleted if d.get("kind") == "index"]
    print(f"{fmt_count(len(data_paths))} deleted data files, {fmt_count(len(index_paths))} cleaned-up index files")

    rng = random.Random(RANDOM_SEED)
    sample = sorted(rng.sample(data_paths, min(SAMPLE_SIZE, len(data_paths)))) + sorted(index_paths)
    print(f"Checking {fmt_count(len(sample))} URLs")

    problems = []
    cached = 0
    for path in sample:
        url = f"{base}/{path}"
        status, cache_status = fetch_status(url)
        if status == 404:
            continue
        if status == 200 and cache_status == "HIT":
            cached += 1
            continue
        problems.append(f"{url}: HTTP {status}, cf-cache-status {cache_status}")
    print(f"{fmt_count(len(sample) - len(problems) - cached)} 404, {fmt_count(cached)} still cached (HIT)")

    section("deep-archive/ listing")
    problems += check_deep_archive_listing(base, torrent_id)

    section("Parent folder listings")
    problems += check_parent_listings(base, data_paths, rng)

    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} problems with the site after deletion")

    workflow.complete(STEP, torrent_id, {"sampled": len(sample), "still_cached": cached})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check site OK for {torrent_id}")


def read_jsonl(path):
    if not os.path.isfile(path):
        fail(f"Not found: {path}")
    with open(path, encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


def fetch_status(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.status, response.headers.get("cf-cache-status")
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("cf-cache-status")
    except urllib.error.URLError as e:
        return 0, str(e.reason)


def index_file_names(index):
    """File names in a DataIndex index.json: {"path": ..., "files": [{"name": ...}, ...], "directories": [...]}."""
    if not isinstance(index, dict) or not isinstance(index.get("files"), list):
        raise ValueError(f"not a DataIndex index.json: {str(index)[:200]}")
    return {f.get("name") for f in index["files"]}


def check_deep_archive_listing(base, torrent_id):
    url = f"{base}/{DEEP_ARCHIVE_DIR}/index.json"
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            entries = json.loads(response.read())
    except (urllib.error.HTTPError, urllib.error.URLError) as e:
        return [f"{url}: {e}"]
    names = index_file_names(entries)
    expected = {f"{torrent_id}.torrent", f"{torrent_id}.txt", f"{torrent_id}.torrent.minisig", "everef-eve-online-deep-archive.xml"}
    missing = expected - names
    if missing:
        return [f"{url}: missing {sorted(missing)}"]
    print(f"{url}: lists {sorted(expected)}")
    return []


def check_parent_listings(base, data_paths, rng):
    problems = []
    folders = sorted({p.rsplit("/", 1)[0] for p in data_paths if "/" in p})
    sample_folders = rng.sample(folders, min(PARENT_SAMPLE_SIZE, len(folders)))
    for folder in sample_folders:
        url = f"{base}/{folder}/index.json"
        try:
            with urllib.request.urlopen(url, timeout=30) as response:
                if response.status != 200:
                    continue
                entries = json.loads(response.read())
        except (urllib.error.HTTPError, urllib.error.URLError):
            continue  # folder itself was cleaned up (no index left): fine
        names = index_file_names(entries)
        deleted_here = {p.rsplit("/", 1)[1] for p in data_paths if p.rsplit("/", 1)[0] == folder}
        still_listed = names & deleted_here
        if still_listed:
            problems.append(f"{url}: still lists deleted file(s) {sorted(still_listed)[:5]}")
        else:
            print(f"{url}: no longer lists its deleted files")
    return problems


if __name__ == "__main__":
    main()
