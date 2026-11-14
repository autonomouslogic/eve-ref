# EVE Ref Deep Archive for EVE Online

Archives data.everef.net data into torrents. One workflow makes one torrent, `everef-eve-online-deep-archive-NN`
(sequential, from `00`): the oldest data not archived yet, as much as the size limits allow. It's signed, seeded,
published and listed on the docs site. Archiving doesn't remove anything from the site: removing old, archived data is a
separate process, the purge ([Purge](#purge)).

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
- `SEED_DIR` on redundant storage (ZFS mirror, RAID), with room for one more torrent (`MAX_BYTES`). Once data is
  deleted from the site, it's EVE Ref's only copy of it (Appendix C).
- qBittorrent-nox 5.2 or later, with its Web API reachable and an API key from its Web UI settings.
- The minisign private key, kept outside this repo.

## Setup

Copy `torrents.env.example` to `torrents.env` and fill it in (Appendix B).

## Tests

`make test-deep-archive` (from the repo root) runs `tests/`, standard library only. The tests that need libtorrent are
skipped without it, as they are in the Docker fallback used when there's no `python3`. CI installs it, so they all
run there.

## Procedure

Run each step in order, from `scripts/deep-archive/`. The checks are what must hold before moving on; if one doesn't,
or anything else looks wrong, stop.

| Step                                                  | Run                                | Writes                                                                        | Bucket |
|-------------------------------------------------------|------------------------------------|-------------------------------------------------------------------------------|--------|
| 00 Preflight                                          | `./00-preflight.py`                |                                                                               | read   |
| 01 Find                                               | `./01-find.py`                     | `find/`                                                                       | read   |
| 02 Review the selection                               | manual                             |                                                                               |        |
| 03 Load                                               | `./03-load.py`                     | `$SEED_DIR/.staging/<ID>/`, `manifest.json`, `<ID>.sha256`                    | read   |
| 04 Sign the checksum file                             | `./04-sign-sha256.sh`              | `<ID>.sha256.minisig`                                                         |        |
| 05 Make torrent                                       | `./05-make-torrent.py`             | root files in `$SEED_DIR/.staging/<ID>/`, `<ID>.torrent`, `<ID>.txt`          |        |
| 06 Sign the torrent file                              | `./06-sign-torrent.sh`             | `<ID>.torrent.minisig`                                                        |        |
| 07 Seed                                               | `./07-seed.py`                     | `$SEED_DIR/.torrents/`, merges `.staging/<ID>/` into `$SEED_DIR`, qBittorrent |        |
| 08 Check seed                                         | `./08-check-seed.py`               |                                                                               |        |
| 09 Publish                                            | `./09-publish.py`                  | **`deep-archive/`**, `feed-backup/`                                           | write  |
| 10 Update docs                                        | manual                             |                                                                               |        |
| 11 Index deep-archive/                                | manual                             | `deep-archive/` index files                                                   | write  |
| 12 Delete the scratch directory                       | manual                             |                                                                               |        |

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
skipped even though they're still on the site: archiving doesn't delete), by date, up to the size limits (`01-find.py`'s docstring); `index.json`
files are never selected, only noted for 03-load to snapshot. Aborts if the oldest date alone is too big for one
`.torrent`, if the selection is under `MIN_BYTES` (not enough data yet) -- unless the `.torrent` size limit stopped
the selection early, which can happen well under `MIN_BYTES` with a lot of small files and isn't treated as a
problem -- or if a selected path would collide with one already archived or already on `$SEED_DIR` once merged into one folder tree (NOTES.md "No path in
two torrents"). In each case nothing is written.

### 02: Review the selection (manual)
Read `$SCRATCH_DIR/find/report.txt`:
- No `market-history/`, `killmails/` or `wars/history/` files.
- No daily files under `<dataset>/history/<year>/`, except for the datasets in `TORRENT_INCLUDE`.
- Every flag looked at, the history folders one especially. The "no rollups yet" list is expected for datasets too
  new to be rolled up: nothing of theirs is archived until they are.
- Every "(no year folder)" dataset is archive data, not a current file that just hasn't changed (a reference file
  tools fetch by URL must stay: add it to `TORRENT_EXCLUDE`). Yearly rollups (`<dataset>/history/<dataset>-<year>.*`)
  show up here; that's expected.
- Files dated by a year in the name (`find.log`): the number is a year, not an ID.
- If the limits weren't reached, a smaller torrent is worth making now.

To change the selection: fix `TORRENT_EXCLUDE` or `TORRENT_INCLUDE` in `lib/workflow.py`, delete
`$SCRATCH_DIR/01-find.done`, and run `01-find` again.

Both are lists of glob patterns from the site root. A file is excluded if a `TORRENT_EXCLUDE` pattern matches it and
no `TORRENT_INCLUDE` pattern does: `TORRENT_INCLUDE` only holds exceptions to `TORRENT_EXCLUDE`, it never limits the
selection. A pattern matches a path or any of its parent folders, segment by segment: `*`, `?` and `[...]` stay
within one folder name, `**` matches any number of folders. So `killmails` covers everything under `killmails/`,
and `*/history/[0-9][0-9][0-9][0-9]` every dataset's year folders under `history/`.

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
final paths), so it checks them where they now are. Does not turn on super seeding (NOTES.md "Super seeding").

### 08: Check seed
Give the trackers a few minutes after seed first. It fails until a tracker reports working; running it again is fine.
While qBittorrent is still checking the torrent (hundreds of GiB), it waits, printing the state and progress
qBittorrent reports once a minute.
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
Add the torrent to the "Published Torrents" table on `docs/src/datasets/deep-archive.md`, newest last: the `.torrent`
and `.txt` links, the size and the magnet link, as publish prints them, and a Seeders cell of
`<TorrentSeeders hash="<v1 info-hash>"></TorrentSeeders>`. Live counts come from the API's
`/v1/deep-archive/seeders`, which reads the feed, so the new torrent shows up there within its 4-hour cache. If the rules in `lib/workflow.py` changed
(`TORRENT_EXCLUDE`, `TORRENT_INCLUDE`, `MIN_AGE_DAYS`, size limits), update the page's rule text too: nothing checks it. Commit it and
deploy the docs site.

Include the torrage.info link as a backup.

### 11: Index deep-archive/ (manual)
From a checkout with production settings in the environment:
```
DATA_INDEX_PREFIX=/deep-archive/ DATA_INDEX_RECURSIVE=false ./gradlew dataIndex
```
- https://data.everef.net/deep-archive/ lists the new torrent, its `.txt` and signature, and the feed.

### 12: Delete the scratch directory (manual)
Delete `$SCRATCH_DIR`. The merged `$SEED_DIR` tree stays and keeps seeding.

## Purge

Deletes archived data from data.everef.net, keeping this year's and the previous year's: run in 2026, everything
dated 2026 or 2025 stays. A separate process from making torrents, run whenever, from `scripts/deep-archive/`, with its
own steps, numbered with a `d`, and its own work folder, `$PURGE_DIR`. The same rules as the torrent steps apply
(Appendix A). **Once a file is deleted, `$SEED_DIR` and the other seeders are the only copy of it** (Appendix C), and
B2's undo window is about 2 days (NOTES.md "Deletion"): the checks are what stands between a bug and lost data.

Only files in a signed torrent are ever deleted, and every script re-derives what may go from fresh data
(the torrents, a new listing of the site, `$SEED_DIR`) rather than trusting what an earlier one wrote. A file is
deleted only if all of these hold (`lib/purge.py`, `blockers`):
- it's a data file of a signed torrent in `$SEED_DIR/.torrents/`: never a root file, an `index.json`/`index.html`
  or anything under `deep-archive/`, and in exactly one torrent;
- the site has it with the size and modification time the torrent records: the archived version, not a newer
  upload;
- it isn't under `ccp/csm`, `ccp/mer`, `market-history/` or `killmails/` (`NEVER_PURGE`);
- its date, worked out by the code `01-find` selects by (`lib/dating.py`: the earliest of the modification time and any
  date or year in the file name) is before 1 January of the previous year;
- `$SEED_DIR` holds a regular file at its path, with the torrent's size and modification time.

Before the first purge, check the Fuzzwork sync's ID floor is deployed (NOTES.md "Fuzzwork sync cutoff"; the code is
in `SyncFuzzworkOrdersets`), or the sync would upload deleted ordersets again.

| Step                         | Run                       | Writes                                                      | Bucket |
|------------------------------|---------------------------|-------------------------------------------------------------|--------|
| d00 Preflight                | `./d00-preflight.py`      |                                                             | read   |
| d01 Find                     | `./d01-find.py`           | `$PURGE_DIR/find/`                                          | read   |
| d02 Review the list          | manual                    |                                                             |        |
| d03 Dry run                  | `./d03-dryrun.py`         | `$PURGE_DIR/dryrun/`                                        | read   |
| d04 Delete                   | `./d04-delete.py`         | `deleted.jsonl`                                             | **delete** |
| d05 Index the site           | manual                    | the site's `index.json` and `index.html` files              | write  |

A step refuses to start unless the one before it completed (d03 needs d01; d02 has no completion file), and
refuses to run twice. Set `PURGE_DIR` in `torrents.env` (Appendix B) to a new, empty folder, apart from `$SEED_DIR` and
`$SCRATCH_DIR`.

### d00: Preflight
Checks the settings, tools, `$PURGE_DIR` (created if missing, must be empty), that `$SEED_DIR` matches its signed
torrents (every file there, with the size and modification time its torrent records), that qBittorrent seeds every
torrent cleanly (100%, 0 bytes downloaded), that every torrent is published (its `.torrent` on the site is the signed
one, its `.txt` is exactly its data files), and that the docs page lists every torrent. Reports every problem
before aborting.

Run `./d00-preflight.py --full` as well the first time, and before any big purge: it also runs `sha256sum -c` of every
torrent from `$SEED_DIR`, reading every byte, which takes hours. Nothing is deleted without a good copy in `$SEED_DIR`.

### d01: Find
Lists the whole site and the contents of every signed torrent, and works out the files to delete by the rules above.
Nothing is written to the bucket. Aborts, writing nothing, if `$SEED_DIR` doesn't match its torrents, a published torrent
isn't the signed one, or an old file has no matching copy in `$SEED_DIR`. Writes `find/delete.txt`, the paths to delete,
and `find/report.txt`.

### d02: Review the list (manual)
Read `$PURGE_DIR/find/report.txt` and `find/delete.txt`:
- The dates run up to the end of the year before last, and no further. The newest date matches the rule.
- The datasets are the ones expected. Nothing looks like a current file tools fetch by URL (add one to
  `TORRENT_EXCLUDE`, or `NEVER_PURGE` in `lib/purge.py`, and run again).
- "Not the archived version" (a file re-uploaded since it was archived) and anything else flagged under "staying"
  has been looked at.
- The total is what's expected.

To delete less, remove lines from `find/delete.txt`. Adding a line only makes d03 fail: it checks every path again.

### d03: Dry run
Checks every path in the reviewed `delete.txt` from scratch against the signed torrents, a fresh listing of the site and
`$SEED_DIR`, by the same rules. A path that's blocked now, or dated in a kept year (the year may have turned since d01),
fails the step; one already gone is dropped. Then `rclone delete --dry-run --files-from-raw` of the rest, and compares
rclone's own log of what it would delete with the list: exactly the same, none missing, nothing else. Writes
`dryrun/verified.txt`, the list d04 deletes, and its SHA-256.

### d04: Delete
Checks `dryrun/verified.txt` is the file d03 wrote, verifies every path again, runs the dry run again, then asks
for the number of files typed back. After that, it verifies a third time against a new listing (the answer can come
much later), and deletes with `rclone delete --files-from-raw`, an explicit list, never a prefix or a pattern, with
`--max-delete` set to its length so rclone itself refuses to delete more. Then lists the site once more: every deleted
path must be gone and every other file that was there must still be.
Running it again after a failure finishes the job: files already gone are skipped.
- Folders left holding nothing but index files are listed in `index-only-folders.txt` and as warnings. They're not
  deleted: only files in a torrent ever are.

### d05: Index the site (manual)
`DataIndex` doesn't remove a folder's old index files when it no longer lists it, and its parents still link the
deleted files until it runs (NOTES.md "Stale index files"). From a checkout with production settings in the environment:
```
./gradlew dataIndex
```
Leave `DATA_INDEX_PREFIX` unset, so the whole site is indexed. Then check data.everef.net: a sample of the deleted URLs is a 404 or, for up to 30 days, still cached by Cloudflare
(NOTES.md "Cloudflare cache"; no purge), the folders in `index-only-folders.txt` no longer list deleted files, and
`deep-archive/` still lists every torrent. `crawling` jobs (`sync-fuzzwork-ordersets`, the market history scrape) may
fail between the delete and the index run, and recover on their next run (NOTES.md "Code that depends on deleted
data"). Then delete `$PURGE_DIR`.

---

## Appendix A: How the scripts work

- **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` (the purge's: `$PURGE_DIR/d<NN>-<step>.done`) as its last act, only on success. A step
  refuses to start if the previous scripted step's file is missing or its own exists. Manual steps have none.
- **Redoing a step** is deliberate: delete its completion file and every later one by hand. A step that failed wrote no completion file and can simply be run again.
- **One step at a time.** Every step holds `$SCRATCH_DIR/log/.lock` (the purge's: `$PURGE_DIR/log/.lock`) while it runs.
- **Output**, like the roll-up scripts: `🚀` once, `🔵` sections, `🟡` warnings (the step carries on), `🔴`
  errors (the step stops), `✅` last on success. Everything is also
  logged to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Show, then go ahead.** The writes (`07-seed`, `09-publish`) show what they do, then do it without asking.
- **Checks re-derive everything** from the bucket, disk, qBittorrent or site. Signatures are verified again before
  each use.
- The two signing steps are bash (`lib/sign.sh`), with the same rules.

## Appendix B: Settings and files

`torrents.env` (from `torrents.env.example`, not committed) is read for anything the environment doesn't set; every
step names the settings the environment overrides.

| Setting | What |
|---|---|
| `SCRATCH_DIR` | Work files for the current workflow. Empty when it starts; deleted by hand at the end. |
| `PURGE_DIR` | Work files for a purge (`d00` to `d04`). Empty when it starts; deleted by hand at the end. Separate from `SEED_DIR`, and a different folder from `SCRATCH_DIR`. |
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
  00-preflight.py … 09-publish.py      # the steps; 04 and 06 are .sh
  d00-preflight.py … d04-delete.py     # the purge steps (d02 is manual)
  check-seed-dir.py                    # standalone SEED_DIR inventory check, not a numbered step
  lib/                                 # shared code (workflow.py, purge.py, dating.py, merge.py, seed_dir.py, trackers.txt, sign.sh, …), the README and comment templates
  tests/

$SCRATCH_DIR/                          # one workflow
  <NN>-<step>.done, NOTES.md (the operator's), log/
  find/ (files.jsonl, indexes.jsonl, …), manifest.json, feed-backup/
  <ID>.sha256, <ID>.sha256.minisig     # copied into the torrent, never published
  <ID>.torrent, <ID>.txt, <ID>.torrent.minisig   # published
  merge-plan.json                      # 07-seed's merge plan, for resuming

$PURGE_DIR/                            # one purge
  d<NN>-<step>.done, log/
  find/ (delete.txt, delete.jsonl, report.txt, summary.json, listing.jsonl.gz)
  dryrun/ (verified.txt, verified.jsonl)
  deleted.jsonl, index-only-folders.txt

$SEED_DIR/
  <every torrent's files, merged, no root folder>   # laid out like data.everef.net
  <ID>-README.md, <ID>.sha256, <ID>.sha256.minisig, <ID>-index.tar.xz   # each torrent's root files
  .staging/<ID>/                       # a torrent being built (03-load to 06-sign-torrent); empty between workflows
  .torrents/<ID>.torrent, <ID>.torrent.minisig   # a signed copy of every published torrent
```

## Appendix C: Maintenance

`$SEED_DIR` is EVE Ref's only copy of any archived data later deleted from the site (no backup: NOTES.md
"Durability"). Monthly:

- `./check-seed-dir.py --full`: every file on disk owned by exactly one torrent with no collisions, qBittorrent
  seeding each one cleanly, and `sha256sum -c` passing for every torrent from the `$SEED_DIR` root. If a problem
  turns up, it names which torrent and path; investigate before doing anything else.
- Force a recheck of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding. If one finds
  damage, qBittorrent can re-download it from other seeders; if there are none, those files are lost.
