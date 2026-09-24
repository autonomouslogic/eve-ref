# Legacy UpdateKillmails System - Functional Analysis

## Overview

The legacy `UpdateKillmails` system is a batch process that maintains a daily-aggregated archive of EVE Online player killmails. It works by comparing kill counts from Zkillboard (a third-party killmail database) against locally archived data, and fetches missing or updated killmails to keep the archive current.

**Primary Goal**: Ensure EVE Ref has a complete, up-to-date archive of all killmails from EVE Online, organized by date into compressed tar.bz2 files, with a summary file tracking daily kill counts.

## System Architecture

The system consists of 8 key components working together in an orchestrated flow:

### 1. UpdateKillmails (Main Command)

This is the entry point and orchestrator for the entire process.

**Responsibilities:**
- Initialize an in-memory MVStore database with two maps:
  - `killmails`: Maps killmail IDs (Long) to killmail JSON objects
  - `totals`: Maps LocalDate to the count of killmails for that date
- Load daily killmail totals from Zkillboard
- Compute which dates need updates by comparing totals
- For each date requiring an update:
  1. Clear the in-memory killmail cache
  2. Load any previously archived killmails for that date from S3
  3. Fetch new/updated killmails from Zkillboard/ESI for that date
  4. Write all killmails (previous + new) to a tar.bz2 file
  5. Upload the file to S3
  6. Update the totals file in S3
- Report health check status on completion

**Configuration:**
- MAX_CONCURRENCY = 4 (controls parallel fetching of killmails)

**Processing Flow:**
The main run method chains these operations:
1. Load totals (clear and fetch fresh from Zkillboard)
2. For each date returned by KillmailUpdateChecker (in reverse chronological order):
   - Reset the in-memory cache
   - Load previous killmails (if file exists)
   - Fetch new killmails for the date
   - If new killmails were fetched:
     - Write to tar.bz2 file
     - Upload to S3
   - Update totals file in S3
3. Close MVStore and report completion

### 2. KillmailUpdateChecker

**Purpose**: Determine which dates require killmail updates

**Process:**
1. Fetch daily kill counts from three sources in parallel:
   - Zkillboard totals (current state of all kills)
   - EVE Ref totals (currently archived state)
   - Missing file dates (complete list of dates from EVE history with gaps where files don't exist)

2. Merge all dates from the three sources into a reverse-chronologically ordered set

3. For each date, create a KillmailUpdate object containing:
   - The date
   - Kill count on Zkillboard for that date
   - Kill count in EVE Ref archive for that date
   - Whether the file currently exists in S3

4. Filter updates to keep only dates that need processing:
   - File doesn't exist, OR
   - Zkillboard count differs from EVE Ref count
   - Exception: Ignore a hardcoded set of dates known to be missing exactly one killmail (permanently unresolved Zkillboard discrepancies)

5. Return filtered list in reverse chronological order (newest first)

**Known Issues Handled:**
Four specific dates have a known permanent discrepancy of exactly one killmail (Zkillboard count is one higher). These dates are excluded from updates to prevent infinite retry loops.

### 3. KillmailFileChecker

**Purpose**: Find all dates in EVE Online history that are missing killmail archive files

**Process:**
1. Define the date range: From Eve's release (2007-12-05) to the current UTC date
2. Build a complete list of all dates in this range
3. Query S3 for all `.tar.bz2` files at the killmails prefix
4. For each file found, extract the date portion (from filename format: `killmails/yyyy/killmails-yyyy-MM-dd.tar.bz2`)
5. Remove found dates from the list, leaving only missing dates
6. Return the missing dates in reverse chronological order

### 4. ZkillboardHelper

**Purpose**: Fetch external data from Zkillboard (a third-party EVE killmail aggregator)

**Key Methods:**

**fetchCorrectKillmailHash(killmailId)**
- Fetches the specific killmail page from Zkillboard
- Parses the HTML using regex to extract the ESI API URL embedded in the page
- Returns the killmail hash needed to fetch from ESI

**fetchTotals()**
- Fetches from `https://r2z2.zkillboard.com/history/totals.json`
- Returns a map of LocalDate → kill count for each day
- Format: JSON object with date strings (ISO format) as keys, kill counts as values

**parseTotals(ObjectNode)**
- Parses a JSON object with date strings as keys and counts as values
- Returns a Map<LocalDate, Long>

**writeTotals(Map<LocalDate, Long>)**
- Converts a map of dates → counts back into JSON format
- Used when preparing the totals file to upload to S3

### 5. KillmailHelper

**Purpose**: Manage S3 paths and fetch EVE Ref's own archived totals

**Key Methods:**

**getUriForDate(LocalDate)**
- Generates the S3 path where a date's killmail file should be stored
- Format: `s3://[bucket]/killmails/yyyy/killmails-yyyy-MM-dd.tar.bz2`
- Example: `killmails/2024/killmails-2024-01-15.tar.bz2`

**fetchTotals()**
- Fetches totals from `https://data.everef.net/killmails/totals.json`
- This is EVE Ref's own published totals (what was previously archived)
- Uses the same ZkillboardHelper parsing logic

### 6. KillmailFileReader

**Purpose**: Extract killmail JSON objects from tar.bz2 archive files

**Process:**
1. Accept either a File or InputStream containing a tar.bz2 archive
2. Decompress the BZip2 stream
3. Read tar entries sequentially
4. For each entry:
   - Skip directories
   - Skip non-JSON files
   - Skip files that don't match the pattern `killmails/[0-9]+.json`
   - For matching files: deserialize the JSON and emit as ObjectNode

5. Close the tar stream when complete

**Output**: Emits each killmail JSON as a separate ObjectNode event

### 7. KillmailFileBuilder

**Purpose**: Write killmail JSON objects into a compressed tar.bz2 archive

**Process:**
1. Open operation:
   - Create a file output stream
   - Wrap with BZip2 compression
   - Wrap with tar archive writer
   - Record the current time for all entries' modification times

2. Write killmail operation:
   - Extract the killmail_id from the JSON object
   - Create a tar entry with path: `killmails/[killmail_id].json`
   - Write the JSON as the entry content
   - Close the entry

3. Close operation:
   - Close the tar writer (which closes BZip2 and file streams)

**Archive Format**:
```
killmails-yyyy-MM-dd.tar.bz2
├── killmails/
│   ├── [id1].json
│   ├── [id2].json
│   └── ...
```

### 8. KillmailUpdate (Data Class)

**Purpose**: Represent a single date that may need killmail updates

**Fields:**
- `date`: The LocalDate being evaluated
- `zkillboardCount`: Number of kills for this date on Zkillboard (or -1 if not available)
- `everefCount`: Number of kills for this date in EVE Ref archive (or -1 if not available)
- `fileExists`: Boolean indicating whether the archive file currently exists in S3

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    UpdateKillmails.run()                        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │ Load Totals  │ (Fetch from Zkillboard)
                    └──────┬──────┘
                           │
            ┌──────────────▼──────────────┐
            │ KillmailUpdateChecker       │
            │ - Fetch Zkillboard totals   │
            │ - Fetch EVE Ref totals      │
            │ - Find missing file dates   │
            │ - Compare and filter        │
            └──────────────┬──────────────┘
                           │
        ┌──────────────────▼──────────────────┐
        │ For each date (reverse chrono):     │
        │                                      │
        │  1. Clear in-memory cache           │
        │  2. Load previous killmails         │  (KillmailFileReader)
        │  3. Fetch new killmails             │  (KillmailFetcher)
        │  4. Write archive file              │  (KillmailFileBuilder)
        │  5. Upload to S3                    │
        │  6. Update totals                   │
        └──────────────────┬──────────────────┘
                           │
            ┌──────────────▼──────────────┐
            │ Upload Killmail Totals      │
            │ (JSON: date → count)        │
            └──────────────┬──────────────┘
                           │
                    ┌──────▼──────┐
                    │ Health Check │
                    └─────────────┘
```

## Key Design Patterns

### In-Memory Cache Strategy
- Uses an MVStore (in-memory database) as a working cache
- For each date processed, the cache is cleared before loading
- This allows combining previous killmails with newly fetched ones
- Reduces S3 API calls by batching reads

### Comparison-Based Update Detection
- Doesn't track "last run" timestamps
- Instead compares counts: if Zkillboard count ≠ EVE Ref count, update is needed
- Handles both missing files and partial updates
- Self-healing: if a file was deleted, it will be regenerated

### Reverse Chronological Processing
- Processes newest dates first (most likely to have updates)
- Older dates are processed but less frequently updated
- Long-running command can be interrupted after processing recent dates

### Three-Source Reconciliation
- Zkillboard: Source of truth for all kills (third-party aggregator)
- EVE Ref totals: What we previously archived
- Missing files: Which dates lack archives entirely
- By checking all three, handles both new data and missing files

## File Storage

**Archive Files Format:**
- Location: `s3://[bucket]/killmails/yyyy/killmails-yyyy-MM-dd.tar.bz2`
- Contents: Tar archive with BZip2 compression
- File structure:
  - Each killmail is a separate JSON file: `killmails/[id].json`
  - Files are sorted by killmail ID within the archive
  - No directory structure within the archive

**Totals File:**
- Location: `s3://[bucket]/killmails/totals.json`
- Format: JSON object with date strings (YYYY-MM-DD) as keys, kill counts as values
- Example:
  ```json
  {
    "2024-01-01": 1234,
    "2024-01-02": 5678,
    ...
  }
  ```

## External Dependencies

### EVE Online ESI API
- Used indirectly via KillmailFetcher to fetch actual killmail data
- Provides: Full killmail details given killmail ID and hash

### Zkillboard (Third-Party)
- Provides: Daily killmail totals and killmail hashes
- Accessed via HTTP GET
- Website HTML parsing for killmail hash extraction
- JSON API for daily totals

### S3 (AWS Object Storage)
- Stores: Archived killmail files and totals
- Configured bucket: From DistributionConfig.getDataBucket()

## Configuration Requirements

- `DistributionConfig.getDataBucket()`: S3 bucket name for killmail storage
- `EsiConfig`: Configuration for ESI API access (used by KillmailFetcher)
- S3 credentials: Named "data" for accessing the distribution bucket

## Known Limitations & Hardcoded Values

1. **Four Known Discrepancies**: The code contains a hardcoded set of four dates where Zkillboard permanently has one more kill than EVE Ref. These are excluded from updates to prevent infinite loops:
   - 2019-12-06
   - 2018-09-14
   - 2018-06-10
   - 2018-06-08

2. **EVE Release Date**: Killmail history starts from 2007-12-05. Dates before this are never checked.

3. **MAX_CONCURRENCY = 4**: Limits parallel killmail fetching to avoid overwhelming ESI API

4. **Regex Patterns Used:**
   - ESI killmail URL: `esi\.evetech\.net\/latest\/killmails\/[0-9]+\/[0-9a-f]+\/`
   - Archive filename: `killmails/[0-9]+\.json`

## Metrics & Logging

The system logs:
- MVStore map sizes on initialization
- Each KillmailUpdate being processed
- Count of killmails loaded from previous files
- Count of killmails fetched for each date
- Count of killmails in store before writing
- Archive file creation and upload operations
- Success/failure of totals upload
- Health check status

## Error Handling

- If any stage fails, the entire run fails and is reported via Slack
- No partial commit: Either the full day's update succeeds, or the previous state is retained
- Resource cleanup: MVStore is always closed, even if errors occur
- S3 failures: Will be retried on the next run
