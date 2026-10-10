# Plan: `data-server` command — streaming proxy for data.everef.net

## Context
data.everef.net is served today by a Cloudflare Worker in front of the B2 bucket `<bucket>`, using the B2
S3-compatible endpoint (`x-amz-meta-*` headers in responses, `dir/` serves `dir/index.html`, Range and ETag work, missing
keys return a plain-text `Not found` 404). The Worker is too expensive, so it is being replaced by our own server: a
Java re-implementation of the Worker, with improvements. The data stays on B2, read the same way the Worker reads it.
Requirements and decisions:
- New, separate command `data-server` with its own deployment. `api` / `ApiRunner` stay as they are.
- Cloudflare stays in front, with caching on (files are served with `Cache-Control: public, max-age=120`). The server
  therefore sees cache misses and Cloudflare revalidations, not every download. That is intended: the access log records
  the requests that reach the server; it is not an analytics pipeline.
- Conditional requests are a hot path, not an edge case: every Cloudflare PoP revalidates each hot file roughly every
  120 s with `If-None-Match` / `If-Modified-Since`.
- No virtual-host routing in this repo. A reverse proxy on the host machine routes by Host.
- Plain B2 passthrough plus access logging. No redirects or deep-archive logic yet.
- Public responses must not change where the Worker's are correct. In particular every `ETag` stays exactly as it is
  today (B2's S3 MD5), and `x-amz-meta-*` headers stay as they are.
- Every file streams through the server, including multi-GB ones. Memory use must stay constant regardless of file size.
- Range requests are not supported and not needed (no known users). They are ignored, never rejected.
- Egress cost has been checked and is acceptable.
- Health checks, metrics, graceful shutdown, rollout and host-proxy config are handled outside this work.

## Upstream: B2 S3-compatible endpoint, plain HTTP
The server reads the same endpoint the Worker reads: B2's S3-compatible endpoint, path-style, with plain anonymous HTTP
GET/HEAD (the bucket is public). It does **not** use the S3 SDK, request signing or any S3 API operation other than
fetching an object.
- **Object:** `GET|HEAD https://s3.us-east-005.backblazeb2.com/<bucket>/<key>`
- **Pinned version:** the same URL plus `?versionId=<x-amz-version-id>`

Why this endpoint and not B2's native download API (`/file/<bucket>/<key>`): the native API sends no `ETag`, renames
metadata to `x-bz-info-*` and ignores conditional headers. Using it would change every public ETag, and unchanged files
are re-uploaded regularly (e.g. `ccp/sde/schema-changelog.yaml`: content from 2026-10-07, uploaded again 2026-10-10), so
any per-version ETag would change on every re-upload.

Behaviour verified against the live bucket (2026-10-10):
- `ETag` is the content MD5, identical to what data.everef.net serves today and stable across re-uploads of identical
  content. This is also true for the large market-orders file (no `-N` multipart suffix).
- `Last-Modified` is the **upload** time, not the source time. It must be rewritten (step 9), as the Worker does.
- Custom metadata arrives as `x-amz-meta-<key>` (e.g. `x-amz-meta-src_last_modified_millis`).
- Every object response carries `x-amz-version-id` (the B2 file ID). A GET with `?versionId=<id>` returns exactly that
  version anonymously. This is used to pin a version between a HEAD and a GET.
- B2 honours conditional headers here (`If-None-Match` gives 304, `If-Match` gives 412), but evaluates dates against
  the upload time. The server evaluates everything locally instead (step 8) and sends B2 no conditionals.
- A missing key returns 404 with an XML body (`<Code>NoSuchKey</Code>`). Error responses carry
  `Cache-Control: max-age=0, no-cache, no-store` (as seen through the Worker).
- Other headers sent: `Strict-Transport-Security`, `Accept-Ranges`, `x-amz-server-side-encryption`,
  `x-amz-request-id`, `x-amz-id-2`, `Cache-Control`, `Content-Type`, `Content-Length`, `Date`.
- The endpoint speaks HTTP/1.1 only, so aborting a GET mid-body discards the pooled connection.

## Design

### Upstream fetch: dedicated OkHttp client
Add `@Named("data-proxy") OkHttpClient` in `inject/OkHttpModule.java`. **Don't reuse `mainHttpClient`.** It has three problems for this use:
- It has a 4 GiB disk `Cache`, so it would write GB files to disk.
- Its `callTimeout(120s)` would kill long downloads.
- Its `LoggingInterceptor` would add noise.

New client settings: no cache, `callTimeout(0)`, `connectTimeout 5s`, `readTimeout 60s` (applies per read, so it is safe
for long streams), `followRedirects(false)`, `retryOnConnectionFailure(true)` (only retries before a response is
received, which is safe for GET/HEAD), `UserAgentInterceptor`, and a `ConnectionPool` with `maxIdleConnections` around
64 (the default of 5 would close most connections after a burst and force new TLS handshakes).
- **Gotcha:** OkHttp adds `Accept-Encoding: gzip` and decompresses transparently, which removes `Content-Length`. Every upstream request must set `Accept-Encoding: identity`.
- **Gotcha:** always use the synchronous `call.execute()`, never `enqueue()`. The `Dispatcher` caps async calls at
  `maxRequestsPerHost = 5`, and every upstream request goes to the same host, so `enqueue()` would serialize all
  downloads behind five slots. `execute()` is not subject to that cap; the number of active upstream connections is
  then unbounded and follows client concurrency.

### Handler: `dataserver/DataProxyHandler.java` (new package `com.autonomouslogic.everef.dataserver`)
Handles GET and HEAD. Checks run in this order, and steps 1–3 never contact B2.

1. **Method.** Anything other than GET or HEAD returns 405 with `Allow: GET, HEAD` (RFC 9110 §15.5.6 requires it).
   Don't reuse `StandardHandlers.HTTP_METHOD_NOT_ALLOWED`, which sends no `Allow` header.
2. **Query string.** Any request with a query component returns 404 `Not found\n` (text/plain). Use the raw query from
   the request prologue. If Helidon can't tell a bare trailing `?` from no query, treat only a non-empty query as present.
3. **Path to key**
   - Work from Helidon's **raw** (undecoded, unnormalized) path, `req.path().rawPath()`, never the decoded `path()`.
     Helidon or the host proxy may already have normalized it, but the guard must not depend on that.
   - Split on `/`, then percent-decode each segment as a **path segment** (UTF-8, strict). Do not use `URLDecoder`: it
     turns `+` into a space. A literal `+` in a key must reach B2 as `+`.
   - Return 400 `Bad request\n` (text/plain) when:
     - a segment is exactly `.` or `..`;
     - a decoded segment contains `/`, `\` or NUL (the `/` case catches an encoded `%2F`);
     - a segment is empty (`//`), other than the trailing empty segment of a path ending in `/`;
     - percent-encoding is invalid (`%zz`, a truncated `%4`) or the decoded bytes are not valid UTF-8;
     - the final key exceeds 1024 bytes in UTF-8 (B2's file-name limit).
   - Names that merely contain dots, like `file..txt`, are fine.
   - Why: the upstream base is a *path-style* S3 URL (`https://s3.us-east-005.backblazeb2.com/<bucket>/`).
     `/../other-bucket/x` would resolve to `https://s3…/other-bucket/x`, letting anyone fetch any public B2 bucket in that
     region through data.everef.net. That is an open proxy under our domain, usable for malware hosting and bandwidth
     abuse.
   - A path that is empty or ends in `/` gets `index.html` appended.
   - Build upstream URLs only through `HttpUrl.Builder` (`addPathSegment` per decoded segment, `addQueryParameter` for
     `versionId`). Never string-join the raw path. The client's query string is never forwarded (step 2 rejects it).
4. **Range.** `Range` and `If-Range` are ignored. They are not forwarded and not evaluated, and the response is a full
   200 (RFC 9110 §14.2 allows a server to ignore `Range`; `If-Range` only has meaning with `Range`). Every response,
   including errors, sets `Accept-Ranges: none` (§14.3), replacing B2's `Accept-Ranges: bytes`.
5. **Don't forward any client request headers to B2.** Upstream requests only carry `Accept-Encoding: identity` and
   `User-Agent`. Every conditional is evaluated here (step 8), because B2 compares dates against the upload time, not
   the rewritten `Last-Modified`.
6. **Call upstream** with `call.execute()`. This blocks, which is fine because Helidon 4 runs each request on a virtual thread. Wrap every `Response` in try-with-resources.
   - **Client HEAD (with or without preconditions):** one upstream HEAD. Preconditions are evaluated on its headers.
   - **Client GET, no preconditions** (`If-Match`, `If-None-Match`, `If-Modified-Since`, `If-Unmodified-Since` all
     absent): one upstream GET. No extra round trip.
   - **Client GET, any precondition present:**
     1. Send an upstream HEAD.
     2. Map non-2xx responses as in step 7. Per §13.2.1, preconditions are ignored when the unconditional response
        wouldn't be 2xx or 412, so a missing file is still 404 even with `If-Match: *`.
     3. Evaluate preconditions on the HEAD's headers. Answer 304 and 412 from them, with response headers taken from the HEAD.
     4. On `PROCEED`, send an upstream GET with `?versionId=<x-amz-version-id from the HEAD>`. That pins the exact
        version (content and metadata), so a file replaced between the two calls can't be served under stale
        validators. Response headers for the 200 come from this GET, which describes the same version.
     5. If the pinned GET returns 400 or 404 (the version was deleted in between), restart once from step 1. A second
        failure returns 503 `Service unavailable\n` with `Retry-After: 1`.
     6. If the pinned GET's `ETag` differs from the HEAD's, return 502 and capture to Sentry. This should never happen.
   - The HEAD-first path avoids opening a multi-GB GET just to abort it for a 304. Over HTTP/1.1, aborting would also throw away the pooled connection.
   - **Required headers.** A 200 from B2 without `Content-Length` or `ETag`, or a HEAD on the pinned path without
     `x-amz-version-id`, returns 500 `Internal server error\n` and is captured to Sentry. The check runs before any
     response header is sent.
7. **Map upstream status codes.** Error bodies are text/plain. Every Sentry capture goes through one helper that calls
   `SentryUtil.configureScope` (which sets `http.method` and `http.uri`, the requested URL) and adds `upstream.url`,
   `upstream.status` and, when an XML error body was available, `b2.code` (the S3 `<Code>`).
   | Upstream | Served | Sentry |
   |---|---|---|
   | 200 | 200, streamed | — |
   | Any other 2xx (e.g. 206), any 3xx | 502 `Bad gateway\n` | yes |
   | 404 | 404 `Not found\n`, keeping upstream `Cache-Control` | — |
   | 400 on an unpinned request (B2 rejects the key) | 404 `Not found\n`, logged at warn | — |
   | 400 / 404 on a pinned (`versionId`) request | restart, per step 6.5 | only on the second failure |
   | 401, 403 (e.g. download cap exceeded, bucket made private) | 503 `Service unavailable\n` | yes, with `b2.code` |
   | 429, 503 | 503, passing through `Retry-After` when present | yes |
   | Other 4xx/5xx, IO failure before response headers | 502 `Bad gateway\n` | yes |
   - Reading the XML error body is best effort, capped at 4 KiB; extract `<Code>` without a full XML parser if simpler.
     HEAD errors have no body, so `b2.code` is absent.
   - 401 and 403 must not become 404, unlike the Worker: that would turn a download-cap or permission outage into a
     silent site-wide 404. If B2 turns out to return 403 for some ordinary missing keys, the Sentry captures will show
     it, and the mapping can be narrowed by `<Code>`.
8. **Evaluate preconditions (RFC 9110 §13)** in a pure, separately unit-tested class `dataserver/Preconditions.java`.
   Inputs: method, request headers, B2's ETag and the resolved Last-Modified (step 9). Result:
   `PROCEED`, `NOT_MODIFIED` or `PRECONDITION_FAILED`. Steps follow §13.2.2 in order:
   1. **`If-Match` present (§13.1.1):**
      - `*` is true when the file exists.
      - Otherwise the list is true if any member **strong-matches** the current ETag. A weak tag (`W/"…"`) never matches.
      - False returns 412.
   2. **Else `If-Unmodified-Since` present (§13.1.4):**
      - Ignored if the value isn't a valid HTTP-date or has more than one member.
      - False (Last-Modified > date) returns 412.
   3. **`If-None-Match` present (§13.1.2):**
      - `*` matches when the file exists.
      - Otherwise any member that **weak-matches** the ETag counts as a match (the `W/` prefix is ignored on both sides).
      - A match returns 304 for GET and HEAD.
   4. **Else `If-Modified-Since` present (§13.1.3, GET/HEAD only):**
      - Ignored if the date is invalid or has more than one member.
      - Last-Modified ≤ date returns 304.
   5. Otherwise proceed. `Range` is ignored (step 4), so §13.2.2 step 5 never applies.

   **Parsing rules**
   - **Entity-tag lists:** a small tokenizer for `#entity-tag`.
     - Commas are legal inside an opaque-tag, so don't use `split(",")`.
     - Repeated header lines are merged into one list.
     - Malformed members are treated as non-matching.
   - **HTTP-dates:** all three formats must be accepted (§5.6.7): IMF-fixdate, RFC 850 and asctime.
     - Prefer `io.helidon.http.DateTime.parseHttp` if it covers all three. Check during implementation.
     - Otherwise write three `DateTimeFormatter`s, with the RFC 850 two-digit year interpreted per §5.6.7 (more than 50 years in the future is the previous century).
   - **Second precision:** truncate the resolved Last-Modified to whole seconds before every date comparison. HTTP dates carry no milliseconds, so without this a client echoing back the exact `Last-Modified` it received gets 200 instead of 304 on IMS, and 412 on IUS.
   - Helidon's `ServerRequestHeaders` has helpers like `ifMatch()` and `ifModifiedSince()`. Only use them if they keep the weak flag and reject multi-member dates; otherwise parse the raw header values.

   **Responses**
   - **304 headers (§15.4.5), taken from the upstream HEAD:** `ETag`, `Cache-Control`, `Date`, plus `Expires`/`Vary` when present.
     - `Last-Modified` is never added: an ETag is always present.
     - No body, `Content-Length`, `Content-Type` or `x-amz-meta-*`.
   - **412:** text/plain `Precondition failed\n` (no body on HEAD), with `Cache-Control: no-store`.
9. **Build response headers** (on 200: from the GET; on 304: from the HEAD).
   - **ETag:** passed through unchanged from B2 (the MD5, same as today).
   - **Rewrite `Last-Modified`** from object metadata, in this order of precedence:
     1. `x-amz-meta-src_last_modified_millis` (epoch millis; reuse `S3HeaderNames.SRC_LAST_MODIFIED_MILLIS`)
     2. `x-amz-meta-mtime` (epoch seconds as a float, set by rclone)
     3. B2's `Last-Modified` (upload time)

     An unparseable value falls through to the next source.
   - **Passed through:** `ETag`, `Content-Type`, `Content-Length`, `Cache-Control`, `Content-Encoding`,
     `Content-Disposition`, `Content-Language`, `Expires`, and every `x-amz-meta-*` (the Worker passes them through
     today, and outside tools may read `x-amz-meta-src_last_modified_millis`).
   - **Dropped:** everything else, including `x-amz-version-id`, `x-amz-request-id`, `x-amz-id-2`,
     `x-amz-server-side-encryption` and any other non-meta `x-amz-*`, any `x-bz-*`, `Accept-Ranges` (replaced with
     `none`), `Content-Range`, `Strict-Transport-Security`, B2's `Date`, and hop-by-hop headers.
   - Set `Server: eve-ref/<version>` (version from `Configs.EVE_REF_VERSION`, as in `ApiUtil`).
10. **Stream the body**
    - Set `Content-Length` explicitly first so Helidon sends a fixed-length response instead of chunked.
    - Then copy `body.byteStream()` to `res.outputStream()` with a 16 KiB buffered loop that counts bytes for the log
      and updates the stream's progress timestamp (step 12a). 16 KiB, not 64 KiB, keeps per-stream heap small at high
      concurrency (see "Concurrency and slow readers").
    - HEAD sends headers only. **A test must confirm that Helidon keeps the forwarded `Content-Length` on HEAD.**
11. **Upstream failure mid-body** (an upstream IO error, or upstream EOF before `Content-Length` bytes, after the response
    is committed):
    - The response can't be changed, and it must not end cleanly: a short body followed by a reused keep-alive connection
      would look like a complete download to some clients. Close the client connection without completing the response.
      Throw Helidon's `io.helidon.webserver.CloseConnectionException`, or whatever the test shows actually closes the
      socket, and don't let the output stream close normally first.
    - Close the upstream response (try-with-resources), log it as `upstream_aborted` with bytes sent, and capture it to
      Sentry through the step 7 helper.
    - The server must keep serving afterwards. Covered by tests.
12. **Client abort.** An IO exception on write closes the upstream response (try-with-resources), which discards that
    upstream connection, and is logged at debug as `aborted`. It is not sent to Sentry.
12a. **Stalled client (write-stall timeout).** A client that stops reading makes the response write block indefinitely.
    Java sockets have no write timeout, and Helidon's idle-connection check doesn't cover a connection with a request in
    progress. Without a timeout, the stream would hold its virtual thread, buffers, file descriptors and B2 connection
    forever.
    - A single shared `StallWatchdog` (one scheduled task, every few seconds) tracks active streams: the handler thread
      and a last-progress timestamp, updated after every successful write.
    - A stream with no progress for `DATA_SERVER_WRITE_STALL_TIMEOUT` (default 120 s) is cancelled by interrupting its
      handler thread. On a virtual thread, interrupting a blocked socket operation closes the socket and throws (JEP 444).
      The write fails, try-with-resources closes the upstream response, and the stream is logged as `stalled`. It is not
      sent to Sentry.
    - **Verify with a test** that the interrupt really unblocks a Helidon response write. If it doesn't, find the Helidon
      hook that closes the connection, and record which mechanism was used here.
    - Only lack of progress counts. A slow client that keeps reading is never cut off, however long the download takes.
      There is no total-duration cap and no minimum-rate rule.
    - The timeout stays well above Cloudflare's and the host proxy's own stalls, so it only catches truly dead streams.
13. **Access log.** Log one line per request through a dedicated logger (`dataserver.access`), so log4j can route it on its own. Fields:
    - method, path, status, bytes sent, upstream Content-Length, duration, outcome (`ok`/`aborted`/`stalled`/`upstream_aborted`), precondition outcome (none/proceed/304/412), upstream calls made (e.g. `GET`, `HEAD`, `HEAD+GET_VERSION`)
    - client IP (`CF-Connecting-IP`, then `X-Forwarded-For`, then remote address), User-Agent, Referer

### Command: `cli/dataserver/DataServer.java`
- Implements `Command`. Its lifecycle mirrors `ApiRunner` (`run()`, `startServer()`, `stop()`, periodic `HealthcheckService.startPeriodicPing()`).
- It does **not** initialize `RefDataService` or the other services, so it starts immediately.
- `WebServer.builder().port(HTTP_PORT).host("0.0.0.0")`:
  - **No `AimdLimit`.** It is latency-based, and multi-minute downloads would shrink the limit to the point of rejecting traffic. Use `FixedLimit` from new config `DATA_SERVER_MAX_CONCURRENCY` (default 10,000), with no queue: requests over the limit get 503 `Service unavailable\n` with `Retry-After: 1`. The limit is a safety net against file-descriptor and memory exhaustion, not a throughput control.
  - Leave Helidon's `maxTcpConnections` and `maxConcurrentRequests` unlimited (the defaults), and raise the listener `backlog` to 8192, so bursts aren't refused at accept.
  - Disable content encoding explicitly: `contentEncoding(ContentEncodingContext.builder().contentEncodingsDiscoverServices(false).build())`. Gzipping GB streams would burn CPU and break Content-Length. Run `./gradlew dependencies | grep encoding` to confirm what is on the classpath.
  - Keep Helidon's default `writeBufferSize`. Don't raise it: a larger buffer multiplies per-stream memory.
- Routing: `get("/*")` and `head("/*")` go to `DataProxyHandler`, and `any("/*")` returns the 405 from step 1. An error
  handler returns text/plain 500 `Internal server error\n` and captures to Sentry through the step 7 helper. The existing
  JSON `ErrorHandler` doesn't fit here.

### Concurrency and slow readers
Goal: the server handles enough concurrent connections that slow clients never become a capacity problem. The
write-stall timeout (step 12a) is only the backstop for dead streams.
- **Per-stream cost:** one virtual thread (a few KB), one 16 KiB copy buffer, Helidon's write buffer, a few OkHttp/Okio
  8 KiB segments, two sockets (client and B2) and their kernel buffers. That is roughly 50 KiB of heap per stream, so
  10,000 concurrent streams need around 500 MB of heap. No platform thread is held per stream.
- **No artificial bottlenecks:** no OkHttp `Dispatcher` (synchronous `execute()`), no AIMD limit, no queueing.
- **Deployment requirement** (for whoever owns the deployment): open-file limit (`nofile`) of at least
  2 × `DATA_SERVER_MAX_CONCURRENCY` plus headroom, e.g. 65,536, and a heap sized from the figure above.
- Cloudflare and the host reverse proxy sit between real users and this server. Depending on how the proxy buffers
  responses, they may absorb most slow end users. This design doesn't rely on it.
- B2 may throttle a single IP at high concurrency. That surfaces as 429/503 and is mapped by step 7.

### Wiring and config
- `cli/CommandRunner.java`: inject `Provider<DataServer>` and add `case "data-server"`.
- `config/Configs.java`:
  - `DATA_SERVER_ORIGIN_URL` (URI, required, e.g. `https://s3.us-east-005.backblazeb2.com/<bucket>/`,
    the same URL the Worker uses). Validate at startup that it has no query and ends with `/`.
  - `DATA_SERVER_MAX_CONCURRENCY` (int, default 10000)
  - `DATA_SERVER_WRITE_STALL_TIMEOUT` (Duration, default `PT120S`)
  - Reuse `HTTP_PORT`.
- `AGENTS.md`: document the `data-server` command next to the `api` description.
- Tests: add `testImplementation "com.squareup.okhttp3:mockwebserver3:5.5.0"`, from the same OkHttp release as the
  existing `mockwebserver`. Add an `inject(DataServerTest)` method to `TestComponent`.

## Tests
All HTTP tests use OkHttp's MockWebServer as the B2 stand-in. Use the `mockwebserver3` package, which is needed for
two cases: streaming bodies that are generated rather than buffered (`MockResponseBody`), and mid-body disconnects
(`SocketEffect`). Follow the `SearchHandlerTest` pattern otherwise: `DaggerTestComponent`, a `Dispatcher` keyed on the
path, `@SetEnvironmentVariable` for the origin URL (MockWebServer plus a bucket path, e.g. `http://localhost:<port>/bucket/`) and a unique
`HTTP_PORT`, and a JDK `HttpClient` as the client. Every test also asserts on the requests MockWebServer recorded:
count, method, path and headers. Write the tests before the implementation.

Test classes:
- `dataserver/PreconditionsTest.java`: pure unit tests, no HTTP.
- `dataserver/HttpDatesTest.java` (or part of `PreconditionsTest`): date parsing.
- `dataserver/KeyResolverTest.java`: path-to-key unit tests (keep the path logic in its own small class so it can be tested without HTTP).
- `dataserver/DataServerTest.java`: end-to-end through Helidon and MockWebServer.
- `dataserver/DataServerLargeStreamTest.java`: tagged `slow`, run in its own Gradle test task with a small heap.
- `dataserver/DataServerConcurrencyTest.java`: tagged `slow`, same task; many concurrent slow readers.

### `DataServerTest` (end to end)
| Area | Case | Expected |
|---|---|---|
| Basic GET | File GET | 200; body byte-identical; Content-Type, Content-Length, Cache-Control and `ETag` passed through unchanged; Last-Modified rewritten; exactly one upstream GET to `/bucket/<key>` with no query |
| Metadata | Upstream sends `x-amz-meta-src_last_modified_millis`, `x-amz-meta-foo` | Both passed through unchanged |
| Header stripping | Upstream sends `x-amz-version-id`, `x-amz-request-id`, `x-amz-id-2`, `x-amz-server-side-encryption`, `Strict-Transport-Security`, `Accept-Ranges: bytes` | None present; `Accept-Ranges: none` |
| Header passthrough | Upstream sends `Content-Encoding`, `Content-Disposition`, `Content-Language`, `Expires` | All passed through |
| Server header | Any response | `Server: eve-ref/<version>` |
| Last-Modified | Millis set / only `mtime` / neither | Precedence: millis, then mtime, then B2's `Last-Modified` |
| Last-Modified | Millis value unparseable | Falls back to mtime / B2's `Last-Modified` |
| Directory mapping | `/` and `/dir/` | Upstream path is `/bucket/index.html` and `/bucket/dir/index.html` |
| Query string | `/file.txt?a=b`, `/?list-type=2` | 404 `Not found`; upstream never called |
| Method | POST, PUT, DELETE, OPTIONS | 405 with `Allow: GET, HEAD`; upstream never called |
| Path safety | `/../x`, `/%2e%2e/x`, `/a/./b`, `/a%2Fb`, `/a%5Cb`, `/a%00b`, `//x`, `/a//b` | 400; upstream never called |
| Path safety | `/%zz`, `/%4`, `/%C3%28` (invalid UTF-8) | 400; upstream never called |
| Path safety | Key longer than 1024 bytes | 400; upstream never called |
| Path safety | `/file..txt` | Served normally |
| Key encoding | `/a+b.txt`, `/a%20b.txt`, `/%C3%A6.txt` | Upstream receives the key `a+b.txt` (not `a b.txt`), `a b.txt`, `æ.txt`, correctly encoded |
| Range | `Range: bytes=0-9` on GET | 200 with full body; no `Content-Range`; `Accept-Ranges: none`; upstream request has no `Range` |
| Range | `If-Range` + `Range` | Same as above; `If-Range` not evaluated or forwarded |
| Range | `Range` on HEAD | 200; full Content-Length |
| Accept-Ranges | 200, 304, 404, 412, 405, 400, 502 | `Accept-Ranges: none` on all |
| HEAD | HEAD request | 200; forwarded Content-Length kept; no body; exactly one upstream HEAD |
| HEAD | HEAD on missing file | 404; no body |
| Request headers | Client sends `Cookie`, `Authorization`, `If-None-Match`, `Range`, `Accept-Encoding: gzip` | Upstream request carries only `Accept-Encoding: identity` and `User-Agent` |
| Compression | Client sends `Accept-Encoding: gzip` | No `Content-Encoding` added; byte-identical body; Content-Length kept |
| Not found | Upstream 404 (GET with `NoSuchKey` XML body, HEAD without) | 404 `Not found` text/plain; upstream `Cache-Control` kept |
| Not found | Upstream 400 on an unpinned request | 404; no Sentry capture |
| Upstream errors | 401; 403 with an XML `<Code>` | 503; Sentry captured with `b2.code` |
| Upstream errors | 429 / 503 with `Retry-After: 5` | 503 with `Retry-After: 5`; Sentry captured |
| Upstream errors | 500, 302, 206 | 502; Sentry captured |
| Upstream errors | Connection refused / disconnect before headers | 502; Sentry captured |
| Required headers | Upstream 200 without `Content-Length` (chunked) | 500; Sentry captured; no partial body sent |
| Required headers | Upstream 200 without `ETag` | 500; Sentry captured |
| Required headers | Conditional GET whose upstream HEAD has no `x-amz-version-id` | 500; Sentry captured |
| Sentry context | Any capture above | Scope has `http.method`, `http.uri` (the requested URL), `upstream.url`, `upstream.status` (verify with `Mockito.mockStatic(Sentry.class)`, running the scope callback against a mock `IScope`) |
| Upstream calls | No precondition headers, GET | Exactly one upstream GET, no query; no HEAD |
| Upstream calls | Precondition passes, GET | Upstream HEAD, then GET with `?versionId=<x-amz-version-id from HEAD>`; no conditional headers sent upstream; 200 headers come from the pinned GET |
| Upstream calls | Precondition gives 304 / 412 | Only an upstream HEAD; no GET |
| Upstream calls | HEAD with preconditions | Only an upstream HEAD |
| Pinning | Pinned GET returns 400 or 404 once | Restarted from HEAD; 200 served |
| Pinning | Pinned GET fails twice | 503 with `Retry-After: 1` |
| Pinning | File replaced between HEAD and GET (unpinned URL now serves new content) | Body served is the version named by the HEAD's `x-amz-version-id` |
| Pinning | Pinned GET returns a different `ETag` from the HEAD | 502; Sentry captured |
| Missing file | Upstream 404 with `If-Match: *` or `If-None-Match: *` | 404 (preconditions ignored, §13.2.1) |
| If-Match | Matching strong tag; `*`; list with one match | 200 |
| If-Match | Mismatch; `W/` tag equal to the ETag | 412 |
| If-None-Match | Exact tag; `W/` tag equal to the ETag; `*`; list with one match | 304 |
| If-None-Match | Mismatch | 200 |
| If-None-Match | Header repeated on two lines, match on the second | 304 |
| Precedence | Failing If-Match together with a matching If-None-Match | 412 |
| Precedence | Failing IUS together with a matching If-None-Match | 412 |
| Precedence | IUS together with If-Match | IUS ignored |
| Precedence | IMS together with If-None-Match (mismatch, IMS would give 304) | 200 |
| If-Modified-Since | Date ≥ resolved Last-Modified | 304 |
| If-Modified-Since | Date < Last-Modified | 200 |
| If-Modified-Since | Invalid date; two dates | Ignored; 200 |
| If-Unmodified-Since | Date ≥ Last-Modified | 200 |
| If-Unmodified-Since | Date < Last-Modified | 412 |
| If-Unmodified-Since | Invalid date | Ignored; 200 |
| Second precision | `src_last_modified_millis` with milliseconds; client sends back the returned `Last-Modified` as IMS / IUS | 304 / 200 |
| Revalidation round trip | GET, then repeat with the returned `ETag` as `If-None-Match` and `Last-Modified` as `If-Modified-Since` (as Cloudflare does) | 304 |
| 304 headers | Any 304 | ETag and Cache-Control from the upstream HEAD; no Content-Length, Content-Type, `x-amz-meta-*`, Last-Modified or body |
| 412 | GET / HEAD | `Precondition failed` / empty; `Cache-Control: no-store` |
| Upstream mid-body failure | Upstream sends headers and half the body, then disconnects (`SocketEffect`) | Client gets an IO error (truncated body), never a clean 200 with a short body; connection not reused; access log `upstream_aborted`; Sentry captured |
| Upstream mid-body failure | Upstream stalls mid-body past `readTimeout` (shortened for the test) | Same as above |
| Upstream mid-body failure | Next request after the failure | Served normally (server healthy) |
| Client abort | Client reads part of a throttled body, then closes | Upstream connection closed (the `data-proxy` client's pool shows no idle connection); access log `aborted`; no Sentry capture |
| Stalled client | Raw socket client sends GET for a large streamed body, reads the headers, then stops reading (`DATA_SERVER_WRITE_STALL_TIMEOUT` set to 2 s) | Within a few seconds after the timeout: server closes the client socket, upstream connection closed, access log `stalled`, no Sentry capture, watchdog no longer tracks the stream |
| Slow but progressing client | Client reads 16 KiB every second for longer than the stall timeout | Not cut off; download completes with matching checksum |
| Concurrency limit | `DATA_SERVER_MAX_CONCURRENCY` set to 2; three concurrent stalled downloads | Third gets 503 with `Retry-After: 1`; after the first two finish, new requests are served |
| No dispatcher cap | 20 concurrent streaming downloads from the same upstream host | All 20 upstream requests are in flight at the same time (MockWebServer sees 20 concurrent requests), proving no 5-per-host cap |
| Connection reuse | Two sequential complete GETs | The second reuses the pooled upstream connection (MockWebServer `sequenceNumber` / connection index) |
| Access log | GET 200, 304, 404, 400, aborted | One line each on `dataserver.access` with the listed fields (captured with a log4j test appender) |
| Error handler | Handler throws an unexpected exception before the response is committed | 500 text/plain; Sentry captured with `http.uri` |

### `PreconditionsTest` (pure)
The full precondition matrix from `DataServerTest`, without HTTP, plus:
- Entity-tag tokenizer: opaque-tag containing a comma; spaces around commas; empty members (`"a", , "b"`); malformed
  member (missing quotes, `W/` without a tag); `*` mixed with tags.
- Strong vs weak comparison tables from RFC 9110 §8.8.3.2.
- Each step of §13.2.2 in isolation and in combination, including HEAD for If-None-Match and If-Modified-Since.

### `HttpDatesTest` (pure)
- IMF-fixdate, RFC 850 and asctime parse to the same instant.
- RFC 850 two-digit year: more than 50 years in the future maps to the previous century.
- Invalid: garbage, wrong weekday format, missing `GMT`, numeric offset → treated as invalid (ignored).

### `KeyResolverTest` (pure)
Every path-safety and key-encoding case from `DataServerTest`, asserting the resolved key or the rejection, plus the
`index.html` mapping.

### `DataServerLargeStreamTest`
- MockWebServer serves around 1 GiB generated on the fly through `MockResponseBody`, never buffered.
- Byte count and checksum match on the client.
- Runs in a dedicated Gradle test task with a small heap (e.g. `maxHeapSize = "128m"`). Passing proves constant memory;
  a buffering bug causes an OOM. Tagged `slow` and excluded from the default `test` task.

### `DataServerConcurrencyTest` (tagged `slow`, same small-heap task)
- Open 2,000 concurrent downloads that read slowly (a few KiB per second), each from a streamed MockWebServer body.
- While they run, plain GETs for a small file must still succeed with low latency (e.g. under 1 s at p99).
- No OOM and no rejected connections with a fixed heap (e.g. 256m; the MockWebServer side shares the JVM, so size it
  from the per-stream estimate rather than the 128m used for the single large stream).
- Then stop all readers and assert the stall watchdog cleans every stream up (no tracked streams, upstream pool idle).

## Worker parity
Current behavior comes from the Cloudflare Worker that proxies to `https://s3.us-east-005.backblazeb2.com/<bucket>/`.
Kept as-is:
- Same upstream endpoint, so `ETag` values and `x-amz-meta-*` headers are unchanged.
- GET and HEAD only; anything else returns 405 (now also with `Allow: GET, HEAD`).
- A path ending in `/` serves `index.html`.
- A missing file returns `Not found\n`.
- `Last-Modified` is rewritten from metadata.
- `Strict-Transport-Security` is stripped.

Intentional differences:
- **Request headers:** the Worker forwards every request header to B2. Here none are forwarded.
- **Query strings:** return 404.
- **Paths:** the Worker resolves `..` through `new URL()`, which on a path-style S3 URL can climb out of the bucket into
  other public buckets (Cloudflare normalizes paths first, so it is likely harmless today). Here `.` and `..` segments
  return 400.
- **Conditional requests:** the Worker forwards `If-Match`, `If-None-Match` and `If-Modified-Since` to B2. B2 compares
  dates against its upload time, then the Worker re-checks IMS against a different date, with no RFC precedence. Here
  every precondition is evaluated locally, in RFC 9110 §13.2.2 order, against the rewritten `Last-Modified`. A
  conditional GET that passes costs an upstream HEAD plus a pinned GET; a 304 or 412 costs only the HEAD.
- **Range:** the Worker supported `Range` and `If-Range`. Here both are ignored, a full 200 is served, and the server
  advertises `Accept-Ranges: none`. No known users rely on Range.
- **`If-Unmodified-Since`:** the Worker returns 501. Here it is evaluated, returning 412 on failure.
- **Millisecond bug:** the Worker compares `src_last_modified_millis` with milliseconds intact against the IMS date.
  Here dates are compared at second precision.
- **ETag precedence:** `If-None-Match` takes precedence over `If-Modified-Since`, and `If-Match` over `If-Unmodified-Since`.
- **B2 permission/cap errors:** the Worker turns 403 into 404. Here 401/403 return 503 and alert through Sentry.

## Verification
1. `make generate-database`, then `./gradlew test --tests "*.dataserver.*"` and the slow large-stream task.
2. Run locally:
   ```
   DATA_SERVER_ORIGIN_URL=https://s3.us-east-005.backblazeb2.com/<bucket>/ HTTP_PORT=8081 bin/eve-ref data-server
   ```
   Then:
   - `curl -I localhost:8081/ccp/sde/schema-changelog.yaml` returns the same `ETag`, `Last-Modified` and
     `x-amz-meta-*` as `curl -I https://data.everef.net/ccp/sde/schema-changelog.yaml`, plus `Accept-Ranges: none`
     and no other `x-amz-*`
   - `curl -i -r 0-99 localhost:8081/market-orders/market-orders-latest.v3.csv.bz2 -o /dev/null -D -` returns 200 with the full Content-Length
   - Take the ETag from `curl -I`, then `curl -i -H 'If-None-Match: <etag>'` should return 304, and `-H 'If-Match: "nope"'` should return 412
   - Take `Last-Modified` from `curl -I`, then `curl -i -H 'If-Modified-Since: <it>'` should return 304
   - `curl -i --path-as-is localhost:8081/../x` returns 400; `curl -i 'localhost:8081/?list-type=2'` returns 404; `curl -i -X POST localhost:8081/` returns 405 with `Allow`
   - `curl -o /dev/null` on a multi-GB file while watching RSS (should stay flat), then compare `sha256sum` against a direct B2 download.
3. `make format`, then `make lint`.

## Out of scope / follow-ups
- 301 redirect from `/dir` to `/dir/` (today returns 404).
- Deep-archive handling.
- Range and If-Range support, if a need appears.
- Health checks, metrics, graceful shutdown, rollout and host-proxy config (handled elsewhere).
- **Caching:** files over 512 MB are never edge-cached by Cloudflare (Free/Pro/Business plans), so they always hit this server.
