# Yearly history rollup: implementation plan

Status: `00-preflight.sh` through `03-prepare.sh` are written; `04-archive.sh` through `07-complete.sh` are
still planned. This file holds implementation notes for review; the operator runbook lives in
[README.md](README.md). **Keep README.md's status line and procedure table up to date whenever a script is
added or changed** — it must never drift from what's actually implemented.

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
- `.bz2` is stripped from every data file. `index.html` files are removed. `index.json` files are **kept**,
  because they hold what `data-index` worked out about each file (size, etag, `last_modified`, `type`,
  `file_time`). Their entries still name the original `.json.bz2` files, which is fine.
- Compression is whatever `tar -J` does by default (xz default preset `-6`). `common.sh` clears `XZ_OPT` and
  `XZ_DEFAULTS`, so the operator's shell can't change it.
- Archive object mtime: `<year>-12-31T23:59:59Z`.
- Archive object `Cache-Control`: `public, max-age=31536000, immutable` (365 days). Finished years never get a
  new archive.
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
- `incursions/history/index.json` picked up the archive and dropped `2022/` without a manual `data-index` run.

## Index pages

The scripts never run `data-index`. `<archive>/history/index.{html,json}` is refreshed by the dataset's own
scrape (every upload re-indexes its parent dirs non-recursively, through `DataIndexHelper`) and by the scheduled
`data-index` run. The scheduled run also rewrites the index files inside finished years. That doesn't matter
here: the year's index files are deleted along with its data files.

## Scale

- `incursions/history/2023`: 288 files a day (5-minute cadence), so about 105k data files plus about 732 index
  files. One sampled day is about 109 KB of bz2, so roughly 40 MB of bz2 for the year.
- `skinr-listings` files are about 200-250 KB of bz2 each. Decompressed data for a year can be several GB, so
  `ROLLUP_SCRATCH_ROOT` should be on disk rather than a tmpfs (RAM) for those, and preflight checks free space.

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
- the current UTC year or later. The data files of finished years never change, which is the basis for
  skipping a re-list before delete. Only their index files get rewritten (see "Index pages").

## Configuration: `scripts/rollup/rollup.env`

All configuration lives in `scripts/rollup/rollup.env`. `scripts/rollup/.gitignore` ignores it (checked with
`git check-ignore`). A committed `scripts/rollup/rollup.env.sample` documents it.
`common.sh` loads it with `set -a; . "$DIR/rollup.env"; set +a`. It aborts if the file is missing or any required
variable is empty.

rclone is configured **only from environment variables** (rclone's `RCLONE_CONFIG_<REMOTE>_<OPTION>`
convention), so no `rclone.conf` is involved. `common.sh` also exports `RCLONE_CONFIG=/dev/null`, which makes
rclone use an in-memory config. Remotes in the operator's own `rclone.conf` can then never be picked up by
mistake.

`rollup.env.sample`:

```bash
# Copy to rollup.env (gitignored) and fill in. Sourced by bash, so ${...} references work.

ROLLUP_BUCKET=

# rclone remote "everef" account/key. common.sh sets the rest (type, hard_delete).
RCLONE_CONFIG_EVEREF_ACCOUNT=
RCLONE_CONFIG_EVEREF_KEY=

# Scratch root. Each rollup gets <root>/<archive>-<year>/. Holds the download, the decompressed stage and the
# archive, so use a disk-backed path (not a tmpfs) for big datasets. common.sh defaults to
# /tmp/everef-rollup; uncomment to override.
#ROLLUP_SCRATCH_ROOT=/tmp/everef-rollup
```

`common.sh` derives `REMOTE="everef:${ROLLUP_BUCKET}"` and `SCRATCH="${ROLLUP_SCRATCH_ROOT}/<archive>-<year>"`.
It hard-codes the public base URL (`https://data.everef.net`) and the lock file (`/tmp/everef-rollup.lock`).

## rclone / B2

- The operator installs rclone. Development and end-to-end testing are done by the operator, on production
  data.
- Delete with `rclone delete` (it hides files). Undo is the bucket lifecycle window: hidden versions are kept
  for **2 days**. Never use `rclone purge`, `rclone cleanup`, `backend cleanup-hidden` or `--b2-hard-delete`: on
  b2 these remove old versions too.
- Use `--fast-list` on listings to keep Class C transactions down.

## Scratch directory and markers

`SCRATCH=$ROLLUP_SCRATCH_ROOT/<archive>-<year>`. The scripts never delete this dir. The operator clears it.

```
$ROLLUP_SCRATCH_ROOT/<archive>-<year>/
  00-preflight.done ... 06-delete.done  completion markers (UTC timestamp + key facts, e.g. counts, sha256)
  COMPLETE                              written last, by 07-complete.sh
  listing.json                          rclone lsjson -R --hash --files-only of the remote year dir
  download/<archive>/history/<year>/    untouched rclone copy (.bz2, index.html, index.json)
  stage/<archive>/history/<year>/       working copy: no index.html, bz2 decompressed (tar root = stage/)
  <archive>-<year>.sha256               sha256 of each staged file, computed directly from stage/
  <archive>-<year>.tar.xz
  archive-contents.sha256               sha256 of each archive entry, streamed out of the archive
  archive.sha256 / archive.sha1
  verify/public/                        archive downloaded back from the public URL
  log/<step>-<UTC timestamp>.log        tee of each run
```

All `.sha256` lists use the `sha256sum` output format, with paths relative to `stage/` (that is,
`<archive>/history/<year>/...`, the same as the tar entry names), in `LC_ALL=C` sorted order. Two lists match
when the files are byte-identical.

Marker rules, enforced by `common.sh` at the start of every step:

1. If `COMPLETE` exists, abort. The rollup is done.
2. If this step's own `<step>.done` exists, abort ("already completed, refusing to rerun").
3. If any earlier step's `.done` is missing, abort.
4. Write `<step>.done` only after every verification in the step has passed.

A step that fails partway leaves no marker. Steps that produce local output (`stage/`, the archive,
`verify/`) refuse to start if that output already exists, so the operator removes the partial output by hand
before retrying. `02-download` is the exception: `rclone copy` resumes. Remote steps handle partial runs
themselves, as described in each step.

## Scripts

Planned files in `scripts/rollup/`. Each is `bash` with `set -euo pipefail`. Each sources `common.sh`, which
loads `rollup.env`. Each takes `<archive> <year>`, validates both (`^[a-z0-9-]+$` plus the scope list,
`^20[0-9]{2}$` plus earlier than the current UTC year), and tees its output to `log/`.

Before anything else, `common.sh` also:

- takes an exclusive lock with `flock -n` on `/tmp/everef-rollup.lock`, held for the whole run. Only one
  rollup step runs at a time on the machine, and a second one aborts at once;
- pins the environment the tools read: `export TZ=UTC LC_ALL=C`, and `TAR_OPTIONS`, `XZ_OPT`, `XZ_DEFAULTS`,
  `BZIP` and `BZIP2` exported as empty. The operator's shell can't change how `tar`, `xz` or `bzip2` behave.

| Script               | Remote       | What it does                                                           |
|----------------------|--------------|------------------------------------------------------------------------|
| `.gitignore`         | n/a          | Ignores `rollup.env`                                                   |
| `rollup.env.sample`  | n/a          | Template for the gitignored `rollup.env`                               |
| `common.sh`          | n/a          | Loads `rollup.env`, lock, tool environment, constants (`REMOTE`, `SCRATCH`, public URL), arg checks, markers, helpers |
| `00-preflight.sh`    | read         | Config, tools, remote access, year dir present, archive absent        |
| `01-list.sh`         | read         | `listing.json`, anomaly report, free space in the scratch root        |
| `02-download.sh`     | read         | `rclone copy` and `rclone check`, then verify against `listing.json`   |
| `03-prepare.sh`      | none         | Build `stage/`, decompress, hash-verify against the originals          |
| `04-archive.sh`      | none         | Build tar.xz, verify headers, order and contents, set mtime            |
| `05-upload.sh`       | write        | Upload, verify via B2 and the public URL                               |
| `06-delete.sh`       | **delete**   | Dry-run preview, typed confirmation, `rclone delete`, verify empty     |
| `07-complete.sh`     | read         | Final checks, print summary, write `COMPLETE`                          |

### 00-preflight

- `rollup.env` loaded, required variables set.
- Tools present: `rclone` (version printed), GNU `tar`, `xz`, `bzip2`, `jq`, `sha256sum`, `sha1sum`, `curl`,
  `find`, `sort`, `flock`.
- `rclone lsf $REMOTE/<archive>/history/` works and contains `<year>/`. It also contains `<year+1>/`,
  confirming the year is actually finished (scraping has moved on), beyond the current-UTC-year check in
  `common.sh`. `<archive>-<year>.tar.xz` exists neither in that listing nor at the public URL (HTTP 404).

### 01-list

- `rclone lsjson -R --hash --files-only --fast-list $REMOTE/<archive>/history/<year>` writes `listing.json`.
  This is the one recursive listing of the year dir; later steps and the free space check below reuse its
  byte counts instead of listing again.
- Abort on anomalies:
  - a non-index name that does not match `^<year>-MM-DD/<archive>-<that date>_HH-mm-ss(\.v[0-9]+)?\.json\.bz2$`;
  - a date outside `<year>`;
  - other names besides `index.html` and `index.json`, which are expected once per dir plus the year root;
  - zero-byte files;
  - files without a SHA-1 hash.
- Report: file count by kind, total bytes, files per day (min, max, missing days), first and last timestamp.
- Free space: download one sampled data file (`rclone cat`) to get its bz2 ratio, and apply that ratio to
  the data bytes already known from `listing.json` to estimate the decompressed size. Free space on the
  filesystem holding `ROLLUP_SCRATCH_ROOT` must be at least 2x the year's total bytes plus 2x that estimate.
  That covers the download, the stage, the archive and its public copy.

### 02-download

- `rclone copy --fast-list $REMOTE/<archive>/history/<year> download/<archive>/history/<year>`. rclone
  verifies SHA-1 on each transfer.
- Verify:
  - `rclone check $REMOTE/... download/... --fast-list`: 0 differences in either direction (SHA-1);
  - the local file list and sizes equal `listing.json` exactly;
  - local mtimes equal the `ModTime` values in `listing.json` to the second;
  - `bzip2 -t` passes on every `.bz2`.

### 03-prepare

- `cp -a download/<archive>/history/<year> stage/<archive>/history/<year>`.
- Delete `index.html` files. The count must equal the `index.html` count in `listing.json`.
- `find stage -name '*.bz2' -print0 | xargs -0 -P"$(nproc)" -n 500 bunzip2`. `bunzip2` fails loudly on
  corrupt input, so no separate integrity re-check is needed here (`bzip2 -t` already ran in `02-download`).
- `<archive>-<year>.sha256` comes straight from `stage/`: `find stage -type f -exec sha256sum {} +`,
  `LC_ALL=C` sorted by path. No separate "expected" hash is computed from `download/`.
- Verify:
  - the file count equals the data plus `index.json` count from `listing.json`;
  - mtimes match `listing.json`;
  - `jq empty` passes on every file (valid JSON).

### 04-archive

- Build with the tar command above. It writes to a temp name and renames on success.
- `touch -d "<year>-12-31T23:59:59Z"` on the archive.
- Verify:
  - `xz -t`;
  - `tar -tv --full-time` shows only regular files, all `0/0` and `-rw-r--r--`, in `LC_ALL=C` sorted order, with
    names, sizes and mtimes equal to `stage/` (no extra, none missing). It runs **without** `--numeric-owner`:
    GNU tar then prints the user and group names when a header has them, so `0/0` proves they are empty
    (checked locally);
  - the archive's contents hash-match the originals, streamed without extracting to disk (checked locally):

    ```bash
    tar -xJf "$ARCHIVE" \
    	--to-command='printf "%s  %s\n" "$(sha256sum | cut -d" " -f1)" "$TAR_FILENAME"' \
    	> archive-contents.sha256
    ```

    `archive-contents.sha256` must equal `<archive>-<year>.sha256` exactly (the tar is already in sorted order).
- Write `archive.sha256` and `archive.sha1`.

### 05-upload

- If the remote object already exists with the same SHA-1 (an earlier partial run), skip the upload and go
  straight to verification. If it exists with a different hash, abort.
- Upload:

  ```bash
  rclone copyto --immutable \
  	--header-upload "Cache-Control: public, max-age=31536000, immutable" \
  	"<archive>-<year>.tar.xz" "$REMOTE/<archive>/history/<archive>-<year>.tar.xz"
  ```

- Verify:
  - `rclone lsjson --hash`: size, SHA-1 equal to `archive.sha1`, ModTime `<year>-12-31T23:59:59Z`;
  - `curl -I` on the public URL: 200, `content-length`, `last-modified` at `<year>-12-31 23:59:59 GMT`,
    `x-amz-meta-src_last_modified_millis`, and `cache-control: public, max-age=31536000, immutable`. The
    `etag` is not checked;
  - download into `verify/public/` and compare the sha256 with `archive.sha256`.

### 06-delete

- The path is built from the validated args and must match `^[a-z0-9-]+/history/20[0-9]{2}$`. It is never a
  bare `history` dir.
- `rclone delete --dry-run --fast-list` on `$REMOTE/<archive>/history/<year>`: show the count, which must
  equal the `listing.json` count. This is a sanity check on the path, not a change check, because the data
  files of finished years don't change.
- The operator types `<archive>-<year>` to confirm.
- `rclone delete --fast-list --max-delete <listing count> $REMOTE/<archive>/history/<year>`. Safe to rerun if
  it fails partway.
- Verify:
  - `rclone lsf -R` of the year dir is empty;
  - a few old public URLs return 404;
  - the archive is still present with an unchanged SHA-1.

### 07-complete

- Recheck that the remote archive's SHA-1 equals `archive.sha1`.
- Recheck that `rclone lsf -R $REMOTE/<archive>/history/<year>` is still empty. A scheduled `data-index` run
  that listed the bucket before the delete could have re-uploaded `<year>/index.{html,json}` afterwards. If any
  appear, the script lists them and stops. The operator deletes them and reruns.
- Print a summary: archive, year, data file count, archive size, sha256.
- Write `COMPLETE`. Leave the scratch dir for the operator to clear once the 2-day lifecycle window has passed:
  until then, `download/` is the only local copy of the original files.

## Development testing (operator)

Things to confirm before the first real run:

1. `rclone copy` from b2 sets local mtimes from `src_last_modified_millis`. For files where the Java app did
   not set it, rclone should fall back to upload time. Compare a downloaded day against that day's
   `index.json` `last_modified`.
2. `rclone copyto` of the archive sets `src_last_modified_millis` from the local mtime, so the public
   `last-modified` and the index show `<year>-12-31T23:59:59Z`.
3. `rclone copyto --header-upload "Cache-Control: ..."` sets the header on the b2 backend, so the public
   response carries it.
4. `rclone lsjson --hash` returns SHA-1 for history files uploaded by the Java app through the S3 API.
5. `rclone delete` produces hide markers (`x-amz-delete-marker: true` on the old URLs).
6. The env-only remote works with `RCLONE_CONFIG=/dev/null`: `rclone lsf everef:<bucket>/incursions/history/`
   lists, and `rclone listremotes` shows only `everef:`.

End-to-end testing happens on production data, starting with `incursions 2023`.

## Undo

- Before `06-delete`: only the archive was added, and deleting it restores the old state.
- After `06-delete`: the hidden versions stay restorable for 2 days (bucket lifecycle). There is no restore
  tooling; it gets built if it is ever needed. Until the scratch dir is cleared, `download/` also holds a full
  local copy of the original files.

## Follow-ups (outside the scripts)

- Docs: `docs/src/datasets/incursions.md`, and each rolled-up dataset's page or `index.md`, should say that
  past years are published as `<archive>-<year>.tar.xz`, extract to the data site layout, and contain plain
  JSON (no bz2).
- Optional: teach `ArchivePathFactories` about rollup files, so `index.json` gets `type` and `file_time` for
  them.
