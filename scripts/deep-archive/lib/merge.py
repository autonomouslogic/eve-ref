"""
merge: moves a torrent's staged content folder (SEED_DIR/.staging/<ID>/) into the merged SEED_DIR tree by
hard-linking every file to its site path directly under SEED_DIR, then removing the staging copy, so the torrent's
own root folder never appears under SEED_DIR itself (NOTES.md "Merged seed directory"). Used by 07-seed, between
copying the signed .torrent into SEED_DIR/.torrents/ and adding it to qBittorrent with content layout NoSubfolder.

A plan (build_plan) is computed once and can be saved by the caller (07-seed writes it to
$SCRATCH_DIR/merge-plan.json) so a later run can resume after an interruption: once a staging file is unlinked,
build_plan can no longer stat it to recompute its (dev, ino), so a resumed run must read the saved plan back
instead of rebuilding it.

Every step here is safe to call again on a plan that's partly done:
- link_all never overwrites an existing target; it only fails if the target exists and isn't the staged file.
- verify_all only reads.
- unlink_staging skips a staging path that's already gone, and never removes the last copy of a file (st_nlink < 2).
"""

import os


class MergeError(Exception):
    pass


def build_plan(staging_dir, seed_dir, entries):
    """
    entries: an iterable of (relative path, size, modification time) for the torrent's data files (the torrent's own
    root folder already stripped). Returns a list of plan entries (dicts: path, size, mtime, staging, target, dev,
    ino), one per file, by stat'ing each one in the staging folder now. Fails (MergeError) if a staging file is
    missing, or its size or modification time don't match the torrent.
    """
    plan = []
    for rel_path, size, mtime in entries:
        staging_path = os.path.join(staging_dir, *rel_path.split("/"))
        try:
            stat = os.stat(staging_path)
        except FileNotFoundError:
            raise MergeError(f"Missing staging file: {staging_path}")
        if stat.st_size != size:
            raise MergeError(f"{staging_path}: {stat.st_size} bytes, the torrent expects {size}")
        if int(stat.st_mtime) != mtime:
            raise MergeError(f"{staging_path}: modified {int(stat.st_mtime)}, the torrent expects {mtime}")
        plan.append(
            {
                "path": rel_path,
                "size": size,
                "mtime": mtime,
                "staging": staging_path,
                "target": os.path.join(seed_dir, *rel_path.split("/")),
                "dev": stat.st_dev,
                "ino": stat.st_ino,
            }
        )
    return plan


def link_all(plan):
    """
    Hard-links every plan entry's staging file to its target, creating parent folders as needed. A target that
    already exists must be the recorded (dev, ino) -- an earlier, interrupted run having already linked it -- or
    this fails (MergeError): a real collision, which 01-find's merge_conflicts should have caught before this ever
    ran, so finding one here means something changed SEED_DIR independently.
    """
    for entry in plan:
        target = entry["target"]
        os.makedirs(os.path.dirname(target), exist_ok=True)
        try:
            os.link(entry["staging"], target)
        except FileExistsError:
            stat = os.stat(target)
            if (stat.st_dev, stat.st_ino) != (entry["dev"], entry["ino"]):
                raise MergeError(f"{target} already exists and isn't the staged file {entry['staging']}: a collision")


def verify_all(plan):
    """
    "<target>: <why>" for every plan entry whose target isn't the staged file (by (dev, ino)), or has the wrong size
    or modification time; empty if every target checks out. Read-only.
    """
    problems = []
    for entry in plan:
        target = entry["target"]
        try:
            stat = os.stat(target)
        except FileNotFoundError:
            problems.append(f"{target}: missing after linking")
            continue
        if (stat.st_dev, stat.st_ino) != (entry["dev"], entry["ino"]):
            problems.append(f"{target}: not the staged file (different inode)")
        elif stat.st_size != entry["size"]:
            problems.append(f"{target}: {stat.st_size} bytes, expected {entry['size']}")
        elif int(stat.st_mtime) != entry["mtime"]:
            problems.append(f"{target}: modified {int(stat.st_mtime)}, expected {entry['mtime']}")
    return problems


def unlink_staging(plan):
    """
    Removes every plan entry's staging file, once its target is confirmed to be the very same file (same (dev, ino))
    with at least one other link (st_nlink >= 2), so the last copy of a file is never removed. Skips a staging path
    that's already gone (an earlier, interrupted run having already unlinked it). Fails (MergeError) if the target
    isn't the staged file -- call verify_all first. Returns the staging paths actually removed.
    """
    removed = []
    for entry in plan:
        staging = entry["staging"]
        if not os.path.exists(staging):
            continue
        stat = os.stat(staging)
        try:
            target_stat = os.stat(entry["target"])
        except FileNotFoundError:
            raise MergeError(f"{entry['target']}: missing, won't unlink {staging}")
        if (target_stat.st_dev, target_stat.st_ino) != (stat.st_dev, stat.st_ino):
            raise MergeError(f"{entry['target']}: not the same file as {staging}, won't unlink")
        if stat.st_nlink < 2:
            raise MergeError(f"{staging}: only {stat.st_nlink} link(s), won't unlink the only copy")
        os.unlink(staging)
        removed.append(staging)
    return removed


def remove_empty_staging_dirs(staging_dir):
    """
    Removes every now-empty folder under staging_dir, deepest first, then staging_dir itself if it's empty too.
    Checks each folder's actual listing rather than os.walk's dirnames/filenames, which os.walk captures before
    recursing and doesn't update as a deeper folder is removed in the same pass.
    """
    for dirpath, _dirnames, _filenames in os.walk(staging_dir, topdown=False):
        if not os.listdir(dirpath):
            os.rmdir(dirpath)
