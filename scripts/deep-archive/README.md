# EVE Ref Deep Archive for EVE Online

Moves old data.everef.net data into torrents. One workflow makes one torrent, `everef-eve-online-deep-archive-NN`
(sequential, from `00`): the oldest data not archived yet, as much as the size limits allow. It's signed, seeded,
published, listed on the docs site, and then deleted from the site.

What each script does in detail: its own docstring. Design constraints and reference material for working on the
scripts: [`NOTES.md`](NOTES.md).

A GitHub Actions workflow (`.github/workflows/deep-archive-reminder.yml`) pings the dev notices Discord on 15 February
and 15 August, the day after that month's rollup check, as a reminder to run a workflow.

## Requirements

- Python 3, rclone, the libtorrent Python bindings **2.x**, aria2c, sha256sum, and Docker (minisign runs through it):
  ```
  sudo apt-get install python3 rclone python3-libtorrent aria2 coreutils docker.io
  ```
- A B2 key for the data bucket.
- `SEED_DIR` on redundant storage (ZFS mirror, RAID), with room for one more torrent (`MAX_BYTES`). After deletion
  it's EVE Ref's only copy of the archived data (Appendix C).
- qBittorrent-nox 5.2 or later, with its Web API reachable and an API key from its Web UI settings.
- The minisign private key, kept outside this repo.

## Setup

Copy `torrents.env.example` to `torrents.env` and fill it in (Appendix B).

Before the first delete of `fuzzwork/` data, the `SyncFuzzworkOrdersets` ID cutoff (NOTES.md "Fuzzwork sync cutoff")
must be deployed, or the sync re-uploads the deleted ordersets.

## Tests

`make test-deep-archive` (from the repo root) runs `tests/`, standard library only. The tests that need libtorrent are
skipped without it, as they are in the Docker fallback used when there's no `python3`.

## Procedure

Run each step in order, from `scripts/deep-archive/`. The checks are what must hold before moving on; if one doesn't,
or anything else looks wrong, stop. Keep notes in `$SCRATCH_DIR/NOTES.md`.

| Step | Run | Writes | Bucket |
|---|---|---|---|
| 00 Preflight | `./00-preflight.py` | | read |
| 01 Find | `./01-find.py` | `find/` | read |
| 02 Review the selection | manual | | |
| 03 Load | `./03-load.py` | `$SEED_DIR/<ID>/`, `manifest.json`, `<ID>.sha256` | read |
| 04 Sign the checksum file | `./04-sign-sha256.sh` | `<ID>.sha256.minisig` | |
| 05 Make torrent | `./05-make-torrent.py` | root files in `$SEED_DIR/<ID>/`, `<ID>.torrent`, `<ID>.txt` | |
| 06 Sign the torrent file | `./06-sign-torrent.sh` | `<ID>.torrent.minisig` | |
| 07 Seed | `./07-seed.py` | qBittorrent | |
| 08 Check seed | `./08-check-seed.py` | | |
| 09 Publish | `./09-publish.py` | **`deep-archive/`**, `feed-backup/` | write |
| 10 Update docs | manual | | |
| 11 Index deep-archive/ | manual | `deep-archive/` index files | write |
| 12 Check before delete | `./12-check-before-delete.py` | `delete-set.json` | read |
| 13 Delete | `./13-delete.py`, then `--execute` | **bucket data (permanent)**, `deleted.jsonl` | write |
| 14 Check delete | `./14-check-delete.py` | | read |
| 15 Full DataIndex | manual | | |
| 16 Check site | `./16-check-site.py` | | |
| 17 Delete the scratch directory | manual | | |

Paths without a folder are in `$SCRATCH_DIR`.

### 00: Preflight
Checks the settings, directories, tools, bucket, tracker lists, `LOCAL_COPY_DIR` and qBittorrent, and picks the next
torrent ID. Reports every problem before aborting.

### 01: Find
Selects the oldest files not archived yet, by date, up to the size limits (NOTES.md "Selection"). Aborts if the
oldest date alone is too big for one `.torrent`, if the selection is under `MIN_BYTES` (not enough data yet), or if a
path a published `<ID>.txt` lists is still on the site (Appendix E). In each case nothing is written.

### 02: Review the selection (manual)
Read `$SCRATCH_DIR/find/report.txt`:
- No `market-history/`, `killmails/` or `ccp/` files.
- Every flag looked at.
- Every "(no year folder)" dataset is archive data, not a current file that just hasn't changed (a reference file
  tools fetch by URL must stay: add it to `TORRENT_EXCLUDE`).
- Files dated by a year in the name (`find.log`): the number is a year, not an ID.
- If the limits weren't reached, a smaller torrent is worth making now.

To change the selection: fix `TORRENT_EXCLUDE` (or `TORRENT_INCLUDE`) in `lib/workflow.py`, delete
`$SCRATCH_DIR/01-find.done`, and run `01-find` again.

`TORRENT_INCLUDE` is optional (`None` by default). Set to a list of folders, only files in those folders are selected
(`TORRENT_EXCLUDE` still applies), and the report lists the rest as "not included". Use it to give the first torrents
only the biggest datasets; set it back to `None` once they're archived.

### 03: Load
Copies the selection into `$SEED_DIR/<ID>/`: what `LOCAL_COPY_DIR` has first, unchecked, then from the bucket with
`--checksum`, which replaces any file that doesn't match the bucket's SHA-1. Then hashes it, and writes
`manifest.json` and `<ID>.sha256`. Resumable.
- File count and bytes equal the selection; nothing missing, different or extra (changed `index.json` files are
  ignored: DataIndex rewrites them).
- No archive test failures, or each one accepted in `$SCRATCH_DIR/NOTES.md` (corrupt files are archived as they are).
- `sha256sum -c` passed.

### 04: Sign the checksum file
Signs `<ID>.sha256` with minisign (asks for the key's passphrase) and verifies the signature.

### 05: Make torrent
Writes the README into the content folder, copies in `<ID>.sha256` and its signature (verified first), and makes
`<ID>.torrent` (hybrid v1+v2) and `<ID>.txt`. Slow: hours for a full torrent. Then checks both.
- The trackers in `$SCRATCH_DIR/trackers/trackers.txt` look sane (no surprising domains).
- Read the `.txt` and the README yourself.
- Copy the v1 and v2 info-hashes into `$SCRATCH_DIR/NOTES.md`.

### 06: Sign the torrent file
Signs `<ID>.torrent` with minisign and verifies the signature. This one is published.

### 07: Seed
Adds the torrent to qBittorrent, which checks the files where they are.

### 08: Check seed
Give the trackers a few minutes after seed first. It fails until a tracker reports working, and while the recheck
runs; running it again is fine.
- 100% with **0 bytes downloaded** (any download means the files don't match: stop), seeding, a tracker working.
- aria2c downloaded the README through the torrent, identical to the seed copy.

### 09: Publish
Verifies the torrent's signature, uploads `<ID>.torrent`, `<ID>.txt` and `<ID>.torrent.minisig` to `deep-archive/`,
rebuilds `everef-eve-online-deep-archive.xml`, and checks what the site serves. Uploads without asking, and never
overwrites: if it fails part way, running it again skips files already published identically. The HTTPS checks retry
for a few minutes while Cloudflare serves the old feed, so a real problem takes that long to report. The old feed is
saved in `feed-backup/`.
- The only difference in the feed is the new `<ID>`.
- On the first workflow only: add the feed to a qBittorrent RSS reader and check the item shows up.

### 10: Update docs (manual)
Add the torrent to the "Torrents" table on `docs/src/datasets/deep-archive.md`, newest first: its ID linked to its URL,
the v1 and v2 info-hashes, and the magnet link, all printed by publish. If the rules in `lib/workflow.py` changed
(`TORRENT_EXCLUDE`, `MIN_AGE_YEARS`, size limits), update the page's rule text too: nothing checks it. Commit it and
deploy the docs site.

### 11: Index deep-archive/ (manual)
From a checkout with production settings in the environment:
```
DATA_INDEX_PREFIX=/deep-archive/ DATA_INDEX_RECURSIVE=false ./gradlew dataIndex
```
- https://data.everef.net/deep-archive/ lists the new torrent, its `.txt` and signature, and the feed.

### 12: Check before delete
Checks the docs page lists the torrent, the seeder is clean and the published `.torrent` matches, then builds the
delete set from the torrent against a fresh listing (NOTES.md "Deletion").
- Files that fail are listed and stay on the site (Appendix E). Many failures mean a problem: look before going on.
- Note the delete set: files, bytes, datasets.

### 13: Delete
```
./13-delete.py              # dry run, as often as you like
./13-delete.py --execute    # type the torrent ID to confirm
```
- The dry run shows exactly the delete set from check-before-delete.

`--execute` deletes exactly those files (soft deletes, never by prefix), then the index files of folders it left
empty (NOTES.md "Stale index files"). Treat it as permanent. Until the full `DataIndex`, jobs that crawl the site
(`sync-fuzzwork-ordersets`, the market history scrape) may fail on stale links; they recover on their next run after
it.

### 14: Check delete
- Everything in the delete set is gone, no folder holds only index files, and every other path that was on the
  bucket when `01-find` ran is still there.

### 15: Full DataIndex (manual)
```
docker run --rm --env-file <production env> <eve-ref image> data-index
```
Or `./gradlew dataIndex` from a checkout with production settings in the environment. Leave `DATA_INDEX_PREFIX` unset.

### 16: Check site
- Sampled deleted URLs return 404, or 200 from Cloudflare's cache (up to 30 days).
- The listings no longer show the deleted files; `deep-archive/` lists the torrent, the `.txt`, the signature and the
  feed.

### 17: Delete the scratch directory (manual)
Delete `$SCRATCH_DIR`. `$SEED_DIR/<ID>/` stays and keeps seeding.

---

## Appendix A: How the scripts work

- **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` as its last act, only on success. A step
  refuses to start if the previous scripted step's file is missing or its own exists. Manual steps have none.
- **Redoing a step** is deliberate: delete its completion file and every later one by hand, and note why in
  `$SCRATCH_DIR/NOTES.md`. A step that failed wrote no completion file and can simply be run again.
- **One step at a time.** Every step holds `$SCRATCH_DIR/log/.lock` while it runs.
- **Output**, like the roll-up scripts: `🚀` once, `🔵` sections, `🟡` warnings (the step carries on), `🔴`
  errors (the step stops), `✅` last on success. Everything is also
  logged to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Show, then ask** before deleting from the bucket (`13-delete`). Other writes (`07-seed`, `09-publish`) show
  what they do and go ahead.
- **Checks re-derive everything** from the bucket, disk, qBittorrent or site. Signatures are verified again before
  each use.
- The two signing steps are bash (`lib/sign.sh`), with the same rules.

## Appendix B: Settings and files

`torrents.env` (from `torrents.env.example`, not committed) is read for anything the environment doesn't set; every
step names the settings the environment overrides.

| Setting | What |
|---|---|
| `SCRATCH_DIR` | Work files for the current workflow. Empty when it starts; deleted by hand at the end. |
| `SEED_DIR` | Torrent content, one folder per torrent, kept for seeding. Separate from `SCRATCH_DIR`. |
| `DATA_BUCKET` | The data site's bucket on B2. Its root is the site root. |
| `RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` | The B2 key. |
| `QBITTORRENT_URL`, `QBITTORRENT_API_KEY` | qBittorrent's Web API and its API key. |
| `QBITTORRENT_SAVE_PATH` | Optional. `SEED_DIR` as qBittorrent sees it, if different (qBittorrent in Docker, say). |
| `LOCAL_COPY_DIR` | Optional. A local copy of the data site, laid out like the bucket; `03-load` copies what it has, then the bucket replaces any that don't match. Never trusted. |
| `MINISIGN_KEY_PATH` | The minisign private key, kept outside this repo and `$SCRATCH_DIR`. Its passphrase is always typed in. |

Everything else is fixed in `lib/workflow.py`: size limits, `TORRENT_EXCLUDE`, `TORRENT_INCLUDE`, the site URL, and the rclone remote
(configured only from `RCLONE_CONFIG_*`, never your own `rclone.conf`, with `hard_delete=false`).

```
scripts/deep-archive/
  README.md, NOTES.md                  # this process; design notes for working on the scripts
  00-preflight.py … 16-check-site.py   # the steps; 04 and 06 are .sh
  lib/                                 # shared code (workflow.py, sign.sh, …) and the README and comment templates
  tests/

$SCRATCH_DIR/                          # one workflow
  <NN>-<step>.done, NOTES.md (the operator's), log/
  find/, manifest.json, delete-set.json, deleted.jsonl, feed-backup/, trackers/
  <ID>.sha256, <ID>.sha256.minisig     # copied into the torrent, never published
  <ID>.torrent, <ID>.txt, <ID>.torrent.minisig   # published

$SEED_DIR/
  everef-eve-online-deep-archive-NN/   # one per torrent, still seeding
```

## Appendix C: Maintenance

`$SEED_DIR` is EVE Ref's only copy of the archived data (no backup: NOTES.md "Durability"). Monthly, force a recheck
of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding. If one finds damage, qBittorrent can
re-download it from other seeders; if there are none, those files are lost.

## Appendix D: Rollback

There is no script. Re-upload the deleted files from `$SEED_DIR/<ID>/` (or other seeders) with rclone, which keeps the
modification times, using the published `<ID>.txt` as the list. While B2 still holds the hidden versions, removing the
hide markers in B2's web console is quicker (the bucket purges them after about 2 days). Then run a full `DataIndex` and check the files are served.

## Appendix E: If data changed

If `12-check-before-delete` finds a file that changed after it was selected, that file isn't deleted; the rest is.
Note it in `$SCRATCH_DIR/NOTES.md`. There is no replacement torrent. The file stays on the site at a path the published `<ID>.txt`
lists, so the next `01-find` stops with a hard error on it, as on any file check-before-delete leaves on the site.
That isn't handled: resolve it by hand before running `01-find` again. Find out why it changed; if the dataset still
changes after the minimum age, add it to `TORRENT_EXCLUDE`.
