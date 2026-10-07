package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
@SetEnvironmentVariable(
		key = "FUZZWORK_MARKET_BASE_PATH",
		value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
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
	List<Long> downloadedIds;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		existingPaths = new ArrayList<>();
		fuzzworkIds = new ArrayList<>();
		downloadedIds = Collections.synchronizedList(new ArrayList<>());
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
	void shouldNotSyncOrdersetsBelowLowestExistingId() {
		existingPaths.add(
				"fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-10.csv.gz");
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(9L, 10L, 50L, 100L, 101L));

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L), downloadedIds);
		assertEquals(
				List.of("fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-101-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
	}

	@Test
	@SneakyThrows
	void shouldSyncMissingOrdersetsAboveLowestExistingId() {
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-100-2026-01-01_00-00-00.csv.gz");
		existingPaths.add("fuzzwork/ordersets/2026/2026-01-01/fuzzwork-orderset-102-2026-01-01_01-00-00.csv.gz");
		fuzzworkIds.addAll(List.of(100L, 101L, 102L, 103L));

		VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run);

		assertEquals(List.of(101L, 103L), downloadedIds.stream().sorted().toList());
		assertEquals(
				List.of(
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-101-2026-01-02_12-00-00.csv.gz",
						"fuzzwork/ordersets/2026/2026-01-02/fuzzwork-orderset-103-2026-01-02_12-00-00.csv.gz"),
				uploadedOrdersets());
	}

	@Test
	@SneakyThrows
	void shouldFailWhenNoOrdersetsExist() {
		existingPaths.add(
				"fuzzwork/ordersets/backfills/caden-hunter-fuzzwork-ordersets/orderset-10.csv.gz");
		fuzzworkIds.addAll(List.of(10L, 50L, 100L));

		assertThrows(Exception.class, () -> VirtualThreads.onVirtualThread(syncFuzzworkOrdersets::run));

		assertEquals(List.of(), downloadedIds);
		assertEquals(List.of(), uploadedOrdersets());
	}

	private List<String> uploadedOrdersets() {
		return mockS3Adapter.getAllPutKeys(BUCKET_NAME, dataClient).stream()
				.filter(key -> key.endsWith(".csv.gz"))
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
				downloadedIds.add(id);
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
