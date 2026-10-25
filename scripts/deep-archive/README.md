# EVE Ref Deep Archive for EVE Online

Moves old data.everef.net data into torrents. One workflow makes one torrent: the oldest data not archived yet, as
much as the size limits allow, named with a sequential two-digit ID (`everef-eve-online-deep-archive-NN`, starting
at `00`). Its cutoff date -- the last date it covers -- is recorded in its own README, inside the torrent only
(00-preflight picks the next ID; 01-find picks the cutoff fresh each run, from its own candidates, and never reads
an earlier torrent's cutoff back). It's then seeded, published, listed on the docs site, and deleted from the site.

Status: **draft**. Every step 00-16 is numbered and every scripted one is written; none has run against real data yet.
Design and reasoning: [`PLAN.md`](PLAN.md).

A GitHub Actions workflow (`.github/workflows/deep-archive-reminder.yml`) pings the dev notices Discord channel on
15 February and 15 August, the day after that month's rollup check, as a reminder to run a workflow.

## Requirements

- Python 3, **rclone**, the libtorrent Python bindings, **2.x** (1.x can't make hybrid torrents), **aria2c** (the test
  download in 08-check-seed), **sha256sum** (`03-load`'s own check of its checksum file), **Docker**
  (`04-sign-sha256` and `06-sign-torrent` run minisign through it):
  ```
  sudo apt-get install python3 rclone python3-libtorrent aria2 coreutils docker.io
  ```
- A B2 key for the data bucket.
- `SEED_DIR` on redundant storage (e.g. a ZFS mirror or RAID), with room for one more torrent (`MAX_BYTES` in
  `lib/workflow.py`). After deletion it's EVE Ref's only copy of the archived data (Appendix C).
- qBittorrent-nox 5.2 or later, for seeding, with its Web API reachable (`QBITTORRENT_URL`) and an API key
  generated in the Web UI settings (`QBITTORRENT_API_KEY`).
- The minisign private key used to sign the checksum file and the torrent file, kept outside this repo
  (`MINISIGN_KEY_PATH`).

## Setup

Copy `torrents.env.example` to `torrents.env` and fill in `SCRATCH_DIR`, `SEED_DIR`, `DATA_BUCKET`,
`RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` and the qBittorrent settings (Appendix B). Optionally set
`LOCAL_COPY_DIR` to a local copy of the data site, and `QBITTORRENT_SAVE_PATH` if qBittorrent sees `SEED_DIR` at another
path.

## Tests

`make test-deep-archive` (from the repo root) runs the unit tests in `tests/`: how `01-find` dates files from their
names, what it skips, and how it chooses the cutoff -- and that a path a published torrent already holds, if it's
still a candidate, is a hard error rather than something clever happening with cutoff dates; the shared dataset/year
classification (`lib/workflow.py`'s `classify_path`, used by `01-find`'s and `11-check-before-delete`'s dataset
breakdowns) and the torrent comment (`render_comment`, that it's the template with the ID filled in and stays a
single line); that `STEPS` matches the numbered scripts; the `.torrent` size estimate (`lib/torrent_size.py`),
including a real torrent built with libtorrent and compared against the estimate for the same files; `09-publish`'s
feed building, round-tripped through its own checks; `11-check-before-delete`'s delete set and docs page check; and
`13-check-delete`'s checks, including that nothing outside the confirmed delete set went missing. Standard library
only, with one exception: the real-torrent-size and feed-building tests need the libtorrent Python bindings (like the
scripts they test) and are skipped, not failed, without them. Uses your `python3`, or Docker if there isn't one; the
Docker image doesn't have libtorrent, so those tests are skipped there.

## Procedure

Run each step in order, from `scripts/deep-archive/`. A step only starts once the previous one has completed, and it
can't run twice (Appendix A). The bullets under a step are what must hold before moving on; if one doesn't, or
anything else looks wrong, stop. Keep notes for the workflow in `$SCRATCH_DIR/NOTES.md`.

| Step | What | Script |
|---|---|---|
| 00 | Preflight | `./00-preflight.py` |
| 01 | Find | `./01-find.py` |
| 02 | Review the selection | manual |
| 03 | Load | `./03-load.py` |
| 04 | Sign the checksum file | `./04-sign-sha256.sh` |
| 05 | Make torrent | `./05-make-torrent.py` |
| 06 | Sign the torrent file | `./06-sign-torrent.sh` |
| 07 | Seed | `./07-seed.py` |
| 08 | Check seed | `./08-check-seed.py` |
| 09 | Publish | `./09-publish.py` |
| 10 | Update docs | manual |
| 11 | Check before delete | `./11-check-before-delete.py` |
| 12 | Delete | `./12-delete.py` |
| 13 | Check delete | `./13-check-delete.py` |
| 14 | Full DataIndex | manual |
| 15 | Check site | `./15-check-site.py` |
| 16 | Delete the scratch directory | manual |

### 00: Preflight
```
./00-preflight.py
```
Checks the settings, both directories (created if missing; `SCRATCH_DIR` empty, room in `SEED_DIR`), rclone, the B2
key and bucket, libtorrent, aria2c, the tracker lists (both download, at least 10 trackers together), `LOCAL_COPY_DIR`
if set, and qBittorrent's Web API (reachable, the API key works, libtorrent 2.x). Names any setting the environment
overrides `torrents.env` with. Also picks the next torrent ID from the published `.torrent` files in
`deep-archive/`: one past the highest sequence number, or `00` if there are none. Reports every problem, then
aborts if there were any; a missing tool (rclone, libtorrent, aria2c) aborts right after the tool checks.

### 01: Find
```
./01-find.py
```
Lists the bucket and selects the oldest files not archived yet, by date (the earliest of the modification date, an
ISO date in the file name, and 31 December of a year in the file name; PLAN 2.2), and nothing newer than the minimum
age (`MIN_AGE_YEARS`). What a published torrent already holds comes from its own `<ID>.txt` file list; a non-index
path still on the site that's already in one of those lists is a hard error (something went wrong -- an earlier
delete didn't happen, or the path reappeared), never silently re-selected. Dates are added oldest first until a
limit (`lib/workflow.py`) stops it:
the `.torrent` size limit is hard (the date that would go over it is left out), the data limit `MAX_BYTES` is soft
(the date that reaches it is the last one taken, so the torrent can be somewhat over). Each folder with a selected
file brings its own `index.json`, whatever that file's date. Uses the torrent ID `00-preflight` picked. Prints the
cutoff, the torrent ID and a report, and writes the selection to `$SCRATCH_DIR/find/files.jsonl`.

Aborts if the oldest date alone is over the `.torrent` size limit: that needs resolving by hand. Also aborts if the
selection is under `MIN_BYTES` (500 GiB): not enough data for a torrent yet. Nothing is written in either case; run
it again later.

### 02: Review the selection (manual)

Read the report (`$SCRATCH_DIR/find/report.txt`):
- No `market-history/`, `killmails/` or `ccp/` files.
- Every flag looked at.
- Every "(no year folder)" dataset is archive data, not a current file that just hasn't changed.
- Files dated by a year in the name (`find.log`): the number is a year, not an ID.
- If the limits weren't reached, a smaller torrent is worth making now.

To change the selection: fix `TORRENT_EXCLUDE` in `lib/workflow.py`, delete `$SCRATCH_DIR/01-find.done`, and run
`01-find` again.

### 03: Load
```
./03-load.py
```
Copies exactly the selected files into `$SEED_DIR/<ID>/` with rclone, keeping their modification times. With
`LOCAL_COPY_DIR` set, files there with the selected size and modification time are checked against the bucket with
`rclone check` (SHA-1 where B2 has one, size only where it doesn't) and copied from it; the rest, and any that fail the
check, are downloaded from the bucket. Then checks every one is there with the selected size and modification time,
and nothing else is. Then reads every file once to compute its SHA-256 and test `.bz2`/`.gz` files decompress, and
writes `manifest.json`. Resumable: if it fails or is interrupted, run it again and it copies only what's missing or
different. Aborts before copying if the folder holds files that aren't selected, or if `$SEED_DIR` lacks room.
- File count and bytes equal the selection; nothing missing, different or extra.
- With `LOCAL_COPY_DIR`: files that differ from the selection or fail `rclone check` are explainable (a file
  re-uploaded since the copy was made, say). Many of them point at the copy, not the bucket.
- No archive test failures, or each one accepted in `NOTES.md` (corrupt files are archived as they are).
- `manifest.json` written, with a SHA-256 for every file.
- `$SCRATCH_DIR/<ID>.sha256` written and verified against it with `sha256sum -c` (an independent check, with a
  different tool, of the hashes step 03 just computed).

### 04: Sign the checksum file
```
./04-sign-sha256.sh
```
Signs `$SCRATCH_DIR/<ID>.sha256` with minisign, run through Docker, using the private key at `MINISIGN_KEY_PATH`
(never committed to this repo, kept outside `$SCRATCH_DIR`). Always prompts for the key's passphrase interactively.
Writes `$SCRATCH_DIR/<ID>.sha256.minisig`, then verifies it against the public key published at
`ui/public/.well-known/minisign.pub` (https://everef.net/.well-known/minisign.pub), if that checkout is there.
Runs before 05-make-torrent, which copies both files into the torrent's content folder: they end up inside the
torrent, next to the README, and are never published standalone.
- `$SCRATCH_DIR/<ID>.sha256.minisig` written.
- Verified against the public key (the script does this itself if it can find it).

### 05: Make torrent
```
./05-make-torrent.py
```
Writes `<ID>-README.md` into `$SEED_DIR/<ID>/` (from `README-template.md`), copies `<ID>.sha256` and
`<ID>.sha256.minisig` in from `$SCRATCH_DIR` (04-sign-sha256's output), downloads the tracker lists (ngosang's
`trackers_best.txt` and XIU2's `best.txt`; fails if either is unreachable or empty, or if they give fewer than 10
trackers together), makes `$SCRATCH_DIR/<ID>.torrent` (hybrid v1+v2, their union as trackers), and generates the file
list `$SCRATCH_DIR/<ID>.txt` from it. Then reloads both from disk and checks them:
- Files = the manifest plus the root files (README, `.sha256`, `.sha256.minisig`); bytes match.
- Hybrid, `name` is `<ID>`, not private, no web seed; trackers one per tier; the comment is right.
- The trackers in `$SCRATCH_DIR/trackers/trackers.txt` look sane (no surprising domains).
- The `.txt` matches the torrent and passes `file_list_problems` (`lib/workflow.py`: full site paths,
  sorted, data files only); no `market-history/`, `killmails/` or `ccp/` paths.
- Loads with libtorrent's default limits.
- Copy the v1 and v2 info-hashes into `NOTES.md`.

Also read the `.txt` and the README yourself.

### 06: Sign the torrent file
```
./06-sign-torrent.sh
```
Signs `$SCRATCH_DIR/<ID>.torrent` with minisign, run through Docker, the same way as step 04. Writes
`$SCRATCH_DIR/<ID>.torrent.minisig`, then verifies it against the public key if that's checked out. This is the
signature 09-publish uploads: it lets someone verify the `.torrent` file itself, before loading it into a client,
without needing the torrent's own piece hashes.
- `$SCRATCH_DIR/<ID>.torrent.minisig` written.
- Verified against the public key (the script does this itself if it can find it).

### 07: Seed
```
./07-seed.py
```
Adds the torrent to qBittorrent with save path `$QBITTORRENT_SAVE_PATH` (`$SEED_DIR` as qBittorrent sees it; `$SEED_DIR`
itself if not set).

### 08: Check seed
```
./08-check-seed.py
```
Checks qBittorrent through its Web API, then downloads the README through the torrent with aria2c (from the
`.torrent`, no magnet link) into a throwaway folder. Give the trackers a few minutes after step 07 first.
- 100% with **0 bytes downloaded** (any download means the files don't match: stop), seeding, a tracker working.
- aria2c downloaded the README, identical to the seed copy.

It runs on the workflow host, so it proves the torrent downloads, not that it's reachable from outside (PLAN,
Review notes, R16).

### 09: Publish
```
./09-publish.py
```
Shows the targets and asks first, then uploads `<ID>.torrent`, `<ID>.txt` and `<ID>.torrent.minisig` to the
bucket's `deep-archive/` (all three from `$SCRATCH_DIR`). The README, the checksum file and its signature stay
inside the torrent only. Never overwrites (a file already published by an earlier attempt must be identical, and
is skipped). Then rebuilds `everef-deep-archive.xml` from the published torrents, shows the difference, asks again,
saves the published `everef-deep-archive.xml` to `$SCRATCH_DIR/feed-backup/feed-<UTC time>.xml`, and uploads it.
Last, checks all three files over HTTPS, retrying for a few minutes while Cloudflare still serves the old feed.
Prints the torrent's URL, v1 and v2 info-hashes, and full magnet link for step 10.
- The only difference in the feed is the new `<ID>`.
- The `.torrent`, `.txt` and `.torrent.minisig` are served from `https://data.everef.net/deep-archive/` with the
  right Content-Type and content; the feed is valid RSS and has `<ID>` with the right info-hash.

### 10: Update docs (manual)

Add the torrent to the "Torrents" table on the docs site's Deep Archive page
(`docs/src/datasets/deep-archive.md`), newest first: its ID linked to its URL, the v1 and v2 info-hashes, and the full
magnet link, all printed by step 09. Commit it and deploy the docs site. Step 11 checks the live page lists it.

### 11: Check before delete
```
./11-check-before-delete.py
```
- The docs page is live and lists `<ID>` with its v1 info-hash (step 10); the seeder is at 100% with 0 bytes
  downloaded.
- The published `.torrent` on the bucket has the same info-hash as `$SCRATCH_DIR/<ID>.torrent`.
- Builds the delete set from the torrent: each data file in it that is in the manifest, has the selected size in the
  torrent, and is on the bucket with the same size and modification time as in the selection (PLAN 2.14). No
  `index.json` or README.
- Files that fail are listed and stay on the site (Appendix E). Many failures mean a problem: look before going on.
- Note the printed delete set: files, bytes, datasets.

### 12: Delete
```
./12-delete.py              # dry run
./12-delete.py --execute    # type the torrent ID to confirm
```
- The dry run shows exactly the delete set from step 11.

`--execute` re-checks against a fresh listing, then deletes exactly the confirmed files (soft deletes; never by
prefix). Then it removes the folders the delete emptied: on B2 a folder disappears with its last file, so that means
removing `index.html` and `index.json` from every folder left with nothing else in it, and from parents left the same
way, deepest first; never the site root or `deep-archive/` (PLAN 2.15). Treat it as permanent.

Until the full `DataIndex` in step 14 runs, parent index pages still link to the now-deleted files/folders:
scheduled jobs that crawl the site (e.g. `sync-fuzzwork-ordersets`, the market history scrape) may fail against
those stale links in the meantime. Left as-is; they recover on their next run after `DataIndex` catches up.

### 13: Check delete
```
./13-check-delete.py
```
- Every file in the delete set is gone, no folder holds only index files, and nothing else changed: files that
  failed the check in step 11 are still there, and so is every other path that was on the bucket when `01-find` ran
  (step 11's pre-delete check already covers the delete set itself; this is what would catch `12-delete` deleting,
  or anything else removing, a path outside it). New uploads since `01-find` ran are fine; they're not checked.

### 14: Full DataIndex (manual)

Rewrites every folder's index pages, so the parents stop listing the deleted files and the folders step 12 removed.
```
docker run --rm --env-file <production env> <eve-ref image> data-index
```
Or, from a checkout with production settings in the environment:
```
./gradlew dataIndex
```
Leave `DATA_INDEX_PREFIX` unset.

### 15: Check site
```
./15-check-site.py
```
- Every sampled deleted URL returns 404, or 200 from Cloudflare's cache (PLAN 2.16).
- The listings no longer show the deleted files; `deep-archive/` lists the torrent, the `.txt`, the signature and the feed.

### 16: Delete the scratch directory (manual)

Delete `$SCRATCH_DIR`. `$SEED_DIR/<ID>/` stays and keeps seeding.

---

## Appendix A: How the scripts work

- **One script per scripted step, all steps numbered in order** (`STEPS` in `lib/workflow.py`; a unit test checks
  the script names match). Scripts never run the next step.
- **Completion files.** Each step writes `$SCRATCH_DIR/<NN>-<step>.done` as the last thing it does, only when it
  succeeded. A step refuses to start if the previous scripted step's file is missing, or if its own already exists. The
  torrent ID is read from `01-find.done`; no step takes arguments for it.
- **One step at a time.** A step holds `$SCRATCH_DIR/log/.lock` while it runs; a second step started in the same
  `$SCRATCH_DIR` refuses to start.
- **Redoing a completed step** is deliberate: delete its completion file and every later one by hand, and note why
  in `NOTES.md`. A step that failed wrote no completion file and can simply be run again; copies and uploads carry
  on where they stopped.
- **Output, like the roll-up scripts:** `🚀 <step>: <ID>` once, `🔵 <section>` headings, plain lines for
  results, `🔴 <message>` for each problem, and `✅ <step> OK …` as the last line on success. Everything is also
  written to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Show, then ask.** A step that writes outside `$SCRATCH_DIR` and `$SEED_DIR` (the bucket, qBittorrent)
  prints what it will do and asks first.
- **Checks are read-only** and re-derive everything from the source (bucket, disk, qBittorrent, site). They never
  trust the action step's output. `03-load`, `05-make-torrent` and `09-publish` check their own results at the
  end, like the roll-up scripts, instead of having a separate check step. `04-sign-sha256` and `06-sign-torrent`
  verify their own signature the same way, against the published public key, when they can find it.
- **Manual steps** are numbered like the others (`MANUAL_STEPS` in `lib/workflow.py`), but have no script and no
  completion file: a script waits for the nearest earlier scripted step.

## Appendix B: Settings, files and directories

`torrents.env` (from `torrents.env.example`, not committed) holds only what differs per host, plus the secrets. The
scripts read it for anything not already set in the environment; `00-preflight` names any setting the environment
overrides.

| Setting | What |
|---|---|
| `SCRATCH_DIR` | Work files for the current workflow. Empty when a workflow starts; deleted by hand at the end. |
| `SEED_DIR` | Torrent content, one folder per torrent, kept for seeding. qBittorrent's save path (see `QBITTORRENT_SAVE_PATH`). |
| `DATA_BUCKET` | The data site's bucket on B2. Its root is the site root. |
| `RCLONE_CONFIG_EVEREF_ACCOUNT`, `RCLONE_CONFIG_EVEREF_KEY` | The B2 key for the `everef` rclone remote. |
| `QBITTORRENT_URL`, `QBITTORRENT_API_KEY` | qBittorrent's Web API and its API key (qBittorrent 5.2+, generated in the Web UI settings). |
| `QBITTORRENT_SAVE_PATH` | Optional. `SEED_DIR` as qBittorrent sees it, if different (qBittorrent in Docker, say). `07-seed` adds torrents with this save path; `SEED_DIR` if not set. |
| `LOCAL_COPY_DIR` | Optional. A local copy of the data site, laid out like the bucket (`index.json` at the top). `03-load` copies the files it has with the selected size, modification time and SHA-1 instead of downloading them. |
| `MINISIGN_KEY_PATH` | Path to the minisign private key (`04-sign-sha256`, `06-sign-torrent`), kept outside this repo and `$SCRATCH_DIR`. Its passphrase is always entered interactively, never put here. |

Everything else is fixed in `lib/workflow.py`: the size limits (`MIN_BYTES`, `MAX_BYTES`, `MAX_TORRENT_BYTES`), the
excluded folders (`TORRENT_EXCLUDE`), the site URL (`DATA_BASE_URL`), and the rclone settings. As in the roll-up
scripts, rclone is configured only from `RCLONE_CONFIG_*` variables (`RCLONE_CONFIG=/dev/null`, so your own
`rclone.conf` is never used), with type `b2` and `hard_delete=false`, so deletes stay soft and the bucket's lifecycle
rules give an undo window. All storage access goes through rclone.

```
scripts/deep-archive/
  README.md  PLAN.md
  README-template.md               # the README inside each torrent (05-make-torrent)
  comment-template.txt             # the torrent's comment, one line, {id} filled in (lib/workflow.py's render_comment)
  torrents.env.example             # copy to torrents.env (not committed)
  00-preflight.py  01-find.py  …   # the steps
  04-sign-sha256.sh                # signs <ID>.sha256 with minisign, through Docker (bash, not Python)
  06-sign-torrent.sh               # signs <ID>.torrent the same way, after 05-make-torrent
  lib/workflow.py                  # shared: settings, size limits, steps, completion files, lock, output, helpers
  lib/trackers.py                  # downloads the tracker lists (00-preflight, 05-make-torrent)
  lib/torrent_size.py              # .torrent size estimate from paths and sizes, for 01-find's cutoff
  lib/qbittorrent.py               # qBittorrent Web API client (00-preflight, 07-seed, 08-check-seed, 11-check-before-delete)
  tests/                           # unit tests: make test-deep-archive (from the repo root)

$SCRATCH_DIR/                      # one workflow; deleted by hand at the end
  00-preflight.done, 01-find.done, … # completion files
  find/                            # 01-find: files.jsonl (the selection), listing.jsonl.gz, report.txt, ...
  NOTES.md                         # your notes for this workflow
  manifest.json                    # selection + SHA-256 (03-load)
  <ID>.sha256  <ID>.sha256.minisig # checksum file (03-load) and its signature (04-sign-sha256); copied into the
                                    # torrent by 05-make-torrent, never published standalone
  <ID>.torrent  <ID>.txt  <ID>.torrent.minisig  # the torrent's own signature (06-sign-torrent), published
  trackers/                        # 05-make-torrent: each tracker list as downloaded, and trackers.txt (the union)
  feed-backup/                     # 09-publish: the everef-deep-archive.xml it replaced
  delete-set.json                  # 11-check-before-delete
  deleted.jsonl                    # 12-delete
  log/<NN>-<step>-<UTC time>.log   # everything each run of a step printed
  log/.lock                        # held by the running step

$SEED_DIR/                         # kept; qBittorrent save path
  everef-eve-online-deep-archive-01/  # torrent content, exactly what the torrent describes
    everef-eve-online-deep-archive-01-README.md
    market-orders/history/2023/…
  everef-eve-online-deep-archive-00/  # earlier torrents, still seeding
```

`SCRATCH_DIR` and `SEED_DIR` must be separate, neither inside the other.

## Appendix C: Maintenance

`$SEED_DIR` is EVE Ref's only copy of the archived data (there is no backup; PLAN 2.11). Check it regularly
(e.g. monthly): force a recheck of every torrent in qBittorrent: all at 100% with 0 bytes downloaded, and seeding.

If a recheck finds damage, let qBittorrent re-download the damaged pieces from other seeders. If there are none,
the damaged files are lost.

## Appendix D: Rollback

Re-upload the deleted files from `$SEED_DIR/<ID>/` (or other seeders) into the bucket, using the published `<ID>.txt`
as the list (planned script, outside the numbered steps). Upload with rclone, which keeps the modification times,
so `01-find` still sees them as the archived version. A path can be in several torrents, one version each: restore
from the newest torrent holding it. While B2 still holds the hidden versions (the bucket's lifecycle rules decide how
long), removing the hide markers in B2's web console is quicker (PLAN 2.14). Then run a full `DataIndex` and check the
files are served again.

## Appendix E: If data changed

If `11-check-before-delete` finds a file that changed after it was selected:
- That file isn't deleted; the rest of the delete set is. Note it in `NOTES.md`.
- No replacement torrent: the changed file goes into a later torrent once it's old enough (PLAN Appendix A).
- Find out why. If the dataset still changes after the minimum age, add it to `TORRENT_EXCLUDE`.

## Appendix F: Script reference

| Step | Writes to | Bucket |
|---|---|---|
| `00-preflight` | none | read |
| `01-find` | `$SCRATCH_DIR/find/` | read |
| `03-load` | `$SEED_DIR/<ID>/`, `manifest.json`, `<ID>.sha256` | read |
| `04-sign-sha256` | `<ID>.sha256.minisig` | none |
| `05-make-torrent` | README, `.sha256`, `.sha256.minisig` in `$SEED_DIR/<ID>/`, `.torrent`, `.txt` | none |
| `06-sign-torrent` | `<ID>.torrent.minisig` | none |
| `07-seed` / `08-check-seed` | qBittorrent / throwaway aria2c download | none |
| `09-publish` | **bucket `deep-archive/`** (`.torrent`, `.txt`, `.torrent.minisig`), `feed-backup/` | write |
| `11-check-before-delete` | `delete-set.json` | read |
| `12-delete` / `13-check-delete` | **bucket data (permanent)**, `deleted.jsonl` / none | write / read |
| `15-check-site` | none | none |

Every step also writes its completion file and its log in `$SCRATCH_DIR`.
