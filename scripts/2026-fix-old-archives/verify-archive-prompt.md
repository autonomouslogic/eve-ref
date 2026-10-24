BASE_URL: https://data.everef.net/sovereignty-map/history/

You are verifying converted data archives published under `BASE_URL` above. Everything you need is
downloadable from there — you have no other context, so discover all of the following yourself
rather than asking for it:

- Fetch `BASE_URL/index.json` to get the list of all `<name>-<year>.tar.bz2` **original** archives
  and their sizes/etags (MD5). The `<name>` prefix is whatever actually appears in that listing —
  don't assume it matches the last path segment of BASE_URL (e.g. a dataset served at
  `.../sovereignty-map/history/` might use archive names like `sovereigntymap-<year>.tar.bz2`).
  Verify every archive this way — do not skip any year that's listed.
- For each such year, the **converted** archive is expected at
  `BASE_URL/<name>-<year>.tar.xz`. If it's missing for a year that has a `.tar.bz2`, that itself is
  a finding — report it, don't skip the year.
- For each year, also fetch `BASE_URL/<year>/index.json`. A 404 means no live leftover for that
  year (nothing further to do). A 200 means there's a leftover set of individual
  `<name>-<date>_<HH-MM-SS>(.vN)?.json.bz2` files nested under per-day directories
  (`<year>/<YYYY-MM-DD>/`) that should have been merged into that year's converted archive —
  download every file listed and treat it as a **live leftover file** (rule 4 below). Verify
  size/MD5 (etag) of every download against what the index.json says.

Download everything over HTTP yourself; don't assume local files exist. Do this for every year
found in `index.json` — produce one pass/fail verdict per year, plus an overall summary.

Your job is to independently confirm each year's conversion was done correctly, with no data loss,
no corruption, and no unauthorized changes. Do not assume the conversion is correct — re-derive
every check yourself from the raw files.

## Conversion rules being verified

1. **Format change only.** The original is a `.tar.bz2` of flat JSON files under one top-level
   directory. The converted archive is a `.tar.xz` (GNU tar, xz-compressed) containing the same
   data, just renamed and recompressed. No JSON content should differ.

2. **Renaming.** Original filenames look like `<name>_<ISO8601 timestamp>.json`, e.g.
   `examplename_2019-01-01T00:20:09.json`. In the converted archive, each file should be renamed to
   `<name>-<date>_<HH-MM-SS>.json` (dashes instead of colons in the time, and nested under a
   `<date>/` directory), e.g. `examplename-2019-01-01_00-20-09.json`. The date and time encoded in
   the new name must exactly match the date and time encoded in the old name for that file.

3. **Files that should have been dropped, not carried over.** Any original file whose entire body
   is one of the following (and nothing else) should be **absent** from the converted archive:
   - An HTML error page titled exactly "502 Bad Gateway" or "504 Gateway Time-out" (standard nginx
     error page format: `<html><head><title>{title}</title></head><body><center><h1>{title}</h1></center>...`).
   - A JSON object whose body is exactly one of:
     - `{"error":"Not found"}`
     - `{"error":"No reply within 10 seconds"}`
     - `{"error":"Timeout contacting tranquility","timeout":10}`
     - `{"error":"The datasource tranquility is temporarily unavailable"}`
     - `{"error":"timeout contacting endpoint"}`
     - the ESI "error limit exceeded" message (mentions "exceeded the error limit for ESI")
     - the ESI "banned" message (mentions "You have been banned from using ESI")
   - Any file that fails to parse as exactly one well-formed JSON value (truncated, corrupt, or
     otherwise invalid JSON), regardless of content.

   Any original file that does NOT match one of the above (e.g. a `{"error": ...}` object with a
   message not in this list, or any other HTML) should have been **carried over**, not dropped. If
   you find a dropped file that doesn't actually match one of the above exactly, or a kept file
   that does match, flag it.

4. **Live leftover files, if provided.** Each is a `.bz2`-compressed JSON file that should appear,
   decompressed and renamed the same way (rule 2), in the converted archive. Every live-leftover
   file's date/time must sort **after** every file that came from the original archive — if any
   live file's timestamp is earlier than or equal to the latest original-archive file's timestamp,
   that's a boundary violation and must be flagged. Live files that fail JSON validation (same
   rule as above) should be absent from the converted archive too.

5. **No duplicates, no gaps you can't explain.** Every surviving file (original minus dropped, plus
   any live leftover minus dropped) should appear exactly once in the converted archive. The
   converted archive should contain no files beyond these.

6. **Archive technical format.** The converted `.tar.xz`, when listed (`tar -tvf ... --full-time`),
   must have every entry as a regular file (no directories, symlinks, etc.) with mode `-rw-r--r--`
   and owner/group `0/0`, and entries must be in strict byte-sorted (`LC_ALL=C`) path order by
   full path (`<date>/<filename>`).

## What to do, per year

1. Download the original `.tar.bz2`, the converted `.tar.xz`, and (if present) the live leftover
   files for that year, verifying each download's size/MD5 against the index.json it came from.
2. Extract both archives (and decompress any live leftover files) into separate working
   directories.
3. Enumerate every file in the original archive. For each, determine: kept or dropped, and why,
   per rule 3. Verify your determination against the actual byte content, not just a guess.
4. For every file you expect to be kept (from the original, plus any valid live leftovers), confirm
   it exists in the converted archive under the correct renamed path (rule 2), and that its
   decompressed JSON content is byte-for-byte identical to the source (compare SHA-256, don't eyeball
   it).
5. Confirm the converted archive contains nothing extra and nothing missing versus your expected
   set from step 4.
6. If live leftovers were present, check the ordering boundary (rule 4).
7. Check the technical archive format (rule 6).

## Output

Report one verdict per year, then an overall summary.

For a year that checks out: a short confirmation stating file counts (kept, dropped, and why) and
that all content/order/format checks passed. No need to list every file.

For a year with a problem: explain exactly what's wrong, which file(s) are affected, and why it
violates the rule above (quote the relevant rule number). Be specific — include filenames and
hashes where relevant so the discrepancy can be tracked down and fixed.
