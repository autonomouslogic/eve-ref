# Yearly history rollups

Manual workflow for collapsing one finished year of a dataset's history on [data.everef.net](https://data.everef.net/)
into a single archive:

```
<archive>/history/<year>/...   ->   <archive>/history/<archive>-<year>.tar.xz
```

> **Status:** the scripts below are planned and not written yet. Design notes are in [PLAN.md](PLAN.md). Until
> the scripts exist, this file documents the intended procedure.

## Archive format (2023 onward)

- Paths exactly as on the data site: `<archive>/history/<year>/<YYYY-MM-DD>/<name>.json`. Extracting several
  archives into one directory recreates the data site layout.
- Regular files only, sorted by path. Headers carry only the name and mtime. Owner is `0/0` with no user or
  group names, and mode is a fixed `0644`.
- Data files are decompressed (no `.bz2`). Each file's mtime is its original upload time. `index.json` files are
  kept and `index.html` files dropped.
- Compressed with `tar -J` defaults (xz preset `-6`).
- The archive's own mtime is `<year>-12-31T23:59:59Z`.

`incursions-2022.tar.xz` was made by hand before this format existed (no `history/` in its paths, owner names,
unsorted). It stays as is.

## When to roll up

- Only **finished years**, never the current UTC year.
- Only in-scope datasets (see PLAN.md, "Scope"). These are `.json.bz2` snapshot datasets whose history the
  code never reads back.

## Setup

- rclone with a native **`b2`** remote for bucket `data-everef-net-425eb511`, with `hard_delete` left at
  `false`. The scripts use the remote name hard-coded in `common.sh`.
- Never run `rclone purge`, `rclone cleanup`, `rclone backend cleanup-hidden` or anything with `--b2-hard-delete`
  against this bucket. Deletes must stay soft, so the bucket lifecycle rules provide the undo window.

## Procedure

All scripts take `<archive> <year>`, e.g. `./20-list.sh incursions 2023`. Run the steps in order and **read
each summary before continuing**.

Scratch dir: `/tmp/everef-rollup/<archive>-<year>/`. It holds the downloads, the build, the checksums, the logs
and the completion markers. The scripts never clear it; remove it yourself once the rollup is `COMPLETE`.

Markers: each step writes `<step>.done` in the scratch dir when all of its checks pass. A step refuses to run if:

- any earlier marker is missing;
- its own marker already exists;
- `COMPLETE` exists.

If a step fails partway, remove its partial output and rerun it. Download and delete can simply be rerun.

| Step | Script            | Changes remote?      | Check before moving on                                        |
|------|-------------------|----------------------|---------------------------------------------------------------|
| 1    | `10-preflight.sh` | no                   | Tools OK, archive not already on the remote, enough space in `/tmp` |
| 2    | `20-list.sh`      | no                   | File counts, days covered, gaps. No anomalies                 |
| 3    | `30-download.sh`  | no                   | `rclone check` 0 differences. Sizes and mtimes match the listing |
| 4    | `40-prepare.sh`   | no                   | Decompressed content hash-matches the originals. JSON valid   |
| 5    | `50-archive.sh`   | no                   | Sorted, owner-free headers. Extracted copy hash-verifies      |
| 6    | `60-upload.sh`    | adds the archive     | B2 SHA-1, public URL headers, public download sha256          |
| 7    | `70-delete.sh`    | **hides year dir**   | Dry-run count equals listing. Typed `<archive>-<year>` confirmation |
| 8    | run `data-index`, then `80-index.sh` | rewrites indexes | `history/index.json` shows the archive and no year. No orphan index files |
| 9    | `90-complete.sh`  | no                   | Paste the printed row into the log below. `COMPLETE` written  |

Steps 1-6 are safe to abandon: nothing is removed from the remote, and the only addition is the archive (6).
Step 7 is the only destructive step. It is a soft delete, which can be undone within the bucket lifecycle window.

## Rollup log

| Date       | Archive      | Year | Data files | Archive size | SHA-256 of archive                                                 | Notes |
|------------|--------------|------|------------|--------------|--------------------------------------------------------------------|-------|
| 2026-10-09 | `incursions` | 2022 | 4,394      | 205,308 B    | `f376eb4e7eb3c8d4f4f2fe234f594f108b9451e9dc238163d677c643d6c78580` | Manual, pre-format (see above). 2022-12-16 to 2022-12-31 |
