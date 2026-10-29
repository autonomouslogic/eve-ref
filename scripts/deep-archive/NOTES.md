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

Strict, not just exact: `01-find` also aborts on a selected path that's merely *close* to one already archived or
already on `SEED_DIR` (`merge_conflicts`, "Merged seed directory") -- the same path once case-folded and
Unicode-normalised, or a path that's also a folder of another path. Both are fine as distinct keys on B2, but not as
two entries in the same merged folder tree. index.json used to be the one deliberate exception (DataIndex rewrites
it every run, so the same folder's would legitimately recur); it's not an exception any more because it's never
selected at all ("Index snapshot").

### MIN_BYTES and the torrent size limit

`min_bytes_reached` only enforces `MIN_BYTES` when the cutoff was reached by running out of old-enough candidates
(`cut.reason == "age"`). A selection that instead stopped because the `.torrent` estimate hit `MAX_TORRENT_BYTES`
(`cut.reason == "torrent"`) is exempt: a lot of small files can reach that hard limit with far less than
`MIN_BYTES` of data (measured in "Torrent size": about 470 GiB of typical files), and that's the size limit working
as intended, not a sign there isn't enough data yet. `cut.reason == "bytes"` never needs the exemption, since
`MAX_BYTES >= MIN_BYTES` by definition.

## Data per torrent

**Decided (2026-10-28): 250 to 300 GiB of data per torrent (`MIN_BYTES`, `MAX_BYTES`), 8 MiB pieces (`PIECE_SIZE`).**
Kept there: a torrent can never change once published, and mixed sizes make the archive harder to reason about. The
archive was about 5.7 TB at the time, so roughly 20 torrents. Before this it was 5 GiB each, which was a placeholder
from testing.

The purpose is crowd-sourced long-term storage, so the size is chosen for volunteers, not for the protocol: any size
from a few GiB to a few hundred GiB works technically. The research behind it is thin and mostly anecdotal. No
study of the best torrent size for long-term seeding turned up.

- **Bigger torrents** mean fewer torrents to build, sign, publish and list, and less tracker load. But fewer people
  have room for a whole one, and seeding a torrent is all or nothing, so each torrent has a smaller pool of possible
  seeders.
- **Smaller torrents** can be held by anyone, spread the seeding load, and make under-seeded ones easy to spot and
  repair. The cost is more torrents: more `.torrent` files and signatures, a longer list, more tracker announces, and
  more workflow runs. Anna's Archive and the Internet Archive's BAK proposal both said that gets hard at around
  100,000 torrents or more, not at hundreds.
- **What others do:** archive.org recommends staying under 1 TB per torrent. Anna's Archive suggests 100 GB to 1 TB
  per folder (it says that may change) and asks people to seed the torrents with the fewest seeders. The BAK proposal
  used 500 GB chunks. One volunteer said they would give up 200 to 500 GB, that archives of 1.5 TB and up put people
  off, that the oldest and largest torrents are the ones short of seeders, and that newer ones are being split to
  300 GB or less. Jason Scott argues for one large torrent per collection, as the average torrent barely lasts two
  months; that is about discoverability, and the trackers and DHT here cover it.
- **Why 250 to 300 GiB:** under archive.org's limit, in the range volunteers say they will hold, and about 20
  torrents is easy to run and list. `MIN_BYTES` is below `MAX_BYTES` so a batch doesn't end with a torrent that
  `01-find` refuses for being a few GiB short. Torrents can end slightly over `MAX_BYTES` (it is a soft limit: the
  whole last date is taken).
- **Consequences at this size:** the `.torrent` is about 3 MB (v1 only, scaled from "Torrent size"'s 9.3 MB for
  the old `MAX_BYTES`, plus the extra pieces), well under `MAX_TORRENT_BYTES`, so the file-count limit no longer decides where a
  selection stops.
  `SEED_DIR` needs 300 GiB free per run (00-preflight). Hashing is about 5 minutes at libtorrent's v1 speed. Only
  data older than `MIN_AGE_YEARS` counts, so not all of the 5.7 TB is eligible yet.

### Piece size

8 MiB, down from 32 MiB. It is what libtorrent picks automatically for a torrent of 164 to 655 GiB (its table in
`create_torrent`: 16 KiB shifted left once for each threshold below the total size, thresholds from 2.5 MiB, growing
4x each, up to 8 MiB for totals up to 703,687,441,777 bytes and 16 MiB above that). A 300 GiB torrent is about 38,000
pieces, fine for every client. The v1 `pieces` string is 20 bytes a piece, so about 0.4 MB more `.torrent` than at
16 MiB.

The reason to prefer smaller is partial downloads. Torrents are v1 only with no pad files, so files sit back to back
and a piece can hold the end of one file and the start of the next. Someone who wants a few files fetches the whole
pieces at each edge of the selection, including their neighbours' bytes. That overhead is *the cost of not padding*
(see "Piece alignment"; a padded hybrid torrent has none, in a client that skips pad files). It only hits partial
downloads, and only at the edges: a full download or a seeder pays nothing. Smaller pieces waste less at each edge
(the table in "Piece alignment" is 1.97 for one market-orders file at 16 MiB against 2.94 at 32 MiB, and 1.0007 for
a month of them; 8 MiB is lower still). Going lower than 8 MiB gains little and adds pieces to hash and track.

### Seeder discovery

Whatever the size, what keeps an archive alive is volunteers knowing which torrents need seeders. Anna's Archive
publishes seeder counts per torrent and asks for the least-seeded ones. A per-torrent seeder health page, and a
"needs seeds" list, would matter more than the choice of size. Not built yet.

Sources: [archive.org BitTorrents](https://help.archive.org/help/archive-bittorrents/),
[Anna's Archive](https://en.wikipedia.org/wiki/Anna%27s_Archive),
[INTERNETARCHIVE.BAK/torrents implementation](https://wiki.archiveteam.org/index.php/INTERNETARCHIVE.BAK/torrents_implementation),
[Jason Scott on one large torrent](https://ascii.textfiles.com/archives/1113),
[a volunteer on seeding large torrents](https://lemmy.dbzer0.com/comment/9326553).

## Torrent size

The numbers below were worked out for torrents of the old `MAX_BYTES` (990 GiB, the one place that figure is given) at
32 MiB pieces, before "Data per torrent" set `MAX_BYTES` to 300 GiB and `PIECE_SIZE` to 8 MiB. "The old `MAX_BYTES`"
elsewhere in this file means that. They show how the `.torrent` scales with the file count; the limit rarely binds at the new size.

libtorrent's defaults also cap a torrent at 2,097,152 pieces, far off at `PIECE_SIZE`. qBittorrent raises some of its
limits in its advanced settings (by default a 100 MiB `.torrent` and 10,000,000 bdecode tokens, against libtorrent's
10,000,000 bytes and 3,000,000 tokens); other clients may not, so libtorrent's defaults are the target.

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

Worked out (2026-10-26, 32 MiB pieces) from file sizes sampled on data.everef.net: market-orders about 17.3 MB per
file, fuzzwork ordersets 24.4 MB, public-contracts 5.2 MB, esi-scrape 76 MB. For the old `MAX_BYTES` laid out like those datasets
(one file of each of the first three every 30 minutes, an esi-scrape file a day: 66,265 files), hybrid sizes from
`lib/torrent_size.py` (checked against a real libtorrent hybrid of 580 of the files: 184,122 bytes, estimate 184,348),
v1-only sizes from libtorrent itself:

| Format  | Piece size | `.torrent` | Pad files                   |
|---------|------------|------------|-----------------------------|
| Hybrid  | 32 MiB     | 21.1 MB    | 1,108 GiB, 112% of the data |
| Hybrid  | 16 MiB     | 26.5 MB    | 758 GiB, 77%                |
| v1 only | 32 MiB     | 8.7 MB     | none                        |
| v1 only | 16 MiB     | 9.3 MB     | none                        |

- Hybrid costs about **317 bytes per file** of these sizes, v1 about 131. 32 MiB is the most compact hybrid piece size
  for them: a file smaller than a piece gets no v2 piece layer.
- **A 10 MB hybrid holds about 31,000 of these files, roughly 470 GiB: less than `MIN_BYTES`.** Decided against
  hybrid partly for this reason (below, "Piece alignment"); `01-find` also no longer treats the `.torrent` limit
  stopping the selection early as "not enough data" (`min_bytes_reached`, "Selection").
- Pad files cost libtorrent clients nothing; other clients download them ("Pad files in other clients"); EVE Ref's
  v1-only torrents have none either way.
- libtorrent's automatic piece size is 16 MiB at this size (its table stops there, from about 655 GiB). Its creator
  refuses pieces over 128 MiB since 2.0.11, qBittorrent offers 16 KiB to 128 MiB, and qBittorrent's libtorrent 1.2
  builds can't load 256 MiB pieces ([qBittorrent #23494](https://github.com/qbittorrent/qBittorrent/issues/23494)).

## Torrent format

- **v1 only**, decided after a review of libtorrent, qBittorrent and other clients (2026-10-26; "Sources"). Hybrid
  v1+v2 was the format up to then; "Piece alignment" and "Pad files in other clients" are what that review found,
  and the decision itself is below them, under "Piece alignment".
- Made with Python **libtorrent 2.x**, the same library as qBittorrent. Not `mktorrent`: it can't make v2 or
  modification-time fields (below), and hasn't had a release since 2017.
- Every file's **modification time** is included (`create_torrent.modification_time`): the manifest's for a data
  file, the file's own for a root file (README, checksum file, signature). Off by default in libtorrent, because a
  file re-created elsewhere with a different mtime would get a different info-hash -- not a risk here, since EVE Ref
  is the only creator and a file's mtime is the site's original upload time, carried through `03-load`. Doesn't
  change what's checked (the piece hashes do that, mtime or not). **Not a standard, and not useful to downloaders**
  (2026-10-27): no accepted BEP (3, 47, 52) defines per-file times, and `mtime` is a libtorrent convention. A
  test download with aria2c showed neither the field nor restored mtimes (other clients not tested), so a
  downloaded file's mtime is its download time. Only a tool parsing the raw bencode sees the
  dates, which people won't do. The practical source for a downloader is `last_modified` in the index tarball
  ("Index snapshot"), plain JSON in a `tar.xz`, data files only: root files' dates exist only in the torrent. No
  restore script is shipped; one reading the tarball and `touch`-ing each file would be easy to add if wanted.
- **No web seed**: the files are deleted from data.everef.net and a torrent can't change, so it would be a dead URL.
- The comment is outside the info dictionary and magnet-link users don't get it: the README inside the torrent carries
  the same link.

### libtorrent

From the source (RC_2_0 at 2.0.15, and 2.1; exact commits under "Sources"):

- `create_torrent` keeps the `file_storage` by reference and, for hybrid and v2, changes it in place: files sorted by
  path component (`a/y` before `a-b/x`, unlike the `.txt`'s whole-string order), and a `.pad/<size>` pad file after
  every file, the last one included (tail padding, since 2.0.8; `canonical_files_no_tail_padding` gives the old layout
  and another info-hash). Pad files of the same size share a path.
- The Python `set_piece_hashes` runs with default settings: one hashing thread (the default since 2.0.7), which Python
  can't change. Hybrid hashing measured 330 MB/s on an i5-10400 (no SHA-NI), data in the page cache, the same at 16 and
  32 MiB: about 55 minutes per old-`MAX_BYTES` torrent at best. v1 only measured 990 MB/s, about 18 minutes. Its callback gets a
  running count of hashed pieces, not a piece index.
- The bindings have `set_hash` but not `set_hash2`, in 2.0 and 2.1: v2 hashes can't be supplied from Python, only
  checked.
- Per-file v1 piece hashes (SHA-1 of each piece of the file, the last one zero-filled to the piece size) and v2 pieces
  roots (BEP 52's merkle tree of SHA-256 over 16 KiB blocks, filled to a power of two with zero hashes) computed with
  `hashlib` match libtorrent's exactly, tail padding included. Both, with the plain SHA-256, in one read at 32 MiB:
  186 MB/s on one thread, about 800 MB/s on six.
- The layout hasn't changed since 2.0.10 (February 2024, what Ubuntu 24.04 ships). 2.0.12 fixed error handling when
  hashing through mmap: on 2.0.10 a read error during `set_piece_hashes` can crash the process instead of raising.
  2.0.14 and 2.0.15 (August and September 2026) fixed v2 merkle tree and hash picker bugs, and a write corruption in
  mmap storage: these matter to the seeder, not to making torrents.
- 2.1 is out: it deprecates `create_torrent(file_storage&)` for `list_files()` and `create_file_entry`, makes
  `file_storage` "more immutable", and deprecates creator, comment and creation date (#7864). CI installs whatever
  `ubuntu-latest` ships.
- The docs call `v1_only` "mainly for tests, not recommended for production use": libtorrent pushing v2, not a defect.
- **`create_torrent` keeps a C++ reference to the `file_storage`, not a copy** (the docs say so: "The file_storage
  and torrent_info objects must stay alive for the entire duration of the create_torrent object"), and the Python
  binding doesn't protect against it. Building the `file_storage` in a helper function and returning only the
  `create_torrent` crashes (a segfault, or `RuntimeError: Bad address`) once Python garbage-collects the
  `file_storage` after the helper returns, while the `create_torrent` still holds the now-dangling reference. Caught
  by `05-make-torrent`'s end-to-end test; the fix is returning the `file_storage` too and keeping it referenced
  until `generate()`.

### qBittorrent

From the source (master, 5.4.0 in development; exact commit under "Sources"):

- Its torrent creator makes the same kind of libtorrent calls as `05-make-torrent`, but defaults differ:
  `create_torrent` (hybrid by default, not v1 only), trackers (all in tier 0 unless separated by blank lines, not a
  fixed checked list), the same single-threaded `set_piece_hashes`, creator, comment, `generate()`, and an optional
  `source` key in the info dictionary. It walks the folder (natural sort, dotfiles skipped) and doesn't check its
  result. Piece size: libtorrent's automatic one, or 16 KiB to 128 MiB.
- Its defaults: one hashing thread, announce to all tiers but not to every tracker in a tier, and the limits in
  "Torrent size".

### Piece alignment

Pad files make every file start at a piece boundary. In v1 (BEP 3) files are back to back, so a piece can hold the end
of one file and the start of the next. Aligned, every piece holds one file's bytes only, and a file's piece hashes
depend on nothing but its content and the piece size.

Required or not:

- **v1**: optional. BitComet added padding files in 2007 (0.85); BEP 47 (2016) made them a standard, and says "the
  presence of padding files does not imply that all files are piece-aligned".
- **v2**: built in. BEP 52 hashes each file as its own merkle tree ("identical files always result in the same root
  hash"), so no piece can span two files.
- **Hybrid**: required for more than one file. BEP 52's "Upgrade Path": the v1 `pieces` and `files` "must be generated
  to describe the same data in the same order", and "since the old format did not align files to piece boundaries a
  multifile torrent must use BEP 47 padding files to achieve identical alignment". Piece N is then the same bytes in
  both swarms, one copy on disk serves both, and every piece is checked against both hashes.

What it's for:

- **Reusing data across torrents.** BEP 47's whole rationale is one line: "Piece-aligned files simplify deduplication
  [BEP 38] and the operations on mutable torrents [BEP 39]." BEP 38 reuses pieces whose hash matches another
  torrent's, which "greatly favors torrents with piece-aligned files": unaligned, the same file only gets the same
  piece hashes if it sits at the same offset modulo the piece size. BEP 39 replaces a torrent with a newer version,
  which can then reuse its unchanged files. libtorrent 1.2's `mutable_torrent_support` aligned every file to the piece
  size "because piece hashes are compared to determine whether files are identical". BitComet's padding was for the same
  thing (Long-Term Seeding: getting a file from users who have it from another torrent) and for getting single files
  from HTTP, FTP and eD2k sources
  ([TorrentFreak, 2007](https://torrentfreak.com/bitcomet-pollutes-bittorrent-with-junk-data/)).
- **Tail padding** follows from it: libtorrent pads the last file as well, "in order to match an equivalent
  tail-padded file" (its 2016 code comment), so a file's last piece hash doesn't depend on the file being last. BEP
  52's reference creator does it for multi-file torrents. libtorrent 1.1 and 1.2 did it only with
  `mutable_torrent_support`, 2.0 to 2.0.7 not at all, 2.0.8 on always. Single-file torrents aren't padded.
- **Files on their own.** A selected file is exactly its own pieces, and a complete file verifies without its
  neighbours. Unaligned, a file's first and last pieces also hold bytes of the files next to it, which get downloaded
  too (libtorrent keeps them in a `.parts` file).
- **Web seeds**: a piece spanning files needs requests to more than one URL. libtorrent's manual ("HTTP seeding"): when
  a web seed redirects files to different hosts, they must be piece-aligned.
- **Disk I/O**: every piece read or write touches one file. libtorrent's old `optimize_alignment` padded "for optimized
  disk-I/O", and 0.15 added unbuffered I/O for aligned files.

For these torrents, beyond hybrid needing it, only partial downloads are left: no path is in two torrents ("No path in
two torrents"), a published torrent never changes, and there's no web seed. For "Torrent size"'s layout of the old `MAX_BYTES`, the
bytes downloaded per byte wanted (hybrid in a libtorrent client: always 1, as pad files are never fetched):

| Selection              | v1 only, 16 MiB | v1 only, 32 MiB |
|------------------------|-----------------|-----------------|
| One market-orders file | 1.97            | 2.94            |
| One day of them        | 1.020           | 1.040           |
| One month of them      | 1.0007          | 1.0014          |

A dataset's files sit together in path order, so v1 only costs extra at the two ends of a selection: a lot for a
single file, little for a date range. A client without pad file support pays on everything instead: 2.1 for the whole
32 MiB hybrid ("Pad files in other clients").

**Decided: v1 only** (NOTES.md "Torrent format"). None of alignment's reasons apply to these torrents: nothing is
ever in two torrents, a published torrent never changes, and there's no web seed, so there's nothing to deduplicate
and no mutable-torrent update to make. What's left is the single-file and partial-run cost above, real but small,
against hybrid's cost: about 2.4 times the `.torrent` bytes per file, pad files other clients download and store
("Pad files in other clients"), a third the hashing speed ("libtorrent"), and the hybrid-only code paths (pad
filtering, the v2-only parts of libtorrent that needed a 2026 run of fixes). `05-make-torrent` makes `v1_only`
torrents; see "Open issues" for what's still unresolved.

### Pad files in other clients

Historical: EVE Ref's torrents are v1 only (decided under "Piece alignment"), so they have no pad files. Kept for
why: a hybrid torrent's v1 file list holds the pad files (BEP 52 requires BEP 47 padding). Clients that know BEP 47
never download or store them; the rest download the zeros and keep them in a `.pad/` folder.

- **Transmission 4.x** doesn't know them ([#3387](https://github.com/transmission/transmission/issues/3387), open as of
  May 2026). Ubuntu 24.04's 4.0.5 lists `.pad/*` as ordinary files: 1.57 MB in all for 0.70 MB of data, in a
  scaled-down copy of these torrents. With "Torrent size"'s 32 MiB hybrid, a Transmission user downloads and stores more
  than twice the data.
- **aria2** 1.37 doesn't either ([#2312](https://github.com/aria2/aria2/issues/2312), open): it fetches the pad files
  and can stall. `08-check-seed` still works, as qBittorrent serves the zeros.
- **rTorrent** before 0.15.2 (Ubuntu 24.04 ships 0.9.8) refuses a torrent with two files on the same path
  ([#1156](https://github.com/rakshasa/rtorrent/issues/1156)), which libtorrent's `.pad/<size>` names all but
  guarantee at this scale. The repeats are BEP 47's recommendation, on purpose: a client without BEP 47 then writes
  every padding file into one folder, "potentially re-using padding files from other torrents".
- libtorrent clients (qBittorrent, Deluge) handle them. BiglyBT supports hybrid torrents since 2.5; its pad handling
  is unchecked.

### Trackers

**Decided: a fixed, hand-picked list** (`lib/trackers.txt`, loaded by `lib/trackers.py`), not fetched from anywhere
at run time. `00-preflight` checks each is reachable (a BEP 15 connect request for UDP, a BEP 3 announce for HTTP(S);
`check_trackers`), and fails if
fewer than `MIN_WORKING_TRACKERS` answer; `05-make-torrent` just uses the list, trusting that check.

The list is seven UDP trackers plus three plain-HTTP ones (`tracker.opentrackr.org:1337`, `open.tracker.cl:1337`,
`tracker.dler.org:6969`), for people whose network blocks UDP. Picked from ngosang's HTTP list on 2026-10-27 as the
ones run by operators already trusted for UDP or long-listed; each answered a real announce with a valid bencoded
reply. `00-preflight` checks them with a BEP 3 announce for a random info-hash (any bencoded reply counts), so they count toward `MIN_WORKING_TRACKERS` like the UDP ones. No HTTPS
tracker was stable enough to add (the HTTPS entries on those lists are mostly short-lived or piracy-branded domains).

Why not fetch a list (ngosang/XIU2's, as it used to): a `.torrent` can't change once published, so a tracker domain
that lapses can be registered by anyone, who then gets the announces of everyone still sharing the torrent (their
IPs and the info-hash) and can hand out junk peers. The lists also decay fast and carry domains not worth that risk:

- Of ngosang's best 20 from 1, 2 and 3 years earlier, only 7–9 were still listed. The same 7 survived every time:
  `opentrackr.org:1337`, `open.stealth.si:80`, `tracker.torrent.eu.org:451`, `open.demonii.com:1337`,
  `exodus.desync.com:6969`, `explodie.org:6969`, `tracker-udp.gbitt.info:80` -- exactly `lib/trackers.txt`'s list.
  DHT and PEX carry the swarm once trackers die, so even these seven matter less over time.
- Both lists include piracy-branded domains, and XIU2's has `bt1/bt2.archive.org` and a WebSocket tracker. On
  2026-10-26 their union was 69 trackers (41 UDP, 14 HTTP, 14 HTTPS), among them `wareztorrent.com`,
  `foreverpirates.co`, nyaa trackers, a `duckdns.org` host, `.xyz`, `.top` and `.ga` domains, and personal servers.

Potential sources, if the fixed list ever needs refreshing by hand: the "best" lists from
[ngosang/trackerslist](https://github.com/ngosang/trackerslist) and
[XIU2/TrackersListCollection](https://github.com/XIU2/TrackersListCollection), UDP and HTTP(S) entries only (the
rest -- WebSocket, I2P, Yggdrasil -- need a client feature EVE Ref can't assume).

Client behaviour, unrelated to where the list comes from:

- Clients follow BEP 12: shuffle a tier, use the first tracker that answers, only move on if the tier fails.
  qBittorrent announces to all tiers by default, so one per tier reaches every tracker. libtorrent's own default
  (Deluge and other libtorrent clients) stops at the first tier with a working tracker: there, the tiers are fallbacks.
- Trackers sit outside the info dictionary: not part of the info-hash, and clients merge tracker lists when the same
  torrent is added again.

An EVE Ref tracker (opentracker, under everef.net), in tier 0, would last as long as the seeder, on a domain nobody
else can register -- not done; would need its own hosting and upkeep.

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

## Index snapshot

`index.json` is never selected and never in a torrent ("No path in two torrents" -- DataIndex rewrites it on every
run, so its size and date say nothing about the archived data, and it would otherwise legitimately recur across
more than one torrent's file list). What it holds that nothing else does: `etag` (B2's, not reproduced anywhere
else) and the `type`, `file_time` and `sequence_number` `ArchivePathFactories` worked out from the path (`size` and
`last_modified` already match the torrent's own per-file length and `mtime`). Dropping it entirely would lose that,
with no way to recover it once the source file is gone from the bucket -- so `03-load` snapshots the `index.json`
of every folder it selects a file from into a root file, `<ID>-index.tar.xz`, instead:

- Downloaded **fresh from the bucket** in `03-load`, not through `LOCAL_COPY_DIR`: the snapshot is meant to be the
  bucket's current state at archive time, and the local copy is explicitly untrusted elsewhere in the same script.
- Checked for **coverage** before being tarred up: every selected file must appear in its folder's downloaded
  `index.json`, with the same size and modification time (to the second) as the selection recorded. A file the
  snapshot doesn't cover fails the step outright, rather than publishing a tarball that's missing data for files it
  claims to index: the fix is a `DataIndex` run for that prefix (so the folder's `index.json` catches up), then
  `03-load` again (resumable).
- **Members named by site path, no prefix:** every member is named exactly as the bucket names it
  (`wars/history/index.json`), so extracting the tarball into a copy of the site's root puts each `index.json` back in its original place. It's
  one tarball, not part of the data tree, so unlike a data file it can't collide with another torrent's paths.
- **PAX format, sorted by path, zeroed uid/gid/uname/gname, mode 0644**, each member's mtime the downloaded file's
  own: the same inputs always produce the same tarball bytes. No directory entries (consistent with the data
  torrent's own file list, which has none either).
- Not in the manifest, the `.txt` file list or the delete set: it's a root file (`root_file_names`), the same
  category as the README and the checksum file, never a data file.
- Covered by `<ID>.sha256`, so a copy obtained any other way can still be checked against the published signature.

## Steps 12 onwards

`12-check-before-delete`, `13-delete`, `14-check-delete` and `16-check-site` (and with them the manual steps 15 and 17)
are **untested and probably broken**. They are disabled: each script calls `refuse_to_run` (lib/workflow.py) from its
`__main__` block and exits with an error before doing anything. Remove that call to enable one.

Revisit them when files are actually about to be deleted from the data site. That is deliberately much later than
building torrents: the data stays on the site until other people are seeding it, and `01-find` skips what earlier
torrents already hold. When revisiting:
- They must not rely on anything in `SCRATCH_DIR` (`manifest.json`, `find/`, completion files and so on): it will
  likely have been wiped by then. What survives is `SEED_DIR` (the merged tree, `.torrents/`, `<ID>.sha256`) and
  what's published on the data site (`deep-archive/<ID>.txt`, the `.torrent`, the feed).
- The README's description of these steps assumes the old flow, where the scratch directory is still there.

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

## Merged seed directory

`$SEED_DIR` holds every torrent's files laid out as one tree matching data.everef.net, with no torrent's own root
folder: the same thing a user merging several of these torrents into one download folder (the README template's
"Merging with other EVE Ref Deep Archive torrents") ends up with. Keeping it that way on the seeder itself means the
first place a path collision would show up is here, caught by `01-find`'s `merge_conflicts` check before a new
torrent is even built, rather than discovered later by someone merging their own downloads. It also means the
monthly check (`check-seed-dir.py`, README Appendix C) tests the exact guarantee users rely on.

- **`$SEED_DIR/.staging/<ID>/`**, not `$SEED_DIR/<ID>/`: where `03-load` through `06-sign-torrent` build a torrent's
  content (`Workflow.content_dir`). Never merge into the live tree directly -- an aborted run would leave files that
  belong to no torrent mixed in with the only copy of everything already archived, and cleaning that up would mean
  deleting inside the merged tree itself, not a self-contained folder.
- **`$SEED_DIR/.torrents/`**: a signed copy of every published `.torrent` and its `.minisig`, copied in by `07-seed`
  (closing the "Open issues" item asking for exactly this). `check-seed-dir.py` reads these back, verified, as the
  authoritative list of what SEED_DIR should contain -- it never trusts the files on disk to say what they are.
- **The merge (`07-seed`, `lib/merge.py`)**: every file is read from the `.torrent`'s own raw bencoded info dict
  (path, size, `mtime`; libtorrent's typed API doesn't expose `mtime` once loaded), hard-linked from staging to its
  site path directly under `$SEED_DIR` (root files land at `$SEED_DIR`'s own root, having no further path segments),
  verified, and only then unlinked from staging. Every step is safe to resume: `os.link` never overwrites an
  existing target (it fails instead, which is also how a genuine collision that slipped past `01-find` would be
  caught here, one layer later), and a staging file is never unlinked until its target is confirmed to be the very
  same inode with at least one more link elsewhere -- the one guard against ever ending up with zero copies of a
  file. The plan (which file goes where, and the staging file's `(dev, ino)` recorded at the time) is saved to
  `$SCRATCH_DIR/merge-plan.json`: once a staging file is unlinked, it can no longer be stat'd to rebuild the plan
  from scratch, so a resumed run has to read it back instead.
- **qBittorrent content layout: `NoSubfolder`, not `Original`.** The files are already at their final paths directly
  under the save path by the time the torrent is added, so `Original` (which would look for them under
  `$SEED_DIR/<ID>/`) would show the torrent as needing a full download. If qBittorrent's resume data for a torrent is
  ever lost and it has to be re-added, it must be re-added with `NoSubfolder` by hand -- nothing else remembers which
  layout a given torrent needs.
- **Known limitation:** Windows-invalid names, reserved names, and trailing dots or spaces (the "Open issues" item
  already tracking this for one torrent's own file list) matter more once torrents are expected to merge: a
  collision from two different torrents' paths colliding under such renaming is exactly the kind `merge_conflicts`
  is meant to catch, but doesn't yet.

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

### Super seeding

`07-seed` does not turn it on. It's meant for bootstrapping a brand new swarm, and this seed runs for years
("Durability"), so it stays in ordinary seeding mode. `lib/qbittorrent.py` keeps `set_super_seeding` for doing it by
hand if ever needed.

## Known limitations

- **Unchecked on real files:** that rclone's `ModTime` on B2 is the upload time for the Java uploads, and that B2 honours
  Content-Type and Cache-Control set through `--header-upload`.
- **Between load and the torrent**, there's no reason a file would change (nothing else writes to the staging folder,
  `$SEED_DIR/.staging/<ID>/`, between the two steps), but `05-make-torrent` checks anyway, for good measure:
  `content_problems`' cheap size and
  modification-time check, then `sha256sum -c` on a random sample of `SHA256_SAMPLE_SIZE` files (`sample_sha256sums`)
  rather than a full re-read: `03-load` already did a full `sha256sum -c` against the same checksum file, and the
  piece hashing right after would catch corruption too, so a second full-content pass only cost time; the sample is
  a cheap extra smoke test, not the thing standing between a bad file and a published torrent. `08-check-seed`'s
  recheck only catches a change made after the torrent was made, not before it.
- **The docs page's rules aren't checked.** `12-check-before-delete` checks the page lists the torrent, not that its
  rules match `lib/workflow.py`, or its links.
- **The feed backup** in `$SCRATCH_DIR/feed-backup/` goes with the scratch directory. The feed can always be rebuilt
  from the published `.torrent` files.

## Open issues

From a review of torrent making against libtorrent, qBittorrent and other clients (2026-10-26; "Sources"). The
format (v1 only), trackers (a fixed list), the `MIN_BYTES`/`.torrent`-limit interaction, and the load-to-torrent gap
that review raised are decided and implemented (see "Torrent format", "Piece alignment", "Trackers", "Selection"
and "Known limitations"). Keeping a copy of `<ID>.torrent` and its signature outside `$SCRATCH_DIR`, and case-fold
collisions across the merged tree, are also done (`$SEED_DIR/.torrents/`, `merge_conflicts`: "Merged seed
directory"). What's left:

- Reject Windows-invalid characters, reserved names (`CON`, `NUL`, …), and trailing dots or spaces, in
  `site_path_problem`: libtorrent renames such files on Windows, and `sha256sum -c` then fails. Matters more now
  that torrents are expected to merge into one tree ("Merged seed directory"): a renamed file from one torrent could
  collide with another's path, and `merge_conflicts` doesn't check for this yet.
- `check-seed-dir.py` doesn't check for a torrent in qBittorrent that isn't in `$SEED_DIR/.torrents/` at all (an
  extra, unexpected torrent sharing `$SEED_DIR` as its save path): only that every known torrent is seeding cleanly.

## Sources

For the research of 2026-10-26 in "Torrent size", "Torrent format" and "Open issues". Source code was read at the
commits given; web pages as of that day.

Specifications, from [bittorrent.org](https://github.com/bittorrent/bittorrent.org) at `7b7b41f4`:

- [BEP 3](https://www.bittorrent.org/beps/bep_0003.html) (v1), [BEP 12](https://www.bittorrent.org/beps/bep_0012.html)
  (tracker tiers), [BEP 15](https://www.bittorrent.org/beps/bep_0015.html) (UDP trackers),
  [BEP 19](https://www.bittorrent.org/beps/bep_0019.html) (web seeds).
- [BEP 38](https://www.bittorrent.org/beps/bep_0038.html) (reusing local data),
  [BEP 39](https://www.bittorrent.org/beps/bep_0039.html) (updating torrents),
  [BEP 47](https://www.bittorrent.org/beps/bep_0047.html) (pad files and file attributes).
- [BEP 52](https://www.bittorrent.org/beps/bep_0052.html) (v2 and hybrid), and its
  [reference torrent creator](https://www.bittorrent.org/beps/bep_0052_torrent_creator.py) (tail padding).

libtorrent, from [arvidn/libtorrent](https://github.com/arvidn/libtorrent):

- RC_2_0 at `eb397173` (2026-10-18: 2.0.15 and unreleased fixes): `src/create_torrent.cpp` (`set_piece_hashes`, the
  automatic piece size, the 128 MiB limit, `generate()`), `src/file_storage.cpp` (`canonicalize_impl`),
  `src/settings_pack.cpp` (hashing threads, announce defaults), `include/libtorrent/torrent_info.hpp` (load limits),
  `include/libtorrent/create_torrent.hpp` (the flags' docs), `bindings/python/src/create_torrent.cpp`,
  `docs/manual.rst` ("HTTP seeding"), `docs/upgrade_to_2.0.rst`, `ChangeLog`, and the release tags' dates.
- Commits `acf77a53` (2016, 1.1.2: "The last file must be padded too"), `74d82a0c` (2022, 2.0.8: tail padding back),
  `56732459` (2.0.12: errors while hashing through mmap), `3f95bf4d` (2.0.14: mmap write corruption), and the 2.0.10
  to 2.0.15 diff of `src/create_torrent.cpp` and `src/file_storage.cpp` (layout unchanged).
- v1.2.19's `src/create_torrent.cpp` (`mutable_torrent_support`), and master at `14fef2ae` (2.1):
  `include/libtorrent/create_torrent.hpp` and `bindings/python/src/create_torrent.cpp`.
- [Reference: Create Torrents](https://www.libtorrent.org/reference-Create_Torrents.html) (2.1.0),
  [2.1.0 release notes](https://newreleases.io/project/github/arvidn/libtorrent/release/v2.1.0),
  [blog: BitTorrent v2](https://blog.libtorrent.org/2020/09/bittorrent-v2/).

qBittorrent, from [qbittorrent/qBittorrent](https://github.com/qbittorrent/qBittorrent) master at `408efd10`
(2026-10-25): `src/base/bittorrent/torrentcreator.cpp` and `.h`, `src/base/bittorrent/sessionimpl.cpp` (announce and
hashing defaults), `src/base/preferences.cpp` (load limits), `src/gui/torrentcreatordialog.cpp` (piece sizes); issue
[#23494](https://github.com/qbittorrent/qBittorrent/issues/23494).

Other clients:

- Issues: Transmission [#3387](https://github.com/transmission/transmission/issues/3387), aria2
  [#2312](https://github.com/aria2/aria2/issues/2312), rTorrent
  [#1156](https://github.com/rakshasa/rtorrent/issues/1156).
- Ubuntu 24.04 versions, from `apt-cache policy`: transmission 4.0.5, rtorrent 0.9.8, aria2 1.37.0, deluge 2.1.2,
  qbittorrent-nox 4.6.3, python3-libtorrent 2.0.10.
- Ubuntu's own torrents, from [releases.ubuntu.com](https://releases.ubuntu.com/): `ubuntu-26.10-desktop-amd64`,
  `ubuntu-26.04.1-live-server-amd64` and `ubuntu-24.04.5.1-desktop-amd64` (`.iso.torrent`), read with libtorrent.
  [mktorrent](https://github.com/pobrn/mktorrent): last release 1.1 (January 2017), last commit January 2021, v2
  support asked for in [#29](https://github.com/pobrn/mktorrent/issues/29), open since 2019.

Trackers and history:

- The two lists considered as "potential sources" ("Trackers"), as downloaded that day (before the fixed-list
  decision): [ngosang/trackerslist](https://github.com/ngosang/trackerslist) and
  [XIU2/TrackersListCollection](https://github.com/XIU2/TrackersListCollection), and ngosang's README.
- How a lapsed domain can end up elsewhere: ExtraTorrent.ag, parked and redirecting to a domain flagged as malicious
  for a day in May 2018
  ([TorrentFreak](https://torrentfreak.com/extratorrent-replacement-displays-warning-on-predecessors-shutdown-anniversary-180518/)).
- BitComet's padding files:
  [TorrentFreak, 2007](https://torrentfreak.com/bitcomet-pollutes-bittorrent-with-junk-data/),
  [Wikipedia](https://en.wikipedia.org/wiki/BitComet) (0.85, early 2007, for "Long-Term Seeding").

Data and measurements:

- File sizes: data.everef.net's `index.json` listings for `market-orders/history` (2022, 2023), `fuzzwork/ordersets`,
  `public-contracts/history/2023` and `esi-scrape`, a few folders each.
- Measured in an Ubuntu 24.04 container on an i5-10400 with Python 3.12 and libtorrent 2.0.10's bindings (the scripts
  weren't kept): `set_piece_hashes` on a 2 GiB random file in the page cache, hybrid and v1 only; `lib/torrent_size.py`
  against a real hybrid of 580 sparse files; v1-only sizes at 8, 16 and 32 MiB from `create_torrent` with `v1_only`
  and dummy `set_hash` values; the `hashlib` hashes against `hash_for_piece` and `files().root` for empty, one-byte,
  one-piece and multi-piece files; `transmission-show` 4.0.5 on a hybrid scaled down to 256 KiB pieces; partial
  downloads from the pieces that each file's offset and size cover in the old `MAX_BYTES` layout.
