# EVE Ref Deep Archive

Moves old data.everef.net data into torrents. One workflow makes one torrent: the oldest data not archived yet, as
much as the size limits allow, named after the last modification date it covers (`everef-deep-archive-YYYY-MM-DD`).
It's then seeded, published, backed up, and deleted from the site.

Status: **draft**. Implemented: `00-preflight`, `01-find`. Every other step is designed but not written yet.
Design and reasoning: [`PLAN.md`](PLAN.md).

## Requirements

- Python 3, **rclone**, and the libtorrent Python bindings, **2.x** (1.x can't make hybrid torrents):
  ```
  sudo apt-get install python3 rclone python3-libtorrent
  ```
- A B2 key for the data bucket.
- `SEED_DIR` on redundant storage (e.g. a ZFS mirror or RAID), with room for one more torrent (`MAX_BYTES` in
  `lib/workflow.py`).
- qBittorrent-nox, for seeding.

## Setup

Copy `torrents.env.example` to `torrents.env` and fill in `SCRATCH_DIR`, `SEED_DIR`, `DATA_BUCKET`,
`RCLONE_CONFIG_EVEREF_ACCOUNT` and `RCLONE_CONFIG_EVEREF_KEY` (Appendix B).

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
key and bucket, libtorrent and the tracker list. Reports every problem, then aborts if there were any.

### 01: Find
```
./01-find.py
```
Lists the bucket and selects the oldest files not archived yet, by modification date, up to the last date that stays
within the size limits (`lib/workflow.py`), and nothing newer than the minimum age (`01-find.py`). Prints the cutoff,
the torrent ID and a report, and writes the selection to `$SCRATCH_DIR/find/files.jsonl`.

Aborts if a single date is over a limit: that needs resolving by hand.

### Review the selection (manual)

Read the report (`$SCRATCH_DIR/find/report.txt`):
- No `market-history/` or `killmails/` files.
- Every flag looked at.
- Every "(no year folder)" dataset is archive data, not a current file that just hasn't changed.
- If the limits weren't reached, a smaller torrent is worth making now.

To change the selection: fix `TORRENT_EXCLUDE` in `lib/workflow.py`, delete `$SCRATCH_DIR/01-find.done`, and run
`01-find` again.

### Announce (manual)

Post the start announcement (PLAN 2.13) with the torrent ID, cutoff date and size.

### 02: Load
```
./02-load.py
```
Downloads exactly the selected files into `$SEED_DIR/<ID>/`. Resumable.

### 03: Check load
```
./03-check-load.py
```
- File count and bytes equal the selection; nothing missing, different or extra.
- No archive test failures, or each one accepted in `NOTES.md` (corrupt files are archived as they are).
- `manifest.json` written, with a SHA-256 for every file.

### 04: Make checksums
```
./04-make-checksums.py
```
Writes `<ID>.sha256` into `$SEED_DIR/<ID>/`, from the manifest.

### 05: Check checksums
```
./05-check-checksums.py
```
- The `.sha256` equals the manifest, and `sha256sum -c` reports 0 failures (re-reads all the data: hours).

### 06: Make torrent
```
./06-make-torrent.py
```
Writes `<ID>-README.txt` into `$SEED_DIR/<ID>/`, makes `$SCRATCH_DIR/<ID>.torrent` (hybrid v1+v2),
and generates the file list `$SCRATCH_DIR/<ID>.txt` from it.

### 07: Check torrent
```
./07-check-torrent.py
```
- Files = the manifest plus `<ID>.sha256` and `<ID>-README.txt`; bytes match.
- Hybrid, `name` is `<ID>`, not private, no web seed; trackers one per tier; the comment is right.
- The `.txt` matches the torrent; no `market-history/` or `killmails/` paths.
- Loads with libtorrent's default limits.
- Copy the v1 and v2 info-hashes into `NOTES.md`.

Also read the `.txt` and the README yourself.

### 08: Seed
```
./08-seed.py
```
Adds the torrent to qBittorrent with save path `$SEED_DIR`.

### 09: Check seed
```
./09-check-seed.py
```
- 100% with **0 bytes downloaded** (any download means the files don't match: stop), seeding, a tracker working.

### Check the trackers from outside (manual)

Give the trackers a few minutes, then upload `<ID>.torrent` to [torrenteditor.com](http://torrenteditor.com/):
- At least one tracker reports a seeder.

### 10: Test download
```
./10-test-download.py
```
Downloads the `.sha256`, the README and a sample of files through the magnet link, in a throwaway client.
- 0 hash mismatches; metadata came through the magnet link.

When possible, also try a magnet download from another network.

### 11: Upload torrent
```
./11-upload-torrent.py
```
Shows the targets and asks first. Uploads `<ID>.torrent` and `<ID>.txt` to the bucket's `deep-archive/`. Never
overwrites.

### 12: Check upload
```
./12-check-upload.py
```
- Both files are served from `https://data.everef.net/deep-archive/` with the right Content-Type and content.

### 13: Build feed
```
./13-build-feed.py
```
Rebuilds `feed.xml` and `torrents.json` in `deep-archive/`, shows the difference, and asks first.
- The only difference is the new `<ID>`.

### 14: Check feed
```
./14-check-feed.py
```
- The feed is valid and served as RSS; `<ID>` is in it and in `torrents.json` with the right info-hash.

On the first workflow, also add the feed to a qBittorrent RSS reader and check the item shows up.

### 15: Check docs
```
./15-check-docs.py
```
- The Deep Archive page is reachable and its links resolve.
- Its rules still match the scripts: minimum age, size-limited torrents, the excluded datasets. If not, update the page
  and confirm it's live before deleting anything.

### 16: Prepare backup
```
./16-prepare-backup.py
```
Plans the backup bundles (ZIPs) from the manifest.

### 17: Check backup plan
```
./17-check-backup-plan.py
```
- Every file is in exactly one bundle, and the sizes add up.

### 18: Backup
```
./18-backup.py
```
Shows the target and asks first. Builds and uploads one bundle at a time, then the metadata files. Resumable.

### 19: Check backup
```
./19-check-backup.py
```
- Every bundle and metadata file is in the backup, with the recorded size and hash.

### Restore drill (manual, first workflow only)

Restore the smallest bundle, unzip it, and run `sha256sum -c --ignore-missing <ID>.sha256` against it. Record the
result. Deleting doesn't wait for it.

### 20: Check before delete
```
./20-check-before-delete.py
```
- Docs live, backup still checks out, seeder at 100%.
- Every selected file is still on the bucket with the same size and modification time. If not, stop
  (Appendix E).
- Note the printed delete set: files, bytes, datasets.

### Pause crawling jobs (manual)

Disable the scheduled `sync-fuzzwork-ordersets` and the market history scrape until the full `DataIndex` has run.

### 21: Delete
```
./21-delete.py              # dry run
./21-delete.py --execute    # type the torrent ID to confirm
```
- The dry run shows exactly the delete set from step 20.

`--execute` re-checks, then deletes exactly the selected files (soft deletes; never by prefix). Treat it as
permanent.

### 22: Check delete
```
./22-check-delete.py
```
- Every selected file is gone, no index files are left in emptied folders, and nothing else changed.

### Full DataIndex (manual)
```
docker run --rm --env-file <production env> <eve-ref image> data-index
```
Leave `DATA_INDEX_PREFIX` unset.

### Resume crawling jobs (manual)

Re-enable the jobs paused before step 21.
- The next `sync-fuzzwork-ordersets` run succeeds and uploads nothing below its ID cutoff.

### 23: Check site
```
./23-check-site.py
```
- Every sampled deleted URL returns 404, or 200 from Cloudflare's cache (PLAN 2.16).
- The listings no longer show the deleted files; `deep-archive/` lists the torrent, the `.txt` and the feed.

### Record (manual)

Add a line to `LOG.md`: ID, cutoff, files, bytes, info-hashes, and the published, backed up and deleted dates.
Commit it.

### Announce the result (manual)

Post the finish announcement (PLAN 2.13).

### Delete the scratch directory (manual)

Delete `$SCRATCH_DIR`. `$SEED_DIR/<ID>/` stays and keeps seeding.

---

## Appendix A: How the scripts work

- **One script per step, numbered in order** (`STEPS` in `lib/workflow.py`). Scripts never run the next step.
- **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` as the last thing it does, only when it
  succeeded. A step refuses to start if the previous step's file is missing, or if its own already exists. The
  torrent ID is read from `01-find.done`; no step takes arguments for it.
- **Redoing a completed step** is deliberate: delete its completion file and every later one by hand, and note why
  in `NOTES.md`. A step that failed wrote no completion file and can simply be run again; copies and uploads carry
  on where they stopped.
- **Output, like the roll-up scripts:** `# 🚀 <step>: <ID>` once, `## 🔵 <section>` headings, plain lines for
  results, `🔴 <message>` for each problem, and `✅ <step> OK …` as the last line on success. Everything is also
  written to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Show, then ask.** A step that writes outside `$SCRATCH_DIR` and `$SEED_DIR` (the bucket, the backup, qBittorrent)
  prints what it will do and asks first.
- **Checks are read-only** and re-derive everything from the source (bucket, disk, qBittorrent, site). They never
  trust the action step's output.
- **Manual steps** have no script and no completion file.

## Appendix B: Settings, files and directories

`torrents.env` (from `torrents.env.example`, not committed) holds only what differs per host, plus the secrets. The
scripts read it for anything not already set in the environment.

| Setting | What |
|---|---|
| `SCRATCH_DIR` | Work files for the current workflow. Empty when a workflow starts; deleted by hand at the end. |
| `SEED_DIR` | Torrent content, one folder per torrent, kept for seeding. qBittorrent's save path. |
| `DATA_BUCKET` | The data site's bucket on B2. Its root is the site root. |
| `RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` | The B2 key for the `everef` rclone remote. |

Everything else is fixed in `lib/workflow.py`: the size limits, the excluded datasets (`TORRENT_EXCLUDE`), the site
URL (`DATA_BASE_URL`), and the rclone settings. As in the roll-up scripts, rclone is
configured only from `RCLONE_CONFIG_*` variables (`RCLONE_CONFIG=/dev/null`, so your own `rclone.conf` is never
used), with type `b2` and `hard_delete=false`, so deletes stay soft and the bucket's lifecycle rules give an undo
window. All storage access goes through rclone, never the AWS CLI or an S3 library.

```
scripts/deep-archive/
  README.md  PLAN.md  LOG.md
  torrents.env.example             # copy to torrents.env (not committed)
  trackers.txt                     # trackers added to every torrent, one per tier
  00-preflight.py  01-find.py  …   # the steps
  create_torrent.py                # experiment: measure .torrent sizes; becomes 06-make-torrent
  lib/workflow.py                  # shared: settings, size limits, completion files, output

$SCRATCH_DIR/                      # one workflow; deleted by hand at the end
  00-preflight.done, 01-find.done, … # completion files
  find/                            # 01-find: files.jsonl (the selection), listing.jsonl.gz, report.txt, ...
  NOTES.md                     # your notes for this workflow
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

`SCRATCH_DIR` and `SEED_DIR` must be separate, neither inside the other.

## Appendix C: Maintenance

`$SEED_DIR` is EVE Ref's primary copy of the archived data. Check it regularly (e.g. monthly):
- Force a recheck of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding.
- The filesystem scrub (ZFS/RAID) is clean.

If a recheck finds damage, let qBittorrent re-download the damaged pieces from other seeders. If there are none,
restore the affected bundles from the backup (`<ID>-backup-index.tsv` says which bundle holds which file).

## Appendix D: Rollback

Re-upload the deleted files from `$SEED_DIR/<ID>/` (or a backup restore) into the bucket, using the published `<ID>.txt`
as the list (planned script, outside the numbered steps). Re-uploaded files get a new upload time. While B2 still holds
the hidden versions (the bucket's lifecycle rules decide how long), removing the hide markers in B2's web console is
quicker (PLAN 2.14). Then run a full `DataIndex` and check the files are served again.

## Appendix E: If data changed

If `20-check-before-delete` finds a file that changed after it was selected:
- Don't delete. Note it in `NOTES.md`.
- Find out why. If the dataset still changes after the minimum age, add it to `TORRENT_EXCLUDE`.
- The torrent is already published: replace it as in PLAN Appendix A (`<ID>-r2`).

## Appendix F: Script reference

| Step | Writes to | Bucket |
|---|---|---|
| `00-preflight` | none | read |
| `01-find` | `$SCRATCH_DIR/find/` | read |
| `02-load` / `03-check-load` | `$SEED_DIR/<ID>/` / `manifest.json` | read |
| `04-make-checksums` / `05-check-checksums` | `.sha256` in `$SEED_DIR/<ID>/` / none | none |
| `06-make-torrent` / `07-check-torrent` | README in `$SEED_DIR/<ID>/`, `.torrent`, `.txt` / none | none |
| `08-seed` / `09-check-seed` | qBittorrent / none | none |
| `10-test-download` | throwaway client | none |
| `11-upload-torrent` / `12-check-upload` | **bucket `deep-archive/`** / none | write |
| `13-build-feed` / `14-check-feed` | **bucket `deep-archive/`** / none | write |
| `15-check-docs` | none | none |
| `16-prepare-backup` / `17-check-backup-plan` | `backup-plan.json`, backup index / none | none |
| `18-backup` / `19-check-backup` | **backup destination** / none | none |
| `20-check-before-delete` | none | read |
| `21-delete` / `22-check-delete` | **bucket data (permanent)** / none | write |
| `23-check-site` | none | none |

Every step also writes its completion file and its log in `$SCRATCH_DIR`.
