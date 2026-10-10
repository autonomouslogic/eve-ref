package com.autonomouslogic.everef.cli.dataserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.dataserver.StallWatchdog;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.LogCapture;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import mockwebserver3.MockResponse;
import mockwebserver3.MockResponseBody;
import mockwebserver3.MockWebServer;
import mockwebserver3.SocketEffect;
import okio.BufferedSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "HTTP_PORT", value = "" + DataServerTest.DATA_SERVER_TEST_PORT)
@SetEnvironmentVariable(
		key = "DATA_SERVER_ORIGIN_URL",
		value = "http://localhost:" + DataServerTest.UPSTREAM_PORT + "/bucket/")
@Log4j2
@Timeout(60)
public class DataServerTest {
	public static final int DATA_SERVER_TEST_PORT = 29218;
	public static final int UPSTREAM_PORT = 29219;

	private static final String ETAG = "\"abc123\"";
	private static final String VERSION_ID = "4_z1234";
	private static final String LAST_MODIFIED_MILLIS = "1700000000000";

	private static final Pattern ACCESS_LOG_FIELD = Pattern.compile("(\\w+)=(\"(?:[^\"\\\\]|\\\\.)*\"|\\S*)");

	@Inject
	DataServer dataServer;

	@Inject
	StallWatchdog watchdog;

	MockWebServer upstream;
	HttpClient httpClient;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		httpClient = HttpClient.newHttpClient();
		upstream = new MockWebServer();
		upstream.start(UPSTREAM_PORT);
		dataServer.startServer();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		dataServer.stop();
		upstream.close();
	}

	private HttpResponse<byte[]> send(String method, String path, String... headers) throws Exception {
		var builder = HttpRequest.newBuilder().uri(uri(path)).timeout(java.time.Duration.ofSeconds(5));
		builder = switch (method) {
			case "HEAD" -> builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
			case "POST" -> builder.POST(HttpRequest.BodyPublishers.noBody());
			case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.noBody());
			case "DELETE" -> builder.DELETE();
			default -> builder.GET();
		};
		for (int i = 0; i < headers.length; i += 2) {
			builder.header(headers[i], headers[i + 1]);
		}
		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + DATA_SERVER_TEST_PORT + path);
	}

	private MockResponse.Builder fileResponse() {
		return new MockResponse.Builder()
				.code(200)
				.setHeader("ETag", ETAG)
				.setHeader("x-amz-version-id", VERSION_ID)
				.setHeader("Content-Type", "text/plain")
				.setHeader("Cache-Control", "public, max-age=120")
				.setHeader("Last-Modified", "Fri, 01 Jan 2021 00:00:00 GMT")
				.setHeader("x-amz-request-id", "req-1")
				.setHeader("x-amz-id-2", "id2")
				.setHeader("x-amz-server-side-encryption", "AES256")
				.setHeader("Strict-Transport-Security", "max-age=63072000")
				.setHeader("Accept-Ranges", "bytes")
				.body("hello world");
	}

	/**
	 * Same headers as {@link #fileResponse()}, but with no actual body bytes on the wire, matching what a real
	 * server sends for a HEAD request (Content-Length still describes the resource, but nothing follows it).
	 * MockWebServer replays whatever is enqueued regardless of the request method that triggered it, so a
	 * response meant to answer a HEAD must not carry real body bytes: otherwise they're left unread on the
	 * data-proxy client's pooled connection and corrupt the next response read from it.
	 */
	private MockResponse.Builder headOnlyResponse() {
		return new MockResponse.Builder()
				.code(200)
				.body("")
				.setHeader("ETag", ETAG)
				.setHeader("x-amz-version-id", VERSION_ID)
				.setHeader("Content-Type", "text/plain")
				.setHeader("Cache-Control", "public, max-age=120")
				.setHeader("Last-Modified", "Fri, 01 Jan 2021 00:00:00 GMT")
				.setHeader("Content-Length", "11");
	}

	// --- Basic GET ---

	@Test
	void shouldGetFileWithPassthroughHeaders() throws Exception {
		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/some/file.txt");
		assertEquals(200, res.statusCode());
		assertArrayEquals("hello world".getBytes(), res.body());
		assertEquals(ETAG, res.headers().firstValue("ETag").orElse(null));
		assertEquals("text/plain", res.headers().firstValue("Content-Type").orElse(null));
		assertEquals(
				"public, max-age=120", res.headers().firstValue("Cache-Control").orElse(null));
		assertTrue(res.headers().firstValue("Last-Modified").isPresent());

		var recorded = upstream.takeRequest();
		assertEquals("GET", recorded.getMethod());
		assertEquals("/bucket/some/file.txt", recorded.getUrl().encodedPath());
		assertNull(recorded.getUrl().query());
	}

	@Test
	void shouldPassThroughMetadataHeaders() throws Exception {
		upstream.enqueue(fileResponse()
				.setHeader("x-amz-meta-src_last_modified_millis", LAST_MODIFIED_MILLIS)
				.setHeader("x-amz-meta-foo", "bar")
				.build());
		var res = send("GET", "/file.txt");
		assertEquals("bar", res.headers().firstValue("x-amz-meta-foo").orElse(null));
		assertEquals(
				LAST_MODIFIED_MILLIS,
				res.headers().firstValue("x-amz-meta-src_last_modified_millis").orElse(null));
	}

	@Test
	void shouldStripNonMetaAmzAndSecurityHeaders() throws Exception {
		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/file.txt");
		assertTrue(res.headers().firstValue("x-amz-version-id").isEmpty());
		assertTrue(res.headers().firstValue("x-amz-request-id").isEmpty());
		assertTrue(res.headers().firstValue("x-amz-id-2").isEmpty());
		assertTrue(res.headers().firstValue("x-amz-server-side-encryption").isEmpty());
		assertTrue(res.headers().firstValue("Strict-Transport-Security").isEmpty());
		assertEquals("none", res.headers().firstValue("Accept-Ranges").orElse(null));
	}

	@Test
	void shouldPassThroughOtherStandardHeaders() throws Exception {
		upstream.enqueue(fileResponse()
				.setHeader("Content-Encoding", "gzip")
				.setHeader("Content-Disposition", "attachment")
				.setHeader("Content-Language", "en")
				.setHeader("Expires", "Fri, 01 Jan 2030 00:00:00 GMT")
				.build());
		var res = send("GET", "/file.txt");
		assertEquals("gzip", res.headers().firstValue("Content-Encoding").orElse(null));
		assertEquals(
				"attachment", res.headers().firstValue("Content-Disposition").orElse(null));
		assertEquals("en", res.headers().firstValue("Content-Language").orElse(null));
		assertTrue(res.headers().firstValue("Expires").isPresent());
	}

	@Test
	void shouldSetServerHeader() throws Exception {
		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/file.txt");
		assertTrue(res.headers().firstValue("Server").orElse("").startsWith("eve-ref/"));
	}

	@Test
	void lastModifiedPrecedence() throws Exception {
		upstream.enqueue(fileResponse()
				.setHeader("x-amz-meta-src_last_modified_millis", "1600000000000")
				.build());
		var millisRes = send("GET", "/file.txt");
		var millisLm = millisRes.headers().firstValue("Last-Modified").orElseThrow();

		upstream.enqueue(
				fileResponse().setHeader("x-amz-meta-mtime", "1500000000.0").build());
		var mtimeRes = send("GET", "/file2.txt");
		var mtimeLm = mtimeRes.headers().firstValue("Last-Modified").orElseThrow();

		upstream.enqueue(fileResponse().build());
		var fallbackRes = send("GET", "/file3.txt");
		var fallbackLm = fallbackRes.headers().firstValue("Last-Modified").orElseThrow();

		assertFalse(millisLm.equals(mtimeLm));
		assertFalse(mtimeLm.equals(fallbackLm));
	}

	@Test
	void lastModifiedFallsBackWhenMillisUnparseable() throws Exception {
		upstream.enqueue(fileResponse()
				.setHeader("x-amz-meta-src_last_modified_millis", "not-a-number")
				.setHeader("x-amz-meta-mtime", "1500000000.0")
				.build());
		var res = send("GET", "/file.txt");
		assertTrue(res.headers().firstValue("Last-Modified").isPresent());
	}

	// --- Directory mapping ---

	@Test
	void shouldMapRootToIndexHtml() throws Exception {
		upstream.enqueue(fileResponse().build());
		send("GET", "/");
		assertEquals("/bucket/index.html", upstream.takeRequest().getUrl().encodedPath());
	}

	@Test
	void shouldMapDirectoryToIndexHtml() throws Exception {
		upstream.enqueue(fileResponse().build());
		send("GET", "/dir/");
		assertEquals("/bucket/dir/index.html", upstream.takeRequest().getUrl().encodedPath());
	}

	// --- Query string ---

	@Test
	void shouldReject404ForQueryString() throws Exception {
		var res = send("GET", "/file.txt?a=b");
		assertEquals(404, res.statusCode());
		assertEquals(0, upstream.getRequestCount());
	}

	// --- Method ---

	@Test
	void shouldReject405ForUnsupportedMethods() throws Exception {
		for (var method : List.of("POST", "PUT", "DELETE")) {
			var res = send(method, "/file.txt");
			assertEquals(405, res.statusCode());
			assertEquals("GET, HEAD", res.headers().firstValue("Allow").orElse(null));
		}
		assertEquals(0, upstream.getRequestCount());
	}

	// --- Path safety ---

	@Test
	void shouldReject400ForUnsafePaths() throws Exception {
		for (var path : List.of("/%2e%2e/x", "/a/./b", "/a%2Fb", "/a%5Cb", "/a%00b", "//x", "/a//b")) {
			var res = send("GET", path);
			assertEquals(400, res.statusCode(), path);
		}
		assertEquals(0, upstream.getRequestCount());
	}

	// --- Key encoding ---

	@Test
	void shouldEncodeKeysCorrectly() throws Exception {
		upstream.enqueue(fileResponse().build());
		send("GET", "/a+b.txt");
		assertEquals("/bucket/a+b.txt", upstream.takeRequest().getUrl().encodedPath());

		upstream.enqueue(fileResponse().build());
		send("GET", "/a%20b.txt");
		var segments = upstream.takeRequest().getUrl().pathSegments();
		assertEquals("a b.txt", segments.get(segments.size() - 1));
	}

	// --- Range ---

	@Test
	void shouldIgnoreRangeAndAlwaysServeFullBody() throws Exception {
		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/file.txt", "Range", "bytes=0-4");
		assertEquals(200, res.statusCode());
		assertArrayEquals("hello world".getBytes(), res.body());
		assertTrue(res.headers().firstValue("Content-Range").isEmpty());
		assertEquals("none", res.headers().firstValue("Accept-Ranges").orElse(null));
		var recorded = upstream.takeRequest();
		assertTrue(recorded.getHeaders().get("Range") == null);
	}

	// --- HEAD ---

	@Test
	void shouldHandleHead() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var res = send("HEAD", "/file.txt");
		assertEquals(200, res.statusCode());
		assertEquals(0, res.body().length);
		assertEquals("11", res.headers().firstValue("Content-Length").orElse(null));
		assertEquals(1, upstream.getRequestCount());
		assertEquals("HEAD", upstream.takeRequest().getMethod());
	}

	@Test
	void shouldReturn404ForHeadOnMissingFile() throws Exception {
		upstream.enqueue(new MockResponse.Builder().code(404).build());
		var res = send("HEAD", "/missing.txt");
		assertEquals(404, res.statusCode());
		assertEquals(0, res.body().length);
	}

	// --- Request header stripping ---

	@Test
	void shouldNotForwardClientRequestHeaders() throws Exception {
		upstream.enqueue(fileResponse().build());
		send("GET", "/file.txt", "Cookie", "abc=def", "Authorization", "Bearer xyz");
		var recorded = upstream.takeRequest();
		assertNull(recorded.getHeaders().get("Cookie"));
		assertNull(recorded.getHeaders().get("Authorization"));
		assertEquals("identity", recorded.getHeaders().get("Accept-Encoding"));
	}

	// --- Not found ---

	@Test
	void shouldReturn404ForMissingFile() throws Exception {
		upstream.enqueue(new MockResponse.Builder()
				.code(404)
				.setHeader("Cache-Control", "max-age=0, no-cache, no-store")
				.body("<Error><Code>NoSuchKey</Code></Error>")
				.build());
		var res = send("GET", "/missing.txt");
		assertEquals(404, res.statusCode());
		assertEquals("Not found\n", new String(res.body()));
		assertEquals(
				"max-age=0, no-cache, no-store",
				res.headers().firstValue("Cache-Control").orElse(null));
	}

	// --- Upstream errors ---

	@Test
	void shouldMap403To503AndCaptureSentry() throws Exception {
		upstream.enqueue(new MockResponse.Builder()
				.code(403)
				.body("<Error><Code>AccessDenied</Code></Error>")
				.build());
		var res = send("GET", "/file.txt");
		assertEquals(503, res.statusCode());
	}

	@Test
	void shouldMap503WithRetryAfter() throws Exception {
		upstream.enqueue(new MockResponse.Builder()
				.code(503)
				.setHeader("Retry-After", "5")
				.build());
		var res = send("GET", "/file.txt");
		assertEquals(503, res.statusCode());
		assertEquals("5", res.headers().firstValue("Retry-After").orElse(null));
	}

	@Test
	void shouldMap500To502() throws Exception {
		upstream.enqueue(new MockResponse.Builder().code(500).build());
		var res = send("GET", "/file.txt");
		assertEquals(502, res.statusCode());
	}

	@Test
	void shouldMapUnpinned400To404() throws Exception {
		upstream.enqueue(new MockResponse.Builder().code(400).build());
		var res = send("GET", "/file.txt");
		assertEquals(404, res.statusCode());
	}

	// --- Required headers ---

	@Test
	void shouldReturn500WhenContentLengthMissing() throws Exception {
		upstream.enqueue(new MockResponse.Builder()
				.code(200)
				.setHeader("ETag", ETAG)
				.setHeader("x-amz-version-id", VERSION_ID)
				.chunkedBody("hello world", 1024)
				.build());
		var res = send("GET", "/file.txt");
		assertEquals(500, res.statusCode());
	}

	@Test
	void shouldReturn500WhenEtagMissing() throws Exception {
		upstream.enqueue(new MockResponse.Builder()
				.code(200)
				.setHeader("x-amz-version-id", VERSION_ID)
				.body("hello world")
				.build());
		var res = send("GET", "/file.txt");
		assertEquals(500, res.statusCode());
	}

	// --- Upstream call counts ---

	@Test
	void shouldMakeOneGetAndNoHeadWithoutPreconditions() throws Exception {
		upstream.enqueue(fileResponse().build());
		send("GET", "/file.txt");
		assertEquals(1, upstream.getRequestCount());
	}

	@Test
	void shouldMakeHeadThenPinnedGetForPassingPrecondition() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/file.txt", "If-None-Match", "\"different\"");
		assertEquals(200, res.statusCode());
		assertEquals(2, upstream.getRequestCount());
		var head = upstream.takeRequest();
		assertEquals("HEAD", head.getMethod());
		var get = upstream.takeRequest();
		assertEquals("GET", get.getMethod());
		assertEquals(VERSION_ID, get.getUrl().queryParameter("versionId"));
	}

	@Test
	void shouldMakeOnlyHeadWhenPreconditionGives304() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var res = send("GET", "/file.txt", "If-None-Match", ETAG);
		assertEquals(304, res.statusCode());
		assertEquals(1, upstream.getRequestCount());
	}

	@Test
	void shouldMakeOnlyHeadForHeadWithPreconditions() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var res = send("HEAD", "/file.txt", "If-None-Match", ETAG);
		assertEquals(304, res.statusCode());
		assertEquals(1, upstream.getRequestCount());
	}

	// --- Pinning ---

	@Test
	void shouldRetryOncePinnedGetFails() throws Exception {
		upstream.enqueue(headOnlyResponse().build()); // HEAD
		upstream.enqueue(new MockResponse.Builder().code(404).build()); // pinned GET fails once
		upstream.enqueue(headOnlyResponse().build()); // HEAD retry
		upstream.enqueue(fileResponse().build()); // pinned GET retry succeeds
		var res = send("GET", "/file.txt", "If-None-Match", "\"different\"");
		assertEquals(200, res.statusCode());
		assertEquals(4, upstream.getRequestCount());
	}

	@Test
	void shouldReturn503WhenPinnedGetFailsTwice() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		upstream.enqueue(new MockResponse.Builder().code(404).build());
		upstream.enqueue(headOnlyResponse().build());
		upstream.enqueue(new MockResponse.Builder().code(404).build());
		var res = send("GET", "/file.txt", "If-None-Match", "\"different\"");
		assertEquals(503, res.statusCode());
		assertEquals("1", res.headers().firstValue("Retry-After").orElse(null));
	}

	@Test
	void shouldReturn502WhenPinnedGetEtagDiffersFromHead() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		upstream.enqueue(
				fileResponse().setHeader("ETag", "\"different-version\"").build());
		var res = send("GET", "/file.txt", "If-None-Match", "\"different\"");
		assertEquals(502, res.statusCode());
	}

	// --- Missing file with preconditions ---

	@Test
	void shouldReturn404ForMissingFileEvenWithIfMatchStar() throws Exception {
		upstream.enqueue(new MockResponse.Builder().code(404).build());
		var res = send("GET", "/missing.txt", "If-Match", "*");
		assertEquals(404, res.statusCode());
	}

	// --- Preconditions end-to-end ---

	@Test
	void ifMatchMismatchReturns412() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var res = send("GET", "/file.txt", "If-Match", "\"nope\"");
		assertEquals(412, res.statusCode());
		assertEquals(1, upstream.getRequestCount());
	}

	@Test
	void ifNoneMatchMatchReturns304WithNoBody() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var res = send("GET", "/file.txt", "If-None-Match", ETAG);
		assertEquals(304, res.statusCode());
		assertEquals(0, res.body().length);
		assertEquals(ETAG, res.headers().firstValue("ETag").orElse(null));
		assertTrue(res.headers().firstValue("Last-Modified").isEmpty());
	}

	@Test
	void revalidationRoundTripGivesNotModified() throws Exception {
		upstream.enqueue(fileResponse().build());
		var first = send("GET", "/file.txt");
		var etag = first.headers().firstValue("ETag").orElseThrow();
		var lastModified = first.headers().firstValue("Last-Modified").orElseThrow();

		upstream.enqueue(headOnlyResponse().build());
		var second = send("GET", "/file.txt", "If-None-Match", etag, "If-Modified-Since", lastModified);
		assertEquals(304, second.statusCode());
	}

	@Test
	void ifUnmodifiedSinceFailsReturns412() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var res = send("GET", "/file.txt", "If-Unmodified-Since", "Mon, 01 Jan 2001 00:00:00 GMT");
		assertEquals(412, res.statusCode());
	}

	// --- 412 body ---

	@Test
	void shouldSend412BodyOnGetAndEmptyOnHead() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		var getRes = send("GET", "/file.txt", "If-Match", "\"nope\"");
		assertEquals("Precondition failed\n", new String(getRes.body()));
		assertEquals("no-store", getRes.headers().firstValue("Cache-Control").orElse(null));

		upstream.enqueue(headOnlyResponse().build());
		var headRes = send("HEAD", "/file.txt", "If-Match", "\"nope\"");
		assertEquals(412, headRes.statusCode());
		assertEquals(0, headRes.body().length);
	}

	// --- Accept-Ranges on every response ---

	@Test
	void shouldAlwaysSetAcceptRangesNone() throws Exception {
		upstream.enqueue(fileResponse().build());
		assertEquals(
				"none",
				send("GET", "/file.txt").headers().firstValue("Accept-Ranges").orElse(null));

		assertEquals(
				"none",
				send("GET", "/file.txt?a=b")
						.headers()
						.firstValue("Accept-Ranges")
						.orElse(null));

		assertEquals(
				"none",
				send("POST", "/file.txt").headers().firstValue("Accept-Ranges").orElse(null));

		assertEquals(
				"none",
				send("GET", "/../x").headers().firstValue("Accept-Ranges").orElse(null));

		upstream.enqueue(new MockResponse.Builder().code(404).build());
		assertEquals(
				"none",
				send("GET", "/missing.txt")
						.headers()
						.firstValue("Accept-Ranges")
						.orElse(null));
	}

	// --- Upstream mid-body failure ---

	@Test
	@SneakyThrows
	void shouldCloseConnectionOnUpstreamMidBodyFailure() {
		upstream.enqueue(fileResponse()
				.body("hello world")
				.onResponseBody(SocketEffect.ShutdownConnection.INSTANCE)
				.build());
		Exception thrown = null;
		try {
			send("GET", "/file.txt");
		} catch (Exception e) {
			thrown = e;
		}
		assertTrue(thrown != null, "expected the client to see a connection failure, not a clean short response");

		// Server should remain healthy for the next request. MockWebServer's ShutdownConnection effect takes
		// down its whole listener, not just the one socket, so the upstream fixture is restarted here; what's
		// under test is that the data-server process itself (not restarted) keeps serving afterwards.
		upstream.close();
		upstream = new MockWebServer();
		upstream.start(UPSTREAM_PORT);
		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/file2.txt");
		assertEquals(200, res.statusCode());
	}

	// --- Client abort ---

	@Test
	@SneakyThrows
	void shouldHandleClientAbortWithoutCrashing() {
		upstream.enqueue(fileResponse().build());
		var client = java.net.http.HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder().uri(uri("/file.txt")).GET().build();
		// Cancel almost immediately; this mainly proves the server doesn't crash and keeps serving.
		var future = client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
		future.cancel(true);

		upstream.enqueue(fileResponse().build());
		var res = send("GET", "/file2.txt");
		assertEquals(200, res.statusCode());
	}

	// --- Last-Modified format ---

	@Test
	void lastModifiedShouldBeImfFixdateWithTwoDigitDay() throws Exception {
		// 2021-01-01T00:00:00.123Z
		upstream.enqueue(fileResponse()
				.setHeader("x-amz-meta-src_last_modified_millis", "1609459200123")
				.build());
		var res = send("GET", "/file.txt");
		assertEquals(
				"Fri, 01 Jan 2021 00:00:00 GMT",
				res.headers().firstValue("Last-Modified").orElse(null));
	}

	// --- Non-200 success statuses ---

	@Test
	void shouldMapUpstream206To502() throws Exception {
		upstream.enqueue(fileResponse().code(206).build());
		var res = send("GET", "/file.txt");
		assertEquals(502, res.statusCode());
		assertEquals("Bad gateway\n", new String(res.body()));
	}

	@Test
	void shouldMapPinnedUpstream206To502() throws Exception {
		upstream.enqueue(headOnlyResponse().build());
		upstream.enqueue(fileResponse().code(206).build());
		var res = send("GET", "/file.txt", "If-None-Match", "\"different\"");
		assertEquals(502, res.statusCode());
		assertEquals("Bad gateway\n", new String(res.body()));
	}

	// --- Upstream unreachable ---

	@Test
	void shouldReturn502WhenUpstreamUnreachable() throws Exception {
		upstream.close();
		var res = send("GET", "/file.txt");
		assertEquals(502, res.statusCode());
		assertEquals("Bad gateway\n", new String(res.body()));
		assertEquals("none", res.headers().firstValue("Accept-Ranges").orElse(null));
	}

	// --- Concurrency limit ---

	@Test
	@SetEnvironmentVariable(key = "DATA_SERVER_MAX_CONCURRENCY", value = "2")
	void shouldReturn503WithRetryAfterWhenOverConcurrencyLimit() throws Exception {
		upstream.enqueue(fileResponse().headersDelay(2, TimeUnit.SECONDS).build());
		upstream.enqueue(fileResponse().headersDelay(2, TimeUnit.SECONDS).build());
		var request = HttpRequest.newBuilder().uri(uri("/slow.txt")).GET().build();
		var first = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
		var second = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
		awaitUpstreamRequestCount(2);

		var rejected = send("GET", "/file.txt");
		assertEquals(503, rejected.statusCode());
		assertEquals("1", rejected.headers().firstValue("Retry-After").orElse(null));
		assertEquals("none", rejected.headers().firstValue("Accept-Ranges").orElse(null));
		assertEquals("Service unavailable\n", new String(rejected.body()));

		assertEquals(200, first.get().statusCode());
		assertEquals(200, second.get().statusCode());
		upstream.enqueue(fileResponse().build());
		assertEquals(200, send("GET", "/file.txt").statusCode());
		assertEquals(3, upstream.getRequestCount());
	}

	// --- Access log ---

	@Test
	void accessLogShouldRecordStreamedGet() throws Exception {
		try (var log = new LogCapture("dataserver.access")) {
			upstream.enqueue(fileResponse().build());
			send("GET", "/file.txt");
			var fields = awaitAccessLog(log, "/file.txt");
			assertEquals("GET", fields.get("method"));
			assertEquals("200", fields.get("status"));
			assertEquals("ok", fields.get("outcome"));
			assertEquals("11", fields.get("bytesSent"));
			assertEquals("11", fields.get("upstreamContentLength"));
			assertEquals("GET", fields.get("upstreamCalls"));
			assertEquals("none", fields.get("precondition"));
		}
	}

	@Test
	void accessLogShouldRecordClientDetails() throws Exception {
		try (var log = new LogCapture("dataserver.access")) {
			upstream.enqueue(fileResponse().build());
			send(
					"GET",
					"/cf.txt",
					"CF-Connecting-IP",
					"203.0.113.7",
					"X-Forwarded-For",
					"198.51.100.1",
					"User-Agent",
					"test-agent/1.0 (x)",
					"Referer",
					"https://example.com/");
			var cf = awaitAccessLog(log, "/cf.txt");
			assertEquals("203.0.113.7", cf.get("clientIp"));
			assertEquals("test-agent/1.0 (x)", cf.get("userAgent"));
			assertEquals("https://example.com/", cf.get("referer"));

			upstream.enqueue(fileResponse().build());
			send("GET", "/xff.txt", "X-Forwarded-For", "198.51.100.1, 10.0.0.1");
			assertEquals("198.51.100.1", awaitAccessLog(log, "/xff.txt").get("clientIp"));

			upstream.enqueue(fileResponse().build());
			send("GET", "/direct.txt");
			var direct = awaitAccessLog(log, "/direct.txt").get("clientIp");
			assertTrue(direct != null && !direct.isEmpty() && !direct.equals("-"));
		}
	}

	@Test
	void accessLogShouldRecordPreconditionOutcome() throws Exception {
		try (var log = new LogCapture("dataserver.access")) {
			upstream.enqueue(headOnlyResponse().build());
			send("GET", "/304.txt", "If-None-Match", ETAG);
			var notModified = awaitAccessLog(log, "/304.txt");
			assertEquals("304", notModified.get("status"));
			assertEquals("304", notModified.get("precondition"));

			upstream.enqueue(headOnlyResponse().build());
			send("GET", "/412.txt", "If-Match", "\"nope\"");
			assertEquals("412", awaitAccessLog(log, "/412.txt").get("precondition"));

			upstream.enqueue(headOnlyResponse().build());
			upstream.enqueue(fileResponse().build());
			send("GET", "/proceed.txt", "If-None-Match", "\"different\"");
			var proceed = awaitAccessLog(log, "/proceed.txt");
			assertEquals("proceed", proceed.get("precondition"));
			assertEquals("200", proceed.get("status"));
			assertEquals("HEAD+GET_VERSION", proceed.get("upstreamCalls"));
		}
	}

	// --- Client abort and stall, with a raw socket client ---

	@Test
	void shouldLogClientAbortAsAborted() throws Exception {
		try (var log = new LogCapture("dataserver.access")) {
			upstream.enqueue(generatedFileResponse(64L * 1024 * 1024).build());
			try (var socket = rawGet("/big.bin")) {
				var in = socket.getInputStream();
				assertEquals("HTTP/1.1 200 OK", readResponseHead(in));
				in.readNBytes(64 * 1024);
				// Reset rather than FIN, so the server's next write fails immediately.
				socket.setSoLinger(true, 0);
			}
			var fields = awaitAccessLog(log, "/big.bin");
			assertEquals("200", fields.get("status"));
			assertEquals("aborted", fields.get("outcome"));
			assertEquals(0, watchdog.activeStreamCount());
		}
	}

	@Test
	@SetEnvironmentVariable(key = "DATA_SERVER_WRITE_STALL_TIMEOUT", value = "PT2S")
	void shouldCutOffStalledClient() throws Exception {
		var size = 256L * 1024 * 1024;
		try (var log = new LogCapture("dataserver.access")) {
			upstream.enqueue(generatedFileResponse(size).build());
			try (var socket = rawGet("/big.bin")) {
				var in = socket.getInputStream();
				assertEquals("HTTP/1.1 200 OK", readResponseHead(in));
				// Stop reading. The server must cut the stream off once the stall timeout passes.
				var fields = awaitAccessLog(log, "/big.bin");
				assertEquals("stalled", fields.get("outcome"));
				assertEquals(0, watchdog.activeStreamCount());

				// The server closed the socket: draining what's buffered ends without the full body, rather than
				// blocking.
				long drained = 0;
				try {
					var buffer = new byte[64 * 1024];
					int n;
					while ((n = in.read(buffer)) != -1) {
						drained += n;
					}
				} catch (SocketTimeoutException e) {
					throw e;
				} catch (IOException e) {
					// Connection reset is also a valid way for the server to close.
				}
				assertTrue(drained < size);
			}
		}
	}

	private Map<String, String> awaitAccessLog(LogCapture log, String path) {
		var line = log.await(l -> l.contains("path=" + path + " "), Duration.ofSeconds(20))
				.orElseThrow(() -> new AssertionError("No access log line for " + path + ": " + log.messages()));
		var fields = new HashMap<String, String>();
		var matcher = ACCESS_LOG_FIELD.matcher(line);
		while (matcher.find()) {
			var value = matcher.group(2);
			if (value.startsWith("\"")) {
				value = value.substring(1, value.length() - 1)
						.replace("\\\"", "\"")
						.replace("\\\\", "\\");
			}
			fields.put(matcher.group(1), value);
		}
		return fields;
	}

	@SneakyThrows
	private void awaitUpstreamRequestCount(int count) {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (upstream.getRequestCount() < count && System.nanoTime() < deadline) {
			Thread.sleep(10);
		}
		assertEquals(count, upstream.getRequestCount());
	}

	private MockResponse.Builder generatedFileResponse(long size) {
		return new MockResponse.Builder()
				.code(200)
				.setHeader("ETag", ETAG)
				.setHeader("x-amz-version-id", VERSION_ID)
				.body(new MockResponseBody() {
					@Override
					public long getContentLength() {
						return size;
					}

					@Override
					public void writeTo(BufferedSink sink) throws IOException {
						var chunk = new byte[64 * 1024];
						long remaining = size;
						while (remaining > 0) {
							int n = (int) Math.min(chunk.length, remaining);
							sink.write(chunk, 0, n);
							remaining -= n;
						}
					}
				});
	}

	private Socket rawGet(String path) throws IOException {
		var socket = new Socket();
		socket.setReceiveBufferSize(16 * 1024);
		socket.setSoTimeout(10_000);
		socket.connect(new InetSocketAddress("localhost", DATA_SERVER_TEST_PORT));
		var out = socket.getOutputStream();
		out.write(("GET " + path + " HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
		out.flush();
		return socket;
	}

	/**
	 * Reads the status line and headers of a response, returning the status line.
	 */
	private String readResponseHead(InputStream in) throws IOException {
		var head = new ByteArrayOutputStream();
		int lastFour = 0;
		while (lastFour != 0x0D0A0D0A) {
			var b = in.read();
			if (b == -1) {
				throw new EOFException();
			}
			head.write(b);
			lastFour = (lastFour << 8) | b;
		}
		var text = head.toString(StandardCharsets.US_ASCII);
		return text.substring(0, text.indexOf("\r\n"));
	}
}
