package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.MockDataIndexHelperModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + SyncFuzzworkOrdersetsTest.BUCKET_NAME + "/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data/")
@SetEnvironmentVariable(key = "FUZZWORK_MARKET_BASE_PATH", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
public class SyncFuzzworkOrdersetsTest {
	static final String BUCKET_NAME = "data-bucket";
	static final Pattern ORDERSET_DOWNLOAD = Pattern.compile("/orderbooks/orderset-([0-9]+)\\.csv\\.gz");

	@Inject
	SyncFuzzworkOrdersets syncFuzzworkOrdersets;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	MockWebServer server;
	List<String> existingPaths;
	List<Long> fuzzworkIds;
	Set<Long> unavailableIds;
	List<Long> requestedIds;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockDataIndexHelperModule(new MockDataIndexHelperModule().setDefaultMock(false))
				.build()
				.inject(this);

		existingPaths = new ArrayList<>();
		fuzzworkIds = new ArrayList<>();
		unavailableIds = new HashSet<>();
		requestedIds = Collections.synchronizedList(new ArrayList<>());
		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldOnlySyncOrdersetsAtOrAboveLowestExistingId() {
		existingPaths.add("fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-10.csv.gz");
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(9L, 10L, 50L, 100L, 101L, 102L));

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L, 102L), requestedIds);
		assertEquals(
				List.of(
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-101-2026-01-02_12-00-00.csv.gz",
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-102-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
		assertEquals(
				List.of(
						"fuzzwork/index.json",
						"fuzzwork/ordersets/2026/2026-01-02/index.json",
						"fuzzwork/ordersets/2026/index.json",
						"fuzzwork/ordersets/index.json",
						"index.json"),
				uploadedIndexes());
	}

	@Test
	@SneakyThrows
	void shouldFillGapsAboveLowestExistingId() {
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-102-2026-01-01_01-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(100L, 101L, 102L, 103L, 104L));

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L, 103L, 104L), requestedIds.stream().sorted().toList());
		assertEquals(
				List.of(
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-101-2026-01-02_12-00-00.csv.gz",
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-103-2026-01-02_12-00-00.csv.gz",
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-104-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
		assertEquals(
				List.of(
						"fuzzwork/index.json",
						"fuzzwork/ordersets/2026/2026-01-02/index.json",
						"fuzzwork/ordersets/2026/index.json",
						"fuzzwork/ordersets/index.json",
						"index.json"),
				uploadedIndexes());
	}

	@Test
	@SneakyThrows
	void shouldNotRetryIdsBelowLowestExistingId() {
		// IDs 1-99 were archived into a torrent and deleted from the main archive: the lowest
		// remaining ID is 100, and Fuzzwork's listing still has 50 from before it was archived. It must stay
		// deleted, not be re-downloaded.
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(50L, 100L, 101L));

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L), requestedIds);
		assertEquals(
				List.of("fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-101-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
		assertEquals(
				List.of(
						"fuzzwork/index.json",
						"fuzzwork/ordersets/2026/2026-01-02/index.json",
						"fuzzwork/ordersets/2026/index.json",
						"fuzzwork/ordersets/index.json",
						"index.json"),
				uploadedIndexes());
	}

	@Test
	@SneakyThrows
	void shouldIgnoreBackfills() {
		existingPaths.add("fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-101.csv.gz");
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(100L, 101L));

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L), requestedIds);
		assertEquals(
				List.of("fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-101-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
		assertEquals(
				List.of(
						"fuzzwork/index.json",
						"fuzzwork/ordersets/2026/2026-01-02/index.json",
						"fuzzwork/ordersets/2026/index.json",
						"fuzzwork/ordersets/index.json",
						"index.json"),
				uploadedIndexes());
	}

	@Test
	@SneakyThrows
	void shouldFailWhenNoOrdersetsExist() {
		existingPaths.add("fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-10.csv.gz");
		fuzzworkIds.addAll(List.of(10L, 50L, 100L));

		assertThrows(Exception.class, () -> VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run));

		assertEquals(List.of(), requestedIds);
		assertEquals(List.of(), uploadedOrdersets());
		assertEquals(List.of(), uploadedIndexes());
	}

	@Test
	@SneakyThrows
	void shouldRequestButNotUploadUnavailableOrdersets() {
		// Fuzzwork lists an orderset but 404s on download (not actually available yet / no new data since the
		// last conditional check). It must still be requested every run, but never uploaded.
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(100L, 101L, 102L));
		unavailableIds.add(101L);

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L, 102L), requestedIds.stream().sorted().toList());
		assertEquals(
				List.of("fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-102-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
		assertEquals(
				List.of(
						"fuzzwork/index.json",
						"fuzzwork/ordersets/2026/2026-01-02/index.json",
						"fuzzwork/ordersets/2026/index.json",
						"fuzzwork/ordersets/index.json",
						"index.json"),
				uploadedIndexes());
	}

	private List<String> uploadedOrdersets() {
		return mockS3Adapter.getAllPutKeys(BUCKET_NAME, dataClient).stream()
				.filter(key -> key.endsWith(".csv.gz"))
				.sorted()
				.toList();
	}

	private List<String> uploadedIndexes() {
		return mockS3Adapter.getAllPutKeys(BUCKET_NAME, dataClient).stream()
				.filter(key -> key.endsWith("index.json"))
				.sorted()
				.toList();
	}

	/**
	 * Renders a minimal data site index page, as parsed by <code>DataCrawler</code>.
	 */
	private String renderIndexPage(String dir) {
		var dirs = new TreeMap<String, String>();
		var files = new ArrayList<String>();
		for (var path : existingPaths) {
			if (!path.startsWith(dir)) {
				continue;
			}
			var rest = path.substring(dir.length());
			var slash = rest.indexOf('/');
			if (slash >= 0) {
				var name = rest.substring(0, slash + 1);
				dirs.put(name, "/data/" + dir + name);
			} else {
				files.add("/data/" + path);
			}
		}
		var html = new StringBuilder("<html><body><table>");
		dirs.values().forEach(href -> html.append("<tr class=\"data-dir\"><td><a href=\"")
				.append(href)
				.append("\">dir</a></td></tr>"));
		files.forEach(href -> html.append("<tr class=\"data-file\"><td><a href=\"")
				.append(href)
				.append("\">file</a></td></tr>"));
		return html.append("</table></body></html>").toString();
	}

	private String renderFuzzworkApiPage() {
		return fuzzworkIds.stream()
				.map(id -> "<a href=\"/orderbooks/orderset-" + id + ".csv.gz\">orderset-" + id + "</a>")
				.collect(Collectors.joining("\n", "<html><body>", "</body></html>"));
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			var path = request.getRequestUrl().encodedPath();
			log.debug("Received request: {}", path);
			if (path.startsWith("/data/") && path.endsWith("/")) {
				var dir = path.substring("/data/".length());
				var known = dir.isEmpty() || existingPaths.stream().anyMatch(p -> p.startsWith(dir));
				if (!known) {
					return new MockResponse().setResponseCode(404);
				}
				return new MockResponse().setResponseCode(200).setBody(renderIndexPage(dir));
			}
			if (path.equals("/api/")) {
				return new MockResponse().setResponseCode(200).setBody(renderFuzzworkApiPage());
			}
			var matcher = ORDERSET_DOWNLOAD.matcher(path);
			if (matcher.matches()) {
				var id = Long.parseLong(matcher.group(1));
				requestedIds.add(id);
				if (unavailableIds.contains(id)) {
					return new MockResponse().setResponseCode(404);
				}
				return new MockResponse()
						.setResponseCode(200)
						.setHeader("Last-Modified", "Fri, 02 Jan 2026 12:00:00 GMT")
						.setBody("orderset-" + id);
			}
			log.error("Unaccounted for URL: {}", path);
			return new MockResponse().setResponseCode(404);
		}
	}
}
