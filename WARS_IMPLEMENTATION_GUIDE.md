# Wars Scraping Implementation Guide

## Status

This guide provides a comprehensive implementation plan for the wars scraping system. A baseline implementation has been started with partial compilation - this guide documents all remaining work and corrections needed.

## What's Been Completed

✅ **Configuration Setup**
- Added WARS constant to ArchivePathFactory.java
- Added 11 new Config values to Configs.java (war database path, AWS credentials, concurrency levels, retention)
- Added wars-db S3 client providers to AwsModule.java
- Added wars-db S3AsyncClient provider to S3Module.java

✅ **Project Structure Created**
- Package: `com.autonomouslogic.everef.cli.wars/`
- Core classes skeleton created (partial):
  - ScrapeWars.java (main orchestrator)
  - WarsFetcher.java (war fetching)
  - KillmailFetcher.java (killmail fetching)
  - ZkillboardHashCorrector.java (hash corrections)
  - WarsFileBuilder.java (archive building)
  - WarsFetchScope.java (fetch scope calculation)

✅ **Basic Test Structure**
- WarsFetcherTest.java created with unit test skeleton

## Critical Fixes Needed

### 1. API Signature Corrections

#### WarsApi.getWarsWarIdWithHttpInfo
**Current (WRONG):**
```java
warsApi.getWarsWarIdWithHttpInfo(warId, null)
```

**Correct Signature:**
```
public ApiResponse<GetWarsWarIdOk> getWarsWarIdWithHttpInfo(
    Integer warId,           // NOT long - must be Integer
    String datasource,       // Can be null
    String ifNoneMatch      // Can be null
)
```

**Fix:**
```java
warsApi.getWarsWarIdWithHttpInfo(Math.toIntExact(warId), null, null)
```

#### KillmailsApi.getKillmailsKillmailIdKillmailHash
**Current (WRONG):**
```java
killmailsApi.getKillmailsKillmailIdKillmailHash(killmailId, hash, null)
```

**Correct Signature:**
```
public GetKillmailsKillmailIdKillmailHashOk getKillmailsKillmailIdKillmailHash(
    String killmailHash,     // The hash comes FIRST
    Integer killmailId,      // Integer, not long
    String datasource,       // Can be null
    String ifNoneMatch      // Can be null
)
```

**Fix:**
```java
killmailsApi.getKillmailsKillmailIdKillmailHash(hash, Math.toIntExact(killmailId), null, null)
```

#### WarsApi.getWarsWarIdKillmailsWithHttpInfo
**Current (WRONG):**
```java
warsApi.getWarsWarIdKillmailsWithHttpInfo(warId, 200, page, "asc", null)
```

**Correct Parameters:**
- warId: Integer (not long)
- limit: Integer
- page: Integer
- order: String ("asc"/"desc")
- datasource: String (nullable)

**Fix:**
```java
warsApi.getWarsWarIdKillmailsWithHttpInfo(
    Math.toIntExact(warId),
    200,
    page,
    "asc",
    null
)
```

#### WarsApi.getWarsWithHttpInfo
**Correct Signature:**
```
public ApiResponse<List<Integer>> getWarsWithHttpInfo(
    Integer limit,      // Items per page
    Integer page,       // Page number
    String order,       // "asc" or "desc"
    String datasource   // Can be null
)
```

### 2. EsiHelper.fetchPages() Pattern Fix

**Current (WRONG):**
```java
esiHelper.fetchPages(
    page -> warsApi.getWarsWarIdKillmailsWithHttpInfo(warId, 200, page, "asc", null),
    response -> response.getData().stream().map(...).toList()
)
```

**Correct Pattern:**
```java
esiHelper.fetchPages(
    page -> warsApi.getWarsWarIdKillmailsWithHttpInfo(
        Math.toIntExact(warId),
        200,
        page,
        "asc",
        null
    )
)
```

EsiHelper.fetchPages() only takes the Function that returns the API response. The mapping to extract data is handled internally.

### 3. Dependency Injection Fixes

**Problem:** Direct instantiation of S3Adapter and EsiHelper

**Current (WRONG):**
```java
new com.autonomouslogic.everef.s3.S3Adapter()
new com.autonomouslogic.everef.esi.EsiHelper()
```

**Fix:** Inject these as dependencies
```java
@Inject
protected S3Adapter s3Adapter;

@Inject
protected EsiHelper esiHelper;
```

Then use them directly:
```java
s3Adapter.getObject(request, dbPath, warsDbS3Client)
esiHelper.fetchPages(...)
```

### 4. OkHttpWrapper Response Handling

**Problem:** Wrong method call for parsing HTTP response

**Current (WRONG):**
```java
var json = objectMapper.readTree(response);  // response is String
```

**Fix:**
```java
var json = objectMapper.readTree(response);  // response is already String, this is correct
```

Or if response is actually a Response object:
```java
var body = response.body().string();
var json = objectMapper.readTree(body);
```

Check the actual OkHttpWrapper.get() return type.

### 5. TempFiles Usage

**Problem:** Method name doesn't exist

**Current (WRONG):**
```java
tempFiles.createFile()  // Method doesn't exist
```

**Fix:** Check TempFiles class for actual method, likely:
```java
File.createTempFile("prefix", ".suffix")
// or
tempFiles.createTempDirectory()
// or similar pattern
```

### 6. MVStore Compaction

**Problem:** Methods don't exist

**Current (WRONG):**
```java
mvStore.compactRewriteFully()
mvStore.compactMoveChunks()
mvStore.getFileName()
```

**Fix:** Use actual MVStore API:
```java
mvStore.commit()
mvStore.compact()  // Single method, not multiple
// For filename, store it separately when creating MVStore
```

## Detailed Implementation Steps

### Step 1: Fix WarsFetchScope.java

```java
private static Set<Long> fetchAllWarIds(WarsApi warsApi, EsiHelper esiHelper) {
    return esiHelper.fetchPages(
        page -> warsApi.getWarsWithHttpInfo(1000, page, "asc", null)
    );
}
```

### Step 2: Fix WarsFetcher.java

- Convert `long warId` to `Integer` for API calls using `Math.toIntExact(warId)`
- Fix method signature calls with correct parameter order
- Inject `EsiHelper` instead of creating it

### Step 3: Fix KillmailFetcher.java

- Add `@Inject` for `EsiHelper`
- Fix KillmailsApi call: hash comes BEFORE killmailId
- Fix parameter order for all API calls
- Convert long IDs to Integer where needed

### Step 4: Fix ScrapeWars.java

- Inject `S3Adapter` instead of creating it
- Inject `EsiHelper` instead of creating it
- Store filename when creating MVStore
- Fix MVStore compaction logic
- Fix TempFiles usage

### Step 5: Fix WarsFileBuilder.java

- Fix TempFiles usage
- Inject it properly with `@Inject`

### Step 6: Fix ZkillboardHashCorrector.java

- Verify OkHttpWrapper response type
- Fix JSON parsing accordingly

## Testing Strategy

After fixes:

```bash
# Compile only
./gradlew compileJava

# Run unit tests
make test

# Run specific test
./gradlew test --tests WarsFetcherTest
```

## Key API Notes

### War IDs
- Store/retrieve as Long in code
- Convert to Integer for API calls
- Bad wars to skip: 90591, 473095, range 472167-473147

### Killmail IDs
- Store/retrieve as Long in code
- Convert to Integer for API calls
- Hash is a String parameter that comes BEFORE killmailId in API

### Early Wars
- Wars < 149786 only have killmails if they're in: {48074, 138678, 144630, 149785}
- Newer wars (>= 149786) may have killmails

### Zkillboard
- Format: https://r2z2.zkillboard.com/history/YYYYMMDD.json
- Returns map of killmailId (as String) → hash (as String)
- Special value: "CCP VERIFIED" means skip this killmail

## MVStore Schema

```
wars: Map<Long, JsonNode>           // war_id → war details
kill_mails: Map<Long, JsonNode>     // killmail_id → killmail details
pending_killmails: Map<Long, Boolean> // killmail_id → true (awaiting export)
meta: Map<String, String>           // "last_export" → ISO timestamp
```

## Configuration Required

Set these environment variables before running:

```bash
# Data output
DATA_PATH=s3://data-bucket/

# Wars database storage
WARS_DB_PATH=s3://wars-db-bucket/wars.mvstore
WARS_DB_AWS_REGION=us-east-1
WARS_DB_AWS_PROFILE=default  # optional

# Or use direct credentials
WARS_DB_AWS_ACCESS_KEY_ID=...
WARS_DB_AWS_SECRET_ACCESS_KEY=...

# Processing parameters
WARS_DATA_RETENTION=P365D  # 365 days
WARS_FETCH_CONCURRENCY=32
KILLMAIL_LIST_CONCURRENCY=2
KILLMAIL_DETAIL_CONCURRENCY=4
```

## Next Steps for Implementation

1. **Immediate fixes** (compilation):
   - Fix all API signatures per section 1 above
   - Add `@Inject` dependencies
   - Fix TempFiles and MVStore usage

2. **Testing fixes**:
   - Update WarsFetcherTest to use correct API signatures
   - Add mocks for all dependencies
   - Add integration tests

3. **Edge cases to handle**:
   - Wars with no killmails
   - Killmails marked as "CCP VERIFIED"
   - Zkillboard unavailable (network errors)
   - MVStore corruption recovery
   - S3 upload failures

4. **Performance**:
   - Verify VirtualThreads parallelism (32 wars, 2 killmail lists, 4 details)
   - Test with 10,000+ wars
   - Monitor memory usage with large MVStore

## File Locations

**Modified files:**
- `src/main/java/com/autonomouslogic/everef/util/ArchivePathFactory.java` - Added WARS constant
- `src/main/java/com/autonomouslogic/everef/config/Configs.java` - Added war configs
- `src/main/java/com/autonomouslogic/everef/inject/AwsModule.java` - Added wars-db credentials
- `src/main/java/com/autonomouslogic/everef/inject/S3Module.java` - Added wars-db S3 client

**New files (partial, need fixes):**
- `src/main/java/com/autonomouslogic/everef/cli/wars/ScrapeWars.java`
- `src/main/java/com/autonomouslogic/everef/cli/wars/WarsFetcher.java`
- `src/main/java/com/autonomouslogic/everef/cli/wars/KillmailFetcher.java`
- `src/main/java/com/autonomouslogic/everef/cli/wars/ZkillboardHashCorrector.java`
- `src/main/java/com/autonomouslogic/everef/cli/wars/WarsFileBuilder.java`
- `src/main/java/com/autonomouslogic/everef/cli/wars/WarsFetchScope.java`

**Test files:**
- `src/test/java/com/autonomouslogic/everef/cli/wars/WarsFetcherTest.java` (skeleton)

## Summary

The architecture and configuration have been set up correctly. The main work remaining is fixing API call signatures, dependency injection, and completing the implementation of each class. All the high-level structure is in place; the detailed work involves correcting parameter types and orders to match the actual ESI OpenAPI signatures.
