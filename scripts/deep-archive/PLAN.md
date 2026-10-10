# EVE Ref Deep Archive: Process & Planning

Status: **draft for review**. Revised 2026-10-10:

- **One workflow, one torrent, as big as the size limits allow** (2.1, 2.8). `01-find` takes the oldest files not
  archived yet, by modification date, until the next date would go over a hard-coded file count or byte size. Torrent
  IDs carry that cutoff date (`everef-deep-archive-YYYY-MM-DD`). There are no yearly torrents.
- **The scripts reach the storage only through rclone** (2.21), with one B2 key. Never the AWS CLI or an S3
  library. The earlier filesystem-mount design (revision of 2026-10-09) is dropped.
- **Numbered step scripts with completion files** (2.10): `00-preflight`, `01-find`, …; each step needs the previous
  one's completion file and refuses to run twice. Work files go in `SCRATCH_DIR` (deleted by hand after the
  workflow), torrent content in `SEED_DIR` (kept for seeding).
- The operator process is in [`README.md`](README.md); the separate runbook is gone.

Related work in progress (as of 2026-10-10):
- `./00-preflight.py`, `./01-find.py` and `./02-load.py`: **drafts** of the first three steps, with `lib/workflow.py`
  (settings, size limits, completion files) and `lib/torrent_size.py` (the `.torrent` estimate). Run directly with Python 3 and rclone (no `run.sh`/Docker image yet).
- Tests written first, for changes not yet made. They fail against the current code, as intended:
  - `DataCrawlerTest.shouldFailOnMissingDirectoryIndex`: `DataCrawler` must fail when a directory index is missing
    (2.17);
  - `SyncFuzzworkOrdersetsTest` (new, plus its `inject` line in `TestComponent`): the Fuzzwork ID cutoff and its
    guard (2.18).
- `ccp/` added to `TORRENT_EXCLUDE` (2.2).
- `create_torrent.py`: **experiment** for measuring `.torrent` sizes (Appendix B). Hybrid v1+v2 with Python
  libtorrent, `PIECE_SIZE` pieces, trackers from `trackers.txt`, comment from the 2.4 template. It becomes
  `06-make-torrent`.
- `trackers.txt`: the 7 long-lived core trackers (2.4 "Trackers"), one per tier. The ngosang snapshot from option 2
  isn't added yet.
- Not done yet: the docs example link changes (2.17, 4.2).

Why this is worth doing: the archive is seeded from a local copy, and the community will likely help seed it, so
moving old data off the data storage cuts hosting costs without losing the data. (Cheaper storage lowers the saving;
see open question 13.)

## Scope

The **EVE Ref Deep Archive**: old data.everef.net data, moved into torrents.

A documented, repeatable workflow that makes **one torrent** each time it runs:

1. **Select** the oldest data on data.everef.net that isn't archived yet, as much as fits in one torrent. The
   selection is a fixed file list, and every later step is checked against it.
2. **Announce** that archiving has started.
3. **Load** the selected files into the seed directory.
4. **Make** the `.torrent` file.
5. **Seed** it, and test it.
6. **Upload** the `.torrent` file and its file list to data.everef.net.
7. **Add** it to an **RSS feed** on data.everef.net, so other people can subscribe and automatically start seeding
   new torrents as they are published.
8. **Check the docs site** (docs.everef.net) is accurate.
9. **Back up** the torrent's data to **S3 Glacier Deep Archive**.
10. **Delete** the archived files from data.everef.net. This is the **last** step, done only after everything above
    is verified.
11. Run a **full `DataIndex`** job and check the site.

Workflows run ad hoc, one at a time. A workflow is always completed (published, backed up, deleted) before the next
one starts. There is **no notice period** between publishing a torrent and deleting its files, but an announcement is
posted when the workflow starts (2.13).

The process is **manual**: a human runs each step from [`README.md`](README.md), with supporting scripts and a check
after every step.

Deletion is the only destructive step. After deletion, the copies of the data are the seed directory, the Glacier
backup, and whatever community seeders hold.

Dates and IDs in this document (`everef-deep-archive-2023-04-11`, …) are **examples**.

---

## 1. Context

- data.everef.net hosts roughly **5.7 TB**.
- Storage today is **Backblaze B2**. The Java code uses its S3-compatible API (`DATA_S3_ENDPOINT_URL` in
  `local.env`). It is moving to a **cheaper storage model**. **The Deep Archive scripts only use rclone** (2.21),
  so moving them to other storage is a matter of rclone remotes.
- A **Cloudflare Worker** sits in front of the storage and serves data.everef.net. It is defined in a **separate
  repo**.
- Upload code lives in this repo (`S3Util`, the `data` S3 client in `inject/S3Module`, config in `Configs.DATA_*`).
  It sets Content-Type and Cache-Control as object metadata; the Deep Archive uploads do the same through rclone
  (2.21).
- `DataIndex` (`cli/DataIndex.java`, `make docker-data-index`):
  - Lists the storage and writes `index.html` + `index.json` into every directory.
  - Can be limited to one prefix with `DATA_INDEX_PREFIX`. A full run has no prefix.
  - When listing, it skips existing `index.html`/`index.json`, so a directory that contains only index files
    disappears from its parent's listing.
  - **It never deletes stale index files.** After data is deleted, the emptied directories keep their old
    `index.html`/`index.json`, still listing the deleted files with dead links. See 2.15.
- `DataCrawler` (`http/DataCrawler.java`) walks the site's `index.html` pages over HTTP. It is used by
  `SyncFuzzworkOrdersets` and the market history scraper. See 2.17.
- Every directory's public `index.json` lists files with `size`, `last_modified`, `type` and `file_time`. An earlier
  draft had the find step read these over HTTP. Now it lists the bucket with rclone (2.8).
- File layout is defined by `util/archive/ArchivePathFactories.java`, e.g.
  - `market-orders/history/YYYY/YYYY-MM-DD/market-orders-….v3.csv.bz2`
  - `public-contracts/history/YYYY/YYYY-MM-DD/public-contracts-….v2.tar.bz2`
  - `market-history/YYYY/market-history-YYYY-MM-DD.csv.bz2`
  - `killmails/YYYY/killmails-YYYY-MM-DD.tar.bz2`
  - `fuzzwork/ordersets/YYYY/YYYY-MM-DD/fuzzwork-orderset-{ID}-….csv.gz`
  - `fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-{ID}.csv.gz` (no year folder)
  - `esi-scrape/history/YYYY/…`, `reference-data/history/YYYY/…`, `ccp/sde/YYYY/…`, `ccp/mer/YYYY/…`, etc.

---

## 2. Design

### 2.1 Unit of archiving: one torrent per workflow, as big as the limits allow

- **Each torrent holds the oldest data not archived yet**, from **every** in-scope dataset, up to a cutoff date:
  everything modified on or before that date that isn't in an earlier torrent (2.8). The torrent ID carries the date:
  `everef-deep-archive-2023-04-11`.
- The cutoff is chosen to make the torrent **as big as possible** within hard-coded limits on file count and bytes
  (2.8). Those limits come from what torrent clients accept (2.4 "Creating the torrent", Appendix B): a `.torrent`
  with hundreds of thousands of files is too big for libtorrent's defaults, and qBittorrent's WebUI hung on 43,620.
- One workflow makes one torrent, and is completed (published, backed up, deleted) before the next starts. The first
  workflows catch up on the backlog (~800,000 files and ~4 TiB on 2026-10-08, Appendix B), one torrent at a time; after
  that, a workflow runs whenever enough data has passed the minimum age to fill a torrent.
- A torrent can't be changed after it's published, and is never replaced. Files that change before their delete
  stay on the site and go into a later torrent (Appendix A).

### 2.2 What gets archived

**Files at least 2 years old, by modification date** (`MIN_AGE_YEARS`). The modification date is rclone's
`ModTime`. On B2 that's the upload time, unless the uploader set `src_last_modified_millis` (EVE Ref's Java uploads go
through B2's S3 API, which doesn't; check on real files). So it's when EVE Ref stored the file, not the date in its
path. For scraped data, the two are
close; for backfills, the upload date can be much later than the data.

The snapshot lookback used by the market history scraper (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`) is only 30 days,
so the 2-year minimum age is safe for `market-orders`.

#### Never archived

A denylist, hard-coded in `lib/workflow.py` (`TORRENT_EXCLUDE`):

- `market-history/` and `killmails/`. These two datasets work as a **database**, not as snapshots: past files are
  modified in place. A torrent can't follow that. (For `market-history`, `ScrapeMarketHistory` also re-reads the last
  `ESI_MARKET_HISTORY_LOOKBACK` (450 days) of files on every run, and `import-market-history` depends on the full
  history being on HTTP.)
- `ccp/`. CCP's own data (SDE, MER, image export collections, CSM minutes, QENs, etc.): reference material linked
  from the docs and downloaded by URL, not scraped history. ~62 GiB, cheap to keep on HTTP (Appendix B).

Also never included, from any dataset: `*-latest.*`, `index.html`, hidden files, files at the root of the site (e.g.
`robots.txt`), and everything under `deep-archive/`.

**`index.json` files are included with their folder:** a folder's `index.json` is selected whenever any file
directly in that folder is, whatever its own modification date. It's never selected on its own date: `DataIndex`
rewrites every index on each full run, so that date is the last `DataIndex` run, not the data's, and an `index.json`
would never reach the minimum age. Only the folder's own `index.json`; parent folders' aren't added. Consequences:

- In the torrent they're data files like any other: in the selection, the `.txt` and the `.sha256`. They count
  towards the `.torrent` size estimate and `MAX_BYTES`.
- They're never in the delete set: their modification date never matches the selection's. The index cleanup removes
  them from folders the delete empties (2.15); in folders that keep files, they stay as the live index.
- A folder split across torrents (files on both sides of a cutoff) has its `index.json` in each of them, as it was
  then: `DataIndex` rewrites it, so it counts as changed (below). The one in the earlier torrent lists files that
  stay on the site. The find report flags those, and folders with selected files but no `index.json`.
- Their content links to data.everef.net URLs that stop working after deletion. That's expected; they're a record.
- rclone mirrors usually have them too (the HTML listing links `index.json`), but possibly a newer version. Mirror
  seeders' clients then re-download those few small files on recheck.

Everything else is in scope. Using a denylist means:

- New datasets are archived automatically once they're old enough.
- `fuzzwork/ordersets` **is** in scope. Its sync command gets a cutoff (2.18), so it doesn't re-upload deleted
  files. `ccp/mer` isn't (excluded with `ccp/`), so `SyncMer` needs no change.

**No version of a file lands in two torrents.** `01-find` reads every published `<ID>.txt`. A listed path still on
the site with a modification date on or before that torrent's cutoff (the date in its ID) is the version the torrent
holds: it's skipped, and flagged, since its delete should have removed it. A later date means the file was uploaded
again after it was archived, so it's a candidate like any other and goes into a later torrent. The same path can be in
several torrents, one version each.

**Risk of selecting by modification date:** a file that is still current but simply hasn't changed in two years
would be archived and deleted from the site, e.g. a reference file a tool downloads by a fixed URL (`ccp/iec`,
`ccp/sde/older`, Appendix B). The find report breaks the selection down by dataset, marks datasets with no year folder,
and the operator must check them (README, "Review the selection"). Add anything that must stay to `TORRENT_EXCLUDE`.

### 2.3 Torrent IDs, naming and layout

Every torrent has an **ID**. The ID is used everywhere: torrent name (root folder), `.torrent` file, `.txt` file
list, files inside the torrent, seed folder, backup prefix.

| Case | ID |
|---|---|
| Normal | `everef-deep-archive-<cutoff date>`, e.g. `everef-deep-archive-2023-04-11` |

Cutoffs only move forward (each torrent skips everything already published), so IDs sort in publication order.

Inside a torrent, the `name` is the ID and file paths are the **full site paths**:

```
name:   everef-deep-archive-2023-04-11
files:  everef-deep-archive-2023-04-11.sha256          # checksums for every data file (2.6)
        everef-deep-archive-2023-04-11-README.txt      # what this is (2.6)
        market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-00-00.v3.csv.bz2
        public-contracts/history/2023/2023-01-01/public-contracts-2023-01-01_00-00-00.v2.tar.bz2
        ccp/sde/2022/...
        ...
```

- By default a download lands in `everef-deep-archive-2023-04-11/market-orders/history/2023/…`. That's clear, and the
  torrents stay separate.
- To get the **exact data.everef.net layout**, which is also what `rclone` produces, download with qBittorrent's
  content layout set to **"Don't create subfolder"** (or the equivalent in other clients). The root folder is then
  dropped, and all torrents merge into one tree.
- Anyone with an existing rclone mirror can **seed immediately** the same way: add the torrent with "Don't create
  subfolder", point it at the mirror root, and let it recheck. That's the quickest way to get more seeders. Document
  this prominently.
- People who only want one dataset can deselect the other files in their client.

### 2.4 Torrent format

- **Hybrid v1+v2** (decided): v1 for client compatibility, v2 for per-file SHA-256 hashes. Hybrid torrents align
  every file to a piece boundary with pad files, which makes the metadata bigger. That's part of the size limits
  (2.8).
- **No web seed pointing at data.everef.net.** The files will be deleted, and a torrent can't be changed afterwards,
  so a web seed would be a permanently dead URL. The Internet Archive can't serve as a web seed either (2.12).
- Not private. DHT/PEX enabled. Public trackers: see "Trackers" below.
- Piece size: see "Creating the torrent" below.
- **Comment** (required): directs the user to the Deep Archive page on the docs site. **Generated automatically**
  by `make-torrent`, never typed by hand. Template:
  ```
  EVE Ref Deep Archive: {ID}
  Data from {YEARS}. Datasets: {DATASETS}.
  What this is, how to use it and how to help seed: https://docs.everef.net/datasets/deep-archive.html
  File list: https://data.everef.net/deep-archive/{ID}.txt
  ```
  Example:
  ```
  EVE Ref Deep Archive: everef-deep-archive-2023-04-11
  Data from 2017–2023. Datasets: esi-scrape, fuzzwork/ordersets, market-orders, public-contracts, reference-data, ...
  What this is, how to use it and how to help seed: https://docs.everef.net/datasets/deep-archive.html
  File list: https://data.everef.net/deep-archive/everef-deep-archive-2023-04-11.txt
  ```
  - `{ID}` is the torrent ID (2.3), which is also the torrent's name.
  - `{YEARS}` comes from the **manifest**, not the ID: the distinct years of the data files (year folder in the
    path, or the modification year for files without one). One year (`2023`), or a range (`2017–2023`).
  - `{DATASETS}` also comes from the manifest: the distinct dataset folders, sorted. If the list is long, it's cut
    off after a fixed length with `...`; the file list has everything.
  - `check-torrent` regenerates the expected comment from the manifest and compares it with the torrent's comment.
  - qBittorrent and most other clients show the comment in the torrent's details, and qBittorrent makes URLs in it
    clickable.
  - The comment is outside the `info` dictionary, so it doesn't change the info-hash, and it could be updated in a
    later re-publish (like trackers).
  - Users who add the torrent from a **magnet link** don't get the comment: only the `info` dictionary is fetched from
    peers. For them, the README inside the torrent carries the same link (2.6).
- `created by`: EVE Ref.

#### Trackers

**Notes for review, researched 2026-10-08. Not decided.**

Proposal (gut feeling): when creating a torrent, download the lists from
[XIU2/TrackersListCollection](https://github.com/XIU2/TrackersListCollection) and
[ngosang/trackerslist](https://github.com/ngosang/trackerslist), and add all of them.

What the lists are:

| List | Entries (2026-10-08) | Notes |
|---|---|---|
| ngosang `trackers_best.txt` | 20 (17 UDP, 3 HTTP) | Ranked by a bot on popularity and latency, updated daily. |
| ngosang `trackers_all.txt` | 75 (47 UDP, 18 HTTP, 10 HTTPS) | All working trackers it knows. Separate lists for WebSocket, I2P and Yggdrasil, which libtorrent clients can't use. |
| XIU2 `best.txt` | 71 (42 UDP, 13 HTTP, 15 HTTPS, 1 WSS) | Hand-picked; aggregates ngosang, newTrackon and others. Updated daily. Contains all 20 of ngosang's best. |
| XIU2 `all.txt` | 120 (71 UDP, 31 HTTP, 17 HTTPS, 1 WSS) | |
| Union of both `all` lists | 123 | |

How stable they are: I compared ngosang's `trackers_best.txt` from earlier dates with today's lists.

| `trackers_best.txt` as of | Still in today's `trackers_all.txt` |
|---|---|
| 2025-10-08 (1 year ago) | 7 of 20 |
| 2024-10-08 (2 years ago) | 9 of 20 |
| 2023-10-08 (3 years ago) | 7 of 20 |

The same 7 survive every time, the **long-lived core**:

```
udp://tracker.opentrackr.org:1337/announce
udp://open.stealth.si:80/announce
udp://tracker.torrent.eu.org:451/announce
udp://open.demonii.com:1337/announce
udp://exodus.desync.com:6969/announce
udp://explodie.org:6969/announce
udp://tracker-udp.gbitt.info:80/announce
```

Dropping off a list doesn't prove a tracker is dead, but most of each year's list is gone a year later. The
Deep Archive torrents are meant to last for years.

How clients use trackers:

- A torrent's trackers are in `announce-list`, grouped in **tiers** ([BEP 12](https://www.bittorrent.org/beps/bep_0012.html)).
  The spec: shuffle each tier, try its trackers in turn until one answers, and only go to the next tier if the whole
  tier fails.
- libtorrent's defaults follow the spec (`announce_to_all_tiers` and `announce_to_all_trackers` are both false).
  qBittorrent turns on "Always announce to all tiers" by default (uTorrent behaviour): one tracker per tier, every
  tier. (Check qBittorrent's current default.)
- So the layout matters more than the count. **One tracker per tier** means qBittorrent and uTorrent users announce
  to every tracker. **All trackers in one tier** means most clients use the first one that works.
- With DHT and PEX on, trackers mainly help peers find each other quickly. Peers on different trackers still meet
  through DHT.
- **Trackers are not part of the info-hash.** The tracker list sits outside the `info` dictionary. So:
  - a `.torrent` can be re-published later with a refreshed tracker list, and it's still the same torrent (same
    info-hash, same feed `guid`, same swarm);
  - clients merge the tracker lists when the same torrent is added again;
  - magnet links carry their own trackers (`tr=`).

Why "add all ~120" is a poor fit for these torrents:

- **For it:** simple, maximum reach on day one, no curation.
- **Against it:**
  - Most entries will be dead within a year or two, and clients show every dead tracker as an error. To a user,
    that looks like a broken torrent.
  - With one tracker per tier, every seeder announces to ~120 trackers per torrent every interval (typically about
    30 min). With 10+ torrents, that's over a thousand announces per interval per seeder: not huge, but wasteful, and
    it's the community seeders who pay for it.
  - Every tracker operator sees the IP of every peer. More trackers, more parties logging the seeders we're asking
    for help.
  - Both lists include domains associated with piracy (e.g. `wareztorrent.com`, `foreverpirates.co`). Not a
    technical problem, but it doesn't look great on an official dataset.
  - XIU2's list includes `bt1.archive.org` and `bt2.archive.org`, which probably only track the Internet Archive's
    own torrents (check), and a WebSocket tracker that qBittorrent can't use.

Options:

1. **All**, as proposed: union of both `all` lists, minus non-UDP/HTTP(S) schemes, one per tier.
2. **Curated** (my recommendation): the long-lived core above, plus a snapshot of ngosang's `trackers_best.txt`
   at creation time. That's about 20–25 trackers, one per tier, UDP and HTTP(S) only, with a denylist for unwanted
   domains. If the Academic Torrents tracker is used, the torrent must be registered there too (check).
3. **Core only**: the 7 long-lived trackers. Smallest, most stable, relies more on DHT.

Whichever option:

- Snapshot the lists at creation time into `$SCRATCH_DIR/trackers.txt` and record where they came from, so a torrent
  can be reviewed. The snapshot goes into the Glacier backup with the `.torrent`.
- `check-torrent` checks the torrent's trackers equal the snapshot, and one tracker per tier.
- Magnet links in the feed and on the docs page carry only the core trackers, to keep them short.
- Consider a **tracker refresh** later: re-publish `.torrent` files with a current list. Same info-hash, so nothing
  breaks. This needs `upload-torrent` to allow replacing a `.torrent` when the info-hash is unchanged.

**Recommendation: don't add all of them. Use option 2 (curated).**

- The main reason is decay: about two thirds of each year's "best" list is gone from the lists a year later, while
  these torrents are meant to last for years. A list of ~120 would soon be mostly dead trackers shown as errors.
- The other reasons: load on community seeders (~120 announces per torrent per interval), more parties logging
  seeder IPs, and piracy-branded and unusable entries (Internet Archive trackers, WebSocket) in an official dataset.
- The 7 long-lived trackers give stability. The snapshot of ngosang's best list adds today's best performers. One
  tracker per tier means qBittorrent and uTorrent users announce to all of them.
- The choice matters less than it seems, because trackers aren't part of the info-hash. If the curated list turns
  out too small, or trackers die, a refresh fixes it without creating a new torrent.

Sources: [ngosang/trackerslist](https://github.com/ngosang/trackerslist) and
[XIU2/TrackersListCollection](https://github.com/XIU2/TrackersListCollection) (lists downloaded on 2026-10-08; history
compared through the ngosang repo's commits), [BEP 12](https://www.bittorrent.org/beps/bep_0012.html),
[libtorrent settings](https://www.libtorrent.org/reference-Settings.html).

#### Creating the torrent

**Notes for review, researched 2026-10-08.**

**Tool.** `mktorrent` is the classic choice, but it **can't make v2 or hybrid torrents**. Its last release is v1.1
(2017) and its last commit is from 2021. Since hybrid is decided (above), it's out. Its option set is still a good
checklist: announce tiers (`-a` repeated per tier, comma-separated within a tier), piece length (`-l`, 2^15 to
2^28), name (`-n`), comment (`-c`), no creation date (`-d`), private (`-p`), source (`-s`), web seeds (`-w`),
threads (`-t`), exclude (`-e`).

Tools that do make hybrid torrents:

| Tool | Notes |
|---|---|
| Python `libtorrent` (2.x) | `create_torrent` makes hybrid by default. Full control, scriptable, same library as qBittorrent. Recommended. |
| qBittorrent Web API torrent creator | Since qBittorrent 5.0: `torrentcreator/addTask` with `format=hybrid`, `pieceSize`, `trackers`. We already run qBittorrent-nox, and it can start seeding straight away. Less control over details. |
| `torrenttools` | CLI, hybrid supported. Check it's still maintained. |
| `torrentfile` (Python) | CLI, v1/v2/hybrid. |

**Best practices:**

- **Hybrid layout.** Every file starts on a piece boundary, with pad files in the v1 view. Clients hide pad files
  and never download them.
- **Piece size.** Must be a power of two (v2 needs at least 16 KiB). Bigger pieces mean fewer hashes and a smaller
  `.torrent`; smaller pieces make partial downloads more precise. With hybrid alignment, small files never share a
  piece, so a bigger piece size doesn't hurt people selecting single files much. Common guidance is to aim for a few
  thousand to tens of thousands of pieces. **Decided: 32 MiB** (`PIECE_SIZE` in `lib/workflow.py`): 32,768 pieces for a
  full 1 TiB torrent, and the size limits (2.8) are worked out for it.
- **Metadata size limits.** This is the real constraint for us. libtorrent's defaults:
  - `.torrent` files up to **10 MB** (`max_buffer_size`) and **3 million** bdecode tokens (`max_decode_tokens`);
  - metadata fetched through a **magnet link** up to about **31 MB** (`max_metadata_size`);
  - at most 2,097,152 pieces.

  qBittorrent raises the file-size and token limits in its advanced settings (check its current defaults), but
  other libtorrent-based clients may not. A hybrid torrent with hundreds of thousands of files (each with a pad file
  and a v2 file-tree entry) can pass 10 MB and millions of tokens. **This is why `01-find` caps the file count**
  (2.8), and its report checks the estimated `.torrent` size against these limits, not just against qBittorrent.
- **Deterministic content.** Sort files by path. Then the same files always give the same info-hash. The creation
  date and trackers are outside the info dictionary, so they don't change it.
- **Keep the creation date.** The feed's `pubDate` uses it (2.5).
- **No `source` field, not private, no web seeds** (above).
- **Verify after creating.** Load it in a client and recheck against the content (README, steps 07–09).

**Recommendation:** create torrents with Python `libtorrent` 2.x, hybrid, files sorted by path, creation date kept,
32 MiB pieces.

Sources: [mktorrent](https://github.com/pobrn/mktorrent) (source code and release history checked),
[qbittorrent-api: torrent creator](https://qbittorrent-api.readthedocs.io/en/latest/apidoc/torrentcreator.html),
[torrentfile](https://pypi.org/project/torrentfile/0.1.1/),
[torrenttools](https://alternativeto.net/software/torrenttools),
[libtorrent settings](https://www.libtorrent.org/reference-Settings.html),
[libtorrent torrent_info limits](https://www.libtorrent.org/reference-Torrent_Info.html).

### 2.5 Files on data.everef.net

Everything published for the Deep Archive lives in one folder, **`deep-archive/`**, at the root of the data site
(`https://data.everef.net/deep-archive/`): the torrents, their file lists, the RSS feed and the catalogue. (The
folder with the same name in this repo holds the scripts and operator docs; it isn't published.)

```
deep-archive/                       # at the root of data.everef.net
  feed.xml                          # RSS: all torrents
  torrents.json                     # catalogue of all torrents (NOT index.json: DataIndex owns that name)
  everef-deep-archive-2023-04-11.torrent
  everef-deep-archive-2023-04-11.txt      # every data file in the torrent, one site path per line
  everef-deep-archive-2024-02-20.torrent
  everef-deep-archive-2024-02-20.txt
  ...
```

- **"Already archived" is decided per file:** `01-find` downloads every published `<ID>.txt` (a `.txt` whose name
  isn't a torrent ID is ignored) and skips the paths whose version a torrent already holds (2.2).
- `torrents.json` and the feed are **regenerated in full** from the `.torrent` files in `deep-archive/` each time, never
  edited by appending. The bucket's `deep-archive/` stays the single source of truth.
  - Publication date comes from the torrent's `creation date` field.

#### File list: `<ID>.txt`

For quick review and search without having to parse torrent files:

- Plain text, UTF-8, `\n` line endings, ending with one. **One file path per line**, as the full site path
  (without the torrent's root folder), **sorted** by code point, no duplicates. Data files only: not the torrent's
  README and `.sha256`. Inside the torrent, every data file has this same path under the root folder (2.3). For
  example:
  ```
  ccp/sde/2023/sde-20230101-TRANSLATIONS.zip
  market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-00-00.v3.csv.bz2
  ...
  ```
- Generated **from the `.torrent` itself** after it's made, minus the two root files. `check-torrent` compares it
  with the manifest.
- The format is checked by `file_list_problems` (`lib/workflow.py`): `check-torrent` checks the new list, and
  `01-find` fails if any published list doesn't pass. A list in the wrong format (say, with the torrent's root folder
  in the paths) would otherwise match no site path, and its files could land in a second torrent without any error.
- Paths match the site layout, so someone looking for e.g. "public contracts from March 2024" can `grep` it and
  knows which torrent to grab and which file to select.
- Content-Type `text/plain; charset=utf-8`, set on the object at upload (2.21). At most ~21,000 files makes a list of
  about 1–2 MB. Cloudflare compresses it in transit.

### 2.6 Files inside the torrent: checksums and README

The checksum file and the README ship **inside the torrent**, at the torrent root. They are **not** published
separately on the data site. Anyone who has the data also has the means to verify it.

**Names start with the ID on purpose.** With "Don't create subfolder", root files from every torrent land in the
same directory. Generic names like `SHA256SUMS` or `README.txt` would collide.

#### `<ID>.sha256`

- Standard `sha256sum` format: `<64 hex chars>  <path>` (two spaces). One line per **data** file, sorted. It does
  not list itself or the README.
- Paths are relative to the torrent root, which are the site paths, so the file works in both layouts.
- No verification instructions are published for it, in the README or on the docs page: torrent clients already check
  every piece, and the format is the standard one.
- Hashes come from `manifest.json`, computed by `03-check-load` from the downloaded files.
- Written into `$SEED_DIR/<ID>/` by its own step, `04-make-checksums`, **before** the torrent is made, so it's
  covered by the torrent's piece hashes. `05-check-checksums` then runs `sha256sum -c` over all the data.

#### `<ID>-README.txt`

Plain text, generated from a template in `deep-archive/`. Contents:

1. What this is: an EVE Ref Deep Archive torrent, with data from data.everef.net modified up to the cutoff date in
   its ID, plus the list of datasets.
2. Layout: paths match data.everef.net, plus the note about "Don't create subfolder".
3. Links: docs page (docs.everef.net/datasets/deep-archive.html) and data.everef.net.
4. A line saying `<ID>.sha256` holds the SHA-256 of every data file, in `sha256sum` format.
5. Licence/attribution notes, the same as the data site (CCP data, see `LICENSE-CCP`).

#### Checks

- `05-check-checksums` checks the `.sha256` equals the manifest and runs `sha256sum -c` over the full content,
  before the torrent is made. This re-reads all the data (hours for a full torrent), but it proves the checksum file
  is correct.
- `07-check-torrent` checks the torrent's file list equals the manifest paths plus exactly the two root files.
- `test-download` downloads the `.sha256`, the README and a sample of data files through the torrent, then runs
  `sha256sum -c --ignore-missing` on them.

### 2.7 RSS feed format

RSS 2.0, one `<item>` per torrent, compatible with qBittorrent's RSS auto-downloader and FlexGet/autodl-style tools:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:torrent="http://xmlns.ezrss.it/0.1/">
  <channel>
    <title>EVE Ref Deep Archive</title>
    <link>https://data.everef.net/deep-archive/</link>
    <description>Archived datasets from data.everef.net</description>
    <item>
      <title>everef-deep-archive-2023-04-11</title>
      <link>https://data.everef.net/deep-archive/everef-deep-archive-2023-04-11.torrent</link>
      <guid isPermaLink="false">INFOHASH_V1</guid>
      <pubDate>Wed, 07 Oct 2026 12:00:00 GMT</pubDate>
      <description>EVE Ref Deep Archive, data modified up to 2023-04-11. 980 GB, 21,000 files.
        Datasets: market-orders, public-contracts, ...
        File list: https://data.everef.net/deep-archive/everef-deep-archive-2023-04-11.txt</description>
      <enclosure url="https://data.everef.net/deep-archive/everef-deep-archive-2023-04-11.torrent"
                 length="TORRENT_FILE_SIZE" type="application/x-bittorrent"/>
      <torrent:infoHash>INFOHASH_V1</torrent:infoHash>
      <torrent:magnetURI><![CDATA[magnet:?xt=urn:btih:...]]></torrent:magnetURI>
      <torrent:contentLength>TOTAL_DATA_BYTES</torrent:contentLength>
    </item>
  </channel>
</rss>
```

- `guid` = v1 info-hash, which is stable, so feed readers never pick up the same torrent twice.
- `pubDate` = the torrent's `creation date` (2.5). New items sort to the top.
- The total data size goes in the description and `torrent:contentLength`, so subscribers can filter by size.
- The feed includes **all** torrents (it's small), so new subscribers can pull in the backlog.
- One feed only.
- Content-Type `application/rss+xml`. Short cache max-age, like `DATA_INDEX_CACHE_CONTROL_MAX_AGE`. Both are set on
  the object at upload (2.21).

### 2.8 Selection first: the file list

The **first** thing a workflow does (after `00-preflight`) is decide exactly which files are archived. Everything
after that is checked against that list, never against "whatever is in the directory".

- `01-find` lists the **whole bucket** with `rclone lsjson --recursive --fast-list`, with each file's size and
  modification time (2.2): one B2 request per 1,000 files, minutes for the whole site.
  - **Never selected:** see 2.2 ("Never archived"), plus paths already in a published `.txt` (read from the bucket's
    `deep-archive/`, 2.5).
  - **Too new:** anything modified less than `MIN_AGE_YEARS` (2) years before today.
  - **Cutoff:** group the remaining candidates (`index.json` aside, 2.2) by modification date (UTC). Find the first
    date where including every candidate on or before it, plus their folders' `index.json`, goes over `MAX_BYTES` of
    data or a `.torrent` estimate over `MAX_TORRENT_BYTES`; the cutoff is **the day before** that date. If
    the limits are never reached, the cutoff is the newest allowed date (and the report flags that the torrent could
    be bigger). If the oldest date alone is over a limit, `01-find` fails: a date can't be split.
  - **Torrent ID** = `everef-deep-archive-<cutoff date>`: everything not yet archived, modified on or before that date.
  - Output in `$SCRATCH_DIR/find/`: `files.jsonl` (path, size, last-modified, URL), `listing.jsonl.gz` (the full
    listing the selection came from), `report.txt`, `summary.json`, `find.log`. The report breaks the selection down
    by modification month, year folder in the path, and dataset, and flags selected `index.json` files whose folder
    keeps files on the site, and selected folders with no `index.json` to select.
- **Limits, hard-coded in `lib/workflow.py`:**
  - `MAX_BYTES` = 1 TiB: what the seeder holds per torrent, and what a full download is.
  - `MAX_TORRENT_BYTES` = 10,000,000: the largest `.torrent`, libtorrent's default `max_buffer_size`. The estimate
    runs a few KB high, so a `.torrent` at the limit still loads. This is what limits the file count. `lib/torrent_size.py` estimates the hybrid `.torrent` from the selection's paths and sizes
    as files are added, mirroring libtorrent's layout (v1 entries and pad files, v1 piece hashes, v2 file tree and
    piece layers), plus a 4 KiB allowance for trackers, comment and root files. The per-file part matched libtorrent
    byte for byte in tests. Earlier, a fixed `MAX_FILES` of 20,986 assumed ~300 bytes per file and the piece hashes
    of a full 1 TiB.
- **`files.jsonl` is the selection.** `01-find` can't run twice in a workflow (2.10), so it can't change afterwards.
  To change it on purpose, delete `01-find`'s completion file and run it again.
- `02-load` downloads **exactly** the files in the list from the bucket into `$SEED_DIR/<ID>/` with
  `rclone copy --files-from`: parallel, resumable, and it keeps each file's modification time.
- `03-check-load` checks every listed file is on disk with the listed size, and nothing else is. It then computes
  SHA-256 for every file into `manifest.json` (the selection plus hashes). The manifest feeds the `.sha256`, the
  torrent checks and the backup.
- The delete set comes from the torrent, confirmed file by file against a fresh listing of the bucket by **size
  and modification time** (2.14).
- **No MD5/ETag matching anywhere.** Size and timestamp are enough to detect changes; content integrity from load
  onwards is covered by SHA-256 (manifest, `.sha256`, torrent piece hashes).

### 2.9 Tools

| Step | Tool | Why |
|---|---|---|
| Preflight | `./00-preflight.py` | Fails early on a wrong setup, before anything is selected. |
| Find | `./01-find.py`: `rclone lsjson` of the whole bucket | One B2 request per 1,000 files; sizes and modification times straight from the storage. |
| Load | `rclone copy --files-from` from the bucket to `$SEED_DIR/<ID>/` | Concurrent and resumable. Copies exactly the selection. |
| Make torrent | Python `libtorrent` 2.x (hybrid v1+v2); alternatives in 2.4 "Creating the torrent". Not `mktorrent` (v1 only). | Mature library, same as qBittorrent. Don't write a torrent creator. |
| Seed | `qBittorrent-nox` in Docker, through its Web API | Headless and scriptable, and it's what most feed subscribers use. |
| Upload | `rclone copyto` into the bucket's `deep-archive/`, with Content-Type and Cache-Control (no overwrite), then checking over HTTPS | Same metadata as the Java uploads (`S3Util`). |
| Feed + catalogue | Python: list the bucket's `deep-archive/` and fetch each `.torrent` with rclone, write `torrents.json` and the feed | Rebuilds from the storage every time. |
| Backup | Python building ZIP bundles, uploaded with rclone (destination not decided, 2.11) | Reads `$SEED_DIR`, not the bucket. |
| Delete | `rclone delete --files-from` with the explicit list (never by prefix), soft deletes only | Precise and auditable. |
| Index pages | Existing `data-index` command, full run | Reuse. |

All scripts will run from a Docker image (`deep-archive/Dockerfile`) containing Python, rclone, libtorrent and the
qBittorrent tooling, so nothing needs to be installed on the host. Until then they run directly on the host.

### 2.10 Manual process: script principles

The workflow is **manual by design**: a human runs each step from `README.md` and double-checks the result before
moving on. The scripts support the human; they don't replace their judgement.

- **One script, one step, numbered in order:** `./00-preflight.py`, `./01-find.py`, …
  (`STEPS` in `lib/workflow.py`). Scripts never call the next step. There is no "run everything" script.
- **Settings in `torrents.env`** (`torrents.env.example`), read for anything the environment doesn't set:
  `SCRATCH_DIR`, `SEED_DIR`, `DATA_BUCKET` and the B2 key (`RCLONE_CONFIG_EVEREF_ACCOUNT`,
  `RCLONE_CONFIG_EVEREF_KEY`). Everything else (rclone settings, size limits, `TORRENT_EXCLUDE`, `DATA_BASE_URL`) is
  fixed in `lib/workflow.py`. No
  `--dir` or ID arguments.
- **One workflow per scratch directory.** `$SCRATCH_DIR` holds the work files of the current workflow and must be
  empty when it starts (`00-preflight` checks). The operator deletes it by hand once the workflow is complete.
  Torrent content goes in `$SEED_DIR/<ID>/` instead, which is kept for seeding. Neither directory is inside the
  other.
- **Completion files.** Every step writes `$SCRATCH_DIR/<NN>-<step>.done` (JSON: step, torrent ID, time,
  summary) as the last thing it does, only on success. A step **refuses to start** without the previous step's
  completion file, and refuses if its **own** exists: no step is skipped, and none runs twice by accident. Redoing a
  step is deliberate: delete its completion file and every later one by hand. The torrent ID comes from
  `01-find.done`.
- **Every action has a separate check step.** E.g. `02-load` is followed by `03-check-load`. Checks are read-only,
  re-derive everything from source (bucket, disk, qBittorrent, site), and never trust the action's output.
- **Show, then ask.** Any step that writes outside `$SCRATCH_DIR` and `$SEED_DIR` (the data bucket, Glacier,
  qBittorrent) first prints what it will do (counts, bytes, sample paths) and asks for confirmation. `21-delete`
  needs the torrent ID typed back in, not just `y`.
- **Resumable until complete.** A step that fails or is interrupted writes no completion file and can be run again;
  copies and uploads carry on where they stopped.
- **Output like the roll-up scripts** (`lib/workflow.py`): `# 🚀 <step>: <ID>` once, `## 🔵 <section>` headings,
  plain lines for results, `🔴 <message>` on stderr for each problem (then exit 1), and `✅ <step> OK …` as the last
  line on success. Everything a step prints is also written to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Notes.** The operator keeps `$SCRATCH_DIR/NOTES.md` for notes on manual steps and decisions. It goes with the
  scratch directory; anything worth keeping goes into `LOG.md`.

### 2.11 Backup: S3 Glacier Deep Archive

Every torrent's data is backed up to **S3 Glacier Deep Archive** before anything is deleted. The backup is the copy
EVE Ref controls if the seed directory is lost.

**Why bundles, not one object per file.** Deep Archive charges per request and adds about 40 KB of billed metadata
per object, and restores are requested per object. A torrent with ~21,000 files, many of them small, would be slow and
expensive to upload and to restore one by one. So the files are packed into large bundles.

**Bundle format:** uncompressed (store-only) **ZIP64** files, `<ID>-part-NNN.zip`, about 50 GB each, files in sorted
path order, paths as on the site. The data is already compressed, so store-only costs nothing. ZIP rather than tar
because the same bundles can go to the Internet Archive, which can serve single files out of a ZIP (2.12). If the
Internet Archive isn't used, tar works just as well.

**Layout in the backup bucket:**

```
s3://<backup bucket>/everef-deep-archive/<ID>/
  <ID>-part-001.zip … <ID>-part-NNN.zip    # storage class DEEP_ARCHIVE
  <ID>-backup-index.tsv                     # path, bundle, size, sha256 for every data file
  <ID>.torrent  <ID>.txt  <ID>.sha256  <ID>-README.txt  manifest.json
```

The small metadata files are stored in a normal storage class, so they can be read without a restore. With them, a
restore can fetch just the bundle that holds a given file, and verify it.

**Steps** (README, steps 16–19):

1. `prepare-backup`: plans the bundles from the manifest (`backup-plan.json`: bundle → files) and writes
   `<ID>-backup-index.tsv`. `check-backup-plan` checks every manifest file is in exactly one bundle and the sizes add
   up.
2. `backup`: for each bundle, builds it in `$SCRATCH_DIR/backup/`, records its size and SHA-256, uploads it with storage
   class `DEEP_ARCHIVE` (SHA-256 stored as object metadata), and deletes the local bundle. Then it uploads the
   metadata files. Resumable: bundles already uploaded with matching size and SHA-256 are skipped. Local disk needed:
   one bundle.
3. `check-backup`: lists the backup prefix and checks every planned bundle exists with the recorded size, SHA-256
   metadata and storage class, and that the metadata files match the local ones.
4. **Restore drill** (first run, then occasionally): restore the smallest bundle (bulk retrieval), unzip it, and run
   `sha256sum -c --ignore-missing` against the `.sha256`. This is the only check that reads the backup back.

**Cost** (approximate, check current pricing): storage about $1/TB/month (5.7 TB ≈ $6/month), 180-day minimum per
object. A full restore costs mainly data transfer out (about $90/TB), with bulk retrieval taking up to 48 hours.
Restores are for disasters only; the torrent is the normal way to get the data back.

**Account: not decided.** The scripts have **one B2 key** (2.21) and never use the AWS CLI, so Glacier would need a
second rclone remote with its own AWS key, against the one-key rule. The alternatives are a separate B2 bucket on the
same account (simple, but not independent of B2 or of that key) or no backup beyond the seed directory and community
seeders (open question 2). Whatever the destination, consider Object Lock or at least versioning, so a mistake can't
delete the backup.

### 2.12 Internet Archive

Researched 2026-10-08 (sources at the end of this section).

**What the Internet Archive offers:**

- Free, public, long-term hosting. Uploads are organised into **items**. Every file in an item is downloadable over
  HTTP at `https://archive.org/download/<item>/<file>`.
- It **generates a torrent for each item automatically**, with archive.org as a web seed, so it's always seeded.
  The torrent is regenerated (new info-hash) whenever the item changes.
- It can serve **single files from inside a ZIP** in an item ("View contents").
- It can also fetch a user-supplied `.torrent` into an item, provided seeds are available.

**Limits:**

- About **1 TB per item** (hard limit), recommended at most **500 GB and 1,000 files per item**; more than 10,000 files
  per item causes problems.
- Bulk uploads: recommended at most 500 files or 500 GB per item upload, and 5,000 files a day.

**What that means for EVE Ref:**

- A torrent has up to ~21,000 files and 1 TiB: over the recommended item size and file count, and up to the hard
  1 TB limit. It **doesn't fit one item well**, so the Internet Archive can't mirror the files one-to-one.
- It **can't be a web seed for our torrents.** A web seed (BEP 19) needs every file at `<url>/<torrent name>/<path>`,
  which would need one item holding the whole torrent's files.
- What does fit: the **backup bundles** from 2.11. About 50 GB per bundle, grouped into items of at most ~500 GB
  (e.g. items `everef-deep-archive-2023-04-11-part-001-010`), plus the metadata files. That gives:
  - a **second, free, off-site copy**, independent of AWS and of the seed host;
  - **public HTTP downloads** of the archived data, in bundle form, without any seeders;
  - possibly **single-file downloads** through ZIP "View contents" (needs testing on large ZIP64 files);
  - an always-seeded torrent per item, via the Internet Archive's own web seed.
- Upload is slow at TB scale, and processing can take hours or days. **Deletion must not wait for it.** The Internet
  Archive is an extra, not part of the delete criteria.

**Caveats:**

- It isn't a guaranteed backup: items can be taken down or made dark, and the site has had long outages. Glacier stays
  the authoritative backup.
- Content rules: the uploader must have the right to share the data. Check that CCP's licence (and Fuzzwork, for
  ordersets) allows redistribution through the Internet Archive.
- For uploads at this scale, contact the Internet Archive first.

**Recommendation:** treat it as an optional extra, done after a torrent's Glacier backup, using the same bundles.
Decide after the first workflow (open question). If it's used, the docs page links the items as an HTTP alternative.

Sources: [Archive BitTorrents](https://help.archive.org/help/archive-bittorrents/),
[Uploading – Tips](https://help.archive.org/help/uploading-tips/),
[Bulk uploading](https://help.archive.org/?p=433), [BEP 19](https://www.bittorrent.org/beps/bep_0019.html).

### 2.13 Announcements

There is no notice period, but a notice is posted to the **announcement channel** (Discord) when a workflow
**starts**, right after `01-find`. A second post follows when it's finished.

**At start:**

> **EVE Ref Deep Archive: archiving data up to {CUTOFF}**
>
> We're moving old data on data.everef.net into the **EVE Ref Deep Archive**: BitTorrent archives.
> This one covers files stored up to **{CUTOFF}** that aren't archived yet (about {SIZE}, mostly from {YEARS}).
>
> - Torrents will be published at https://data.everef.net/deep-archive/, with an RSS feed at
>   https://data.everef.net/deep-archive/feed.xml.
> - Once the torrent is published, seeded and backed up, the HTTP copies of these files will be **removed** from
>   data.everef.net. There's no fixed date.
> - `market-history`, `killmails`, `ccp` and all `-latest` files are **not** affected.
>
> **What you might need to do:**
> - If you mirror the data with `rclone sync`, switch to `rclone copy`, or your local copies of these files will be
>   deleted.
> - If you need these files, download them now, or use the torrent once it's out.
> - Already have a mirror? You can seed the torrents from it without downloading anything. Please do!
>
> Details: https://docs.everef.net/datasets/deep-archive.html

**At finish:**

> **EVE Ref Deep Archive: {ID} published**
>
> The torrent for data up to **{CUTOFF}** is out: https://data.everef.net/deep-archive/ (RSS:
> https://data.everef.net/deep-archive/feed.xml). The files have been removed from data.everef.net. Every torrent has
> a `.txt` file list, so you can find what you need and select only those files.
>
> Seeders are very welcome. That's what keeps this data available.
> How to seed and find files: https://docs.everef.net/datasets/deep-archive.html

### 2.14 Deletion design

**Deletion is the last step of a workflow.** It happens only after the torrent has passed publishing, seeding,
testing, docs and backup checks.

- **Only files confirmed to be in the torrent are deleted.** The delete set is built from the **torrent itself**,
  not from the selection, and every file in it is confirmed against a **fresh listing** of the bucket. A data file
  is in the delete set only if all of these hold:
  - it's in the published `.torrent` (`deep-archive/<ID>.torrent`, read back from the bucket, with the same
    info-hash as `$SCRATCH_DIR/<ID>.torrent`), at its site path under the root folder, with the size the torrent
    gives;
  - it's in the manifest, so its SHA-256 was computed from the loaded copy, which the seeder verified against the
    torrent (full recheck at 100%);
  - it's on the bucket now with the **same size and modification time as in the selection**: the same version that
    was loaded. On B2 a new upload always gets a new modification time.
  - it isn't an `index.json` (they're rewritten by `DataIndex`, so they never match; 2.15 handles them), or the
    torrent's README or `.sha256` (not site files).
- A file that fails a check **stays on the site**, and the step goes on with the rest. It's reported, and noted in
  `NOTES.md`. A file that changed after it was selected has a newer modification date, so `01-find` picks it up for a
  later torrent once it's old enough (2.2, Appendix A). Many failures point at a problem rather than at changed data:
  look before executing.
- Files in the same folders that aren't in the torrent (uploaded after the cutoff) **stay**. Only confirmed keys are
  ever deleted.
- Deletes are an explicit list (`rclone delete --files-from`), never a prefix or a pattern.
- **Treat deletion as permanent.** On B2, rclone's delete **hides** the file: the `everef` remote has `hard_delete`
  fixed to `false` (2.21), and the bucket's lifecycle rule purges hidden files, currently after **2 days**. The new
  storage may have no soft delete at all. So the process doesn't rely on it:
  - rollback is a re-upload from `$SEED_DIR/<ID>/` (or a backup restore), using the published `<ID>.txt` as the list;
  - `undelete` is a storage-specific extra (B2: remove the hide markers in B2's web console), only used if the window
    is still open. It isn't part of the scripts.
- **Dry run by default.** The step prints the file count, total bytes and a sample of keys, and only acts with an
  explicit `--execute`. The dry run doesn't complete the step.
- Log every deleted path (with size and modification time) to `$SCRATCH_DIR/deleted.jsonl` for audit and rollback.
- Folders are virtual on B2: they disappear with their last file. Index files left in emptied folders are removed
  right after the data (2.15).
- **Pause jobs that crawl the data site** (`sync-fuzzwork-ordersets`, the market history scrape) from the delete
  until the full `DataIndex` has finished (2.17).

### 2.15 Stale index files after deletion

`DataIndex` regenerates the parent directories correctly (the deleted year vanishes from them), but it **leaves the
old `index.html`/`index.json` in every emptied directory**. So `21-delete` cleans them up, right after the data:

- It lists the bucket again, and checks every folder that held a deleted file, and each of its parent folders up to
  the site root, deepest first.
- A folder holding **nothing but `index.html` and `index.json`** (no other file, at any depth) loses those index
  files. Deepest first, so a parent left with only index files once its children are gone is cleaned up too.
- Only `index.html` and `index.json` are deleted this way, by explicit key. Any other file keeps its folder, and all
  its parents, as they are.
- Never the site root, and never anything under `deep-archive/`.
- Logged to `$SCRATCH_DIR/deleted.jsonl` like the data, marked as index cleanup.
- The full `DataIndex` that follows rewrites the parents, so they stop listing the removed folders.

Follow-up: make `DataIndex` itself delete index files in folders with no data, so stale indexes can't build up from
other deletes either. It's a Java change, and it needs care so it never deletes the root index.

### 2.16 Cloudflare cache

**No cache purge.** Archive files are served with a 30-day max-age (`DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE`), so
Cloudflare can keep serving a deleted file from cache for up to 30 days. Index pages have a 2-minute max-age, so they
update almost at once. That's accepted.

`23-check-site` therefore checks **many** URLs, not one: a random sample of deleted files spread across datasets and
months (e.g. 50), plus the `index.json` of emptied folders. Per URL:

- `404`: pass.
- `200` with `cf-cache-status: HIT`: pass, reported as "still cached". It will expire.
- Anything else (`200` from the origin, `5xx`, …): **fail**. The origin still serves the file, or something is broken.

### 2.17 Code and users that depend on the deleted data

Must be handled **before** deleting the affected datasets:

| Consumer | Problem | Mitigation |
|---|---|---|
| `SyncFuzzworkOrdersets` | Re-syncs every orderset missing from the data site, so it **would re-upload the deleted files**. Fuzzwork's listing goes back a long way (IDs 90019–174817 on 2026-10-08). | ID cutoff (2.18). |
| `SyncMer` | Rescans from `MER_FIRST_MONTH` if no MER files exist. | `ccp/` is never archived. |
| `DataCrawler` | Today it logs and returns nothing when an index page is missing. Decision: make it **fail** instead (revert if it causes problems). Between the delete and the full `DataIndex`, parent indexes still link to removed directories, so crawling jobs will fail. | Pause crawling jobs during that window (2.14). |
| `ScrapeMarketHistory` / `import-market-history` | Re-read up to 450 days of `market-history/`, and the full history. | `market-history` is never archived. |
| `HistoricalOrdersRegionTypeSource` | Reads the last 30 days of market orders (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`). | Safe with the 2-year minimum age. |
| Docs examples | `downloading-datasets.md` uses `market-orders/history/2023/` as its example; `structures.md` links 2024 files. They'd 404. | Change the downloading example to `https://data.everef.net/market-history/2026/` (never deleted). Change the structures examples to recent files, or remove them. |
| Users following `downloading-datasets.md` (`rclone sync`) | **`rclone sync` deletes their local copies** of files deleted on the site. | Change the docs to `rclone copy` before the first deletion. The start announcement repeats it. |
| Users with `wget -N` / custom scripts | Get 404s. | Start announcement, docs page. |

### 2.18 Sync cutoff for Fuzzwork

The sync commands must never fetch data the archival process removed. The change must be **deployed before the
first deletion** of `fuzzwork/` data (README, "Setup"). Tests first (repo convention).

**`SyncFuzzworkOrdersets`, decided: cutoff at the lowest ID on the data site.**

- It decides what to sync by **sequence ID**, comparing Fuzzwork's listing with the IDs found on the data site. The
  year of an orderset is only known after downloading it, so a date cutoff would mean re-downloading every archived
  orderset on every run.
- New rule: skip any Fuzzwork ID **below the lowest ID currently on the data site**. Gaps above it are still filled.
- **Count only the dated layout** (`fuzzwork/ordersets/YYYY/…`), not `backfills/`. The backfills (IDs 21000–59999)
  have no year folder and may be archived before or after the dated ordersets (by upload date, 2.2), and Fuzzwork's
  listing starts at 90019 anyway. If the backfills counted while still on the site, the lowest ID would be 21000
  and the cutoff would never apply.
- **Guard:** if the data site has no dated ordersets at all, fail instead of syncing everything.
- Fuzzwork sequence IDs are incremental (confirmed), and ordersets are uploaded in ID order, so the ID cutoff follows
  the archive cutoff automatically: once older ordersets are deleted, the lowest remaining ID moves up.

**`SyncMer`: no change.** `ccp/` is never archived (2.2), so MER files are never deleted. (An earlier draft had a
guard for "no MER files at all, so re-sync from `MER_FIRST_MONTH`", and before that an `ARCHIVE_MIN_AGE_YEARS`
setting; both dropped.)

### 2.19 Durability

Copies of each torrent's data after deletion:

1. **The seed directory (`SEED_DIR`)**, on redundant storage, checked regularly (README, "Requirements" and
   Appendix C, "Maintenance").
2. **The Glacier Deep Archive backup** (2.11), restorable if the seed directory is lost.
3. **Community seeders**, encouraged through the RSS feed, rclone-mirror seeding and the announcements.
4. Optionally the **Internet Archive** (2.12).

Any soft-delete window on the storage is not counted. The data can never be re-scraped.

### 2.20 Discoverability after deletion

Archived files disappear from the listings, so someone browsing the site won't see that the data exists as a
torrent.

- **The docs site** explains the archive rules and links the torrents (4.2). It's set up once, before the first
  deletion.
- Optional: enhance `DataIndex` to read `deep-archive/torrents.json` and render an "Archived data" note on each
  dataset's index page (e.g. "Older files: see the Deep Archive"), linking to the torrents and their `.txt` lists.

### 2.21 Storage access: rclone only

Revised 2026-10-10. The scripts reach storage **only through rclone**, with **one B2 key** and one remote,
`everef`. **Never the AWS CLI or an S3 library.** Same setup as the roll-up scripts:

- rclone is configured **only from `RCLONE_CONFIG_*` environment variables**. `lib/workflow.py` sets
  `RCLONE_CONFIG=/dev/null`, so the operator's own `rclone.conf` and its remotes are out of the picture.
- Only the secrets come from `torrents.env`: `RCLONE_CONFIG_EVEREF_ACCOUNT` and `RCLONE_CONFIG_EVEREF_KEY`. The rest
  is fixed in `lib/workflow.py` and overrides the environment: type `b2`, and `hard_delete=false`, which must never be
  true: deletes stay soft, so the bucket's lifecycle rules provide the undo window (2.14).
- The bucket is a setting, `DATA_BUCKET` in `torrents.env` (like `ROLLUP_BUCKET` in the roll-up scripts). The site
  root is `everef:$DATA_BUCKET`.

One key means every step could write and delete. The safeguards are the steps themselves: only `11-upload-torrent`,
`13-build-feed` and `21-delete` write, only to explicit paths (or `deep-archive/`), after showing what they'll do and
asking. (The 2026-10-09 design used a filesystem mount; the first draft of this revision used boto3, then separate
read, write and backup remotes. All dropped.)

- **No mount, no public-HTTP crawl** for decisions. HTTPS against data.everef.net is only used to check what the site
  serves (`12-check-upload`, `14-check-feed`, `23-check-site`). The other remote API is qBittorrent.
- **Listings are always fresh.** An rclone listing goes straight to the storage, so `01-find`,
  `20-check-before-delete`, `21-delete` and `22-check-delete` see what EVE Ref's jobs wrote a moment ago.
- **The modification date is rclone's `ModTime`** everywhere: selection (2.2), load (file mtimes), pre-delete check
  (2.14). On B2 it's the upload time, unless the uploader set `src_last_modified_millis` (check on real files).
- **Content-Type and Cache-Control** go on the files at upload, like `S3Util` does today: `.torrent` →
  `application/x-bittorrent`, `.txt` → `text/plain; charset=utf-8`, `feed.xml` → `application/rss+xml` with a short
  max-age, `torrents.json` → short max-age. With rclone that's `--header-upload`; **check B2 honours both headers**
  through rclone before `11-upload-torrent` is written.
- The writing steps only ever touch paths from an explicit list (or `deep-archive/`).

**Throughput and cost.** Listing ~1 million files is ~1,000 B2 list requests (class C transactions, cents).
`--fast-list` keeps it to that. Loading up to 1 TiB per torrent goes through B2's egress; check its pricing (egress to
Cloudflare is free, direct egress has an allowance).

**Dependencies outside this process.** `DataIndex` and the sync jobs use the same storage through `S3Util`. Don't
start a workflow in the middle of the storage migration: the selection, the delete and the full `DataIndex` must all
see the same storage.

---

## 3. The process

The step-by-step process lives in [`README.md`](README.md). This document holds the design and reasoning
behind it.

## 4. Documentation

| Audience | Where | What |
|---|---|---|
| Operator (us) | `deep-archive/*.md` in this repo, alongside the scripts | How to run the workflow: `README.md`; `PLAN.md` (design, temporary). |
| Users of the data | docs.everef.net (`docs/src/…`) | What the torrents are, why they exist, how to use them, and when new ones appear. |

The docs site does **not** describe the internal workflow (scripts, checks, storage, backups). It links to the
`deep-archive/` directory on GitHub for anyone curious.

### 4.1 Operator docs (`deep-archive/`)

```
deep-archive/
  README.md             # setup and the step-by-step manual process
  PLAN.md               # design decisions and reasoning; goes away once the process is settled
  LOG.md                # one line per published torrent
  trackers.txt
  torrents.env.example  # copy to torrents.env (not committed)
  README-template.txt   # source for the README inside each torrent
  run.sh                # Docker wrapper (planned)
  Dockerfile            # (planned)
  00-preflight.py       # the workflow's steps: 00-preflight.py, 01-find.py, 02-load.py, ...
  lib/                  # shared code: workflow.py
```

Keep the README and scripts in sync: any script change that changes output or behaviour updates `README.md` in the
same commit. This follows the repo's "docs never drift" convention, and `AGENTS.md` should mention the directory.

### 4.2 Docs site: new page `docs/src/datasets/deep-archive.md` ("EVE Ref Deep Archive")

Written once. **Rule-based rather than listing specific torrents**, so it doesn't need editing for every workflow.
The live list of torrents is the `deep-archive/` directory and the feed.

1. **What it is.** BitTorrent archives of data.everef.net datasets, `everef-deep-archive-YYYY-MM-DD.torrent`. Each one
   holds the oldest data that wasn't in an earlier torrent, from every archived dataset, up to the date in its name,
   in the same paths as on the site. Each torrent is as big as client limits allow (up to ~21,000 files, 1 TiB).
2. **Why they exist.** data.everef.net keeps growing (5.7 TB+). Old data is moved off the HTTP site to keep hosting
   sustainable, and torrents keep it available, with the community helping to host it. Be honest about the
   trade-off: old data is only available as long as someone seeds it, and seeding is appreciated.
3. **What gets archived, and what doesn't.**
   - Files stored on the site at least 2 years ago, oldest first.
   - Once a torrent is published, its files are **removed from data.everef.net**.
   - Never archived (always on HTTP): `market-history` and `killmails` (they work as a database and change in
     place), `ccp` (CCP's own reference data), and `*-latest` files.
4. **When new torrents appear.** Ad hoc, whenever enough data has passed the 2-year age to fill a torrent. There is
   no fixed date. Until a file is archived, it stays on data.everef.net. Each new torrent is announced on Discord
   when work on it starts. Subscribe to the feed to be notified of new torrents.
5. **How to use them.**
   - Links: [`deep-archive/`](https://data.everef.net/deep-archive/) directory and the RSS feed `feed.xml`.
   - **Finding specific data:** every torrent has a matching `.txt` listing every file. Search them, then add the
     torrent and select only the files you need.
   - **Getting the site layout:** content layout "Don't create subfolder" (qBittorrent) puts files in the same
     paths as on data.everef.net, and multiple torrents merge into one tree, matching an `rclone` mirror.
   - **Already have an rclone mirror?** Add the torrent with "Don't create subfolder", point it at your mirror root,
     and let it recheck. You start seeding without downloading anything (except possibly a few `index.json` files,
     if your mirror has newer versions).
   - **Helping to seed (auto):** qBittorrent → RSS → add the feed URL → RSS Downloader rule matching everything,
     auto-download → new archives are picked up as they're published. Size warning: each torrent can be up to 1 TiB,
     and the full archive is TBs.
6. **Integrity.** Torrent clients verify every piece. Each torrent also contains `<ID>.sha256`, the SHA-256 of every
   data file in `sha256sum` format; no instructions beyond that.

Related one-time changes, all live **before the first deletion**:
- `docs/src/datasets/downloading-datasets.md`: switch `rclone sync` to `rclone copy`, and explain that old years get
  removed from the site and `sync` would delete your local copies. Change the example path
  `market-orders/history/2023/` to `https://data.everef.net/market-history/2026/` (never deleted). Link the Deep
  Archive page.
- `docs/src/datasets/structures.md`: replace the two 2024 example links.
- `docs/src/datasets/index.md`: add the Deep Archive page, and a note that old years are archived.
- Dataset pages: one generic line, "Files older than 2 years are archived as torrents", with a link.
- Docs sidebar/navigation entry for the new page.

### 4.3 Per workflow

The docs are rule-based, so normally **nothing changes**. Step `15-check-docs` is a review: confirm the page is still
accurate (excluded datasets, timing, links), and update it only if the rules changed.

---

## 5. Changes outside the scripts

- **This repo:**
  - Docs site (4.2): the Deep Archive page and related edits, before the first deletion.
  - Operator docs in `deep-archive/` (4.1), kept in sync with the scripts.
  - `SyncFuzzworkOrdersets` ID cutoff (2.18). Deployed before the first deletion.
  - `DataCrawler` fails on a missing directory index (2.17).
  - Optional `DataIndex` changes: the "Archived data" note (2.20) and stale index cleanup (2.15b).
  - `deep-archive/` scripts and `Dockerfile`.
  - An `AGENTS.md` update describing the `deep-archive/` directory and process.
- **Cloudflare Worker (separate repo), or whatever serves the new storage:**
  - Pass through the objects' Content-Type and Cache-Control (2.21).
  - Optionally make deleted archive paths return a 404 page that links to the Deep Archive page.
  - Make sure no bot protection blocks RSS readers or torrent clients fetching `.torrent` files.
- **B2 key (2.21):** one key for the data bucket, in `torrents.env` on the host that runs the scripts.
- **Backup destination** (2.11): not decided.

---

## 6. Open questions

Decided so far: one torrent per workflow, as big as the size limits allow (2.1, 2.8); hard-coded limits
(a `.torrent` estimate of at most 10 MB, `MAX_BYTES` 1 TiB, 32 MiB pieces); selection by modification date with a 2-year minimum age;
torrent IDs from the cutoff date; exclusions `market-history`, `killmails` and `ccp`; Fuzzwork in scope with a sync
cutoff; Fuzzwork cutoff at the lowest dated ID on the site; checksums and README inside the torrent; hybrid v1+v2; no
notice period, but a start announcement; deletion last; no MD5 matching; no Cloudflare
purge; `DataCrawler` fails on missing indexes; a manual process with numbered scripts and completion files in
`deep-archive/`; storage is reached only through rclone with one B2 key, soft deletes only, never the AWS CLI
(2.21); a date too busy to fit one
torrent aborts `01-find` and is resolved by hand.

Still open:

1. **Is rclone's `ModTime` on B2 the upload time** for files uploaded by EVE Ref's Java code (2.2)? And does B2
   honour `Content-Type` and `Cache-Control` set through rclone's `--header-upload` (2.21)? Check on real files.
2. **Backup destination** (2.11): with one B2 key and no AWS CLI, Glacier needs a second key. A separate B2 bucket
   on the same account, Glacier with its own key after all, or no backup? Then: Object Lock or versioning? Bundle size
   (~50 GB proposed)?
3. Bundle format: ZIP64 store-only (Internet-Archive-friendly) or tar?
4. Internet Archive: use it at all? Is redistribution there allowed for CCP and Fuzzwork data? Item grouping?
5. Seeder host: where is it, how much disk and upload, and does it have a redundant filesystem?
6. Where to run `10-test-download` so it actually tests reachability from outside (another machine on another
   network)?
7. Stale index cleanup: in the delete step (2.15a), in `DataIndex` (2.15b), or both?
8. Add the `DataIndex` "Archived data" note (2.20)?
9. **Small-snapshot datasets** (Appendix B): ~553,700 files (69 %) for ~6 GiB; at ~21,000 files per torrent they'd
   fill ~26 torrents with almost no data. Being addressed separately: a script that rolls whole years of small files
   up into a single highly compressed file. Still open: which "no year folder" directories must stay on HTTP
   (`characters-corporations-alliances/backfills`, …)? (`ccp/` is excluded.)
10. Which Discord channel gets the announcements, and who posts them?
11. Trackers: add all from both lists, a curated set, or the core only (2.4 "Trackers")? Refresh tracker lists in
    published `.torrent` files later?
12. Torrent creation tool: Python `libtorrent` or the qBittorrent Web API creator (2.4 "Creating the torrent")?
13. **Is deleting still worth it on the cheaper storage?** The saving is ~4 TiB at the new storage's price per month.
    The cost is a seeder host with 4 TiB+ of redundant disk, the Glacier backup, operator time, and data that's only
    available while someone seeds it. If the saving is small, a middle option is to publish torrents and back up
    (steps 00–19) without deleting: users get torrents, and nothing is removed. Decide once the new storage's pricing
    is known.
14. Does the new storage have soft delete, snapshots or versioning? If so, use it as an extra safety net around
    `21-delete` (2.14 doesn't rely on it).
15. Timing against the storage migration: run the first workflows before, after, or as part of it? Archiving first
    means ~4 TiB less to migrate; migrating first means the process runs on the final storage.

---

## Appendix A: If archived data turns out to have changed

If the pre-delete check (`20-check-before-delete`) finds that a file changed on the storage after it was selected:
- **Don't delete that file.** It isn't the version in the torrent. Everything else that's confirmed is deleted as
  usual (2.14).
- The torrent stays as it is: it holds the version from when it was made. No replacement torrent.
- The changed file has a new modification date, so a later `01-find` selects it for a new torrent once it's old
  enough (2.2). The same path is then in two torrents, one version each.
- Find out why it changed. If the dataset's old files still change, consider adding it to `TORRENT_EXCLUDE`.

---

## Appendix B: First find run on real data (2026-10-08)

The earlier, year-based draft of `find` (`--year 2025`) against a local backup. Selection now works by date and
size limits (2.8); these numbers are kept as measurements. The per-year breakdown from the report (year from the path,
or the modification time for files without a year in the path):

| Year | Files | Size |
|---|---:|---:|
| 2006 | 2 | 309.1 MiB |
| 2009 | 1 | 610 B |
| 2010 | 11 | 30.0 MiB |
| 2011 | 2 | 1.9 MiB |
| 2012 | 1 | 296 B |
| 2013 | 1 | 182.9 KiB |
| 2014 | 4 | 14.5 GiB |
| 2015 | 1 | 229.6 KiB |
| 2016 | 28 | 558.8 MiB |
| 2017 | 4,118 | 101.8 GiB |
| 2018 | 14,733 | 349.1 GiB |
| 2019 | 5,719 | 134.4 GiB |
| 2020 | 14,892 | 345.4 GiB |
| 2021 | 33,172 | 527.8 GiB |
| 2022 | 60,375 | 696.9 GiB |
| 2023 | 264,988 | 750.1 GiB |
| 2024 | 282,463 | 792.6 GiB |
| 2025 | 119,534 | 337.9 GiB |
| **Total** | **800,045** | **≈ 3.96 TiB** |

Dataset breakdown from the same run ("(by mtime)": no year in the path, selected by modification time):

| Dataset | Files | Size |
|---|---:|---:|
| `ccp/ccp_quant` (by mtime) | 1 | 2.0 GiB |
| `ccp/csm` (by mtime) | 17 | 11.1 MiB |
| `ccp/iec` (by mtime) | 135 | 12.4 GiB |
| `ccp/mer` | 131 | 6.2 GiB |
| `ccp/portraits` (by mtime) | 1 | 14.3 GiB |
| `ccp/qen` (by mtime) | 11 | 31.9 MiB |
| `ccp/sde` | 185 | 16.2 GiB |
| `ccp/sde` (by mtime) | 109 | 11.0 GiB |
| `ccp/sde/older` (by mtime) | 3 | 268.0 MiB |
| `characters-corporations-alliances/backfills` (by mtime) | 3 | 1.4 GiB |
| `esi-scrape` | 801 | 56.8 GiB |
| `faction-warfare-leaderboards` | 1,284 | 603.7 KiB |
| `faction-warfare-leaderboards-characters` | 1,284 | 3.5 MiB |
| `faction-warfare-leaderboards-corporations` | 854 | 451.4 KiB |
| `faction-warfare-stats` | 1,284 | 621.2 KiB |
| `faction-warfare-systems` | 42,709 | 50.9 MiB |
| `faction-warfare-wars` | 1,279 | 435.0 KiB |
| `freelance-jobs` | 1,053 | 75.5 MiB |
| `fuzzwork/ordersets` | 85,464 | 1.9 TiB |
| `fuzzwork/ordersets/backfills` (by mtime) | 23,854 | 560.4 GiB |
| `hoboleaks-sde` | 372 | 221.2 MiB |
| `incursions` | 252,357 | 125.9 MiB |
| `incursions/history/backfills` (by mtime) | 1 | 1.6 MiB |
| `industry-facilities` | 21,460 | 262.7 MiB |
| `industry-systems` | 21,446 | 528.4 MiB |
| `industry-systems/history` (by mtime) | 5 | 668.3 MiB |
| `insurance-prices` | 21,446 | 400.7 MiB |
| `market-orders` | 67,833 | 1.1 TiB |
| `markets-prices` | 3,440 | 536.5 MiB |
| `public-contracts` | 66,967 | 322.2 GiB |
| `public-contracts/history/backfills` (by mtime) | 5 | 1.7 GiB |
| `reference-data` | 757 | 8.9 GiB |
| `shadow-war-arc-goals` | 32,132 | 12.4 MiB |
| `shadow-war-arc-goals/history` (by mtime) | 1 | 39.5 KiB |
| `sovereignty-campaigns` | 21,249 | 17.1 MiB |
| `sovereignty-campaigns/history` (by mtime) | 5 | 13.3 MiB |
| `sovereignty-map` | 21,214 | 367.4 MiB |
| `sovereignty-map/history` (by mtime) | 5 | 294.8 MiB |
| `sovereignty-structures` | 21,153 | 1003.9 MiB |
| `sovereignty-structures/history` (by mtime) | 5 | 1.1 GiB |
| `structures` | 20,003 | 1.6 GiB |
| `system-jumps` | 21,064 | 280.8 MiB |
| `system-jumps/history` (by mtime) | 5 | 267.5 MiB |
| `system-kills` | 21,010 | 213.8 MiB |
| `system-kills/history` (by mtime) | 5 | 199.2 MiB |
| `wars` | 1,587 | 153.6 MiB |
| `wars/history` (by mtime) | 18 | 347.8 MiB |
| `warzone` | 20,878 | 35.4 MiB |
| `warzone-insurgency` | 3,160 | 5.4 MiB |
| **Total** | **800,045** | **≈ 4.0 TiB** |

Not recorded here yet: the report's flags and `.torrent` estimate.

### Measured: mktorrent, 296,922 files (2026-10-08)

A test torrent made with `mktorrent` over **296,922 files** came out at **27 MiB**, about **95 bytes per file**.
Piece size and total data size weren't recorded.

- `mktorrent` makes **v1 only**, so this is a lower bound. A hybrid v1+v2 torrent adds a pad file and a v2 file-tree
  entry per file and will be larger. Measure it with `create_torrent.py`.
- Already **over libtorrent's 10 MB `.torrent` default** (2.4), even as v1. Just under the ~31.5 MB magnet metadata
  limit, but a hybrid version almost certainly won't be.
- The file count is about the size of one large year (2023: 264,988; 2024: 282,463), so it backs up the conclusion
  below: a large year doesn't fit libtorrent's defaults as one torrent.

### Measured: `create_torrent.py`, 13 files (2026-10-08)

Small hybrid v1+v2 test with `create_torrent.py` (libtorrent 2.x, 16 MiB pieces):

```
Files:     13 (148.7 MiB)
Pieces:    18 of 16.0 MiB
.torrent:  3.6 KiB (3,666 bytes)
Per file:  282 bytes
Loads with libtorrent default limits: yes
Done in 1s
```

- **Opens in qBittorrent without problems.** The script makes valid hybrid torrents.
- **18 pieces for 148.7 MiB** (about 10 if packed end to end) shows the hybrid alignment: every file starts on a
  piece boundary.
- **282 bytes per file isn't a per-file figure.** With 13 files, the fixed overhead (info dictionary, piece hashes)
  dominates. Run it on the same ~297,000 files as the mktorrent test to compare hybrid with v1 directly.
- **Seeding from existing files works.** The content directory was copied as it was into qBittorrent's incomplete
  torrents directory, then the torrent was added. qBittorrent found the files, checked them, marked the torrent
  complete and started seeding, without downloading anything.
  - For the real archive, copying terabytes into the incomplete directory isn't practical. `08-seed` instead adds
    the torrent with its save path set to `$SEED_DIR`, so qBittorrent checks the files where they are.
    **Still to test.**

### Measured: `create_torrent.py`, 43,620 files (2026-10-08)

Hybrid v1+v2 with `create_torrent.py` (16 MiB pieces) over a directory of small files:

```
Files:     43,620 (45.8 MiB)
Pieces:    43,620 of 16.0 MiB
.torrent:  10.8 MiB (11,372,274 bytes)
Per file:  261 bytes
Loads with libtorrent default limits: NO (metadata too large [libtorrent:100])
Done in 16m 51s
```

- **qBittorrent didn't open it at all: the WebUI hung.** That's at only 10.8 MiB and 43,620 files, far below a large
  year (265,000–282,000 files). Whatever qBittorrent's own limits are, about 44,000 files is already too many in
  practice.
- **Over libtorrent's default limit**, as expected: 11.4 MB > 10 MB.
- **261 bytes per file for hybrid**, against about 95 for v1 (the mktorrent test above): **about 2.7×**. The `find`
  estimator's ~300 bytes per file is a little high, but close.
- **One piece per file**: every file is far smaller than 16 MiB, and hybrid alignment gives each one its own piece.
  For small files the piece size doesn't affect the `.torrent` size.
- At 261 bytes per file, libtorrent's 10 MB default allows about **38,000 files** per hybrid torrent. Given the
  qBittorrent hang, the practical limit is probably lower; test a smaller count to find where it opens.
- **Small files are the problem, not bytes.** 45.8 MiB of data (about 1 KiB per file) produced a 10.8 MiB
  `.torrent`, a quarter of the data size. This is exactly the small-snapshot kind of dataset, and supports excluding
  those datasets from the archive (see "What the dataset breakdown shows" below).
- 16m 51s for 46 MiB of data is slow; the time goes on per-file overhead, not hashing bytes.


### Commentary

**Totals.** About 800,000 files and 4 TiB, roughly three quarters of the site. That matches the expectation that the
first run is most of the 5.7 TB. The seeder needs at least 4 TiB free for the content, plus a backup bundle.

**Torrent size is the deciding problem.** Using the ratio from the `find` estimator (≈ 300 bytes and ≈ 27 bdecode
tokens per file for a hybrid torrent), everything in one torrent would be ~240 MB and ~22 M tokens, over every limit
in 2.4, and large single years (120,000–280,000 files) would be 36–85 MB each. Staying under libtorrent's 10 MB
default means roughly 33,000 files per torrent at most, before any margin.

**Outcome (2026-10-10):** size-limited torrents (2.8). Each torrent takes the oldest data up to the file and byte
limits (20,986 files, 1 TiB), which fits every default limit with margin. Options considered and not taken: aiming
at qBittorrent's raised limits (other clients refuse them), v1-only torrents (2.4 decided hybrid), and bundling
small files into ZIPs inside the torrent (breaks the site layout and mirror seeding).

The dataset breakdown (below) still matters: most of the file count is small-snapshot datasets that are almost free
to keep on B2, and under a file limit they'd take up most of the torrents (open question 9).

**Things that look odd and need checking:**

- **2023 file count jumps 4.4×** (60,375 → 264,988) while bytes barely grow (697 → 750 GiB): many more, smaller
  files. Most likely explained by the dataset breakdown: small-snapshot datasets (incursions alone is 252,357
  files), probably mostly started around 2023 (a per-dataset-per-year breakdown would confirm).
- **2025 is less than half of 2024** (119,534 vs 282,463 files). Is the backup complete for 2025, or did collection
  change during 2025? The first real `01-find` answers it: it lists the bucket itself, not the backup.
- **2019 dips** (5,719 files vs ~14,700 in 2018 and 2020). A gap in collection, or a dataset that stopped and
  restarted?
- **2006–2016: 51 files, ~15.4 GiB**, including 4 files of 14.5 GiB in 2014 and 2 of 309 MiB in 2006. EVE Ref data
  doesn't go back that far, so these are selected by **modification time** (no year in the path): CCP files that
  kept their upstream timestamp. The dataset breakdown points at `ccp/portraits` (1 file, 14.3 GiB), `ccp/iec`,
  `ccp/ccp_quant`, `ccp/sde` and `ccp/sde/older`, `ccp/csm`, `ccp/qen`. See below.
- **2017 (4,118 files)** is probably where EVE Ref's own collection starts, plus the Fuzzwork backfills (selected by
  modification time, 2017).

### What the dataset breakdown shows

**Two kinds of data.** Grouping datasets by average file size:

| Kind | Datasets | Files | Size |
|---|---|---:|---:|
| Small snapshots (average file < 1 MiB) | `incursions`, `faction-warfare-*`, `shadow-war-arc-goals`, `industry-*`, `insurance-prices`, `sovereignty-*`, `structures`, `system-*`, `warzone*`, `markets-prices`, `freelance-jobs`, `wars` | ~553,700 (69 %) | ~5.8 GiB (0.14 %) |
| Everything else | mainly `fuzzwork/ordersets` (+ backfills), `market-orders`, `public-contracts`, `esi-scrape`, `reference-data`, `ccp/*` | ~246,300 (31 %) | ~4.0 TiB (99.86 %) |

- **The torrent-size problem is almost entirely the small snapshots.** `incursions` alone is 252,357 files for
  126 MiB (~500 bytes per file). With `faction-warfare-systems` (42,709), `shadow-war-arc-goals` (32,132) and about a
  dozen ~21,000-file hourly datasets, the small snapshots are 69 % of the files and 0.14 % of the bytes.
- **Archiving them saves nothing.** 5.8 GiB on B2 costs a few cents a month. The point of the Deep Archive is
  storage cost, and these aren't it.
- **Without them**, the whole catch-up is ~246,000 files: about 12 torrents at 20,986 files each, rather than ~38
  with them.
- **The big three** are `fuzzwork/ordersets` (85,464 files, 1.9 TiB, plus 23,854 backfill files, 560 GiB),
  `market-orders` (67,833, 1.1 TiB) and `public-contracts` (66,967, 322 GiB). Together ~244,000 files and ~3.9 TiB:
  that's the archive.

**Recommendation:** add the small-snapshot datasets to `TORRENT_EXCLUDE`. They stay on HTTP; the archive drops from
800,000 to ~246,000 files at the cost of keeping ~6 GiB on B2 (growing slowly). This also keeps those datasets
complete on the site, which suits how they're used (time series of small JSON files). A rule could replace the list:
"don't archive datasets whose average file is under N KiB", but an explicit list is easier to review. This would also
change the "Never archived" list (2.2) and the docs page (4.2).

**The "(by mtime)" entries**, files with no year folder in the path (24,189 files, ~606 GiB; 560 GiB of it the
Fuzzwork backfills). Each needs a decision (README, "Review the selection"):

| Entry | Files | Size | Comment |
|---|---:|---:|---|
| `fuzzwork/ordersets/backfills` | 23,854 | 560.4 GiB | Old historical data, never changes. Archive. |
| `ccp/portraits` | 1 | 14.3 GiB | Likely the 2014 file. A single CCP image dump; static. Archive, or keep as a reference download? |
| `ccp/iec` | 135 | 12.4 GiB | CCP Image Export Collections. Linked from the docs datasets index. Reference material people download by URL: **probably keep on HTTP**. |
| `ccp/sde` (no year folder) | 109 | 11.0 GiB | Old SDE files outside the year folders. Linked from docs (`sde.md`, `build-ref-data.md`). Check what they are before archiving. |
| `ccp/sde/older` | 3 | 268.0 MiB | Linked directly from `docs/src/datasets/sde.md`. Keep, or fix the docs. |
| `ccp/ccp_quant` | 1 | 2.0 GiB | Linked from the docs datasets index. Probably keep. |
| `ccp/csm`, `ccp/qen` | 17, 11 | 11.1 MiB, 31.9 MiB | CSM minutes and Quarterly Economic Newsletters; `csm.md` docs page. Tiny: **keep on HTTP**. |
| `characters-corporations-alliances/backfills` | 3 | 1.4 GiB | Linked from the docs datasets index. Check. |
| `public-contracts/history/backfills` | 5 | 1.7 GiB | Backfill. Archive. |
| `incursions/history/backfills` | 1 | 1.6 MiB | Linked from `incursions.md`. Goes with `incursions` (exclude). |
| `<dataset>/history` (no year folder): `industry-systems`, `sovereignty-*`, `system-jumps`, `system-kills`, `wars`, `shadow-war-arc-goals` | 5–18 each | ~4.1 GiB in total | Files directly under `history/`: probably an older layout or bulk backfills. Check what they are; most belong to small-snapshot datasets (exclude with them). |

**Decided: all of `ccp/` is excluded** (2.2), including `ccp/mer` and the dated `ccp/sde` folders, not just the
reference collections above: ~62 GiB in all, cheap to keep, linked from docs and likely downloaded by URL. CCP's data
is a different kind of data from the scraped history, which is what the archive is for. The `ccp/*` rows above are
no longer selected.

**Follow-ups for `01-find`:**

- An "average file size" column per dataset, to make small-snapshot datasets obvious.
