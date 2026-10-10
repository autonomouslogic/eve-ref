# deep-archive/ — EVE Ref Deep Archive

Tooling and operator process for the **EVE Ref Deep Archive**: old data.everef.net data moved into torrents. Each
torrent holds the oldest data not archived yet, as much of it as the size limits allow, and is named after the last
modification date it covers (`everef-deep-archive-YYYY-MM-DD`).

Status: **draft**. Implemented: `00-preflight`, `01-find`. `create_torrent.py` is an experiment that later becomes
`04-make-torrent`. Every other step below is designed but not written yet. Design and reasoning: [`PLAN.md`](PLAN.md).

## What a workflow does

One workflow makes **one torrent**, start to finish. Workflows run ad hoc, one at a time, by hand:

1. Check the setup (`00-preflight`).
2. Select the files (`01-find`): list the data bucket, and take the oldest files not archived yet, by modification
   date, up to the last date that keeps the torrent within the hard-coded file count and size limits. Nothing modified
   in the last 2 years, never `market-history` or `killmails`, never `index.html`.
3. Announce it on Discord.
4. Download the selection from the bucket into `$SEED_DIR/<ID>/`.
5. Make `<ID>.torrent` (hybrid v1+v2), with checksums (`<ID>.sha256`) and a README with verification instructions
   inside it, plus a file list `<ID>.txt`.
6. Seed it and test it.
7. Upload the torrent and file list to `data.everef.net/deep-archive/`, and rebuild the RSS feed there.
8. Check the docs site is accurate.
9. Back it up to S3 Glacier Deep Archive.
10. Last: delete the selected files from data.everef.net, run a full `DataIndex`, check the site, announce the result.
11. Delete `$SCRATCH_DIR` by hand. The content stays in `$SEED_DIR/<ID>/` and keeps seeding.

Every action has a separate check, and no step starts until the previous one has completed.

## Files here

```
deep-archive/
  README.md              # this file: setup and the step-by-step process
  PLAN.md                # design decisions and reasoning; goes away once the process is settled
  LOG.md                 # one line per published torrent (created with the first one)
  torrents.env.example   # copy to torrents.env (not committed)
  trackers.txt           # trackers added to every torrent, one per tier
  00-preflight.py        # the workflow's steps, numbered in order
  01-find.py
  create_torrent.py      # experiment: measure .torrent sizes
  lib/
    workflow.py          # shared: settings, size limits, completion files
```

## Setup

### Requirements

Python 3, **rclone**, and the libtorrent Python bindings, **2.x** (1.x can't make hybrid torrents):
```
sudo apt-get install python3 rclone python3-libtorrent
python3 -c "import libtorrent; print(libtorrent.__version__)"
```
All storage access goes through rclone, never the AWS CLI or an S3 library.
A Docker image and a `run.sh` wrapper are planned (PLAN 2.9); until then the scripts run directly on the host.

### Settings: `torrents.env`

Copy `torrents.env.example` to `torrents.env` and fill it in. The scripts read it for anything not already set in the
environment, so a setting on the command line wins.

| Setting | What |
|---|---|
| `SCRATCH_DIR` | Work files for the current workflow. Must be **empty** when a workflow starts. Delete it by hand once the workflow is complete. |
| `SEED_DIR` | Torrent content, one folder per torrent: `$SEED_DIR/<ID>/`. Kept for seeding; qBittorrent's save path. Needs room for one more torrent (up to 1 TiB). |
| `RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` | The B2 key for the `everef` rclone remote. |

Everything else is fixed in `lib/workflow.py`: the bucket, the rclone settings, the size limits, the excluded
datasets (`TORRENT_EXCLUDE`: `market-history`, `killmails`) and the site URL (`DATA_BASE_URL`).

**rclone.** One B2 key, one remote, `everef`. Only the account and key are secrets, so only those come from
`torrents.env`. Everything else is fixed in `lib/workflow.py`, the same way as in the roll-up scripts:
`RCLONE_CONFIG=/dev/null` (your own `rclone.conf` and its remotes are never used), type `b2`, and
`hard_delete=false`, so deletes stay soft and the bucket's lifecycle rules give an undo window. The bucket
(`DATA_REMOTE`, `everef:data-everef-net-425eb511`) is fixed there too.

### Directories

```
$SCRATCH_DIR/                      # one workflow; deleted by hand at the end
  00-preflight.done, 01-find.done, … # completion files, one per completed step
  find/                            # 01-find: files.jsonl (the selection), listing.jsonl.gz, report.txt, ...
  CHECKLIST.md                     # your notes for this workflow
  manifest.json                    # selection + SHA-256 (03-check-load)
  <ID>.torrent  <ID>.txt  trackers.txt
  backup-plan.json  <ID>-backup-index.tsv  backup/
  deleted.jsonl
  log/<NN>-<step>-<UTC time>.log   # everything each run of a step printed

$SEED_DIR/                         # kept; qBittorrent save path
  everef-deep-archive-2023-04-11/  # torrent content, exactly what the torrent describes
    everef-deep-archive-2023-04-11.sha256
    everef-deep-archive-2023-04-11-README.txt
    market-orders/history/2023/…
  everef-deep-archive-…/           # earlier torrents, still seeding
```

`SCRATCH_DIR` and `SEED_DIR` must be separate, neither inside the other. Nothing but torrent content ever goes into
`$SEED_DIR/<ID>/`.

## How the scripts work

1. **One script per step, numbered in order:** `00-preflight.py`, `01-find.py`, …, run from `scripts/deep-archive/`
   (`./00-preflight.py`). Scripts never run the next step. Shared code is in `lib/`.
2. **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` as the very last thing it does, only
   when it succeeded. A step **refuses to start** if the previous step's completion file is missing, or if **its own**
   already exists (no accidental double execution). The torrent ID is read from `01-find.done`; no step takes it as an
   argument.
3. **Output, like the roll-up scripts:** `# 🚀 <step>: <ID>` once, `## 🔵 <section>` headings, plain lines for
   results, `🔴 <message>` for each problem, and `✅ <step> OK …` as the last line on success. Everything is also
   written to `$SCRATCH_DIR/log/`. On 🔴, nothing is marked complete: fix the cause and run the same step again.
   Steps that copy or upload can be run again after an interruption and carry on where they stopped.
4. **Redoing a completed step** is deliberate: delete its completion file and every later one by hand, and write down
   why in `CHECKLIST.md`.
5. **Show, then ask.** A step that writes anywhere outside `$SCRATCH_DIR` and `$SEED_DIR` (the data bucket, Glacier,
   qBittorrent) prints what it will do and asks first. `19-delete` needs the torrent ID typed back in.
6. **Checks are read-only** and re-derive everything from the source (bucket, disk, qBittorrent, site). They never
   trust the action step's own output.
7. **Manual steps** (announce, tracker check, `DataIndex`) have no script and no completion file. Tick them off in
   `CHECKLIST.md`.
8. If something looks wrong and no check caught it, **stop anyway**.

---

## The workflow

### Before you start

Tick all of these. If any isn't true, stop.

- [ ] These code changes are **deployed in production** (PLAN 2.17, 2.18): the `SyncFuzzworkOrdersets` ID cutoff,
      the `SyncMer` guard, and `DataCrawler` failing on a missing index.
- [ ] One-time docs are **live on docs.everef.net** (PLAN 4.2): the Deep Archive page; `downloading-datasets.md`
      recommends `rclone copy`, not `rclone sync`, and its examples use `market-history/`.
- [ ] The backup destination exists and works (PLAN 2.11; not decided yet).
- [ ] `$SEED_DIR` is on redundant storage (e.g. a ZFS mirror or RAID), scrubbed or checked recently.
- [ ] The storage migration isn't in progress (PLAN 2.21): the selection, the delete and the full `DataIndex` must
      all see the same storage.
- [ ] `$SCRATCH_DIR` is empty: the previous workflow is finished and its scratch directory deleted.

### 00 — Preflight
```
./00-preflight.py
```
Runs every check and reports each failure with a 🔴 line, then aborts if any failed:
- the settings are there (environment or `torrents.env`);
- `$SCRATCH_DIR` exists, is writable and is **empty** (apart from `log/`, left by earlier failed attempts);
- `$SEED_DIR` exists, is writable, and has room for the largest possible torrent (`MAX_BYTES`, 1 TiB);
- the two directories are separate;
- rclone installed; the B2 key set; the site root's `index.json` readable; `deep-archive/` listable (prints how many
  torrents are published and the newest);
- libtorrent 2.x and `trackers.txt`.

Read-only, apart from its log and completion file.

### 01 — Find
```
./01-find.py
```
Lists the **whole bucket** with `rclone lsjson` (one B2 request per 1,000 files) and chooses the selection (PLAN 2.8):
- Skips `index.html`, the datasets in `TORRENT_EXCLUDE` (`lib/workflow.py`), `*-latest.*`, hidden files,
  root-level files, `deep-archive/`, and files listed in a published `deep-archive/<ID>.txt`. `index.json` files are
  selected by their own modification date.
- Never selects anything modified less than 2 years ago (`MIN_AGE_YEARS`).
- **Cutoff:** the day before the first modification date that would take the selection over `MAX_FILES` (14,320) or
  `MAX_BYTES` (1 TiB). If the limits aren't reached, the cutoff is the newest allowed date. Fails if the oldest date
  alone is over a limit (a date can't be split).
- The torrent ID is `everef-deep-archive-<cutoff>`. Fails if `$SEED_DIR/<ID>/` already exists.
- Writes `$SCRATCH_DIR/find/`: `files.jsonl` (path, size, last-modified, URL), `listing.jsonl.gz` (the full listing),
  `report.txt`, `summary.json`, `find.log`.
- Prints: listed, candidate, too-new and selected counts and sizes; the cutoff and why; the estimated `.torrent` size,
  bdecode tokens and pieces; breakdowns by modification month, year folder in the path, and dataset; flags; and what
  was skipped. Listing progress every `--progress-seconds` (default 10, `0` for none).

`files.jsonl` **is the selection**: everything later is checked against it, and it can't change, because `01-find`
can't run again.

**Review the report** before going on:
- [ ] No `market-history/` or `killmails/` files anywhere.
- [ ] Flags looked at: empty files, `index.json` files whose folder keeps files on the site, an estimate over a limit.
- [ ] Every "(no year folder)" dataset checked: archive data, not a current file that just hasn't changed. Anything
      that must stay goes into `TORRENT_EXCLUDE` in `lib/workflow.py`.
- [ ] If the cutoff is "the newest allowed date (limits not reached)": a smaller torrent is worth making now, rather
      than waiting.

If the selection is wrong (e.g. a dataset must be excluded), fix `lib/workflow.py`, delete `$SCRATCH_DIR/01-find.done`
and run `01-find` again.

### Announce (manual)

Post the **start announcement** (template in PLAN 2.13) with the torrent ID, cutoff date and total size. Note it in
`CHECKLIST.md`.

### 02 — Load, 03 — Check load
```
./02-load.py
./03-check-load.py
```
`02-load` downloads exactly the files in `files.jsonl` from the bucket into `$SEED_DIR/<ID>/` with
`rclone copy --files-from` (parallel, resumable, keeps modification times). Nothing else.

`03-check-load` must show:
- [ ] File count and total bytes equal `files.jsonl`; `missing: 0`, `size mismatch: 0`, `unexpected files: 0`.
- [ ] `archive test failures: 0`, **or** every failure listed and accepted in `CHECKLIST.md`. Corrupt files are
      archived as they are.
- [ ] `manifest.json written` with the same file count (SHA-256 for every file).

### 04 — Make torrent, 05 — Check torrent
```
./04-make-torrent.py
./05-check-torrent.py
```
`04-make-torrent`, in order:
1. Writes `<ID>.sha256` (from the manifest, `sha256sum` format) and `<ID>-README.txt` (from the template, with the
   verification instructions) **into** `$SEED_DIR/<ID>/`.
2. Snapshots the trackers into `$SCRATCH_DIR/trackers.txt` (PLAN 2.4 "Trackers"), generates the comment (PLAN 2.4),
   and creates `$SCRATCH_DIR/<ID>.torrent` (hybrid v1+v2, `PIECE_SIZE` 32 MiB) from `$SEED_DIR/<ID>/`.
3. Generates `$SCRATCH_DIR/<ID>.txt` **from the torrent**: one data file path per line, sorted.

`05-check-torrent` must show:
- [ ] Torrent file list = manifest paths + exactly `<ID>.sha256` and `<ID>-README.txt`; total data bytes = manifest
      bytes.
- [ ] Hybrid v1+v2, `name` is `<ID>`, no web seed, not private.
- [ ] Trackers equal the snapshot, one per tier.
- [ ] The comment equals the one regenerated from the manifest, and its docs link returns 200.
- [ ] `.txt` equals the torrent's data file list; `.sha256` equals the manifest.
- [ ] **Documented verification passes:** `sha256sum -c <ID>.sha256` in `$SEED_DIR/<ID>/` reports 0 failures (re-reads
      all the data: hours).
- [ ] No `market-history/` or `killmails/` paths in the `.txt`.
- [ ] The `.torrent` loads with libtorrent's default limits.
- [ ] Info-hashes (v1 and v2) printed. Copy them into `CHECKLIST.md`.

Also read the `.txt` and the README yourself: the datasets you expect, nothing odd, links working.

### 06 — Seed, 07 — Check seed
```
./06-seed.py
./07-check-seed.py
```
`06-seed` adds the torrent to qBittorrent with save path `$SEED_DIR`, so it finds the content in `$SEED_DIR/<ID>/`.

`07-check-seed` must show:
- [ ] Save path is `$SEED_DIR`; progress 100% with **downloaded bytes: 0** (any download means the files don't match:
      stop); state seeding; at least one tracker working.

**Trackers from outside (manual).** Upload `<ID>.torrent` to [torrenteditor.com](http://torrenteditor.com/), which
queries the trackers in it. At least one should report 1 seeder or more. Give them a few minutes after `06-seed`.
Note which answered in `CHECKLIST.md`.

### 08 — Test download
```
./08-test-download.py
```
Starts a throwaway client with an empty disk, using only the magnet link. Downloads the `.sha256`, the README and a
random sample of data files, then runs `sha256sum -c --ignore-missing <ID>.sha256`, as the README tells users to.
- [ ] `hash mismatches: 0`; the `.sha256` and README are identical to the local ones; metadata came through the magnet
      link.

On the seeder host this doesn't prove the seeder is reachable from the internet. When possible, also try a magnet
download on another machine on another network, and note the result.

### 09 — Upload torrent, 10 — Check upload
```
./09-upload-torrent.py
./10-check-upload.py
```
`09-upload-torrent` shows the target paths and asks first. It refuses to overwrite. It uploads into the bucket's
`deep-archive/`: `<ID>.torrent` (`application/x-bittorrent`) and `<ID>.txt` (`text/plain; charset=utf-8`), with
Content-Type and Cache-Control set on the files (PLAN 2.21). The checksums and README aren't uploaded; they're inside
the torrent.

`10-check-upload` must show, for both files:
- [ ] `https://data.everef.net/deep-archive/<ID>.<ext>` returns 200 with the right Content-Type, and the bytes equal
      the local file.
- [ ] The file-list link in the torrent's comment returns 200.

### 11 — Build feed, 12 — Check feed
```
./11-build-feed.py
./12-check-feed.py
```
`11-build-feed` rebuilds `torrents.json` and `feed.xml` from every `.torrent` in the bucket's `deep-archive/` (and
`superseded.txt` here), shows the difference, and asks first. **Expect exactly one addition: `<ID>`.** Anything else:
stop.

`12-check-feed` must show:
- [ ] `feed.xml` is valid XML, served as `application/rss+xml` with a short max-age.
- [ ] It has an item `<ID>` with the info-hash from `05-check-torrent`, an enclosure URL that returns 200, and a
      link to the `.txt`; `torrents.json` has `<ID>` with the same info-hash.

On the first workflow, also add `feed.xml` to a qBittorrent RSS reader and confirm the item shows up.

### 13 — Check docs
```
./13-check-docs.py
```
The docs page is rule-based (PLAN 4.2), so normally nothing changes. Read
`docs.everef.net/datasets/deep-archive.html` and confirm:
- [ ] The rules still match: 2-year minimum age, size-limited torrents, the excluded datasets.
- [ ] `13-check-docs` ends in ✅: the page is reachable, has the verification instructions, and its links resolve
      (`deep-archive/`, `feed.xml`, `<ID>.torrent` and `.txt`).

If the rules changed, update the page and confirm it's live **before** deleting anything.

### 14 — Prepare backup, 15 — Check backup plan
```
./14-prepare-backup.py
./15-check-backup-plan.py
```
`14-prepare-backup` plans ZIP bundles (`<ID>-part-NNN.zip`, ~50 GB each) from the manifest, and writes
`backup-plan.json` and `<ID>-backup-index.tsv`.

`15-check-backup-plan` must show:
- [ ] Every manifest file in exactly one bundle (`unplanned: 0`, `duplicates: 0`); bundle sizes add up to the
      manifest bytes; the index has one line per file with the manifest's SHA-256.

### 16 — Backup, 17 — Check backup
```
./16-backup.py
./17-check-backup.py
```
`16-backup` shows the target prefix, bundle count and bytes, and asks first. One bundle at a time, it builds the
bundle in `$SCRATCH_DIR/backup/`, records its SHA-256, uploads it with storage class `DEEP_ARCHIVE`, and deletes the
local bundle. Then it uploads `<ID>-backup-index.tsv`, `<ID>.torrent`, `<ID>.txt`,
`<ID>.sha256`, `<ID>-README.txt` and `manifest.json`. Resumable.

`17-check-backup` must show:
- [ ] Every planned bundle exists with the recorded size, SHA-256 metadata and storage class `DEEP_ARCHIVE`.
- [ ] The metadata files are identical to the local ones; `$SCRATCH_DIR/backup/` is empty.

**First workflow only:** a **restore drill**. Restore the smallest bundle (bulk retrieval, up to 48 hours), unzip it,
run `sha256sum -c --ignore-missing <ID>.sha256` against it, and record the result. Deletion doesn't wait for it.

Internet Archive (optional, not decided, PLAN 2.12): if used, upload the bundles after `17-check-backup`. Deletion
doesn't wait for it.

### 18 — Check before delete
```
./18-check-before-delete.py
```
Re-checks from scratch:
- [ ] The docs page is live, and the backup check passes again.
- [ ] **The bucket matches the selection**, in a fresh listing: every selected file still exists with the same
      **size and modification time** (`index.json`: existence only). Any difference stops the workflow (see "If data
      changed").
- [ ] The seeder is still at 100% and seeding.
- [ ] Prints the delete set: file count, total bytes, per-dataset breakdown, and the `index.html` files that will go
      with emptied folders. Records the object count under each affected top-level folder, for `20-check-delete`.

**Pause crawling jobs (manual).** Disable the scheduled `sync-fuzzwork-ordersets` and the market history scrape
until after the full `DataIndex`. Until then, parent index pages still link to removed folders, and `DataCrawler`
fails on those.

### 19 — Delete, 20 — Check delete
```
./19-delete.py              # dry run: prints what would be deleted
./19-delete.py --execute    # asks you to type the ID to confirm
```
- The dry run must show exactly the counts from `18-check-before-delete`. It doesn't complete the step.
- `--execute` lists again, re-checks size and modification time, then deletes exactly the selected files with
  `rclone delete --files-from` (never by prefix or pattern; soft deletes only), plus `index.html` in folders
  left with only index files. Every path goes to `deleted.jsonl`.
- **Treat it as permanent.** B2 keeps a hidden version for a while; that isn't relied on (PLAN 2.14).

```
./20-check-delete.py
```
- [ ] Every selected file is gone from a fresh listing, and no index files are left in emptied folders.
- [ ] Nothing else changed: the counts under the affected top-level folders went down by exactly the delete set.

**Full DataIndex (manual).**
```
docker run --rm --env-file <production env> <eve-ref image> data-index
```
Leave `DATA_INDEX_PREFIX` unset so it's a full run. Then **resume the crawling jobs**, and check the next
`sync-fuzzwork-ordersets` run succeeds and uploads nothing below its ID cutoff.

### 21 — Check site
```
./21-check-site.py
```
Cloudflare isn't purged; deleted files can stay cached for up to 30 days (PLAN 2.16). So it checks a random sample of
deleted files across datasets and months. A URL passes on `404`, or `200` with `cf-cache-status: HIT` ("still
cached").
- [ ] Every sampled URL passes. `200` from the origin, `5xx` or anything else fails.
- [ ] The dataset listings no longer show the deleted files.
- [ ] `https://data.everef.net/deep-archive/` lists the torrent, the `.txt` and the feed.

### Finish (manual)

1. Add a line to `LOG.md`: ID, cutoff, files, bytes, v1/v2 info-hashes, published, backed up and deleted dates.
   Commit it.
2. Post the **finish announcement** (template in PLAN 2.13).
3. **Delete `$SCRATCH_DIR`** by hand. `$SEED_DIR/<ID>/` stays and keeps seeding; the Glacier backup holds the
   `.torrent`, file list, checksums and manifest.

---

## Maintenance (between workflows)

`$SEED_DIR` is EVE Ref's primary copy of the archived data. Check it regularly (e.g. monthly):
- [ ] Force a recheck of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding.
- [ ] The filesystem scrub (ZFS/RAID) is clean.

If a recheck finds damage, let qBittorrent re-download the damaged pieces from other seeders. If there are none,
restore the affected bundles from Glacier (`<ID>-backup-index.tsv` says which bundle holds which file).

## Rollback

Re-upload the deleted files from `$SEED_DIR/<ID>/` (or a Glacier restore) into the bucket, using the published
`<ID>.txt` as the list (planned script, outside the numbered workflow). Re-uploaded files get a new upload time.
If B2's hidden versions still exist (the lifecycle rule keeps them about 2 days), removing the hide markers with B2's
own tools is quicker; that's manual and storage-specific (PLAN 2.14). Then run a full `DataIndex` and check the files
are served again. The torrent and docs can stay as they are.

## If data changed (`18-check-before-delete` finds a mismatch)

- Don't delete. Write it down in `CHECKLIST.md`.
- Find out why the file changed. If the dataset still changes after two years, add it to `TORRENT_EXCLUDE`.
- The torrent is already published: handle it as in PLAN Appendix A (replacement `<ID>-r2`).

## Script reference

| Step | Writes to | Data bucket access |
|---|---|---|
| `00-preflight` | completion file | read |
| `01-find` | `$SCRATCH_DIR/find/` | read |
| `02-load` / `03-check-load` | `$SEED_DIR/<ID>/` / `manifest.json` | read |
| `04-make-torrent` / `05-check-torrent` | `.sha256` + README in `$SEED_DIR/<ID>/`, `.torrent`, `.txt` / — | — |
| `06-seed` / `07-check-seed` | qBittorrent / — | — |
| `08-test-download` | throwaway client | — |
| `09-upload-torrent` / `10-check-upload` | **bucket `deep-archive/`** / — | write |
| `11-build-feed` / `12-check-feed` | **bucket `deep-archive/`** / — | write |
| `13-check-docs` | — | — |
| `14-prepare-backup` / `15-check-backup-plan` | `backup-plan.json`, backup index / — | — |
| `16-backup` / `17-check-backup` | **backup destination** / — | — |
| `18-check-before-delete` | — | read |
| `19-delete` / `20-check-delete` | **bucket data (permanent)** / — | write |
| `21-check-site` | — | — |

Every step also writes its completion file and its log in `$SCRATCH_DIR`.
