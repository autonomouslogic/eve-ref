# Implementation Plan: Killmail Scraping System

## Overview

Implement a daily killmail scraping system that maintains a complete archive of EVE Online killmails organized by date in tar.bz2 files, following the architecture described in `LEGACY_KILLMAILS_ANALYSIS.md`.

## Architecture Summary

**Main Flow:**
1. Fetch daily totals from Zkillboard
2. Compare with EVE Ref totals to find dates needing updates
3. For each date (reverse chronological):
   - Load previous killmails from S3
   - Fetch killmail IDs/hashes from Zkillboard History API
   - Fetch killmail details from ESI
   - Build tar.bz2 archive
   - Upload to S3
4. Upload updated totals.json

**File Structure:**
- Archives: `killmails/yyyy/killmails-yyyy-MM-dd.tar.bz2`
- Totals: `killmails/totals.json`
- Internal: `killmails/[id].json` (within tar)

## Components to Implement

### Package: `src/main/java/com/autonomouslogic/everef/cli/killmails/`

### 1. ScrapeKillmails.java (Main Command)

**Type:** Command implementation
**Pattern:** Similar to ScrapePublicContracts.java

**Key Responsibilities:**
- Initialize in-memory MVStore with two maps: `killmailsMap` (Long→JsonNode), `totalsMap` (LocalDate→Long)
- Orchestrate full workflow
- Clean up resources in finally block

**Dependencies:**
```java
@Inject Provider<KillmailUpdateChecker> updateCheckerProvider
@Inject Provider<KillmailFileReader> fileReaderProvider
@Inject Provider<KillmailFileBuilder> fileBuilderProvider
@Inject ZkillboardHelper zkillboardHelper
@Inject KillmailHelper killmailHelper
@Inject MVStoreUtil mvStoreUtil
@Inject S3Util s3Util
@Inject S3Adapter s3Adapter
@Inject @Named("data") S3AsyncClient s3Client
@Inject OkHttpWrapper okHttpWrapper
@Inject ObjectMapper objectMapper
@Inject UrlParser urlParser
@Inject TempFiles tempFiles
```

**Main Flow:**
```java
@Override
public void run() {
    VirtualThreads.checkThread();
    try {
        initMvStore();
        loadTotalsFromZkillboard();
        var updates = updateChecker.getUpdates(totalsMap);
        processUpdates(updates);
        uploadTotals();
    } finally {
        closeMvStore();
    }
}
```

**Per-Date Processing:**
1. Clear killmailsMap
2. Load previous killmails from S3 (KillmailFileReader)
3. Fetch killmail IDs from Zkillboard History API (`https://r2z2.zkillboard.com/history/YYYYMMDD.json`)
4. Fetch new killmail details from ESI (VirtualThreads.parallel with concurrency=4)
5. Build tar.bz2 (KillmailFileBuilder)
6. Upload to S3 (custom upload, NOT S3Util.uploadLatestAndArchive)
7. Update totalsMap entry

**Reference:** `/src/main/java/com/autonomouslogic/everef/cli/publiccontracts/ScrapePublicContracts.java`

---

### 2. ZkillboardHelper.java

**Type:** Singleton utility
**Pattern:** HTTP client wrapper

**Key Methods:**
```java
// Fetch daily kill counts
Map<LocalDate, Long> fetchTotals()
// GET https://r2z2.zkillboard.com/history/totals.json

// Fetch killmail IDs and hashes for a specific date
List<KillmailEntry> fetchKillmailsForDate(LocalDate date)
// GET https://r2z2.zkillboard.com/history/YYYYMMDD.json
// Returns list of {killmail_id, hash} objects

// Parse totals JSON (date strings → counts)
Map<LocalDate, Long> parseTotals(ObjectNode node)

// Write totals JSON (for upload)
ObjectNode writeTotals(Map<LocalDate, Long> totals)

// Extract killmail hash from Zkillboard HTML page (for hash correction)
Optional<String> fetchCorrectKillmailHash(long killmailId)
// GET https://zkillboard.com/kill/{id}/
// Parse HTML with regex: "esi\.evetech\.net/latest/killmails/[0-9]+/([0-9a-f]+)/"
```

**Data Class:**
```java
@Value
class KillmailEntry {
    long killmailId;
    String hash;
}
```

**Dependencies:** OkHttpWrapper, ObjectMapper

---

### 3. KillmailUpdateChecker.java

**Type:** Request-scoped helper
**Pattern:** Comparison logic

**Key Responsibilities:**
- Perform 3-source reconciliation (Zkillboard, EVE Ref, missing files)
- Filter dates needing updates
- Exclude known discrepancies
- Return sorted list (reverse chronological)

**Key Method:**
```java
List<KillmailUpdate> getUpdates(Map<LocalDate, Long> zkillboardTotals)
```

**Filtering Logic:**
- Include if: file missing OR Zkillboard count ≠ EVE Ref count
- Exclude if: known discrepancy date AND difference is exactly +1

**Known Discrepancies:**
```java
private static final Set<LocalDate> KNOWN_DISCREPANCIES = Set.of(
    LocalDate.of(2019, 12, 6),
    LocalDate.of(2018, 9, 14),
    LocalDate.of(2018, 6, 10),
    LocalDate.of(2018, 6, 8)
);
```

**Dependencies:** ZkillboardHelper, KillmailHelper, KillmailFileChecker

**Parallelism:** Use VirtualThreads.parallel to fetch 3 sources concurrently

---

### 4. KillmailFileChecker.java

**Type:** Request-scoped helper
**Pattern:** S3 listing and comparison

**Key Responsibilities:**
- Generate complete date range (2007-12-05 to today)
- List existing files from S3
- Return dates with missing files

**Key Method:**
```java
Set<LocalDate> findMissingDates()
```

**File Pattern Regex:**
```java
Pattern.compile("killmails/(\\d{4})/(\\d{4}-\\d{2}-\\d{2})\\.tar\\.bz2")
```

**Dependencies:** S3Adapter, S3AsyncClient (named "data"), UrlParser

---

### 5. KillmailHelper.java

**Type:** Singleton utility
**Pattern:** Path and totals helper

**Key Methods:**
```java
// Generate S3 URL for a date's killmail file
S3Url getUrlForDate(LocalDate date)
// Uses ArchivePathFactory.KILLMAILS.createArchivePath(date)

// Fetch EVE Ref's published totals
Map<LocalDate, Long> fetchTotals()
// GET https://data.everef.net/killmails/totals.json
// Returns empty map if 404
```

**Dependencies:** UrlParser, OkHttpWrapper, ZkillboardHelper (for parsing)

---

### 6. KillmailFileReader.java

**Type:** Request-scoped helper
**Pattern:** Tar.bz2 decompression

**Key Responsibilities:**
- Download tar.bz2 from S3
- Decompress (BZip2 → Tar)
- Extract killmail JSON files
- Populate killmailsMap

**Key Method:**
```java
int loadKillmailsForDate(LocalDate date, Map<Long, JsonNode> killmailsMap, S3Url fileUrl)
```

**Stream Chain:**
```
FileInputStream → BZip2CompressorInputStream → TarArchiveInputStream
```

**Entry Pattern:**
```java
Pattern.compile("killmails/([0-9]+)\\.json")
```

**Dependencies:** S3Adapter, S3AsyncClient, OkHttpWrapper, ObjectMapper, TempFiles

**Reference:** `/src/main/java/com/autonomouslogic/everef/cli/publiccontracts/ContractsFileLoader.java`

---

### 7. KillmailFileBuilder.java

**Type:** Request-scoped helper
**Pattern:** Tar.bz2 compression

**Key Responsibilities:**
- Sort killmails by ID (deterministic output)
- Write to tar archive
- Compress with bzip2

**Key Method:**
```java
File buildFileForDate(LocalDate date, Map<Long, JsonNode> killmailsMap)
```

**Implementation:**
1. Create tar file in temp directory
2. Iterate killmailsMap (sorted by key)
3. For each: create entry `killmails/{id}.json`, write JSON bytes
4. Close tar
5. Compress with bzip2 (CompressUtil.compressBzip2)
6. Return compressed file

**Dependencies:** ObjectMapper, TempFiles, CompressUtil

**Reference:** `/src/main/java/com/autonomouslogic/everef/cli/publiccontracts/ContractsFileBuilder.java`

---

### 8. KillmailUpdate.java

**Type:** Immutable data class

```java
@Value
@Builder
public class KillmailUpdate {
    @NonNull LocalDate date;
    long zkillboardCount;  // -1 if not available
    long everefCount;      // -1 if not available
    boolean fileExists;
}
```

---

## Key Implementation Details

### S3 Upload Strategy

**DO NOT use S3Util.uploadLatestAndArchive()** - killmails use date-based paths without latest/archive pattern.

**Custom Upload:**
```java
// For killmail files
var putRequest = s3Util.putPublicObjectRequest(
    file.length(),
    killmailHelper.getUrlForDate(date),
    "application/x-bzip2",
    Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired()
);
s3Adapter.putObject(putRequest, file, s3Client);

// For totals.json
S3Url totalsUrl = (S3Url) urlParser.parse(
    Configs.DATA_PATH.getRequired() + "/killmails/totals.json"
);
var putRequest = s3Util.putPublicObjectRequest(
    file.length(),
    totalsUrl,
    "application/json",
    Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired()
);
s3Adapter.putObject(putRequest, totalsFile, s3Client);
```

### Concurrency Strategy

**Level 1: Update Check Sources (3-way parallel)**
```java
VirtualThreads.parallel(List.of(
    () -> zkillboardHelper.fetchTotals(),
    () -> killmailHelper.fetchTotals(),
    () -> killmailFileChecker.findMissingDates()
), 3);
```

**Level 2: Killmail Fetching (4-way parallel)**
```java
var tasks = killmailEntries.stream()
    .map(entry -> (Callable<JsonNode>) () ->
        fetchKillmailFromEsi(entry.getKillmailId(), entry.getHash())
    )
    .collect(Collectors.toList());
var results = VirtualThreads.parallel(tasks, 4);
```

**Level 3: Date Processing (sequential)**
- Process dates one at a time in reverse chronological order
- Allows interruption after processing recent dates

### MVStore Pattern

```java
// Initialize
mvStore = mvStoreUtil.createTempStore("killmails");
killmailsMap = mvStoreUtil.openJsonMap(mvStore, "killmails", Long.class);
totalsMap = mvStoreUtil.openJsonMap(mvStore, "totals", LocalDate.class);

// Clean up (in finally block)
if (mvStore != null && !mvStore.isClosed()) {
    mvStore.close();
}
```

**Per-Date Cache Reset:**
```java
killmailsMap.clear(); // Before processing each date
```

### ESI Killmail Fetching

**Endpoint:** `GET /killmails/{killmail_id}/{killmail_hash}/`

**Implementation:**
```java
private JsonNode fetchKillmailFromEsi(long killmailId, String hash) {
    var url = String.format(
        "https://esi.evetech.net/latest/killmails/%d/%s/",
        killmailId, hash
    );
    try (var response = okHttpWrapper.get(url)) {
        if (response.code() == 200) {
            return objectMapper.readTree(response.body().string());
        }
        if (response.code() == 422) {
            // Try hash correction via Zkillboard
            var correctedHash = zkillboardHelper.fetchCorrectKillmailHash(killmailId);
            if (correctedHash.isPresent()) {
                return fetchKillmailFromEsi(killmailId, correctedHash.get());
            }
        }
        throw new RuntimeException("Failed to fetch killmail: " + response.code());
    }
}
```

**Error Handling:**
- 200: Success, parse JSON
- 422: Hash mismatch, try Zkillboard correction
- 404: Killmail not found, skip
- 5xx: Retry with backoff (implement retry logic)

### Error Handling Strategy

**Per-Date Isolation:**
```java
for (KillmailUpdate update : updates) {
    try {
        processDate(update);
    } catch (Exception e) {
        log.error("Failed to process date {}", update.getDate(), e);
        // Continue with next date
    }
}
```

**Resource Cleanup:**
- MVStore: Close in finally block
- Temp files: Use TempFiles utility (auto-cleanup)
- HTTP responses: Use try-with-resources

---

## Configuration

**Existing configs to reuse:**
- `DATA_PATH` - Base S3 URL
- `DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE` - Cache control for archives
- `DATA_LATEST_CACHE_CONTROL_MAX_AGE` - Cache control for totals
- `MVSTORE_CACHE_SIZE_MB` - MVStore memory limit

**New config (add to Configs.java):**
```java
public static final Config<Integer> KILLMAILS_MAX_CONCURRENCY = Config.<Integer>builder()
    .name("KILLMAILS_MAX_CONCURRENCY")
    .type(Integer.class)
    .defaultValue(4)
    .build();
```

---

## Reusable Components

**Confirmed reuse:**
- `ArchivePathFactory.KILLMAILS` - Already exists (lines 59-66)
- `S3Util` - For S3 request building
- `S3Adapter` - For S3 operations
- `MVStoreUtil` - For MVStore creation
- `TempFiles` - For temp file management
- `OkHttpWrapper` - For HTTP requests
- `ObjectMapper` - For JSON parsing
- `CompressUtil` - For bzip2 compression
- `VirtualThreads` - For concurrency
- `UrlParser` - For S3 URL parsing

---

## Testing Strategy

### Unit Tests (High Priority)

**1. ZkillboardHelperTest.java**
- Test totals JSON parsing
- Test killmails-for-date JSON parsing
- Test hash extraction regex
- Mock HTTP responses with MockWebServer

**2. KillmailUpdateCheckerTest.java**
- Test 3-source reconciliation
- Test filtering logic
- Test known discrepancy handling
- Mock all dependencies

**3. KillmailFileCheckerTest.java**
- Test date range generation
- Test filename regex extraction
- Mock S3 listing

**4. KillmailFileBuilderTest.java & KillmailFileReaderTest.java**
- Test round-trip: build → read
- Test tar.bz2 structure
- Test JSON serialization
- Use real temp files

### Integration Tests (Medium Priority)

**5. ScrapeKillmailsTest.java**
- Test full workflow with mocked dependencies
- Test MVStore cleanup
- Test error handling

---

## Critical Files for Reference

1. **Command Pattern:**
   `/src/main/java/com/autonomouslogic/everef/cli/publiccontracts/ScrapePublicContracts.java`
   Lines 83-100: Initialization and run flow

2. **Tar.bz2 Writing:**
   `/src/main/java/com/autonomouslogic/everef/cli/publiccontracts/ContractsFileBuilder.java`
   Full file: Entry creation, compression workflow

3. **Tar.bz2 Reading:**
   `/src/main/java/com/autonomouslogic/everef/cli/publiccontracts/ContractsFileLoader.java`
   Full file: Decompression, entry extraction

4. **Path Factory:**
   `/src/main/java/com/autonomouslogic/everef/util/ArchivePathFactory.java`
   Lines 59-66: KILLMAILS definition
   Lines 299-310: createArchivePath() method

5. **Business Logic:**
   `/LEGACY_KILLMAILS_ANALYSIS.md`
   Full document: Update detection, 3-source reconciliation, known discrepancies

---

## Implementation Order

1. **ZkillboardHelper** - Foundation for external data fetching
2. **KillmailHelper** - S3 path utilities
3. **KillmailUpdate** - Simple data class
4. **KillmailFileChecker** - S3 listing and date range
5. **KillmailUpdateChecker** - Comparison logic (depends on 1-4)
6. **KillmailFileBuilder** - Tar.bz2 writing
7. **KillmailFileReader** - Tar.bz2 reading
8. **ScrapeKillmails** - Main orchestrator (depends on all)
9. **Unit Tests** - For all components

---

## Verification Plan

**After implementation:**

1. **Compile:** `./gradlew compileJava compileTestJava`
2. **Run tests:** `./gradlew test --tests *Killmail*`
3. **Manual test:** Run ScrapeKillmails locally with a small date range
4. **Verify S3 uploads:**
   - Check `killmails/yyyy/killmails-yyyy-MM-dd.tar.bz2` structure
   - Check `killmails/totals.json` content
5. **Verify tar.bz2 contents:** Extract and inspect JSON files
6. **Test update detection:** Run twice, verify no unnecessary re-uploads

---

## Key Architectural Decisions

1. **In-memory MVStore** - Temporary working cache, not persistent
2. **Custom S3 upload** - Not using uploadLatestAndArchive pattern
3. **Two-level parallelism** - Sources (3-way) and killmails (4-way)
4. **Sequential date processing** - Reverse chronological, interruptible
5. **Per-date isolation** - Continue on failure, each date independent
6. **Zkillboard History API** - Primary source for killmail ID/hash discovery
7. **ESI direct fetch** - Primary source for killmail JSON content

---

## Notes

- **Known limitation:** 4 hardcoded dates with permanent Zkillboard +1 discrepancy
- **EVE launch date:** 2007-12-05 (hardcoded start of date range)
- **Date format:** LocalDate.of(year, month, day) in code, YYYYMMDD for Zkillboard API
- **No data index updates:** Killmails don't use the standard latest/archive pattern
