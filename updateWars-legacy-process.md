# UpdateWars Legacy Process - Detailed Overview

This document describes what the UpdateWars legacy process does, from a functional perspective. This enables reimplementation in another codebase.

## High-Level Purpose

The UpdateWars system maintains a persistent database of EVE Online wars and killmails, periodically fetching new data from the ESI API, and exporting this data in structured archive formats for distribution to end users.

---

## Process Flow Overview

### Entry Point: Shell Script (updateWars.sh)

1. **Restore Database State**
   - Pulls the wars.mvstore database file from S3 (`s3://eve-ref-legacy-store/db/wars.mvstore`)
   - Places it in `/var/data/evemarket/cache/wars.mvstore`

2. **Run Update Job**
   - Invokes the Java application with command: `evemarket updateWars`

3. **Persist Changes**
   - Pushes the updated wars.mvstore back to S3

---

## Main Job: Incremental Update Process

The primary operation is a continuous incremental update that:

### A. Initialize State

1. **Load MVStore Database**
   - Opens/creates a local MVStore file with 4 maps:
     - `wars`: Stores complete war data (Long warId → JsonNode)
     - `kill_mails`: Stores complete killmail data (Long killmailId → JsonNode)
     - `pending_killmails`: Tracks killmails that haven't been exported yet (Set of Long killmailIds)
     - `meta`: Stores metadata including the timestamp of the last export


### B. Determine Scope of Fetch

1. **Get Upper Bound War ID**
   - **ESI Endpoint**: `GET https://esi.evetech.net/latest/wars/`
   - Queries ESI for all available war IDs
   - Response is an array of integer war IDs (paginated if necessary)
   - Determines the maximum war ID currently in the ESI system

2. **Identify Unfinished Wars**
   - Scans the local store for wars without a "finished" date
   - These wars are still active and need their data updated

3. **Identify Unknown Wars**
   - Compares the maximum war ID in the local store against the ESI upper bound
   - Creates a range of all unknown war IDs that fall between the stored max and the current max

4. **Combine Fetch Target**
   - Merges unfinished + unknown wars into a single set to fetch
   - Logs statistics: total wars, unfinished count, new count

### C. Fetch War Data

1. **Fetch War Records**
   - **ESI Endpoint**: `GET https://esi.evetech.net/latest/wars/{war_id}/`
   - Retrieves complete war data from ESI for each targeted war ID
   - Parameter: `war_id` - The numeric ID of the war to fetch
   - Concurrent fetching (32 wars in parallel)
   - **Response Fields Stored** (at minimum):
     - `war_id`: The ESI war ID (long)
     - `declared`: ISO-8601 timestamp when war was declared (e.g., "2021-06-15T12:34:56Z")
     - `started`: ISO-8601 timestamp when war started (e.g., "2021-06-15T12:34:56Z")
     - `retracted`: ISO-8601 timestamp when war was retracted (optional - may be null)
     - `finished`: ISO-8601 timestamp when war ended (optional - may be null)
     - `aggressor_id`: Entity ID of the aggressor (long)
     - `defender_id`: Entity ID of the defender (long)
     - `allies`: Array of allied entities
     - `mutual`: Boolean indicating if war is mutual
     - `open_for_allies`: Boolean indicating if war is open for allies
     - `hp`: Object with shield/armor/hull values
     - `reward`: ISK reward pool (long)
   - **Additional Field Stored**:
     - `http_last_modified`: Captured from HTTP Last-Modified response header (ISO-8601)
   - Stores complete response in the `wars` map

2. **War Exclusion Rules**
   - Skips specific "bad wars" by ID: 90591, 473095
   - Skips entire range: 472167-473147 (problematic war IDs)
   - Return null/empty response for these wars (no error thrown)

### D. Fetch Killmail Data

1. **Fetch Killmail IDs for Each War**
   - **ESI Endpoint**: `GET https://esi.evetech.net/latest/wars/{war_id}/killmails/`
   - Parameter: `war_id` - The numeric ID of the war
   - Queries ESI for each war's killmail list
   - Response is an array of killmail objects with fields:
     - `killmail_id` (long): Unique killmail ID
     - `killmail_hash` (string): Required for fetching the full killmail details
   - Concurrent fetching (2 wars in parallel for killmail IDs)
   - **Early Killmail Rules**: Only 4 early wars have killmails (48074, 138678, 144630, 149785)
     - For wars with ID < 149785, only fetch if the war ID is in the hardcoded list above
     - For wars with ID >= 149786, fetch killmails (full history available)
     - Return empty list for wars that don't meet the criteria

2. **Fetch Individual Killmail Details**
   - **ESI Endpoint**: `GET https://esi.evetech.net/latest/killmails/{killmail_id}/{killmail_hash}/`
   - Parameters:
     - `killmail_id` (long): The killmail's unique ID
     - `killmail_hash` (string): The killmail hash (obtained from step 1)
   - For each killmail not already in the store:
     - Attempts to fetch from ESI with the hash from step 1
     - **If ESI returns 422 error (Unprocessable Entity - invalid hash)**:
       - Falls back to Zkillboard API to get the correct hash
       - **Zkillboard Endpoint**: `GET https://r2z2.zkillboard.com/history/{date}.json`
       - Parameter: `date` in format `YYYY-MM-DD` (ISO-8601 date)
       - Response is a JSON object where keys are killmail IDs and values are correct hashes
       - Searches this response for the desired killmail ID
       - If hash is found, retries ESI fetch with the corrected hash
       - Special case: If hash in response is "CCP VERIFIED", skip this killmail (it's inaccessible)
     - If still unable to retrieve after Zkillboard fallback (422 error again), logs error and skips
   - Concurrent fetching (4 killmails in parallel)
   - **Response Fields Stored** (at minimum):
     - `killmail_id` (long): The killmail's unique ID
     - `war_id` (long): Associated war ID (from the fetching context, not ESI response)
     - `killmail_hash` (string): The killmail hash (from the fetching context)
     - `killmail_time`: ISO-8601 timestamp when the kill occurred (e.g., "2021-07-01T14:30:45Z")
     - `victim`: Object containing victim details (character, corporation, alliance, ship, position, damage_taken)
     - `attackers`: Array of attacker objects (character, corporation, alliance, ship, damage_done, security_status, etc.)
     - `solar_system_id` (long): ID of the solar system where the kill occurred
     - `moon_id` (long, optional): ID of nearby moon if applicable
   - **Additional Field Stored**:
     - `http_last_modified`: Captured from HTTP Last-Modified response header (ISO-8601)
   - Stores complete response in the `kill_mails` map
   - Marks each new killmail as "pending" in the `pending_killmails` set

### E. Generate Incremental Export File

1. **Determine Which Killmails to Include**
   - Identifies killmails with `killmail_time` >= last export timestamp
   - Adds all killmails in the `pending_killmails` set
   - This ensures no data is missed even if timing is tight

2. **Create TAR.BZ2 Archive**
   - Creates a temporary TAR.BZ2 compressed file with structure:
     - `wars/{warId}.json` - One JSON file per war in sorted order
     - `wars/{warId}/killmails/{killmailId}.json` - Killmails organized by war, in sorted order
   - Each JSON file contains the complete record (all fields from the store)

3. **Upload to S3**
   - Uploads incremental file to S3 with path:
     - `s3://data-bucket/wars/history/{YYYY}/wars-{YYYY-MM-DD_HH-MM-SS}.tar.bz2`
   - One file per update with timestamped naming

### F. Commit Metadata

1. **Record Export Timestamp**
   - Stores the current export timestamp in the `meta` map under key `last_export`

2. **Clear Pending List**
   - Empties the `pending_killmails` set (all pending killmails are now exported)

3. **Commit MVStore Changes**
   - Persists all changes to the local MVStore file

### G. Export Current Wars List

1. **Create Current Wars JSON**
   - Generates a single JSON file containing all currently active wars
   - Filters to only include wars without a `finished` date
   - Format: Object with war IDs as keys, complete war data as values
   - Sorted by war ID

2. **Upload to S3**
   - Uploads to: `s3://data-bucket/wars/wars-current.json`
   - Replaces the previous version
   - Content type: `application/json`

### H. Clean Up Old Data

1. **Delete Old Wars**
   - Removes wars that fall outside the current time range
   - Criteria: A war is kept if any of its key dates (declared, started, retracted, finished) overlap with the current time window
   - Logs count of deleted wars

2. **Delete Orphaned Killmails**
   - Removes killmails that:
     - Are NOT in the pending list (not waiting to be exported)
     - AND their associated war is no longer in the store
   - Rationale: If we delete a war, we should also delete its killmails to save space, unless they're pending export
   - Logs count of deleted killmails

### I. Close and Compact Database

1. **Commit Final Changes**
   - Final commit to MVStore

2. **Compact Database**
   - Runs full rewrite compaction
   - Moves chunks to optimize storage layout
   - Reduces file size and improves performance

3. **Close Connection**
   - Closes the MVStore safely

---

## API Reference

### ESI (EVE Swagger Interface) Endpoints

All ESI endpoints use:
- **Base URL**: `https://esi.evetech.net/latest/`
- **Error Handling**: Non-200 responses should be logged but not considered fatal
- **Rate Limiting**: Standard ESI rate limits apply; implement backoff if necessary
- **Response Format**: JSON

#### 1. Fetch All War IDs
- **Endpoint**: `GET /wars/`
- **Parameters**: None
- **Response**: Array of integers (war IDs)
- **Example**: `[123456, 123457, 123458, ...]`
- **Used by**: Section B.1 (Get Upper Bound War ID)

#### 2. Fetch Specific War Details
- **Endpoint**: `GET /wars/{war_id}/`
- **Path Parameters**: `war_id` (long) - The war to fetch
- **Query Parameters**: None
- **Response**: JSON object with fields described in Section C.1
- **Headers Captured**: `Last-Modified` (store as `http_last_modified`)
- **Used by**: Section C.1 (Fetch War Records)

#### 3. Fetch Killmail IDs for a War
- **Endpoint**: `GET /wars/{war_id}/killmails/`
- **Path Parameters**: `war_id` (long) - The war ID
- **Query Parameters**: None (pagination may be handled automatically)
- **Response**: Array of killmail objects with structure:
  ```json
  [
    {
      "killmail_id": 123456789,
      "killmail_hash": "abcdef1234567890"
    }
  ]
  ```
- **Used by**: Section D.1 (Fetch Killmail IDs for Each War)

#### 4. Fetch Specific Killmail Details
- **Endpoint**: `GET /killmails/{killmail_id}/{killmail_hash}/`
- **Path Parameters**:
  - `killmail_id` (long) - The killmail ID
  - `killmail_hash` (string) - The killmail hash
- **Query Parameters**: None
- **Response**: JSON object with fields described in Section D.2
- **Headers Captured**: `Last-Modified` (store as `http_last_modified`)
- **Error Handling**:
  - **422 (Unprocessable Entity)**: Invalid killmail hash - trigger Zkillboard fallback
  - **Other non-200**: Log and skip
- **Used by**: Section D.2 (Fetch Individual Killmail Details)

### Zkillboard API Endpoints

#### 1. Fetch Killmail Hashes by Date
- **Base URL**: `https://r2z2.zkillboard.com/`
- **Endpoint**: `GET /history/{date}.json`
- **Path Parameters**: `date` in format `YYYY-MM-DD` (UTC)
- **Query Parameters**: None
- **Response**: JSON object mapping killmail IDs to hashes:
  ```json
  {
    "123456789": "abcdef1234567890",
    "123456790": "CCP VERIFIED",
    "123456791": "fedcba0987654321"
  }
  ```
- **Special Cases**:
  - If a killmail hash is "CCP VERIFIED", the killmail is inaccessible via ESI (skip it)
  - Not all killmails from a date may be present; only the ones in the response are available
- **Used by**: Section D.2 (Fetch Individual Killmail Details - Fallback for 422 errors)

### S3 API Operations

All S3 operations use the AWS SDK with the "data" bucket configuration.

#### 1. Download Database from S3
- **Bucket**: `eve-ref-legacy-store`
- **Key**: `db/wars.mvstore`
- **Local Path**: `/var/data/evemarket/cache/wars.mvstore`
- **Used by**: Shell script (Section: Entry Point)

#### 2. Upload Updated Database to S3
- **Bucket**: `eve-ref-legacy-store`
- **Key**: `db/wars.mvstore`
- **Source**: `/var/data/evemarket/cache/wars.mvstore`
- **Used by**: Shell script (Section: Entry Point)

#### 3. Upload Incremental Archive
- **Bucket**: Data bucket (from config)
- **Key Pattern**: `wars/history/{YYYY}/wars-{YYYY-MM-DD_HH-MM-SS}.tar.bz2`
- **Example Key**: `wars/history/2021/wars-2021-07-15_14-30-45.tar.bz2`
- **Content-Type**: `application/x-gtar`
- **File Format**: TAR.BZ2 archive
- **Used by**: Section E.3 (Upload to S3)

#### 4. Upload Current Wars JSON
- **Bucket**: Data bucket (from config)
- **Key**: `wars/wars-current.json`
- **Content-Type**: `application/json`
- **File Format**: JSON object
- **Used by**: Section G.2 (Upload Current Wars)

#### 5. Upload Yearly Archive (Backfill Only)
- **Bucket**: Data bucket (from config)
- **Key Pattern**: `wars/history/wars-{YEAR}.tar.bz2`
- **Example Key**: `wars/history/wars-2021.tar.bz2`
- **Content-Type**: `application/x-gtar`
- **Used by**: Backfill operations only

---

## Data Structures

### Database Initialization

The system uses an H2 MVStore embedded database for persistence. Initialize it as follows:

- **File Location**: `/var/data/evemarket/cache/wars.mvstore`
- **Configuration**:
  - Enable high-level compression
  - Cache size: 128 MB
  - Keep 0 versions for auto-cleanup
- **Maps Required**: 4 maps must be created/opened

### Wars Map
**Storage**: Named map `wars` in MVStore
**Key Type**: `Long` (war ID)
**Value Type**: `JsonNode` (Jackson ObjectNode)

Each war entry must contain (required fields):
- `war_id` (long): Unique war identifier (matches the key)
- `declared` (string): ISO-8601 timestamp when war was declared (e.g., "2021-06-15T12:34:56Z")
- `started` (string): ISO-8601 timestamp when war started
- `retracted` (string, nullable): ISO-8601 timestamp when war was retracted
- `finished` (string, nullable): ISO-8601 timestamp when war ended
- `http_last_modified` (string): ISO-8601 timestamp from ESI Last-Modified header

Additional fields from ESI (store all as returned):
- `aggressor_id` (long): Entity ID of the aggressor
- `defender_id` (long): Entity ID of the defender
- `allies` (array): Allied entities involved in the war
- `mutual` (boolean): Whether the war is mutual
- `open_for_allies` (boolean): Whether additional allies can join
- `hp` (object): Shield/armor/hull status
- `reward` (long): ISK reward pool

### Killmails Map
**Storage**: Named map `kill_mails` in MVStore
**Key Type**: `Long` (killmail ID)
**Value Type**: `JsonNode` (Jackson ObjectNode)

Each killmail entry must contain (required fields):
- `killmail_id` (long): Unique killmail identifier (matches the key)
- `war_id` (long): Which war this killmail belongs to
- `killmail_hash` (string): Hash required for ESI fetching (needed for re-fetching)
- `killmail_time` (string): ISO-8601 timestamp when the kill occurred
- `http_last_modified` (string): ISO-8601 timestamp from ESI Last-Modified header

Additional fields from ESI (store all as returned):
- `victim` (object): Victim information
  - `character_id` (long, optional): Character ID if applicable
  - `corporation_id` (long): Corporation ID
  - `alliance_id` (long, optional): Alliance ID if applicable
  - `ship_type_id` (long): Type of ship destroyed
  - `damage_taken` (double): Total damage taken
  - `position` (object, optional): x, y, z coordinates
- `attackers` (array): Array of attackers involved
  - Each attacker object contains:
    - `character_id` (long, optional): Character ID
    - `corporation_id` (long): Corporation ID
    - `alliance_id` (long, optional): Alliance ID
    - `ship_type_id` (long, optional): Ship type if in a ship
    - `weapon_type_id` (long, optional): Weapon used
    - `damage_done` (long): Damage dealt
    - `final_blow` (boolean): Whether this attacker delivered final blow
    - `security_status` (double, optional): Attacker's security status
- `solar_system_id` (long): Solar system where kill occurred
- `moon_id` (long, optional): Nearby moon ID if applicable

### Pending Killmails Map
**Storage**: Named map `pending_killmails` in MVStore
**Key Type**: `Long` (killmail ID)
**Value Type**: `Boolean` (always `true`)

Tracks killmails that have been fetched but not yet exported:
- Keys are Long killmail IDs
- Values are always Boolean.TRUE
- This is used as a Set (only keys matter)
- Cleared completely after each export cycle
- Used to ensure no killmails are lost between fetch and export

### Meta Map
**Storage**: Named map `meta` in MVStore
**Key Type**: `String`
**Value Type**: `String`

Stores metadata as string key-value pairs:
- **Key**: `last_export`
  - **Value**: ISO-8601 timestamp of when the last incremental export occurred
  - **Format**: "2021-07-15T14:30:45Z"
  - **Purpose**: Determines which killmails to include in the next incremental export
  - **Update Timing**: Set during section F.1 after successful export
  - **Initial Value**: Can be null/absent on first run

Additional metadata entries may exist for:
- Backfill checkpoints (not used in normal operation)

---

## Backfill Operations (Not Used in Normal Operation)

The code includes three additional modes for special operations:

### 1. Initial Backfill
- For populating the database with all historical killmails for all wars
- Processes wars in batches of 500
- Checkpoints progress regularly in case of interruption
- Can be resumed from where it left off

### 2. Year-Based Backfill Export
- Exports wars and killmails by year (2003-2021 or other ranges)
- Creates separate archive files for each year
- Useful for distributing historical data in chunks
- Runs sequentially (4 years in parallel)

### 3. Load Archive Files
- Reads previously exported TAR.BZ2 archive files
- Validates that wars and killmails match what's in the store
- Used for verification or recovery purposes

---

## Concurrency Levels

The system uses multiple levels of parallelism:

| Operation | Concurrency Level | Purpose |
|-----------|-------------------|---------|
| War Fetching | 32 | Fetch individual war details in parallel |
| War Killmail Fetching | 2 | Fetch killmail lists for different wars |
| Killmail Fetching | 4 | Fetch individual killmail details |
| Year-based Backfill | 4 | Process multiple years in parallel |

---

## File Formats

### TAR.BZ2 Archive Format (Incremental and Yearly Exports)

**Compression**: BZIP2 format (.tar.bz2 extension)

**Archive Structure**: TAR archive containing JSON files organized by war ID

**Directory Layout**:
```
wars/{warId}.json
wars/{warId}/killmails/{killmailId}.json
wars/{warId}/killmails/{killmailId}.json
wars/{warId}/killmails/{killmailId}.json
wars/{warId2}/killmails/{killmailId}.json
wars/{warId2}/killmails/{killmailId}.json
...
```

**Entry Details**:
- Wars are sorted numerically by war ID (ascending)
- Killmails within each war are sorted numerically by killmail ID (ascending)
- File content is complete JSON as stored in the database
- Each entry includes all fields (no pruning)
- Timestamps are in ISO-8601 format as returned from ESI

**War JSON File Example** (`wars/123456.json`):
```json
{
  "war_id": 123456,
  "declared": "2021-06-15T12:34:56Z",
  "started": "2021-06-15T12:34:56Z",
  "retracted": null,
  "finished": null,
  "aggressor_id": 99000123,
  "defender_id": 99000456,
  "allies": [...],
  "mutual": false,
  "open_for_allies": true,
  "hp": {...},
  "reward": 1000000,
  "http_last_modified": "2021-07-01T10:15:30Z"
}
```

**Killmail JSON File Example** (`wars/123456/killmails/987654321.json`):
```json
{
  "killmail_id": 987654321,
  "war_id": 123456,
  "killmail_hash": "abcdef1234567890",
  "killmail_time": "2021-07-01T14:30:45Z",
  "victim": {...},
  "attackers": [...],
  "solar_system_id": 30002652,
  "moon_id": null,
  "http_last_modified": "2021-07-02T10:15:30Z"
}
```

### Current Wars JSON Format

**Content Type**: `application/json`

**Structure**: Single JSON object (not an array)

**Format**:
```json
{
  "123456": { /* complete war object */ },
  "123457": { /* complete war object */ },
  "123458": { /* complete war object */ },
  ...
}
```

**Key Details**:
- Keys are war IDs as strings (JSON object keys must be strings)
- Values are complete war data objects (same format as in TAR archive)
- Includes only ongoing wars (wars without a `finished` date)
- Sorted by war ID numerically (ascending)
- Updated with every incremental export cycle

**Purpose**: Provides a quick JSON index of currently active wars without decompressing archives

---

## HTTP Headers and Response Handling

### Request Headers (All ESI Requests)

**User-Agent**: Include a descriptive user agent identifying your application
**Accept**: `application/json`
**Accept-Encoding**: Support gzip encoding for compression

### Response Headers (Capture These)

**Last-Modified**:
- Present on successful ESI responses (HTTP 200)
- Format: RFC 7231 HTTP-date (e.g., "Tue, 01 Jul 2021 10:15:30 GMT")
- Convert to ISO-8601 and store in `http_last_modified` field
- This header indicates when CCP last modified the data

**Content-Type**: Should be `application/json`

**X-Pages**: Present on paginated endpoints (wars list, killmail lists)
- Indicates total number of pages available
- Implement pagination if necessary

### HTTP Status Codes and Handling

| Status Code | Meaning | Action |
|-------------|---------|--------|
| 200 | Success | Process response, capture Last-Modified header |
| 304 | Not Modified | Optional: skip processing if using conditional requests |
| 400 | Bad Request | Log and skip (malformed request) |
| 401 | Unauthorized | Fatal error - authentication issue |
| 403 | Forbidden | Log and skip (access denied for this war/killmail) |
| 404 | Not Found | Log and skip (war/killmail doesn't exist) |
| 422 | Unprocessable Entity | For killmails: trigger Zkillboard fallback; for wars: log and skip |
| 429 | Too Many Requests | Implement backoff and retry |
| 500-503 | Server Error | Implement exponential backoff and retry |

---

## Error Handling

1. **Invalid Killmail Hashes**
   - ESI sometimes returns 422 errors for killmail hashes
   - System falls back to Zkillboard API to get the correct hash
   - If hash cannot be corrected, killmail is skipped (logged)

2. **Bad War IDs**
   - Hardcoded list of problematic war IDs are skipped entirely
   - Prevents errors during fetching

3. **Non-200 HTTP Responses**
   - Logs but doesn't fail
   - Empty data is returned gracefully

4. **MVStore Corruption**
   - Compaction after each run helps prevent corruption
   - Database is pulled fresh from S3 before each run

---

## Configuration Requirements

The system requires the following configuration values:

### Database Configuration
- **Database Path**: `/var/data/evemarket/cache/wars.mvstore`
  - Must be writable by the application
  - Directory must exist: `/var/data/evemarket/cache/`
  - Backed up to S3: `s3://eve-ref-legacy-store/db/wars.mvstore`

### S3 Configuration
- **Database Bucket**: `eve-ref-legacy-store`
- **Data Bucket**: Configured data distribution bucket (from DistributionConfig)
- **AWS SDK**: Use standard AWS credential chain
- **Region**: Should be configured for your AWS region

### HTTP Client Configuration
- **ESI Base URL**: `https://esi.evetech.net/latest/`
- **Zkillboard Base URL**: `https://r2z2.zkillboard.com/`
- **Connection Timeout**: Reasonable timeout (ESI can be slow)
- **Read Timeout**: Reasonable timeout
- **Retry Logic**: Implement backoff for rate limiting and temporary errors

### Concurrency Settings
- **War Fetching Parallelism**: 32 concurrent requests
- **War Killmail Fetching Parallelism**: 2 concurrent requests
- **Killmail Fetching Parallelism**: 4 concurrent requests
- **Thread Pool**: Use virtual threads or equivalent if available

---

## Implementation Notes

### Timestamp Handling
- All timestamps must be handled as ISO-8601 strings internally
- Store in database as received from ESI (don't convert to other formats)
- When comparing timestamps (e.g., for determining killmails to export), parse to Instant and compare
- Example parsing: `Instant.parse("2021-07-15T14:30:45Z")`

### Duplicate Handling
- Before fetching a killmail from ESI, check if it already exists in `kill_mails` map by ID
- Never fetch the same killmail twice (wastes bandwidth and API quota)
- Wars can be fetched multiple times (they're overwritten with fresh data)

### Database Commits
- Commit the MVStore after recording export metadata (section F.3)
- Commit the MVStore after clearing pending killmails
- Commit the MVStore at the end of the process before compaction
- Each commit flushes changes to disk

### Database Compaction
- After every successful run, perform the following operations (in order):
  1. `mvStore.commit()` - Flush changes
  2. `mvStore.compactRewriteFully()` - Full rewrite to optimize storage
  3. `mvStore.compactMoveChunks()` - Reorganize chunks for better performance
  4. `mvStore.close()` - Close the store cleanly
- These operations reduce file size and improve performance on next run

### Pagination Handling
- ESI endpoints may return paginated results (especially war list)
- Check the `X-Pages` header to determine if pagination is needed
- Implement fetching all pages if pagination is present
- Combine results from all pages into a single list

### Clock Skew
- Use system clock for recording export timestamps
- Don't trust client-side clock for timestamps; let ESI provide them
- When comparing timestamps, allow for small clock skew (e.g., 1-2 seconds)

### War Time Range Calculation
- A war is considered to be in the "current time range" if ANY of its key dates overlap with the current time
- Key dates: `declared`, `started`, `retracted`, `finished`
- If any of these timestamps fall within the time window being considered, keep the war
- If `finished` is null, treat it as `Instant.now()` for the comparison

---


---

## S3 Locations

**Database:**
- `s3://eve-ref-legacy-store/db/wars.mvstore` - Persistent state

**Exports:**
- `s3://data-bucket/wars/history/{YYYY}/wars-{YYYY-MM-DD_HH-MM-SS}.tar.bz2` - Incremental exports
- `s3://data-bucket/wars/wars-current.json` - Current ongoing wars
- `s3://data-bucket/wars/history/wars-{YEAR}.tar.bz2` - Yearly archives

---

## Key Design Patterns

1. **Incremental Exports**: Only exports data that's new since the last export
2. **Persistent Database**: Uses MVStore to maintain state between runs
3. **Dual-layer Versioning**: Maintains both incremental timestamped files and a "current" JSON file
4. **Graceful Degradation**: Missing or invalid killmails don't stop the entire process
5. **Cleanup**: Old data is automatically deleted to prevent unlimited growth

---

## Expected Environment

- Java application with dependency injection (Guice)
- RxJava 3 for reactive operations
- S3 access via AWS SDK
- HTTP client for ESI and Zkillboard APIs
- Local filesystem access for temp files

---

## Implementation Checklist

Use this checklist when reimplementing the UpdateWars system:

### Database Setup
- [ ] Create MVStore database with 4 maps: `wars`, `kill_mails`, `pending_killmails`, `meta`
- [ ] Configure compression (high level)
- [ ] Set cache size to 128 MB
- [ ] Set versions to keep to 0
- [ ] Implement database download from S3 before run
- [ ] Implement database upload to S3 after run

### API Integration
- [ ] Implement ESI client for fetching war IDs from `/wars/`
- [ ] Implement ESI client for fetching war details from `/wars/{war_id}/`
- [ ] Implement ESI client for fetching killmail IDs from `/wars/{war_id}/killmails/`
- [ ] Implement ESI client for fetching killmail details from `/killmails/{killmail_id}/{killmail_hash}/`
- [ ] Capture `Last-Modified` header and store as `http_last_modified`
- [ ] Implement Zkillboard fallback for 422 errors on killmails
- [ ] Implement pagination handling for paginated responses
- [ ] Implement backoff/retry for 429 and 5xx errors

### Core Logic
- [ ] Implement war exclusion rules (hardcoded bad war IDs + range)
- [ ] Implement early killmail war list check (only 4 early wars)
- [ ] Implement incremental update (unfinished + unknown wars only)
- [ ] Implement war range determination (min/max from declared/started/retracted/finished)
- [ ] Implement pending killmail tracking
- [ ] Implement cleanup of old wars (outside time range)
- [ ] Implement cleanup of orphaned killmails

### File Export
- [ ] Implement TAR.BZ2 archive creation
- [ ] Implement sorted war/killmail writing to archive
- [ ] Implement current wars JSON export
- [ ] Implement S3 uploads with correct paths and content types
- [ ] Implement file timestamps in archives

### Concurrency
- [ ] Configure parallelism: 32 for wars, 2 for killmail lists, 4 for killmails
- [ ] Use virtual threads or equivalent for parallel processing
- [ ] Implement proper error handling within parallel operations

### Testing Checklist
- [ ] Test with a single war ID
- [ ] Test with new war IDs
- [ ] Test with unfinished wars (no finished date)
- [ ] Test with wars that have all 4 timestamps
- [ ] Test with wars that have null timestamps (use Instant.now() for null)
- [ ] Test with wars that have 422 killmail hash errors
- [ ] Test Zkillboard fallback integration
- [ ] Test TAR.BZ2 archive creation and readability
- [ ] Test current wars JSON sorting
- [ ] Test database persistence across runs
- [ ] Test concurrent fetching (verify parallelism level)
- [ ] Test cleanup of old data
- [ ] Test database compaction

---

## Troubleshooting Guide

### Issue: Killmails not exporting

**Possible Causes:**
1. Killmails marked as pending but export failed
   - Check that `pending_killmails` is being cleared after successful export
   - Verify S3 upload completed without errors
2. `last_export` timestamp is null
   - On first run, set initial export time
   - Check database commit after export
3. Killmail `killmail_time` is before or equal to `last_export`
   - Check timestamp parsing (ensure ISO-8601)
   - Verify no clock skew issues

### Issue: Database growing too large

**Possible Causes:**
1. Cleanup of old data not running
   - Verify cleanup logic is executing in section H
   - Check war date range calculation logic
2. Compaction not running
   - Verify `compactRewriteFully()` and `compactMoveChunks()` are called
   - Check that MVStore is being closed properly
3. Too many killmail versions
   - Verify `mvStore.setVersionsToKeep(0)` was called on init

### Issue: Killmail hash errors (422)

**Possible Causes:**
1. Zkillboard API is down
   - ESI data may be newer than Zkillboard; add delay and retry
   - Fall back to skipping the killmail with logging
2. Killmail is "CCP VERIFIED"
   - This is expected; skip these killmails gracefully
3. Incorrect war date for Zkillboard query
   - Use `killmail_time` for Zkillboard date lookup
   - Handle time zone conversion correctly (UTC)

### Issue: High ESI API errors

**Possible Causes:**
1. Rate limiting (429 errors)
   - Implement exponential backoff
   - Reduce concurrency levels
   - Check your user agent is correct
2. War/killmail not found (404)
   - Some data may be deleted by CCP; log and continue
   - Expected behavior for very old content
3. Bad war IDs
   - Verify hardcoded exclusion list is current
   - Report new bad war IDs to issue tracker

---


## Source Code Reference

The UpdateWars system is implemented across these key classes in the `eve-ref-legacy` codebase:

- **UpdateWars.java** - Main orchestration and state management
- **WarFetcher.java** - ESI war data fetching
- **KillmailFetcher.java** - ESI killmail data fetching with Zkillboard fallback
- **WarsFileBuilder.java** - TAR.BZ2 archive creation
- **WarsFileHandler.java** - File uploads to S3
- **WarsFileReader.java** - TAR.BZ2 archive reading
- **KillmailIdAndHash.java** - Data class for killmail references
- **updateWars.sh** - Shell script entry point
