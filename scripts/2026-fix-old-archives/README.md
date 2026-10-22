# 2026-fix-old-archives

Temporary working notes for fixing old data archives (see `scripts/rollup/`). Kept for
future records, not a permanent doc.

## scripts/rollup notes

### 1. Archive format

- Layout: `<archive>/history/<year>/<YYYY-MM-DD>/<name>.json` (same paths as the live data site, so multiple extracted archives recombine into one tree) — `scripts/rollup/common.sh:81` (`YEAR_PATH`), `scripts/rollup/README.md:15-21`.
- Naming: `<archive>-<year>.tar.xz` — `common.sh:80` (`ARCHIVE_NAME`).
- Contents: `.json.bz2` data files decompressed to plain `.json` (`04-prepare.sh:38-39`); `index.html` dropped (`04-prepare.sh:21-36`); `index.json` kept (README.md:19-21).
- Tar entries: regular files only, sorted by path (`LC_ALL=C sort -z`), `--owner=0 --group=0 --numeric-owner --mode='a=r,u+w'`, GNU tar format (`05-archive.sh:19-25`; `NOTES.md:22-27` bans `--format=posix`). Compression: `tar -cJf` = xz default preset -6 (`05-archive.sh:24`; README.md:22).
- Archive mtime = build time, no custom metadata (README.md:23); no `Cache-Control` header set on upload — documented gap (`README.md:105-112`).
- Checksums/manifest: per-file `sha256sum` manifest `$ARCHIVE-$YEAR.sha256` written in `04-prepare.sh:51-56`, rechecked against extracted archive contents in `05-archive.sh:82-92`. Whole-archive `archive.sha256`/`archive.sha1` written in `05-archive.sh:94-99`, verified against the B2 remote and the public HTTPS copy in `06-upload.sh`. No manifest file is shipped alongside the archive itself — the hashes live only in the local scratch dir and command output.

### 2. Safety checks

- **Scope/arg validation**: archive name regex, scope allow-list (`lib/scope.sh`), year regex, and "year must be finished" check — `common.sh:25-44`.
- **Locking**: single flock on `/tmp/everef-rollup.lock`, non-blocking (`common.sh:83-87`).
- **Step gating/idempotency**: refuses rerun if `COMPLETE` or this step's `.done` exists, and refuses to run if an earlier step's `.done` is missing — `common.sh:99-117`. `mark_done` only written after all checks pass (`common.sh:126-131`).
- **Preflight**: tool presence + GNU tar check, confirms next year's dir exists (proves scraping moved on), confirms archive absent on remote and public URL (404) — `01-preflight.sh:9-46`.
- **Listing anomaly checks**: zero-byte files, missing hashes, unexpected paths, mismatched dir/filename dates, expected index.html/json counts per day, missing-day detection, free-space estimate via sampled decompression ratio (2x remote + 2x decompressed) — `02-list.sh:23-152`.
- **Download verification**: `rclone check` against remote, full path/size/mtime diff against listing.json, `bzip2 -t` integrity on every file — `03-download.sh:47-80`. `LOCAL_COPY_DIR` seed is explicitly "never trusted," re-verified with `--checksum` — `03-download.sh:10-39`.
- **Prepare verification**: index.html count check, decompressed file count check, sha256 manifest, mtime diff vs listing, and a JSON-validity check robust to truncated/concatenated files via `input_filename` matching — `04-prepare.sh:26-101`.
- **Archive verification**: xz integrity (`xz -t`), mode/owner/sort-order checks on tar headers, entry count match, name/size/mtime diff vs stage, and full content-hash re-extraction compared to the manifest — `05-archive.sh:29-92`.
- **Upload verification**: refuses overwrite if remote already has the name with a different SHA-1 (`06-upload.sh:19-24`); uses `rclone copyto --immutable`; re-verifies remote size/SHA-1/mtime, retries public URL up to 6x, checks headers and downloads the public copy to re-hash — `06-upload.sh:32-107`.
- **Delete safety (the only destructive step)**: path-shape guard before deleting (`07-delete.sh:7-10`), refuses if remote has unknown files not in listing.json, dry-run count must match remaining count before real delete, `--max-delete` capped to exact count, hard_delete forced false everywhere (`common.sh:74`), post-delete empties verified, archive re-verified unchanged by SHA-1 — `07-delete.sh:16-79`.
- **Completion**: re-verifies archive SHA-1 and that the year dir is still empty (catches a scheduled `data-index` re-upload race) before writing `COMPLETE` — `08-complete.sh:7-31`.
- Env pinning (`TZ`, `LC_ALL`, tar/xz/bzip2 options) for reproducibility — `common.sh:90-95`.

## Problems found (data.everef.net audit, 2026-10-21)

Affected datasets: `industry-systems`, `sovereignty-campaigns`, `sovereignty-map`,
`sovereignty-structures`, `system-jumps`, `system-kills` (likely more not yet logged).
`system-jumps` was missing from the first version of this list, but it has both problems
too (in rollup scope, live `history/2022/` still present alongside `system-jumps-2022.tar.bz2`,
checked 2026-10-22).

- **Wrong format**: 2017/2018/2019/2021/2022 archives are `.tar.bz2`, not `.tar.xz`. Old
  rollup (pre-`scripts/rollup` rewrite) must have used bzip2, no migration done since.
- **2022 leftover**: for each dataset, `history/2022/` still exists on the remote
  alongside `*-2022.tar.bz2` — delete step (`07-delete.sh`) never ran or never completed
  for 2022, so raw data and archive both sit on the remote.
- 2020 not flagged — only year missing from the warnings, so presumably fine already.

## Goals

1. Convert the old `.tar.bz2` archives into `.tar.xz` archives matching the exact format
   `scripts/rollup` produces (see "Archive format" above — GNU tar, sorted entries,
   `--owner=0 --group=0 --numeric-owner --mode='a=r,u+w'`, xz -6, per-file sha256 manifest,
   whole-archive sha256/sha1).
2. For each affected 2022 dataset, merge the data sitting in `history/2022/` on the
   remote with the data already inside `*-2022.tar.bz2` (that archive was apparently
   created by mistake, so the live `history/2022/` dir and the archive's contents need
   to be reconciled into one correct `2022.tar.xz`), then clear `history/2022/`.

### Working constraints

- Downloads/extraction/inspection happen under `/tmp` freely, but anything copied in
  for inspection (per the environment's untrusted-archive rule) goes in its own fresh
  directory, kept separate from any scripts.
- End deliverable: a series of **bash scripts**, reviewed by the user, not auto-run —
  no download/convert/upload is executed by the agent as a side effect of "fixing"
  things. The user runs each step and does the final upload themselves.
- Before writing the conversion scripts: fan out subagents to download and inspect the
  `.tar.bz2` archives (all affected datasets/years), group them by shared
  format/structure if they differ, and document what's found here.
- **Script design**: as simple as possible, with built-in safety checks — same spirit
  as `scripts/rollup` (see logging format there, e.g. `common.sh` log helpers). Ideally
  one script per format-group (if archives group cleanly), parameterized by dataset +
  year: build the new `.tar.xz`, verify it, and on any problem log it and stop (no
  partial/silent continuation). No download/convert/upload runs automatically as a
  side effect of this work — scripts are reviewed, then run, then uploaded by the user.

## Old `.tar.bz2` archive inspection findings

### sovereignty-campaigns (2017/2018/2019/2021/2022)

- Genuine bzip2 (`BZh9` magic), POSIX ustar tar headers (no pax), owner/group `kenn/kenn`,
  file mode `-rw-rw-r--`, dir mode `drwxrwxr-x`. Entries **not sorted** (directory-
  iteration order at build time).
- Single top-level dir per archive: `sovereignty-campaigns-<year>/`, files flat inside,
  **plain uncompressed JSON**, named `sovereigntycampaigns_<YYYY-MM-DDTHH:MM:SS>.json`
  (no hyphen between words, unlike current live naming).
- No manifest/checksum/index bundled inside any archive.
- Entry counts vary a lot by year — cadence changed over time (hourly in 2017/2018/2022,
  ~2hr in 2021), 2019 is a short stub (only Jan 1–9), not a 365/366-per-day pattern.
  Not an anomaly, just variable collection frequency historically.
- **2022 archive vs. live `history/2022/`**: disjoint and contiguous, not
  overlapping/duplicated. Archive covers 2022-01-01 through 2022-12-16 15:45:08Z; live
  `history/2022/` starts 2022-12-16 16:16:54Z (~31 min later) and runs through
  2022-12-31. Live files use the newer naming/compression
  (`sovereignty-campaigns-2022-12-16_16-16-54.json.bz2`, individually bz2'd, hyphenated)
  vs. archive's internal naming (`sovereigntycampaigns_2022-12-16T15:45:08.json`,
  uncompressed) — merge logic must handle both naming schemes.
- Inspection artifacts left at `/tmp/inspect-sovereignty-campaigns/` (5 archives, a 2019
  extraction sample, 2022 index/day-listing samples).

### sovereignty-map (2017/2018/2019/2021/2022)

- Genuine bzip2 (`BZh9`). **GNU tar** format (magic `"ustar  \0"` at offset 257, not
  POSIX), owner/group `kenn/kenn`, file mode `rw-rw-r--`, dir `rwxrwxr-x`, no PaxHeader
  entries. Entries **not sorted** (raw readdir order).
- Single top-level dir `sovereignty-map-<year>/`, flat files inside, **plain
  uncompressed JSON**, named `sovereigntymap_<YYYY-MM-DD>T<HH:MM:SS>.json` (no hyphen,
  matches sovereignty-campaigns' pattern). Content: flat JSON array of `{"system_id":N}`.
- No manifest/checksum/index bundled.
- Entry counts vary by year/collection gaps (2017 partial-Dec-only 777 files, 2018:
  7905, 2019 only Jan 1–9 (204), 2021: 4397, 2022: 8334) — same "variable historical
  cadence, not an anomaly" pattern as sovereignty-campaigns.
- **All 5 years identical format** — no structural differences between years.
- **2022 archive vs. live `history/2022/`**: clean, disjoint, non-overlapping handoff.
  Archive ends 2022-12-16T15:45:03 (old naming, uncompressed); live `history/2022/`
  covers only 2022-12-16 (from 16:16:34, likely cutover timestamp) through 12-31, new
  naming (`sovereignty-map-2022-12-16_16-16-34.json.bz2`, bz2'd, settling into :40
  cadence). No dedup needed when merging — straight concatenation/rollup of live files
  into the 2022 archive.
- Inspection artifacts left at `/tmp/inspect-sovereignty-map/` (archives + samples;
  extracted .tar copies cleaned up).

### system-jumps and system-kills (2017/2018/2019/2021/2022)

- Genuine bzip2. **GNU tar** (magic `"ustar  \0"` at offset 257), owner/group
  `kenn/kenn`, mode `rw-r--r--`/dir `rwxr-xr-x`, no pax headers, entries **not sorted**.
  All 10 archives (both datasets x 5 years) built same run, mtimes cluster
  2022-12-17 04:26-04:38.
- Single top-level dir per archive (`system-jumps-<year>/`, `system-kills-<year>/`),
  flat files inside, **plain uncompressed JSON**, named
  `universe_system_jumps_<ISO8601>.json` / `universe_system_kills_<ISO8601>.json` (old
  scheme, no hyphen — unlike live `system-jumps-2022-12-16_16-15-16.json.bz2`).
  Content: JSON arrays, jumps `{"system_id":N,"ship_jumps":N}`, kills
  `{"system_id":N,"ship_kills":N,"npc_kills":N,"pod_kills":N}`.
- No manifest/checksum/index bundled in any archive.
- Entry counts vary by year same as other datasets (2017 partial ~32 days, 2019 an
  8-day stub, hourly-ish cadence with ~25min offset in later years) — not an anomaly.
- **system-jumps and system-kills are structurally identical** to each other (and to
  sovereignty-campaigns/-map) apart from field names — same format, same build run.
- **system-kills 2022 archive vs. live `history/2022/`**: same clean disjoint-cutover
  pattern as sovereignty-campaigns/-map. Archive covers 2022-01-01 through 2022-12-16
  15:45:05 (old naming, uncompressed); live `history/2022/` covers 2022-12-16 16:15:23
  through 2022-12-31 (new naming, bz2'd per-file). 30-min gap, no overlap/duplication.
  Merge logic must handle both naming/compression schemes.
- **system-jumps 2022 archive vs. live `history/2022/`**: same pattern as system-kills.
  Archive ends at `2022-12-16_15-45-03`; live `history/2022/` has 16 day dirs
  (2022-12-16 through 2022-12-31, 369 files) starting at `2022-12-16_16-15-16`. No
  overlap. Checked by a `rebuild-archive.sh system-jumps 2022` run (2026-10-22), not by
  hand like the other datasets.
- Inspection artifacts left at `/tmp/inspect-system-jumps/` and `/tmp/inspect-system-kills/`.

### industry-systems (2017/2018/2019/2021/2022)

- Genuine bzip2 (`BZh9`). **GNU tar** (ustar magic, two trailing spaces, no PAX/long-name
  entries), owner/group `kenn/kenn`, dir mode `755`, file mode `644`, entries **not
  sorted**. All mtimes `2022-12-17` (build day) — original snapshot time only in
  filename, not tar mtime.
- Single top-level dir `industry-systems-<year>/`, flat files inside, **plain
  uncompressed JSON**, named `systemcost_<ISO8601>.json`. Content:
  `{"cost_indices":[...], "solar_system_id":N}` — same schema live API uses today.
  Live format differs: `industry-systems-<date>_<time>.json.bz2` (hyphenated,
  per-file bz2).
- No manifest/checksum/index bundled.
- **None of the 5 "year" archives is a full calendar year** — 2017: Nov 29-Dec 31 only
  (33 days); 2018: 333/365 days present (32 missing); 2019: only Jan 1-9 (9 days); 2021:
  only Jul 1-Dec 31 (second half only); 2022: Jan 1-Dec 16 (350 days). Cadence varies by
  era (hourly in 2017/2018, ~4hr in 2021/2022). Migration tooling must not assume
  365/366-day completeness.
- **2022 archive vs. live `history/2022/`**: same clean day-level split as other
  datasets. Archive's 2022-12-16 entries stop at 15:45:04 (old naming, uncompressed);
  live `history/2022/` 2022-12-16 entries start at 16:52:34 (new naming, bz2'd) through
  year-end. Contiguous, disjoint, no overlap — straightforward concatenation to merge,
  not dedup.
- Inspection artifacts at `/tmp/inspect-industry-systems/` (archives, `extracted-<year>/`,
  `live-sample/`, `2022-index.json`).

### sovereignty-structures (2017/2018/2019/2021/2022)

- Genuine bzip2 (`BZh9`). **POSIX ustar** (magic `ustar\0` version `00` — not GNU, no
  pax headers), owner/group `kenn/kenn`, dir `rwxrwxr-x`, file `rw-rw-r--`, entries
  **not sorted**. All 5 built in one batch run 2022-12-16/17.
- Single top-level dir `sovereignty-structures-<year>/`, flat files inside, **plain
  uncompressed JSON** (array), named
  `sovereigntystructures_<YYYY-MM-DDTHH:MM:SS>.json` (no hyphen). Live naming differs:
  `sovereignty-structures-2022-12-31_00-40-08.json.bz2` (hyphenated, per-file bz2'd).
- No manifest/checksum/index bundled.
- **No year is complete**: 2017 ~24 days (Dec only), 2018 most of year hourly, 2019
  only Jan 1-9 (9 days), 2021 only Jul 1-Dec 31 (second half), 2022 Jan 1-Dec 16 (350
  days). Historical scrape gaps, not migration artifacts.
- **2022 archive vs. live `history/2022/`**: same clean mid-day split as all other
  datasets. Archive's 2022-12-16 entries run 02:45-15:45:06 (old naming, uncompressed);
  live dir's 2022-12-16 entries start 16:16:43 onward (new naming, bz2'd) through
  12-31. Contiguous, no overlap/gap.

## Cross-dataset summary

All 6 affected datasets (industry-systems, sovereignty-campaigns, sovereignty-map,
sovereignty-structures, system-jumps, system-kills) share the same migration shape:

- Container: bzip2-compressed tar, single top-level `<dataset>-<year>/` dir, flat
  uncompressed `.json` files inside, no bundled manifest/checksum. Two tar sub-flavors
  seen: **GNU tar** (industry-systems, sovereignty-map, system-jumps, system-kills) vs
  **POSIX ustar** (sovereignty-campaigns, sovereignty-structures) — doesn't affect
  conversion (target format is `scripts/rollup`'s own GNU-tar spec regardless), but
  note it's not uniform in the *old* archives.
- Old internal naming has no hyphen between dataset-name words
  (`sovereigntystructures_...`, `systemcost_...`, `universe_system_kills_...`) and
  files are uncompressed; current live/rollup format uses hyphenated names with
  per-file `.json.bz2` or `.json` (post `04-prepare.sh` decompression). Decision:
  `rebuild-archive.sh` renames old-archive files to the current convention
  (`<dataset>-<date>_<HH-MM-SS>.json`) rather than preserving the old scheme — see
  "Script: rebuild-archive.sh" below.
- Every "year" archive is **partial** in some way (gaps, half-years, short stubs) —
  never assume 365/366 complete days.
- All 6 datasets have the "2022 leftover" problem (industry-systems,
  sovereignty-campaigns, sovereignty-map, sovereignty-structures, system-jumps,
  system-kills), and all show the **same clean, contiguous, non-overlapping split**: the
  old `*-2022.tar.bz2` archive covers Jan 1 up to some hour on Dec 16, and live
  `history/2022/` picks up within ~30-90 min after that and runs through Dec 31. No dedup
  needed — merging is concatenation across the naming/compression boundary. When
  clearing `history/2022/` afterwards, include system-jumps.

## Script: rebuild-archive.sh

`rebuild-archive.sh <dataset> <year>` — one script covers both goals (no per-dataset
grouping needed; the only real variable across datasets was internal filename prefix,
handled by a generic `_<date>T<time>.json` regex, not per-dataset logic).

- Downloads the old `<dataset>-<year>.tar.bz2`, checks its size and MD5 against the
  `size`/`etag` in `<dataset>/history/index.json` (data-index's `etag` is the object's MD5,
  confirmed on real downloads up to 271 MB), extracts it (a corrupt bzip2 stream is caught
  by `tar -xjf` itself, so there's no separate `bzip2 -t` step), checks the extracted
  filesystem layout is exactly one `<dataset>-<year>/` dir of flat `.json` files (aborts on
  anything else — only one `tar` pass over the archive this way, instead of a separate
  listing pass before extraction), and copies each file into
  `stage/<dataset>/history/<year>/<date>/`
  by the date embedded in its filename (aborts if a filename doesn't match, or its date
  isn't in `<year>`). Each file is also renamed from its old internal scheme
  (`sovereigntycampaigns_2019-01-01T00:20:09.json`) to the current convention
  (`sovereignty-campaigns-2019-01-01_00-20-09.json`), matching what live/rollup produces.
- **Saved error responses**: the old scraper sometimes wrote an HTTP error body in place of a
  snapshot. Before staging, old-archive files whose whole body is a known error response are
  moved to `removed/` and listed in `removed.tsv`, so they're left out of the new archive:
  nginx `502 Bad Gateway` / `504 Gateway Time-out` pages (matched on `<html>` start plus the
  exact `<title>` and `<h1>`), and these JSON bodies, compared in full after `jq -c` (so
  spacing doesn't matter): `{"error":"Not found"}`, `{"error":"No reply within 10 seconds"}`,
  `{"error":"Timeout contacting tranquility","timeout":10}`,
  `{"error":"The datasource tranquility is temporarily unavailable"}`,
  `{"error":"timeout contacting endpoint"}`, and the full ESI "exceeded the error limit" and
  "You have been banned" messages (exact text in `ERROR_JSON_BODIES`). Any other HTML file or
  `{"error": ...}` object aborts the run and lists the file, so a new kind of error body is
  reviewed and added to `ERROR_PAGE_TITLES`/`ERROR_JSON_BODIES` by hand, never removed by guess.
- Checks `https://data.everef.net/<dataset>/history/<year>/index.json`. 404 → no
  merge needed (e.g. any non-2022 year). 200 → live leftover, handled strictly, since
  whatever is recorded here is what later gets deleted from the remote. Any of these
  aborts the run:
  - **Listing**: the year index or a day index has the wrong `path`; files directly
    under `history/<year>/`; a day dir that isn't a real `<year>-MM-DD` date, or is
    listed twice; subdirectories inside a day; a file name not matching
    `<dataset>-<day>_HH-MM-SS[.vN].json.bz2` for its own day (same rule as
    `scripts/rollup/02-list.sh`); a `url` that isn't exactly the file's path on
    data.everef.net; missing or malformed `size`, `etag` or `last_modified`.
  - **Downloads**: size or MD5 not matching the listing, or `bzip2 -t` failing.
  - **Overlap**: a live file that doesn't sort after the last old-archive file (names are
    `<dataset>-<date>_<HH-MM-SS>`, so this is a time order check), or that would
    overwrite a staged file. Overlap was ruled out beforehand for every affected dataset
    (by hand, and for system-jumps by a script run), so this means something changed.
  - **Archive**: any live file missing from the new archive, or with a different sha256
    than when it was staged.
  - **Listing changed**: the year and day indexes are fetched again at the end of the run
    and compared to the ones used (entries sorted, so only real changes count).
- **Live leftover record**, written to the scratch dir:
  - `live-files.tsv`: one row per live file, tab-separated: remote path (from the bucket
    root), size, MD5, SHA-1 (both of the `.json.bz2` as stored on the remote),
    `last_modified`, path in the new archive, sha256 (of the decompressed file).
  - `live-files.txt`: just the remote paths, for `rclone --files-from-raw`.
  - `live/`: the year and day `index.json` files exactly as served, `live-recheck/` the
    end-of-run re-fetch, `history-index.json` the dataset's history index.
- Validates every staged file holds exactly one JSON value, hashes the stage
  (`sha256sum`), then builds the `.tar.xz` with the **exact** `tar`/`xz` invocation
  `scripts/rollup/05-archive.sh` uses (`--owner=0 --group=0 --numeric-owner
  --mode='a=r,u+w'`, `LC_ALL=C` sorted entries, `-cJf`), and runs the same
  verification suite: `xz -t`, mode/owner/sort checks, entry count, and a full
  content-hash re-extraction against the stage manifest. Pins the environment the same
  way as `scripts/rollup/common.sh` (`TZ=UTC`, `LC_ALL=C`, and `TAR_OPTIONS`, `XZ_OPT`,
  `XZ_DEFAULTS`, `BZIP`, `BZIP2` cleared), so an operator's shell settings can't change
  the tar format or xz preset.
- Logging mirrors `scripts/rollup`'s format exactly (🚀/🔵/✅/🔴, tee'd to a log file
  under the scratch dir). Any failed check logs to stderr and `exit 1`s immediately —
  no partial/silent continuation.
- Uses its own scratch dir (`/tmp/everef-fix-old-archives/<dataset>-<year>`, refuses to
  run if it already exists) and its own lock file — fully separate from
  `scripts/rollup`'s scratch/lock, no interference with live rollups.
- **Never uploads, deletes, or writes to the remote.** Leaves
  `<dataset>-<year>.tar.xz`, `<dataset>-<year>.sha256`, `archive.sha256`, `archive.sha1`
  (plus `removed.tsv`, and `live-files.tsv`/`live-files.txt` for a live leftover) in the
  scratch dir for manual review; upload is a separate manual step by the user.
- Test runs so far (2026-10-22) were on a sample of small archives only, not every
  dataset/year — each run still needs its own review before upload.
- **Leftover-file mtimes**: preserved via the live day `index.json`'s `last_modified`
  metadata field (`touch -d` after decompression) — the same field rollup's own mtime
  check (`04-prepare.sh`) trusts, not anything derived from JSON content. Missing/null
  `last_modified` aborts the run rather than silently falling back to download time.
  Old-archive-derived files keep whatever mtime the old `.tar.bz2` stored (its own
  build-day mtime per the inspection findings, not the true snapshot time) — this
  script doesn't change that, since snapshot time is only ever recoverable from the
  filename for those, not a trustworthy embedded mtime.

## Clearing a live leftover

Only after the new `.tar.xz` is uploaded and verified. Run from the run's scratch dir, with
the rclone `everef` remote set up the same way as `scripts/rollup/common.sh` (env vars from
`rollup.env`, `RCLONE_CONFIG=/dev/null`, `RCLONE_CONFIG_EVEREF_TYPE=b2`,
`RCLONE_CONFIG_EVEREF_HARD_DELETE=false`), and `DATASET`, `YEAR`, `BUCKET` set.

1. The remote year dir must hold exactly the archived files (path, size, SHA-1), apart
   from `index.html`/`index.json`. This must print nothing:
   ```bash
   rclone lsjson -R --hash --files-only "everef:$BUCKET/$DATASET/history/$YEAR" \
     | jq -r --arg p "$DATASET/history/$YEAR/" '.[]
         | select(.Name | test("^index\\.(html|json)$") | not)
         | [$p + .Path, .Size, .Hashes.sha1] | map(tostring) | @tsv' \
     | LC_ALL=C sort \
     | diff - <(cut -f1,2,4 live-files.tsv)
   ```
2. Delete only the listed files, dry run first, capped at the listed count:
   ```bash
   rclone delete "everef:$BUCKET" --files-from-raw live-files.txt \
     --max-delete "$(wc -l <live-files.txt)" --dry-run
   ```
   then the same without `--dry-run`. Never delete the year dir as a whole: a file that
   isn't in `live-files.txt` isn't in the archive, and must stay to be looked at.
3. Only `index.html`/`index.json` files should remain under `history/$YEAR/`.

