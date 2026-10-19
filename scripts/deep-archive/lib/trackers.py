"""
trackers: the trackers for a new torrent (PLAN 2.4 "Trackers"), downloaded when it's made: the union of the "best" lists
from ngosang/trackerslist and XIU2/TrackersListCollection, in that order, duplicates dropped. Only UDP and HTTP(S)
trackers are kept; libtorrent clients can't use the rest (WebSocket, I2P, ...).

stdlib only, for 00-preflight and 04-make-torrent.
"""

import urllib.error
import urllib.request

# (name, URL), in the order their trackers go into the torrent: ngosang's list is ranked, so its trackers come first.
TRACKER_SOURCES = [
    ("ngosang", "https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_best.txt"),
    ("XIU2", "https://raw.githubusercontent.com/XIU2/TrackersListCollection/master/best.txt"),
]
TRACKER_SCHEMES = ("udp://", "http://", "https://")

# Fewest trackers a torrent may have, after the union.
MIN_TRACKERS = 10

FETCH_TIMEOUT = 30  # seconds


class TrackersError(Exception):
    pass


def parse_tracker_list(text):
    """
    (trackers, skipped) from a tracker list: one URL per line, blank lines and # comments ignored. trackers are the
    UDP and HTTP(S) ones, in order, duplicates dropped; skipped are the other lines.
    """
    trackers = []
    skipped = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if not line.startswith(TRACKER_SCHEMES):
            skipped.append(line)
        elif line not in trackers:
            trackers.append(line)
    return trackers, skipped


def combine(lists):
    """
    The union of the tracker lists, given as [(name, text)], in order. Raises TrackersError if a list has no usable
    trackers, or the union has fewer than MIN_TRACKERS. Returns (trackers, [(name, usable count, skipped)]).
    """
    trackers = []
    counts = []
    for name, text in lists:
        usable, skipped = parse_tracker_list(text)
        if not usable:
            raise TrackersError(f"No UDP or HTTP(S) trackers in the {name} list")
        counts.append((name, len(usable), skipped))
        trackers += [t for t in usable if t not in trackers]
    if len(trackers) < MIN_TRACKERS:
        raise TrackersError(f"Only {len(trackers)} trackers from all lists together, need at least {MIN_TRACKERS}")
    return trackers, counts


def fetch_tracker_lists():
    """[(name, url, text)] for every TRACKER_SOURCES list. Raises TrackersError if one can't be downloaded."""
    lists = []
    for name, url in TRACKER_SOURCES:
        request = urllib.request.Request(url, headers={"User-Agent": "EVE Ref Deep Archive"})
        try:
            with urllib.request.urlopen(request, timeout=FETCH_TIMEOUT) as response:
                text = response.read().decode("utf-8")
        except urllib.error.HTTPError as e:
            raise TrackersError(f"Can't download the {name} tracker list {url}: HTTP {e.code}")
        except (urllib.error.URLError, OSError) as e:
            raise TrackersError(f"Can't download the {name} tracker list {url}: {getattr(e, 'reason', e)}")
        except UnicodeDecodeError:
            raise TrackersError(f"The {name} tracker list {url} isn't UTF-8")
        lists.append((name, url, text))
    return lists
