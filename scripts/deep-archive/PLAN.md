# EVE Ref Deep Archive: Process & Planning

Status: **draft for review**. Every step is written; none has run against real data yet (R12).

Current shape (2026-10-25):

- **Steps 00-16**, numbered the same in the README, `STEPS` (`lib/workflow.py`) and the script names (2.10); a unit
  test checks the script names against `STEPS`. Scripted: `00-preflight`, `01-find`, `03-load`, `04-sign-sha256`
  (bash), `05-make-torrent`, `06-sign-torrent` (bash), `07-seed`, `08-check-seed`, `09-publish`,
  `12-check-before-delete`, `13-delete`, `14-check-delete`, `16-check-site`. Manual: `02` review, `10` update-docs,
  `11` index-deep-archive, `15` data-index, `17` delete-scratch.
- **One workflow, one torrent, as big as the size limits allow** (2.1, 2.8): the oldest files not archived yet, by
  date (2.2). Sequential torrent IDs, `everef-eve-online-deep-archive-NN` (2.3); the cutoff date is stated in the
  torrent's own README.
- **Limits** (2.8): a `.torrent` estimate of at most 10 MB (hard), `MAX_BYTES` 1 TiB (soft), `MIN_BYTES` 500 GiB,
  32 MiB pieces.
- **Inside the torrent** (2.6): the data, a README, a `sha256sum` checksum file and its minisign signature.
  **Published** in `deep-archive/` (2.5): the `.torrent`, its `.txt` file list, the `.torrent`'s own minisign
  signature, and the RSS feed (2.7).
- **The docs page lists every torrent** (4.2, 4.3): `10` update-docs adds it, and `12-check-before-delete` fails until
  the live page lists the torrent's ID and v1 info-hash.
- **The scripts reach the storage only through rclone** (2.21), with one B2 key; soft deletes only.
- **No backup** (2.11), **no Internet Archive** (2.12), **no announcements** (2.13). `$SEED_DIR` is EVE Ref's only copy
  after deletion.
- **Java changes**, tests first: `DataCrawler` fails on a missing directory index (2.17); `SyncFuzzworkOrdersets`
  syncs only IDs at or above the lowest dated ID on the site, gaps filled (2.18), and must be deployed before the first
  `fuzzwork/` deletion.
- A GitHub Actions reminder (`.github/workflows/deep-archive-reminder.yml`) pings the dev notices Discord on 15
  February and 15 August, the day after that month's rollup check.

Why this is worth doing: the archive is seeded from a local copy, and the community will likely help seed it, so
moving old data off the data storage cuts hosting costs without losing the data. The data stays on B2, which then
needs to hold much less (open questions 13, 14).

## Scope

The **EVE Ref Deep Archive**: old data.everef.net data, moved into torrents.

A documented, repeatable workflow that makes **one torrent** each time it runs:

1. **Select** the oldest data on data.everef.net that isn't archived yet, as much as fits in one torrent. The
   selection is a fixed file list, and every later step is checked against it.
2. **Load** the selected files into the seed directory.
3. **Make** the `.torrent` file, and sign it.
4. **Seed** it, and test it.
5. **Upload** the `.torrent` file and its file list to data.everef.net.
6. **Add** it to an **RSS feed** on data.everef.net, so other people can subscribe and automatically start seeding
   new torrents as they are published.
7. List the torrent on the **docs site** (docs.everef.net): its ID, info-hashes and magnet link (4.3).
8. **Delete** the archived files from data.everef.net. This is the **last** step, done only after everything above
   is verified.
9. Run a **full `DataIndex`** job and check the site.

Workflows run ad hoc, one at a time. A workflow is always completed (published, deleted) before the next
one starts. There is **no notice period** between publishing a torrent and deleting its files, and no
announcement (2.13).

The process is **manual**: a human runs each step from [`README.md`](README.md), with supporting scripts and a check
after every step.

Deletion is the only destructive step. After deletion, the copies of the data are the seed directory and whatever
community seeders hold (no backup: 2.11).

Dates and IDs in this document (`everef-eve-online-deep-archive-00`, `2023-04-11`, …) are **examples**.

---

## 1. Context

- data.everef.net hosts roughly **5.7 TB**.
- Storage today is **Backblaze B2**. The Java code uses its S3-compatible API (`DATA_S3_ENDPOINT_URL` in
  `local.env`). It **stays on B2** (2026-10-25), with a lower storage need once old data is in torrents. **The Deep
  Archive scripts only use rclone** (2.21), so moving them to other storage would only be a matter of rclone remotes.
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
  everything dated (2.2) on or before that date that isn't in an earlier torrent (2.8). The torrent ID is
  sequential (2.3): `everef-eve-online-deep-archive-00`, `everef-eve-online-deep-archive-01`, and so on, picked by
  `00-preflight` from the highest published one. The cutoff date is recorded in the torrent's own README (2.6).
- The cutoff is chosen to make the torrent **as big as possible** within hard-coded limits on the `.torrent` size
  and the bytes (2.8). Those limits come from what torrent clients accept (2.4 "Creating the torrent", Appendix B): a `.torrent`
  with hundreds of thousands of files is too big for libtorrent's defaults, and qBittorrent's WebUI hung on 43,620.
- One workflow makes one torrent, and is completed (published, deleted) before the next starts. The first
  workflows catch up on the backlog (~800,000 files and ~4 TiB on 2026-10-08, Appendix B), one torrent at a time; after
  that, a workflow runs whenever enough data has passed the minimum age to fill a torrent.
- A torrent can't be changed after it's published, and is never replaced. Files that change before their delete
  stay on the site, and the next `01-find` stops with a hard error on them (2.2, Appendix A).

### 2.2 What gets archived

**Files at least 2 years old, by date** (`MIN_AGE_YEARS`). A file's date is the **earliest** of:

- its modification date: rclone's `ModTime`, in UTC. On B2 that's the upload time, unless the uploader set
  `src_last_modified_millis` (EVE Ref's Java uploads go through B2's S3 API, which doesn't; check on real files). So
  it's when EVE Ref stored the file;
- any ISO date (`YYYY-MM-DD`) in the file name;
- 31 December of any year (a 4-digit number from 2003, EVE's release, on) in the file name.

Only the file name counts, not the folders. For scraped data, the upload date and the name's date are close; for
backfills and re-uploads, the upload date can be much later than the data, and the name's date puts the file with its
period. A 4-digit ID in a name would read as a year and date the file too early: the find report counts the files
dated each way, and `find.log` lists samples dated by a year in the name, to check.

The snapshot lookback used by the market history scraper (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`) is only 30 days,
so the 2-year minimum age is safe for `market-orders`.

#### Never archived

A denylist, hard-coded in `lib/workflow.py` (`TORRENT_EXCLUDE`):

- `market-history/` and `killmails/`. These two datasets work as a **database**, not as snapshots: past files are
  modified in place. A torrent can't follow that. (For `market-history`, `ScrapeMarketHistory` also re-reads the last
  `ESI_MARKET_HISTORY_LOOKBACK` (450 days) of files on every run, and `import-market-history` depends on the full
  history being on HTTP.)
- `ccp/`. CCP's own data (SDE, MER, image export collections, CSM results, QENs, etc.): reference material, ~62 GiB.
  **Excluded for now (2026-10-25), to revisit later:** the docs link its old files and folders directly (`sde.md`
  links `ccp/sde/older/`; the datasets index links `ccp/ccp_quant/`, `ccp/iec/`, `ccp/csm/`), and those links would
  break once a torrent took them. Archiving it needs those docs changed first. The syncs wouldn't stand in the way:
  `SyncMer` only checks from two months before its latest MER, and `SyncStaticData` only the latest SDE, so neither
  would re-upload archived files.
- `deep-archive/`. The published torrents and their file lists (`DEEP_ARCHIVE_DIR`).

Also never included, from any dataset: `*-latest.*`, `index.html`, hidden files, and files at the root of the site
(e.g. `robots.txt`).

**`index.json` files are included with their folder:** a folder's `index.json` is selected whenever any file
directly in that folder is, whatever its own modification date. It's never selected on its own date: `DataIndex`
rewrites every index on each full run, so that date is the last `DataIndex` run, not the data's, and an `index.json`
would never reach the minimum age. Only the folder's own `index.json`; parent folders' aren't added. Consequences:

- In the torrent they're data files like any other: in the selection and the `.txt`. They count
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
  files. `ccp/mer` isn't (excluded with `ccp/`); `SyncMer` wouldn't need a change if it were (above).

**No path lands in two torrents.** `01-find` reads every published `<ID>.txt`. A listed non-index path still on the
site (and not skipped) is a hard error, whatever its date: an earlier delete didn't happen, the path reappeared, or
the pre-delete check left it on the site (Appendix A). `01-find` aborts listing every such path, rather than guessing
whether it changed and silently re-selecting or re-skipping it. `index.json` is exempt: `DataIndex` rewrites it on
every run regardless of the underlying data, so the same folder's `index.json` legitimately recurs across several
torrents' file lists as a folder is archived in parts. `01-find` never reads an earlier torrent's cutoff back: its
own cutoff is always picked fresh from this run's candidates.

**Risk of selecting by date:** a file that is still current but simply hasn't changed in two years
would be archived and deleted from the site, e.g. a reference file a tool downloads by a fixed URL (`ccp/iec`,
`ccp/sde/older`, Appendix B). The find report breaks the selection down by dataset, marks datasets with no year folder,
and the operator must check them (README, "Review the selection"). Add anything that must stay to `TORRENT_EXCLUDE`.

**Optional allowlist (`TORRENT_INCLUDE`, 2026-10-25).** `None` by default: everything above. Set to a list of folders
(paths from the site root), `01-find` selects only files in them; `TORRENT_EXCLUDE` still applies within them, and
root files stay root files. Everything else is counted as "not included" (by top-level folder) in the report, and
`05-make-torrent` checks the `.txt` has no path outside it. Meant for the first torrents: archive the biggest datasets
first, to get them out of the way, then set it back to `None`. The cutoff is chosen among the included files only, so
a later torrent can hold other datasets' files dated before an earlier torrent's cutoff; nothing relies on cutoffs
being in order (2.3, `01-find` never reads one back).

### 2.3 Torrent IDs, naming and layout

Every torrent has an **ID**. The ID is used everywhere: torrent name (root folder), `.torrent` file, `.txt` file
list, files inside the torrent, seed folder.

**The ID is sequential, not the cutoff date:** `everef-eve-online-deep-archive-NN`, a two-digit sequence number
starting at `00`. `00-preflight` lists `deep-archive/`'s published `.torrent` files and picks one past the highest
sequence number found (`00` if there are none); `01-find` uses it. The torrent's cutoff is stated in its own README,
inside the torrent only (2.6).

| Case | ID |
|---|---|
| Normal | `everef-eve-online-deep-archive-<NN>`, e.g. `everef-eve-online-deep-archive-00`, `-01`, ... |

Sequence numbers only move forward (each new ID is one past the highest published), so IDs sort in publication
order. Two digits caps this at 100 torrents (`00`-`99`); `00-preflight` fails once that's reached, rather than
silently widening the format.

Inside a torrent, the `name` is the ID and file paths are the **full site paths**:

```
name:   everef-eve-online-deep-archive-00
files:  everef-eve-online-deep-archive-00-README.md       # what this is (2.6)
        everef-eve-online-deep-archive-00.sha256          # SHA-256 of every data file (2.6)
        everef-eve-online-deep-archive-00.sha256.minisig  # its signature (2.6)
        market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-00-00.v3.csv.bz2
        public-contracts/history/2023/2023-01-01/public-contracts-2023-01-01_00-00-00.v2.tar.bz2
        fuzzwork/ordersets/2023/...
        ...
```

- By default a download lands in `everef-eve-online-deep-archive-00/market-orders/history/2023/…`. That's clear, and the
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
  by `make-torrent` from a template file, `lib/comment-template.txt`, never typed by hand. One line: the sequence
  number and the docs page, no years or datasets. Template:
  ```
  EVE Ref Deep Archive for EVE Online #{sequence} - https://docs.everef.net/datasets/deep-archive.html
  ```
  Example:
  ```
  EVE Ref Deep Archive for EVE Online #00 - https://docs.everef.net/datasets/deep-archive.html
  ```
  - `{sequence}` is the torrent ID's (2.3) two-digit sequence number, e.g. `00`. `render_comment` fails for a name
    that isn't a torrent ID.
  - `load_comment_template` (`lib/workflow.py`) trims the file and fails if the result has more than one line: the
    comment is kept deliberately short, and a template edit can't silently turn it into several lines.
  - `make-torrent` reloads the torrent and compares its comment with the one rendered from the template.
  - qBittorrent and most other clients show the comment in the torrent's details, and qBittorrent makes URLs in it
    clickable.
  - The comment is outside the `info` dictionary, so it doesn't change the info-hash. Published `.torrent` files are
    never republished, though (open question 11), so it's fixed once published.
  - Users who add the torrent from a **magnet link** don't get the comment: only the `info` dictionary is fetched from
    peers. For them, the README inside the torrent carries the same link (2.6).
- `created by`: EVE Ref.

#### Trackers

**Decided (2026-10-12): the union of both "best" lists, downloaded when the torrent is made.** `05-make-torrent`
downloads ngosang's `trackers_best.txt` and XIU2's `best.txt` (`lib/trackers.py`), keeps the UDP and HTTP(S)
trackers, and adds their union, ngosang's first, one per tier. It fails if either list can't be downloaded or has no
usable trackers, or if the union has fewer than 10 (`MIN_TRACKERS`); `00-preflight` checks the same. Both lists, as
downloaded, and the union (`trackers.txt`) are saved in `$SCRATCH_DIR/trackers/`. There is no tracker list in the
repo and no fixed core list.
Magnet links in the feed carry each torrent's first 10 trackers (`MAGNET_TRACKERS` in `09-publish`). That's about 70
trackers today (XIU2's `best.txt` holds all of ngosang's): more than option 2 below, with the decay, load and
unwanted-domain concerns noted there, and no denylist. **No tracker refresh (decided 2026-10-25, open question 11):**
a published `.torrent` is never republished, so its trackers are fixed when it's made; DHT, PEX and the magnet
links' `tr=` carry the rest. The refresh ideas in the notes below are not planned.

**Notes for review, researched 2026-10-08.**

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

- Snapshot the lists at creation time into `$SCRATCH_DIR/trackers/` and record where they came from, so a torrent
  can be reviewed.
- `make-torrent` checks the torrent's trackers equal the snapshot, and one tracker per tier.
- Magnet links in the feed and on the docs page carry only the core trackers, to keep them short.
- Consider a **tracker refresh** later: re-publish `.torrent` files with a current list. Same info-hash, so nothing
  breaks. This needs `publish` to allow replacing a `.torrent` when the info-hash is unchanged.

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
  and a v2 file-tree entry) can pass 10 MB and millions of tokens. **This is why `01-find` caps the estimated
  `.torrent` size** (2.8), and its report checks the estimate against these limits, not just against qBittorrent.
- **Deterministic content.** Sort files by path. Then the same files always give the same info-hash. The creation
  date and trackers are outside the info dictionary, so they don't change it.
- **Keep the creation date.** The feed's `pubDate` uses it (2.7).
- **No `source` field, not private, no web seeds** (above).
- **Verify after creating.** Load it in a client and recheck against the content (`07-seed`, `08-check-seed`).

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
(`https://data.everef.net/deep-archive/`): the torrents, their file lists and the RSS feed. (The
folder with the same name in this repo holds the scripts and operator docs; it isn't published.)

```
deep-archive/                       # at the root of data.everef.net
  everef-eve-online-deep-archive.xml                          # RSS: all torrents
  everef-eve-online-deep-archive-00.torrent
  everef-eve-online-deep-archive-00.txt         # every data file in the torrent, one site path per line
  everef-eve-online-deep-archive-00.torrent.minisig # signature of the .torrent file itself (2.6)
  everef-eve-online-deep-archive-01.torrent
  everef-eve-online-deep-archive-01.txt
  everef-eve-online-deep-archive-01.torrent.minisig
  ...
```

- **"Already archived" is decided per file:** `01-find` downloads every published `<ID>.txt` (a `.txt` whose name
  isn't a torrent ID is ignored); a listed non-index path still on the site is a hard error (2.2), never something
  decided by date.
- The feed is **regenerated in full** from the `.torrent` files in `deep-archive/` each time, never
  edited by appending. The bucket's `deep-archive/` stays the single source of truth.
  - Publication date comes from the torrent's `creation date` field.

#### File list: `<ID>.txt`

For quick review and search without having to parse torrent files:

- Plain text, UTF-8, `\n` line endings, ending with one. **One file path per line**, as the full site path
  (without the torrent's root folder), **sorted** by code point, no duplicates. Data files only: not the torrent's
  root files (README, checksum file, its signature). Inside the torrent, every data file has this same path under the
  root folder (2.3). For example:
  ```
  fuzzwork/ordersets/2023/2023-01-01/fuzzwork-orderset-100000-2023-01-01_00-00-00.csv.gz
  market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-00-00.v3.csv.bz2
  ...
  ```
- Generated **from the `.torrent` itself** after it's made, minus the root files. `make-torrent` compares it with
  the manifest.
- The format is checked by `file_list_problems` (`lib/workflow.py`): `make-torrent` checks the new list, and
  `01-find` fails if any published list doesn't pass. A list in the wrong format (say, with the torrent's root folder
  in the paths) would otherwise match no site path, and its files could land in a second torrent without any error.
- Paths match the site layout, so someone looking for e.g. "public contracts from March 2024" can `grep` it and
  knows which torrent to grab and which file to select.
- Content-Type `text/plain; charset=utf-8`, set on the object at upload (2.21). The `.torrent` size limit keeps a
  torrent to tens of thousands of files, so a list of a few MB at most. Cloudflare compresses it in transit.

### 2.6 Files inside the torrent: README, checksum file and its signature

The README (`<ID>-README.md`), the checksum file (`<ID>.sha256`) and its minisign signature (`<ID>.sha256.minisig`)
all ship **inside the torrent**, at the torrent root (`root_file_names` in `lib/workflow.py`). None of the three is
published separately on the data site.

**Their names start with the ID on purpose.** With "Don't create subfolder", root files from every torrent land in
the same directory. A generic name like `README.md` would collide.

#### `<ID>.sha256` and the signatures

- `03-load` computes every data file's SHA-256 into `manifest.json`, writes `<ID>.sha256` from it (standard
  `sha256sum` format, sorted by path), and checks it with `sha256sum -c`: a second, independent read of the hashes.
- `04-sign-sha256` signs it with minisign (`<ID>.sha256.minisig`). `05-make-torrent` verifies the signature, then
  copies both into the torrent's content folder.
- `06-sign-torrent` signs the `.torrent` file itself (`<ID>.torrent.minisig`). That signature **is** published,
  next to `<ID>.torrent` and `<ID>.txt` in `deep-archive/`, by `09-publish`, which verifies it first: it lets someone
  check the `.torrent` they downloaded before they have any piece hashes to lean on. The checksum file's signature
  covers files *extracted from* an already-trusted torrent, including single files found another way (e.g. a mirror).
- Both signing steps are bash (`lib/sign.sh`), with completion files like the Python steps; the private key's
  passphrase is always typed in (`MINISIGN_KEY_PATH`). `03-load` removes an earlier run's `<ID>.sha256.minisig`,
  `05-make-torrent` an earlier `<ID>.torrent.minisig`, and `05-make-torrent` and `09-publish` verify before use, so a
  signature of a file since remade is never shipped.

The public key for both is published at `https://everef.net/.well-known/minisign.pub` (from
`ui/public/.well-known/minisign.pub` in this repo), on the main site rather than the data bucket, so whoever can
write to the bucket still can't replace the key.

#### `<ID>-README.md`

Markdown, generated from `lib/README-template.md` by `05-make-torrent`. Contents:

1. What this is: an EVE Ref Deep Archive torrent, with data from data.everef.net dated up to the torrent's cutoff
   date. This is the only place the cutoff is recorded.
2. Links: docs page (docs.everef.net/datasets/deep-archive.html) and data.everef.net.
3. How to verify the files with the checksum file and its signature (minisign, or minisign through Docker).
4. Licence/attribution notes, the same as the data site (CCP data, from `LICENSE-CCP`).

#### Checks

- `make-torrent` checks the torrent's file list equals the manifest paths plus exactly the root files (README,
  `.sha256`, `.sha256.minisig`).
- `check-seed` downloads the README through the torrent with aria2c, and checks it's identical to the seed
  directory's copy. No data files are test-downloaded (2026-10-12: the README is a good enough test).

### 2.7 RSS feed format

RSS 2.0, one `<item>` per torrent, compatible with qBittorrent's RSS auto-downloader and FlexGet/autodl-style tools:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:torrent="http://xmlns.ezrss.it/0.1/">
  <channel>
    <title>EVE Ref Deep Archive</title>
    <link>https://docs.everef.net/datasets/deep-archive.html</link>
    <description>Archived datasets from data.everef.net</description>
    <item>
      <title>everef-eve-online-deep-archive-00</title>
      <link>https://data.everef.net/deep-archive/everef-eve-online-deep-archive-00.torrent</link>
      <guid isPermaLink="false">INFOHASH_V1</guid>
      <pubDate>Wed, 07 Oct 2026 12:00:00 GMT</pubDate>
      <description>EVE Ref Deep Archive: everef-eve-online-deep-archive-00</description>
      <enclosure url="https://data.everef.net/deep-archive/everef-eve-online-deep-archive-00.torrent"
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
- The total data size goes in `torrent:contentLength`, so subscribers can filter by size. The magnet link carries
  the torrent's first 10 trackers (`MAGNET_TRACKERS`).
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
  - **Too new:** anything dated (2.2) less than `MIN_AGE_YEARS` (2) years before today.
  - **Cutoff:** group the remaining candidates (`index.json` aside, 2.2) by date (2.2), and add them oldest first,
    each with their folders' `index.json`. A date is never split. (Revised 2026-10-24.)
    - `MAX_TORRENT_BYTES` is **hard**: the first date that takes the `.torrent` estimate over it is left out, and the
      cutoff is **the day before**. If that's the oldest date, `01-find` fails.
    - `MAX_BYTES` is **soft**: the first date that takes the data to it or over is the cutoff, included whole. The
      torrent can be over `MAX_BYTES` by up to that date's data.
    - If neither is reached, the cutoff is the newest allowed date (and the report flags that the torrent could be
      bigger).
    - Then, if the selection holds less than `MIN_BYTES`, `01-find` fails: not enough data for a torrent yet.
  - **Torrent ID** comes from `00-preflight` (sequential, 2.3), not the cutoff: the torrent holds everything not yet
    archived, dated on or before the chosen cutoff, which is instead recorded in the torrent's own README (2.6).
  - Output in `$SCRATCH_DIR/find/`: `files.jsonl` (path, size, last-modified, date, what dated it, URL),
    `listing.jsonl.gz` (the full listing the selection came from), `report.txt`, `summary.json`, `find.log`. The
    report breaks the selection down by what dated each file, date month, year folder in the path, and dataset, and
    flags selected `index.json` files whose folder
    keeps files on the site, and selected folders with no `index.json` to select.
- **Limits, hard-coded in `lib/workflow.py`:**
  - `MAX_BYTES` = 1 TiB, soft: roughly what the seeder holds per torrent, and what a full download is.
  - `MIN_BYTES` = 500 GiB: the least data worth a workflow.
  - `MAX_TORRENT_BYTES` = 10,000,000, hard: the largest `.torrent`, libtorrent's default `max_buffer_size`. This is
    what limits the file count. `lib/torrent_size.py` estimates the hybrid `.torrent` from the selection's paths and
    sizes as files are added, mirroring libtorrent's layout (v1 entries and pad files, v1 piece hashes, v2 file tree
    and piece layers), plus a 16 KiB allowance (`FIXED_BYTES`) for trackers, comment and root files. The per-file
    part matched libtorrent byte for byte in tests; the allowance makes the estimate run up to about 12 KB high, so
    a `.torrent` at the limit still loads.
- **`files.jsonl` is the selection.** `01-find` can't run twice in a workflow (2.10), so it can't change afterwards.
  To change it on purpose, delete `01-find`'s completion file and run it again.
- `03-load` copies **exactly** the files in the list into `$SEED_DIR/<ID>/` with `rclone copy --files-from-raw`:
  parallel, resumable, and it keeps each file's modification time. With `LOCAL_COPY_DIR` set (a local copy of the
  site, a shortcut that is never trusted), the selected files it has are copied first, unchecked; then the whole
  selection is copied from the bucket with `--checksum`, which replaces any file that doesn't match the bucket's
  SHA-1. Files the bucket has no SHA-1 for always come from the bucket.
- `03-load` then checks every listed file is on disk with the listed size and modification time, and nothing else
  is, computes SHA-256 for every file into `manifest.json` (the selection plus hashes), and writes `<ID>.sha256` from
  it (2.6). The manifest feeds the torrent checks and the delete set.
- The delete set comes from the torrent, confirmed file by file against a fresh listing of the bucket by **size
  and modification time** (2.14).
- **No MD5/ETag matching anywhere.** Size and timestamp are enough to detect changes; content integrity from load
  onwards is covered by SHA-256 (manifest, torrent piece hashes).

### 2.9 Tools

| Step | Tool | Why |
|---|---|---|
| Preflight | `./00-preflight.py` | Fails early on a wrong setup, before anything is selected. |
| Find | `./01-find.py`: `rclone lsjson` of the whole bucket | One B2 request per 1,000 files; sizes and modification times straight from the storage. |
| Load | `rclone copy --files-from-raw` from the bucket (after copying what `LOCAL_COPY_DIR` has, if set, unchecked; `--checksum` replaces what doesn't match) to `$SEED_DIR/<ID>/`; `sha256sum -c` | Concurrent and resumable. Copies exactly the selection. |
| Sign | `minisign` through Docker (`jedisct1/minisign`), from bash (`lib/sign.sh`) | Small, standard signatures. Docker because minisign isn't readily available from package managers. |
| Make torrent | Python `libtorrent` 2.x (hybrid v1+v2); alternatives in 2.4 "Creating the torrent". Not `mktorrent` (v1 only). | Mature library, same as qBittorrent. Don't write a torrent creator. |
| Seed | `qBittorrent-nox` in Docker, through its Web API | Headless and scriptable, and it's what most feed subscribers use. |
| Upload | `rclone copyto` into the bucket's `deep-archive/`, with Content-Type and Cache-Control (no overwrite), then checking over HTTPS | Same metadata as the Java uploads (`S3Util`). |
| Feed | Python: list the bucket's `deep-archive/` and fetch each `.torrent` with rclone, write the feed | Rebuilds from the storage every time. |
| Delete | `rclone delete --files-from-raw` with the explicit list (never by prefix), soft deletes only | Precise and auditable. |
| Index pages | Existing `data-index` command, full run | Reuse. |

The scripts run directly on the host, with the tools in the README's Requirements. Only minisign runs through Docker.

### 2.10 Manual process: script principles

The workflow is **manual by design**: a human runs each step from `README.md` and double-checks the result before
moving on. The scripts support the human; they don't replace their judgement.

- **One script, one step, numbered in order:** `./00-preflight.py`, `./01-find.py`, …
  (`STEPS` in `lib/workflow.py`). Scripts never call the next step. There is no "run everything" script. The two
  signing steps are bash (`04-sign-sha256.sh`, `06-sign-torrent.sh`, through `lib/sign.sh`), with the same rules.
  Manual steps take a number in the same sequence (`02` review, `10` update-docs, `11` index-deep-archive,
  `15` data-index, `17` delete-scratch) without a script or completion file (`MANUAL_STEPS`).
- **Settings in `torrents.env`** (`torrents.env.example`), read for anything the environment doesn't set:
  `SCRATCH_DIR`, `SEED_DIR`, `DATA_BUCKET`, the B2 key (`RCLONE_CONFIG_EVEREF_ACCOUNT`,
  `RCLONE_CONFIG_EVEREF_KEY`), the qBittorrent Web API, `MINISIGN_KEY_PATH` and, optionally, `LOCAL_COPY_DIR` and
  `QBITTORRENT_SAVE_PATH`. Everything else (rclone settings, size limits, `TORRENT_EXCLUDE`, `TORRENT_INCLUDE`, `DATA_BASE_URL`) is fixed
  in `lib/workflow.py`. No `--dir` or ID arguments. Every step names any setting the environment overrides the file
  with (R2).
- **One workflow per scratch directory.** `$SCRATCH_DIR` holds the work files of the current workflow and must be
  empty when it starts (`00-preflight` checks). The operator deletes it by hand once the workflow is complete.
  Torrent content goes in `$SEED_DIR/<ID>/` instead, which is kept for seeding. Neither directory is inside the
  other.
- **Completion files.** Every step writes `$SCRATCH_DIR/<NN>-<step>.done` (JSON: step, torrent ID, time,
  summary) as the last thing it does, only on success. A step **refuses to start** without the previous step's
  completion file, and refuses if its **own** exists: no step is skipped, and none runs twice by accident. Redoing a
  step is deliberate: delete its completion file and every later one by hand. The torrent ID comes from
  `01-find.done`.
- **Every action has a separate check step.** E.g. `07-seed` is followed by `08-check-seed`. Checks are read-only,
  re-derive everything from source (bucket, disk, qBittorrent, site), and never trust the action's output.
  Exceptions, like the roll-up scripts' steps: `03-load` checks its own download and writes the manifest;
  `04-sign-sha256` and `06-sign-torrent` verify their own signature; `05-make-torrent` reloads the `.torrent` and
  `.txt` it wrote and checks them; `09-publish` checks what data.everef.net serves for what it uploaded. They re-read
  from disk or the site rather than trusting in-memory state, but they're not independent of the action (Review
  notes, R6).
- **Show, then ask.** `13-delete` prints what it will delete (counts, bytes, sample paths) and needs the torrent ID
  typed back in, not just `y`. The other steps that write outside `$SCRATCH_DIR` and `$SEED_DIR` (`07-seed` to
  qBittorrent, `09-publish` to the bucket's `deep-archive/`, which never overwrites) print what they do and go ahead.
- **Resumable until complete.** A step that fails or is interrupted writes no completion file and can be run again;
  copies and uploads carry on where they stopped.
- **Output like the roll-up scripts** (`lib/workflow.py`): `🚀 <step>: <ID>` once, `🔵 <section>` headings,
  plain lines for results, `🟡 <message>` on stderr for problems the step carries on past, `🔴 <message>` on stderr
  for each problem that stops it (then exit 1), and `✅ <step> OK …` as the last
  line on success. Everything a step prints is also written to `$SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log`.
- **Notes.** The operator keeps `$SCRATCH_DIR/NOTES.md` for notes on manual steps and decisions. It goes with the
  scratch directory.
- **One step at a time.** Every step, the bash ones and `13-delete`'s dry run included, holds
  `$SCRATCH_DIR/log/.lock` while it runs (R1).

### 2.11 Backup: none (removed 2026-10-12)

There is **no backup** of the torrent data beyond the seed directory and community seeders (2.19).

Earlier designs backed every torrent up to **S3 Glacier Deep Archive**: first as ~50 GB store-only ZIP64 bundles with
an index (`<ID>-backup-index.tsv`) saying which bundle held which file, then as a single uncompressed `<ID>.tar`
uploaded with the AWS CLI and a dedicated AWS key (`prepare-backup`, `check-backup-plan`, `backup`, `check-backup`
and a manual restore drill). All removed, because of what a restore would cost:

- Storage is cheap (about $1/TB/month), but getting the data back means AWS data transfer out, about $90/TB:
  roughly $500 for the 5.7 TB, plus retrieval fees, 12–48 hours, and 180-day minimum storage per object.
- Deep Archive restores whole objects, so a single tarball meant restoring all of a torrent to recover any file.
- It needed its own AWS account and key, the AWS CLI, a 1 TiB `$SCRATCH_DIR` for the tarball, and four steps.

What it leaves: **`$SEED_DIR` is EVE Ref's only copy** of archived data once it's deleted from the site. If it's lost
and nobody else seeds a torrent, that data is gone. **Accepted (2026-10-25, Review notes, R23).** Options considered
for a second copy, if that's ever revisited: a second seeder (another host, or a friendly community seeder), an
offline disk kept off site, or B2 itself (its egress to Cloudflare is free). Not the Internet Archive (2.12).

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

- A torrent has tens of thousands of files and up to about 1 TiB: over the recommended item size and file count, and up to the hard
  1 TB limit. It **doesn't fit one item well**, so the Internet Archive can't mirror the files one-to-one.
- It **can't be a web seed for our torrents.** A web seed (BEP 19) needs every file at `<url>/<torrent name>/<path>`,
  which would need one item holding the whole torrent's files.
- What does fit: **bundles** of a torrent's files (e.g. ~50 GB store-only ZIP64 parts, built from `$SEED_DIR`). About
  50 GB per bundle, grouped into items of at most ~500 GB
  (e.g. items `everef-eve-online-deep-archive-00-part-001-010`), plus the metadata files. That gives:
  - a **second, free, off-site copy**, independent of the seed host: with no backup (2.11), the only one;
  - **public HTTP downloads** of the archived data, in bundle form, without any seeders;
  - possibly **single-file downloads** through ZIP "View contents" (needs testing on large ZIP64 files);
  - an always-seeded torrent per item, via the Internet Archive's own web seed.
- Upload is slow at TB scale, and processing can take hours or days. **Deletion must not wait for it.** The Internet
  Archive is an extra, not part of the delete criteria.

**Caveats:**

- It isn't a guaranteed backup: items can be taken down or made dark, and the site has had long outages.
- Content rules: the uploader must have the right to share the data. Check that CCP's licence (and Fuzzwork, for
  ordersets) allows redistribution through the Internet Archive.
- For uploads at this scale, contact the Internet Archive first.

**Decided (2026-10-24): not used** (open question 4).

Sources: [Archive BitTorrents](https://help.archive.org/help/archive-bittorrents/),
[Uploading – Tips](https://help.archive.org/help/uploading-tips/),
[Bulk uploading](https://help.archive.org/?p=433), [BEP 19](https://www.bittorrent.org/beps/bep_0019.html).

### 2.13 Announcements: none (removed 2026-10-12)

No announcement is posted when a workflow starts or finishes. Earlier designs had two manual steps, `announce` (a
start notice on Discord right after `01-find`) and `announce-result` (a finish notice), both removed. Users learn
about new torrents from the RSS feed and the docs page (4.2).

### 2.14 Deletion design

**Deletion is the last step of a workflow.** It happens only after the torrent has passed publishing, seeding,
and testing.

- **Only files confirmed to be in the torrent are deleted.** The delete set is built from the **torrent itself**,
  not from the selection, and every file in it is confirmed against a **fresh listing** of the bucket. A data file
  is in the delete set only if all of these hold:
  - it's in the published `.torrent` (`deep-archive/<ID>.torrent`, read back from the bucket, with the same
    info-hash as `$SCRATCH_DIR/<ID>.torrent`), at its site path under the root folder, with the size the torrent
    gives;
  - it's in the manifest, so its SHA-256 was computed from the loaded copy, which the seeder verified against the
    torrent (full recheck at 100%);
  - it's on the bucket now with the **size the torrent gives it**, and the **same size and modification time as in
    the selection**: the same version that was loaded. On B2 a new upload always gets a new modification time.
  - it isn't an `index.json` (they're rewritten by `DataIndex`, so they never match; 2.15 handles them), or one of
    the torrent's root files (README, checksum file, its signature: not site files).
- A file that fails a check **stays on the site**, and the step goes on with the rest. It's reported, and noted in
  `NOTES.md`. It's still at a path the published `<ID>.txt` lists, so the next `01-find` stops with a hard error on
  it (2.2, Appendix A). Many failures point at a problem rather than at changed data: look before executing.
- Files in the same folders that aren't in the torrent (uploaded after the cutoff) **stay**. Only confirmed keys are
  ever deleted.
- Deletes are an explicit list (`rclone delete --files-from-raw`), never a prefix or a pattern.
- **Treat deletion as permanent.** On B2, rclone's delete **hides** the file: the `everef` remote has `hard_delete`
  fixed to `false` (2.21), and the bucket's lifecycle rule purges hidden files, currently after **2 days**. That
  window is short, so the process doesn't rely on it:
  - rollback is a re-upload by hand from `$SEED_DIR/<ID>/` (or from other seeders), using the published `<ID>.txt` as
    the list (README Appendix D; no script);
  - `undelete` is a storage-specific extra (B2: remove the hide markers in B2's web console), only used if the window
    is still open. It isn't part of the scripts.
- **Dry run by default.** The step prints the file count, total bytes and a sample of keys, and only acts with an
  explicit `--execute`. The dry run doesn't complete the step.
- Log every deleted path (with size and modification time) to `$SCRATCH_DIR/deleted.jsonl` for audit and rollback.
- Folders are virtual on B2: they disappear with their last file. Index files left in emptied folders are removed
  right after the data (2.15).
- **Jobs that crawl the data site** (`sync-fuzzwork-ordersets`, the market history scrape) may fail against stale
  parent index pages between the delete and the full `DataIndex` run; left as-is, they recover on their next
  scheduled run (2.17).

### 2.15 Stale index files after deletion

`DataIndex` regenerates the parent directories correctly (the deleted year vanishes from them), but it **leaves the
old `index.html`/`index.json` in every emptied directory**. So `13-delete` cleans them up, right after the data:

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

`16-check-site` therefore checks **many** URLs, not one: a random sample of deleted files spread across datasets and
months (e.g. 50), plus the `index.json` of emptied folders. Per URL:

- `404`: pass.
- `200` with `cf-cache-status: HIT`: pass, reported as "still cached". It will expire.
- Anything else (`200` from the origin, `5xx`, …): **fail**. The origin still serves the file, or something is broken.

### 2.17 Code and users that depend on the deleted data

Must be handled **before** deleting the affected datasets:

| Consumer | Problem | Mitigation |
|---|---|---|
| `SyncFuzzworkOrdersets` | Re-syncs every orderset missing from the data site, so it **would re-upload the deleted files**. Fuzzwork's listing goes back a long way (IDs 90019–174817 on 2026-10-08). | ID cutoff (2.18). |
| `SyncMer` | Rescans from `MER_FIRST_MONTH` if no MER files exist. | `ccp/` is excluded for now. Even archived, it couldn't happen: the latest MER is always too new to archive, and with any MER on the site it only checks from two months before the latest. |
| `DataCrawler` | Used to log and return nothing when an index page was missing; it now **fails** instead (done, #1151; revert if it causes problems). Between the delete and the full `DataIndex`, parent indexes still link to removed directories, so crawling jobs will fail. | Accepted: left to fail and recover on their next scheduled run after `DataIndex` catches up (2.14). |
| `ScrapeMarketHistory` / `import-market-history` | Re-read up to 450 days of `market-history/`, and the full history. | `market-history` is never archived. |
| `HistoricalOrdersRegionTypeSource` | Reads the last 30 days of market orders (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`). | Safe with the 2-year minimum age. |
| Docs examples | `downloading-datasets.md` and `structures.md` linked files that would be archived. | Done: `downloading-datasets.md`'s examples use `market-history/2025/` (never deleted); `structures.md` names its two 2024 files and links the roll-up holding them. |
| Users following `downloading-datasets.md` (`rclone sync`) | **`rclone sync` deletes their local copies** of files deleted on the site. | Done: the docs use `rclone copy`, and say why. |
| Users with `wget -N` / custom scripts | Get 404s. | Docs page. |

### 2.18 Sync cutoff for Fuzzwork

The sync commands must never fetch data the archival process removed. The change must be **deployed before the
first deletion** of `fuzzwork/` data (README, "Setup"). Tests first (repo convention).

**`SyncFuzzworkOrdersets`, decided (2026-10-14) and implemented: cutoff at the lowest ID on the data site, gaps above
it filled** (open question 15).

- It decides what to sync by **sequence ID**, comparing Fuzzwork's listing with the IDs found on the data site. The
  year of an orderset is only known after downloading it, so a date cutoff would mean re-downloading every archived
  orderset on every run.
- Skip any Fuzzwork ID **below the lowest ID currently on the data site**; sync any ID at or above it that isn't
  already present, gaps included. A download that fails mid-run (`CONCURRENCY`, 4 at a time) is retried on the next
  run instead of leaving a permanent gap; an ID the archival process already deleted is never retried, since it's
  below the lowest remaining ID.
- **Count only the dated layout** (`fuzzwork/ordersets/YYYY/…`), not `backfills/`. The backfills (IDs 21000–59999)
  have no year folder and may be archived before or after the dated ordersets (by upload date, 2.2), and Fuzzwork's
  listing starts at 90019 anyway. If the backfills counted while still on the site, the lowest ID would be 21000
  and the cutoff would never apply.
- **Guard:** if the data site has no dated ordersets at all, fail instead of syncing everything.
- Fuzzwork sequence IDs are incremental (confirmed), and ordersets are uploaded in ID order, so the ID cutoff follows
  the archive cutoff automatically: once older ordersets are deleted, the lowest remaining ID moves up.

**`SyncMer`: no change.** `ccp/` is excluded for now (2.2). If it's archived later, the latest MER never is, and
`SyncMer` only checks from two months before the latest one, so archived MER files wouldn't be fetched again. (An earlier draft had a
guard for "no MER files at all, so re-sync from `MER_FIRST_MONTH`", and before that an `ARCHIVE_MIN_AGE_YEARS`
setting; both dropped.)

### 2.19 Durability

Copies of each torrent's data after deletion:

1. **The seed directory (`SEED_DIR`)**, on redundant storage, checked regularly (README, "Requirements" and
   Appendix C, "Maintenance").
2. **Community seeders**, encouraged through the RSS feed and rclone-mirror seeding.

There is no backup (2.11): if the seed directory is lost, a torrent nobody else seeds is gone.

Any soft-delete window on the storage is not counted. The data can never be re-scraped.

### 2.20 Discoverability after deletion

Archived files disappear from the listings, so someone browsing the site won't see that the data exists as a
torrent.

- **The docs site** explains the archive rules and links the torrents (4.2). It's set up once, before the first
  deletion.

### 2.21 Storage access: rclone only

Revised 2026-10-10. The scripts reach the data bucket **only through rclone**, with **one B2 key** and one remote,
`everef`. **Never the AWS CLI or an S3 library.** Same setup as the roll-up scripts:

- rclone is configured **only from `RCLONE_CONFIG_*` environment variables**. `lib/workflow.py` sets
  `RCLONE_CONFIG=/dev/null`, so the operator's own `rclone.conf` and its remotes are out of the picture.
- Only the secrets come from `torrents.env`: `RCLONE_CONFIG_EVEREF_ACCOUNT` and `RCLONE_CONFIG_EVEREF_KEY`. The rest
  is fixed in `lib/workflow.py` and overrides the environment: type `b2`, and `hard_delete=false`, which must never be
  true: deletes stay soft, so the bucket's lifecycle rules provide the undo window (2.14).
- The bucket is a setting, `DATA_BUCKET` in `torrents.env` (like `ROLLUP_BUCKET` in the roll-up scripts). The site
  root is `everef:$DATA_BUCKET`.

One key means every step could write and delete. The safeguards are the steps themselves: only `09-publish` and
`13-delete` write to the data bucket, only to explicit paths (or `deep-archive/`), after showing what they'll do and
asking. (The 2026-10-09 design used a filesystem mount; the first draft of this revision used boto3, then separate
read, write and backup remotes. All dropped.)

- **No mount, no public-HTTP crawl** for decisions. HTTPS against data.everef.net is only used to check what the site
  serves (`09-publish`, `16-check-site`). The other remote API is qBittorrent.
- **Listings are always fresh.** An rclone listing goes straight to the storage, so `01-find`,
  `12-check-before-delete`, `13-delete` and `14-check-delete` see what EVE Ref's jobs wrote a moment ago.
- **The modification date is rclone's `ModTime`** everywhere: selection (2.2), load (file mtimes), pre-delete check
  (2.14). On B2 it's the upload time, unless the uploader set `src_last_modified_millis` (check on real files).
- **Content-Type and Cache-Control** go on the files at upload, like `S3Util` does today: `.torrent` →
  `application/x-bittorrent`, `.txt` → `text/plain; charset=utf-8`, `everef-eve-online-deep-archive.xml` → `application/rss+xml` with a short
  max-age. With rclone that's `--header-upload`. Whether B2 honours both through rclone is still unchecked (open
  question 1); `09-publish` checks the Content-Type it serves over HTTPS after uploading, and fails if it's wrong.
- The writing steps only ever touch paths from an explicit list (or `deep-archive/`).

**Throughput and cost.** Listing ~1 million files is ~1,000 B2 list requests (class C transactions, cents).
`--fast-list` keeps it to that. Loading up to 1 TiB per torrent goes through B2's egress; check its pricing (egress to
Cloudflare is free, direct egress has an allowance).

**Dependencies outside this process.** `DataIndex` and the sync jobs use the same storage through `S3Util`.

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
  torrents.env.example  # copy to torrents.env (not committed)
  00-preflight.py       # the workflow's steps: 00-preflight.py, 01-find.py, 03-load.py, 04-sign-sha256.sh, ...
  lib/                  # shared code: workflow.py, sign.sh, trackers.py, torrent_size.py, qbittorrent.py, and the
                        # templates: README-template.md (the README inside each torrent), comment-template.txt
  tests/                # unit tests (make test-deep-archive)
```

Keep the README and scripts in sync: any script change that changes output or behaviour updates `README.md` in the
same commit. This follows the repo's "docs never drift" convention; `AGENTS.md` describes the directory.

### 4.2 Docs site: new page `docs/src/datasets/deep-archive.md` ("EVE Ref Deep Archive")

Explains the rules, and **lists every torrent** in a "Torrents" table (ID, v1 and v2 info-hashes, magnet link),
newest first, added by step 10 of each workflow (4.3). The `deep-archive/` directory and the feed are the
authoritative list.

1. **What it is.** BitTorrent archives of data.everef.net datasets, `everef-eve-online-deep-archive-NN.torrent`
   (sequential IDs, `00`, `01`, ...). Each one holds the oldest data that wasn't in an earlier torrent, from every
   archived dataset, up to the cutoff date stated in its own README, in the same paths as on the site. Each torrent
   is as big as client limits on the `.torrent` file allow, up to about 1 TiB.
2. **Why they exist.** data.everef.net keeps growing (5.7 TB+). Old data is moved off the HTTP site to keep hosting
   sustainable, and torrents keep it available, with the community helping to host it. Be honest about the
   trade-off: old data is only available as long as someone seeds it, and seeding is appreciated.
3. **What gets archived, and what doesn't.**
   - Files stored on the site at least 2 years ago, oldest first.
   - Once a torrent is published, its files are **removed from data.everef.net**.
   - Never archived (always on HTTP): `market-history` and `killmails` (they work as a database and change in
     place), `ccp` (CCP's own reference data, for now), and `*-latest` files.
4. **When new torrents appear.** Ad hoc, whenever enough data has passed the 2-year age to fill a torrent. There is
   no fixed date. Until a file is archived, it stays on data.everef.net. Subscribe to the feed to be notified of new
   torrents.
5. **How to use them.**
   - Links: [`deep-archive/`](https://data.everef.net/deep-archive/) directory and the RSS feed `everef-eve-online-deep-archive.xml`.
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
6. **Integrity and signatures.** Torrent clients verify every piece (and, for v2 clients, every file's SHA-256 hash
   tree). On top of that, each `.torrent` has a minisign signature next to it, and each torrent holds a signed
   `sha256sum` checksum file (2.6). The page shows how to verify the `.torrent`; the README inside the torrent shows
   how to verify the files.

Related one-time changes, all live **before the first deletion**:
- `docs/src/datasets/downloading-datasets.md`: **done**, `rclone copy` instead of `rclone sync`, with why, a link to
  the Deep Archive page, and examples on `market-history/2025/` (never deleted).
- `docs/src/datasets/structures.md`: **done**, the two 2024 example files are named, and linked to the
  `structures-2024.tar.xz` roll-up they're now in.
- `docs/src/datasets/index.md`: **done**, a note that old data is archived, with a link.
- Docs sidebar/navigation entry for the new page: **done**.

### 4.3 Per workflow

Step 10 (update-docs, manual) adds the torrent to the page's "Torrents" table, and the docs site is
deployed. `12-check-before-delete` fails until the live page has the torrent's ID and v1 info-hash, so nothing is
deleted before the torrent is listed. Update the rule text too if the rules changed (excluded datasets, timing, size
limits).

---

## 5. Changes outside the scripts

- **This repo:**
  - Docs site (4.2): the Deep Archive page and related edits, before the first deletion.
  - Operator docs in `deep-archive/` (4.1), kept in sync with the scripts.
  - `SyncFuzzworkOrdersets` ID cutoff (2.18): done; must be deployed before the first deletion.
  - `DataCrawler` fails on a missing directory index (2.17): done.
  - Optional `DataIndex` change: stale index cleanup (2.15, follow-up).
  - `deep-archive/` scripts: done.
  - An `AGENTS.md` update describing the `deep-archive/` directory and process: done.
- **Cloudflare Worker (separate repo):**
  - Pass through the objects' Content-Type and Cache-Control (2.21).
  - Optionally make deleted archive paths return a 404 page that links to the Deep Archive page.
  - Make sure no bot protection blocks RSS readers or torrent clients fetching `.torrent` files.
- **B2 key (2.21):** one key for the data bucket, in `torrents.env` on the host that runs the scripts.
- **Backup:** none (2.11).

---

## 6. Open questions

Decided so far: one torrent per workflow, as big as the size limits allow (2.1, 2.8); hard-coded limits
(a `.torrent` estimate of at most 10 MB, `MAX_BYTES` 1 TiB, 32 MiB pieces); selection by date (the earliest of the
modification date and dates in the file name, 2.2) with a 2-year minimum age;
sequential torrent IDs (2.3); exclusions `market-history`, `killmails` and, for now, `ccp`; Fuzzwork in scope with a sync
cutoff (lowest ID, gaps filled: 2.18, question 15); a README, a checksum file and its signature inside the torrent,
and a signature of the `.torrent` next to it (2.6); hybrid v1+v2; no
notice period and no announcements; deletion last; no MD5 matching; no Cloudflare
purge; `DataCrawler` fails on missing indexes; a manual process with numbered scripts and completion files in
`deep-archive/`; the data bucket is reached only through rclone with one B2 key, soft deletes only (2.21); no
backup (2.11); a date too busy to fit one torrent aborts `01-find` and is resolved by hand; so does a published path
still on the site (question 17).

Still open:

1. **Is rclone's `ModTime` on B2 the upload time** for files uploaded by EVE Ref's Java code (2.2)? And does B2
   honour `Content-Type` and `Cache-Control` set through rclone's `--header-upload` (2.21)? Check on real files.
2. ~~**Backup destination** (2.11)~~ **Decided (2026-10-11): real AWS S3, storage class DEEP_ARCHIVE, with its own
   AWS credentials** (not a second B2 key, not rclone). Revised 2026-10-12: the AWS CLI (a bash `backup.sh` step) with a
   dedicated key (`BACKUP_AWS_*`), not boto3. **Revised again 2026-10-12: no backup at all** (2.11, restore egress
   cost).
3. ~~Bundle format: ZIP64 store-only or tar?~~ Moot: the backup it was for was removed (2.11).
4. ~~Internet Archive: use it at all?~~ **Decided (2026-10-24): no.** BitTorrent plus the seeder host is the
   distribution plan; IA would add open redistribution-rights questions for CCP and Fuzzwork data, and item
   grouping, for unclear benefit.
5. ~~Seeder host: where is it, how much disk and upload, and does it have a redundant filesystem?~~ **Dropped
   (2026-10-25):** not something this plan needs to settle; the README's Requirements say what it needs.
6. ~~How to test reachability from outside?~~ **Decided (2026-10-25): no outside test** (Review notes, R16).
7. ~~Stale index cleanup: in the delete step (2.15a), in `DataIndex` (2.15b), or both?~~ **Decided (2026-10-24):
   the delete step** deletes the data files, then the folders that leaves empty (on B2: the `index.html`/`index.json`
   of folders with nothing else in them, 2.15). The full `DataIndex` (step 15) does the final indexing.
8. ~~Add a `DataIndex` "Archived data" note to index pages?~~ **Dropped (2026-10-25):** the docs page is enough.
9. ~~**Small-snapshot datasets** (Appendix B): ~553,700 files (69 %) for ~6 GiB.~~ **Done (2026-10-24):** the
   roll-up rolled them into a few large files and deleted the originals, so they no longer fill the torrents. Still
   open: which "no year folder" directories must stay on HTTP (`characters-corporations-alliances/backfills`, …)?
   (`ccp/` is excluded for now, 2.2.)
10. ~~Which Discord channel gets the announcements, and who posts them?~~ **Dropped (2026-10-12): no announcements**
    (2.13).
11. ~~Trackers: add all from both lists, a curated set, or the core only?~~ **Decided (2026-10-12): the union of
    both "best" lists** (2.4 "Trackers"). **No tracker refresh (decided 2026-10-25):** published `.torrent` files are
    never republished.
12. ~~Torrent creation tool: Python `libtorrent` or the qBittorrent Web API creator (2.4 "Creating the torrent")?~~
    **Decided (2026-10-24): Python `libtorrent`** (already what `05-make-torrent.py` uses).
13. ~~Is deleting still worth it on cheaper storage?~~ **Decided (2026-10-24): yes, keep deleting** — the saving
    still justifies the seeder-host cost and operator time.
14. ~~Does the new storage have soft delete, snapshots or versioning?~~ **Moot (2026-10-25):** the data stays on B2,
    with a lower storage need once old data is in torrents. B2's soft delete (2.14) is the only extra safety net.
15. ~~Fuzzwork sync: lowest ID with gap filling (2.18 as decided) or latest ID without?~~ **Decided (2026-10-14):
    lowest ID, gaps filled.** A brief attempt at latest-ID-only (no gap filling) risked a permanent gap: it
    downloads 4 ordersets at a time (`CONCURRENCY`), so a failure on one while a later one uploads would mean the
    next run starts after the later one and never retries the failed one. The lowest-ID rule already follows the
    archive (deletes take the oldest files, so the lowest remaining ID moves up and anything below it is never
    retried), and fills gaps above it, so a failed download is simply retried on the next run.
16. **4-digit IDs read as years (2.2).** Any 4-digit number from 2003 on in a file name dates the file 31 December of
    that "year", possibly much too early. Fuzzwork IDs are clear (backfills 21000–59999, live from 90019); other
    datasets aren't checked. The first real run's `find.log` ("dated by a year in the name") answers it; if IDs show
    up, narrow the rule (e.g. only years next to a separator and no other digits, or per dataset).
17. ~~**Re-selection after a failed delete (2.2).**~~ **Decided (2026-10-25): no re-selection.** A non-index path in
    a published `<ID>.txt` that's still on the site, for any reason (a failed delete, a file that changed before its
    delete, Appendix A), is a hard error in `01-find`. Addressed if it ever actually happens (e.g. by recording each
    file's modification time in `<ID>.txt` so a new version can be told apart).

---

## 7. Review notes (2026-10-12)

Notes and warnings found while reworking the scripts, for later review. None blocks a first test run; each says
what to decide. Numbered so other sections can point at them; R3, R4 and R8–R11 were about the Glacier backup
step and were dropped with it (2.11), and R14 was about an experiment script that's gone.

**Safety**

- **R1. One step at a time. Done.** Two steps started by mistake (e.g. two terminals) could both pass the
  completion-file checks. Every step takes `$SCRATCH_DIR/log/.lock` with `flock`, like the roll-up scripts:
  `Workflow.begin` for the Python steps (`13-delete`'s dry run included), `00-preflight`, and `lib/sign.sh` for the
  bash ones.
- **R2. The environment wins over `torrents.env`. Done.** `load_env` only fills in what the environment doesn't set,
  so a stale shell export (`DATA_BUCKET`, `SEED_DIR`, `RCLONE_CONFIG_EVEREF_*`) overrides the file. Every step names
  each setting the environment overrides, right after its first line (names only, no values: some are secrets);
  `lib/sign.sh` does the same for its two settings.

**Integrity**

- **R5. A SHA-256 of every archived file survives the workflow.** `manifest.json` is deleted with `$SCRATCH_DIR`, but
  `03-load` writes the same hashes to `<ID>.sha256`, which is signed (`04-sign-sha256`) and ships inside the torrent
  (2.6). The `.torrent` itself is signed too (`06-sign-torrent`), and that signature is published next to it.
- **R6. Three steps check themselves.** `03-load`, `05-make-torrent` and `09-publish` now check their own results at
  the end, like the roll-up scripts, instead of having a separate read-only check step (2.10). They re-read from disk
  or the site, but a bug shared by the action and its check (e.g. the same wrong comment template, the same path
  handling) is no longer caught: the old `check-torrent` re-rendered the comment from its own copy of the template,
  the merged check compares against the same rendering.
- **R7. Nothing re-reads the data between load and the torrent. Cheap guard done (2026-10-25).** `03-load` hashes
  every file into the manifest; `05-make-torrent` reads every file again to compute piece hashes; `08-check-seed`'s
  recheck then compares the disk with the torrent. The manifest's hashes and the torrent's data files are never
  compared directly (`03-load`'s `sha256sum -c` runs right after hashing, and the test download fetches only the
  README). `05-make-torrent` now checks every file's size and modification time against the manifest before hashing
  (`content_problems`), which catches a file replaced or rewritten in between; silent corruption that keeps both
  isn't caught, a low risk on redundant storage (ZFS/RAID). A full guard would be one more read of all the data.

**Housekeeping**

- **R12. Barely run since the rework.** The shared helpers, `01-find`'s selection logic, the `.torrent` estimate,
  the feed and the delete-set checks have unit tests (`tests/`, `make test-deep-archive`). No step has run against
  rclone, libtorrent, qBittorrent, aria2c or minisign. Run `00-preflight` and every step on a small test selection
  before real data.
- **R13. Completion-file names changed.** Step numbers changed with every merge on 2026-10-12, and again on
  2026-10-24 and 2026-10-25 (now 00-16, with the two signing steps); a `SCRATCH_DIR` from before won't resume. (None
  has run on real data.) A unit test checks `STEPS` against the script names.
- **R15. Documentation size. Done (2026-10-25).** The README repeated much of this plan; it's now built around one
  step table, with a few lines and the operator's checks per step. Detail lives in each script's docstring, design
  here.

**Seed check (`08-check-seed`, merged 2026-10-12)**

- **R16. Not an outside test any more.** The manual "check the trackers from outside" step (torrenteditor.com) and
  the separate test download are gone. The aria2c download runs on the workflow host, so it proves the torrent can be
  downloaded, not that the seeder is reachable from the internet. If qBittorrent runs on the same host, aria2c may
  only reach it through the host's own public address (NAT hairpinning), which some routers don't allow: the step
  would then fail with no download progress even though outside peers are fine. Options if that happens: enable
  local peer discovery on both sides, or check from another machine by hand. **Accepted (2026-10-25):** if aria2c
  downloads the README, the torrent is seeded, and at first EVE Ref is the only seeder, so the seed works; no outside
  test is added.
- **R17. aria2c is v1 only, and the README is all it fetches.** aria2c doesn't support BitTorrent v2, so it tests the
  hybrid torrent's v1 swarm; v2-only clients and magnet-link metadata (what most users start from, via the feed) go
  untested. The README's file index comes from `aria2c --show-files`, where pad files count; this, and aria2c's exit
  code on `--bt-stop-timeout`, are untested (no aria2c where it was written). No data file is downloaded, so a
  problem specific to data files (e.g. paths with unusual characters) wouldn't show.
- **R18. Rerunning while the trackers warm up.** The step fails until a tracker reports as working; that's expected
  for a few minutes after `07-seed`, and rerunning is fine (no completion file is written on failure).

**Publish (`09-publish`, merged 2026-10-12)**

- **R19. The feed backup is deleted with the scratch directory.** `09-publish` saves the published `everef-eve-online-deep-archive.xml` to
  `$SCRATCH_DIR/feed-backup/feed-<UTC time>.xml`, which goes when `$SCRATCH_DIR` is deleted at the end of the
  workflow. Enough to undo a bad rebuild during the workflow; for a longer history, keep it somewhere that persists
  outside `$SCRATCH_DIR`. `everef-eve-online-deep-archive.xml` can always be rebuilt from the published `.torrent` files.
- **R20. Two uploads, one step.** It uploads the `.torrent`/`.txt`, then replaces the feed, without asking (the
  diff is printed, and the HTTPS checks verify the result). If it fails between the two, rerunning skips the already-published, identical `.torrent`/`.txt`; a
  different one stops it (never overwritten).
- **R21. Checks retry for the Cloudflare cache.** The HTTPS checks retry for up to `FEED_CACHE_MAX_AGE_SECONDS` + 60
  seconds, because Cloudflare may serve the old `everef-eve-online-deep-archive.xml` (or a cached 404 for the new `.torrent`) right after the
  upload. A real problem therefore takes a few minutes to report.

**Docs (removed 2026-10-12)**

- **R22. Nothing checks the docs page against the scripts any more.** `check-docs` followed every link on the Deep
  Archive page and printed `MIN_AGE_YEARS`, `TORRENT_EXCLUDE` and the size limits to compare with the page. Now
  `12-check-before-delete` checks the page is reachable and lists the torrent's ID and v1 info-hash (2026-10-24), but
  not the rules: if a rule changes in `lib/workflow.py` (e.g. a dataset added to `TORRENT_EXCLUDE`) the page has to be
  updated by hand, before the next delete; a broken link on it goes unnoticed.

**Backup (removed 2026-10-12)**

- **R23. One copy.** With the Glacier backup gone (2.11), deleted data exists only in `$SEED_DIR` and with whoever
  else seeds it. Early on, most torrents will have no other seeders, so a lost or corrupted seed host loses that data
  for good (B2's soft-delete window aside). The egress cost that ruled Glacier out is only paid in a disaster (about
  $500 for everything); the risk it covered is paid in data. **Accepted (2026-10-25):** one copy, on redundant
  storage, plus whoever else seeds; no second copy. Appendix C's monthly recheck matters more for it; consider
  alerting on it.

## Appendix A: If archived data turns out to have changed

If the pre-delete check (`12-check-before-delete`) finds that a file changed on the storage after it was selected:
- **Don't delete that file.** It isn't the version in the torrent. Everything else that's confirmed is deleted as
  usual (2.14).
- The torrent stays as it is: it holds the version from when it was made. No replacement torrent.
- The changed file stays on the site at a path the published `<ID>.txt` lists, so the next `01-find` stops with a
  hard error on it (2.2), as for any file the pre-delete check leaves on the site. **Not handled yet (2026-10-25):
  addressed if it ever actually happens.** Until then, resolve it by hand before running `01-find` again.
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
  entry per file and will be larger: measured below.
- Already **over libtorrent's 10 MB `.torrent` default** (2.4), even as v1. Just under the ~31.5 MB magnet metadata
  limit, but a hybrid version almost certainly won't be.
- The file count is about the size of one large year (2023: 264,988; 2024: 282,463), so it backs up the conclusion
  below: a large year doesn't fit libtorrent's defaults as one torrent.

### Measured: libtorrent 2.x, 13 files (2026-10-08)

Small hybrid v1+v2 test with a throwaway libtorrent 2.x script (not kept; 16 MiB pieces):

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
  - For the real archive, copying terabytes into the incomplete directory isn't practical. `07-seed` instead adds
    the torrent with its save path set to `$SEED_DIR`, so qBittorrent checks the files where they are.
    **Still to test.**

### Measured: libtorrent 2.x, 43,620 files (2026-10-08)

Hybrid v1+v2 with the same script (16 MiB pieces) over a directory of small files:

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
first run is most of the 5.7 TB. The seeder needs at least 4 TiB free for the content.

**Torrent size is the deciding problem.** Using the ratio from the `find` estimator (≈ 300 bytes and ≈ 27 bdecode
tokens per file for a hybrid torrent), everything in one torrent would be ~240 MB and ~22 M tokens, over every limit
in 2.4, and large single years (120,000–280,000 files) would be 36–85 MB each. Staying under libtorrent's 10 MB
default means roughly 33,000 files per torrent at most, before any margin.

**Outcome (2026-10-10):** size-limited torrents (2.8). Each torrent takes the oldest data up to the size limits,
which fit every default limit with margin. (Then a fixed 20,986 files and 1 TiB; now a `.torrent` estimate of at
most 10 MB, hard, and 1 TiB, soft.) Options considered and not taken: aiming
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

**Superseded (2026-10-24):** the roll-up rolled the small-snapshot datasets into a few large files and deleted the
originals instead (open question 9), so they're neither excluded nor filling the torrents. The recommendation, as it
was: add the small-snapshot datasets to `TORRENT_EXCLUDE`. They stay on HTTP; the archive drops from
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

**Decided: all of `ccp/` is excluded for now** (2.2), including `ccp/mer` and the dated `ccp/sde` folders, not just
the reference collections above: ~62 GiB in all, linked from docs and likely downloaded by URL. The `ccp/*` rows above
aren't selected. To revisit later, together with the docs that link them.

**Follow-ups for `01-find`:**

- An "average file size" column per dataset, to make small-snapshot datasets obvious.
