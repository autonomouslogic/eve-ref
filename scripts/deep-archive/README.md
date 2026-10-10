# deep-archive/ — EVE Ref Deep Archive

Tooling and operator documentation for the **EVE Ref Deep Archive**: old data.everef.net data moved into yearly
torrents.

Status: **planning**. The scripts don't exist yet.

## What this does

Ad hoc, by hand, **at most once per calendar year**:

1. Select the data on data.everef.net from years ≤ current year − 2. Never `market-history` or `killmails`. The
   selection is a frozen file list that every later step is checked against.
2. Announce the run on Discord.
3. Copy the selection from the mounted data storage into a local directory, which is also the seeding directory.
4. Make `everef-deep-archive-YYYY.torrent` (hybrid v1+v2), with checksums (`.sha256`) and a README with verification
   instructions inside it, plus a file list `everef-deep-archive-YYYY.txt`.
5. Seed it and test it.
6. Upload the torrent and file list to `data.everef.net/deep-archive/`, and add the torrent to the RSS feed there.
7. Check that the docs site is accurate.
8. Back it up to S3 Glacier Deep Archive.
9. Last, once every year in the run has passed all of the above: delete the archived files from data.everef.net,
   run a full `DataIndex`, check the site, and announce the result.

Every step has a separate check script, and nothing moves forward until its check passes.

## Documents

- [`RUNBOOK.md`](RUNBOOK.md): **the process to follow**, step by step.
- [`PLAN.md`](PLAN.md): design decisions, reasoning, risks and open questions. Temporary; it goes away once the
  process is settled.
- `LOG.md`: record of archived torrents (created on the first run).

User-facing documentation (what the torrents are, why, how to use them, when they appear) lives on the docs site:
`docs/src/datasets/deep-archive.md`.

## Layout (planned)

```
deep-archive/
  README.md  RUNBOOK.md  PLAN.md  LOG.md
  superseded.txt          # replaced torrents, created when first needed
  run.sh                  # Docker wrapper: deep-archive/run.sh <script> --dir <dir> [args]
  Dockerfile              # rclone, torrent creator, qBittorrent tooling, Python
  torrents.env.example    # copy to torrents.env (not committed)
  README-template.txt     # source for <ID>-README.txt inside each torrent
  trackers.txt            # trackers added to every torrent, one per tier
  scripts/                # find.py, create_torrent.py (drafts); later check-env, start-year, load, make-torrent, ...
```

`--dir` is required on every script. It's the working directory and the directory torrents are seeded from. See
RUNBOOK "Conventions".

The scripts reach the data site's storage only through a filesystem mount, `DATA_MOUNT` in `torrents.env` (today
B2 through `rclone mount`). No S3 API or B2 credentials. See PLAN 2.21.
