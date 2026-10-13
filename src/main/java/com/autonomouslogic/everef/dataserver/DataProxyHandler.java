package com.autonomouslogic.everef.dataserver;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.pug.PugHelper;
import com.autonomouslogic.everef.s3.S3HeaderNames;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.SentryUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.CloseConnectionException;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Handles GET and HEAD requests for the data-server command, streaming objects straight through from B2 using the
 * data S3 client. See data-server-plan.md for the full design this follows step by step.
 */
@Singleton
@Log4j2
public class DataProxyHandler implements Handler {
	private static final org.apache.logging.log4j.Logger accessLog =
			org.apache.logging.log4j.LogManager.getLogger("dataserver.access");

	private static final int COPY_BUFFER_SIZE = 16 * 1024;
	/**
	 * IMF-fixdate (RFC 9110 §5.6.7). Not {@link DateTimeFormatter#RFC_1123_DATE_TIME}, which omits the leading zero on
	 * single-digit days.
	 */
	private static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.ofPattern(
					"EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
			.withZone(ZoneOffset.UTC);

	private final S3AsyncClient client;
	private final StallWatchdog watchdog;
	private final ClientRateLimiter rateLimiter;
	private final PugHelper pugHelper;
	private final String version;
	private final String bucket;
	private final String prefix;
	/**
	 * Short-lived cache of HEAD results by S3 key. Only successful responses are cached. Soft values let the JVM drop
	 * entries under memory pressure.
	 */
	private final Cache<String, Upstream> headCache;
	/**
	 * Short-lived cache of 404 results by S3 key, so repeated requests for missing keys don't each hit B2. Covers both
	 * HEAD and unpinned GET.
	 */
	private final Cache<String, UpstreamStatusException> notFoundCache;

	@Inject
	protected DataProxyHandler(
			@Named("data") S3AsyncClient client,
			StallWatchdog watchdog,
			UrlParser urlParser,
			ClientRateLimiter rateLimiter,
			PugHelper pugHelper) {
		this.client = client;
		this.watchdog = watchdog;
		this.rateLimiter = rateLimiter;
		this.pugHelper = pugHelper;
		this.version = Configs.EVE_REF_VERSION.getRequired();
		var dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		this.bucket = dataPath.getBucket();
		var path = dataPath.getPath();
		this.prefix = path == null || path.isEmpty() || path.endsWith("/") ? (path == null ? "" : path) : path + "/";
		this.headCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Configs.DATA_SERVER_HEAD_CACHE_TTL.getRequired())
				.softValues()
				.build();
		this.notFoundCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Configs.DATA_SERVER_NOT_FOUND_CACHE_TTL.getRequired())
				.softValues()
				.build();
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		var method = req.prologue().method().text();
		var access = new AccessLog(
				method,
				req.path().rawPath(),
				fullUrl(req),
				clientIp(req),
				req.headers().first(HeaderNames.USER_AGENT).orElse(null),
				req.headers().first(HeaderNames.REFERER).orElse(null));
		try {
			handle(req, res, method, access);
		} finally {
			access.log(res);
		}
	}

	private void handle(ServerRequest req, ServerResponse res, String method, AccessLog access) throws Exception {
		if (!method.equals("GET") && !method.equals("HEAD")) {
			sendPlain(
					res,
					method,
					Status.create(405, "Method Not Allowed"),
					"Method not allowed\n",
					"Allow",
					"GET, HEAD");
			access.outcome = "ok";
			return;
		}
		if (req.prologue().hasQuery()) {
			sendPlain(res, method, Status.NOT_FOUND_404, "Not found\n");
			access.outcome = "ok";
			return;
		}
		var keyOpt = KeyResolver.resolve(req.path().rawPath());
		if (keyOpt.isEmpty()) {
			sendPlain(res, method, Status.create(400, "Bad Request"), "Bad request\n");
			access.outcome = "ok";
			return;
		}
		try {
			proxy(req, res, method, keyOpt.get(), access);
		} catch (UpstreamRequestException e) {
			// Every upstream call happens before any response bytes are written, so a 502 can still be sent.
			captureSentry(req, res, "Upstream request failed", e.url, null, null, e.getCause());
			sendPlain(res, method, Status.create(502, "Bad Gateway"), "Bad gateway\n");
			access.outcome = "ok";
		} catch (RateLimitedException e) {
			sendRateLimited(req, res, method, e.retryAfterSeconds);
			// The GET was refused before it was made.
			access.upstreamCalls = access.upstreamCalls.equals("GET") ? "NONE" : "HEAD";
			access.outcome = "rate_limited";
		}
	}

	/** Path plus raw query, as the client sent it. */
	private static String fullUrl(ServerRequest req) {
		var path = req.path().rawPath();
		return req.prologue().hasQuery() ? path + "?" + req.query().rawValue() : path;
	}

	/** CF-Connecting-IP, then X-Forwarded-For, then the network peer. Also the rate limiting key. */
	private static String clientIp(ServerRequest req) {
		return ClientIps.resolve(
				req.headers().first(HeaderNames.create("CF-Connecting-IP")).orElse(null),
				req.headers().first(HeaderNames.X_FORWARDED_FOR).orElse(null),
				req.remotePeer().host());
	}

	private void proxy(ServerRequest req, ServerResponse res, String method, String key, AccessLog access)
			throws IOException {
		var hasPreconditions = req.headers().contains(HeaderNames.IF_MATCH)
				|| req.headers().contains(HeaderNames.IF_NONE_MATCH)
				|| req.headers().contains(HeaderNames.IF_MODIFIED_SINCE)
				|| req.headers().contains(HeaderNames.IF_UNMODIFIED_SINCE);
		var s3Key = prefix + key;

		if (method.equals("HEAD")) {
			access.upstreamCalls = "HEAD";
			Upstream head;
			try {
				head = head(s3Key, access);
			} catch (UpstreamStatusException e) {
				mapAndSendUpstreamError(req, res, method, e, false, access);
				return;
			}
			if (head.versionId == null || head.etag == null) {
				send500(req, res, method, head.url, access);
				return;
			}
			if (!hasPreconditions) {
				sendHeadSuccess(res, head, access);
				return;
			}
			var result = evaluatePreconditions(req, method, head.etag, head.lastModified, access);
			if (result == PreconditionResult.PRECONDITION_FAILED) {
				send412(res, access);
				return;
			}
			if (result == PreconditionResult.NOT_MODIFIED) {
				send304(res, head, access);
				return;
			}
			sendHeadSuccess(res, head, access);
			return;
		}

		// GET
		if (!hasPreconditions) {
			access.upstreamCalls = "GET";
			try (var get = get(s3Key, null, access)) {
				if (get.meta.etag == null || get.meta.contentLength == null) {
					send500(req, res, method, get.meta.url, access);
					return;
				}
				sendGetSuccess(req, res, get, access);
			} catch (UpstreamStatusException e) {
				mapAndSendUpstreamError(req, res, method, e, false, access);
			}
			return;
		}

		access.upstreamCalls = "HEAD+GET_VERSION";
		getWithPreconditions(req, res, method, s3Key, access, 0);
	}

	private void getWithPreconditions(
			ServerRequest req, ServerResponse res, String method, String key, AccessLog access, int attempt)
			throws IOException {
		Upstream head;
		try {
			head = head(key, access);
		} catch (UpstreamStatusException e) {
			mapAndSendUpstreamError(req, res, method, e, false, access);
			return;
		}
		if (head.versionId == null || head.etag == null) {
			send500(req, res, method, head.url, access);
			return;
		}
		var result = evaluatePreconditions(req, method, head.etag, head.lastModified, access);
		if (result == PreconditionResult.PRECONDITION_FAILED) {
			send412(res, access);
			return;
		}
		if (result == PreconditionResult.NOT_MODIFIED) {
			send304(res, head, access);
			return;
		}
		try (var get = get(key, head.versionId, access)) {
			if (get.meta.etag == null || get.meta.contentLength == null) {
				send500(req, res, method, get.meta.url, access);
				return;
			}
			if (!get.meta.etag.equals(head.etag)) {
				headCache.invalidate(key);
				captureSentry(req, res, "Pinned GET ETag differs from HEAD", get.meta.url, 200, null);
				sendPlain(res, method, Status.create(502, "Bad Gateway"), "Bad gateway\n");
				access.outcome = "ok";
				return;
			}
			sendGetSuccess(req, res, get, access);
		} catch (UpstreamStatusException e) {
			if (e.status == 400 || e.status == 404) {
				headCache.invalidate(key);
				if (attempt == 0) {
					getWithPreconditions(req, res, method, key, access, attempt + 1);
					return;
				}
				captureSentry(req, res, "Pinned GET failed twice", e.url, e.status, null);
				sendPlain(res, method, Status.SERVICE_UNAVAILABLE_503, "Service unavailable\n", "Retry-After", "1");
				access.outcome = "ok";
				return;
			}
			mapAndSendUpstreamError(req, res, method, e, true, access);
		}
	}

	private PreconditionResult evaluatePreconditions(
			ServerRequest req, String method, String currentEtag, Instant lastModified, AccessLog access) {
		var result = Preconditions.evaluate(
				method,
				rawHeaderLines(req, HeaderNames.IF_MATCH),
				rawHeaderLines(req, HeaderNames.IF_UNMODIFIED_SINCE),
				rawHeaderLines(req, HeaderNames.IF_NONE_MATCH),
				rawHeaderLines(req, HeaderNames.IF_MODIFIED_SINCE),
				currentEtag,
				lastModified);
		access.precondition = switch (result) {
			case PROCEED -> "proceed";
			case NOT_MODIFIED -> "304";
			case PRECONDITION_FAILED -> "412";
		};
		return result;
	}

	private List<String> rawHeaderLines(ServerRequest req, io.helidon.http.HeaderName name) {
		if (!req.headers().contains(name)) {
			return List.of();
		}
		return req.headers().get(name).allValues();
	}

	private Upstream head(String key, AccessLog access) throws UpstreamRequestException, UpstreamStatusException {
		throwIfCachedNotFound(key);
		try {
			var head = headCache.get(key, () -> headUncached(key));
			access.upstreamContentLength = String.valueOf(head.contentLength);
			return head;
		} catch (ExecutionException | UncheckedExecutionException e) {
			if (e.getCause() instanceof UpstreamRequestException ure) {
				throw ure;
			}
			if (e.getCause() instanceof UpstreamStatusException use) {
				cacheIfNotFound(key, use);
				throw use;
			}
			throw new RuntimeException(e.getCause());
		}
	}

	private Upstream headUncached(String key) throws UpstreamRequestException, UpstreamStatusException {
		var url = s3Url(key, null);
		var response = call(
				url,
				() -> client.headObject(
						HeadObjectRequest.builder().bucket(bucket).key(key).build()));
		requireOk(url, response.sdkHttpResponse().statusCode());
		return new Upstream(
				url,
				response.eTag(),
				response.versionId(),
				response.contentLength(),
				response.contentType(),
				response.cacheControl(),
				response.contentEncoding(),
				response.contentDisposition(),
				response.contentLanguage(),
				response.expiresString(),
				response.metadata(),
				resolveLastModified(response.metadata(), response.lastModified()));
	}

	private UpstreamBody get(String key, String versionId, AccessLog access)
			throws UpstreamRequestException, UpstreamStatusException {
		if (versionId == null) {
			throwIfCachedNotFound(key);
			try {
				return getUncached(key, null, access);
			} catch (UpstreamStatusException e) {
				cacheIfNotFound(key, e);
				throw e;
			}
		}
		return getUncached(key, versionId, access);
	}

	private void throwIfCachedNotFound(String key) throws UpstreamStatusException {
		var cached = notFoundCache.getIfPresent(key);
		if (cached != null) {
			throw cached;
		}
	}

	private void cacheIfNotFound(String key, UpstreamStatusException e) {
		if (e.status == 404) {
			notFoundCache.put(key, e);
		}
	}

	private UpstreamBody getUncached(String key, String versionId, AccessLog access)
			throws UpstreamRequestException, UpstreamStatusException {
		var url = s3Url(key, versionId);
		// Only a getObject costs a token. headObject calls and cached answers are free.
		var limit = rateLimiter.tryConsume(access.clientIp);
		if (!limit.allowed()) {
			throw new RateLimitedException(limit.retryAfterSeconds());
		}
		var request = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.versionId(versionId)
				.build();
		var stream = call(url, () -> client.getObject(request, AsyncResponseTransformer.toBlockingInputStream()));
		var response = stream.response();
		try {
			requireOk(url, response.sdkHttpResponse().statusCode());
		} catch (UpstreamStatusException e) {
			closeQuietly(stream);
			throw e;
		}
		access.upstreamContentLength = String.valueOf(response.contentLength());
		return new UpstreamBody(
				new Upstream(
						url,
						response.eTag(),
						response.versionId(),
						response.contentLength(),
						response.contentType(),
						response.cacheControl(),
						response.contentEncoding(),
						response.contentDisposition(),
						response.contentLanguage(),
						response.expiresString(),
						response.metadata(),
						resolveLastModified(response.metadata(), response.lastModified())),
				stream);
	}

	/**
	 * Runs an S3 call, mapping SDK failures to either an upstream HTTP status or a failure to get any response.
	 */
	private <T> T call(String url, Supplier<CompletableFuture<T>> call)
			throws UpstreamRequestException, UpstreamStatusException {
		try {
			return call.get().join();
		} catch (CompletionException | CancellationException e) {
			var cause = e.getCause() == null ? e : e.getCause();
			if (cause instanceof S3Exception s3e) {
				throw new UpstreamStatusException(url, s3e);
			}
			throw new UpstreamRequestException(url, cause);
		} catch (SdkException e) {
			if (e instanceof S3Exception s3e) {
				throw new UpstreamStatusException(url, s3e);
			}
			throw new UpstreamRequestException(url, e);
		}
	}

	/**
	 * The SDK treats any 2xx as success, but only 200 is a complete object response.
	 */
	private static void requireOk(String url, int status) throws UpstreamStatusException {
		if (status != 200) {
			throw new UpstreamStatusException(url, status);
		}
	}

	private static void closeQuietly(ResponseInputStream<?> stream) {
		try {
			stream.abort();
		} catch (RuntimeException ignored) {
			// nothing to do
		}
	}

	private String s3Url(String key, String versionId) {
		return "s3://" + bucket + "/" + key + (versionId == null ? "" : "?versionId=" + versionId);
	}

	private static Instant resolveLastModified(Map<String, String> metadata, Instant sdkLastModified) {
		var millisHeader = metadata.get(S3HeaderNames.SRC_LAST_MODIFIED_MILLIS);
		if (millisHeader != null) {
			try {
				return Instant.ofEpochMilli(Long.parseLong(millisHeader)).truncatedTo(ChronoUnit.SECONDS);
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		var mtimeHeader = metadata.get("mtime");
		if (mtimeHeader != null) {
			try {
				var seconds = Double.parseDouble(mtimeHeader);
				return Instant.ofEpochMilli((long) (seconds * 1000)).truncatedTo(ChronoUnit.SECONDS);
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		if (sdkLastModified != null) {
			return sdkLastModified.truncatedTo(ChronoUnit.SECONDS);
		}
		return Instant.EPOCH;
	}

	private void sendHeadSuccess(ServerResponse res, Upstream head, AccessLog access) {
		applyCommonHeaders(res, head);
		if (head.contentLength != null) {
			res.header("Content-Length", String.valueOf(head.contentLength));
		}
		res.status(Status.OK_200);
		res.send();
		access.outcome = "ok";
	}

	private void sendGetSuccess(ServerRequest req, ServerResponse res, UpstreamBody get, AccessLog access)
			throws IOException {
		var contentLength = get.meta.contentLength;
		applyCommonHeaders(res, get.meta);
		res.status(Status.OK_200);
		res.contentLength(contentLength);
		var watchdogId = watchdog.register(Thread.currentThread());
		try {
			var in = get.stream;
			var out = res.outputStream();
			var buffer = new byte[COPY_BUFFER_SIZE];
			long bytesSent = 0;
			while (true) {
				int n;
				try {
					n = in.read(buffer);
				} catch (IOException | SdkException e) {
					access.bytesSent = bytesSent;
					if (watchdog.isStalled(watchdogId)) {
						throw clientWriteFailed(access, watchdogId, e);
					}
					access.outcome = "upstream_aborted";
					captureSentry(req, res, "Upstream read failed mid-body", get.meta.url, 200, null);
					throw new CloseConnectionException("upstream read failed mid-body", e);
				}
				if (n == -1) {
					break;
				}
				// Helidon reports socket write failures as ServerConnectionException (a CloseConnectionException),
				// not IOException.
				try {
					out.write(buffer, 0, n);
				} catch (IOException | CloseConnectionException e) {
					access.bytesSent = bytesSent;
					throw clientWriteFailed(access, watchdogId, e);
				}
				bytesSent += n;
				watchdog.progress(watchdogId);
			}
			access.bytesSent = bytesSent;
			if (bytesSent < contentLength) {
				access.outcome = "upstream_aborted";
				captureSentry(req, res, "Upstream EOF before Content-Length bytes", get.meta.url, 200, null);
				throw new CloseConnectionException("upstream EOF before declared Content-Length");
			}
			try {
				out.close();
			} catch (IOException | CloseConnectionException e) {
				throw clientWriteFailed(access, watchdogId, e);
			}
			access.outcome = "ok";
		} finally {
			watchdog.unregister(watchdogId);
		}
	}

	/**
	 * Classifies a failed client write as a stall cut off by the watchdog or a client abort. Neither goes to Sentry.
	 */
	private CloseConnectionException clientWriteFailed(AccessLog access, long watchdogId, Exception cause) {
		if (watchdog.isStalled(watchdogId)) {
			access.outcome = "stalled";
			// Clear the watchdog's interrupt so it can't leak into anything else this thread does.
			Thread.interrupted();
			log.debug("Client stalled, stream cut off: {}", access.path);
		} else {
			access.outcome = "aborted";
			log.debug("Client aborted: {}", access.path);
		}
		return new CloseConnectionException("client write failed", cause);
	}

	private void send304(ServerResponse res, Upstream head, AccessLog access) {
		res.header("Accept-Ranges", "none");
		if (head.etag != null) {
			res.header("ETag", head.etag);
		}
		if (head.cacheControl != null) {
			res.header("Cache-Control", head.cacheControl);
		}
		if (head.expires != null) {
			res.header("Expires", head.expires);
		}
		res.status(Status.NOT_MODIFIED_304);
		res.send();
		access.outcome = "ok";
	}

	private void send412(ServerResponse res, AccessLog access) {
		res.header("Accept-Ranges", "none");
		res.header("Cache-Control", "no-store");
		res.status(Status.create(412, "Precondition Failed"));
		res.send("Precondition failed\n".getBytes(StandardCharsets.UTF_8));
		access.outcome = "ok";
	}

	private void send500(ServerRequest req, ServerResponse res, String method, String url, AccessLog access) {
		captureSentry(req, res, "Required upstream header missing", url, 200, null);
		sendPlain(res, method, Status.INTERNAL_SERVER_ERROR_500, "Internal server error\n");
		access.outcome = "ok";
	}

	private void mapAndSendUpstreamError(
			ServerRequest req,
			ServerResponse res,
			String method,
			UpstreamStatusException upstream,
			boolean pinned,
			AccessLog access) {
		var code = upstream.status;
		var b2Code = upstream.errorCode;
		if (code == 404) {
			res.header("Accept-Ranges", "none");
			if (upstream.cacheControl != null) {
				res.header("Cache-Control", upstream.cacheControl);
			}
			sendBody(res, method, Status.NOT_FOUND_404, "Not found\n");
			access.outcome = "ok";
			return;
		}
		if (code == 400 && !pinned) {
			log.warn("Upstream rejected key with 400: {}", upstream.url);
			sendPlain(res, method, Status.NOT_FOUND_404, "Not found\n");
			access.outcome = "ok";
			return;
		}
		if (code == 401 || code == 403) {
			captureSentry(req, res, "Upstream auth/permission error", upstream.url, code, b2Code);
			sendPlain(res, method, Status.SERVICE_UNAVAILABLE_503, "Service unavailable\n");
			access.outcome = "ok";
			return;
		}
		if (code == 429 || code == 503) {
			captureSentry(req, res, "Upstream rate-limited or unavailable", upstream.url, code, b2Code);
			if (upstream.retryAfter != null) {
				sendPlain(
						res,
						method,
						Status.SERVICE_UNAVAILABLE_503,
						"Service unavailable\n",
						"Retry-After",
						upstream.retryAfter);
			} else {
				sendPlain(res, method, Status.SERVICE_UNAVAILABLE_503, "Service unavailable\n");
			}
			access.outcome = "ok";
			return;
		}
		captureSentry(req, res, "Unexpected upstream status", upstream.url, code, b2Code);
		sendPlain(res, method, Status.create(502, "Bad Gateway"), "Bad gateway\n");
		access.outcome = "ok";
	}

	private void applyCommonHeaders(ServerResponse res, Upstream upstream) {
		res.header("Accept-Ranges", "none");
		res.header("Server", "eve-ref/" + version);
		if (upstream.etag != null) {
			res.header("ETag", upstream.etag);
		}
		res.header("Last-Modified", HTTP_DATE_FORMAT.format(upstream.lastModified));
		setIfPresent(res, "Content-Type", upstream.contentType);
		setIfPresent(res, "Cache-Control", upstream.cacheControl);
		setIfPresent(res, "Content-Encoding", upstream.contentEncoding);
		setIfPresent(res, "Content-Disposition", upstream.contentDisposition);
		setIfPresent(res, "Content-Language", upstream.contentLanguage);
		setIfPresent(res, "Expires", upstream.expires);
		upstream.metadata.forEach((name, value) -> res.header("x-amz-meta-" + name, value));
	}

	private static void setIfPresent(ServerResponse res, String name, String value) {
		if (value != null) {
			res.header(name, value);
		}
	}

	private void sendPlain(ServerResponse res, String method, Status status, String body) {
		res.header("Accept-Ranges", "none");
		res.header("Server", "eve-ref/" + version);
		sendBody(res, method, status, body);
	}

	private void sendPlain(
			ServerResponse res,
			String method,
			Status status,
			String body,
			String extraHeaderName,
			String extraHeaderValue) {
		res.header("Accept-Ranges", "none");
		res.header("Server", "eve-ref/" + version);
		res.header(extraHeaderName, extraHeaderValue);
		sendBody(res, method, status, body);
	}

	private void sendRateLimited(ServerRequest req, ServerResponse res, String method, long retryAfterSeconds) {
		res.header("Accept-Ranges", "none");
		res.header("Server", "eve-ref/" + version);
		res.header("Cache-Control", "no-store");
		res.header("Retry-After", String.valueOf(retryAfterSeconds));
		var wantsHtml = req.headers().first(HeaderNames.ACCEPT).orElse("").contains("text/html");
		if (!wantsHtml) {
			sendBody(res, method, Status.create(429, "Too Many Requests"), "Too many requests\n");
			return;
		}
		var html = pugHelper.renderTemplate(
				"data/rate-limited.pug",
				Map.of("pageTitle", "Too many requests", "retryAfterSeconds", retryAfterSeconds));
		res.header("Content-Type", "text/html; charset=utf-8");
		res.status(Status.create(429, "Too Many Requests"));
		if (method.equals("HEAD")) {
			res.contentLength(html.length);
			res.send();
		} else {
			res.send(html);
		}
	}

	private void sendBody(ServerResponse res, String method, Status status, String body) {
		res.header("Content-Type", "text/plain");
		res.status(status);
		if (method.equals("HEAD")) {
			res.contentLength(body.getBytes(StandardCharsets.UTF_8).length);
			res.send();
		} else {
			res.send(body.getBytes(StandardCharsets.UTF_8));
		}
	}

	private void captureSentry(
			ServerRequest req,
			ServerResponse res,
			String message,
			String upstreamUrl,
			Integer upstreamStatus,
			String b2Code) {
		captureSentry(req, res, message, upstreamUrl, upstreamStatus, b2Code, null);
	}

	private void captureSentry(
			ServerRequest req,
			ServerResponse res,
			String message,
			String upstreamUrl,
			Integer upstreamStatus,
			String b2Code,
			Throwable cause) {
		log.warn("{}: upstream={} status={} b2Code={}", message, upstreamUrl, upstreamStatus, b2Code, cause);
		Sentry.captureMessage(message, SentryLevel.ERROR, scope -> {
			SentryUtil.configureScope(scope, req, res);
			if (upstreamUrl != null) {
				scope.setExtra("upstream.url", upstreamUrl);
			}
			if (upstreamStatus != null) {
				scope.setExtra("upstream.status", String.valueOf(upstreamStatus));
			}
			if (b2Code != null) {
				scope.setExtra("b2.code", b2Code);
			}
			if (cause != null) {
				scope.setExtra("upstream.error", cause.toString());
			}
		});
	}

	/**
	 * Object metadata from a HEAD or GET response.
	 */
	private record Upstream(
			String url,
			String etag,
			String versionId,
			Long contentLength,
			String contentType,
			String cacheControl,
			String contentEncoding,
			String contentDisposition,
			String contentLanguage,
			String expires,
			Map<String, String> metadata,
			Instant lastModified) {}

	/**
	 * A GET response whose body is still being streamed from upstream. Closing aborts the upstream transfer if it
	 * wasn't read to the end.
	 */
	private record UpstreamBody(Upstream meta, ResponseInputStream<GetObjectResponse> stream) implements AutoCloseable {
		@Override
		public void close() throws IOException {
			stream.close();
		}
	}

	/**
	 * The client is out of rate limit tokens, so no object was fetched from upstream.
	 */
	private static class RateLimitedException extends RuntimeException {
		final long retryAfterSeconds;

		RateLimitedException(long retryAfterSeconds) {
			super("Rate limited", null, false, false);
			this.retryAfterSeconds = retryAfterSeconds;
		}
	}

	/**
	 * An upstream request that failed before a response was received (connection refused, timeout, etc.).
	 */
	private static class UpstreamRequestException extends IOException {
		final String url;

		UpstreamRequestException(String url, Throwable cause) {
			super("Upstream request failed: " + url, cause);
			this.url = url;
		}
	}

	/**
	 * An upstream request that got an HTTP error response.
	 */
	private static class UpstreamStatusException extends Exception {
		final String url;
		final int status;
		final String errorCode;
		final String cacheControl;
		final String retryAfter;

		UpstreamStatusException(String url, int status) {
			super("Upstream returned " + status + ": " + url);
			this.url = url;
			this.status = status;
			this.errorCode = null;
			this.cacheControl = null;
			this.retryAfter = null;
		}

		UpstreamStatusException(String url, S3Exception cause) {
			super("Upstream returned " + cause.statusCode() + ": " + url, cause);
			this.url = url;
			this.status = cause.statusCode();
			this.errorCode = cause.awsErrorDetails() == null
					? null
					: cause.awsErrorDetails().errorCode();
			var headers =
					cause.awsErrorDetails() == null || cause.awsErrorDetails().sdkHttpResponse() == null
							? null
							: cause.awsErrorDetails().sdkHttpResponse();
			this.cacheControl = headers == null
					? null
					: headers.firstMatchingHeader("Cache-Control").orElse(null);
			this.retryAfter = headers == null
					? null
					: headers.firstMatchingHeader("Retry-After").orElse(null);
		}
	}

	private static class AccessLog {
		final String method;
		final String path;
		final String url;
		final String clientIp;
		final String userAgent;
		final String referer;
		final Instant start = Instant.now();
		String outcome = "error";
		String upstreamCalls = "NONE";
		String upstreamContentLength;
		String precondition = "none";
		long bytesSent = 0;

		AccessLog(String method, String path, String url, String clientIp, String userAgent, String referer) {
			this.method = method;
			this.path = path;
			this.url = url;
			this.clientIp = clientIp;
			this.userAgent = userAgent;
			this.referer = referer;
		}

		void log(ServerResponse res) {
			var duration = java.time.Duration.between(start, Instant.now());
			// A streamed response isn't marked as sent until Helidon commits it after the handler returns, so use the
			// status set on the response. On "error" the final status is set later by the error handler.
			var status =
					outcome.equals("error") ? "-" : String.valueOf(res.status().code());
			accessLog.info(
					"method={} path={} url={} status={} bytesSent={} upstreamContentLength={} durationMs={} outcome={} "
							+ "precondition={} upstreamCalls={} clientIp={} userAgent={} referer={}",
					method,
					LogSanitizer.sanitizeUnquoted(path),
					LogSanitizer.quote(url),
					status,
					bytesSent,
					Optional.ofNullable(upstreamContentLength).orElse("-"),
					duration.toMillis(),
					outcome,
					precondition,
					upstreamCalls,
					LogSanitizer.quote(clientIp),
					LogSanitizer.quote(userAgent),
					LogSanitizer.quote(referer));
		}
	}
}
