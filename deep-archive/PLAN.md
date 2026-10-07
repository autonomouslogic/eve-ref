# EVE Ref Deep Archive — Process & Planning

Status: **draft for review** (2026-10-08). Nothing implemented yet.

Related work in progress (not committed, as of 2026-10-08):
- Tests written first, for changes not yet made. They fail against the current code, as intended:
  - `DataCrawlerTest.shouldFailOnMissingDirectoryIndex`: `DataCrawler` must fail when a directory index is missing
    (2.17);
  - `SyncFuzzworkOrdersetsTest` (new, plus its `inject` line in `TestComponent`): the Fuzzwork ID cutoff and its
    guard (2.18).
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

### 2.1 Unit of archiving: one torrent per closed year, all datasets

- **One torrent per year: `everef-deep-archive-YYYY.torrent`.** It contains that year's files from **every** in-scope
  dataset, e.g. `market-orders/history/2025/…`, `public-contracts/history/2025/…`, `ccp/sde/2025/…`.
- Runs are ad hoc, **at most once per calendar year** by convention, to keep the number of torrents small and the
  process simple. There is no hard guard in the scripts.
- The first run produces one torrent per eligible year, back to the earliest year on the site. Each later run produces
  one torrent per year that has become eligible since the last run: usually one, more if a year was skipped.
- A torrent can't be changed after it's published. Extra torrents for an already-archived year are possible but
  should be rare (2.3). Avoid them by settling the scope before the first run.
- **Size risk — check during the first find:** a full year across all datasets could have **hundreds of thousands of
  files**. Market order and contract snapshots are written several times a day, and Fuzzwork ordersets add more.
  - The `.torrent` file becomes large (tens of MB for the file list alone; hybrid torrents add more, see 2.4).
    Fetching metadata through a magnet link is slow, and some clients refuse very large metadata. libtorrent-based
    clients have limits on decode tokens and metadata size, and qBittorrent exposes those as advanced settings.
  - The find step prints the file count and the **estimated `.torrent` size** per year, and `check-torrent` loads
    the result in qBittorrent with default settings.
  - Fallback if a year is too big: split it per dataset (2.3). Decide after the first find run.

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

Also never included, from any dataset: `*-latest.*`, `index.html`, `index.json`, `totals.json`, and everything under
`deep-archive/`.

Everything else with a recognisable year folder is in scope. Using a denylist means:

- New datasets are archived automatically once they're old enough.
- `fuzzwork/ordersets` and `ccp/mer` **are** in scope. Their sync commands get cutoffs (2.18), so they don't
  re-upload deleted files.

Directories without a recognisable year folder (e.g. `fuzzwork/ordersets/backfills/`, `ccp/iec`, `ccp/ccp_quant`,
`characters-corporations-alliances`, depending on their actual layout) are **reported and skipped**, never guessed
at. They stay on HTTP.

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
  - `{YEARS}` comes from the **manifest**, not the ID: the distinct year folders of the data files. Normally one year
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
(`https://data.everef.net/deep-archive/`): the torrents, their file lists, the RSS feed and the catalogue. (Not to be
confused with `torrents/` in this repo, which holds the scripts and operator docs.)

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
  - Replaced torrents are listed in `torrents/superseded.txt` in this repo (`<old ID> <new ID>` per line).
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

Plain text, generated from a template in `torrents/`. Contents:

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

- `find` walks the public `index.json` files and writes, per candidate year, a file list (`files-YYYY.jsonl`: path,
  size, last-modified, URL). It skips excluded datasets, latest/index files, unrecognised layouts, and paths already
  listed in a published `.txt` (2.5).
- The operator reviews it and picks the years for the run. `start-year` **freezes** that year's list into the work
  directory (`files.jsonl`). That frozen list is the selection.
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
| Load | `rclone copy --files-from` with the `:http:` backend against data.everef.net | Already the documented download method. Concurrent and resumable. Copies exactly the selection. |
| Make torrent | Python `libtorrent` 2.x (hybrid v1+v2); alternatives in 2.4 "Creating the torrent". Not `mktorrent` (v1 only). | Mature library, same as qBittorrent. Don't write a torrent creator. |
| Seed | `qBittorrent-nox` in Docker, through its Web API | Headless and scriptable, and it's what most feed subscribers use. |
| Upload | `rclone copyto` to the B2 S3 remote with explicit content type and cache headers | Same bucket/credentials as `DATA_*`. |
| Feed + catalogue | Python script: list `deep-archive/`, parse each `.torrent`, write `torrents.json` and feed | Rebuilds from the bucket every time. |
| Backup | Python script building ZIP bundles, `rclone` to S3 with storage class `DEEP_ARCHIVE` | See 2.11. |
| Delete | Python script using the S3 API with an explicit key list (never a prefix delete) | Precise and auditable. |
| Index pages | Existing `data-index` command, full run | Reuse. |

All scripts run from a Docker image (`torrents/Dockerfile`) containing Python, rclone, the torrent creator and the
qBittorrent tooling, so nothing needs to be installed on the host.

### 2.10 Manual process — script principles

The workflow is **manual by design**: a human runs each step from `RUNBOOK.md` and double-checks the result before
moving on. The scripts support the human; they don't replace their judgement.

- **One script, one step.** Scripts never call the next step. There is no "run everything" script.
- **Run through the wrapper:** `torrents/run.sh <script> --dir <dir> [args]`.
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
    differs, **stop that year**: the data changed after it was selected (Appendix A).
  - Files in the bucket under that year's paths that are *not* in the selection (added later): **stop** and
    investigate. Never delete files that aren't in the torrent.
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

- **(a)** The delete script also deletes `index.html` and `index.json` in every directory it emptied. Simple, and
  self-contained in the process.
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
| `SyncMer` | Rescans from `MER_FIRST_MONTH` if no MER files exist. | Date cutoff (2.18). |
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
  have no year folder, so they're never archived and stay on the site. Fuzzwork's listing starts at 90019. If the
  backfills counted, the lowest ID would always be 21000 and the cutoff would never apply.
- **Guard:** if the data site has no dated ordersets at all, fail instead of syncing everything.
- Fuzzwork sequence IDs are incremental (confirmed), so the ID cutoff follows the archive cutoff automatically: once
  a year is deleted, the lowest remaining ID moves up.

**`SyncMer` — date cutoff.** Add an `ARCHIVE_MIN_AGE_YEARS` config to `Configs` (default `2`), shared with the
scripts through `torrents.env`. The start month becomes
`max(existing.isEmpty() ? MER_FIRST_MONTH : latest.minusMonths(2), January of (currentYear - ARCHIVE_MIN_AGE_YEARS + 1))`.
In practice MER is already mostly safe: it only goes back two months from the latest file it finds. The cutoff closes
the "all files gone, so re-sync from `MER_FIRST_MONTH`" hole.

Update `AGENTS.md` and the command docs for the new config.

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
| Operator (us) | `torrents/*.md` in this repo, alongside the scripts | How to run the workflow: `README.md`, `RUNBOOK.md`, `PLAN.md` (design, temporary). |
| Users of the data | docs.everef.net (`docs/src/…`) | What the torrents are, why they exist, how to use them, and when new ones appear. |

The docs site does **not** describe the internal workflow (scripts, checks, B2, backups). It links to the
`torrents/` directory on GitHub for anyone curious.

### 4.1 Operator docs (`torrents/`)

```
torrents/
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

1. **What it is.** Yearly BitTorrent archives of data.everef.net datasets, `everef-deep-archive-YYYY.torrent`, one per
   calendar year, containing that year's files from every archived dataset in the same paths as on the site.
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
     and let it recheck. You start seeding without downloading anything.
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
  - Operator docs in `torrents/` (4.1), kept in sync with the scripts.
  - `SyncFuzzworkOrdersets` ID cutoff, `ARCHIVE_MIN_AGE_YEARS` config and `SyncMer` cutoff (2.18). Deployed before the
    first deletion.
  - `DataCrawler` fails on a missing directory index (2.17).
  - Optional `DataIndex` changes: the "Archived years" note (2.20) and stale index cleanup (2.15b).
  - `torrents/` scripts and `Dockerfile`.
  - An `AGENTS.md` update describing the `torrents/` directory and process.
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

Decided so far: one torrent per year; cutoff `year <= current_year - 2`; exclusions `market-history` and `killmails`;
Fuzzwork/MER in scope with sync cutoffs; Fuzzwork cutoff at the lowest dated ID on the site; naming and IDs (2.3);
checksums and README inside the torrent; hybrid v1+v2; no notice period, but a start announcement; Glacier Deep
Archive backup; deletion last; no MD5 matching; no Cloudflare purge; `DataCrawler` fails on missing indexes; runs at
most yearly with no hard guard; a manual process with scripts in `torrents/`.

Still open:

1. Is one torrent per year workable? It depends on the file count and `.torrent` size from the first find run. The
   fallback is a split per dataset (2.3).
2. Glacier: which AWS account, bucket and region? Object Lock or versioning? Bundle size (~50 GB proposed)?
3. Bundle format: ZIP64 store-only (Internet-Archive-friendly) or tar?
4. Internet Archive: use it at all? Is redistribution there allowed for CCP and Fuzzwork data? Item grouping?
5. Seeder host: where is it, how much disk and upload, and does it have a redundant filesystem?
6. Where to run `test-download` so it actually tests reachability from outside (another machine on another network)?
7. Stale index cleanup: in the delete script (2.15a), in `DataIndex` (2.15b), or both?
8. Add the `DataIndex` "Archived years" note (2.20)?
9. `fuzzwork/ordersets/backfills/` stays on HTTP forever (no year folder). Leave it, or archive it once as its own
   torrent later?
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
  `torrents/superseded.txt`, and run Parts D and E for the replacement. Keep seeding the old torrent too, if possible.
- Then consider adding the dataset to `TORRENT_EXCLUDE`, since its old files evidently still change.
