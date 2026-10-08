# EVE Ref Deep Archive — Process & Planning

Status: **draft for review** (2026-10-08). Nothing implemented yet.

Related work in progress (as of 2026-10-08):
- `scripts/find.py`: **draft** of the selection script (RUNBOOK B1). One year per run (`--year`). It reads a local
  copy of the site, not the public `index.json` files yet, and is run directly with Python 3 (no `run.sh`/Docker
  image yet). Not committed.
- Tests written first, for changes not yet made. They fail against the current code, as intended:
  - `DataCrawlerTest.shouldFailOnMissingDirectoryIndex`: `DataCrawler` must fail when a directory index is missing
    (2.17);
  - `SyncFuzzworkOrdersetsTest` (new, plus its `inject` line in `TestComponent`): the Fuzzwork ID cutoff and its
    guard (2.18).
- `scripts/create_torrent.py`: **experiment** for measuring `.torrent` sizes (Appendix B). Hybrid v1+v2 with Python
  libtorrent, trackers from `trackers.txt`, comment from the 2.4 template (years and datasets from the file paths),
  piece size chosen automatically by libtorrent.
- `trackers.txt`: the 7 long-lived core trackers (2.4 "Trackers"), one per tier. The ngosang snapshot from option 2
  isn't added yet.
- Not done yet: the docs example link changes (2.17, 4.2).

Why this is worth doing: the archive is seeded from a local copy, and the community will likely help seed it, so
moving old years off B2 cuts hosting costs without losing the data.

## Scope

The **EVE Ref Deep Archive**: old data.everef.net data, moved into yearly torrents.

A documented, repeatable process to:

1. **Select** the old data on data.everef.net that is ready to be archived. The selection is a frozen file list, and
   every later step is checked against it.
2. **Announce** that archiving has started.
3. **Load** the selected files into a local directory.
4. **Make** a `.torrent` file for each year.
5. **Seed** it, and test it.
6. **Upload** the `.torrent` file and its file list to data.everef.net.
7. **Add** it to an **RSS feed** on data.everef.net, so other people can subscribe and automatically start seeding
   new torrents as they are published.
8. **Check the docs site** (docs.everef.net) is accurate.
9. **Back up** each year to **S3 Glacier Deep Archive**.
10. **Delete** the archived files from data.everef.net. This is the **last** step, done only after everything above
    is verified for every year in the run.
11. Run a **full `DataIndex`** job and check the site.

There is **no notice period** between publishing a torrent and deleting its files, but an announcement is posted when
the process starts (2.13).

The process is **manual**: a human runs each step from [`RUNBOOK.md`](RUNBOOK.md), with supporting scripts and a
check after every step.

Deletion is the only destructive step. After deletion, the copies of the data are the seed directory, the Glacier
backup, and whatever community seeders hold.

Years in this document (2025, 2027, …) are **examples**. The rule is relative to the year the process runs in.

---

## 1. Context

- data.everef.net hosts roughly **5.7 TB**.
- Storage is **Backblaze B2**, accessed through its S3-compatible API (`DATA_S3_ENDPOINT_URL` in `local.env`).
- A **Cloudflare Worker** sits in front of B2 and serves data.everef.net. It is defined in a **separate repo**.
- Upload code lives in this repo (`S3Util`, the `data` S3 client in `inject/S3Module`, config in `Configs.DATA_*`).
- `DataIndex` (`cli/DataIndex.java`, `make docker-data-index`):
  - Lists the bucket and writes `index.html` + `index.json` into every directory.
  - Can be limited to one prefix with `DATA_INDEX_PREFIX`. A full run has no prefix.
  - When listing, it skips existing `index.html`/`index.json`, so a directory that contains only index files
    disappears from its parent's listing.
  - **It never deletes stale index files.** After data is deleted, the emptied directories keep their old
    `index.html`/`index.json`, still listing the deleted files with dead links. See 2.15.
- `DataCrawler` (`http/DataCrawler.java`) walks the site's `index.html` pages over HTTP. It is used by
  `SyncFuzzworkOrdersets` and the market history scraper. See 2.17.
- Every directory's public `index.json` lists files with `size`, `last_modified`, `type` and `file_time`. That lets the
  find step work over public HTTP without B2 credentials.
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

### 2.1 Unit of archiving: one torrent per run, everything up to a year, all datasets

- **`everef-deep-archive-YYYY.torrent` contains everything up to and including YYYY that isn't already archived**,
  from **every** in-scope dataset, e.g. `market-orders/history/2025/…`, `market-orders/history/2024/…`,
  `ccp/sde/2023/…`. The selection is cumulative: `find --year 2025` also selects 2024, 2023, 2022 and so on, minus
  files already in a published torrent (2.8).
- Runs are ad hoc, **at most once per calendar year** by convention, to keep the number of torrents small and the
  process simple. There is no hard guard in the scripts.
- The **first** torrent catches up on every old year at once, back to the earliest year on the site (e.g.
  `everef-deep-archive-2025` with data from 2015–2025). Each later run makes one torrent with whatever became
  eligible since: usually one year, more if a year was skipped, plus stragglers.
- A torrent can't be changed after it's published. Extra torrents for an already-archived year are possible but
  should be rare (2.3). Avoid them by settling the scope before the first run.
- **Size risk — check during the first find:** a single year across all datasets could have **hundreds of thousands
  of files**, and the first, catch-up torrent holds every old year, so likely **millions**. That's far over the
  limits below; the first torrent will very likely have to be split (per dataset, or per year after all). Market
  order and contract snapshots are written several times a day, and Fuzzwork ordersets add more.
  - The `.torrent` file becomes large (tens of MB for the file list alone; hybrid torrents add more, see 2.4).
    Fetching metadata through a magnet link is slow, and some clients refuse very large metadata. libtorrent-based
    clients have limits on decode tokens and metadata size, and qBittorrent exposes those as advanced settings.
  - The find step prints the file count, a per-year breakdown and the **estimated `.torrent` size**, and
    `check-torrent` loads the result in qBittorrent with default settings.
  - Fallback if a torrent is too big: split it per dataset (2.3). Decide after the first find run.

### 2.2 What gets archived

**A year is old when `year <= current_year - 2`.** A run during 2027 archives everything up to and including the end
of 2025. The same rule applies to every dataset, and the year comes from the `YYYY` directory in the path.

The snapshot lookback used by the market history scraper (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`) is only 30 days,
so this cutoff is safe for `market-orders`.

#### Never archived

A configurable denylist (`TORRENT_EXCLUDE`):

- `market-history/` and `killmails/`. These two datasets work as a **database**, not as snapshots: past files are
  modified in place. A torrent can't follow that. (For `market-history`, `ScrapeMarketHistory` also re-reads the last
  `ESI_MARKET_HISTORY_LOOKBACK` (450 days) of files on every run, and `import-market-history` depends on the full
  history being on HTTP.)

Also never included, from any dataset: `*-latest.*`, `index.html`, `totals.json`, and everything under
`deep-archive/`.

**`index.json` files are included** when their path has the year (e.g. `market-orders/history/2025/index.json` and
each day folder's): the year's directory listings (size, last-modified, type per file) go into the torrent as
metadata. `index.json` files without a year in the path list directories that stay on the site, so they're never
selected. Consequences:

- They're data files like any other: in the manifest, the `.txt`, the `.sha256` and the delete set.
- `DataIndex` rewrites them on every run, so their last-modified changes even when nothing else does. The pre-delete
  check only checks they still exist (2.14), and the find step leaves them out of the "recently modified" flag.
- Their content links to data.everef.net URLs that stop working after deletion. That's expected; they're a record.
- rclone mirrors usually have them too (the HTML listing links `index.json`), but possibly a newer version. Mirror
  seeders' clients then re-download those few small files on recheck.

Everything else is in scope. Using a denylist means:

- New datasets are archived automatically once they're old enough.
- `fuzzwork/ordersets` and `ccp/mer` **are** in scope. Their sync commands get cutoffs (2.18), so they don't
  re-upload deleted files.

#### Which year a file belongs to

When selecting up to year Y (`find --year Y`):

- **Year in the URL:** the first `YYYY` folder in the path decides. Selected if it's Y **or earlier**. A file in a
  folder newer than Y is never selected, even if its modification time is old.
- **No year in the URL:** the file's **modification time** decides. Selected if modified in Y **or earlier**. E.g.
  `fuzzwork/ordersets/backfills/` (modified 2017); `ccp/iec`, `ccp/ccp_quant` and similar static files too.
- So no file lands in two torrents, `find` skips files already in a published torrent's `.txt`, and files in
  another torrent's frozen selection in the work directory (`<ID>.work/files.jsonl`).
- Files at the root of the site (e.g. `robots.txt`) are never selected by modification time.
- Files without a year in the URL and modified after Y aren't part of year Y.
- There's no cutoff setting (`ARCHIVE_MIN_AGE_YEARS` was dropped): which years to archive is the operator's decision,
  following the policy above (years ≤ current year − 2). The scripts don't enforce it.

**Risk of the modification-time rule:** a file that is still current but simply hasn't changed in two years would be
archived and deleted from the site, e.g. a reference file a tool downloads by a fixed URL. The find step lists every
file selected this way, per directory, marked "(by mtime)", and the operator must check them (RUNBOOK B2). Add
anything that must stay to `TORRENT_EXCLUDE`. The modification time must be the site's (`Last-Modified`), so a local
copy must keep the original timestamps.

**First-run size:** the first run covers every eligible year, which is most of the 5.7 TB. The seeder needs that much
disk. After that, each run only adds the newly eligible year(s).

### 2.3 Torrent IDs, naming and layout

Every torrent has an **ID**. The ID is used everywhere: torrent name (root folder), `.torrent` file, `.txt` file
list, files inside the torrent, work directory, backup prefix.

| Case | ID |
|---|---|
| Normal: one year | `everef-deep-archive-2025` |
| Year split per dataset (2.1 fallback) | `everef-deep-archive-2025-market-orders`, `everef-deep-archive-2025-other`, … |
| Supplement (a dataset added to scope after its year was archived) | `everef-deep-archive-2025-s2`, `-s3`, … |
| Replacement (Appendix A) | `everef-deep-archive-2025-r2`, `-r3`, … |

Inside a torrent, the `name` is the ID and file paths are the **full site paths**:

```
name:   everef-deep-archive-2025
files:  everef-deep-archive-2025.sha256          # checksums for every data file (2.6)
        everef-deep-archive-2025-README.txt      # what this is + how to verify (2.6)
        market-orders/history/2025/2025-01-01/market-orders-2025-01-01_00-00-00.v3.csv.bz2
        public-contracts/history/2025/2025-01-01/public-contracts-2025-01-01_00-00-00.v2.tar.bz2
        ccp/sde/2025/...
        ...
```

- By default a download lands in `everef-deep-archive-2025/market-orders/history/2025/…`. That's clear, and the years
  stay separate.
- To get the **exact data.everef.net layout**, which is also what `rclone` produces, download with qBittorrent's
  content layout set to **"Don't create subfolder"** (or the equivalent in other clients). The root folder is then
  dropped, and all years merge into one tree.
- Anyone with an existing rclone mirror can **seed immediately** the same way: add the torrent with "Don't create
  subfolder", point it at the mirror root, and let it recheck. That's the quickest way to get more seeders. Document
  this prominently.
- People who only want one dataset can deselect the other files in their client.

### 2.4 Torrent format

- **Hybrid v1+v2** (decided): v1 for client compatibility, v2 for per-file SHA-256 hashes. Hybrid torrents align
  every file to a piece boundary with pad files, which makes the metadata bigger. That's part of the size check in
  2.1.
- **No web seed pointing at data.everef.net.** The files will be deleted, and a torrent can't be changed afterwards,
  so a web seed would be a permanently dead URL. The Internet Archive can't serve as a web seed either (2.12).
- Not private. DHT/PEX enabled. Public trackers: see "Trackers" below.
- Piece size: see "Creating the torrent" below.
- **Comment** (required): directs the user to the Deep Archive page on the docs site. **Generated automatically**
  by `make-torrent`, never typed by hand. Template:
  ```
  EVE Ref Deep Archive: {ID}
  Data from {YEARS}. Datasets: {DATASETS}.
  What this is, how to use and verify it, and how to help seed: https://docs.everef.net/datasets/deep-archive.html
  File list: https://data.everef.net/deep-archive/{ID}.txt
  ```
  Example:
  ```
  EVE Ref Deep Archive: everef-deep-archive-2025
  Data from 2025. Datasets: ccp/mer, ccp/sde, esi-scrape, fuzzwork/ordersets, market-orders, public-contracts, ...
  What this is, how to use and verify it, and how to help seed: https://docs.everef.net/datasets/deep-archive.html
  File list: https://data.everef.net/deep-archive/everef-deep-archive-2025.txt
  ```
  - `{ID}` is the torrent ID (2.3), which is also the torrent's name.
  - `{YEARS}` comes from the **manifest**, not the ID: the distinct years of the data files (2.2: year folder, or
    modification years for files without one). Normally one year
    (`2025`). Shown as a range (`2015–2025`) if a torrent ever covers several, so the comment stays right for
    supplements, splits and any multi-year torrent.
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
    30 min). With 10+ yearly torrents, that's over a thousand announces per interval per seeder: not huge, but
    wasteful, and it's the community seeders who pay for it.
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

- Snapshot the lists at creation time into `<ID>.work/trackers.txt` and record where they came from, so a torrent
  can be reproduced and reviewed.
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
  thousand to tens of thousands of pieces. For hundreds of GB, 16 MiB is a reasonable start. Pick it from the find
  step's numbers.
- **Metadata size limits.** This is the real constraint for us. libtorrent's defaults:
  - `.torrent` files up to **10 MB** (`max_buffer_size`) and **3 million** bdecode tokens (`max_decode_tokens`);
  - metadata fetched through a **magnet link** up to about **31 MB** (`max_metadata_size`);
  - at most 2,097,152 pieces.

  qBittorrent raises the file-size and token limits in its advanced settings (check its current defaults), but
  other libtorrent-based clients may not. A hybrid torrent with hundreds of thousands of files (each with a pad file
  and a v2 file-tree entry) can pass 10 MB and millions of tokens. **The find step's `.torrent` size estimate
  should be checked against these limits, not just against qBittorrent.** This may be what forces a split per dataset
  (2.1).
- **Deterministic content.** Sort files by path. Then the same files always give the same info-hash. The creation
  date and trackers are outside the info dictionary, so they don't change it.
- **Keep the creation date.** The feed's `pubDate` uses it (2.5).
- **No `source` field, not private, no web seeds** (above).
- **Verify after creating.** Load it in a client and recheck against the content (RUNBOOK D3, D4).

**Recommendation:** create torrents with Python `libtorrent` 2.x, hybrid, files sorted by path, creation date kept.
Choose the piece size from the first find run's numbers, and check the estimated `.torrent` size against
libtorrent's default limits as well as qBittorrent's.

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
  torrents.json                     # catalogue of all torrents (NOT index.json — DataIndex owns that name)
  everef-deep-archive-2025.torrent
  everef-deep-archive-2025.txt      # every file path in the torrent, one per line
  everef-deep-archive-2024.torrent
  everef-deep-archive-2024.txt
  ...
```

- **"Already archived" is decided per file, not per year:** the find step downloads every published `.txt` and skips
  any path listed in one. That works the same for normal, split, supplementary and replacement torrents.
- `torrents.json` and the feed are **regenerated in full** from the `.torrent` files in `deep-archive/` each time, never
  edited by appending. The bucket stays the single source of truth, so re-runs are safe.
  - Publication date comes from the torrent's `creation date` field.
  - Replaced torrents are listed in `deep-archive/superseded.txt` in this repo (`<old ID> <new ID>` per line).
    `build-feed` marks them as superseded in `torrents.json` and leaves them out of the feed.

#### File list: `<ID>.txt`

For quick review and search without having to parse torrent files:

- Plain text, UTF-8, `\n` line endings. **One file path per line**, as the full site path (without the torrent's root
  folder), **sorted**. Data files only. For example:
  ```
  ccp/sde/2025/sde-20250101-TRANSLATIONS.zip
  market-orders/history/2025/2025-01-01/market-orders-2025-01-01_00-00-00.v3.csv.bz2
  ...
  ```
- Generated **from the `.torrent` itself** after it's made, minus the two root files. `check-torrent` compares it
  with the manifest.
- Paths match the site layout, so someone looking for e.g. "public contracts from March 2024" can `grep` it and
  knows which torrent to grab and which file to select.
- Content-Type `text/plain; charset=utf-8`. A year with hundreds of thousands of files makes a list of 10–30 MB.
  Cloudflare compresses it in transit.

### 2.6 Files inside the torrent: checksums and README

The checksum file and the README ship **inside the torrent**, at the torrent root. They are **not** published
separately on the data site. Anyone who has the data also has the means to verify it.

**Names start with the ID on purpose.** With "Don't create subfolder", root files from every torrent land in the
same directory. Generic names like `SHA256SUMS` or `README.txt` would collide.

#### `<ID>.sha256`

- Standard `sha256sum` format: `<64 hex chars>  <path>` (two spaces). One line per **data** file, sorted. It does
  not list itself or the README.
- Paths are relative to the torrent root, which are the site paths. So the same command works in both layouts: run
  it from the directory that contains the `.sha256` file.
- Hashes come from `manifest.json`, computed by `check-load` from the downloaded files.
- Written into `$DIR/<ID>/` **before** the torrent is made, so it's covered by the torrent's piece hashes.

#### `<ID>-README.txt`

Plain text, generated from a template in `deep-archive/`. Contents:

1. What this is: the EVE Ref Deep Archive for the year, taken from data.everef.net, plus the list of datasets.
2. Layout: paths match data.everef.net, plus the note about "Don't create subfolder".
3. Links: docs page (docs.everef.net/datasets/deep-archive.html) and data.everef.net.
4. **How to verify the checksums** (below).
5. Licence/attribution notes, the same as the data site (CCP data, see `LICENSE-CCP`).

#### Manual verification instructions

The same text goes in the README and on the docs page:

> All commands are run from the directory that contains `everef-deep-archive-YYYY.sha256`.
>
> **Linux**
> ```
> sha256sum -c everef-deep-archive-YYYY.sha256                    # verify everything
> sha256sum -c --ignore-missing everef-deep-archive-YYYY.sha256   # only the files you downloaded
> sha256sum -c --quiet everef-deep-archive-YYYY.sha256            # print failures only
> ```
> Each file prints `OK` or `FAILED`, and the command ends with a summary of failures.
>
> **macOS**
> ```
> shasum -a 256 -c everef-deep-archive-YYYY.sha256
> shasum -a 256 -c --ignore-missing everef-deep-archive-YYYY.sha256   # if your shasum supports it
> ```
>
> **Windows (PowerShell)**
> ```powershell
> Get-Content everef-deep-archive-YYYY.sha256 | ForEach-Object {
>     $hash, $path = $_ -split '  ', 2
>     if (Test-Path -LiteralPath $path) {
>         $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLower()
>         if ($actual -eq $hash) { "OK: $path" } else { "FAILED: $path" }
>     }
> }
> ```
> Files you haven't downloaded are skipped.
>
> **A single file**
> ```
> grep 'market-orders-2025-01-01_00-00-00' everef-deep-archive-YYYY.sha256
> sha256sum market-orders/history/2025/2025-01-01/market-orders-2025-01-01_00-00-00.v3.csv.bz2
> ```
> The two hashes must be identical.
>
> Your torrent client already checks every piece while downloading. These checksums are an extra, client-independent
> check, useful for long-term storage, or after copying the files somewhere else.

To check before publishing:
- The PowerShell snippet works on paths with spaces or special characters. Site paths have neither, but test it.
- macOS `shasum` supports `--ignore-missing` in recent versions. Confirm on a current macOS.

#### Checks

- `check-torrent` checks the torrent's file list equals the manifest paths plus exactly the two root files, checks
  the `.sha256` equals the manifest, and **runs the documented Linux command** (`sha256sum -c`) over the full content.
  This re-reads all the data (hours for a large year), but it proves the checksum file and the user instructions are
  correct.
- `test-download` downloads the `.sha256`, the README and a sample of data files through the torrent, then runs
  `sha256sum -c --ignore-missing`, exactly as a user would.

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
      <title>everef-deep-archive-2025</title>
      <link>https://data.everef.net/deep-archive/everef-deep-archive-2025.torrent</link>
      <guid isPermaLink="false">INFOHASH_V1</guid>
      <pubDate>Wed, 07 Oct 2026 12:00:00 GMT</pubDate>
      <description>EVE Ref Deep Archive for 2025. 812 GB, 240,000 files. Datasets: market-orders,
        public-contracts, ... File list: https://data.everef.net/deep-archive/everef-deep-archive-2025.txt</description>
      <enclosure url="https://data.everef.net/deep-archive/everef-deep-archive-2025.torrent"
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
- The feed includes **all** current torrents (it's small), so new subscribers can pull in the backlog. Superseded
  torrents are left out.
- One feed only.
- Content-Type `application/rss+xml`. Short cache max-age, like `DATA_INDEX_CACHE_CONTROL_MAX_AGE`.

### 2.8 Selection first: the file list

The **first** thing the process does is decide exactly which files are archived. Everything after that is checked
against that list, never against "whatever is in the directory".

- `find --year Y` runs for **one year** at a time. It walks the public `index.json` files and writes that year's file
  list to `<dir>/<ID>.work/find/files.jsonl` (path, size, last-modified, URL). It also creates the empty torrent
  content folder `<dir>/<ID>/`, and never writes inside it. It skips excluded datasets, latest/index files, and
  paths already listed in a published `.txt` (2.5). The year comes from the path, or from the modification time for
  files without a year in the path (2.2). Each record says which (`selected_by`: `path` or `mtime`).
  - The draft reads a **local copy** of the site instead (an rclone mirror or backup). The dataset is the path before
    the year folder, without a trailing `history`; for files selected by modification time, the first three
    directory levels. It also flags recently
    modified files, empty files, and files whose dated path disagrees with their year folder, and estimates the
    year's `.torrent` size and bdecode tokens against the limits in 2.4.
  - There's no overview of all candidate years in one run any more; run `find` per year. An overview mode could
    come back later if picking years gets tedious.
  - Open: once a local copy is the source, should `load` also copy from it instead of downloading over HTTP? The
    pre-delete check compares the bucket with the selection by size and last-modified either way.
- The operator reviews it and picks the years for the run. `start-year` **freezes** `find/files.jsonl` into
  `<ID>.work/files.jsonl`, so re-running `find` can't change it. That frozen list is the selection.
- `load` copies **exactly** the files in the list: `rclone copy --files-from <list>` with the `:http:` backend.
  rclone matches files by size and modification time.
- `check-load` checks every listed file is on disk with the listed size, and nothing else is. It then computes
  SHA-256 for every file into `manifest.json` (the selection plus hashes). The manifest feeds the `.sha256`, the
  torrent checks and the backup.
- The delete set is the selection. Before deleting, the bucket is checked against it by **size and last-modified**.
- **No MD5/ETag matching anywhere.** Size and timestamp are enough to detect changes; content integrity from load
  onwards is covered by SHA-256 (manifest, `.sha256`, torrent piece hashes).

### 2.9 Tools

| Step | Tool | Why |
|---|---|---|
| Find | Python script reading public `index.json` over HTTP | No credentials. Has size and last-modified. |
| Find (draft) | `scripts/find.py --year Y --source <local copy>`: walks a local mirror/backup instead of HTTP | Fast; works from an existing backup. Needs the copy to keep the site's timestamps. |
| Load | `rclone copy --files-from` with the `:http:` backend against data.everef.net | Already the documented download method. Concurrent and resumable. Copies exactly the selection. |
| Make torrent | Python `libtorrent` 2.x (hybrid v1+v2); alternatives in 2.4 "Creating the torrent". Not `mktorrent` (v1 only). | Mature library, same as qBittorrent. Don't write a torrent creator. |
| Seed | `qBittorrent-nox` in Docker, through its Web API | Headless and scriptable, and it's what most feed subscribers use. |
| Upload | `rclone copyto` to the B2 S3 remote with explicit content type and cache headers | Same bucket/credentials as `DATA_*`. |
| Feed + catalogue | Python script: list `deep-archive/`, parse each `.torrent`, write `torrents.json` and feed | Rebuilds from the bucket every time. |
| Backup | Python script building ZIP bundles, `rclone` to S3 with storage class `DEEP_ARCHIVE` | See 2.11. |
| Delete | Python script using the S3 API with an explicit key list (never a prefix delete) | Precise and auditable. |
| Index pages | Existing `data-index` command, full run | Reuse. |

All scripts run from a Docker image (`deep-archive/Dockerfile`) containing Python, rclone, the torrent creator and the
qBittorrent tooling, so nothing needs to be installed on the host.

### 2.10 Manual process — script principles

The workflow is **manual by design**: a human runs each step from `RUNBOOK.md` and double-checks the result before
moving on. The scripts support the human; they don't replace their judgement.

- **One script, one step.** Scripts never call the next step. There is no "run everything" script.
- **Run through the wrapper:** `deep-archive/run.sh <script> --dir <dir> [args]`.
- **`--dir <dir>` on every script (required).** It is both the working directory **and** the directory torrents are
  seeded from (qBittorrent save path). Content goes in `<dir>/<ID>/`. Working files (`.torrent`, `.txt`, `.work/`)
  sit beside it, never inside, so the content folder is exactly the torrent. The wrapper mounts `<dir>` at the same
  path in the container, so paths match on the host, in the scripts and in qBittorrent.
- **Every action has a separate check script.** E.g. `load` is followed by `check-load`. Check scripts are read-only,
  re-derive everything from source (bucket, disk, qBittorrent, site), and never trust the action script's output.
- **Check scripts leave a receipt.** On success a check writes `<dir>/<ID>.work/checks/<step>.ok`, containing a
  timestamp and a summary (counts, bytes, hashes). On failure it writes nothing and exits non-zero.
- **Action scripts require the previous receipts.** E.g. `seed` refuses to run without `check-torrent.ok`, and
  `delete` refuses without *all* earlier receipts, including the backup. Steps can't be skipped by accident.
- **Show, then ask.** Any script that writes anywhere remote (B2, Glacier, the site, qBittorrent) first prints what it
  will do (counts, bytes, sample paths) and asks for confirmation. `delete` needs the torrent ID typed back in, not
  just `y`.
- **Idempotent.** Re-running a step is always safe. Re-running a check re-checks everything.
- **Short, readable output.** One summary block per script ending in `OK` or `FAILED: <reason>`. Details go to
  `<dir>/<ID>.work/logs/<step>.log`.
- **Per-year checklist.** `start-year` copies a checklist template into `<dir>/<ID>.work/CHECKLIST.md`, which the
  operator ticks off as they go. It's the human record alongside the receipts.

### 2.11 Backup: S3 Glacier Deep Archive

Every archived year is backed up to **S3 Glacier Deep Archive** before anything is deleted. The backup is the copy
EVE Ref controls if the seed directory is lost.

**Why bundles, not one object per file.** Deep Archive charges per request and adds about 40 KB of billed metadata
per object, and restores are requested per object. A year with hundreds of thousands of small files would be slow and
expensive to upload and to restore. So the files are packed into large bundles.

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

**Steps** (RUNBOOK Part E):

1. `prepare-backup`: plans the bundles from the manifest (`backup-plan.json`: bundle → files) and writes
   `<ID>-backup-index.tsv`. `check-backup-plan` checks every manifest file is in exactly one bundle and the sizes add
   up.
2. `backup`: for each bundle, builds it in `<ID>.work/backup/`, records its size and SHA-256, uploads it with storage
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

**Account:** a separate bucket (ideally a separate AWS account) with credentials that the rest of EVE Ref doesn't
use. Consider S3 Object Lock or at least versioning, so a mistake can't delete the backup.

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

- A yearly torrent has hundreds of thousands of files and hundreds of GB. It **doesn't fit one item**, so the
  Internet Archive can't mirror the files one-to-one.
- It **can't be a web seed for our torrents.** A web seed (BEP 19) needs every file at `<url>/<torrent name>/<path>`,
  which would need one item holding the whole year's files.
- What does fit: the **backup bundles** from 2.11. About 50 GB per bundle, grouped into items of at most ~500 GB
  (e.g. items `everef-deep-archive-2025-part-001-010`), plus the metadata files. That gives:
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

**Recommendation:** treat it as an optional extra, done after a year's Glacier backup, using the same bundles. Decide
after the first run (open question). If it's used, the docs page links the items as an HTTP alternative.

Sources: [Archive BitTorrents](https://help.archive.org/help/archive-bittorrents/),
[Uploading – Tips](https://help.archive.org/help/uploading-tips/),
[Bulk uploading](https://help.archive.org/?p=433), [BEP 19](https://www.bittorrent.org/beps/bep_0019.html).

### 2.13 Announcements

There is no notice period, but a notice is posted to the **announcement channel** (Discord) when a run **starts**,
right after the years are chosen. A second post follows when the run is finished.

**At start:**

> **EVE Ref Deep Archive: archiving {YEARS}**
>
> We're moving old data on data.everef.net into the **EVE Ref Deep Archive**: yearly BitTorrent archives.
> This run covers **{YEARS}** (about {SIZE}).
>
> - Torrents will be published at https://data.everef.net/deep-archive/, with an RSS feed at
>   https://data.everef.net/deep-archive/feed.xml.
> - Once the torrents are published, seeded and backed up, the HTTP copies of these files will be **removed** from
>   data.everef.net. There's no fixed date.
> - `market-history`, `killmails` and all `-latest` files are **not** affected.
>
> **What you might need to do:**
> - If you mirror the data with `rclone sync`, switch to `rclone copy`, or your local copies of these years will be
>   deleted.
> - If you need files from these years, download them now, or use the torrents once they're out.
> - Already have a mirror? You can seed the torrents from it without downloading anything. Please do!
>
> Details: https://docs.everef.net/datasets/deep-archive.html

**At finish:**

> **EVE Ref Deep Archive: {YEARS} archived**
>
> The torrents for **{YEARS}** are out: https://data.everef.net/deep-archive/ (RSS: https://data.everef.net/deep-archive/feed.xml).
> The files have been removed from data.everef.net. Every torrent has a `.txt` file list, so you can find what you
> need and select only those files.
>
> Seeders are very welcome. That's what keeps this data available.
> How to seed, verify and find files: https://docs.everef.net/datasets/deep-archive.html

### 2.14 Deletion design

**Deletion is the last step of a run.** It happens only after every year in the run has passed publishing, seeding,
testing, docs and backup checks.

- **Delete set = the selection** (2.8), re-validated against the bucket at deletion time:
  - Every file must still exist on the bucket with the same size and last-modified as in the selection. If any
    differs, **stop that year**: the data changed after it was selected (Appendix A). Exception: `index.json` files
    are rewritten by `DataIndex`, so for them only existence is checked; differences are reported.
  - Files in the bucket under that year's paths that are *not* in the selection (added later): **stop** and
    investigate. Never delete files that aren't in the torrent.
    For files selected by modification time (2.2), there's no year path: only the selected files themselves are
    checked, since their directories can hold newer files that stay.
  - Deletes are per key, never by prefix wildcard.
- **Soft delete.** On B2, an S3 `DeleteObject` without a version ID **hides** the file: a hide marker is written and
  the old version is kept until the bucket's lifecycle rule purges it. That rule is currently **2 days**, which isn't
  a guarantee of anything. With the Glacier backup and the seed directory, the lifecycle rule doesn't matter:
  - within the window, rollback is `undelete` (remove the hide markers);
  - after it, rollback is `reupload`: copy the selection back from `$DIR` (or a Glacier restore) to the bucket.
- **Dry run by default.** The script prints the key count, total bytes and a sample of keys, and only acts with an
  explicit `--execute`.
- Log every deleted key (with version ID) to `<dir>/<ID>.work/deleted.jsonl` for audit and rollback.
- **Pause jobs that crawl the data site** (`sync-fuzzwork-ordersets`, the market history scrape) from the delete
  until the full `DataIndex` has finished (2.17).

### 2.15 Stale index files after deletion

`DataIndex` regenerates the parent directories correctly (the deleted year vanishes from them), but it **leaves the
old `index.html`/`index.json` in every emptied directory**. Fix it one of these ways:

- **(a)** The delete script also deletes `index.html` in every directory it emptied (`index.json` is already in the
  delete set, 2.2). Simple, and self-contained in the process.
- **(b)** Make `DataIndex` delete index files in directories that no longer contain any data. A more general fix,
  but it's a Java change, and it needs care so it never deletes the root index.

Recommendation: **(a)** now, and **(b)** as a follow-up.

### 2.16 Cloudflare cache

**No cache purge.** Archive files are served with a 30-day max-age (`DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE`), so
Cloudflare can keep serving a deleted file from cache for up to 30 days. Index pages have a 2-minute max-age, so they
update almost at once. That's accepted.

`check-site` therefore checks **many** URLs, not one: for each year, a random sample of deleted files spread across
datasets and months (e.g. 50), plus every year directory's `index.json`. Per URL:

- `404`: pass.
- `200` with `cf-cache-status: HIT`: pass, reported as "still cached". It will expire.
- Anything else (`200` from the origin, `5xx`, …): **fail**. The origin still serves the file, or something is broken.

### 2.17 Code and users that depend on the deleted data

Must be handled **before** deleting the affected datasets:

| Consumer | Problem | Mitigation |
|---|---|---|
| `SyncFuzzworkOrdersets` | Re-syncs every orderset missing from the data site, so it **would re-upload the deleted files**. Fuzzwork's listing goes back a long way (IDs 90019–174817 on 2026-10-08). | ID cutoff (2.18). |
| `SyncMer` | Rescans from `MER_FIRST_MONTH` if no MER files exist. | Guard (2.18). |
| `DataCrawler` | Today it logs and returns nothing when an index page is missing. Decision: make it **fail** instead (revert if it causes problems). Between the delete and the full `DataIndex`, parent indexes still link to removed directories, so crawling jobs will fail. | Pause crawling jobs during that window (2.14). |
| `ScrapeMarketHistory` / `import-market-history` | Re-read up to 450 days of `market-history/`, and the full history. | `market-history` is never archived. |
| `HistoricalOrdersRegionTypeSource` | Reads the last 30 days of market orders (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`). | Safe with the `current_year - 2` cutoff. |
| Docs examples | `downloading-datasets.md` uses `market-orders/history/2023/` as its example; `structures.md` links 2024 files. They'd 404. | Change the downloading example to `https://data.everef.net/market-history/2026/` (never deleted). Change the structures examples to recent files, or remove them. |
| Users following `downloading-datasets.md` (`rclone sync`) | **`rclone sync` deletes their local copies** of files deleted on the site. | Change the docs to `rclone copy` before the first deletion. The start announcement repeats it. |
| Users with `wget -N` / custom scripts | Get 404s. | Start announcement, docs page. |

### 2.18 Sync cutoffs for Fuzzwork and MER

The sync commands must never fetch data the archival process removed. Both changes must be **deployed before the
first deletion** of `fuzzwork/` or `ccp/mer/` data (RUNBOOK A1). Tests first (repo convention).

**`SyncFuzzworkOrdersets` — decided: cutoff at the lowest ID on the data site.**

- It decides what to sync by **sequence ID**, comparing Fuzzwork's listing with the IDs found on the data site. The
  year of an orderset is only known after downloading it, so a date cutoff would mean re-downloading every archived
  orderset on every run.
- New rule: skip any Fuzzwork ID **below the lowest ID currently on the data site**. Gaps above it are still filled.
- **Count only the dated layout** (`fuzzwork/ordersets/YYYY/…`), not `backfills/`. The backfills (IDs 21000–59999)
  have no year folder; they're archived by modification time (2.2), possibly before the dated years, and Fuzzwork's
  listing starts at 90019 anyway. If the backfills counted while still on the site, the lowest ID would be 21000
  and the cutoff would never apply.
- **Guard:** if the data site has no dated ordersets at all, fail instead of syncing everything.
- Fuzzwork sequence IDs are incremental (confirmed), so the ID cutoff follows the archive cutoff automatically: once
  a year is deleted, the lowest remaining ID moves up.

**`SyncMer` — guard.** MER is already mostly safe: it only goes back two months from the latest file it finds, and
recent MER files stay on the site. The one hole is "no MER files at all, so re-sync from `MER_FIRST_MONTH`".
**Guard:** if the data site has no MER files, fail instead of syncing everything (same as Fuzzwork). No config
needed (an earlier draft had an `ARCHIVE_MIN_AGE_YEARS` setting; dropped).

### 2.19 Durability

Copies of each archived year after deletion:

1. **The seed directory (`--dir`)**, on redundant storage (RUNBOOK A5), checked regularly (RUNBOOK "Maintenance").
2. **The Glacier Deep Archive backup** (2.11), restorable if the seed directory is lost.
3. **Community seeders**, encouraged through the RSS feed, rclone-mirror seeding and the announcements.
4. Optionally the **Internet Archive** (2.12).

The B2 hide-to-delete window is not counted. The data can never be re-scraped.

### 2.20 Discoverability after deletion

`market-orders/history/2025/` disappears from the listings, so someone browsing the site won't see that the year
exists as a torrent.

- **The docs site** explains the archive rules and links the torrents (4.2). It's set up once, before the first
  deletion.
- Optional: enhance `DataIndex` to read `deep-archive/torrents.json` and render an "Archived years" note on each
  dataset's index page (e.g. "2015–2025: see `everef-deep-archive-YYYY`"), linking to the torrent and the `.txt`.

---

## 3. The process

The step-by-step process lives in [`RUNBOOK.md`](RUNBOOK.md). This document holds the design and reasoning
behind it.

## 4. Documentation

| Audience | Where | What |
|---|---|---|
| Operator (us) | `deep-archive/*.md` in this repo, alongside the scripts | How to run the workflow: `README.md`, `RUNBOOK.md`, `PLAN.md` (design, temporary). |
| Users of the data | docs.everef.net (`docs/src/…`) | What the torrents are, why they exist, how to use them, and when new ones appear. |

The docs site does **not** describe the internal workflow (scripts, checks, B2, backups). It links to the
`deep-archive/` directory on GitHub for anyone curious.

### 4.1 Operator docs (`deep-archive/`)

```
deep-archive/
  README.md             # what this directory is, quick overview, links to RUNBOOK/PLAN, prerequisites
  RUNBOOK.md            # the step-by-step manual process
  PLAN.md               # design decisions and reasoning; goes away once the process is settled
  LOG.md                # one line per archived torrent
  superseded.txt        # replaced torrents (2.5); created when first needed
  run.sh                # Docker wrapper
  Dockerfile
  torrents.env.example
  README-template.txt   # source for the README inside each torrent (incl. verification instructions)
  scripts/              # find, start-year, load, check-load, make-torrent, ...
```

Keep the runbook and scripts in sync: any script change that changes output or behaviour updates `RUNBOOK.md` in
the same commit. This follows the repo's "docs never drift" convention, and `AGENTS.md` should mention the directory.

### 4.2 Docs site: new page `docs/src/datasets/deep-archive.md` ("EVE Ref Deep Archive")

Written once. **Rule-based rather than listing specific years**, so it doesn't need editing every run. The live
list of torrents is the `deep-archive/` directory and the feed.

1. **What it is.** BitTorrent archives of data.everef.net datasets, `everef-deep-archive-YYYY.torrent`. Each one holds
   everything up to and including YYYY that wasn't in an earlier torrent, from every archived dataset, in the same
   paths as on the site. The first one covers all the early years; after that, usually one new year each time.
2. **Why they exist.** data.everef.net keeps growing (5.7 TB+). Old data is moved off the HTTP site to keep hosting
   sustainable, and torrents keep it available, with the community helping to host it. Be honest about the
   trade-off: old data is only available as long as someone seeds it, and seeding is appreciated.
3. **What gets archived, and what doesn't.**
   - Years ≤ current year − 2. E.g. during 2027, data up to the end of 2025 is archived.
   - Once a year is archived, its files are **removed from data.everef.net**.
   - Never archived (always on HTTP): `market-history` and `killmails` (they work as a database and change in
     place), and `*-latest` files.
4. **When new torrents appear.** Ad hoc, **at most once per year**. A year becomes eligible on 1 January two years
   later, and its torrent is created at some point after that. There is no fixed date. Until a year is archived, its
   data stays on data.everef.net. Each run is announced on Discord when it starts. Subscribe to the feed to be
   notified of new torrents.
5. **How to use them.**
   - Links: [`deep-archive/`](https://data.everef.net/deep-archive/) directory and the RSS feed `feed.xml`.
   - **Finding specific data:** every torrent has a matching `everef-deep-archive-YYYY.txt` listing every file. Search it,
     then add the torrent and select only the files you need.
   - **Getting the site layout:** content layout "Don't create subfolder" (qBittorrent) puts files in the same
     paths as on data.everef.net, and multiple years merge into one tree, matching an `rclone` mirror.
   - **Already have an rclone mirror?** Add the torrent with "Don't create subfolder", point it at your mirror root,
     and let it recheck. You start seeding without downloading anything (except possibly a few `index.json` files,
     if your mirror has newer versions).
   - **Helping to seed (auto):** qBittorrent → RSS → add the feed URL → RSS Downloader rule matching everything,
     auto-download → new archives are picked up as they're published. Size warning: each year is hundreds of GB,
     and the full archive is TBs.
6. **Integrity.** Torrents verify every piece. Each torrent also **contains** `everef-deep-archive-YYYY.sha256`. Include the
   full manual verification instructions from 2.6 (Linux, macOS, Windows, a single file).

Related one-time changes, all live **before the first deletion**:
- `docs/src/datasets/downloading-datasets.md`: switch `rclone sync` to `rclone copy`, and explain that old years get
  removed from the site and `sync` would delete your local copies. Change the example path
  `market-orders/history/2023/` to `https://data.everef.net/market-history/2026/` (never deleted). Link the Deep
  Archive page.
- `docs/src/datasets/structures.md`: replace the two 2024 example links.
- `docs/src/datasets/index.md`: add the Deep Archive page, and a note that old years are archived.
- Dataset pages: one generic line, "Years older than the year before last are archived as torrents", with a link.
- Docs sidebar/navigation entry for the new page.

### 4.3 Per run

The docs are rule-based, so normally **nothing changes**. Runbook step D8 is a review: confirm the page is still
accurate (excluded datasets, timing, links), and update it only if the rules changed.

---

## 5. Changes outside the scripts

- **This repo:**
  - Docs site (4.2): the Deep Archive page and related edits, before the first deletion.
  - Operator docs in `deep-archive/` (4.1), kept in sync with the scripts.
  - `SyncFuzzworkOrdersets` ID cutoff and `SyncMer` guard (2.18). Deployed before the first deletion.
  - `DataCrawler` fails on a missing directory index (2.17).
  - Optional `DataIndex` changes: the "Archived years" note (2.20) and stale index cleanup (2.15b).
  - `deep-archive/` scripts and `Dockerfile`.
  - An `AGENTS.md` update describing the `deep-archive/` directory and process.
- **Cloudflare Worker (separate repo):**
  - Serve `.torrent` as `application/x-bittorrent` and the feed as `application/rss+xml` (or pass through B2's stored
    Content-Type).
  - Optionally make deleted archive paths return a 404 page that links to the Deep Archive page.
  - Make sure no bot protection blocks RSS readers or torrent clients fetching `.torrent` files.
- **B2:** if possible, an application key scoped so the delete step can't touch anything outside the intended
  prefixes.
- **AWS:** a bucket (ideally a separate account) for the Glacier backup, with its own credentials (2.11).

---

## 6. Open questions

Decided so far: one cumulative torrent per run (everything up to a year, 2.1); policy `year <= current_year - 2`;
exclusions `market-history` and `killmails`;
Fuzzwork/MER in scope with sync cutoffs; Fuzzwork cutoff at the lowest dated ID on the site; naming and IDs (2.3);
checksums and README inside the torrent; hybrid v1+v2; no notice period, but a start announcement; Glacier Deep
Archive backup; deletion last; no MD5 matching; no Cloudflare purge; `DataCrawler` fails on missing indexes; runs at
most yearly with no hard guard; a manual process with scripts in `deep-archive/`.

Still open:

1. Is one torrent per run workable, especially the first, catch-up one? It depends on the file count and `.torrent`
   size from the first find run. The fallback is a split per dataset (2.3). Early signal from the draft `find`
   estimate: a synthetic year of 240,000 files gives a hybrid `.torrent` of about 68 MiB and 6.5 million tokens at
   16 MiB pieces, well over libtorrent's defaults (10 MB, 3 million tokens). **First real numbers: Appendix B** —
   about 800,000 files and 4 TiB up to 2025, so the catch-up torrent can't be one torrent.
2. Glacier: which AWS account, bucket and region? Object Lock or versioning? Bundle size (~50 GB proposed)?
3. Bundle format: ZIP64 store-only (Internet-Archive-friendly) or tar?
4. Internet Archive: use it at all? Is redistribution there allowed for CCP and Fuzzwork data? Item grouping?
5. Seeder host: where is it, how much disk and upload, and does it have a redundant filesystem?
6. Where to run `test-download` so it actually tests reachability from outside (another machine on another network)?
7. Stale index cleanup: in the delete script (2.15a), in `DataIndex` (2.15b), or both?
8. Add the `DataIndex` "Archived years" note (2.20)?
9. Modification-time selection (2.2): is selecting files without a year in the URL by modification time right for
   every such directory? Which ones must stay on HTTP (add to `TORRENT_EXCLUDE`)? Root-level files are never
   selected this way.
10. Which Discord channel gets the announcements, and who posts them?
11. Trackers: add all from both lists, a curated set, or the core only (2.4 "Trackers")? Refresh tracker lists in
    published `.torrent` files later?
12. Torrent creation tool: Python `libtorrent` or the qBittorrent Web API creator? Piece size, once the first find
    run gives real numbers (2.4 "Creating the torrent").

---

## Appendix A — If archived data turns out to have changed

If the pre-delete check (RUNBOOK F1) finds that a file changed on the bucket after it was selected:
- **Don't delete that year.** Other years in the run can go ahead.
- If the torrent hasn't been published yet: start the year again from RUNBOOK D1 with a fresh find.
- If it has been published: make a replacement `<ID>-r2` torrent, add `<old ID> <new ID>` to
  `deep-archive/superseded.txt`, and run Parts D and E for the replacement. Keep seeding the old torrent too, if possible.
- Then consider adding the dataset to `TORRENT_EXCLUDE`, since its old files evidently still change.

---

## Appendix B — First find run on real data (2026-10-08)

`find.py --year 2025` against the local backup. The per-year breakdown from the report (year from the path, or the
modification time for files without a year in the path):

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
  entry per file and will be larger. Measure it with `scripts/create_torrent.py`.
- Already **over libtorrent's 10 MB `.torrent` default** (2.4), even as v1. Just under the ~31.5 MB magnet metadata
  limit, but a hybrid version almost certainly won't be.
- The file count is about the size of one large year (2023: 264,988; 2024: 282,463), so it backs up the conclusion
  below: a large year doesn't fit libtorrent's defaults as one torrent.

### Measured: `create_torrent.py`, 13 files (2026-10-08)

Small hybrid v1+v2 test with `scripts/create_torrent.py` (libtorrent 2.x, 16 MiB pieces):

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
  - For the real archive, copying terabytes into the incomplete directory isn't practical. RUNBOOK D4 instead adds
    the torrent with its save path set to `$DIR`, so qBittorrent checks the files where they are. **Still to test.**

### Measured: `create_torrent.py`, 43,620 files (2026-10-08)

Hybrid v1+v2 with `scripts/create_torrent.py` (16 MiB pieces) over a directory of small files:

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
tokens per file for a hybrid torrent; rough, derived from a synthetic run, not from these files):

| Grouping | Files | Est. `.torrent` | Est. tokens |
|---|---:|---:|---:|
| Everything up to 2025 in one torrent | 800,045 | ~240 MB | ~21.6 M |
| 2024 | 282,463 | ~85 MB | ~7.6 M |
| 2023 | 264,988 | ~79 MB | ~7.2 M |
| 2025 | 119,534 | ~36 MB | ~3.2 M |
| 2022 | 60,375 | ~18 MB | ~1.6 M |
| 2021 | 33,172 | ~10 MB | ~0.9 M |
| 2020 and earlier, together | 39,513 | ~12 MB | ~1.1 M |

Against the limits in 2.4 (libtorrent defaults: 10 MB `.torrent`, 3 M tokens, ~31.5 MB magnet metadata; qBittorrent
raises the first two, assumed 100 MiB and 10 M, still to check):

- **One catch-up torrent is out.** ~240 MB and ~22 M tokens is over every limit, including qBittorrent's assumed
  ones.
- **Even one torrent per year is over libtorrent's defaults** from 2022 on. Staying under 10 MB means roughly
  **33,000 files per torrent** at most.
- **2023, 2024 and 2025 are also over the magnet limit** (~31.5 MB). Users adding them by magnet link would fail on
  default libtorrent settings; only the `.torrent` file would work, and only in clients with raised limits.
- Future years look like 2023–2025 (120,000–280,000 files each), so this isn't a one-off: every yearly torrent will
  hit it.

**Options:**

1. **Target qBittorrent's limits, split by year.** One torrent per year (2023, 2024, 2025 at 80–85 MB), plus one for
   2022 and earlier. Fewest torrents, but over libtorrent's defaults and the magnet limit; clients other than
   qBittorrent may refuse them. Needs qBittorrent's real defaults confirmed.
2. **Split each large year per dataset** (2.3 naming: `everef-deep-archive-2024-market-orders`, …). Whether that
   helps depends on the dataset breakdown: if one dataset holds most of a year's files, it may still be too big.
3. **Split by size target**, e.g. at most ~30,000 files per torrent, by dataset and then by month range. Fits every
   default, but means many torrents (~30 for the current data) and a naming scheme for parts.
4. **Make torrents smaller per file.** v1-only torrents drop the pad files and the v2 file tree, roughly halving the
   per-file overhead (2.4 decided hybrid; this would revisit it). Helps, but doesn't fix 280,000-file years alone.
5. **Fewer, bigger files.** Bundle small files (e.g. per day or per month into a ZIP) inside the torrent. Cuts the
   file count by orders of magnitude, but the torrent no longer matches the site layout, rclone mirrors can't seed
   it, and single-file downloads get coarser. Probably not worth it.

The dataset breakdown (below) changes the picture: most of the file count is small-snapshot datasets that are
almost free to keep on B2. See "What the dataset breakdown shows" and the revised recommendation there.

**Things that look odd and need checking:**

- **2023 file count jumps 4.4×** (60,375 → 264,988) while bytes barely grow (697 → 750 GiB): many more, smaller
  files. Most likely explained by the dataset breakdown: small-snapshot datasets (incursions alone is 252,357
  files), probably mostly started around 2023 (a per-dataset-per-year breakdown would confirm).
- **2025 is less than half of 2024** (119,534 vs 282,463 files). Is the backup complete for 2025, or did collection
  change during 2025? If the backup is behind the site, the selection would miss files, and the pre-delete check
  (which compares the bucket with the selection) would stop on "extra files".
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
- **Without them**, the whole catch-up is ~246,000 files: ~74 MB and ~6.7 M tokens by the rough estimate, so still
  too big for one torrent, but split per year it's roughly 25,000–45,000 files per year (needs a per-dataset-per-year
  breakdown to confirm), i.e. ~8–14 MB per torrent: around libtorrent's 10 MB default and well under the magnet
  limit.
- **The big three** are `fuzzwork/ordersets` (85,464 files, 1.9 TiB, plus 23,854 backfill files, 560 GiB),
  `market-orders` (67,833, 1.1 TiB) and `public-contracts` (66,967, 322 GiB). Together ~244,000 files and ~3.9 TiB:
  that's the archive.

**Revised recommendation:**

1. **Add the small-snapshot datasets to `TORRENT_EXCLUDE`.** They stay on HTTP; the archive drops from 800,000 to
   ~246,000 files at the cost of keeping ~6 GiB on B2 (growing slowly). This also keeps those datasets complete on the
   site, which suits how they're used (time series of small JSON files). A rule could replace the list: "don't
   archive datasets whose average file is under N KiB", but an explicit list is easier to review.
2. **One torrent per year** for the rest, plus one for all years before 2018 or so, or split per dataset where a
   year is still too big. Decide with a per-dataset-per-year breakdown (a `find` report addition) and the real
   estimate on real paths.
3. Decide which limit to target (libtorrent defaults or qBittorrent's) after that; with the small snapshots gone,
   the difference may not matter much.

This would also change the "Never archived" list (2.2) and the docs page (4.2), and makes `--year` cumulative
selection less of a problem: only the big datasets accumulate.

**The "(by mtime)" entries** (24,189 files, ~606 GiB; 560 GiB of it the Fuzzwork backfills). Each needs a decision
(RUNBOOK B2):

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

Suggestion: exclude `ccp/iec`, `ccp/ccp_quant`, `ccp/csm`, `ccp/qen`, `ccp/sde/older` and probably `ccp/portraits` as
**reference collections** (~29 GiB, cheap to keep, linked from docs and likely downloaded by URL). The CCP
reference collections are a different kind of data from the scraped history, which is what the archive is for.

**Follow-ups for `find.py`:**

- A per-dataset-per-year breakdown, to size per-year torrents once the small snapshots are excluded.
- The estimate per year as well as for the whole selection.
- An "average file size" column per dataset, to make small-snapshot datasets obvious.
