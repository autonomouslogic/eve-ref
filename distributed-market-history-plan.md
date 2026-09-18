# Distributed Market History Scrape — Implementation Plan

## Background

The ESI market history endpoint (`/markets/{region_id}/history/?type_id={type_id}`) is rate-limited
to **5 requests/second per IP**. The current `ScrapeMarketHistory` command runs on a single machine
and takes almost the entire day to finish. Permission has been obtained to run multiple workers from
separate IPs simultaneously, each limited to 5 req/s.

The total search space is roughly **1.7–2 million valid region/type pairs**. The existing sources
intelligently select ~370k of the highest-value pairs per daily cycle rather than attempting all of
them. At 5 req/s per worker, fetching 370k pairs takes ~20 hours on one worker; two workers halve
that; ten bring it under 2 hours. If coverage is expanded toward the full 2M space in future, the
distributed architecture scales accordingly — 10 workers could cover the full space in ~11 hours.

---

## Goals

1. Central daemon manages the pair queue, stores results in MVStore, and uploads daily archives to S3.
2. Worker daemons fetch from ESI and POST results back to the daemon.
3. No pair is assigned to two workers simultaneously; timed-out leases are retried.
4. The daemon runs a daily cycle tied to EVE downtime: queue clears at 11:00 UTC, sources re-run at
   11:05 UTC, workers fetch all day until the next reset.
5. All components are well-separated for unit testing; a full in-JVM integration test exercises a live
   daemon plus multiple workers.

---

## High-Level Architecture

```
[ESI] <──── 5 req/s ────< [Worker A] \
[ESI] <──── 5 req/s ────< [Worker B] ─── HTTP ──> [Daemon] ──> [S3]
[ESI] <──── 5 req/s ────< [Worker C] /
```

Workers are long-running daemons. They maintain a small local queue of leased pairs and top it up
from the daemon whenever it runs low. Fetched entries are POSTed back to the daemon as they arrive.
The daemon stores entries in MVStore and periodically uploads updated daily archives to S3, **while
still accepting new records from workers**.

---

## New Commands

| Command | Class | Description |
|---|---|---|
| `market-history-daemon` | `MarketHistoryDaemon` | Long-running central daemon |
| `market-history-worker` | `MarketHistoryWorker` | Long-running worker per machine |

Both live under `src/main/java/com/autonomouslogic/everef/cli/markethistory/distributed/`.

---

## New Packages

```
cli/markethistory/distributed/
  MarketHistoryDaemon.java          Command — starts HTTP server and background threads
  MarketHistoryWorker.java          Command — fetch loop + top-up loop
  daemon/
    DaemonHttpServer.java           Helidon SE server setup (separate from ApiRunner)
    QueueManager.java               Thread-safe queue + lease lifecycle
    QueueReplenisher.java           Runs sources on a schedule, adds new pairs to queue
    RecordReceiver.java             Decodes verbatim ESI body, enriches, writes to MVStore
    ArchiveUploader.java            Periodic S3 upload (snapshot-based, concurrent-safe)
    DaemonApiKeyAuth.java           Helidon filter for X-Api-Key validation
  worker/
    WorkerHttpClient.java           OkHttp client wrapping daemon API
    EsiFetchLoop.java               Fetches ESI at capped rate, posts results
    WorkerQueueManager.java         Local queue, triggers top-up when low
```

---

## Component Design

### QueueManager

Core data structure for the daemon. All methods are thread-safe.

**Cycle states:**

The queue has an overall cycle state (not per-pair), driven by the daily EVE downtime schedule:

```
ACTIVE ──(11:00 UTC)──> DRAINING ──(11:05 UTC)──> REPLENISHING ──(sources done)──> ACTIVE
```

- **ACTIVE**: normal operation. Leases issued and submissions accepted.
- **DRAINING**: queue cleared; submissions still accepted (workers finishing in-flight leases);
  no new leases issued. `/lease` returns an empty pair list with a `"draining": true` flag so
  workers know to idle rather than retry immediately.
- **REPLENISHING**: sources are running to build the new pair set. No leases issued. `/lease`
  returns `"replenishing": true`. Transitions to ACTIVE automatically when sources finish.

**Pair lifecycle (within ACTIVE state):**

```
PENDING ──> IN_FLIGHT ──> DONE
               │
               └──(timeout)──> front of PENDING queue
```

Timed-out pairs are re-inserted at the **front** of the pending queue so they are retried before
newly queued pairs (they are higher-priority because their cache data is more likely to be fresh).

**Implementation:**

Two data structures work together:

- `ConcurrentLinkedDeque<RegionTypePair> pendingQueue` — ordered FIFO for new pairs; `addFirst` for
  timed-out re-inserts.
- `ConcurrentHashMap<RegionTypePair, Instant> inFlight` — maps in-flight pair to its expiry
  `Instant`. Presence in this map means the pair is currently being fetched by a worker.
- A `ConcurrentHashSet<RegionTypePair> done` (or a separate map) tracks pairs completed this cycle
  to prevent re-queuing by `populate()` in edge cases.

Key methods:
- `lease(): List<RegionTypePair>` — polls up to `BATCH_SIZE` pairs from the front of `pendingQueue`,
  adds each to `inFlight` with expiry `Instant.now() + LEASE_TIMEOUT`. Returns empty list (with
  state flag) when cycle state is not ACTIVE.
  - **Rollback on error:** pairs move out of `pendingQueue` into `inFlight` before the method
    returns. If anything throws after some pairs have been polled/added (or the endpoint handler
    fails serializing the response), those pairs would be stranded in `inFlight` until the reaper
    timeout. `lease()` wraps the poll/add work in try/catch: on exception, it removes each
    already-leased pair from `inFlight` and re-inserts it at the **front** of `pendingQueue`
    (`addFirst`, preserving order), then rethrows. The `/lease` endpoint handler catches the
    rethrown exception and returns HTTP `500` — the pairs are already back on the queue for the
    next request. `releasePairs(List<RegionTypePair>)` is the reusable helper for this rollback
    (also used by the endpoint handler if it fails after `lease()` succeeds).
- `submit(List<RegionTypePair> pairs)` — removes pairs from `inFlight`, adds to `done`. Accepted in
  all cycle states. Daemon derives the pair identity from the region/type IDs in the submitted
  entries — no lease ID required.
- `clear()` — called at 11:00: clears `pendingQueue`, `inFlight`, and `done`; sets state to
  DRAINING.
- `populate(List<RegionTypePair> pairs)` — bulk-adds all pairs to the back of `pendingQueue` **in
  list order** (`addLast` per element), sets state to ACTIVE. **Order is significant:** the source
  chain returns pairs in priority order (higher-value pairs first), and `pendingQueue` is a FIFO
  leased from the front — so pairs at the start of the list are fetched first. Must be a `List`, not
  a `Set`, to preserve that ordering.
- `reapExpiredPairs()` — scans `inFlight` for entries where `expiry.isBefore(Instant.now())`,
  removes from `inFlight`, calls `pendingQueue.addFirst()` for each. Called by background thread
  every 60s.
- `pendingCount()`, `inFlightCount()`, `doneCount()`, `cycleState()` — for stats endpoint.

**Config:**
- `MARKET_HISTORY_DAEMON_LEASE_TIMEOUT` — default `PT10M`
- `MARKET_HISTORY_DAEMON_BATCH_SIZE` — default `200` (daemon-controlled; workers request work with no size hint)
- `MARKET_HISTORY_DAEMON_CLEAR_TIME` — default `11:00` (UTC wall-clock time, `LocalTime`)
- `MARKET_HISTORY_DAEMON_REPLENISH_TIME` — default `11:05` (UTC wall-clock time, `LocalTime`)

**Queue rebuild on startup:** The daemon does not persist the queue. On start, if current time is
past `REPLENISH_TIME` (i.e. we're mid-cycle), call `QueueReplenisher.replenishNow()` immediately so
workers can start fetching. If current time is before `CLEAR_TIME`, the previous cycle already
completed and the queue starts empty in DRAINING state until the scheduled replenish fires.

---

### QueueReplenisher

Wraps the existing `RegionTypeSource` chain (same sources as `ScrapeMarketHistory.initSources()`).
Triggered on two occasions only:

1. **Daemon startup** (if current UTC time ≥ `REPLENISH_TIME` — i.e. mid-cycle).
2. **Daily at `REPLENISH_TIME`** (11:05 UTC, configurable) via a `ScheduledExecutorService` that
   fires at the next wall-clock occurrence of that time each day.

On each run:

1. Sets `QueueManager` cycle state to REPLENISHING.
2. Calls all sources to collect the ordered pair list for this cycle. The `CompoundRegionTypeSource`
   chain already preserves order (backed by `LinkedHashSet`); collect the `Flowable` with
   `.toList().blockingGet()` — **do not** re-collect into a `HashSet`, which would drop the priority
   ordering.
3. Calls `QueueManager.populate(pairs)` with the ordered `List`, which bulk-inserts all pairs as
   PENDING (front = highest priority) and sets state to ACTIVE.

The `clear()` call (11:00, `CLEAR_TIME`) is a separate scheduled task — it runs 5 minutes before
replenishment, empties the map, and sets state to DRAINING. These two scheduled tasks are the only
things that drive the daily cycle; there is no interval-based replenishment.

---

### DaemonHttpServer

A **separate Helidon SE server** from `ApiRunner`/`BasicLogin` — own port and own Routing.

Config:
- `MARKET_HISTORY_DAEMON_HTTP_PORT` — default `8081`
- `MARKET_HISTORY_DAEMON_API_KEY` — required, no default

Endpoints:

```
POST /api/v1/queue/lease
  Request:  (empty body)
  Response: { "pairs": [{"regionId": N, "typeId": N}, ...], "cycleState": "ACTIVE|DRAINING|REPLENISHING" }
  Notes: daemon decides batch size (BATCH_SIZE config). pairs list is empty when cycleState != ACTIVE;
         workers check cycleState to decide whether to idle-wait or retry immediately.
         On any error (in QueueManager.lease() or while building/serializing the response), the
         handler rolls the leased pairs back to the front of the pending queue via
         releasePairs(...) and returns HTTP 500 with body {"error": "lease_failed"}. No pairs are
         stranded in inFlight; the worker retries and gets them on the next request.

POST /api/v1/queue/submit/{regionId}/{typeId}
  Path:     regionId, typeId — identify the pair (not read from the body).
  Headers:  Last-Modified — the ESI response's Last-Modified value, forwarded verbatim by the worker
            (source of http_last_modified). Absent header => http_last_modified is omitted.
  Request:  the raw ESI response body VERBATIM — the JSON array exactly as returned by
            /markets/{region_id}/history/, no re-encoding, no per-entry enrichment by the worker.
            Content-Type: application/json.
  Response: { "accepted": N }
  Notes: - One request PER PAIR (no cross-pair batching). Submitted for every leased pair that got a
           200 from ESI, INCLUDING an empty array [] — an empty submit still marks the pair done so
           it is not re-leased/retried. (This also fixes a latent gap in the old design, which
           derived the pair identity from entry fields and so could not mark an empty result done.)
         - The daemon does the decode/enrich/dedup that the worker used to do: parse the array,
           inject region_id/type_id from the PATH into each entry, apply http_last_modified from the
           Last-Modified header, then run the existing saveMarketHistory dedup/put logic into MVStore.
         - Accepted in all cycle states. Pair marked done in the inFlight map by (regionId, typeId)
           from the path. No lease ID required or tracked.

GET /api/v1/queue/stats
  Response: { "pending": N, "inFlight": N, "done": N, "cycleState": "...", "uploadedDates": N }
```

Authentication: `DaemonApiKeyAuth` is a Helidon SE `io.helidon.webserver.http.Filter`
(registered via `HttpRouting.Builder.addFilter(...)` — Helidon 4.5.4 has no `HttpFilter`) that
checks the `X-Api-Key` header against `MARKET_HISTORY_DAEMON_API_KEY` using a **constant-time
compare** (`MessageDigest.isEqual`). Returns `401` on mismatch.

---

### RecordReceiver

Called by the `/submit/{regionId}/{typeId}` endpoint handler once per pair. Receives the verbatim
ESI response body, the pair IDs from the path, and the forwarded `Last-Modified` value.

`receive(RegionTypePair pair, byte[] rawEsiBody, Optional<Instant> lastModified): int`:
1. Decode `rawEsiBody` into an array of entries (`esiHelper.decodeArrayNode`). Empty array → 0
   entries, still valid.
2. For each entry: inject `region_id`/`type_id` from `pair` (the URL path — authoritative, ignores
   any IDs that might be in the body), and set `http_last_modified` from `lastModified` if present
   (same shape as `EsiHelper.populateLastModified`).
3. Run the existing dedup/put logic from `ScrapeMarketHistory.saveMarketHistory()` (minDate guard,
   rollover map creation past `today`, http_last_modified-only-change skip) into the `StoreMapSet`.
4. Return count of entries written. The handler then marks `pair` done in `QueueManager` by its
   path IDs (independent of body contents, so an empty result is correctly marked done).

This is where the decode/enrich logic that `MarketHistoryFetcher` + `saveMarketHistory` used to do
on the scraper now lives, server-side.

Concurrency concern: `ArchiveUploader` reads from the same MVStore maps while `RecordReceiver` writes.
MVStore maps are `ConcurrentMap`-compatible, so individual `put` calls are safe. However, the uploader
needs a **consistent snapshot** of a date's entries when building an archive.

Solution: per-date `ReadWriteLock` (or `StampedLock`). `RecordReceiver.receive()` acquires a read lock
(multiple concurrent writes allowed). `ArchiveUploader.uploadDate()` acquires a write lock briefly to
take the count and copy keys, then releases it before the slow S3 upload. This keeps upload latency
out of the hot write path.

---

### ArchiveUploader

Background thread. Logic mirrors `ScrapeMarketHistory.uploadArchives()` and `uploadTotalPairs()`.

- Wakes every `MARKET_HISTORY_DAEMON_UPLOAD_INTERVAL` (default `PT30M`).
- For each date in MVStore: compares current entry count to `totals` map.
  - If count grew, builds archive via `MarketHistoryFileBuilder`, uploads to S3.
- Uploads `totals.json`.
- Calls `dataIndexHelper.updateIndex(...)`.
- Upload runs fully concurrently with record receipt; per-date locking (above) ensures consistency.

Config: `MARKET_HISTORY_DAEMON_UPLOAD_INTERVAL` — default `PT30M`.

---

### MarketHistoryDaemon (Command)

1. Builds Dagger subgraph (or uses `@Named` providers) for daemon components.
2. **Loads existing state into MVStore before accepting work** (mirrors `ScrapeMarketHistory.run()`):
   - `downloadTotalPairs()` → `totals` map from remote `totals.json`.
   - **`loadMarketHistory()` via `ScrapeMarketHistoryBatchLoader`** → downloads existing daily
     archives into the (empty temp) MVStore. **This is mandatory**, not optional: the MVStore is a
     fresh `createTempStore` on every start, but `totals` is loaded from remote. Without preloading
     the archives, `ArchiveUploader.uploadArchive()` sees `entries.size() < existingCount` and
     throws `IllegalStateException` ("entries have shrunk") on the first upload after any restart.
3. If current UTC time ≥ `REPLENISH_TIME`, calls `QueueReplenisher.replenishNow()` (blocking) so
   the queue is populated before accepting connections. Otherwise queue starts in DRAINING state
   (empty, no leases) until the scheduled clear+replenish fires.
4. Starts background threads (all virtual threads or a `ScheduledExecutorService`):
   - Lease reaper (every 60s, calls `QueueManager.reapExpiredPairs()`)
   - Daily clear task (fires at next `CLEAR_TIME` UTC, then every 24h)
   - Daily replenish task (fires at next `REPLENISH_TIME` UTC, then every 24h). **Also performs date
     rollover** (see below).
   - Archive uploader (every `UPLOAD_INTERVAL`, calls `ArchiveUploader.uploadAll()`)
5. Starts `DaemonHttpServer`.
6. Blocks forever (or until interrupted — handles SIGTERM gracefully by completing in-flight uploads).

**Date rollover (was open question #4 — now a requirement).** The daemon runs continuously across
midnight/downtime, so `today`/`minDate` must advance and stale dates must be pruned from the MVStore.
The daily **clear/replenish task (after downtime)** is the rollover trigger. On each fire, in order:

1. Recompute `today = LocalDate.now(UTC)` and `minDate = today - 1 - ESI_MARKET_HISTORY_LOOKBACK`.
2. `mapSet.getOrCreateMap(date)` + `totals.putIfAbsent(date, 0)` for any new date(s) up to `today`.
3. **Prune every date before the new `minDate`:** for each map name `< minDate`, call
   `mapSet.removeMap(name)` (drops it from the in-memory cache **and** the underlying MVStore) and
   remove the date from `totals`. This bounds MVStore growth to the active `[minDate, today]` window.
   Guard each removal with that date's per-date write lock (see *RecordReceiver*) so a concurrent
   upload/receive is never mid-map when it is dropped.

`removeMap(String)` does not exist on `StoreMapSet` yet — **add it** (removes the cache entry and
calls `mvStore.removeMap(...)` on the backing map).

`RecordReceiver` keeps the same guard as `ScrapeMarketHistory.saveMarketHistory()`: reject entries
before `minDate` (so a late submit for a just-pruned date cannot re-create its map via
`getOrCreateMap`), and create a rollover map for entries past `today`. `ArchiveUploader` iterates
`mapSet.getMapNames()`, so pruned dates simply stop being uploaded.

---

### WorkerQueueManager

Local FIFO queue for one worker. Thread-safe.

- `localQueue: ArrayDeque<RegionTypePair>` protected by a lock.
- `LOW_WATER_MARK` config: when local queue size drops below this, trigger a top-up request.
- `topUp()` — calls `WorkerHttpClient.lease()` (no size argument — daemon decides), adds returned
  pairs to back of local queue. If response `cycleState` is DRAINING or REPLENISHING, backs off
  and retries after a short sleep rather than spinning.
- `poll()` — returns next pair; triggers async top-up if below low-water mark.

Config:
- `MARKET_HISTORY_WORKER_LOW_WATER_MARK` — default `50`

---

### EsiFetchLoop

Fetches ESI at up to 5 req/s. Runs on multiple virtual threads controlled by a semaphore.

1. Polls `WorkerQueueManager.poll()` for the next pair.
2. Fetches ESI directly via `EsiHelper.fetch(esiUrl)` + `esiHelper.standardErrorHandling(...)`
   (the URL is `/markets/{regionId}/history/?type_id={typeId}`). The worker does **not** decode or
   enrich the body — that moved to the daemon. It captures the **raw response body bytes** and the
   `Last-Modified` response header.
   - `MarketHistoryFetcher.fetchMarketHistory()` is no longer reused by the worker (it decodes and
     enriches). The worker uses `EsiHelper` for the fetch + error handling/retry only. The decode +
     enrich + dedup half of the old fetcher/`saveMarketHistory` path now runs in `RecordReceiver`.
3. On a 200, immediately calls `WorkerHttpClient.submit(pair, rawBody, lastModified)` — **one submit
   per pair, no buffering**. Empty array `[]` is still submitted so the daemon marks the pair done.
   (`MARKET_HISTORY_WORKER_SUBMIT_BATCH_SIZE` is removed — verbatim per-pair bodies can't be
   coalesced across pairs.)
4. Non-200 after error handling: log and skip submit; the pair times out and is re-leased.
5. Rate limiting: handled entirely by the existing OkHttp interceptor chain on the
   `@Named("marketHistory")` OkHttpClient — no new rate limiting code needed.
   - `EsiRateLimitInterceptor` applies the Guava `RateLimiter` at `ESI_RATE_LIMIT_PER_S` permits/s.
     Workers set `ESI_RATE_LIMIT_PER_S=5` in their env.
   - `EsiMarketHistoryRateLimitExceededInterceptor` retries HTTP 500 responses.

---

### WorkerHttpClient

OkHttp-based client. Wraps the daemon HTTP API.

- `lease(): LeaseResponse` — POSTs empty body, returns pair list + cycleState.
- `submit(RegionTypePair pair, byte[] rawEsiBody, Optional<Instant> lastModified): int` — POSTs the
  verbatim ESI body to `/submit/{regionId}/{typeId}`, forwarding `Last-Modified`; returns `accepted`.
  No lease ID.
- Adds `X-Api-Key` header to all requests.
- Retry on network errors (3 attempts, 5s backoff).

Config:
- `MARKET_HISTORY_WORKER_DAEMON_URL` — required
- `MARKET_HISTORY_WORKER_API_KEY` — required

---

### MarketHistoryWorker (Command)

1. Warms the local queue (calls `WorkerQueueManager.topUp()` blocking until `BATCH_SIZE` pairs
   are available or the daemon signals an empty queue).
2. Starts `EsiFetchLoop` across `ESI_MARKET_HISTORY_CONCURRENCY` virtual threads.
3. Runs forever; top-ups happen automatically via `WorkerQueueManager`.
4. Handles graceful shutdown on interrupt: finishes in-progress ESI calls and their per-pair
   submits, then exits. Unstarted leases are not returned early — they recover via daemon timeout
   (lease release = timeout only).

---

## Concurrency Model

```
Daemon
├── HTTP accept thread (Helidon managed)
│   ├── /lease handler ──> QueueManager.lease() [lock-free CAS]
│   └── /submit handler ──> RecordReceiver.receive() [per-date read lock]
├── Lease reaper thread (every 60s)
├── Daily clear task (fires at CLEAR_TIME UTC — empties queue, state → DRAINING)
├── Daily replenish task (fires at REPLENISH_TIME UTC — runs sources, state → ACTIVE)
└── Archive uploader thread (every UPLOAD_INTERVAL, per-date write lock briefly)

Worker
├── EsiFetchLoop × ESI_MARKET_HISTORY_CONCURRENCY virtual threads
│   └── each: poll WorkerQueueManager → fetch ESI → buffer entries → flush to daemon
└── WorkerQueueManager top-up (triggered in-line when low, runs on separate virtual thread)
```

All daemon threads use Java virtual threads. `ScheduledExecutorService` for periodic tasks, with
`VirtualThreads.checkThread()` at start of each task's run body.

---

## New Config Keys

| Key | Type | Default | Description |
|---|---|---|---|
| `MARKET_HISTORY_DAEMON_HTTP_PORT` | int | `8081` | Daemon HTTP port |
| `MARKET_HISTORY_DAEMON_API_KEY` | String | required | Shared secret |
| `MARKET_HISTORY_DAEMON_LEASE_TIMEOUT` | Duration | `PT10M` | Max time a lease can be outstanding |
| `MARKET_HISTORY_DAEMON_BATCH_SIZE` | int | `200` | Pairs per lease (daemon-controlled) |
| `MARKET_HISTORY_DAEMON_CLEAR_TIME` | LocalTime (UTC) | `11:00` | Daily time to clear queue → DRAINING |
| `MARKET_HISTORY_DAEMON_REPLENISH_TIME` | LocalTime (UTC) | `11:05` | Daily time to run sources → ACTIVE |
| `MARKET_HISTORY_DAEMON_UPLOAD_INTERVAL` | Duration | `PT30M` | Archive upload schedule |
| `MARKET_HISTORY_WORKER_DAEMON_URL` | URI | required | Daemon base URL |
| `MARKET_HISTORY_WORKER_API_KEY` | String | required | Must match daemon key |
| `MARKET_HISTORY_WORKER_LOW_WATER_MARK` | int | `50` | Top-up trigger threshold |

---

## Storage

- **MVStore** (`MarketHistoryDaemon` owns it): same `StoreMapSet` pattern as current
  `ScrapeMarketHistory`. One map per date (keyed by `date.toString()`).
- **totals map**: `ConcurrentHashMap<LocalDate, Integer>` in memory, updated after each upload,
  loaded from `totals.json` on startup (same as `ScrapeMarketHistory.downloadTotalPairs()`).
- **Queue state**: fully in-memory `ConcurrentHashMap<RegionTypePair, QueueEntry>`. Rebuilt on restart.

---

## Authentication

HTTP header `X-Api-Key: <key>`. `DaemonApiKeyAuth` is a Helidon SE `Filter` applied globally.
Returns `401 Unauthorized` (JSON body `{"error": "unauthorized"}`) on missing or wrong key,
comparing keys with `MessageDigest.isEqual` (constant-time). API key is a shared secret configured
via environment variable on both daemon and each worker.

---

## Testing Plan

### Unit Tests (one test class per component)

| Component | Test class | What is tested |
|---|---|---|
| `QueueManager` | `QueueManagerTest` | lease assigns from front of deque, no duplicate assignments, timeout reaping re-inserts at front, `clear()` empties all structures + sets DRAINING, `populate(List)` sets ACTIVE **and leases pairs back in list order (FIFO)**, `lease()` returns empty + state flag when not ACTIVE, **error mid-lease rolls all leased pairs back to front of pending and leaves inFlight unchanged (`releasePairs`)** |
| `QueueReplenisher` | `QueueReplenisherTest` | calls all sources, bulk-populates queue **preserving source order**, sets state ACTIVE; clear task sets state DRAINING |
| rollover | `MarketHistoryDaemonRolloverTest` | advances `today`/`minDate`, creates new date map, **prunes maps + totals before new `minDate` via `removeMap` (dropped from MVStore)**, late submit for pruned date rejected |
| `RecordReceiver` | `RecordReceiverTest` | writes entries to mock MVStore, handles duplicate entry (same logic as current `saveMarketHistory`) |
| `ArchiveUploader` | `ArchiveUploaderTest` | skips unchanged dates, uploads changed dates, concurrent write during upload does not corrupt |
| `DaemonApiKeyAuth` | `DaemonApiKeyAuthTest` | rejects missing key, rejects wrong key, passes correct key |
| `WorkerQueueManager` | `WorkerQueueManagerTest` | low-water trigger, blocks when empty, top-up fills queue |
| `EsiFetchLoop` | `EsiFetchLoopTest` | fetches pairs, rate limit respected, submits raw body per pair, empty `[]` still submitted, non-200 skips submit |
| `RecordReceiver` | (see above) | injects region/type from path, applies http_last_modified from header, empty body accepted (0 written), dedup skip on http_last_modified-only change |
| `WorkerHttpClient` | `WorkerHttpClientTest` | posts to `/submit/{regionId}/{typeId}` with verbatim body + Last-Modified, retry on network error, auth header present |

### Integration Test: `DistributedMarketHistoryIntegrationTest`

Runs inside a single JVM with real HTTP connections on localhost.

**Setup:**
1. Spin up `DaemonHttpServer` on an ephemeral port (0 — let OS pick).
2. Prepopulate `QueueManager` with a fixed set of ~50 test pairs (bypass sources).
3. Start `ArchiveUploader` with a mock S3 adapter (captures uploaded archives in memory).
4. Create 3 `MarketHistoryWorker` instances configured to point at the test daemon port.
5. Each worker uses a mock `EsiHelper` that returns a fixed set of fake history entries.

**Assertions:**
- Every pair is fetched exactly once (no duplicates, no missed pairs).
- All entries appear in the daemon's MVStore.
- After the upload cycle, the mock S3 adapter received correct archive content.
- Test completes within a timeout (e.g. 60s) — validates concurrency is real.

**Timeout / retry test (separate `@Test`):**
- Prepopulate queue. Start one worker that fetches but never submits (simulates crash).
- Set a very short `LEASE_TIMEOUT` (PT2S). Wait for reaper to fire.
- Verify timed-out pairs are re-inserted at the front of the pending deque.
- Start a second worker. Verify all pairs are eventually submitted exactly once.

---

## Relationship to Existing Code

- `ScrapeMarketHistory` is **not removed**. It remains as a single-machine fallback and for
  environments without multiple IPs.
- `MarketHistoryFileBuilder`, `ScrapeMarketHistoryBatchLoader`, `MarketHistorySourceStats`,
  `CompoundRegionTypeSource`, and all `RegionTypeSource` implementations are reused — **but they are
  currently package-private (`class`, not `public class`) in `...markethistory.scrape`.** The new
  `...markethistory.distributed` package cannot see them. **Resolution: promote these classes to
  `public`** so they can be injected/used from the distributed package. `MarketHistoryUtil` and
  `EsiHelper` are already accessible.
- `MarketHistoryFetcher` is **not reused by the worker**: it decodes/enriches the body, but the
  worker now forwards the raw ESI body verbatim (decode/enrich moved to the daemon's
  `RecordReceiver`). The worker fetches via `EsiHelper` directly. `RecordReceiver` reuses the
  decode/enrich/dedup logic from `EsiHelper` + `ScrapeMarketHistory.saveMarketHistory()` — factor
  that save/dedup logic into a shared helper both `ScrapeMarketHistory` and `RecordReceiver` call.
- **RxJava boundary:** the sources return `Flowable<RegionTypePair>` and `EsiHelper` returns RxJava
  types. These stay RxJava. New code (`EsiFetchLoop`, `QueueReplenisher`, `RecordReceiver`) bridges
  to synchronous virtual-thread code with `.blocking*()` at the call boundary — consistent with the
  codebase's mid-migration state (`run()` sync, `blockingAwait()` at chain ends). No new RxJava
  chains beyond these reuse boundaries.
- `StoreMapSet`, `MVStoreUtil`, `DataIndexHelper`, `S3Util`, `S3Adapter` are reused. `StoreMapSet`
  gains a new `removeMap(String)` method (drops the cached map + `mvStore.removeMap(...)`) for
  rollover pruning — used by the daemon, harmless to existing callers.
- `DaemonHttpServer` follows the same Helidon SE pattern as `ApiRunner` (`io.helidon.webserver.WebServer`
  + `HttpRouting`) but is a fresh, isolated server with no shared routing.

---

## Resolved Decisions

1. **Reuse strategy**: promote the reused `...scrape` classes to `public` (see *Relationship to
   Existing Code*). New code lives in `...markethistory.distributed`.
2. **Startup/restart consistency**: preload existing daily archives into MVStore on startup via
   `ScrapeMarketHistoryBatchLoader` (see *MarketHistoryDaemon* step 2). Mandatory — prevents the
   "entries have shrunk" crash.
3. **Lease release**: **timeout only.** No `/release` endpoint. Both clean worker shutdown and
   worker crashes are recovered by the reaper after `LEASE_TIMEOUT`. Workers still flush their submit
   buffer on clean shutdown, but do not return unstarted leases early. Simpler; recovery latency is
   bounded by `LEASE_TIMEOUT`.
4. **Date rollover**: driven by the daily replenish task (see *MarketHistoryDaemon* rollover note).

## Remaining Open Questions

1. **Graceful drain on SIGTERM for daemon**: should in-progress leases be allowed to complete their
   submit window (e.g. wait up to `LEASE_TIMEOUT`) before the process exits, or hard-stop after
   finishing any in-flight upload?
2. **Daemon metrics**: should the `/stats` endpoint also expose per-worker stats (last seen, pairs
   fetched), or is aggregate-only acceptable?
