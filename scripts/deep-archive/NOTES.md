# EVE Ref Deep Archive: Notes

Reference for working on the scripts: only what the code and its comments don't already say, such as the constraints
behind the design, measurements, and facts from outside these scripts. Anything the code says belongs in the code, not
here. How to run a workflow: [`README.md`](README.md). What each script does: its own docstring. Sections are referred
to by name from the code (e.g. `NOTES.md "Deletion"`).

## Context

- data.everef.net is a **Backblaze B2** bucket behind a **Cloudflare Worker** (separate repo). The Worker must pass
  the objects' Content-Type and Cache-Control through, and must not block RSS readers or torrent clients fetching
  `.torrent` files.

## Selection

### Dating

- The Java uploads go through B2's S3 API, which doesn't set `src_last_modified_millis`, so their `ModTime` should be
  the upload time (unchecked: "Known limitations").
- Dates in the file name put backfills and re-uploads with their period.
- Fuzzwork IDs never read as years (backfills 21000–59999, live from 90019); other datasets' IDs aren't checked.
- The 2-year age is well clear of the market history scraper's 30-day snapshot lookback
  (`ESI_MARKET_HISTORY_SNAPSHOT_LOOKBACK`, read by `HistoricalOrdersRegionTypeSource`).

### Never archived

`TORRENT_EXCLUDE` is a denylist, so new datasets are archived automatically once old enough.

- `market-history/` and `killmails/`: `ScrapeMarketHistory` also re-reads the last 450 days
  (`ESI_MARKET_HISTORY_LOOKBACK`), and `import-market-history` needs the full history over HTTP.
- `ccp/` is about 62 GiB, QENs included. The syncs wouldn't re-upload it: `SyncMer` only checks from two months before
  its latest MER, `SyncStaticData` only the latest SDE.

### No path in two torrents

Nothing relies on cutoffs being in order: `TORRENT_INCLUDE` can put older files in a later torrent. Telling a new
version of a listed path apart would need its modification time in the `.txt`.

## Torrent size

libtorrent's defaults also cap a torrent at 2,097,152 pieces, far off at `PIECE_SIZE`. qBittorrent raises some of its
limits in its advanced settings; other clients may not, so libtorrent's defaults are the target.

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

- **Hybrid v1+v2**: v1 for client compatibility, v2 for per-file SHA-256.
- Made with Python **libtorrent 2.x**, the same library as qBittorrent. Not `mktorrent`: v1 only.
- **No web seed**: the files are deleted from data.everef.net and a torrent can't change, so it would be a dead URL.
- The comment is outside the info dictionary and magnet-link users don't get it: the README inside the torrent carries
  the same link.

### Trackers

- Clients follow BEP 12: shuffle a tier, use the first tracker that answers, only move on if the tier fails.
  qBittorrent announces to all tiers by default, so one per tier reaches every tracker.
- Trackers sit outside the info dictionary: not part of the info-hash, and clients merge tracker lists when the same
  torrent is added again.
- The lists decay fast: of ngosang's best 20 from 1, 2 and 3 years earlier, only 7–9 were still listed. The same 7
  survived every time (`opentrackr.org:1337`, `open.stealth.si:80`, `tracker.torrent.eu.org:451`,
  `open.demonii.com:1337`, `exodus.desync.com:6969`, `explodie.org:6969`, `tracker-udp.gbitt.info:80`). DHT and PEX
  carry the swarm once trackers die.
- Both lists include piracy-branded domains, and XIU2's has `bt1/bt2.archive.org` and a WebSocket tracker.

## Published files

### `<ID>.txt`

Every published list is format-checked, not just a new one: a list in the wrong format (e.g. with the root folder in
its paths) would match no site path, and its files could land in a second torrent unnoticed.

### Feed

The target is qBittorrent's RSS downloader and FlexGet/autodl-style tools. `guid` is the v1 info-hash, so readers
never pick a torrent up twice.

## Inside the torrent

- `<ID>.sha256` is the only record of every file's SHA-256 that outlives `$SCRATCH_DIR`.
- The README's licence text is copied from `LICENSE-CCP`: update both together.
- The public key is on the main site, not the data bucket, so whoever can write to the bucket can't replace it.

## Deletion

On B2 a new upload always gets a new modification time, so size and time detect changes: no MD5/ETag matching
anywhere, and SHA-256 covers integrity from load onwards.

**Soft delete, treated as permanent.** The bucket's lifecycle rule purges hidden versions after about 2 days: too
short to rely on, so rollback is a re-upload from `$SEED_DIR` (README Appendix D).

### Stale index files

Folders are virtual on B2, but `DataIndex` (`cli/DataIndex.java`) never deletes stale index files: a folder it no
longer lists keeps its old `index.html`/`index.json`, still linking the deleted files. It also skips existing index
files when listing, so a folder holding only index files drops out of its parent's listing. A `DataIndex` that cleaned
these up itself would stop stale indexes from other deletes too; it must never delete the root index.

### Cloudflare cache

No purge: archive files have a 30-day max-age (`DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE`), index pages 2 minutes, so a
deleted file can still be served from the cache for up to 30 days.

## Code that depends on deleted data

| Consumer | Problem | Handled by |
|---|---|---|
| `SyncFuzzworkOrdersets` | Syncs every orderset missing from the site, so would re-upload deleted ones. | ID cutoff ("Fuzzwork sync cutoff"). |
| `DataCrawler` | Between the delete and the full `DataIndex`, parent indexes link removed folders; it fails on the missing index. | Accepted: crawling jobs (`sync-fuzzwork-ordersets`, market history scrape) fail and recover on their next run after `DataIndex`. |
| `SyncMer` | Rescans from `MER_FIRST_MONTH` if no MER files exist. | `ccp/` excluded. Even archived, the latest MER is always too new, and it only checks from two months before it. |
| `ScrapeMarketHistory`, `import-market-history` | Read up to 450 days, or all, of `market-history/`. | Never archived. |
| Users running `rclone sync` | Deletes their local copies of files deleted on the site. | The docs use `rclone copy`, and say why. |

### Fuzzwork sync cutoff

Must be deployed before the first `fuzzwork/` delete.

- By ID, not date: an orderset's date is only known after downloading it.
- Gaps are filled because downloads run `CONCURRENCY` at a time: with a "latest ID" rule, one failing while a later one
  succeeds would leave a permanent gap.
- Counting `backfills/` would pin the floor at 21000 while they're on the site.

## Storage access

- Only rclone, never the AWS CLI or an S3 library, so other storage would only mean another rclone remote.
- Listings go straight to the storage, so they're always fresh, unlike the site behind Cloudflare.
- One key means any step could write. Only `09-publish` (to `deep-archive/`) and `13-delete` (an explicit list) do.
- HTTPS against data.everef.net is only used to check what the site serves (`09-publish`, `16-check-site`).
- Cost: a full listing is one class C transaction per 1,000 files, about 1,000 for the site's million files, and takes
  minutes. Loading a torrent is up to 1 TiB of B2 egress (free to Cloudflare, an allowance direct).

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

- Scripts never call the next step. No directory or torrent ID arguments: settings come from `torrents.env`,
  everything else is fixed in `lib/workflow.py`.
- Check steps are read-only and re-derive everything from the bucket, disk, qBittorrent or site. `03-load`,
  `04`/`06` signing, `05-make-torrent` and `09-publish` check their own results instead, re-reading from disk or the
  site; a bug shared by an action and its own check isn't caught.
- **Nothing removes or stops a torrent in qBittorrent.** `lib/qbittorrent.py` has no `torrents/delete` call, with or
  without `deleteFiles`, and no `torrents/stop` (`torrents/pause` before qBittorrent 5.0), and must never get either:
  after `13-delete`, `$SEED_DIR` holds EVE Ref's only copy ("Durability"), and it has to keep seeding. Removing a
  torrent (e.g. to redo `07-seed`) is done by hand.
- Any script change that changes behaviour or output updates `README.md` in the same commit.

## Known limitations

- **Unchecked on real files:** that rclone's `ModTime` on B2 is the upload time for the Java uploads, and that B2 honours
  Content-Type and Cache-Control set through `--header-upload`.
- **Between load and the torrent**, only each file's size and modification time are checked: silent corruption that
  keeps both isn't caught until `08-check-seed`'s recheck; a full guard would be another read of all the data.
- **`08-check-seed` isn't an outside test**: when NAT hairpinning gets in the way, use local peer discovery, or check
  from another machine.
- **aria2c is v1 only** and fetches only the README: v2-only clients, magnet metadata and data files with unusual
  paths go untested.
- **The docs page's rules aren't checked.** `12-check-before-delete` checks the page lists the torrent, not that its
  rules match `lib/workflow.py`, or its links.
- **The feed backup** in `$SCRATCH_DIR/feed-backup/` goes with the scratch directory. The feed can always be rebuilt
  from the published `.torrent` files.
