"""
seed_dir: the merged SEED_DIR tree as a whole (NOTES.md "Merged seed directory") -- every published torrent's files,
laid out like data.everef.net, none of them under the torrent's own root folder. Loads every signed .torrent in
SEED_DIR/.torrents/ and checks the tree against them: every file on disk belongs to exactly one torrent, nothing is
missing, changed or unowned, and there's no collision between torrents once merged (quick_check); separately,
qBittorrent is seeding each one cleanly (check_qbittorrent), and (check_sha256, slow) every torrent's own checksum
file passes from the SEED_DIR root.

Used by 00-preflight and 08-check-seed (quick_check only) and check-seed-dir.py (quick_check, check_qbittorrent and,
with --full, check_sha256 -- also the one README Appendix C's monthly maintenance runs).
"""

import glob
import os
from collections import defaultdict

from workflow import STAGING_DIR_NAME, TORRENTS_DIR_NAME, merge_conflicts, verify_sha256sums, verify_signature

# qBittorrent states that count as seeding (08-check-seed.py's SEEDING_STATES): not still checking, not an error.
SEEDING_STATES = {"uploading", "stalledUP", "queuedUP", "forcedUP"}


class SeedDirError(Exception):
    pass


def read_torrent_entries(torrent_path):
    """
    (torrent_id, {relative path: (size, modification time)}) straight from a .torrent's raw bencoded info dict:
    BEP 3's "files" list paths are already relative to the torrent's own name/root folder, so there's no prefix to
    strip. libtorrent's typed torrent_info doesn't expose a file's mtime once loaded (NOTES.md "libtorrent"), hence
    reading the raw dict. v1-only torrents have no pad files to filter out (NOTES.md "Piece alignment").
    """
    import libtorrent as lt

    with open(torrent_path, "rb") as f:
        data = f.read()
    info = lt.bdecode(data)[b"info"]
    torrent_id = info[b"name"].decode("utf-8")
    files = {}
    for file_e in info[b"files"]:
        path = "/".join(part.decode("utf-8") for part in file_e[b"path"])
        files[path] = (file_e[b"length"], file_e[b"mtime"])
    return torrent_id, files


def load_torrents(torrents_dir):
    """
    {torrent_id: {"files": {path: (size, mtime)}, "v1_hash": str}} for every <ID>.torrent in SEED_DIR/.torrents/,
    each signature verified first (verify_signature; fails via workflow.fail on a missing or bad one). Fails
    (SeedDirError) if a torrent's own name doesn't match its file name.
    """
    torrents = {}
    for torrent_path in sorted(glob.glob(os.path.join(torrents_dir, "*.torrent"))):
        verify_signature(torrent_path)
        torrent_id, files = read_torrent_entries(torrent_path)
        expected_name = os.path.basename(torrent_path)[: -len(".torrent")]
        if torrent_id != expected_name:
            raise SeedDirError(f"{torrent_path}: torrent name {torrent_id!r} != file name {expected_name!r}")
        import libtorrent as lt

        ti = lt.torrent_info(torrent_path)
        torrents[torrent_id] = {"files": files, "v1_hash": str(ti.info_hashes().v1)}
    return torrents


def disk_paths(seed_dir):
    """
    {path: (size, modification time)} for every file directly under SEED_DIR, skipping STAGING_DIR_NAME,
    TORRENTS_DIR_NAME and any other dot-folder.
    """
    skip_top = {STAGING_DIR_NAME, TORRENTS_DIR_NAME}
    paths = {}
    for dirpath, dirnames, filenames in os.walk(seed_dir, followlinks=False):
        rel_dir = os.path.relpath(dirpath, seed_dir)
        parts = [] if rel_dir == "." else rel_dir.split(os.sep)
        if parts and (parts[0] in skip_top or parts[0].startswith(".")):
            dirnames[:] = []
            continue
        dirnames[:] = [d for d in dirnames if not d.startswith(".")]
        for name in filenames:
            full = os.path.join(dirpath, name)
            rel = (f"{rel_dir}/{name}" if rel_dir != "." else name).replace(os.sep, "/")
            stat = os.stat(full)
            paths[rel] = (stat.st_size, int(stat.st_mtime))
    return paths


def check_ownership(torrents, disk_stats):
    """
    Problem strings: a path owned by more than one torrent (merge_conflicts in 01-find should already have prevented
    this -- finding one here means SEED_DIR changed independently of the workflow), a path on disk owned by none, a
    path owned but missing from disk, or on disk with a different size or modification time than its torrent
    records. torrents: {torrent_id: {"files": {path: (size, mtime)}, ...}}.
    """
    problems = []
    owners = defaultdict(list)
    for torrent_id, info in torrents.items():
        for path in info["files"]:
            owners[path].append(torrent_id)
    for path, ids in sorted(owners.items()):
        if len(ids) > 1:
            problems.append(f"{path}: owned by {len(ids)} torrents: {sorted(ids)}")
    for path in sorted(set(disk_stats) - set(owners)):
        problems.append(f"{path}: on disk but not owned by any torrent")
    for torrent_id, info in sorted(torrents.items()):
        for path, (size, mtime) in sorted(info["files"].items()):
            stat = disk_stats.get(path)
            if stat is None:
                problems.append(f"{path}: owned by {torrent_id} but missing from disk")
            elif stat != (size, mtime):
                problems.append(
                    f"{path}: owned by {torrent_id}, disk has {stat[0]} bytes modified {stat[1]}, "
                    f"the torrent expects {size} bytes modified {mtime}"
                )
    return problems


def check_merge_conflicts(torrents):
    """
    merge_conflicts across every torrent's paths together: case/Unicode-normalised collisions and file/folder
    clashes between torrents. Exact duplicates are check_ownership's "owned by more than one torrent", not this.
    """
    all_paths = [path for info in torrents.values() for path in info["files"]]
    return merge_conflicts(all_paths)


def quick_check(seed_dir, torrents_dir):
    """
    (torrents, disk, problems): loads every signed torrent, walks the disk tree, and returns check_ownership plus
    check_merge_conflicts' problems (empty if clean). Fails (SeedDirError) on a torrent/file name mismatch; lets
    verify_signature's own workflow.fail() propagate as SystemExit on a bad signature.
    """
    torrents = load_torrents(torrents_dir)
    disk = disk_paths(seed_dir)
    problems = check_ownership(torrents, disk) + check_merge_conflicts(torrents)
    return torrents, disk, problems


def check_qbittorrent(client, torrents, expected_save_path):
    """
    Problem strings: a torrent (by its v1 info-hash, qBittorrent's own ID since these are v1-only) not in
    qBittorrent, not at 100% with 0 bytes downloaded, not in a seeding state, or with a different save path than
    expected_save_path (qbittorrent_save_path(seed_dir): SEED_DIR as qBittorrent itself sees it).
    """
    from qbittorrent import QbittorrentError

    problems = []
    for torrent_id, info in sorted(torrents.items()):
        qbittorrent_id = info["v1_hash"]  # v1-only: no v2 hash to prefer (qbittorrent.torrent_id)
        try:
            qinfo = client.info(qbittorrent_id)
        except QbittorrentError as e:
            problems.append(f"{torrent_id}: can't read from qBittorrent: {e}")
            continue
        if qinfo is None:
            problems.append(f"{torrent_id}: not in qBittorrent")
            continue
        if qinfo["progress"] != 1.0 or qinfo["downloaded"] != 0:
            problems.append(
                f"{torrent_id}: progress {qinfo['progress'] * 100:.2f}%, {qinfo['downloaded']} bytes downloaded"
            )
        if qinfo["state"] not in SEEDING_STATES:
            problems.append(f"{torrent_id}: state {qinfo['state']!r}, not a seeding state")
        if qinfo["save_path"] != expected_save_path:
            problems.append(f"{torrent_id}: save path {qinfo['save_path']!r}, expected {expected_save_path!r}")
    return problems


def check_sha256(torrents, seed_dir):
    """
    sha256sum -c <ID>.sha256 from the SEED_DIR root, for every torrent: the checksum file's paths have no root
    folder (they never did -- NOTES.md "Published files"), so this works once merged. Fails via workflow.fail() on
    a mismatch, the same as 03-load's own check.
    """
    for torrent_id in sorted(torrents):
        sha256_path = os.path.join(seed_dir, f"{torrent_id}.sha256")
        verify_sha256sums(sha256_path, seed_dir)
