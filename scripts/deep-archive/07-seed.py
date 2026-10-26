#!/usr/bin/env python3
"""
seed: a step of the EVE Ref Deep Archive workflow. Moves the torrent's staged content folder,
SEED_DIR/.staging/<ID>/, into the merged SEED_DIR tree (NOTES.md "Merged seed directory"), then adds the torrent to
qBittorrent with content layout NoSubfolder, so it checks the files at their site paths, directly under SEED_DIR,
instead of downloading them.

DRAFT. Writes SEED_DIR/.torrents/, merges SEED_DIR/.staging/<ID>/ into SEED_DIR itself (never copies: every file is
hard-linked, then the staging copy is removed), and writes to qBittorrent and SCRATCH_DIR.

    07-seed.py

Needs 06-sign-torrent's completion file, and refuses to run twice (lib/workflow.py).

1. Verify SCRATCH_DIR/<ID>.torrent.minisig (lib/workflow.py's verify_signature), then copy the .torrent and its
   signature into SEED_DIR/.torrents/ (already there, identically, if this is a resumed run).
2. Load the .torrent, check its name and v1 info-hash match 05-make-torrent's, and read every file's path (relative
   to the torrent's own root folder -- BEP 3's "files" list already is), size and modification time straight from
   its raw bencoded info dict (read_torrent_entries; libtorrent's typed API doesn't expose mtime once loaded). v1
   only has no pad files to filter out (NOTES.md "Piece alignment").
3. Build the merge plan (lib/merge.py's build_plan): for every file, stat it in the staging folder and record its
   (dev, ino) there, failing if it's missing or doesn't match the torrent's size/modification time. Saved to
   SCRATCH_DIR/merge-plan.json so a later run can resume once staging files start being removed (step 5) and can no
   longer be stat'd to rebuild the plan.
4. Hard-link every file from the staging folder to its site path directly under SEED_DIR (root files -- the README,
   checksum file, its signature, the index tarball -- have no further path segments, so they land at SEED_DIR's own
   root; data files land at their data.everef.net path). Never overwrites an existing target (lib/merge.py's
   link_all): a target that's already there must be the very file just linked (resuming), or this fails -- a real
   collision 01-find's merge_conflicts should have already ruled out.
5. Verify every target (lib/merge.py's verify_all), then remove the staging copies (lib/merge.py's unlink_staging:
   never the last copy of a file) and the now-empty staging folder.
6. Add the .torrent to qBittorrent with save path QBITTORRENT_SAVE_PATH (SEED_DIR as qBittorrent sees it; SEED_DIR
   itself if not set), automatic torrent management off, content layout NoSubfolder (the files are already directly
   under the save path, step 4 having merged them out of the torrent's own root folder).
7. Read the torrent's files back from qBittorrent (torrents/files) and check their names are exactly step 2's
   relative paths, with no root folder: confirms NoSubfolder actually took.
8. Wait for qBittorrent to report the torrent, turn on super seeding and wait SUPER_SEEDING_DELAY for qBittorrent to
   report it (a warning, not a failure, if it doesn't; NOTES.md "Super seeding": has no effect until the initial
   check qBittorrent runs on the added torrent finishes and it's actually a seed, so the order doesn't matter).
   No recheck is triggered: qBittorrent checks the files when the torrent is added.

If qBittorrent's resume data for this torrent is ever lost and it has to be re-added, it must be re-added with
content layout NoSubfolder, never Original (qBittorrent's own default): Original would look for the files under
SEED_DIR/<ID>/, which no longer exists once this step has run, and the torrent would show as 0% downloaded. No data
is lost -- the files are still at their site paths -- but the re-add has to ask for NoSubfolder by hand.

Writes:
    SEED_DIR/.torrents/<ID>.torrent, <ID>.torrent.minisig
    SEED_DIR/<every torrent path, merged, no root folder>
    SCRATCH_DIR/merge-plan.json
    07-seed.done             completion file

Environment (or torrents.env, see lib/workflow.py):
    SCRATCH_DIR, SEED_DIR, QBITTORRENT_URL, QBITTORRENT_API_KEY
    QBITTORRENT_SAVE_PATH   optional: SEED_DIR as qBittorrent sees it, if different (e.g. qBittorrent in Docker)

Needs libtorrent 2.x.
"""

import filecmp
import json
import os
import shutil
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), "lib"))
import merge  # noqa: E402
from qbittorrent import Client, QbittorrentError  # noqa: E402
from qbittorrent import torrent_id as qbittorrent_torrent_id  # noqa: E402
from workflow import (  # noqa: E402
    Workflow,
    error,
    fail,
    fmt_count,
    fmt_duration,
    ok,
    qbittorrent_save_path,
    qbittorrent_settings,
    section,
    verify_signature,
    warning,
)

STEP = "seed"
WAIT_TIMEOUT = 60  # seconds, for qBittorrent to report the torrent after adding it
WAIT_INTERVAL = 2
SUPER_SEEDING_DELAY = 5  # seconds, for qBittorrent to report super seeding after it's turned on
LOG_SAMPLE = 20


def main():
    started = time.monotonic()
    workflow = Workflow()
    workflow.begin(STEP)
    torrent_id = workflow.torrent_id()
    make_torrent = workflow.read_done("make-torrent")
    v1_hash = make_torrent["summary"]["v1_info_hash"]
    qbittorrent_id = qbittorrent_torrent_id(v1_hash, make_torrent["summary"]["v2_info_hash"])
    torrent_path = os.path.join(workflow.scratch_dir, f"{torrent_id}.torrent")
    content_dir = workflow.content_dir(torrent_id)

    section("Torrent signature")
    verify_signature(torrent_path)

    section("Torrent copy")
    torrents_dir = workflow.torrents_dir()
    os.makedirs(torrents_dir, exist_ok=True)
    copy_torrent_files(torrents_dir, workflow.scratch_dir, torrent_id)
    print(f"Copied into {torrents_dir}: {torrent_id}.torrent, {torrent_id}.torrent.minisig")

    section("Torrent entries")
    check_torrent_identity(torrent_path, torrent_id, v1_hash)
    entries = read_torrent_entries(torrent_path)
    print(f"{fmt_count(len(entries))} files in the torrent (data and root files)")

    section("Merge plan")
    plan_path = os.path.join(workflow.scratch_dir, "merge-plan.json")
    plan = load_or_build_plan(plan_path, content_dir, workflow.seed_dir, entries)
    print(f"{fmt_count(len(plan))} files to merge into {workflow.seed_dir}")

    section("Merge")
    try:
        merge.link_all(plan)
    except merge.MergeError as e:
        fail(str(e))
    problems = merge.verify_all(plan)
    if problems:
        for problem in problems[:LOG_SAMPLE]:
            error(f"  {problem}")
        fail(f"{fmt_count(len(problems))} problems after linking into {workflow.seed_dir}: investigate before going on")
    try:
        removed = merge.unlink_staging(plan)
    except merge.MergeError as e:
        fail(str(e))
    merge.remove_empty_staging_dirs(content_dir)
    print(
        f"Linked {fmt_count(len(plan))} files into {workflow.seed_dir}, removed {fmt_count(len(removed))} staging "
        f"copies, {content_dir} gone"
    )

    section("qBittorrent")
    url, api_key = qbittorrent_settings()
    try:
        client = Client(url, api_key)
        version = client.version()
    except QbittorrentError as e:
        fail(f"Can't reach qBittorrent at {url}: {e}")
    print(f"{url}: qBittorrent {version}")

    print(f"qBittorrent's torrent ID: {qbittorrent_id} (the v2 info-hash, truncated)")
    try:
        existing = client.info(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't read the torrents in qBittorrent: {e}")
    if existing is not None:
        fail(f"Torrent {qbittorrent_id} is already in qBittorrent: remove it by hand first if this is a redo")

    section("Add torrent")
    save_path = qbittorrent_save_path(workflow.seed_dir)
    print(f"Adding {torrent_path}, save path {save_path}, content layout NoSubfolder")
    try:
        client.add_torrent_file(torrent_path, save_path, "NoSubfolder")
    except QbittorrentError as e:
        fail(f"Adding the torrent failed: {e}")

    try:
        info = wait_for(client, qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't read the torrent back from qBittorrent: {e}")
    if info is None:
        fail(f"qBittorrent didn't report {qbittorrent_id} within {WAIT_TIMEOUT}s")
    print(f"Added: state {info['state']}, {info['num_seeds']} seeds reported")

    section("Read back")
    check_no_subfolder(client, qbittorrent_id, entries)
    print(f"qBittorrent reports {fmt_count(len(entries))} files with no subfolder, matching the merged layout")

    section("Super seeding")
    try:
        client.set_super_seeding(qbittorrent_id, True)
        print(f"Waiting {SUPER_SEEDING_DELAY}s for qBittorrent to apply it")
        time.sleep(SUPER_SEEDING_DELAY)
        info = client.info(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Turning on super seeding failed: {e}")
    super_seeding = info is not None and bool(info.get("super_seeding"))
    if super_seeding:
        print(
            "Super seeding on"
        )
    else:
        warning(f"qBittorrent doesn't report super seeding as on for {qbittorrent_id}: check it by hand in the WebUI")

    print("qBittorrent checks the files itself when a torrent is added. Run 08-check-seed once it reaches 100%.")

    workflow.complete(
        STEP,
        torrent_id,
        {
            "info_hash": v1_hash,
            "qbittorrent_id": qbittorrent_id,
            "save_path": save_path,
            "content_layout": "NoSubfolder",
            "merged_files": len(plan),
            "super_seeding": super_seeding,
        },
    )
    print(f"Done in {fmt_duration(time.monotonic() - started)}")
    ok(f"Seed OK for {torrent_id}")


def copy_torrent_files(torrents_dir, scratch_dir, torrent_id):
    """
    Copies <ID>.torrent and its .minisig from SCRATCH_DIR into SEED_DIR/.torrents/. If already there (an earlier,
    interrupted run having copied them), they must be byte-identical to what's being copied now, or this fails.
    """
    for name in (f"{torrent_id}.torrent", f"{torrent_id}.torrent.minisig"):
        src = os.path.join(scratch_dir, name)
        dst = os.path.join(torrents_dir, name)
        if not os.path.isfile(src):
            fail(f"Not found: {src}")
        if os.path.exists(dst):
            if not filecmp.cmp(src, dst, shallow=False):
                fail(f"{dst} already exists and differs from {src}: resolve by hand")
            continue
        tmp = dst + ".tmp"
        shutil.copyfile(src, tmp)
        os.rename(tmp, dst)


def check_torrent_identity(torrent_path, torrent_id, expected_v1_hash):
    """The .torrent's name and v1 info-hash match what 05-make-torrent recorded, before anything is merged from it."""
    import libtorrent as lt

    ti = lt.torrent_info(torrent_path)
    if ti.name() != torrent_id:
        fail(f"{torrent_path}: name {ti.name()!r}, expected {torrent_id!r}")
    v1_hash = str(ti.info_hashes().v1)
    if v1_hash != expected_v1_hash:
        fail(f"{torrent_path}: v1 info-hash {v1_hash}, expected {expected_v1_hash} (05-make-torrent's)")


def read_torrent_entries(torrent_path):
    """
    (relative path, size, modification time) for every file in a .torrent (root files and data files alike),
    straight from its raw bencoded info dict: BEP 3's "files" list paths are already relative to the torrent's own
    name/root folder, so there's no prefix left to strip. libtorrent's typed torrent_info doesn't expose a file's
    mtime once loaded (NOTES.md "libtorrent"), hence reading the raw dict instead.
    """
    import libtorrent as lt

    with open(torrent_path, "rb") as f:
        data = f.read()
    info = lt.bdecode(data)[b"info"]
    entries = []
    for file_e in info[b"files"]:
        path = "/".join(part.decode("utf-8") for part in file_e[b"path"])
        entries.append((path, file_e[b"length"], file_e[b"mtime"]))
    return entries


def load_or_build_plan(plan_path, content_dir, seed_dir, entries):
    """
    The merge plan from SCRATCH_DIR/merge-plan.json if an earlier run already built one (trusted as is: by the time
    it exists, files may already have been unlinked from the staging folder and can no longer be stat'd to rebuild
    it), or built fresh from entries and saved there.
    """
    if os.path.isfile(plan_path):
        with open(plan_path, encoding="utf-8") as f:
            return json.load(f)
    try:
        plan = merge.build_plan(content_dir, seed_dir, entries)
    except merge.MergeError as e:
        fail(str(e))
    tmp = plan_path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(plan, f, indent=2, sort_keys=True)
    os.rename(tmp, plan_path)
    return plan


def check_no_subfolder(client, qbittorrent_id, entries):
    """qBittorrent's torrents/files reports exactly entries' relative paths: NoSubfolder actually took."""
    try:
        reported = client.files(qbittorrent_id)
    except QbittorrentError as e:
        fail(f"Can't read the torrent's files back from qBittorrent: {e}")
    reported_names = sorted(f["name"].replace("\\", "/") for f in reported)
    expected_names = sorted(path for path, _size, _mtime in entries)
    if reported_names != expected_names:
        fail(
            "qBittorrent's file names don't match the merged, no-subfolder layout "
            f"({fmt_count(len(reported_names))} reported, {fmt_count(len(expected_names))} expected): if this is a "
            "re-add after lost resume data, remove it and re-add with content layout NoSubfolder, never Original"
        )


def wait_for(client, info_hash):
    deadline = time.monotonic() + WAIT_TIMEOUT
    while time.monotonic() < deadline:
        info = client.info(info_hash)
        if info is not None:
            return info
        time.sleep(WAIT_INTERVAL)
    return None


if __name__ == "__main__":
    main()
