# Plan: `data-server` command — streaming proxy for data.everef.net

## Context
data.everef.net is served today by Cloudflare in front of the B2 bucket `data-everef-net-425eb511`, using the S3 endpoint
(`x-amz-meta-*` headers in responses, `dir/` serves `dir/index.html`, Range and ETag work, missing keys return a plain-text `Not found` 404).
We want our own server in that path so we control logging and analytics. Requirements and decisions:
- New, separate command `data-server` with its own deployment. `api` / `ApiRunner` stay as they are.
- No virtual-host routing in this repo. A reverse proxy on the host machine routes by Host.
- Plain B2 passthrough plus access logging. No redirects or deep-archive logic yet.
- Every file streams through the server, including multi-GB ones. Memory use must stay constant regardless of file size.

## Assumptions (check these)
- The bucket is public, so the server reads with plain anonymous HTTP GET/HEAD and does not use the S3 SDK.
  If it is private, swap the OkHttp call for `S3Client.getObject` with `ResponseInputStream`. Nothing else changes.
- "Analytics" means a structured access-log line per request. A database or metrics pipeline would be follow-up work.

## Design

### Upstream fetch: dedicated OkHttp client
Add `@Named("data-proxy") OkHttpClient` in `inject/OkHttpModule.java`. **Don't reuse `mainHttpClient`.** It has three problems for this use:
- It has a 4 GiB disk `Cache`, so it would write GB files to disk.
- Its `callTimeout(120s)` would kill long downloads.
- Its `LoggingInterceptor` would add noise.

New client settings: no cache, `callTimeout(0)`, `connectTimeout 5s`, `readTimeout 60s` (applies per read, so it is safe for long streams), `followRedirects(false)`, `UserAgentInterceptor`.
- **Gotcha:** OkHttp adds `Accept-Encoding: gzip` and decompresses transparently, which removes `Content-Length`. Every upstream request must set `Accept-Encoding: identity`.

### Handler: `dataserver/DataProxyHandler.java` (new package `com.autonomouslogic.everef.dataserver`)
Handles GET and HEAD. Any other method returns 405 via `StandardHandlers.HTTP_METHOD_NOT_ALLOWED`.

1. **Reject unsupported headers** before the upstream call, so B2 is never contacted:
   - `Range` returns 501 with text/plain body `Range is not supported\n`.
   - `If-Range` returns 501 with text/plain body `If-Range is not supported\n`.
   - This runs after the 405 method check.
   - Every response, including 404s and errors, sets `Accept-Ranges: none` (RFC 9110 §14.3). That tells clients not to try ranges. Otherwise B2's `Accept-Ranges: bytes` would invite them to.
2. **Path to key**
   - **Path traversal guard:** percent-decode each segment, then return 400 for any segment that is exactly `.` or `..`, or that contains `/`, `\` or a NUL (the `/` case catches an encoded `%2F`). Empty segments (`//`) also return 400.
     - Names that merely contain dots, like `file..txt`, are fine.
     - Why: the upstream base is a *path-style* S3 URL (`https://s3.us-east-005.backblazeb2.com/data-everef-net-425eb511/`). `/../other-bucket/x` would resolve to `https://s3…/other-bucket/x`, letting anyone fetch any public B2 bucket in that region through data.everef.net. That is an open proxy under our domain, usable for malware hosting and bandwidth abuse.
     - Build the key from the validated segments, never by string-joining the raw path.
   - A path that is empty or ends in `/` gets `index.html` appended.
   - Build the upstream URL as `DATA_SERVER_ORIGIN_URL` + key, with path segments encoded through `HttpUrl.Builder`.
3. **Don't forward any client request headers to B2.** Upstream requests only carry `Accept-Encoding: identity`, `User-Agent` and the pinning `If-Match` from step 4.
   Every conditional header is evaluated here (step 6), because B2's `Last-Modified` is the upload time, not the rewritten value. Splitting evaluation between B2 and this server can't follow the RFC 9110 order.
4. **Call upstream** with `call.execute()`. This blocks, which is fine because Helidon 4 runs each request on a virtual thread. Wrap every `Response` in try-with-resources.
   - **No preconditions in the request** (`If-Match`, `If-None-Match`, `If-Modified-Since`, `If-Unmodified-Since` all absent): a single upstream GET, or HEAD for HEAD. This is the common path and costs no extra round trip.
   - **Any precondition present:**
     1. Send an upstream HEAD.
     2. Map non-2xx responses as in step 5. Per §13.2.1, preconditions are ignored when the unconditional response wouldn't be 2xx or 412, so a missing file is still 404 even with `If-Match: *`.
     3. Evaluate preconditions on the HEAD's headers. Answer 304 and 412 from them, and answer a client HEAD from them too.
     4. For a client GET that passes, send an upstream GET with `If-Match: <ETag from the HEAD>`. That pins the object, so a file replaced between the two calls can't be served under stale validators.
     5. If the pinned GET gets 412, restart once from the HEAD. A second 412 returns 503 with `Retry-After: 1`.
   - This avoids opening a multi-GB GET just to abort it for a 304. Aborting would also throw away the pooled connection.
5. **Map status codes**
   - A 2xx from B2 is served. B2 should never return a 206, because `Range` is never sent; treat one as 502.
   - 403 and 404 become 404 with body `Not found\n` (text/plain). Upstream headers such as `Cache-Control: max-age=0, no-cache, no-store` are kept, as the Worker does.
   - Other upstream 4xx/5xx and IO failures become 502, captured to Sentry.
6. **Evaluate preconditions (RFC 9110 §13)** in a pure, separately unit-tested class `dataserver/Preconditions.java`. Inputs: method, request headers, ETag, resolved Last-Modified (computed with the rewrite rules in step 7, from the HEAD or GET headers; S3 HEAD returns `x-amz-meta-*` too). Result: `PROCEED`, `NOT_MODIFIED` or `PRECONDITION_FAILED`. Steps follow §13.2.2 in order:
   1. **`If-Match` present (§13.1.1):**
      - `*` is true when the file exists.
      - Otherwise the list is true if any member **strong-matches** the current ETag. A weak tag (`W/"…"`) never matches.
      - False returns 412.
   2. **Else `If-Unmodified-Since` present (§13.1.4):**
      - Ignored if the value isn't a valid HTTP-date, has more than one member, or there is no Last-Modified.
      - False (Last-Modified > date) returns 412.
   3. **`If-None-Match` present (§13.1.2):**
      - `*` matches when the file exists.
      - Otherwise any member that **weak-matches** the ETag counts as a match (the `W/` prefix is ignored on both sides).
      - A match returns 304 for GET and HEAD.
   4. **Else `If-Modified-Since` present (§13.1.3, GET/HEAD only):**
      - Ignored if the date is invalid, has more than one member, or there is no Last-Modified.
      - Last-Modified ≤ date returns 304.
   5. Otherwise proceed. `Range` and `If-Range` were already rejected in step 1, so §13.2.2 step 5 never applies.

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
   - **304 headers (§15.4.5):** `ETag`, `Cache-Control`, `Date`, plus `Expires`/`Vary` when present.
     - Add `Last-Modified` only when there is no ETag.
     - No body, `Content-Length`, `Content-Type` or `x-amz-meta-*`.
   - **412:** text/plain `Precondition failed\n` (no body on HEAD), with `Cache-Control: no-store`.
7. **Forward response headers** (on 2xx). Allowlist: `Content-Type`, `Content-Length`, `ETag`, `Last-Modified`, `Cache-Control`, `Content-Encoding`, `Content-Disposition`, and `x-amz-meta-*`.
   - `x-amz-meta-*` is kept because the Worker passes it through today and outside tools may read `x-amz-meta-src_last_modified_millis`.
   - Drop `Accept-Ranges` (replaced with `none`), `Content-Range`, other `x-amz-*`, `x-bz-*`, `Strict-Transport-Security` and hop-by-hop headers.
   - Set `Server: eve-ref/<version>` (version from `Configs.EVE_REF_VERSION`, as in `ApiUtil`).
   - **Rewrite `Last-Modified`** from object metadata, in this order of precedence:
     1. `x-amz-meta-src_last_modified_millis` (epoch millis; reuse `S3HeaderNames.SRC_LAST_MODIFIED_MILLIS`)
     2. `x-amz-meta-mtime` (epoch seconds as a float, set by rclone)
     3. B2's `Last-Modified` (upload time)
8. **Stream the body**
   - Set `Content-Length` explicitly first so Helidon sends a fixed-length response instead of chunked.
   - Then `try (out = res.outputStream()) { body.byteStream().transferTo(out) }`, or a 64 KiB buffered copy loop that counts bytes for the log.
   - HEAD sends headers only. **A test must confirm that Helidon keeps the forwarded `Content-Length` on HEAD.**
9. **Client abort.** An IO exception on write closes the upstream response (try-with-resources) and is logged at debug as `aborted`. It is not sent to Sentry.
10. **Access log.** Log one line per request through a dedicated logger (`dataserver.access`), so log4j can route it on its own. Fields:
   - method, path, status, bytes sent, upstream Content-Length, duration, aborted flag, precondition outcome (none/proceed/304/412)
   - Also log rejected `Range`/`If-Range` requests (they show up as 501s), to measure demand for range support.
   - client IP (`CF-Connecting-IP`, then `X-Forwarded-For`, then remote address), User-Agent, Referer

### Command: `cli/dataserver/DataServer.java`
- Implements `Command`. Its lifecycle mirrors `ApiRunner` (`run()`, `startServer()`, `stop()`, periodic `HealthcheckService.startPeriodicPing()`).
- It does **not** initialize `RefDataService` or the other services, so it starts immediately.
- `WebServer.builder().port(HTTP_PORT).host("0.0.0.0")`:
  - **No `AimdLimit`.** It is latency-based, and multi-minute downloads would shrink the limit to the point of rejecting traffic. Use `FixedLimit` from new config `DATA_SERVER_MAX_CONCURRENCY` (default around 256), or no limit.
  - Disable content encoding explicitly: `contentEncoding(ContentEncodingContext.builder().contentEncodingsDiscoverServices(false).build())`. Gzipping GB streams would burn CPU and break Content-Length. Run `./gradlew dependencies | grep encoding` to confirm what is on the classpath.
  - Consider raising `writeBufferSize` to 64 KiB for throughput.
- Routing: `get("/*")` and `head("/*")` go to `DataProxyHandler`, `any("/*")` returns 405, and an error handler returns text 500 with Sentry capture (reuse `SentryUtil.configureScope`).
  The existing JSON `ErrorHandler` doesn't fit here.

### Wiring and config
- `cli/CommandRunner.java`: inject `Provider<DataServer>` and add `case "data-server"`.
- `config/Configs.java`:
  - `DATA_SERVER_ORIGIN_URL` (URI, required, e.g. `https://s3.us-east-005.backblazeb2.com/data-everef-net-425eb511/`)
  - `DATA_SERVER_MAX_CONCURRENCY`
  - Reuse `HTTP_PORT`.
- `AGENTS.md`: document the `data-server` command next to the `api` description.

## Tests: `src/test/java/.../dataserver/DataServerTest.java`
Follow the `SearchHandlerTest` pattern: `DaggerTestComponent`, `MockWebServer` as the B2 stand-in, `@SetEnvironmentVariable` for the origin and port, and a JDK `HttpClient`. Write the tests before the implementation.

| Area | Case | Expected |
|---|---|---|
| Basic GET | File GET | 200; body, Content-Type, Content-Length, ETag, Last-Modified and Cache-Control passed through; `x-amz-meta-*` kept; other `x-amz-*`, `x-bz-*` and HSTS stripped |
| Directory mapping | `/` and `/dir/` | Upstream request is for `index.html` |
| Range | `Range: bytes=0-9` | 501 `Range is not supported`; upstream never called |
| If-Range | `If-Range` with an ETag or a date | 501 `If-Range is not supported`; upstream never called |
| Range | Both on HEAD | Same 501 |
| Range | Any normal 200 | `Accept-Ranges: none`; no `Content-Range` |
| HEAD | HEAD request | Correct Content-Length, no body |
| Not found | Upstream 404 or 403 | 404 with `Not found` |
| Upstream failure | Upstream 500 or disconnect | 502 |
| Bad method | POST | 405 |
| Path safety | `/../x`, `/%2e%2e/x`, `/a%2Fb`, `//x` | 400; upstream never called |
| Path safety | `/file..txt` | Served normally |
| Last-Modified rewrite | `src_last_modified_millis` vs `mtime` vs upstream | Precedence is millis, then mtime, then upstream |
| Upstream calls | No precondition headers | Exactly one upstream GET; no HEAD; no client headers forwarded |
| Upstream calls | Any precondition passes | Upstream HEAD, then GET carrying `If-Match: <etag>`; no client conditionals forwarded |
| Upstream calls | Precondition gives 304/412 | Only an upstream HEAD; GET never issued |
| Upstream calls | HEAD with preconditions | Only an upstream HEAD |
| Upstream calls | Pinned GET gets 412 once | Retried from HEAD; 200 served |
| Upstream calls | Pinned GET gets 412 twice | 503 with `Retry-After` |
| Missing file | Upstream 404 with `If-Match: *` or `If-None-Match` | 404 (preconditions ignored, §13.2.1) |
| If-Match | Matching strong tag; `*`; list with one match | 200 |
| If-Match | Mismatch; `W/` tag equal to the ETag | 412 |
| If-None-Match | Exact tag; `W/` tag equal to the ETag; `*`; list with one match | 304 |
| If-None-Match | Mismatch | 200 |
| Entity-tag parsing | Opaque-tag containing a comma; header repeated on two lines; malformed member | Parsed correctly; malformed member doesn't match |
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
| Date formats | IMF-fixdate, RFC 850, asctime | All three parsed identically |
| Second precision | `src_last_modified_millis` with milliseconds; client sends back the returned `Last-Modified` as IMS / IUS | 304 / 200 |
| 304 headers | Any 304 | ETag and Cache-Control present; no Content-Length, Content-Type or body; Last-Modified only when there is no ETag |
| 412 | GET / HEAD | `Precondition failed` / empty |
| `Preconditions` unit tests | — | Same matrix tested on the pure class without HTTP |
| Compression | Client sends `Accept-Encoding: gzip` | No `Content-Encoding` added; byte-identical body |
| Upstream encoding | Any upstream request | Carries `Accept-Encoding: identity` |
| Large stream | Around 1 GiB body, from a small upstream stub that writes generated bytes (MockWebServer buffers bodies in memory) | Byte count and checksum match; heap stays flat. Mark as a slow/tagged test if needed. |
| Client abort | Client disconnects mid-stream | Upstream connection is closed |

## Worker parity
Current behavior comes from the Cloudflare Worker that proxies to `https://s3.us-east-005.backblazeb2.com/data-everef-net-425eb511/`. Kept as-is:
- GET and HEAD only; anything else returns 405.
- A path ending in `/` serves `index.html`.
- 403 or 404 from B2 becomes `Not found\n`.
- `Last-Modified` is rewritten from metadata.
- `Strict-Transport-Security` is stripped.
- `x-amz-meta-*` passes through.

Intentional differences:
- **Request headers:** the Worker forwards every request header to B2. Here none are forwarded.
- **Paths:** the Worker resolves `..` through `new URL()`, which on a path-style S3 URL can climb out of the bucket into other public buckets (Cloudflare normalizes paths first, so it is likely harmless today). Here `.` and `..` segments return 400.
- **Conditional requests:** the Worker forwards `If-Match`, `If-None-Match` and `If-Modified-Since` to B2. B2 compares dates against its upload time, then the Worker re-checks IMS against a different date, with no RFC precedence. Here every precondition is evaluated locally, in RFC 9110 §13.2.2 order, against the rewritten `Last-Modified`. A conditional request costs an upstream HEAD, plus the GET when it passes.
- **Range regression:** the Worker supports `Range` and `If-Range` (B2 returns 206). Here both return 501 and the server advertises `Accept-Ranges: none`. Resumable and parallel downloads of large files stop working.
- **`If-Unmodified-Since`:** the Worker returns 501. Here it is evaluated properly against the resolved Last-Modified, returning 412 on failure.
- **Millisecond bug:** the Worker compares `src_last_modified_millis` with its milliseconds intact against the IMS date, which has none. A client echoing back the exact `Last-Modified` can therefore get a 200 instead of a 304. Here dates are compared at second precision.
- **ETag precedence:** `If-None-Match` takes precedence over `If-Modified-Since`, and `If-Match` over `If-Unmodified-Since`. The Worker could return 304 on IMS even when the ETag didn't match.

## Verification
1. `make generate-database`, then `./gradlew test --tests "*DataServerTest"`.
2. Run locally:
   ```
   DATA_SERVER_ORIGIN_URL=<b2 url> HTTP_PORT=8081 bin/eve-ref data-server
   ```
   Then:
   - `curl -I localhost:8081/`
   - `curl -i -r 0-99 localhost:8081/market-orders/market-orders-latest.v3.csv.bz2` should return 501
   - Take the ETag from `curl -I`, then `curl -i -H 'If-None-Match: <etag>'` should return 304, and `-H 'If-Match: "nope"'` should return 412
   - Take `Last-Modified` from `curl -I`, then `curl -i -H 'If-Modified-Since: <it>'` should return 304
   - `curl -i --path-as-is localhost:8081/../x` should return 400
   - `curl -o /dev/null` on a multi-GB file while watching RSS (should stay flat), then compare `sha256sum` against a direct B2 download.
3. `make format`, then `make lint`.

## Out of scope / follow-ups
- 301 redirect from `/dir` to `/dir/` (today returns 404).
- Deep-archive handling.
- Analytics beyond logs.
- Range and If-Range support: forward to B2, pass 206/416/`Content-Range` through, evaluate date `If-Range` locally.
- Host-proxy config.
- **Cost:** B2-to-server egress isn't covered by the Bandwidth Alliance (free up to 3x stored data per month, then about $0.01/GB), and every byte also leaves through the server's own uplink. Watch this after launch.
- **Caching:** if Cloudflare stays in front, files over 512 MB are never edge-cached (Free/Pro/Business plans), so they always hit this server.
