# EVE Ref Deep Archive

Moves old data.everef.net data into torrents. One workflow makes one torrent: the oldest data not archived yet, as
much as the size limits allow, named after the last date it covers (`everef-deep-archive-YYYY-MM-DD`).
It's then seeded, published, backed up, and deleted from the site.

Status: **draft**. Every step 00-18 is numbered and every scripted one is written; none has run against real data yet.
Design and reasoning: [`PLAN.md`](PLAN.md).

## Requirements

- Python 3, **rclone**, the libtorrent Python bindings, **2.x** (1.x can't make hybrid torrents), **aria2c** (the test
  download in 07-check-seed):
  ```
  sudo apt-get install python3 rclone python3-libtorrent aria2
  ```
- A B2 key for the data bucket.
- `SEED_DIR` on redundant storage (e.g. a ZFS mirror or RAID), with room for one more torrent (`MAX_BYTES` in
  `lib/workflow.py`). After deletion it's EVE Ref's only copy of the archived data (Appendix C).
- qBittorrent-nox, for seeding, with its Web API reachable (`QBITTORRENT_URL`).

## Setup

Copy `torrents.env.example` to `torrents.env` and fill in `SCRATCH_DIR`, `SEED_DIR`, `DATA_BUCKET`,
`RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` and the qBittorrent settings (Appendix B).

## Tests

`make test-deep-archive` (from the repo root) runs the unit tests in `tests/`: how `01-find` dates files from their
names, what it skips, how published torrents and `index.json` files are handled, and how it chooses the cutoff.
Standard library only; it uses your `python3`, or Docker if there isn't one.

## Procedure

Run each step in order, from `scripts/deep-archive/`. A step only starts once the previous one has completed, and it
can't run twice (Appendix A). The bullets under a step are what must hold before moving on; if one doesn't, or
anything else looks wrong, stop. Keep notes for the workflow in `$SCRATCH_DIR/NOTES.md`.

Before starting: `$SCRATCH_DIR` is empty, `$SEED_DIR` was scrubbed or checked recently, and the storage migration
isn't in progress (PLAN 2.21).

### 00: Preflight
```
./00-preflight.py
```
Checks the settings, both directories (created if missing; `SCRATCH_DIR` empty, room in `SEED_DIR`), rclone, the B2
key and bucket, libtorrent, the tracker list and aria2c. Reports every problem, then aborts if there were any.

### 01: Find
```
./01-find.py
```
Lists the bucket and selects the oldest files not archived yet, by date (the earliest of the modification date, an
ISO date in the file name, and 31 December of a year in the file name; PLAN 2.2), up to the last date that stays
within the size limits (`lib/workflow.py`), and nothing newer than the minimum age (`01-find.py`). Each folder with a
selected file brings its own `index.json`, whatever that file's date. Prints the cutoff, the torrent ID and a report,
and writes the selection to `$SCRATCH_DIR/find/files.jsonl`.

Aborts if a single date is over a limit: that needs resolving by hand.

### 02: Review the selection (manual)

Read the report (`$SCRATCH_DIR/find/report.txt`):
- No `market-history/`, `killmails/` or `ccp/` files.
- Every flag looked at.
- Every "(no year folder)" dataset is archive data, not a current file that just hasn't changed.
- Files dated by a year in the name (`find.log`): the number is a year, not an ID.
- If the limits weren't reached, a smaller torrent is worth making now.

To change the selection: fix `TORRENT_EXCLUDE` in `lib/workflow.py`, delete `$SCRATCH_DIR/01-find.done`, and run
`01-find` again.

### 03: Announce (manual)

Post the start announcement (PLAN 2.13) with the torrent ID, cutoff date and size.

### 04: Load
```
./04-load.py
```
Downloads exactly the selected files into `$SEED_DIR/<ID>/` with rclone, keeping their modification times, then
checks every one is there with the selected size and modification time, and nothing else is. Then reads every file
once to compute its SHA-256 and test `.bz2`/`.gz` files decompress, and writes `manifest.json`. Resumable: if it fails
or is interrupted, run it again and it downloads only what's missing or different. Aborts before downloading if the
folder holds files that aren't selected, or if `$SEED_DIR` lacks room.
- File count and bytes equal the selection; nothing missing, different or extra.
- No archive test failures, or each one accepted in `NOTES.md` (corrupt files are archived as they are).
- `manifest.json` written, with a SHA-256 for every file.

### 05: Make torrent
```
./05-make-torrent.py
```
Writes `<ID>-README.txt` into `$SEED_DIR/<ID>/`, makes `$SCRATCH_DIR/<ID>.torrent` (hybrid v1+v2),
and generates the file list `$SCRATCH_DIR/<ID>.txt` from it. Then reloads both from disk and checks them:
- Files = the manifest plus `<ID>-README.txt`; bytes match.
- Hybrid, `name` is `<ID>`, not private, no web seed; trackers one per tier; the comment is right.
- The `.txt` matches the torrent and passes `file_list_problems` (`lib/workflow.py`: full site paths,
  sorted, data files only); no `market-history/`, `killmails/` or `ccp/` paths.
- Loads with libtorrent's default limits.
- Copy the v1 and v2 info-hashes into `NOTES.md`.

Also read the `.txt` and the README yourself.

### 06: Seed
```
./06-seed.py
```
Adds the torrent to qBittorrent with save path `$SEED_DIR`.

### 07: Check seed
```
./07-check-seed.py
```
Checks qBittorrent through its Web API, then downloads the README through the torrent with aria2c (from the
`.torrent`, no magnet link) into a throwaway folder. Give the trackers a few minutes after step 06 first.
- 100% with **0 bytes downloaded** (any download means the files don't match: stop), seeding, a tracker working.
- aria2c downloaded the README, identical to the seed copy.

It runs on the workflow host, so it proves the torrent downloads, not that it's reachable from outside (PLAN,
Review notes, R16).

### 08: Publish
```
./08-publish.py
```
Shows the targets and asks first, then uploads `<ID>.torrent` and `<ID>.txt` to the bucket's `deep-archive/`. Never
overwrites (a file already published by an earlier attempt must be identical, and is skipped). Then rebuilds
`feed.xml` and `torrents.json` from the published torrents, shows the difference, asks again, saves the published
`feed.xml` to `$SCRATCH_DIR/feed-backup/feed-<UTC time>.xml`, and uploads both. Last, checks all four over HTTPS,
retrying for a few minutes while Cloudflare still serves the old feed.
- The only difference in the feed and catalogue is the new `<ID>`.
- The `.torrent` and `.txt` are served from `https://data.everef.net/deep-archive/` with the right Content-Type and
  content; the feed is valid RSS and has `<ID>` with the right info-hash, and so does `torrents.json`.

On the first workflow, also add the feed to a qBittorrent RSS reader and check the item shows up.

### 09: Check before delete
```
./09-check-before-delete.py
```
- Docs page live, seeder at 100%.
- The published `.torrent` on the bucket has the same info-hash as `$SCRATCH_DIR/<ID>.torrent`.
- Builds the delete set from the torrent: each data file in it that is in the manifest, and is on the bucket with
  the same size and modification time as in the selection (PLAN 2.14). No `index.json` or README.
- Files that fail are listed and stay on the site (Appendix E). Many failures mean a problem: look before going on.
- Note the printed delete set: files, bytes, datasets.

### 10: Pause crawling jobs (manual)

Disable the scheduled `sync-fuzzwork-ordersets` and the market history scrape until the full `DataIndex` has run.

### 11: Delete
```
./11-delete.py              # dry run
./11-delete.py --execute    # type the torrent ID to confirm
```
- The dry run shows exactly the delete set from step 09.

`--execute` re-checks against a fresh listing, then deletes exactly the confirmed files (soft deletes; never by
prefix). Then it removes `index.html` and `index.json` from every folder the delete left with nothing else in it,
and from parents left the same way, deepest first; never the site root or `deep-archive/` (PLAN 2.15). Treat it as
permanent.

### 12: Check delete
```
./12-check-delete.py
```
- Every file in the delete set is gone, no folder holds only index files, and nothing else changed (files that
  failed the check in step 09 are still there).

### 13: Full DataIndex (manual)
```
docker run --rm --env-file <production env> <eve-ref image> data-index
```
Leave `DATA_INDEX_PREFIX` unset.

### 14: Resume crawling jobs (manual)

Re-enable the jobs paused in step 10.
- The next `sync-fuzzwork-ordersets` run succeeds and uploads nothing below its ID cutoff.

### 15: Check site
```
./15-check-site.py
```
- Every sampled deleted URL returns 404, or 200 from Cloudflare's cache (PLAN 2.16).
- The listings no longer show the deleted files; `deep-archive/` lists the torrent, the `.txt` and the feed.

### 16: Record (manual)

Add a line to `LOG.md`: ID, cutoff, files, bytes, info-hashes, and the published, backed up and deleted dates.
Commit it.

### 17: Announce the result (manual)

Post the finish announcement (PLAN 2.13).

### 18: Delete the scratch directory (manual)

Delete `$SCRATCH_DIR`. `$SEED_DIR/<ID>/` stays and keeps seeding.

---

## Appendix A: How the scripts work

- **One script per scripted step, all steps numbered in order** (`STEPS` in `lib/workflow.py`). Scripts never run the
  next step.
- **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` as the last thing it does, only when it
  succeeded. A step refuses to start if the previous scripted step's file is missing, or if its own already exists. The
  torrent ID is read from `01-find.done`; no step takes arguments for it.
- **Redoing a completed step** is deliberate: delete its completion file and every later one by hand, and note why
  in `NOTES.md`. A step that failed wrote no completion file and can simply be run again; copies and uploads carry
  on where they stopped.
- **Output, like the roll-up scripts:** `# 🚀 <step>: <ID>` once, `## 🔵 <section>` headings, plain lines for
  results, `🔴 <message>` for each problem, and `✅ <step> OK …` as the last line on success. Everything is also
  written to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Show, then ask.** A step that writes outside `$SCRATCH_DIR` and `$SEED_DIR` (the bucket, qBittorrent)
  prints what it will do and asks first.
- **Checks are read-only** and re-derive everything from the source (bucket, disk, qBittorrent, site). They never
  trust the action step's output. `04-load`, `05-make-torrent` and `08-publish` check their own results at the
  end, like the roll-up scripts, instead of having a separate check step.
- **Manual steps** are numbered like the others (`MANUAL_STEPS` in `lib/workflow.py`), but have no script and no
  completion file: a script waits for the nearest earlier scripted step.

## Appendix B: Settings, files and directories

`torrents.env` (from `torrents.env.example`, not committed) holds only what differs per host, plus the secrets. The
scripts read it for anything not already set in the environment.

| Setting | What |
|---|---|
| `SCRATCH_DIR` | Work files for the current workflow. Empty when a workflow starts; deleted by hand at the end. |
| `SEED_DIR` | Torrent content, one folder per torrent, kept for seeding. qBittorrent's save path. |
| `DATA_BUCKET` | The data site's bucket on B2. Its root is the site root. |
| `RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` | The B2 key for the `everef` rclone remote. |
| `QBITTORRENT_URL`, `QBITTORRENT_USERNAME`, `QBITTORRENT_PASSWORD` | qBittorrent's Web API. |

Everything else is fixed in `lib/workflow.py`: the size limits, the excluded datasets (`TORRENT_EXCLUDE`), the site
URL (`DATA_BASE_URL`), and the rclone settings. As in the roll-up scripts, rclone is
configured only from `RCLONE_CONFIG_*` variables (`RCLONE_CONFIG=/dev/null`, so your own `rclone.conf` is never
used), with type `b2` and `hard_delete=false`, so deletes stay soft and the bucket's lifecycle rules give an undo
window. All storage access goes through rclone.

```
scripts/deep-archive/
  README.md  PLAN.md  LOG.md
  torrents.env.example             # copy to torrents.env (not committed)
  trackers.txt                     # trackers added to every torrent, one per tier
  00-preflight.py  01-find.py  …   # the steps
  create_torrent.py                # experiment: measure .torrent sizes; becomes 05-make-torrent
  lib/workflow.py                  # shared: settings, size limits, completion files, output
  lib/torrent_size.py              # .torrent size estimate from paths and sizes, for 01-find's cutoff
  tests/                           # unit tests: make test-deep-archive (from the repo root)

$SCRATCH_DIR/                      # one workflow; deleted by hand at the end
  00-preflight.done, 01-find.done, … # completion files
  find/                            # 01-find: files.jsonl (the selection), listing.jsonl.gz, report.txt, ...
  NOTES.md                     # your notes for this workflow
  manifest.json                    # selection + SHA-256 (04-load)
  <ID>.torrent  <ID>.txt  trackers.txt
  feed-backup/                     # 08-publish: the feed.xml it replaced
  deleted.jsonl
  log/<NN>-<step>-<UTC time>.log   # everything each run of a step printed

$SEED_DIR/                         # kept; qBittorrent save path
  everef-deep-archive-2023-04-11/  # torrent content, exactly what the torrent describes
    everef-deep-archive-2023-04-11-README.txt
    market-orders/history/2023/…
  everef-deep-archive-…/           # earlier torrents, still seeding
```

`SCRATCH_DIR` and `SEED_DIR` must be separate, neither inside the other.

## Appendix C: Maintenance

`$SEED_DIR` is EVE Ref's only copy of the archived data (there is no backup; PLAN 2.11). Check it regularly
(e.g. monthly):
- Force a recheck of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding.
- The filesystem scrub (ZFS/RAID) is clean.

If a recheck finds damage, let qBittorrent re-download the damaged pieces from other seeders. If there are none,
the damaged files are lost.

## Appendix D: Rollback

Re-upload the deleted files from `$SEED_DIR/<ID>/` (or other seeders) into the bucket, using the published `<ID>.txt`
as the list (planned script, outside the numbered steps). Upload with rclone, which keeps the modification times,
so `01-find` still sees them as the archived version. A path can be in several torrents, one version each: restore
from the newest torrent holding it. While B2 still holds
the hidden versions (the bucket's lifecycle rules decide how long), removing the hide markers in B2's web console is
quicker (PLAN 2.14). Then run a full `DataIndex` and check the files are served again.

## Appendix E: If data changed

If `09-check-before-delete` finds a file that changed after it was selected:
- That file isn't deleted; the rest of the delete set is. Note it in `NOTES.md`.
- No replacement torrent: the changed file goes into a later torrent once it's old enough (PLAN Appendix A).
- Find out why. If the dataset still changes after the minimum age, add it to `TORRENT_EXCLUDE`.

## Appendix F: Script reference

| Step | Writes to | Bucket |
|---|---|---|
| `00-preflight` | none | read |
| `01-find` | `$SCRATCH_DIR/find/` | read |
| `04-load` | `$SEED_DIR/<ID>/`, `manifest.json` | read |
| `05-make-torrent` | README in `$SEED_DIR/<ID>/`, `.torrent`, `.txt` | none |
| `06-seed` / `07-check-seed` | qBittorrent / throwaway aria2c download | none |
| `08-publish` | **bucket `deep-archive/`**, `feed-backup/` | write |
| `09-check-before-delete` | none | read |
| `11-delete` / `12-check-delete` | **bucket data (permanent)** / none | write |
| `15-check-site` | none | none |

Every step also writes its completion file and its log in `$SCRATCH_DIR`.
