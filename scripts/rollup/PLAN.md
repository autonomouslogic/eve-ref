# Yearly history rollup: implementation plan

Status: **planning**. No scripts exist yet. This file holds implementation notes for review; the operator
runbook lives in [README.md](README.md).

## Goal

Replace a full year of small history files on data.everef.net with one archive:

```
<archive>/history/<year>/<year>-MM-DD/<archive>-<year>-MM-DD_HH-mm-ss.json.bz2   (many files, many dirs)
        ->  <archive>/history/<archive>-<year>.tar.xz                             (one file)
```

`<archive>` is the dataset folder name (`incursions`, `skinr-listings`, ...), and the scripts take it and the
year as arguments. The process is manual: the operator runs the steps one by one. Each step checks the
completion markers of earlier steps and its own (no accidental reruns), verifies its own output, and only
then writes its marker.

## Archive format (2023 onward)

- **Paths exactly as on the data site**: `<archive>/history/<year>/<YYYY-MM-DD>/<name>.json`. Extracting several
  archives into one directory recreates the data site layout.
- **Files only, sorted**: the tar holds regular file entries only, with no directory entries. They are in
  byte-wise (`LC_ALL=C`) sorted order of their full path. The paths are zero-padded dates and timestamps, so
  this is the same as sorting by filename within each day.
- **Only name and mtime carry information**. Every other header field is constant: uid/gid `0`, empty user and
  group names (`--numeric-owner`), mode `0644`. Each file's mtime is its original upload time on the data site,
  as rclone reports it.
- `.bz2` is stripped from every data file. `index.json` files are **kept**, `index.html` files removed.
- Compression is whatever `tar -J` does by default (xz default preset `-6`). No `XZ_OPT`.
- Archive object mtime: `<year>-12-31T23:59:59Z`.
- No checksum file is published. Checksums exist only in the scratch dir, for verification.

Tar command, checked locally with GNU tar 1.35 and xz 5.4.5:

```bash
cd "$STAGE"   # contains <archive>/history/<year>/...
find "<archive>/history/<year>" -type f -print0 \
	| LC_ALL=C sort -z \
	| tar --null --no-recursion -T - \
		--owner=0 --group=0 --numeric-owner --mode='a=r,u+w' \
		-cJf "$ARCHIVE"
```

The resulting header contains: name, `0000644`, uid `0`, gid `0`, size, mtime, checksum, type `0`, GNU magic,
empty uname and gname. GNU format (the default) does not store atime or ctime. Avoid `--format=posix`, which
would add them as pax headers.

## Reference: incursions 2022 (manual rollup, 2026-10-09)

Done by hand through an rclone mount at `/tmp/data.everef.net2`, before this plan. **It does not follow the
format above and will not be fixed**. It is kept here as a reference only.

Commands used: copy `incursions/history/2022` out of the mount with `cp --preserve=timestamps`, compare
sizes with `du -bs`, delete `index.html`, `bunzip2` everything, `tar -cJf` from `/tmp` with root `incursions`,
`touch -d "2022-12-31T23:59:59Z"`, copy back into the mount, check via `curl --head`, `wget` and `tar -t`,
then `rm -Rv` the year dir through the mount.

| Property             | Value                                                                              |
|----------------------|------------------------------------------------------------------------------------|
| Size                 | 205,308 bytes (uncompressed tar: 9,625,600 bytes)                                  |
| MD5 (= HTTP ETag)    | `44cfbdbeebe9df41bc9f81fa2c16bc86`                                                 |
| SHA-256              | `f376eb4e7eb3c8d4f4f2fe234f594f108b9451e9dc238163d677c643d6c78580`                 |
| xz                   | 1 stream, 1 block, CRC64, `--lzma2=dict=8MiB` (default preset)                     |
| Entries              | 4,429: 18 dirs, 4,394 data files, 17 `index.json`                                  |
| Day range            | 2022-12-16 to 2022-12-31                                                           |

Differences from the 2023+ format:

- the root is `incursions/2022/...`, missing `history/`;
- it has `kenn/kenn` owner names;
- the entries are unsorted;
- it has directory entries.

Useful facts it confirmed:

- The live object has `x-amz-meta-src_last_modified_millis: 1672531199000`, and both the HTTP `Last-Modified` and
  `index.json` show `2022-12-31T23:59:59Z`. `DataIndex` reads that field (`S3Adapter.headLastModified`,
  `S3HeaderNames.SRC_LAST_MODIFIED_MILLIS`). The rclone `b2` backend writes and reads it natively.
- The data files' mtimes came out as upload time, a few seconds after the timestamp in the file name.
  `bunzip2` keeps mtimes.
- After the delete, `https://data.everef.net/incursions/history/2022/` returns 404 with
  `x-amz-delete-marker: true`. The rclone delete only hides files, and the bucket lifecycle rules give the undo
  window.

## Scale

- `incursions/history/2023`: 288 files a day (5-minute cadence), so about 105k data files plus about 732 index
  files. One sampled day is about 109 KB of bz2, so roughly 40 MB of bz2 for the year.
- `skinr-listings` files are about 200-250 KB of bz2 each. Decompressed data for a year can be several GB, and
  `/tmp` may be a tmpfs (RAM), so preflight checks free space.

## Scope: which datasets

Candidates are the `ArchivePathFactories` datasets with a `history/<year>/<date>/` layout and a `.json.bz2` or
`.vN.json.bz2` suffix, whose old files the code never reads back: `incursions`, `skinr-listings`,
`skinr-details`, `sovereignty-*`, `industry-*`, `system-jumps`, `system-kills`, `warzone*`, `insurance-prices`,
`universe-structures`, `faction-warfare-*`, `military-campaigns`, `markets-prices`, `freelance-jobs`,
`structures` (`DataUtil` only downloads the latest file). Verify each layout before that dataset's first
rollup, and grep `src/main` for consumers of its factory constant.

**Out of scope**, so the scripts refuse these:

- `market-orders` and `market-history`: read back by `HistoricalOrdersRegionTypeSource` and `MarketHistoryUtil`;
- `killmails`, `public-contracts`, `esi-scrape`, `hoboleaks-sde`, `reference-data`: already archives, or
  have different layouts;
- `fuzzwork/ordersets`, `ccp/*`;
- the current UTC year or later. Finished years never change, which is the basis for skipping a re-list before
  delete.

## rclone / B2

- The operator installs and configures rclone. Development testing is done by the operator too.
- **One rclone remote, native `b2` backend**, with `hard_delete` at its default (`false`). It is hard-coded in
  `common.sh` as `REMOTE=everef-data:data-everef-net-425eb511` (name to be confirmed; bucket from
  `local.env`).
- Delete with `rclone delete` (it hides files). Undo is the bucket lifecycle window. Never use `rclone purge`,
  `rclone cleanup`, `backend cleanup-hidden` or `--b2-hard-delete`: on b2 these remove old versions too.
- Use `--fast-list` on listings to keep Class C transactions down.

## Scratch directory and markers

Hard-coded: `SCRATCH=/tmp/everef-rollup/<archive>-<year>`. The scripts never delete this dir. The operator
clears it.

```
/tmp/everef-rollup/<archive>-<year>/
  10-preflight.done ... 80-index.done   completion markers (UTC timestamp + key facts, e.g. counts, sha256)
  COMPLETE                              written last, by 90-complete.sh
  listing.json                          rclone lsjson -R --hash --files-only of the remote year dir
  download/<archive>/history/<year>/    untouched rclone copy (.bz2, index.html, index.json)
  stage/<archive>/history/<year>/       working copy: no index.html, bz2 decompressed (tar root = stage/)
  expected.sha256                       sha256 of each staged file, computed from download/ (bzcat for .bz2)
  stage.sha256                          sha256 of each file in stage/
  <archive>-<year>.tar.xz
  archive.sha256 / archive.sha1 / archive.md5
  verify/built/                         extraction of the built archive
  verify/public/                        archive downloaded back from the public URL
  log/<step>-<UTC timestamp>.log        tee of each run
```

Marker rules, enforced by `common.sh` at the start of every step:

1. If `COMPLETE` exists, abort. The rollup is done.
2. If this step's own `<step>.done` exists, abort ("already completed, refusing to rerun").
3. If any earlier step's `.done` is missing, abort.
4. Write `<step>.done` only after every verification in the step has passed.

A step that fails partway leaves no marker. Steps that produce local output (`stage/`, the archive,
`verify/`) refuse to start if that output already exists, so the operator removes the partial output by hand
before retrying. `30-download` is the exception: `rclone copy` resumes. Remote steps handle partial runs
themselves, as described in each step.

## Scripts

Planned files in `scripts/rollup/`. Each is `bash` with `set -euo pipefail` and `export TZ=UTC LC_ALL=C`, takes
`<archive> <year>`, validates both (`^[a-z0-9-]+$` plus the scope list, `^20[0-9]{2}$` plus earlier than the
current UTC year), and tees its output to `log/`.

| Script            | Remote       | What it does                                                              |
|-------------------|--------------|---------------------------------------------------------------------------|
| `common.sh`       | n/a          | Constants (`REMOTE`, `SCRATCH`, public URL), arg checks, markers, helpers |
| `10-preflight.sh` | read         | Tools, remote access, year dir present, archive absent, free space in `/tmp` |
| `20-list.sh`      | read         | `listing.json` plus anomaly report                                         |
| `30-download.sh`  | read         | `rclone copy` and `rclone check`, then verify against `listing.json`      |
| `40-prepare.sh`   | none         | Build `stage/`, decompress, hash-verify against the originals             |
| `50-archive.sh`   | none         | Build tar.xz, verify headers, order and contents, set mtime               |
| `60-upload.sh`    | write        | Upload, verify via B2 and the public URL                                  |
| `70-delete.sh`    | **delete**   | Dry-run preview, typed confirmation, `rclone delete`, verify empty        |
| `80-index.sh`     | read         | After `data-index` has run: verify `history/index.json`, no orphan index files |
| `90-complete.sh`  | read         | Final checks, print README log row, write `COMPLETE`                      |

### 10-preflight

- Tools present: `rclone` (version printed), GNU `tar`, `xz`, `bzip2`, `jq`, `sha256sum`, `sha1sum`,
  `md5sum`, `curl`, `find`, `sort`.
- `rclone lsf $REMOTE/<archive>/history/` works and contains `<year>/`. `<archive>-<year>.tar.xz` exists
  neither in that listing nor at the public URL (HTTP 404).
- `rclone size` of the year dir. Free space on `/tmp` must be at least 2x that size plus an estimate of the
  decompressed size (from a sampled file's bz2 ratio) times 3, to cover stage, tar and verification
  extraction.

### 20-list

- `rclone lsjson -R --hash --files-only --fast-list $REMOTE/<archive>/history/<year>` writes `listing.json`.
- Abort on anomalies:
  - a non-index name that does not match `^<year>-MM-DD/<archive>-<that date>_HH-mm-ss(\.v[0-9]+)?\.json\.bz2$`;
  - a date outside `<year>`;
  - other names besides `index.html` and `index.json`, which are expected once per dir plus the year root;
  - zero-byte files;
  - files without a SHA-1 hash.
- Report: file count by kind, total bytes, files per day (min, max, missing days), first and last timestamp.

### 30-download

- `rclone copy --fast-list $REMOTE/<archive>/history/<year> download/<archive>/history/<year>`. rclone
  verifies SHA-1 on each transfer.
- Verify:
  - `rclone check $REMOTE/... download/... --fast-list`: 0 differences in either direction (SHA-1);
  - the local file list and sizes equal `listing.json` exactly;
  - local mtimes equal the `ModTime` values in `listing.json` to the second;
  - `bzip2 -t` passes on every `.bz2`.

### 40-prepare

- `cp -a download/<archive>/history/<year> stage/<archive>/history/<year>`.
- Delete `index.html` files. The count must equal the `index.html` count in `listing.json`.
- `expected.sha256` comes from `download/`: `bzcat | sha256sum` for each `.bz2` (recorded under the name
  without `.bz2`), plain `sha256sum` for `index.json`.
- `find stage -name '*.bz2' -print0 | xargs -0 -P"$(nproc)" -n 500 bunzip2`.
- Verify:
  - no `.bz2` left;
  - the file count equals the data plus `index.json` count from `listing.json`;
  - `stage.sha256` equals `expected.sha256`;
  - mtimes match `listing.json`;
  - `jq empty` passes on every file (valid JSON).

### 50-archive

- Build with the tar command above. It writes to a temp name and renames on success.
- `touch -d "<year>-12-31T23:59:59Z"` on the archive.
- Verify:
  - `xz -t`;
  - `tar -tv --numeric-owner --full-time` shows only regular files, all `0/0` and `-rw-r--r--`, in
    `LC_ALL=C` sorted order, with names, sizes and mtimes equal to `stage/` (no extra, none missing);
  - no uname or gname in the raw headers (`xz -dc | strings` has no user or host names);
  - `tar --compare -C stage` reports no differences;
  - extract into empty `verify/built/`, then `sha256sum -c expected.sha256` passes.
- Write `archive.sha256`, `archive.sha1` and `archive.md5`.

### 60-upload

- If the remote object already exists with the same SHA-1 (an earlier partial run), skip the upload and go
  straight to verification. If it exists with a different hash, abort.
- `rclone copyto --immutable <archive>-<year>.tar.xz $REMOTE/<archive>/history/<archive>-<year>.tar.xz`.
- Verify:
  - `rclone lsjson --hash`: size, SHA-1 equal to `archive.sha1`, ModTime `<year>-12-31T23:59:59Z`;
  - `curl -I` on the public URL: 200, `content-length`, `last-modified` at `<year>-12-31 23:59:59 GMT`,
    `x-amz-meta-src_last_modified_millis`, and `etag` equal to `archive.md5` (single-part uploads only);
  - download into `verify/public/` and compare the sha256 with `archive.sha256`.

### 70-delete

- The path is built from the validated args and must match `^[a-z0-9-]+/history/20[0-9]{2}$`. It is never a
  bare `history` dir.
- `rclone delete --dry-run --fast-list` on `$REMOTE/<archive>/history/<year>`: show the count, which must
  equal the `listing.json` count. This is a sanity check on the path, not a change check, because finished years
  don't change.
- The operator types `<archive>-<year>` to confirm.
- `rclone delete --fast-list --max-delete <listing count> $REMOTE/<archive>/history/<year>`. Safe to rerun if
  it fails partway.
- Verify:
  - `rclone lsf -R` of the year dir is empty;
  - a few old public URLs return 404;
  - the archive is still present with an unchanged SHA-1.

### 80-index

- Prerequisite, done outside the script: `data-index` has run, either the scheduled run or a manual one with
  `DATA_INDEX_PREFIX=<archive>/history`. Today `make docker-data-index` cannot pass a prefix.
- Verify:
  - `https://data.everef.net/<archive>/history/index.json` lists `<archive>-<year>.tar.xz` with the right
    size and `last_modified`, and no `<year>` directory;
  - `rclone lsf -R $REMOTE/<archive>/history/<year>` is still empty. A `data-index` run that listed the
    bucket before the delete could have re-uploaded `<year>/index.{html,json}` afterwards. If any appear, the
    script lists them and stops. The operator deletes them and reruns.

### 90-complete

- Recheck that the remote archive's SHA-1 equals `archive.sha1` and that the year dir is empty.
- Print a row for the README rollup log: date, archive, year, data file count, archive size, sha256.
- Write `COMPLETE`. Leave the scratch dir for the operator to clear.

## Development testing (operator)

Things to confirm before the first real run:

1. `rclone copy` from b2 sets local mtimes from `src_last_modified_millis`. For files where the Java app did
   not set it, rclone should fall back to upload time. Compare a downloaded day against that day's
   `index.json` `last_modified`.
2. `rclone copyto` of the archive sets `src_last_modified_millis` from the local mtime, so the public
   `last-modified` and the index show `<year>-12-31T23:59:59Z`.
3. `rclone lsjson --hash` returns SHA-1 for history files uploaded by the Java app through the S3 API.
4. `rclone delete` produces hide markers (`x-amz-delete-marker: true` on the old URLs).
5. End to end on a small, throwaway input before 2023 incursions. One option: make `REMOTE` and the public
   base URL overridable for testing and point them at a test prefix (e.g. `rollup-test/`) holding a
   server-side copy of a few days.

## Undo

- Before `70-delete`: only the archive was added, and deleting it restores the old state.
- After `70-delete`: restore the hidden versions within the bucket lifecycle window (B2 web UI, or rclone with
  `--b2-version-at` set before the delete). Nothing is kept locally after completion.

## Open questions

1. rclone remote name to hard-code (placeholder `everef-data`).
2. Should `80-index` trigger `data-index` itself, or keep relying on the scheduled run or a manual one?
3. Test approach for item 5 above: a test prefix in the live bucket (briefly visible on the site), or a
   separate bucket?
4. Keep the README rollup log table?

## Follow-ups (outside the scripts)

- Docs: `docs/src/datasets/incursions.md`, and each rolled-up dataset's page or `index.md`, should say that
  past years are published as `<archive>-<year>.tar.xz`, extract to the data site layout, and contain plain
  JSON (no bz2).
- Optional: teach `ArchivePathFactories` about rollup files, so `index.json` gets `type` and `file_time` for
  them.
