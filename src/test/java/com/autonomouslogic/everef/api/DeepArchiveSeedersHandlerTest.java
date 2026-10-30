package com.autonomouslogic.everef.api;

import static com.autonomouslogic.everef.api.DeepArchiveSeedersHandlerTest.API_TEST_PORT;
import static com.autonomouslogic.everef.api.DeepArchiveSeedersHandlerTest.TRACKER_TEST_PORT;
import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
@SetEnvironmentVariable(
		key = "DEEP_ARCHIVE_TRACKER_SCRAPE_URL",
		value = "http://localhost:" + TRACKER_TEST_PORT + "/scrape")
@SetEnvironmentVariable(key = "HTTP_PORT", value = "" + API_TEST_PORT)
@Log4j2
@Timeout(60)
public class DeepArchiveSeedersHandlerTest {
	public static final int API_TEST_PORT = 29218;
	public static final int TRACKER_TEST_PORT = 30160;

	static final String FEED_PATH = "/deep-archive/everef-eve-online-deep-archive.xml";
	static final String HASH_00 = "60a2240f3d29b71e679d9f1bc55cd474f567b346";
	static final String HASH_01 = "2ee41593454ef093e57f06dab430760ff0264d0c";
	static final String HASH_02 = "390ec4fcb927e0aa38d0e86beae88f6664c2b6a0";

	@Inject
	ApiRunner apiRunner;

	@Inject
	JsonMapper jsonMapper;

	MockWebServer dataServer;
	MockWebServer trackerServer;
	HttpClient httpClient;

	volatile Supplier<MockResponse> feedResponse;
	volatile Supplier<MockResponse> trackerResponse;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		feedResponse = () -> feed(HASH_02, HASH_01, HASH_00);
		trackerResponse = () -> scrape(Map.of(HASH_00, 11, HASH_01, 1, HASH_02, 3));

		dataServer = new MockWebServer();
		dataServer.setDispatcher(new DataDispatcher());
		dataServer.start(TEST_PORT);

		trackerServer = new MockWebServer();
		trackerServer.setDispatcher(new TrackerDispatcher());
		trackerServer.start(TRACKER_TEST_PORT);

		apiRunner.startServer();
		httpClient = HttpClient.newHttpClient();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		apiRunner.stop();
		dataServer.shutdown();
		trackerServer.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldReturnSeedersForEveryTorrentInFeed() {
		var before = Instant.now();
		var response = get();
		assertEquals(200, response.statusCode());
		var json = jsonMapper.readTree(response.body());
		assertEquals(Map.of(HASH_02, 3, HASH_01, 1, HASH_00, 11), seeders(json));
		var scrapedAt = Instant.parse(json.get("scraped_at").asString());
		assertFalse(scrapedAt.isBefore(before.minusSeconds(1)));
		assertFalse(scrapedAt.isAfter(Instant.now()));
	}

	@Test
	@SneakyThrows
	void shouldPreserveFeedOrder() {
		var json = jsonMapper.readTree(get().body());
		assertEquals(
				List.of(HASH_02, HASH_01, HASH_00), List.copyOf(seeders(json).keySet()));
	}

	@Test
	@SneakyThrows
	void shouldReturnZeroForTorrentsUnknownToTracker() {
		trackerResponse = () -> scrape(Map.of(HASH_00, 11));
		var json = jsonMapper.readTree(get().body());
		assertEquals(Map.of(HASH_02, 0, HASH_01, 0, HASH_00, 11), seeders(json));
	}

	@Test
	@SneakyThrows
	void shouldIgnoreTorrentsNotInFeed() {
		feedResponse = () -> feed(HASH_00);
		var json = jsonMapper.readTree(get().body());
		assertEquals(Map.of(HASH_00, 11), seeders(json));
	}

	@Test
	@SneakyThrows
	void shouldNormalizeUppercaseInfoHashesInFeed() {
		feedResponse = () -> feed(HASH_00.toUpperCase());
		var json = jsonMapper.readTree(get().body());
		assertEquals(Map.of(HASH_00, 11), seeders(json));
	}

	@Test
	@SneakyThrows
	void shouldSendAllInfoHashesInOneScrapeRequest() {
		get();
		assertEquals(1, trackerServer.getRequestCount());
		var request = trackerServer.takeRequest();
		assertEquals("GET", request.getMethod());
		assertEquals("/scrape", request.getRequestUrl().encodedPath());
		assertEquals(List.of(HASH_02, HASH_01, HASH_00), infoHashes(request));
	}

	@Test
	@SneakyThrows
	void shouldSetHeaders() {
		var response = get();
		var headers = response.headers();
		assertEquals("*", headers.firstValue("Access-Control-Allow-Origin").orElseThrow());
		assertEquals(
				"public, max-age=3600, immutable",
				headers.firstValue("Cache-Control").orElseThrow());
		assertEquals("application/json", headers.firstValue("Content-Type").orElseThrow());
		assertEquals(
				"https://docs.everef.net/datasets/deep-archive.html",
				headers.firstValue("X-Docs").orElseThrow());
	}

	@Test
	@SneakyThrows
	void shouldCacheResults() {
		for (int i = 0; i < 3; i++) {
			assertEquals(200, get().statusCode());
		}
		assertEquals(1, dataServer.getRequestCount());
		assertEquals(1, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldScrapeOnceForConcurrentRequests() {
		var futures = new ArrayList<Future<HttpResponse<String>>>();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < 20; i++) {
				futures.add(executor.submit(this::get));
			}
		}
		for (var future : futures) {
			var response = future.get();
			assertEquals(200, response.statusCode());
			assertEquals(Map.of(HASH_02, 3, HASH_01, 1, HASH_00, 11), seeders(jsonMapper.readTree(response.body())));
		}
		assertEquals(1, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_CACHE_DURATION", value = "PT0S")
	void shouldRefreshAfterCacheExpires() {
		assertEquals(Map.of(HASH_02, 3, HASH_01, 1, HASH_00, 11), seeders(jsonMapper.readTree(get().body())));
		trackerResponse = () -> scrape(Map.of(HASH_00, 12, HASH_01, 2, HASH_02, 4));
		var response = get();
		assertEquals(Map.of(HASH_02, 4, HASH_01, 2, HASH_00, 12), seeders(jsonMapper.readTree(response.body())));
		assertEquals(
				"public, max-age=3600, immutable",
				response.headers().firstValue("Cache-Control").orElseThrow());
		assertEquals(2, dataServer.getRequestCount());
		assertEquals(2, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldNotRefetchFeedWithinCacheDuration() {
		feedResponse = () -> feed(HASH_00);
		assertEquals(Map.of(HASH_00, 11), seeders(jsonMapper.readTree(get().body())));
		feedResponse = () -> feed(HASH_01, HASH_00);
		assertEquals(Map.of(HASH_00, 11), seeders(jsonMapper.readTree(get().body())));
		assertEquals(1, dataServer.getRequestCount());
		assertEquals(1, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_CACHE_DURATION", value = "PT0S")
	void shouldPickUpNewTorrentsAfterCacheExpires() {
		feedResponse = () -> feed(HASH_00);
		assertEquals(Map.of(HASH_00, 11), seeders(jsonMapper.readTree(get().body())));
		feedResponse = () -> feed(HASH_01, HASH_00);
		assertEquals(Map.of(HASH_01, 1, HASH_00, 11), seeders(jsonMapper.readTree(get().body())));
	}

	@Test
	@SneakyThrows
	void shouldNotFetchAnythingBeforeFirstRequest() {
		assertEquals(0, dataServer.getRequestCount());
		assertEquals(0, trackerServer.getRequestCount());
		get();
		assertEquals(1, dataServer.getRequestCount());
		assertEquals(1, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_CACHE_CONTROL_MAX_AGE", value = "PT2M")
	void shouldUseConfiguredCacheControlMaxAge() {
		assertEquals(
				"public, max-age=120, immutable",
				get().headers().firstValue("Cache-Control").orElseThrow());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorWhenTrackerFailsWithoutCache() {
		trackerResponse = () -> new MockResponse().setResponseCode(500);
		var response = get();
		assertEquals(500, response.statusCode());
		assertEquals(
				"An internal error occurred",
				jsonMapper.readTree(response.body()).get("message").asString());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_CACHE_DURATION", value = "PT0S")
	void shouldServeCachedResultWhenRefreshFails() {
		var first = jsonMapper.readTree(get().body());
		trackerResponse = () -> new MockResponse().setResponseCode(500);
		var second = get();
		assertEquals(200, second.statusCode());
		assertEquals(first, jsonMapper.readTree(second.body()));
		assertEquals(2, trackerServer.getRequestCount());
		// Within the retry duration, the tracker isn't contacted again.
		assertEquals(first, jsonMapper.readTree(get().body()));
		assertEquals(2, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_CACHE_DURATION", value = "PT0S")
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_RETRY_DURATION", value = "PT0S")
	void shouldRecoverAfterRetryDuration() {
		get();
		trackerResponse = () -> new MockResponse().setResponseCode(500);
		assertEquals(200, get().statusCode());
		trackerResponse = () -> scrape(Map.of(HASH_00, 12, HASH_01, 2, HASH_02, 4));
		assertEquals(Map.of(HASH_02, 4, HASH_01, 2, HASH_00, 12), seeders(jsonMapper.readTree(get().body())));
		assertEquals(3, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "DEEP_ARCHIVE_SEEDERS_CACHE_DURATION", value = "PT0S")
	void shouldServeCachedResultWhenFeedFails() {
		var first = jsonMapper.readTree(get().body());
		feedResponse = () -> new MockResponse().setResponseCode(503);
		var second = get();
		assertEquals(200, second.statusCode());
		assertEquals(first, jsonMapper.readTree(second.body()));
		assertEquals(1, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorOnTrackerFailureReason() {
		trackerResponse = () -> new MockResponse()
				.setResponseCode(200)
				.setBody(new Buffer().write(bytes("d14:failure reason11:not allowede")));
		assertEquals(500, get().statusCode());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorOnMalformedTrackerResponse() {
		trackerResponse = () -> new MockResponse().setResponseCode(200).setBody("<html>Bad gateway</html>");
		assertEquals(500, get().statusCode());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorOnTruncatedTrackerResponse() {
		trackerResponse = () -> {
			var body = scrapeBytes(Map.of(HASH_00, 11));
			return new MockResponse().setResponseCode(200).setBody(new Buffer().write(body, 0, body.length - 3));
		};
		assertEquals(500, get().statusCode());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorWhenFeedUnavailable() {
		feedResponse = () -> new MockResponse().setResponseCode(404);
		assertEquals(500, get().statusCode());
		assertEquals(0, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorOnMalformedFeed() {
		feedResponse = () -> new MockResponse().setResponseCode(200).setBody("<rss><channel>");
		assertEquals(500, get().statusCode());
		assertEquals(0, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldReturnErrorOnInvalidInfoHashInFeed() {
		feedResponse = () -> feed(HASH_00, "not-a-hash");
		assertEquals(500, get().statusCode());
		assertEquals(0, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldRejectDoctypeInFeed() {
		feedResponse = () -> new MockResponse()
				.setResponseCode(200)
				.setBody("<?xml version=\"1.0\"?><!DOCTYPE rss [<!ENTITY x \"y\">]>" + feedXml(HASH_00));
		assertEquals(500, get().statusCode());
		assertEquals(0, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldReturnEmptyForEmptyFeed() {
		feedResponse = () -> feed();
		var response = get();
		assertEquals(200, response.statusCode());
		assertEquals(Map.of(), seeders(jsonMapper.readTree(response.body())));
		assertEquals(0, trackerServer.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldRejectNonGetMethods() {
		var response = httpClient.send(
				HttpRequest.newBuilder(uri())
						.POST(HttpRequest.BodyPublishers.noBody())
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(405, response.statusCode());
		assertEquals(0, trackerServer.getRequestCount());
	}

	@SneakyThrows
	private HttpResponse<String> get() {
		var response =
				httpClient.send(HttpRequest.newBuilder(uri()).GET().build(), HttpResponse.BodyHandlers.ofString());
		log.info("Response {}: {}", response.statusCode(), response.body());
		return response;
	}

	private static URI uri() {
		return URI.create("http://localhost:" + API_TEST_PORT + "/v1/deep-archive/seeders");
	}

	private static Map<String, Integer> seeders(JsonNode json) {
		var result = new LinkedHashMap<String, Integer>();
		var seeders = json.get("seeders");
		assertNotNull(seeders);
		for (var name : seeders.propertyNames()) {
			assertTrue(seeders.get(name).isIntegralNumber());
			result.put(name, seeders.get(name).asInt());
		}
		return result;
	}

	private static List<String> infoHashes(RecordedRequest request) {
		var query = request.getRequestUrl().encodedQuery();
		var hashes = new ArrayList<String>();
		for (var param : query.split("&")) {
			var parts = param.split("=", 2);
			assertEquals("info_hash", parts[0]);
			var raw = URLDecoder.decode(parts[1], StandardCharsets.ISO_8859_1);
			hashes.add(HexFormat.of().formatHex(raw.getBytes(StandardCharsets.ISO_8859_1)));
		}
		return hashes;
	}

	private static String feedXml(String... hashes) {
		var xml = new StringBuilder();
		xml.append("<rss version=\"2.0\" xmlns:torrent=\"http://xmlns.ezrss.it/0.1/\"><channel>")
				.append("<title>EVE Ref Deep Archive</title>");
		for (var hash : hashes) {
			xml.append("<item>")
					.append("<guid isPermaLink=\"false\">")
					.append(hash)
					.append("</guid>")
					.append("<torrent:infoHash>")
					.append(hash)
					.append("</torrent:infoHash>")
					.append("</item>");
		}
		xml.append("</channel></rss>");
		return xml.toString();
	}

	private static MockResponse feed(String... hashes) {
		return new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/rss+xml")
				.setBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + feedXml(hashes));
	}

	private static MockResponse scrape(Map<String, Integer> seeders) {
		return new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "text/plain")
				.setBody(new Buffer().write(scrapeBytes(seeders)));
	}

	@SneakyThrows
	private static byte[] scrapeBytes(Map<String, Integer> seeders) {
		var out = new ByteArrayOutputStream();
		out.write(bytes("d5:filesd"));
		for (var hash : seeders.keySet().stream().sorted().toList()) {
			out.write(bytes("20:"));
			out.write(HexFormat.of().parseHex(hash));
			out.write(bytes("d8:completei" + seeders.get(hash) + "e10:downloadedi0e10:incompletei5ee"));
		}
		out.write(bytes("ee"));
		return out.toByteArray();
	}

	private static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.US_ASCII);
	}

	class DataDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			if (FEED_PATH.equals(request.getPath())) {
				return feedResponse.get();
			}
			return new MockResponse().setResponseCode(404);
		}
	}

	class TrackerDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			if ("/scrape".equals(request.getRequestUrl().encodedPath())) {
				return trackerResponse.get();
			}
			return new MockResponse().setResponseCode(404);
		}
	}
}
