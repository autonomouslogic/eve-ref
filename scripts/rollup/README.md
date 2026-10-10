# Yearly history rollups

Manual workflow for collapsing one finished year of a dataset's history on [data.everef.net](https://data.everef.net/)
into a single archive:

```
<archive>/history/<year>/...   ->   <archive>/history/<archive>-<year>.tar.xz
```

> **Status:** `00-preflight.sh` through `04-archive.sh` are written. `05-upload.sh` through `07-complete.sh`
> are still planned. Design notes are in [PLAN.md](PLAN.md).

## Archive format (2023 onward)

- Paths exactly as on the data site: `<archive>/history/<year>/<YYYY-MM-DD>/<name>.json`. Extracting several
  archives into one directory recreates the data site layout.
- Regular files only, sorted by path. Headers carry only the name and mtime. Owner is `0/0` with no user or
  group names, and mode is a fixed `0644`.
- Data files are decompressed (no `.bz2`). Each file's mtime is its original upload time. `index.json` files are
  kept, because they hold what `data-index` worked out about each file. Their entries still name the original
  `.json.bz2` files. `index.html` files are dropped.
- Compressed with `tar -J` defaults (xz preset `-6`).
- The archive's own mtime is `<year>-12-31T23:59:59Z`. It is served with
  `Cache-Control: public, max-age=31536000, immutable`.

`incursions-2022.tar.xz` was made by hand before this format existed (no `history/` in its paths, owner names,
unsorted). It stays as is.

## When to roll up

- Only **finished years**, never the current UTC year.
- Only in-scope datasets (see PLAN.md, "Scope"). These are `.json.bz2` snapshot datasets whose history the
  code never reads back.

## Setup

1. Install rclone.
2. `cp scripts/rollup/rollup.env.sample scripts/rollup/rollup.env` and fill in the B2 key. `rollup.env` is
   gitignored by `scripts/rollup/.gitignore`. Every script reads its configuration from this file and nowhere else:
   - the rclone remote `everef` (native `b2` backend, `hard_delete=false`), defined purely through
     `RCLONE_CONFIG_EVEREF_*` variables. Your own `rclone.conf` is ignored;
   - `ROLLUP_SCRATCH_ROOT`, the scratch root. For big datasets, point it at a disk, not a tmpfs.

Never run `rclone purge`, `rclone cleanup`, `rclone backend cleanup-hidden` or anything with `--b2-hard-delete`
against this bucket. Deletes must stay soft, so the bucket lifecycle rules provide the 2-day undo window.

## Procedure

All scripts take `<archive> <year>`, e.g. `./01-list.sh incursions 2023`. Run the steps in order and **read
each summary before continuing**.

Scratch dir: `$ROLLUP_SCRATCH_ROOT/<archive>-<year>/`. It holds the downloads, the build, the checksums, the logs
and the completion markers. The scripts never clear it. Remove it yourself once the rollup is `COMPLETE` and the
2-day undo window has passed: until then, `download/` is the only local copy of the original files.

If a step fails partway, remove its partial output and rerun it. Download and delete can simply be rerun.

| Step | Script            | Changes remote?      | Check before moving on                                        |
|------|-------------------|----------------------|---------------------------------------------------------------|
| 1    | `00-preflight.sh` | no                   | Tools OK, archive not already on the remote                   |
| 2    | `01-list.sh`      | no                   | File counts, days covered, gaps, no anomalies, enough space in the scratch root |
| 3    | `02-download.sh`  | no                   | `rclone check` 0 differences. Sizes and mtimes match the listing |
| 4    | `03-prepare.sh`   | no                   | Decompressed content hash-matches the originals. JSON valid   |
| 5    | `04-archive.sh`   | no                   | Sorted, owner-free headers. Archive contents hash-match the originals |
| 6    | `05-upload.sh`    | adds the archive     | B2 SHA-1, public URL headers including `Cache-Control`, public download sha256 |
| 7    | `06-delete.sh`    | **hides year dir**   | Dry-run count equals listing. Typed `<archive>-<year>` confirmation |
| 8    | `07-complete.sh`  | no                   | Year dir still empty (no re-uploaded index files). Summary printed. `COMPLETE` written |

Steps 1-6 are safe to abandon: nothing is removed from the remote, and the only addition is the archive (6).
Step 7 is the only destructive step. It is a soft delete, which can be undone within the 2-day bucket lifecycle
window.

The scripts don't touch index pages. `<archive>/history/index.json` picks up the archive at the dataset's next
scrape, or at the next scheduled `data-index` run for datasets that no longer scrape.

## Appendix: locking and markers

Implementation detail, not needed to run a rollup.

Only one step runs at a time: each step locks `/tmp/everef-rollup.lock` and aborts if another step holds it.

Each step writes `<step>.done` in the scratch dir when all of its checks pass. A step refuses to run if:

- any earlier marker is missing;
- its own marker already exists;
- `COMPLETE` exists.
