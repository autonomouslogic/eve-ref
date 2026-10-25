# EVE Ref Deep Archive: Notes

Reference for working on the scripts: the constraints behind the design, and facts that aren't obvious from the code.
How to run a workflow: [`README.md`](README.md). What each script does: its own docstring. Sections are referred to
by name from the code (e.g. `NOTES.md "Deletion"`).

## Context

- data.everef.net is a **Backblaze B2** bucket behind a **Cloudflare Worker** (separate repo). The site root is the
  bucket root. The Worker must pass the objects' Content-Type and Cache-Control through, and must not block RSS
  readers or torrent clients fetching `.torrent` files.
- The Java code writes to the bucket through B2's S3 API (`S3Util`, `inject/S3Module`, `Configs.DATA_*`), setting
  Content-Type and Cache-Control as object metadata. The Deep Archive scripts do the same through rclone ("Storage
  access").
- `DataIndex` (`cli/DataIndex.java`) writes `index.html` + `index.json` into every folder, optionally under one
  prefix (`DATA_INDEX_PREFIX`). It skips existing index files when listing, so a folder holding only index files
  drops out of its parent's listing. **It never deletes stale index files** ("Stale index files").
- `DataCrawler` (`http/DataCrawler.java`) walks the site's `index.html` pages over HTTP, for `SyncFuzzworkOrdersets`
  and the market history scrape. It fails on a missing index page ("Code that depends on deleted data").
- File layout comes from `util/archive/ArchivePathFactories.java`. Most datasets have a year folder
  (`market-orders/history/YYYY/YYYY-MM-DD/…`); some don't (`fuzzwork/ordersets/backfills/…`, `<dataset>/history/…`).

## Selection

`01-find` lists the whole bucket (`rclone lsjson --recursive --fast-list`: one B2 request per 1,000 files, minutes for
the site) and picks the selection, `find/files.jsonl`. Every later step is checked against it, never against
"whatever is in the folder".

### Dating

Files at least `MIN_AGE_YEARS` (2) old are archived. A file's date is the **earliest** of:

- rclone's `ModTime`, in UTC: on B2, the upload time unless the uploader set `src_last_modified_millis` (the Java
  uploads go through the S3 API, which doesn't);
- any ISO date (`YYYY-MM-DD`) in the file name;
- 31 December of any 4-digit year from 2003 (EVE's release) in the file name.

Only the file name counts, not the folders. The name's date puts backfills and re-uploads with their period. A
4-digit ID in a name would read as a year and date the file too early: the report counts files by how they were
dated, and `find.log` lists samples dated by a year in the name. Fuzzwork IDs are safe (backfills 21000–59999, live
from 90019); other datasets aren't checked.

The 2-year age is well clear of the market history scraper's 30-day snapshot lookback
(`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`, read by `HistoricalOrdersRegionTypeSource`).

### Never archived

`TORRENT_EXCLUDE` in `lib/workflow.py` is a denylist, so new datasets are archived automatically once old enough:

- `market-history/` and `killmails/` work as databases: past files are modified in place, which a torrent can't
  follow. `ScrapeMarketHistory` also re-reads the last 450 days (`ESI_MARKET_HISTORY_LOOKBACK`) and
  `import-market-history` needs the full history over HTTP.
- `ccp/` (SDE, MER, image export collections, CSM, QENs; ~62 GiB): the docs link its old files and folders directly
  (`sde.md` links `ccp/sde/older/`, the datasets index links `ccp/ccp_quant/`, `ccp/iec/`, `ccp/csm/`). Archiving it
  needs those docs changed first. The syncs wouldn't re-upload it: `SyncMer` only checks from two months before its
  latest MER, `SyncStaticData` only the latest SDE.
- `deep-archive/`: the published torrents.

Also never selected: `*-latest.*`, `index.html`, hidden files, files at the site root.

Selecting by date can catch a file that's still current but hasn't changed in two years (a reference file a tool
fetches by a fixed URL). That's what step 02's review of "(no year folder)" datasets is for.

### `index.json`

A folder's `index.json` is selected whenever any file directly in it is, and never on its own date: `DataIndex`
rewrites it on every run, so it would never reach the minimum age. Parent folders' aren't added.

- It's a data file in the torrent: in the `.txt`, counted in the size estimate and `MAX_BYTES`.
- It's never in the delete set (its modification time never matches the selection's). The index cleanup removes it
  from folders the delete empties ("Stale index files").
- A folder split across torrents has its `index.json` in each, so the same path legitimately recurs across published
  `.txt` files. The report flags selected `index.json` files whose folder keeps files on the site, and selected
  folders with no `index.json`.
- Mirror seeders may hold a newer version; their client re-downloads those few small files on recheck.

### No path in two torrents

`01-find` downloads every published `<ID>.txt`. A listed non-index path still on the site is a hard error, whatever
its date: it means a delete didn't happen, the path reappeared, or `12-check-before-delete` left it there (README
Appendix E). `01-find` never guesses whether such a file changed, and never reads an earlier torrent's cutoff back: its
cutoff is picked fresh from this run's candidates, so nothing relies on cutoffs being in order (`TORRENT_INCLUDE` can
put older files in a later torrent). Telling a new version of a listed path apart would need its modification time
in the `.txt`.

### Cutoff and limits

Candidates (`index.json` aside) are grouped by date and added oldest first, each with their folders' `index.json`. A
date is never split.

| Limit | Value | Kind |
|---|---|---|
| `MAX_TORRENT_BYTES` | 10,000,000 | Hard: the first date that takes the `.torrent` estimate over it is left out, and the cutoff is the day before. If that's the oldest date, `01-find` fails. |
| `MAX_BYTES` | 1 TiB | Soft: the first date that reaches it is the cutoff, included whole. |
| `MIN_BYTES` | 500 GiB | Below it, `01-find` fails: not enough data yet. |
| `PIECE_SIZE` | 32 MiB | 32,768 pieces for 1 TiB. |

If neither maximum is reached, the cutoff is the newest allowed date and the report says the torrent could be bigger.
The file count, not the bytes, is what limits a torrent ("Torrent size").

## Torrent size

`MAX_TORRENT_BYTES` is libtorrent's default `max_buffer_size`. libtorrent's defaults also cap a `.torrent` at
3 million bdecode tokens (`max_decode_tokens`), magnet metadata at about 31 MB (`max_metadata_size`) and 2,097,152
pieces. qBittorrent raises some of these in its advanced settings; other clients may not, so the defaults are the
target.

`lib/torrent_size.py` estimates the hybrid `.torrent` from paths and sizes, mirroring libtorrent's layout (v1 entries
and pad files, v1 piece hashes, v2 file tree and piece layers), plus `FIXED_BYTES` (16 KiB) for trackers, comment and
root files. The per-file part matched libtorrent byte for byte in tests; the allowance makes it up to about 12 KB high.

Measured (2026-10-08, 16 MiB pieces):

- Hybrid costs about **261 bytes per small file**, v1 about 95: every file gets a pad file and a v2 file-tree entry,
  and its own piece. For small files the piece size doesn't change the `.torrent` size.
- 43,620 small files made an 11.4 MB hybrid `.torrent` that libtorrent's defaults refused and **qBittorrent's WebUI
  hung on**. Roughly 38,000 small files fit under 10 MB.
- qBittorrent finds and checks existing files when a torrent is added with its save path pointing at them, and seeds
  without downloading (what `07-seed` relies on).

Small files are the problem, not bytes. The small-snapshot datasets (`incursions`, `faction-warfare-*`, the hourly
`sovereignty-*`/`system-*`/`industry-*` sets, …) were most of the file count for a fraction of a percent of the bytes,
until the roll-up merged them into a few large files.

## Torrent format

- **Hybrid v1+v2**: v1 for client compatibility, v2 for per-file SHA-256. Every file is aligned to a piece boundary.
- Made with Python **libtorrent 2.x** (same library as qBittorrent). Not `mktorrent`: v1 only.
- Files sorted by path, so the same files give the same info-hash. Creation date kept (the feed's `pubDate`).
- Not private, DHT/PEX on, no `source` field, `created by` EVE Ref.
- **No web seed**: the files are deleted from data.everef.net and a torrent can't change, so it would be a dead URL.
- **Comment**: one line from `lib/comment-template.txt`, `{sequence}` filled in, pointing at the docs page.
  `load_comment_template` fails on more than one line. The comment is outside the info dictionary, and magnet-link
  users don't get it, so the README inside the torrent carries the same link.
- A published `.torrent` is **never republished**, so its trackers and comment are fixed once published.

### Trackers

`lib/trackers.py` downloads ngosang's `trackers_best.txt` and XIU2's `best.txt` when the torrent is made, keeps UDP
and HTTP(S), and adds their union (ngosang's first), **one tracker per tier**. Fails under `MIN_TRACKERS` (10). The
lists are saved in `$SCRATCH_DIR/trackers/`. Magnet links in the feed carry the first 10 (`MAGNET_TRACKERS`).

- Clients follow BEP 12: shuffle a tier, use the first tracker that answers, only move on if the tier fails.
  qBittorrent announces to all tiers by default, so one per tier reaches every tracker.
- Trackers sit outside the info dictionary: not part of the info-hash, and clients merge tracker lists when the same
  torrent is added again.
- The lists decay fast: of ngosang's best 20 from 1, 2 and 3 years earlier, only 7–9 were still listed. The same 7
  survived every time (`opentrackr.org:1337`, `open.stealth.si:80`, `tracker.torrent.eu.org:451`,
  `open.demonii.com:1337`, `exodus.desync.com:6969`, `explodie.org:6969`, `tracker-udp.gbitt.info:80`). DHT and PEX
  carry the swarm once trackers die.
- Both lists include piracy-branded domains, and XIU2's has `bt1/bt2.archive.org` and a WebSocket tracker.

## Torrent IDs and layout

`everef-eve-online-deep-archive-NN`, a two-digit sequence number from `00`. `00-preflight` picks one past the highest
published `.torrent` in `deep-archive/`, and fails at 100 rather than widening the format. The cutoff date is
recorded only in the torrent's own README.

The torrent's `name` is the ID; file paths inside it are the **full site paths**, with the root files next to them:

```
everef-eve-online-deep-archive-00/
  everef-eve-online-deep-archive-00-README.md
  everef-eve-online-deep-archive-00.sha256
  everef-eve-online-deep-archive-00.sha256.minisig
  market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-00-00.v3.csv.bz2
  …
```

With qBittorrent's "Don't create subfolder", the root folder is dropped and every torrent merges into the
data.everef.net layout, the same as an rclone mirror. That's why root file names start with the ID (they'd collide
otherwise), and why a mirror can seed straight away: add the torrent with that layout, point it at the mirror, recheck.

## Published files

Everything public lives in the bucket's `deep-archive/` (not this repo folder):

```
deep-archive/
  everef-eve-online-deep-archive.xml              # RSS feed, all torrents
  everef-eve-online-deep-archive-NN.torrent
  everef-eve-online-deep-archive-NN.txt           # file list
  everef-eve-online-deep-archive-NN.torrent.minisig
```

The bucket's `deep-archive/` is the single source of truth: the feed and `01-find`'s "already archived" both come
from it, never from local state. `09-publish` never overwrites.

### `<ID>.txt`

UTF-8, `\n` line endings with a final one, one data file per line as its full site path (no root folder), sorted by
code point, no duplicates, no root files. Generated from the `.torrent` itself. Checked by `file_list_problems`
(`lib/workflow.py`) in `05-make-torrent`, and on every published list in `01-find`: a list in the wrong format (e.g.
with the root folder in its paths) would match no site path, and its files could land in a second torrent unnoticed.
Content-Type `text/plain; charset=utf-8`.

### Feed

RSS 2.0, rebuilt in full by `09-publish` from every published `.torrent`, newest first. Compatible with qBittorrent's
RSS downloader and FlexGet/autodl-style tools:

```xml
<item>
  <title>everef-eve-online-deep-archive-00</title>
  <link>https://data.everef.net/deep-archive/everef-eve-online-deep-archive-00.torrent</link>
  <guid isPermaLink="false">INFOHASH_V1</guid>
  <pubDate>…the torrent's creation date…</pubDate>
  <enclosure url="…torrent URL…" length="TORRENT_FILE_SIZE" type="application/x-bittorrent"/>
  <torrent:infoHash>INFOHASH_V1</torrent:infoHash>
  <torrent:magnetURI><![CDATA[magnet:?xt=urn:btih:…]]></torrent:magnetURI>
  <torrent:contentLength>TOTAL_DATA_BYTES</torrent:contentLength>
</item>
```

`guid` is the v1 info-hash, so readers never pick a torrent up twice. Content-Type `application/rss+xml`, Cache-Control
max-age `FEED_CACHE_MAX_AGE_SECONDS` (2 minutes, like `DATA_INDEX_CACHE_CONTROL_MAX_AGE`).

## Inside the torrent

- `<ID>.sha256`: `sha256sum` format, from `03-load`'s `manifest.json`, checked with `sha256sum -c` (a second,
  independent read). It's the only record of every file's SHA-256 that outlives `$SCRATCH_DIR`.
- `<ID>.sha256.minisig`: covers files taken out of an already-trusted torrent, or found elsewhere (a mirror).
- `<ID>-README.md`, from `lib/README-template.md`: what the torrent is and its cutoff date, links, how to verify the
  files (minisign, or minisign through Docker), and the licence notes from `LICENSE-CCP`.

`<ID>.torrent.minisig` is published next to the `.torrent`, so the `.torrent` can be checked before there are piece
hashes to rely on.

The public key is `ui/public/.well-known/minisign.pub`, served at `https://everef.net/.well-known/minisign.pub`: on
the main site, not the data bucket, so whoever can write to the bucket can't replace it. Signatures are verified again
before each use, and a step that remakes a file removes its old signature (`03-load`, `05-make-torrent`), so a
signature of an older file is never shipped.

## Deletion

`12-check-before-delete` builds the delete set from the **published torrent**, not the selection. A file is in it
only if:

- it's a data file in `deep-archive/<ID>.torrent` (read back from the bucket, same info-hash as the local one), with
  the size the torrent gives it;
- it's in the manifest, so its SHA-256 came from the copy the seeder rechecked at 100%;
- it's on the bucket now, in a fresh listing, with the torrent's size and the selection's size and modification time.
  On B2 a new upload always gets a new modification time. No MD5/ETag matching anywhere: size and time detect changes,
  SHA-256 covers integrity from load onwards;
- it isn't an `index.json` or a root file.

A file that fails stays on the site and the rest goes ahead (README Appendix E). Files in the same folders that aren't
in the torrent (uploaded after the cutoff) stay. `13-delete` re-checks against another fresh listing, then deletes
with `rclone delete --files-from-raw`: an explicit list, never a prefix or pattern. Every deleted path is logged to
`deleted.jsonl`.

**Soft delete, treated as permanent.** The `everef` remote has `hard_delete=false`, so B2 hides files; the bucket's
lifecycle rule purges hidden versions after about 2 days. Too short to rely on: rollback is a re-upload from
`$SEED_DIR` (README Appendix D).

### Stale index files

Folders are virtual on B2, but `DataIndex` leaves the old `index.html`/`index.json` in folders it no longer lists, still
linking the deleted files. So `13-delete`, right after the data, lists the bucket again and walks every folder that
held a deleted file and its parents, deepest first: a folder holding nothing but `index.html`/`index.json` (at any
depth) loses them, by explicit key. Never the site root, never `deep-archive/`. The full `DataIndex` then rewrites the
parents. A `DataIndex` that cleaned these up itself would stop stale indexes from other deletes too; it must never
delete the root index.

### Cloudflare cache

No purge. Archive files have a 30-day max-age (`DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE`), index pages 2 minutes. So
`16-check-site` samples many URLs (deleted files across datasets and months, plus emptied folders' `index.json`):
`404` passes, `200` with `cf-cache-status: HIT` passes as "still cached", anything else fails (the origin still serves
it, or something's broken).

## Code that depends on deleted data

| Consumer | Problem | Handled by |
|---|---|---|
| `SyncFuzzworkOrdersets` | Syncs every orderset missing from the site, so would re-upload deleted ones. | ID cutoff ("Fuzzwork sync cutoff"). |
| `DataCrawler` | Between the delete and the full `DataIndex`, parent indexes link removed folders; it fails on the missing index. | Accepted: crawling jobs (`sync-fuzzwork-ordersets`, market history scrape) fail and recover on their next run after `DataIndex`. |
| `SyncMer` | Rescans from `MER_FIRST_MONTH` if no MER files exist. | `ccp/` excluded. Even archived, the latest MER is always too new, and it only checks from two months before it. |
| `ScrapeMarketHistory`, `import-market-history` | Read up to 450 days, or all, of `market-history/`. | Never archived. |
| Users running `rclone sync` | Deletes their local copies of files deleted on the site. | The docs use `rclone copy`, and say why. |

### Fuzzwork sync cutoff

`SyncFuzzworkOrdersets` skips any Fuzzwork ID **below the lowest ID on the data site** and syncs any missing ID at or
above it, gaps included. Must be deployed before the first `fuzzwork/` delete.

- By ID, not date: an orderset's date is only known after downloading it.
- Gaps are filled because downloads run `CONCURRENCY` (4) at a time: with a "latest ID" rule, one failing while a later
  one succeeds would leave a permanent gap.
- Only the dated layout (`fuzzwork/ordersets/YYYY/…`) counts, not `backfills/` (IDs 21000–59999, no year folder,
  archived by upload date). Counting them would pin the floor at 21000 while they're on the site.
- Fails if the site has no dated ordersets at all, rather than syncing everything.
- Fuzzwork IDs are incremental and uploaded in order, so the floor follows the archive as old ordersets are deleted.

## Storage access

The scripts reach the bucket **only through rclone**, one remote (`everef`) and one B2 key. Never the AWS CLI or an S3
library, so other storage would only mean another rclone remote.

- `lib/workflow.py` sets `RCLONE_CONFIG=/dev/null`: the remote comes only from `RCLONE_CONFIG_*` variables, never the
  operator's `rclone.conf`. Type `b2` and `hard_delete=false` are fixed and override the environment; only the account
  and key come from `torrents.env`.
- Listings go straight to the storage, so they're always fresh.
- Uploads set Content-Type and Cache-Control with `--header-upload`; `09-publish` checks the Content-Type served over
  HTTPS.
- One key means any step could write. Only `09-publish` (to `deep-archive/`) and `13-delete` (an explicit list) do.
- HTTPS against data.everef.net is only used to check what the site serves (`09-publish`, `16-check-site`).
- Cost: listing ~1 million files is ~1,000 class C transactions. Loading a torrent is up to 1 TiB of B2 egress (free to
  Cloudflare, an allowance direct).

## Durability

After deletion, the copies are `$SEED_DIR` (on redundant storage, rechecked monthly: README Appendix C) and community
seeders. There is **no backup**; the data can't be re-scraped. Ruled out:

- S3 Glacier Deep Archive: storage is about $1/TB/month, but a restore costs about $90/TB in egress plus retrieval
  fees, 12–48 hours, and restores whole objects.
- Internet Archive: items are capped at about 1 TB, recommended at most 500 GB and 1,000 files, so a torrent doesn't
  fit one item, and it can't act as a web seed (BEP 19 needs every file under one URL). Redistribution rights for CCP
  and Fuzzwork data would need checking.

A second copy, if ever wanted: another seeder, an offline disk off site, or B2 itself.

## Script principles

- One script per step, numbered as in `STEPS` (`lib/workflow.py`); a unit test checks script names against it. Manual
  steps have a number but no script (`MANUAL_STEPS`). Scripts never call the next step. No directory or torrent ID
  arguments: settings come from `torrents.env`, everything else is fixed in `lib/workflow.py`.
- `load_env` only fills in what the environment doesn't set, so every step names the settings the environment
  overrides (names only: some are secrets).
- Completion files (JSON: step, torrent ID, time, summary) gate the order. The torrent ID comes from `01-find.done`.
- Check steps are read-only and re-derive everything from the bucket, disk, qBittorrent or site. `03-load`,
  `04`/`06` signing, `05-make-torrent` and `09-publish` check their own results instead, re-reading from disk or the
  site; a bug shared by an action and its own check isn't caught.
- Output and logging follow the roll-up scripts.
- Any script change that changes behaviour or output updates `README.md` in the same commit.

## Known limitations

- **Unchecked on real files:** that rclone's `ModTime` on B2 is the upload time for the Java uploads, and that B2 honours
  Content-Type and Cache-Control set through `--header-upload`.
- **Between load and the torrent**, `05-make-torrent` only checks each file's size and modification time against the
  manifest (`content_problems`). Silent corruption that keeps both isn't caught until `08-check-seed`'s recheck; a full
  guard would be another read of all the data.
- **`08-check-seed` isn't an outside test.** aria2c runs on the workflow host. If qBittorrent is on the same host,
  aria2c may only reach it through the public address (NAT hairpinning), which some routers don't allow: no download
  progress, though outside peers are fine. Fix with local peer discovery, or check from another machine.
- **aria2c is v1 only** and fetches only the README: v2-only clients, magnet metadata and data files with unusual
  paths go untested. The README's file index comes from `aria2c --show-files`, where pad files count.
- **The docs page's rules aren't checked.** `12-check-before-delete` checks the page lists the torrent's ID and v1
  info-hash, not that its rules match `lib/workflow.py`, or its links.
- **The feed backup** in `$SCRATCH_DIR/feed-backup/` goes with the scratch directory. The feed can always be rebuilt
  from the published `.torrent` files.
- **A changed file is never re-archived** (README Appendix E): it stays on the site and stops the next `01-find`.
