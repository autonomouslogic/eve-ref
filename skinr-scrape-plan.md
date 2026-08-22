# Plan: ScrapeSkinr command

## Context

CCP added SKINR cosmetics sold on Paragon Hub. We want two datasets:
1. Historical snapshots of Paragon Hub listings (incremental cursor updates)
2. Accumulated SKINR detail records (static data, keyed by skinr_id)

One combined command `scrape-skinr` producing two output files per run.

## ESI Endpoints

### Listings: `GET /paragon-hub/skinr`
- Cursor-based pagination
- `after` cursor = fetch records newer than this position
- Empty `listings` array = end of new records
- Records ordered by last_modified, most recent last
- Each listing: `id`, `state` (listed/sold_out/expired/removed), `skinr_id`, `seller_id`, `created`, `expires`, `quantity`, `price` (ISK or PLEX)
- Response shape: `{ "listings": [...], "cursor": { "after": "...", "before": "..." } }`

### Details: `GET /cosmetics/skinr/{skinr_id}`
- Single resource, no pagination
- Fields: `id`, `name`, `creator_id`, `ship_type_id`, `tier`, `line`, `layout`
- Client cache TTL: 1 year

## Two Output Files

### File A: Paragon Hub Listings
- Format: same shape as the ESI response — `{ "listings": [...], "cursor": { "after": "...", "before": "..." } }`
  - `cursor.after` from the last non-empty page is the bookmark for next incremental run
- Latest: uncompressed `.json`, archive: compressed `.json.bz2`
- Factory: `SKINR_LISTINGS`, path prefix: `paragon-hub-skinr/`

### File B: SKINR Details (accumulating)
- Format: JSON object keyed by `skinr_id`: `{ "<skinr_id>": { detail }, ... }`
- Latest: uncompressed `.json`, archive: compressed `.json.bz2`
- Factory: `SKINR_DETAILS`, path prefix: `skinr-details/`

## Listings Fetch Logic

### First run (no previous file)
1. `GET /paragon-hub/skinr?limit=100` — proxy handles full initial traversal, returns all listings
2. Store response as-is: listings array + cursor object

### Subsequent runs (previous file exists)
1. Load previous file, extract `cursor.after` and `listings` array
2. Fetch `GET /paragon-hub/skinr?limit=100&after={cursor.after}` — new/modified records
3. If `listings` not empty: collect listings, update cursor to this response's cursor, repeat step 2 with new `after`
4. Continue until empty `listings` array
5. Build merged result:
   - Start with previous `listings` as map keyed by `id` (in-memory merge only)
   - Remove any entry where `state != "listed"` (purge dead listings)
   - Overwrite/add all entries from update pages (newer data supersedes)
   - Convert map values back to array for output
6. Output: `{ "listings": [merged array], "cursor": { "after": last_after, "before": ... } }`

## SKINR Details Logic

1. Download previous details file → `Map<String, JsonNode>` keyed by `skinr_id` (empty map if no previous)
2. Collect all `skinr_id` values from current listings
3. For each skinr_id not in map: fetch `/cosmetics/skinr/{skinr_id}` in parallel (`VirtualThreads.parallel()`)
4. Merge new details into map
5. Serialize map and upload

## Files to Create/Modify

### New
- `src/main/java/com/autonomouslogic/everef/cli/ScrapeSkinr.java`

### Modified
- `src/main/java/com/autonomouslogic/everef/util/archive/ArchivePathFactories.java` — add `SKINR_LISTINGS`, `SKINR_DETAILS` factories (`latestSuffix=.json`, `suffix=.json.bz2`)
- `src/main/java/com/autonomouslogic/everef/cli/CommandRunner.java` — `Provider<ScrapeSkinr>` + `"scrape-skinr"` case
- `src/main/java/com/autonomouslogic/everef/esi/EsiUrl.java` — add `after`, `before`, `limit` params if missing

## Verification
- `./gradlew compileJava`
- `./gradlew test`
- Manual run: verify two files in S3; second run produces incremental update with only new listings merged
