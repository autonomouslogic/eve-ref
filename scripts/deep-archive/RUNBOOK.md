# Runbook — EVE Ref Deep Archive

Status: **draft for review** (2026-10-08; revised 2026-10-09 for the mounted data storage, PLAN 2.21). The scripts
referenced here don't exist yet. Design and reasoning are in [`PLAN.md`](PLAN.md).

This is a **manual** process, run **ad hoc, at most once per calendar year**. You run each step yourself, read the
output, and only move on when the check passes.

## Rules

1. **Do the steps in order.** Every action has a matching `check-*` step, and you always run the check.
2. **A check passes only when it prints `OK`.** Anything else means **stop**: read `logs/<step>.log`, fix the cause,
   re-run the action, then re-run the check.
3. **Never create or edit files in `checks/` by hand.** Those receipts are how the later scripts know the earlier
   checks passed.
4. **The selection is fixed.** The file list frozen by `start-year` decides what is loaded, published, backed up and
   deleted. Every check compares against it.
5. **Deletion (Part F) is the only destructive step, and it comes last**: only after every year in the run has
   passed Parts D and E.
6. If something looks wrong and no check caught it, **stop anyway**. Write it down in the year's `CHECKLIST.md`.

## Conventions

- `Y` = the year being archived, e.g. `2025`. Years in this runbook are examples.
- `<ID>` = the torrent ID. Normally `everef-deep-archive-Y`. Splits, supplements and replacements have a suffix
  (PLAN 2.3). Scripts take the ID where this runbook writes `<ID>`; for a normal year that's `everef-deep-archive-Y`.
- Run every script through the wrapper: `deep-archive/run.sh <script> --dir <dir> [args]`. It runs inside the Docker
  image with `deep-archive/torrents.env` loaded, and mounts `<dir>` at the same path, so paths are identical on the host,
  in the scripts, and in qBittorrent.
- **`--dir` is the working directory, and it is the directory torrents are seeded from.** Every script takes it
  (required, no default). Set it once per session:
  ```
  DIR=/srv/everef-deep-archive    # example
  ```
  and the commands below use `--dir "$DIR"`. Use the same `--dir` for the whole run.
- **`$DATA_MOUNT` is the data site's storage, mounted on the filesystem** (PLAN 2.21), e.g. `/mnt/everef-data`. Its
  root is the site root. It's set in `torrents.env`; scripts read it from there. Today it's B2 through
  `rclone mount`. Scripts get it **read-only**, except `upload-torrent`, `build-feed`, `delete` and `reupload`.
  `$DIR` must not be inside `$DATA_MOUNT`.
- Layout inside `--dir`:
  ```
  $DIR/
    everef-deep-archive-2025/              # torrent content; qBittorrent save path = $DIR
      everef-deep-archive-2025.sha256      #   checksums (inside the torrent)
      everef-deep-archive-2025-README.txt  #   what it is + how to verify (inside the torrent)
      market-orders/history/2025/…
      public-contracts/history/2025/…
    everef-deep-archive-2025.torrent
    everef-deep-archive-2025.txt           # file list, published on the data site
    everef-deep-archive-2025.work/         # never part of the torrent
      find/                           #   output of the find step (B1)
      files.jsonl                     #   the frozen selection (D1)
      manifest.json                   #   selection + sha256
      backup-plan.json  everef-deep-archive-2025-backup-index.tsv
      backup/                         #   one bundle at a time, while backing up
      CHECKLIST.md  deleted.jsonl
      checks/*.ok  logs/*.log
    RUN-<date>.md                     # years chosen for this run
  ```
- Only `<ID>/` is torrent content. Everything else sits **beside** it, never inside, so the content folder stays
  exactly what the torrent describes.

### Requirements (draft scripts)

Until `run.sh` and the Docker image exist, the draft scripts (`scripts/find.py`, `scripts/create_torrent.py`) run
directly on the host. They need **Python 3** and the **libtorrent** Python bindings (`find.py` only needs Python 3):
```
sudo apt-get install python3 python3-libtorrent
```
libtorrent must be **2.x**: 1.x can't make v2 or hybrid torrents. Check the installed version:
```
python3 -c "import libtorrent; print(libtorrent.__version__)"
```

---

## Part A — Pre-flight (once per run)

Tick all of these before starting. If any is not true, stop.

- [ ] **A1.** These code changes are **deployed in production** (PLAN 2.17, 2.18):
  - `SyncFuzzworkOrdersets` ID cutoff;
  - `SyncMer` guard (fails if the site has no MER files);
  - `DataCrawler` failing on a missing index.
- [ ] **A2.** One-time docs are **live on docs.everef.net** (PLAN 4.2):
  - the Deep Archive page exists;
  - `downloading-datasets.md` recommends `rclone copy`, not `rclone sync`, and its examples use
    `market-history/2026/`;
  - no docs page links to files in a year that this run will archive.
- [ ] **A3.** The Glacier backup bucket exists and `torrents.env` has its credentials (PLAN 2.11).
- [ ] **A4.** The seeder host has enough free disk for this run, plus room for one backup bundle (~50 GB). You'll
      confirm the actual sizes in B1, but rough guide: the first run is most of the 5.7 TB.
- [ ] **A5.** `$DIR` is on redundant storage (e.g. a ZFS mirror or RAID), and it was scrubbed or checked recently.
- [ ] **A6.** `$DATA_MOUNT` is mounted (PLAN 2.21), and the serving layer sets Content-Type and Cache-Control for
      `deep-archive/` files by extension (PLAN 5).
- [ ] **A7.** `deep-archive/run.sh check-env --dir "$DIR"` prints `OK`. It checks the env file, the data mount (a real
      mount point with the site root's `index.json` and `deep-archive/`; mtimes of sample files equal their HTTP
      `Last-Modified`; a fresh listing can be forced), credentials (read-only calls) for the backup bucket, the
      qBittorrent API, disk space, tool versions. It also prints when the last torrent was published, for
      information. Runs should be at most once a year, but nothing enforces it.
- [ ] **A8.** The storage migration isn't in progress (PLAN 2.21): the selection, the delete and the full `DataIndex`
      must all see the same storage.

---

## Part B — Select (once per run)

The selection comes first. Everything later is checked against it. Pick Y, normally the newest year the policy allows
(current year − 2). One run makes one torrent with everything up to and including Y that isn't archived yet.

**B1. Find the files up to Y**
```
deep-archive/run.sh find --dir "$DIR" --year Y
```
`find` walks `$DATA_MOUNT`. **Draft (current version):** `run.sh` doesn't exist yet. Run it directly with Python 3,
pointing `--source` at the mount:
```
deep-archive/scripts/find.py --dir "$DIR" --year Y --source "$DATA_MOUNT"
```
`--source` can also be a local backup with the site's layout and timestamps, for trial runs; the real selection must
come from the mount. Walking the mount takes a while (one listing per directory). Optional: `--recent-days N` for the
"recently modified" flag (default 365). While running it logs each stage to stderr (published lists, frozen selections, each
top-level directory, scan progress every `--progress-seconds` seconds, default 10, `0` to turn it off, writing
results); the report goes to stdout.

- Doesn't check Y against the archive policy: only choose years ≤ current year − 2.
- Selects Y **and every year before it** (PLAN 2.1, 2.2): files with a year in the path, by that year; files without
  one, by modification time. Except files already in a published torrent or in another frozen selection in `$DIR`.
- `--limit N` (debug only): stops the scan after N selected files. The selection is incomplete and flagged as such;
  never freeze it (`start-year` must refuse a selection whose `summary.json` has `limit` set).
- Read-only on the source. Walks `$DATA_MOUNT`, and reads every published `<ID>.txt` file list from
  `$DATA_MOUNT/deep-archive/`.
- Creates the year's folders in `$DIR`:
  - `$DIR/<ID>/`: the torrent content folder, empty if new. `find` never writes into it; `load` fills it.
  - `$DIR/<ID>.work/find/`, replaced on every run: `files.jsonl` (path, size, last-modified, URL, `selected_by`),
    `report.txt` (what it prints), `summary.json` and `find.log` (flagged files, symlinks).
- Prints for the year:
  - file count and total size, and the oldest and newest last-modified;
  - **estimated `.torrent` size**, bdecode tokens and piece count;
  - a per-dataset breakdown, with "(by mtime)" for directories selected by modification time;
  - flags;
  - what was skipped: excluded datasets (`market-history`, `killmails`), files already in a published torrent, and
    root-level files modified in the year (never selected).

**B2. Review the output.** Check each of these:
- [ ] No `market-history/` or `killmails/` files anywhere in the breakdown.
- [ ] Flags (recently modified files, dates that don't match the year folder, empty files) have been looked at. Leave
      the year out of this run if in doubt.
- [ ] Every "(by mtime)" entry (files with no year in the path, selected by modification time, PLAN 2.2) has been
      checked: it's archive data, not a current file that just hasn't changed. Anything that must stay goes into
      `TORRENT_EXCLUDE`; then re-run B1.
- [ ] File count and estimated `.torrent` size are acceptable (PLAN 2.1 size risk). If not, stop and decide on a
      split before continuing.
- [ ] The total size of all chosen years fits on the seeder disk (A4).

Write the chosen years into `$DIR/RUN-<date>.md`.


---

## Part C — Announce (once per run)

**C1.** Post the **start announcement** (template in PLAN 2.13) to the announcement channel, with the chosen years
and total size. Note the date in `RUN-<date>.md`.

---

## Part D — Per year: prepare, publish, seed

Do D1–D8, then Part E, for the torrent (normally one per run; if the selection was split, for each part). Don't
start Part F until every torrent in the run is through Part E.

**D1. Start year**
```
deep-archive/run.sh start-year --dir "$DIR" Y
```
Needs B1's output for the year. **Freezes** `<ID>.work/find/files.jsonl` into `<ID>.work/files.jsonl`, and creates
`CHECKLIST.md`. Re-running `find` after this doesn't change the frozen selection. Open the checklist and tick off
each step as you go.

**D2. Load**
```
deep-archive/run.sh load --dir "$DIR" <ID>
deep-archive/run.sh check-load --dir "$DIR" <ID>
```
`load` runs `rclone copy --files-from` with the frozen list, from `$DATA_MOUNT` into `$DIR/<ID>/` (local to local,
parallel, keeps modification times). It copies exactly the selected files, nothing else. Re-run it after an
interruption; it skips files already copied.

`check-load` must show:
- [ ] File count and total bytes equal `files.jsonl`.
- [ ] `missing: 0`, `size mismatch: 0`, `unexpected files: 0`.
- [ ] `archive test failures:` is 0, **or** every failure is listed and you've accepted it (write it in
      `CHECKLIST.md`). Corrupt files are archived as they are.
- [ ] `manifest.json written` with the same file count (SHA-256 for every file).

`check-load` only looks at the selected data files. It ignores the `.sha256` and README that D3 adds, so re-running
it is safe.

**D3. Make checksums, README, torrent and file list**
```
deep-archive/run.sh make-torrent --dir "$DIR" <ID>
deep-archive/run.sh check-torrent --dir "$DIR" <ID>
```
`make-torrent`, in order:
1. Writes `<ID>.sha256` (from the manifest, `sha256sum` format) and `<ID>-README.txt` (from the template, including
   the verification instructions) **into** `$DIR/<ID>/`.
2. Snapshots the tracker lists into `<ID>.work/trackers.txt` (PLAN 2.4 "Trackers"), generates the comment from the manifest
   (PLAN 2.4), then creates `<ID>.torrent` (hybrid v1+v2) from `$DIR/<ID>/`.
3. Generates `<ID>.txt` **from the torrent**: one data file path per line, sorted.

`check-torrent` must show:
- [ ] Torrent file list = manifest paths + exactly `<ID>.sha256` and `<ID>-README.txt` (`path mismatches: 0`,
      `unexpected files: 0`).
- [ ] Total data bytes = manifest bytes.
- [ ] Hybrid v1+v2. `name` is `<ID>`. No web seed, not private.
- [ ] Trackers equal the snapshot in `<ID>.work/trackers.txt`, one tracker per tier (PLAN 2.4 "Trackers").
- [ ] The comment equals the one regenerated from the manifest (ID, years, datasets; PLAN 2.4), and its Deep
      Archive docs link returns 200. (The file-list link only works after D6; `check-upload` checks it.)
- [ ] `.txt` equals the torrent's data file list (`txt mismatches: 0`).
- [ ] The `.sha256` equals the manifest: same paths, same hashes, data files only (`sha256 mismatches: 0`).
- [ ] **Documented verification passes:** `sha256sum -c <ID>.sha256`, run in `$DIR/<ID>/`, reports 0 failures. This
      re-reads all the data, so expect hours on a large year.
- [ ] No `market-history/` or `killmails/` paths in the `.txt`.
- [ ] `.torrent` loads in a stock qBittorrent with default settings (size-limit check).
- [ ] Info-hashes (v1 and v2) printed. Copy them into `CHECKLIST.md`.

Also look through these yourself:
- the `.txt`: the datasets you expect, only year Y, nothing odd;
- the README: the year and the dataset list are right, and the links work.

**D4. Seed**
```
deep-archive/run.sh seed --dir "$DIR" <ID>
deep-archive/run.sh check-seed --dir "$DIR" <ID>
```
`seed` adds the torrent with save path `$DIR`, so qBittorrent finds the content in `$DIR/<ID>/`.

`check-seed` must show:
- [ ] Save path is `$DIR`.
- [ ] Progress 100%, **downloaded bytes: 0**. Any download means the local files don't match, so stop.
- [ ] State: seeding.
- [ ] Tracker status: working on at least one tracker.

**D4b. Check the trackers from outside (manual)**

Upload `$DIR/<ID>.torrent` to [torrenteditor.com](http://torrenteditor.com/). It queries the trackers in the file
and reports the seeders and leechers each one knows about.
- [ ] At least one tracker reports **1 seeder or more** (the seeder from D4).
- [ ] Note which trackers answered and which didn't in `CHECKLIST.md`.

This is a manual check with no receipt. It shows what the trackers have recorded, which is our seeder's announce.
It doesn't prove the seeder accepts incoming connections: D5 and a download from another network cover that.
Give the trackers a few minutes after D4 before checking. Very large `.torrent` files may be too big for the site
to accept; if so, note it and rely on D4 and D5.

**D5. Test download**
```
deep-archive/run.sh test-download --dir "$DIR" <ID>
```
Starts a throwaway client with an empty disk, using only the magnet link. It downloads the `.sha256`, the README and a
random sample of data files spread across datasets through the torrent, then runs
`sha256sum -c --ignore-missing <ID>.sha256`, exactly as the README tells users to.
- [ ] `sample: N files, hash mismatches: 0`.
- [ ] The `.sha256` and README downloaded through the torrent are identical to the local ones.
- [ ] Metadata was fetched through the magnet link.

**Note:** run on the seeder host, this doesn't prove the seeder is reachable from the internet. The client may
connect over the LAN or loopback. When possible, run `test-download` (or a manual magnet download) on another machine
on another network as well, and note the result in `CHECKLIST.md`.

**D6. Upload torrent and file list**
```
deep-archive/run.sh upload-torrent --dir "$DIR" <ID>
deep-archive/run.sh check-upload --dir "$DIR" <ID>
```
`upload-torrent` shows the target paths and asks for confirmation. It refuses to overwrite existing files. It
copies into `$DATA_MOUNT/deep-archive/`:
- `<ID>.torrent` (served as `application/x-bittorrent`)
- `<ID>.txt` (served as `text/plain; charset=utf-8`)

The Content-Type comes from the serving layer, by extension (PLAN 2.21), not from the file. The mount may finish
uploading after the copy returns, so check over HTTPS, not through the mount.

The checksums and README are **not** uploaded; they're inside the torrent.

`check-upload` must show, for **both** files:
- [ ] `https://data.everef.net/deep-archive/<ID>.<ext>` returns 200 with the right Content-Type.
- [ ] The downloaded bytes are identical to the local file.
- [ ] The file-list link in the torrent's comment returns 200.

**D7. Feed**
```
deep-archive/run.sh build-feed --dir "$DIR"
deep-archive/run.sh check-feed --dir "$DIR" <ID>
```
`build-feed` rebuilds `torrents.json` and `feed.xml` from all torrents in `$DATA_MOUNT/deep-archive/` (and
`deep-archive/superseded.txt` in this repo), and writes them there.
It shows a diff (items added or removed) and asks for confirmation. **Expect exactly one addition: `<ID>`.** If it
shows anything else, stop.

`check-feed` must show:
- [ ] `feed.xml` is valid XML, and served as `application/rss+xml` with a short max-age.
- [ ] It contains an item `<ID>` with the same info-hash as D3, an enclosure URL that returns 200, and a link to the
      `.txt`.
- [ ] `torrents.json` contains `<ID>` with the same info-hash.

On the first run, also add `feed.xml` to a qBittorrent RSS reader and confirm the item shows up.

**D8. Docs site review**
```
deep-archive/run.sh check-docs --dir "$DIR" <ID>
```
The docs page is rule-based (PLAN 4.2), so normally nothing needs editing. Read the live page at
`docs.everef.net/datasets/deep-archive.html` and confirm:
- [ ] The archive rule still matches what you're doing: years ≤ current − 2, the excluded datasets, and the timing.
- [ ] `check-docs` prints `OK`: the page is reachable, it has the manual verification instructions, and its links
      resolve: the `deep-archive/` directory, `feed.xml`, and `<ID>.torrent`/`.txt`.

If the rules changed (e.g. new exclusions), update the page, merge it, and confirm it's live **before** Part F.

---

## Part E — Per year: backup

**E1. Prepare the backup**
```
deep-archive/run.sh prepare-backup --dir "$DIR" <ID>
deep-archive/run.sh check-backup-plan --dir "$DIR" <ID>
```
`prepare-backup` plans the ZIP bundles (`<ID>-part-NNN.zip`, about 50 GB each) from the manifest, and writes
`backup-plan.json` and `<ID>-backup-index.tsv`.

`check-backup-plan` must show:
- [ ] Every manifest file is in exactly one bundle (`unplanned: 0`, `duplicates: 0`).
- [ ] Bundle sizes add up to the manifest bytes.
- [ ] The index has one line per manifest file, with the manifest's SHA-256.

**E2. Back up to Glacier Deep Archive**
```
deep-archive/run.sh backup --dir "$DIR" <ID>
deep-archive/run.sh check-backup --dir "$DIR" <ID>
```
`backup` shows the target prefix, bundle count and bytes, and asks for confirmation. Then, one bundle at a time, it
builds the bundle in `<ID>.work/backup/`, records its SHA-256, uploads it with storage class `DEEP_ARCHIVE`, and
deletes the local bundle. Last, it uploads `<ID>-backup-index.tsv`, `<ID>.torrent`, `<ID>.txt`, `<ID>.sha256`,
`<ID>-README.txt` and `manifest.json`. It's resumable: re-run it after an interruption.

`check-backup` must show:
- [ ] Every planned bundle exists, with the recorded size, SHA-256 metadata, and storage class `DEEP_ARCHIVE`.
- [ ] The metadata files exist and are identical to the local ones.
- [ ] `<ID>.work/backup/` is empty.

**First run only:** do a **restore drill**. Restore the smallest bundle (bulk retrieval, up to 48 hours), unzip it,
and run `sha256sum -c --ignore-missing <ID>.sha256` against it. Record the result in `CHECKLIST.md`. Part F doesn't
wait for the drill, but do it.

**E3. Internet Archive (optional, not decided yet)** — PLAN 2.12. If used, upload the bundles and metadata after E2.
Part F doesn't wait for it.

---

## Part F — Delete (once per run, last)

Start only when **every** year in `RUN-<date>.md` has passed Parts D and E. If a year fails F1, leave it out; the
others can go ahead.

**F1. Pre-delete check** (per year)
```
deep-archive/run.sh check-before-delete --dir "$DIR" <ID>
```
This **re-checks everything from scratch** and doesn't trust old receipts:
- [ ] All earlier receipts are present: load, torrent, seed, test-download, upload, feed, docs, backup plan, backup.
- [ ] Docs check: the Deep Archive page on docs.everef.net is live.
- [ ] Backup check: `check-backup` passes again.
- [ ] **Data mount matches the selection**, using a fresh listing (PLAN 2.21; data files only; the `.sha256` and
      README were never on the site): every file still exists with the same **size and modification time**
      (`index.json`: existence only), and **no extra files** exist under any of the year's dataset paths (for files
      selected by modification time, only the files themselves are checked). Any difference stops this year (see
      "If data changed" below).
- [ ] Seeder still at 100% and seeding.
- [ ] Prints the delete set: file count, total bytes, per-dataset breakdown, and the index files that will be
      removed. Records the file count under each affected top-level directory, for `check-delete`.

**F2. Pause crawling jobs.** Disable the scheduled `sync-fuzzwork-ordersets` and the market history scrape until F6.
Between the delete and the full `DataIndex`, parent index pages still link to removed directories, and `DataCrawler`
fails on those.

**F3. Delete** (per year)
```
deep-archive/run.sh delete --dir "$DIR" <ID>              # dry run: prints what would be deleted
deep-archive/run.sh delete --dir "$DIR" <ID> --execute    # asks you to type the ID to confirm
```
- The dry run must show exactly the counts from F1.
- `--execute` refreshes the mount's listing, re-checks size and modification time, then deletes each selected file
  through `$DATA_MOUNT` (one path at a time, never recursively), including the year's `index.json` files. It
  removes `index.html` from the emptied directories, then the emptied directories themselves. It logs every path to
  `deleted.jsonl`.
- **Treat it as permanent.** Whether the storage keeps a soft-deleted copy (B2 does, for a while) isn't relied on
  (PLAN 2.14). Rollback is a re-upload from `$DIR`.

**F4. Check delete** (per year)
```
deep-archive/run.sh check-delete --dir "$DIR" <ID>
```
- [ ] Every selected file is gone from a fresh listing of the mount.
- [ ] No `index.html` or `index.json` is left in the emptied directories, and the emptied directories are gone.
- [ ] Nothing else changed: the file count under the affected top-level directories went down by exactly the delete
      set (compared with the count `check-before-delete` recorded).

**F5. Full DataIndex**
```
docker run --rm --env-file <production env> <eve-ref image> data-index
```
Leave `DATA_INDEX_PREFIX` unset so it's a full run. It takes a while.

**F6. Resume crawling jobs** paused in F2. Check the next `sync-fuzzwork-ordersets` run succeeds and uploads nothing
below its ID cutoff.

**F7. Check site** (per year)
```
deep-archive/run.sh check-site --dir "$DIR" <ID>
```
Cloudflare is **not** purged. Deleted files can stay in its cache for up to 30 days (PLAN 2.16). So `check-site`
checks many URLs: a random sample of deleted files across datasets and months, plus every year directory's
`index.json`. A URL passes on `404`, or on `200` with `cf-cache-status: HIT` (reported as "still cached").
- [ ] Each dataset's listing (`index.html` and `index.json`) no longer shows year Y.
- [ ] Every sampled URL passes. `200` from the origin, `5xx` or anything else fails.
- [ ] `https://data.everef.net/deep-archive/` lists the torrent, the `.txt` and the feed.

---

## Part G — Finish (once per run)

**G1. Record**
- Add one line per torrent to `deep-archive/LOG.md`: ID, files, bytes, v1/v2 info-hashes, published date, backup date,
  deleted date.
- Commit it.
- Keep `$DIR/<ID>.work/` (selection, manifest, checklist, receipts, logs) in place, next to the content.

**G2. Announce.** Post the **finish announcement** (template in PLAN 2.13).

---

## Maintenance (between runs)

`$DIR` is the primary EVE Ref copy of archived years, so check it regularly (e.g. monthly):
```
deep-archive/run.sh check-seed --dir "$DIR" <ID> --recheck   # per torrent: forces a full qBittorrent recheck
```
- [ ] Every torrent is at 100% with 0 bytes downloaded, and seeding.
- [ ] The filesystem scrub (ZFS/RAID) is clean.

If a recheck finds damage, let qBittorrent re-download the damaged pieces from other seeders. If there are none,
restore the affected bundles from the Glacier backup (`<ID>-backup-index.tsv` says which bundle holds which file).

## Rollback

- Re-upload the selection from `$DIR/<ID>/` (or from a Glacier restore) into `$DATA_MOUNT`:
  ```
  deep-archive/run.sh reupload --dir "$DIR" <ID>            # dry run
  deep-archive/run.sh reupload --dir "$DIR" <ID> --execute
  ```
  It refuses to run unless `$DATA_MOUNT` is a real mount point (a dropped mount would fill the local disk instead).
  The re-uploaded files get new modification times unless the mount keeps them (rclone does).
- If the storage has a soft-delete window that's still open (B2: hide markers, currently 2 days), restoring with the
  storage's own tools is quicker. That's a manual, storage-specific step, not a script (PLAN 2.14).

Then run a full DataIndex (F5) and check that the files are served again. The torrent and docs can stay as they are.

## If data changed (F1 finds a mismatch)

- Do **not** delete that year. Write it down in `CHECKLIST.md`.
- Find out why the file changed. If the dataset still changes after two years, add it to `TORRENT_EXCLUDE`.
- The torrent is already published, so handle it as described in PLAN Appendix A (replacement `<ID>-r2`).

## Script reference

| Script | Writes to | Requires |
|---|---|---|
| `check-env` | — | — |
| `find --year Y` | `$DIR/<ID>/` (empty), `$DIR/<ID>.work/find/` | — |
| `start-year Y` | `$DIR/<ID>.work/` (frozen `files.jsonl`) | `find` output |
| `load` / `check-load` | `$DIR/<ID>/` / receipt + `manifest.json` | start-year / — |
| `make-torrent` / `check-torrent` | `.sha256` + README in content, `.torrent`, `.txt` / receipt | load |
| `seed` / `check-seed` | qBittorrent / receipt | torrent |
| `test-download` | temp container / receipt | seed |
| `upload-torrent` / `check-upload` | `$DATA_MOUNT/deep-archive/` / receipt | test-download |
| `build-feed` / `check-feed` | `$DATA_MOUNT/deep-archive/` / receipt | upload |
| `check-docs` | receipt | feed |
| `prepare-backup` / `check-backup-plan` | `backup-plan.json`, backup index / receipt | torrent |
| `backup` / `check-backup` | **Glacier backup bucket** / receipt | backup plan |
| `check-before-delete` | receipt | all above |
| `delete` / `check-delete` | **`$DATA_MOUNT` data (permanent)** / receipt | before-delete |
| `check-site` | receipt | delete + full DataIndex |
| `reupload` | `$DATA_MOUNT` data | delete |
