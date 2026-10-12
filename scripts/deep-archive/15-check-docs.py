#!/usr/bin/env python3
"""
check-docs: step 16 of the EVE Ref Deep Archive workflow. Checks the Deep Archive docs page is reachable and its
links resolve, and prints the scripts' current rules next to it for the operator to compare.

DRAFT. Read-only, over HTTPS.

    15-check-docs.py

Needs 14-check-feed's completion file, and refuses to run twice (lib/workflow.py).

This is a review, not a full check: whether the page's wording still matches the rules is a judgement call for
the operator (PLAN 4.3). The script fetches the page, follows every link it finds, and fails if any of them
don't resolve; then it prints MIN_AGE_YEARS, TORRENT_EXCLUDE and the size limits so they can be compared against
what the page says by eye. Update the page and confirm it's live before deleting anything (README, step 15).

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR
"""

import html.parser
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
from workflow import (  # noqa: E402
    MAX_BYTES,
    MAX_TORRENT_BYTES,
    MIN_AGE_YEARS,
    TORRENT_EXCLUDE,
    Workflow,
    error,
    fail,
    fmt_bytes,
    fmt_count,
    fmt_duration,
    ok,
    section,
)

STEP = "check-docs"
DOCS_PAGE = "https://docs.everef.net/datasets/deep-archive.html"


class LinkCollector(html.parser.HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []

    def handle_starttag(self, tag, attrs):
        attr = "href" if tag == "a" else "src" if tag in ("img", "script") else None
        if attr:
            value = dict(attrs).get(attr)
            if value:
                self.links.append(value)


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()

    section("Docs page")
    status, body = fetch(DOCS_PAGE)
    if status != 200:
        fail(f"{DOCS_PAGE}: HTTP {status}")
    print(f"{DOCS_PAGE}: 200, {len(body)} bytes")

    section("Links")
    collector = LinkCollector()
    collector.feed(body.decode("utf-8", errors="replace"))
    links = sorted({urllib.parse.urljoin(DOCS_PAGE, link) for link in collector.links if not link.startswith("#")})
    links = [link for link in links if link.startswith(("http://", "https://"))]  # not mailto:, magnet:, ...
    problems = []
    for link in links:
        link_status = fetch_status(link)
        print(f"  {link_status} {link}")
        if link_status not in (200, 301, 302):
            problems.append(f"{link}: HTTP {link_status}")
    if problems:
        for problem in problems:
            error(f"  {problem}")
        fail(f"{len(problems)} broken link(s) on {DOCS_PAGE}")

    section("Current rules (compare against the page by eye)")
    print(f"Minimum age: {MIN_AGE_YEARS} years")
    print(f"Excluded datasets: {', '.join(TORRENT_EXCLUDE)}")
    print(f"Size limits: data {fmt_bytes(MAX_BYTES)} per torrent, .torrent {fmt_count(MAX_TORRENT_BYTES)} bytes")
    print("If any of this changed, update the docs page and confirm it's live before deleting anything.")

    workflow.complete(STEP, torrent_id, {"links_checked": len(links)})
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Check docs OK for {torrent_id}")


def fetch(url):
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as e:
        return e.code, b""
    except urllib.error.URLError as e:
        fail(f"{url}: {e.reason}")


def fetch_status(url):
    try:
        request = urllib.request.Request(url, method="HEAD")
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status
    except urllib.error.HTTPError as e:
        return e.code
    except urllib.error.URLError:
        return 0


if __name__ == "__main__":
    main()
