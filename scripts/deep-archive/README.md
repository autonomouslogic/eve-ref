# EVE Ref Deep Archive for EVE Online

Moves old data.everef.net data into torrents. One workflow makes one torrent, `everef-eve-online-deep-archive-NN`
(sequential, from `00`): the oldest data not archived yet, as much as the size limits allow. It's signed, seeded,
published, listed on the docs site, and then deleted from the site.

What each script does in detail: its own docstring. Design constraints and reference material for working on the
scripts: [`NOTES.md`](NOTES.md).

A GitHub Actions workflow (`.github/workflows/deep-archive-reminder.yml`) pings the dev notices Discord on 15 February
and 15 August, the day after that month's rollup check, as a reminder to run a workflow.

## Requirements

- Python 3, rclone, the libtorrent Python bindings **2.x**, sha256sum, and Docker (minisign runs through it):
  ```
  sudo apt-get install python3 rclone python3-libtorrent coreutils docker.io
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
skipped without it, as they are in the Docker fallback used when there's no `python3`. CI installs it, so they all
run there.

## Procedure

Run each step in order, from `scripts/deep-archive/`. The checks are what must hold before moving on; if one doesn't,
or anything else looks wrong, stop.

| Step                                    | Run                                | Writes                                                                        | Bucket |
|-----------------------------------------|------------------------------------|-------------------------------------------------------------------------------|--------|
| 00 Preflight                            | `./00-preflight.py`                |                                                                               | read   |
| 01 Find                                 | `./01-find.py`                     | `find/`                                                                       | read   |
| 02 Review the selection                 | manual                             |                                                                               |        |
| 03 Load                                 | `./03-load.py`                     | `$SEED_DIR/.staging/<ID>/`, `manifest.json`, `<ID>.sha256`                    | read   |
| 04 Sign the checksum file               | `./04-sign-sha256.sh`              | `<ID>.sha256.minisig`                                                         |        |
| 05 Make torrent                         | `./05-make-torrent.py`             | root files in `$SEED_DIR/.staging/<ID>/`, `<ID>.torrent`, `<ID>.txt`          |        |
| 06 Sign the torrent file                | `./06-sign-torrent.sh`             | `<ID>.torrent.minisig`                                                        |        |
| 07 Seed                                 | `./07-seed.py`                     | `$SEED_DIR/.torrents/`, merges `.staging/<ID>/` into `$SEED_DIR`, qBittorrent |        |
| 08 Check seed                           | `./08-check-seed.py`               |                                                                               |        |
| 09 Publish                              | `./09-publish.py`                  | **`deep-archive/`**, `feed-backup/`                                           | write  |
| 10 Update docs                          | manual                             |                                                                               |        |
| 11 Index deep-archive/                  | manual                             | `deep-archive/` index files                                                   | write  |
| _Wait for files to be seeded by others_ |                                    |                                                                               |        |
| 12 Check before delete (UNTESTED)       | `./12-check-before-delete.py`      | `delete-set.json`                                                             | read   |
| 13 Delete (UNTESTED)                    | `./13-delete.py`, then `--execute` | **bucket data (permanent)**, `deleted.jsonl`                                  | write  |
| 14 Check delete (UNTESTED)              | `./14-check-delete.py`             |                                                                               | read   |
| 15 Full DataIndex                       | manual                             |                                                                               |        |
| 16 Check site (UNTESTED)                | `./16-check-site.py`               |                                                                               |        |
| 17 Delete the scratch directory         | manual                             |                                                                               |        |

`check-seed-dir.py` isn't a numbered step: it's a standalone, read-only check of the whole `$SEED_DIR` tree against
every signed torrent (00 and 08 both run its quick checks inline; Appendix C's monthly maintenance runs it with
`--full`).

Paths without a folder are in `$SCRATCH_DIR`.

### 00: Preflight
Checks the settings, directories (creating `$SCRATCH_DIR` if missing, which is all it writes to; `$SEED_DIR` must
exist, warning if it has no `.torrents/`, and failing if `.staging/` exists and isn't empty), tools, bucket, the
fixed trackers' reachability, `LOCAL_COPY_DIR`, qBittorrent, and the merged `$SEED_DIR` tree against its signed
torrents (`check-seed-dir.py`'s quick checks), and picks the next torrent ID. Reports every problem before aborting.

### 01: Find
Selects the oldest files not archived yet (files an earlier torrent holds, published or only in `$SEED_DIR/.torrents/`, are
skipped even though they're still on the site: data is deleted much later), by date, up to the size limits (`01-find.py`'s docstring); `index.json`
files are never selected, only noted for 03-load to snapshot. Aborts if the oldest date alone is too big for one
`.torrent`, if the selection is under `MIN_BYTES` (not enough data yet) -- unless the `.torrent` size limit stopped
the selection early, which can happen well under `MIN_BYTES` with a lot of small files and isn't treated as a
problem -- or if a selected path would collide with one already archived or already on `$SEED_DIR` once merged into one folder tree (NOTES.md "No path in
two torrents"). In each case nothing is written.

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
Copies the selection into `$SEED_DIR/.staging/<ID>/`: what `LOCAL_COPY_DIR` has first, unchecked (a `cp --reflink=always` pass for the files not yet there, which on
BtrFS on the same filesystem costs almost no space or time, then rclone for the rest), then from the
bucket, which replaces any file that doesn't match the bucket's size and modification time. Then hashes it,
downloads a fresh snapshot of the selected folders' `index.json` and tars it into `<ID>-index.tar.xz`, and writes
`manifest.json` and `<ID>.sha256`. Resumable.
- File count and bytes equal the selection; nothing missing, different or extra.
- Every selected file is covered by its folder's downloaded `index.json` (size and modification time match); if not,
  run `DataIndex` for that prefix and load again.
- No archive test failures, or each one accepted (corrupt files are archived as they are).
- `sha256sum -c` passed, index tarball included.

### 04: Sign the checksum file
Signs `<ID>.sha256` with minisign (asks for the key's passphrase) and verifies the signature.

### 05: Make torrent
Checks the staging folder still matches the manifest's size and modification time (cheap; 03-load's own
`sha256sum -c` already checked every byte), then `sha256sum -c`s a random sample of the checksum file as a smoke
test, writes the README into the staging folder, copies in `<ID>.sha256` and its signature (verified first), and
makes `<ID>.torrent` (v1 only, NOTES.md "Open issues"; root files include `<ID>-index.tar.xz`) and `<ID>.txt`.
Trackers are the fixed list in `lib/trackers.txt`; no fetch. Slow: hours for a full torrent, with a progress line
every 30 seconds. Then checks both.
- Read the `.txt` and the README yourself.

### 06: Sign the torrent file
Signs `<ID>.torrent` with minisign and verifies the signature. This one is published.

### 07: Seed
Copies the signed `.torrent` and its signature into `$SEED_DIR/.torrents/`, merges the staging folder into
`$SEED_DIR` itself -- every file hard-linked to its site path directly under `$SEED_DIR`, verified, then the staging
copy removed (NOTES.md "Merged seed directory"; resumable at any point) -- and only then adds the torrent to
qBittorrent, with content layout **NoSubfolder** (not qBittorrent's Original default: the files are already at their
final paths), so it checks them where they now are. Turns on super seeding (NOTES.md "Super seeding"): once other
seeders have turned up, turn it off by hand in the WebUI.

### 08: Check seed
Give the trackers a few minutes after seed first. It fails until a tracker reports working, and while qBittorrent is
checking the torrent; running it again is fine.
- 100% with **0 bytes downloaded** (any download means the files don't match: stop), seeding, a tracker working.
- No file still sits under a `<ID>/` root folder in qBittorrent: confirms the merge and NoSubfolder actually took.
- The whole `$SEED_DIR` tree still matches every signed torrent (`check-seed-dir.py`'s quick checks), not just this
  one.

### 09: Publish
Verifies the torrent's signature, uploads `<ID>.torrent`, `<ID>.txt` and `<ID>.torrent.minisig` to `deep-archive/`,
rebuilds `everef-eve-online-deep-archive.xml`, and checks what the site serves. Uploads without asking, and never
overwrites: if it fails part way, running it again skips files already published identically. The HTTPS checks retry
for a few minutes while Cloudflare serves the old feed, so a real problem takes that long to report. The old feed is
saved in `feed-backup/`.
- The only difference in the feed is the new `<ID>`.
- On the first workflow only: add the feed to a qBittorrent RSS reader and check the item shows up.

### 10: Update docs (manual)
Add the torrent to the "Published Torrents" table on `docs/src/datasets/deep-archive.md`, newest first: the `.torrent`
and `.txt` links, the size and the magnet link, as publish prints them. If the rules in `lib/workflow.py` changed
(`TORRENT_EXCLUDE`, `MIN_AGE_YEARS`, size limits), update the page's rule text too: nothing checks it. Commit it and
deploy the docs site.

### 11: Index deep-archive/ (manual)
From a checkout with production settings in the environment:
```
DATA_INDEX_PREFIX=/deep-archive/ DATA_INDEX_RECURSIVE=false ./gradlew dataIndex
```
- https://data.everef.net/deep-archive/ lists the new torrent, its `.txt` and signature, and the feed.

### 12: Check before delete
**Steps 12 onwards are disabled: untested and probably broken (NOTES.md "Steps 12 onwards").** Don't run them yet.

Checks the docs page lists the torrent, the seeder is clean and the published `.torrent` matches, then builds the
delete set from the torrent against a fresh listing (`12-check-before-delete.py`'s docstring).
- Files that fail are listed and stay on the site (Appendix E). Many failures mean a problem: look before going on.
- Note the delete set: files, bytes, datasets.

### 13: Delete
```
./13-delete.py              # dry run, as often as you like
./13-delete.py --execute    # type the torrent ID to confirm
```
- The dry run shows exactly the delete set from check-before-delete, and rclone's dry run deleting exactly those files.

`--execute` asks, re-checks against a fresh listing, and deletes exactly those files (soft deletes, never by prefix,
each delete only after an rclone dry run of the same list matched it, and capped with `--max-delete`), then the index
files of folders it left empty (NOTES.md "Stale index files"). Treat it as permanent. Until the full `DataIndex`, jobs that crawl the site
(`sync-fuzzwork-ordersets`, the market history scrape) may fail on stale links; they recover on their next run after
it.

### 14: Check delete
- Everything in the delete set is gone, no folder the delete touched holds only index files (others are a warning),
  and every other path that was on the bucket when `01-find` ran is still there.

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
Delete `$SCRATCH_DIR`. The merged `$SEED_DIR` tree stays and keeps seeding.

---

## Appendix A: How the scripts work

- **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` as its last act, only on success. A step
  refuses to start if the previous scripted step's file is missing or its own exists. Manual steps have none.
- **Redoing a step** is deliberate: delete its completion file and every later one by hand. A step that failed wrote no completion file and can simply be run again.
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
| `SEED_DIR` | Every published torrent's files, merged into one tree matching data.everef.net (no torrent's own root folder), kept for seeding. Separate from `SCRATCH_DIR`. |
| `DATA_BUCKET` | The data site's bucket on B2. Its root is the site root. |
| `RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` | The B2 key. |
| `QBITTORRENT_URL`, `QBITTORRENT_API_KEY` | qBittorrent's Web API and its API key. |
| `QBITTORRENT_SAVE_PATH` | Optional. `SEED_DIR` as qBittorrent sees it, if different (qBittorrent in Docker, say). |
| `LOCAL_COPY_DIR` | Optional. A local copy of the data site, laid out like the bucket; `03-load` copies what it has, then the bucket replaces any that don't match. Never trusted. |
| `MINISIGN_KEY_PATH` | The minisign private key, kept outside this repo and `$SCRATCH_DIR`. Its passphrase is always typed in. |

Everything else is fixed in `lib/workflow.py`: size limits, `TORRENT_EXCLUDE`, `TORRENT_INCLUDE`, the site URL, and the rclone remote
(configured only from `RCLONE_CONFIG_*`, never your own `rclone.conf`, with `hard_delete=false`; every other `RCLONE_*`
setting is ignored, and named); and the trackers, `lib/trackers.txt` (not a setting: edit the file to change them).

```
scripts/deep-archive/
  README.md, NOTES.md                  # this process; design notes for working on the scripts
  00-preflight.py … 16-check-site.py   # the steps; 04 and 06 are .sh
  check-seed-dir.py                    # standalone SEED_DIR inventory check, not a numbered step
  lib/                                 # shared code (workflow.py, merge.py, seed_dir.py, trackers.txt, sign.sh, …), the README and comment templates
  tests/

$SCRATCH_DIR/                          # one workflow
  <NN>-<step>.done, NOTES.md (the operator's), log/
  find/ (files.jsonl, indexes.jsonl, …), manifest.json, delete-set.json, deleted.jsonl, feed-backup/
  <ID>.sha256, <ID>.sha256.minisig     # copied into the torrent, never published
  <ID>.torrent, <ID>.txt, <ID>.torrent.minisig   # published
  merge-plan.json                      # 07-seed's merge plan, for resuming

$SEED_DIR/
  <every torrent's files, merged, no root folder>   # laid out like data.everef.net
  <ID>-README.md, <ID>.sha256, <ID>.sha256.minisig, <ID>-index.tar.xz   # each torrent's root files
  .staging/<ID>/                       # a torrent being built (03-load to 06-sign-torrent); empty between workflows
  .torrents/<ID>.torrent, <ID>.torrent.minisig   # a signed copy of every published torrent
```

## Appendix C: Maintenance

`$SEED_DIR` is EVE Ref's only copy of the archived data (no backup: NOTES.md "Durability"). Monthly:

- `./check-seed-dir.py --full`: every file on disk owned by exactly one torrent with no collisions, qBittorrent
  seeding each one cleanly, and `sha256sum -c` passing for every torrent from the `$SEED_DIR` root. If a problem
  turns up, it names which torrent and path; investigate before doing anything else.
- Force a recheck of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding. If one finds
  damage, qBittorrent can re-download it from other seeders; if there are none, those files are lost.
- Check for torrents still in super seeding mode (NOTES.md "Super seeding") with other seeders already on them, and
  turn it off in the WebUI: it's meant for bootstrapping a brand new swarm, not for the life of the seed.

## Appendix D: Rollback

There is no script. Re-upload the deleted files from `$SEED_DIR` (or other seeders) with rclone, which keeps the
modification times, using the published `<ID>.txt` as the list (its paths are already relative to `$SEED_DIR`, with
no torrent root folder to strip): `rclone copy "$SEED_DIR" <remote> --files-from-raw <ID>.txt`. While B2 still holds
the hidden versions, removing the hide markers in B2's web console is quicker (the bucket purges them after about 2
days). Then run a full `DataIndex` and check the files are served.

## Appendix E: If data changed

If `12-check-before-delete` finds a file that changed after it was selected, that file isn't deleted; the rest is.
There is no replacement torrent. The file stays on the site at a path an earlier torrent holds, which `01-find` skips
like any other archived path, so the changed version is never archived. Find out why it changed; if the dataset still
changes after the minimum age, add it to `TORRENT_EXCLUDE`.
