package com.autonomouslogic.everef.dataserver;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.s3.S3HeaderNames;
import com.autonomouslogic.everef.util.SentryUtil;
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
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles GET and HEAD requests for the data-server command, streaming objects straight through from B2.
 * See data-server-plan.md for the full design this follows step by step.
 */
@Singleton
@Log4j2
public class DataProxyHandler implements Handler {
	private static final org.apache.logging.log4j.Logger accessLog =
			org.apache.logging.log4j.LogManager.getLogger("dataserver.access");

	private static final int COPY_BUFFER_SIZE = 16 * 1024;
	private static final int MAX_ERROR_BODY_BYTES = 4096;
	private static final Pattern B2_CODE_PATTERN = Pattern.compile("<Code>([^<]*)</Code>");
	/**
	 * IMF-fixdate (RFC 9110 §5.6.7). Not {@link DateTimeFormatter#RFC_1123_DATE_TIME}, which omits the leading zero on
	 * single-digit days.
	 */
	private static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.ofPattern(
					"EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
			.withZone(ZoneOffset.UTC);

	private static final List<String> PASSTHROUGH_HEADERS = List.of(
			"Content-Type", "Cache-Control", "Content-Encoding", "Content-Disposition", "Content-Language", "Expires");

	private final okhttp3.OkHttpClient client;
	private final StallWatchdog watchdog;
	private final String version;
	private final HttpUrl origin;

	@Inject
	protected DataProxyHandler(@Named("data-proxy") okhttp3.OkHttpClient client, StallWatchdog watchdog) {
		this.client = client;
		this.watchdog = watchdog;
		this.version = Configs.EVE_REF_VERSION.getRequired();
		var originUri = Configs.DATA_SERVER_ORIGIN_URL.getRequired();
		var originStr = originUri.toString();
		if (originUri.getRawQuery() != null || !originStr.endsWith("/")) {
			throw new IllegalArgumentException(
					"DATA_SERVER_ORIGIN_URL must have no query and end with '/': " + originUri);
		}
		this.origin = HttpUrl.get(originStr.substring(0, originStr.length() - 1));
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		var method = req.prologue().method().text();
		var access = new AccessLog(
				method,
				req.path().rawPath(),
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
		}
	}

	private static String clientIp(ServerRequest req) {
		var cfConnectingIp = req.headers().first(HeaderNames.create("CF-Connecting-IP"));
		if (cfConnectingIp.isPresent() && !cfConnectingIp.get().isBlank()) {
			return cfConnectingIp.get().trim();
		}
		var forwardedFor = req.headers().first(HeaderNames.X_FORWARDED_FOR);
		if (forwardedFor.isPresent()) {
			var first = forwardedFor.get().split(",", 2)[0].trim();
			if (!first.isEmpty()) {
				return first;
			}
		}
		return req.remotePeer().host();
	}

	private void proxy(ServerRequest req, ServerResponse res, String method, String key, AccessLog access)
			throws IOException {
		var hasPreconditions = req.headers().contains(HeaderNames.IF_MATCH)
				|| req.headers().contains(HeaderNames.IF_NONE_MATCH)
				|| req.headers().contains(HeaderNames.IF_MODIFIED_SINCE)
				|| req.headers().contains(HeaderNames.IF_UNMODIFIED_SINCE);

		if (method.equals("HEAD")) {
			access.upstreamCalls = "HEAD";
			try (var head = upstreamCall("HEAD", buildUrl(key, null), access)) {
				if (head.code() != 200) {
					mapAndSendUpstreamError(req, res, method, head, false, access);
					return;
				}
				var versionId = requireHeader(head, "x-amz-version-id");
				var etag = requireHeader(head, "ETag");
				var contentLength = head.header("Content-Length");
				if (versionId == null || etag == null) {
					send500(req, res, method, head, access);
					return;
				}
				var lastModified = resolveLastModified(head);
				if (!hasPreconditions) {
					sendHeadSuccess(res, head, lastModified, contentLength, access);
					return;
				}
				var result = evaluatePreconditions(req, method, etag, lastModified, access);
				if (result == PreconditionResult.PRECONDITION_FAILED) {
					send412(res, access);
					return;
				}
				if (result == PreconditionResult.NOT_MODIFIED) {
					send304(res, head, access);
					return;
				}
				sendHeadSuccess(res, head, lastModified, contentLength, access);
			}
			return;
		}

		// GET
		if (!hasPreconditions) {
			access.upstreamCalls = "GET";
			try (var get = upstreamCall("GET", buildUrl(key, null), access)) {
				if (get.code() != 200) {
					mapAndSendUpstreamError(req, res, method, get, false, access);
					return;
				}
				var etag = requireHeader(get, "ETag");
				var contentLengthHeader = requireHeader(get, "Content-Length");
				if (etag == null || contentLengthHeader == null) {
					send500(req, res, method, get, access);
					return;
				}
				var lastModified = resolveLastModified(get);
				sendGetSuccess(req, res, get, lastModified, Long.parseLong(contentLengthHeader), access);
			}
			return;
		}

		access.upstreamCalls = "HEAD+GET_VERSION";
		getWithPreconditions(req, res, method, key, access, 0);
	}

	private void getWithPreconditions(
			ServerRequest req, ServerResponse res, String method, String key, AccessLog access, int attempt)
			throws IOException {
		try (var head = upstreamCall("HEAD", buildUrl(key, null), access)) {
			if (head.code() != 200) {
				mapAndSendUpstreamError(req, res, method, head, false, access);
				return;
			}
			var versionId = requireHeader(head, "x-amz-version-id");
			var headEtag = requireHeader(head, "ETag");
			if (versionId == null || headEtag == null) {
				send500(req, res, method, head, access);
				return;
			}
			var lastModified = resolveLastModified(head);
			var result = evaluatePreconditions(req, method, headEtag, lastModified, access);
			if (result == PreconditionResult.PRECONDITION_FAILED) {
				send412(res, access);
				return;
			}
			if (result == PreconditionResult.NOT_MODIFIED) {
				send304(res, head, access);
				return;
			}
			try (var get = upstreamCall("GET", buildUrl(key, versionId), access)) {
				if (get.code() == 400 || get.code() == 404) {
					if (attempt == 0) {
						getWithPreconditions(req, res, method, key, access, attempt + 1);
						return;
					}
					captureSentry(
							req, res, "Pinned GET failed twice", get.request().url(), get.code(), null);
					sendPlain(res, method, Status.SERVICE_UNAVAILABLE_503, "Service unavailable\n", "Retry-After", "1");
					access.outcome = "ok";
					return;
				}
				if (get.code() != 200) {
					mapAndSendUpstreamError(req, res, method, get, true, access);
					return;
				}
				var getEtag = requireHeader(get, "ETag");
				var contentLengthHeader = requireHeader(get, "Content-Length");
				if (getEtag == null || contentLengthHeader == null) {
					send500(req, res, method, get, access);
					return;
				}
				if (!getEtag.equals(headEtag)) {
					captureSentry(
							req,
							res,
							"Pinned GET ETag differs from HEAD",
							get.request().url(),
							get.code(),
							null);
					sendPlain(res, method, Status.create(502, "Bad Gateway"), "Bad gateway\n");
					access.outcome = "ok";
					return;
				}
				sendGetSuccess(req, res, get, lastModified, Long.parseLong(contentLengthHeader), access);
			}
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

	private Response upstreamCall(String method, HttpUrl url, AccessLog access) throws UpstreamRequestException {
		var builder = new Request.Builder().url(url).header("Accept-Encoding", "identity");
		if (method.equals("HEAD")) {
			builder.head();
		} else {
			builder.get();
		}
		Response response;
		try {
			response = client.newCall(builder.build()).execute();
		} catch (IOException e) {
			throw new UpstreamRequestException(url, e);
		}
		if (response.code() == 200) {
			access.upstreamContentLength = response.header("Content-Length");
		}
		return response;
	}

	private HttpUrl buildUrl(String key, String versionId) {
		var builder = origin.newBuilder();
		for (var segment : key.split("/")) {
			builder.addPathSegment(segment);
		}
		if (versionId != null) {
			builder.addQueryParameter("versionId", versionId);
		}
		return builder.build();
	}

	private String requireHeader(Response resp, String name) {
		return resp.header(name);
	}

	private Instant resolveLastModified(Response resp) {
		var millisHeader = resp.header("x-amz-meta-" + S3HeaderNames.SRC_LAST_MODIFIED_MILLIS);
		if (millisHeader != null) {
			try {
				return Instant.ofEpochMilli(Long.parseLong(millisHeader)).truncatedTo(ChronoUnit.SECONDS);
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		var mtimeHeader = resp.header("x-amz-meta-mtime");
		if (mtimeHeader != null) {
			try {
				var seconds = Double.parseDouble(mtimeHeader);
				return Instant.ofEpochMilli((long) (seconds * 1000)).truncatedTo(ChronoUnit.SECONDS);
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		var lastModifiedHeader = resp.header("Last-Modified");
		if (lastModifiedHeader != null) {
			var parsed = HttpDates.parse(lastModifiedHeader);
			if (parsed.isPresent()) {
				return parsed.get();
			}
		}
		return Instant.EPOCH;
	}

	private void sendHeadSuccess(
			ServerResponse res, Response head, Instant lastModified, String contentLength, AccessLog access) {
		applyCommonHeaders(res, head, lastModified);
		if (contentLength != null) {
			res.header("Content-Length", contentLength);
		}
		res.status(Status.OK_200);
		res.send();
		access.outcome = "ok";
	}

	private void sendGetSuccess(
			ServerRequest req,
			ServerResponse res,
			Response get,
			Instant lastModified,
			long contentLength,
			AccessLog access)
			throws IOException {
		applyCommonHeaders(res, get, lastModified);
		res.status(Status.OK_200);
		res.contentLength(contentLength);
		var watchdogId = watchdog.register(Thread.currentThread());
		try (var body = get.body();
				var in = body.byteStream()) {
			var out = res.outputStream();
			var buffer = new byte[COPY_BUFFER_SIZE];
			long bytesSent = 0;
			while (true) {
				int n;
				try {
					n = in.read(buffer);
				} catch (IOException e) {
					access.bytesSent = bytesSent;
					if (watchdog.isStalled(watchdogId)) {
						throw clientWriteFailed(access, watchdogId, e);
					}
					access.outcome = "upstream_aborted";
					captureSentry(
							req,
							res,
							"Upstream read failed mid-body",
							get.request().url(),
							get.code(),
							null);
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
				captureSentry(
						req,
						res,
						"Upstream EOF before Content-Length bytes",
						get.request().url(),
						get.code(),
						null);
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

	private void send304(ServerResponse res, Response head, AccessLog access) {
		res.header("Accept-Ranges", "none");
		var etag = head.header("ETag");
		if (etag != null) {
			res.header("ETag", etag);
		}
		var cacheControl = head.header("Cache-Control");
		if (cacheControl != null) {
			res.header("Cache-Control", cacheControl);
		}
		var expires = head.header("Expires");
		if (expires != null) {
			res.header("Expires", expires);
		}
		var vary = head.header("Vary");
		if (vary != null) {
			res.header("Vary", vary);
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

	private void send500(ServerRequest req, ServerResponse res, String method, Response upstream, AccessLog access) {
		captureSentry(
				req, res, "Required upstream header missing", upstream.request().url(), upstream.code(), null);
		sendPlain(res, method, Status.INTERNAL_SERVER_ERROR_500, "Internal server error\n");
		access.outcome = "ok";
	}

	private void mapAndSendUpstreamError(
			ServerRequest req, ServerResponse res, String method, Response upstream, boolean pinned, AccessLog access) {
		var code = upstream.code();
		var b2Code = extractB2Code(upstream);
		if (code == 404) {
			var cacheControl = upstream.header("Cache-Control");
			res.header("Accept-Ranges", "none");
			if (cacheControl != null) {
				res.header("Cache-Control", cacheControl);
			}
			sendBody(res, method, Status.NOT_FOUND_404, "Not found\n");
			access.outcome = "ok";
			return;
		}
		if (code == 400 && !pinned) {
			log.warn("Upstream rejected key with 400: {}", upstream.request().url());
			sendPlain(res, method, Status.NOT_FOUND_404, "Not found\n");
			access.outcome = "ok";
			return;
		}
		if (code == 401 || code == 403) {
			captureSentry(
					req,
					res,
					"Upstream auth/permission error",
					upstream.request().url(),
					code,
					b2Code);
			sendPlain(res, method, Status.SERVICE_UNAVAILABLE_503, "Service unavailable\n");
			access.outcome = "ok";
			return;
		}
		if (code == 429 || code == 503) {
			captureSentry(
					req,
					res,
					"Upstream rate-limited or unavailable",
					upstream.request().url(),
					code,
					b2Code);
			var retryAfter = upstream.header("Retry-After");
			if (retryAfter != null) {
				sendPlain(
						res,
						method,
						Status.SERVICE_UNAVAILABLE_503,
						"Service unavailable\n",
						"Retry-After",
						retryAfter);
			} else {
				sendPlain(res, method, Status.SERVICE_UNAVAILABLE_503, "Service unavailable\n");
			}
			access.outcome = "ok";
			return;
		}
		captureSentry(req, res, "Unexpected upstream status", upstream.request().url(), code, b2Code);
		sendPlain(res, method, Status.create(502, "Bad Gateway"), "Bad gateway\n");
		access.outcome = "ok";
	}

	private String extractB2Code(Response resp) {
		try {
			var body = resp.peekBody(MAX_ERROR_BODY_BYTES);
			var text = body.string();
			var matcher = B2_CODE_PATTERN.matcher(text);
			return matcher.find() ? matcher.group(1) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private void applyCommonHeaders(ServerResponse res, Response upstream, Instant lastModified) {
		res.header("Accept-Ranges", "none");
		res.header("Server", "eve-ref/" + version);
		var etag = upstream.header("ETag");
		if (etag != null) {
			res.header("ETag", etag);
		}
		res.header("Last-Modified", HTTP_DATE_FORMAT.format(lastModified));
		for (var name : PASSTHROUGH_HEADERS) {
			var value = upstream.header(name);
			if (value != null) {
				res.header(name, value);
			}
		}
		for (var name : upstream.headers().names()) {
			if (name.toLowerCase().startsWith("x-amz-meta-")) {
				res.header(name, upstream.header(name));
			}
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
			HttpUrl upstreamUrl,
			Integer upstreamStatus,
			String b2Code) {
		captureSentry(req, res, message, upstreamUrl, upstreamStatus, b2Code, null);
	}

	private void captureSentry(
			ServerRequest req,
			ServerResponse res,
			String message,
			HttpUrl upstreamUrl,
			Integer upstreamStatus,
			String b2Code,
			Throwable cause) {
		log.warn("{}: upstream={} status={} b2Code={}", message, upstreamUrl, upstreamStatus, b2Code, cause);
		Sentry.captureMessage(message, SentryLevel.ERROR, scope -> {
			SentryUtil.configureScope(scope, req, res);
			if (upstreamUrl != null) {
				scope.setExtra("upstream.url", upstreamUrl.toString());
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
	 * An upstream request that failed before a response was received (connection refused, timeout, etc.).
	 */
	private static class UpstreamRequestException extends IOException {
		final HttpUrl url;

		UpstreamRequestException(HttpUrl url, IOException cause) {
			super("Upstream request failed: " + url, cause);
			this.url = url;
		}
	}

	private static class AccessLog {
		final String method;
		final String path;
		final String clientIp;
		final String userAgent;
		final String referer;
		final Instant start = Instant.now();
		String outcome = "error";
		String upstreamCalls = "NONE";
		String upstreamContentLength;
		String precondition = "none";
		long bytesSent = 0;

		AccessLog(String method, String path, String clientIp, String userAgent, String referer) {
			this.method = method;
			this.path = path;
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
					"method={} path={} status={} bytesSent={} upstreamContentLength={} durationMs={} outcome={} "
							+ "precondition={} upstreamCalls={} clientIp={} userAgent={} referer={}",
					method,
					path,
					status,
					bytesSent,
					Optional.ofNullable(upstreamContentLength).orElse("-"),
					duration.toMillis(),
					outcome,
					precondition,
					upstreamCalls,
					quote(clientIp),
					quote(userAgent),
					quote(referer));
		}

		/**
		 * Quotes a client-supplied value so it can't break the key=value format.
		 */
		private static String quote(String value) {
			if (value == null) {
				return "-";
			}
			return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		}
	}
}
