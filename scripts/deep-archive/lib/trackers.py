"""
trackers: the trackers for a new torrent (NOTES.md "Trackers"): a small, fixed list of long-lived public UDP
trackers, picked by hand, not fetched from anywhere at run time. Also a BEP 15 connectivity check for 00-preflight.

stdlib only, for 00-preflight and 05-make-torrent.
"""

import os
import secrets
import socket
import struct
import urllib.parse

# The fixed tracker list, one URL per line, blank lines and # comments ignored (NOTES.md "Trackers"): picked by hand
# for longevity, not fetched or ranked from anywhere, so the list a torrent is made with never depends on what a
# third-party list happens to contain that day, and never carries a piracy-branded or short-lived domain in. Edit
# the file by hand if a tracker stops answering; 00-preflight checks each is reachable (check_trackers).
GOOD_TRACKERS_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)), "trackers.txt")


def load_good_trackers(path=GOOD_TRACKERS_PATH):
    trackers = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                trackers.append(line)
    return trackers


GOOD_TRACKERS = load_good_trackers()

# 00-preflight fails if fewer than this many of GOOD_TRACKERS answer a connect request.
MIN_WORKING_TRACKERS = 3

# Schemes a libtorrent client can use; also read by 08-check-seed to tell a real tracker row from DHT/PEX/LSD in
# qBittorrent's torrents/trackers listing.
TRACKER_SCHEMES = ("udp://", "http://", "https://")

# BEP 15 (UDP Tracker Protocol): the fixed "magic" connection ID of a connect request, and the action numbers.
CONNECT_MAGIC = 0x41727101980
CONNECT_ACTION = 0
CONNECT_TIMEOUT = 5  # seconds


def check_tracker(url, timeout=CONNECT_TIMEOUT):
    """
    Whether a udp:// tracker answers a BEP 15 connect request. Returns (True, None) if it does, or (False, reason)
    if it doesn't answer, answers wrong, or isn't UDP (there's no connectivity check this cheap for HTTP(S)
    trackers, so those are reported as unchecked, not failed).
    """
    parts = urllib.parse.urlsplit(url)
    if parts.scheme != "udp":
        return None, "not UDP: not checked"
    if not parts.hostname or not parts.port:
        return False, "no host or port in the URL"
    transaction_id = secrets.randbits(32)
    packet = struct.pack(">QII", CONNECT_MAGIC, CONNECT_ACTION, transaction_id)
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.settimeout(timeout)
            sock.sendto(packet, (parts.hostname, parts.port))
            response, _ = sock.recvfrom(16)
    except OSError as e:
        return False, str(e) or type(e).__name__
    if len(response) < 16:
        return False, f"short response ({len(response)} bytes)"
    action, reply_transaction_id, _connection_id = struct.unpack(">IIQ", response[:16])
    if action != CONNECT_ACTION or reply_transaction_id != transaction_id:
        return False, "unexpected connect response"
    return True, None


def check_trackers(trackers, timeout=CONNECT_TIMEOUT):
    """[(url, ok, reason)] for every tracker, in order: check_tracker's result for each. ok is None for a tracker
    this can't check (not UDP)."""
    return [(url, *check_tracker(url, timeout)) for url in trackers]
