# Rollup development notes

Guidance for changing the rollup scripts. The operator runbook is [README.md](README.md). Keep README's
procedure table in sync with the scripts whenever a step is added or changed.

## Scope

A dataset is in scope (`ROLLUP_SCOPE` in `lib/scope.sh`, shared by `common.sh` and `advise.sh`) only if it has a `history/<year>/<date>/` layout with
`.json.bz2` (or `.vN.json.bz2`) files, and **nothing in the code reads its history back**. Before adding one,
check its layout on the data site and grep `src/main` for consumers of its `ArchivePathFactories` constant.

Never in scope:

- `market-orders`, `market-history`: read back by `HistoricalOrdersRegionTypeSource` and `MarketHistoryUtil`.
- `killmails`, `public-contracts`, `esi-scrape`, `hoboleaks-sde`, `reference-data`, `fuzzwork/ordersets`,
  `ccp/*`: already archives, or different layouts.
- The current UTC year. The scripts assume a finished year's data files never change, which is why there is
  no re-list before delete.

## Archive format

The format in README must stay stable, so archives from different years extract into one tree. Tar pitfalls:

- Don't use `--format=posix`: it adds atime/ctime pax headers. Keep GNU format.
- Header verification runs `tar -tv` **without** `--numeric-owner`, so `0/0` in the output proves the user and
  group names are empty.

## B2 / rclone

- Deletes must stay soft (hide markers). The 2-day bucket lifecycle window is the only undo; there is no
  restore tooling. Never use `purge`, `cleanup`, `backend cleanup-hidden` or `hard_delete`.
- Object mtime travels as `x-amz-meta-src_last_modified_millis`. rclone's `b2` backend reads and writes it,
  and `DataIndex` uses it (`S3Adapter.headLastModified`) for `Last-Modified` in `index.json`.
- rclone's native `b2` backend ignores `--header-upload` (see README, "Known gaps").
- Use `--fast-list` on listings to limit Class C transactions.

## Index pages

The scripts never run `data-index`. A scheduled `data-index` run that listed the bucket before the delete can
re-upload `<year>/index.{html,json}` afterwards. `08-complete.sh` checks for this.

## Scale

Decompressed years can be several GB (e.g. `skinr-listings`), so the scratch root must be disk-backed.
`02-list.sh` estimates the needed space from a sampled bz2 ratio.

## Possible follow-ups

- Teach `ArchivePathFactories` about rollup files, so `index.json` gets `type` and `file_time` for them.
- Set `Cache-Control` on archives, via B2's S3-compatible endpoint or a bucket-level default.
