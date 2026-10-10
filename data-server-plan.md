# Plan: `data-server` command — streaming proxy for data.everef.net

## Context
data.everef.net is served today by a Cloudflare Worker in front of the B2 bucket `data-everef-net-425eb511`, using the B2
S3-compatible endpoint (`x-amz-meta-*` headers in responses, `dir/` serves `dir/index.html`, Range and ETag work, missing
keys return a plain-text `Not found` 404). The Worker is too expensive, so it is being replaced by our own server.
Requirements and decisions:
- New, separate command `data-server` with its own deployment. `api` / `ApiRunner` stay as they are.
- Cloudflare stays in front, with caching on (files are served with `Cache-Control: public, max-age=120`). The server
  therefore sees cache misses and Cloudflare revalidations, not every download. That is intended: the access log records
  the requests that reach the server; it is not an analytics pipeline.
- Conditional requests are a hot path, not an edge case: every Cloudflare PoP revalidates each hot file roughly every
  120 s with `If-None-Match` / `If-Modified-Since`.
- No virtual-host routing in this repo. A reverse proxy on the host machine routes by Host.
- Plain B2 passthrough plus access logging. No redirects or deep-archive logic yet.
- Every file streams through the server, including multi-GB ones. Memory use must stay constant regardless of file size.
- Range requests are not supported and not needed (no known users). They are ignored, never rejected.
- Egress cost has been checked and is acceptable.
- Health checks, metrics, graceful shutdown, rollout and host-proxy config are handled outside this work.

## Upstream: B2 native download API (not S3)
The server does **not** talk to B2 over the S3 API, and does not use any SDK. It uses anonymous HTTP against B2's native
download endpoints, which work because the bucket is public:
- **By name:** `GET|HEAD https://f005.backblazeb2.com/file/data-everef-net-425eb511/<key>`
- **By ID:** `GET|HEAD https://f005.backblazeb2.com/b2api/v3/b2_download_file_by_id?fileId=<x-bz-file-id>`

Behaviour verified against the live bucket (2026-10-10). The design depends on every point here:
- Responses carry **no `ETag` and no `Last-Modified`**. They carry `x-bz-file-id`, `x-bz-file-name`,
  `x-bz-content-sha1` (may be `unverified:<sha1>` or `none` for large files), `X-Bz-Upload-Timestamp` (epoch millis),
  `Content-Length`, `Content-Type`, `Cache-Control`, `Accept-Ranges`, `Strict-Transport-Security` and
  `X-Bz-Server-Side-Encryption`.
- Custom metadata arrives as `x-bz-info-<key>` (e.g. `x-bz-info-src_last_modified_millis`), not `x-amz-meta-<key>`.
  Values are percent-encoded.
- **All conditional headers are ignored.** `If-None-Match`, `If-Match`, `If-Modified-Since` and `If-Unmodified-Since`
  all return 200. Every precondition must therefore be evaluated locally.
- Download by ID returns the same headers as download by name, and identifies one immutable file version (content and
  file info). It is used to pin a version between a HEAD and a GET.
- A missing file returns 404 with a JSON body `{"code":"not_found",...}` on GET, and no body on HEAD. Error responses
  carry `Cache-Control: max-age=0, no-cache, no-store`.
- An unknown file ID returns 400 `{"code":"bad_request",...}`.
- Query strings on the by-name URL are ignored by B2.
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
   - Why: the upstream base `https://f005.backblazeb2.com/file/data-everef-net-425eb511/` sits next to other buckets
     (`/file/<other-bucket>/…`) and the B2 API (`/b2api/…`). `/../other-bucket/x` would let anyone fetch any public B2
     bucket on that cluster through data.everef.net. That is an open proxy under our domain, usable for malware hosting
     and bandwidth abuse.
   - A path that is empty or ends in `/` gets `index.html` appended.
   - Build upstream URLs only through `HttpUrl.Builder` (`addPathSegment` per decoded segment, `addQueryParameter` for
     the file ID). Never string-join the raw path.
4. **Range.** `Range` and `If-Range` are ignored. They are not forwarded and not evaluated, and the response is a full
   200 (RFC 9110 §14.2 allows a server to ignore `Range`; `If-Range` only has meaning with `Range`). Every response,
   including errors, sets `Accept-Ranges: none` (§14.3), replacing B2's `Accept-Ranges: bytes`.
5. **Don't forward any client request headers to B2.** Upstream requests only carry `Accept-Encoding: identity` and
   `User-Agent`. B2 ignores conditionals anyway (see above), so all of them are evaluated here (step 8).
6. **Call upstream** with `call.execute()`. This blocks, which is fine because Helidon 4 runs each request on a virtual thread. Wrap every `Response` in try-with-resources.
   - **Client HEAD (with or without preconditions):** one upstream HEAD by name. Preconditions are evaluated on its headers.
   - **Client GET, no preconditions** (`If-Match`, `If-None-Match`, `If-Modified-Since`, `If-Unmodified-Since` all
     absent): one upstream GET by name. No extra round trip.
   - **Client GET, any precondition present:**
     1. Send an upstream HEAD by name.
     2. Map non-2xx responses as in step 7. Per §13.2.1, preconditions are ignored when the unconditional response
        wouldn't be 2xx or 412, so a missing file is still 404 even with `If-Match: *`.
     3. Evaluate preconditions on the HEAD's headers. Answer 304 and 412 from them, with response headers taken from the HEAD.
     4. On `PROCEED`, send an upstream GET **by ID** using the HEAD's `x-bz-file-id`. That pins the exact version, so a
        file replaced between the two calls can't be served under stale validators. Response headers for the 200 come
        from this GET, which describes the same version.
     5. If the by-ID GET returns 400 or 404 (the version was deleted in between), restart once from step 1. A second
        failure returns 503 `Service unavailable\n` with `Retry-After: 1`.
     6. If the by-ID GET's `x-bz-file-name` doesn't equal the requested key, return 502 and capture to Sentry. This
        should never happen.
   - The HEAD-first path avoids opening a multi-GB GET just to abort it for a 304. Over HTTP/1.1, aborting would also throw away the pooled connection.
   - **Required headers.** A 200 from B2 without `Content-Length` or `x-bz-file-id` returns 500 `Internal server error\n`
     and is captured to Sentry. The check runs before any response header is sent.
7. **Map upstream status codes.** Error bodies are text/plain. Every Sentry capture goes through one helper that calls
   `SentryUtil.configureScope` (which sets `http.method` and `http.uri`, the requested URL) and adds `upstream.url`,
   `upstream.status` and, when a JSON body was available, `b2.code`.
   | Upstream | Served | Sentry |
   |---|---|---|
   | 200 | 200, streamed | — |
   | Any other 2xx (e.g. 206), any 3xx | 502 `Bad gateway\n` | yes |
   | 404 | 404 `Not found\n`, keeping upstream `Cache-Control` | — |
   | 400 on a by-name request (B2 rejects the file name) | 404 `Not found\n`, logged at warn | — |
   | 400 / 404 on a by-ID request | restart, per step 6.5 | only on the second failure |
   | 401, 403 (e.g. `download_cap_exceeded`, bucket made private) | 503 `Service unavailable\n` | yes, with `b2.code` |
   | 429, 503 | 503, passing through `Retry-After` when present | yes |
   | Other 4xx/5xx, IO failure before response headers | 502 `Bad gateway\n` | yes |
   - Reading the JSON error body is best effort, capped at 4 KiB. HEAD errors have no body, so `b2.code` is absent.
   - 401 and 403 must not become 404: that would turn a download-cap or permission outage into a silent site-wide 404.
8. **Evaluate preconditions (RFC 9110 §13)** in a pure, separately unit-tested class `dataserver/Preconditions.java`.
   Inputs: method, request headers, the synthesized ETag and the resolved Last-Modified (both from step 9). Result:
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
   - **ETag (synthesized):** B2's native API sends none, so set a strong `ETag: "<x-bz-file-id>"`. The file ID is
     present on every response, unique per uploaded version, and covers both content and metadata (B2 file info is
     immutable per version). Rejected alternative: `x-bz-content-sha1`, which is `none` or `unverified:` for large files.
     - Consequence: ETag values change at cutover from the S3 MD5 to the file ID. Caches and clients holding an MD5 ETag
       will refetch once. The `etag` fields in the published data index (`DataIndex`) are S3 MD5s and will no longer
       equal the HTTP ETag.
   - **Last-Modified (synthesized)**, in this order of precedence:
     1. `x-bz-info-src_last_modified_millis` (epoch millis; reuse `S3HeaderNames.SRC_LAST_MODIFIED_MILLIS` for the key name)
     2. `x-bz-info-mtime` (epoch seconds as a float, set by rclone)
     3. `X-Bz-Upload-Timestamp` (epoch millis)

     An unparseable value falls through to the next source.
   - **Metadata:** each `x-bz-info-<key>` becomes `x-amz-meta-<key>`, with its value percent-decoded, so the public
     headers stay as they are today (outside tools may read `x-amz-meta-src_last_modified_millis`). Keys starting with
     `b2-` are B2-reserved and are skipped. If a decoded value isn't valid for a header (non-ASCII or control characters),
     the encoded value is sent instead.
   - **Passed through:** `Content-Type`, `Content-Length`, `Cache-Control`, `Content-Encoding`, `Content-Disposition`,
     `Content-Language`, `Expires`.
   - **Dropped:** everything else, including all `x-bz-*`, `X-Bz-*`, `Accept-Ranges` (replaced with `none`),
     `Content-Range`, `Strict-Transport-Security`, B2's `Date`, and hop-by-hop headers.
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
    - method, path, status, bytes sent, upstream Content-Length, duration, outcome (`ok`/`aborted`/`stalled`/`upstream_aborted`), precondition outcome (none/proceed/304/412), upstream calls made (e.g. `GET`, `HEAD+GET_BY_ID`)
    - client IP (`CF-Connecting-IP`, then `X-Forwarded-For`, then remote address), User-Agent, Referer

### Command: `cli/dataserver/DataServer.java`
- Implements `Command`. Its lifecycle mirrors `ApiRunner` (`run()`, `startServer()`, `stop()`, periodic `HealthcheckService.startPeriodicPing()`).
- It does **not** initialize `RefDataService` or the other services, so it starts immediately.
- `WebServer.builder().port(HTTP_PORT).host("0.0.0.0")`:
  - **No `AimdLimit`.** It is latency-based, and multi-minute downloads would shrink the limit to the point of rejecting traffic. Use `FixedLimit` from new config `DATA_SERVER_MAX_CONCURRENCY` (default 10,000), with no queue: requests over the limit get 503 `Service unavailable\n` with `Retry-After: 1`. The limit is a safety net against file-descriptor and memory exhaustion, not a throughput control.
  - Leave Helidon's `maxTcpConnections` and `maxConcurrentRequests` unlimited (the defaults), and raise the listener `backlog` to 8192, so bursts aren't refused at accept.
  - Disable content encoding explicitly: `contentEncoding(ContentEncodingContext.builder().contentEncodingsDiscoverServices(false).build())`. Gzipping GB streams would burn CPU and break Content-Length. Run `./gradlew dependencies | grep encoding` to confirm what is on the classpath.
  - Keep Helidon's default `writeBufferSize`. Don't raise it: a larger buffer multiplies per-stream memory.

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
- Routing: `get("/*")` and `head("/*")` go to `DataProxyHandler`, and `any("/*")` returns the 405 from step 1. An error
  handler returns text/plain 500 `Internal server error\n` and captures to Sentry through the step 7 helper. The existing
  JSON `ErrorHandler` doesn't fit here.

### Wiring and config
- `cli/CommandRunner.java`: inject `Provider<DataServer>` and add `case "data-server"`.
- `config/Configs.java`:
  - `DATA_SERVER_B2_DOWNLOAD_URL` (URI, default `https://f005.backblazeb2.com`)
  - `DATA_SERVER_B2_BUCKET` (string, required, e.g. `data-everef-net-425eb511`)
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
path, `@SetEnvironmentVariable` for the B2 download URL (pointing at MockWebServer), the bucket and a unique
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
| Basic GET | File GET | 200; body byte-identical; Content-Type, Content-Length and Cache-Control passed through; `ETag: "<file-id>"`; Last-Modified synthesized; exactly one upstream GET to `/file/<bucket>/<key>` |
| Header mapping | Upstream sends `x-bz-info-src_last_modified_millis`, `x-bz-info-foo` | Served as `x-amz-meta-src_last_modified_millis`, `x-amz-meta-foo`; no `x-bz-info-*` |
| Header mapping | `x-bz-info-foo: a%20b` | `x-amz-meta-foo: a b` |
| Header mapping | `x-bz-info-b2-content-disposition` | Not forwarded as `x-amz-meta-*` |
| Header stripping | Upstream sends `x-bz-file-id`, `x-bz-file-name`, `x-bz-content-sha1`, `X-Bz-Upload-Timestamp`, `X-Bz-Server-Side-Encryption`, `Strict-Transport-Security`, `Accept-Ranges: bytes` | None present; `Accept-Ranges: none` |
| Header passthrough | Upstream sends `Content-Encoding`, `Content-Disposition`, `Content-Language`, `Expires` | All passed through |
| Server header | Any response | `Server: eve-ref/<version>` |
| Last-Modified | Millis set / only `mtime` / only upload timestamp | Precedence: millis, then mtime, then upload timestamp |
| Last-Modified | Millis value unparseable | Falls back to mtime / upload timestamp |
| Directory mapping | `/` and `/dir/` | Upstream path is `…/index.html` and `…/dir/index.html` |
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
| Not found | Upstream 404 (GET with JSON body, HEAD without) | 404 `Not found` text/plain; upstream `Cache-Control` kept |
| Not found | Upstream 400 on by-name request | 404; no Sentry capture |
| Upstream errors | 401, 403 with `download_cap_exceeded` | 503; Sentry captured with `b2.code` |
| Upstream errors | 429 / 503 with `Retry-After: 5` | 503 with `Retry-After: 5`; Sentry captured |
| Upstream errors | 500, 302, 206 | 502; Sentry captured |
| Upstream errors | Connection refused / disconnect before headers | 502; Sentry captured |
| Required headers | Upstream 200 without `Content-Length` (chunked) | 500; Sentry captured; no partial body sent |
| Required headers | Upstream 200 without `x-bz-file-id` | 500; Sentry captured |
| Sentry context | Any capture above | Scope has `http.method`, `http.uri` (the requested URL), `upstream.url`, `upstream.status` (verify with `Mockito.mockStatic(Sentry.class)`, running the scope callback against a mock `IScope`) |
| Upstream calls | No precondition headers, GET | Exactly one upstream GET by name; no HEAD |
| Upstream calls | Precondition passes, GET | Upstream HEAD by name, then GET to `/b2api/v3/b2_download_file_by_id?fileId=<id from HEAD>`; 200 headers come from the by-ID GET |
| Upstream calls | Precondition gives 304 / 412 | Only an upstream HEAD; no GET |
| Upstream calls | HEAD with preconditions | Only an upstream HEAD |
| Pinning | By-ID GET returns 400 or 404 once | Restarted from HEAD; 200 served |
| Pinning | By-ID GET fails twice | 503 with `Retry-After: 1` |
| Pinning | File replaced between HEAD and GET (by-name now returns a new ID) | Body served is the version from the HEAD's file ID |
| Pinning | By-ID GET returns a different `x-bz-file-name` | 502; Sentry captured |
| Missing file | Upstream 404 with `If-Match: *` or `If-None-Match: *` | 404 (preconditions ignored, §13.2.1) |
| If-Match | Matching strong tag; `*`; list with one match | 200 |
| If-Match | Mismatch; `W/` tag equal to the ETag; an old S3 MD5 ETag | 412 |
| If-None-Match | Exact tag; `W/` tag equal to the ETag; `*`; list with one match | 304 |
| If-None-Match | Mismatch; an old S3 MD5 ETag | 200 |
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
Kept as-is:
- GET and HEAD only; anything else returns 405 (now also with `Allow: GET, HEAD`).
- A path ending in `/` serves `index.html`.
- A missing file returns `Not found\n`.
- `Last-Modified` is rewritten from metadata.
- `Strict-Transport-Security` is stripped.
- `x-amz-meta-*` headers are served (now mapped from `x-bz-info-*`).

Intentional differences:
- **Upstream API:** B2 native download API instead of the S3 endpoint.
- **ETag values:** the file ID instead of the S3 MD5. Clients and Cloudflare refetch once after cutover. The data
  index's `etag` fields no longer match the HTTP ETag.
- **Request headers:** the Worker forwards every request header to B2. Here none are forwarded.
- **Query strings:** return 404.
- **Paths:** the Worker resolves `..` through `new URL()`, which can climb out of the bucket. Here `.` and `..`
  segments return 400.
- **Conditional requests:** evaluated locally, in RFC 9110 §13.2.2 order, against the synthesized ETag and
  Last-Modified (the native API ignores conditionals). A conditional GET that passes costs an upstream HEAD plus a
  by-ID GET; a 304 or 412 costs only the HEAD.
- **Range:** the Worker supported `Range` and `If-Range`. Here both are ignored, a full 200 is served, and the server
  advertises `Accept-Ranges: none`. No known users rely on Range.
- **`If-Unmodified-Since`:** the Worker returns 501. Here it is evaluated, returning 412 on failure.
- **Millisecond bug:** the Worker compares `src_last_modified_millis` with milliseconds intact against the IMS date.
  Here dates are compared at second precision.
- **ETag precedence:** `If-None-Match` takes precedence over `If-Modified-Since`, and `If-Match` over `If-Unmodified-Since`.
- **B2 permission/cap errors:** 401/403 return 503 and alert through Sentry instead of becoming 404.

## Verification
1. `make generate-database`, then `./gradlew test --tests "*.dataserver.*"` and the slow large-stream task.
2. Run locally:
   ```
   DATA_SERVER_B2_BUCKET=data-everef-net-425eb511 HTTP_PORT=8081 bin/eve-ref data-server
   ```
   Then:
   - `curl -I localhost:8081/` returns 200 with `ETag`, `Last-Modified`, `Accept-Ranges: none` and no `x-bz-*`
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
