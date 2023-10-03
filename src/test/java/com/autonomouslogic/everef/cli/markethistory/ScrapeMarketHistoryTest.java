package com.autonomouslogic.everef.cli.markethistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * <ul>
 *     <li><code>2023-01-01</code> and <code>2023-01-02</code> exist in the downloaded history.</li>
 *     <li><code>2023-01-01</code> will return from the ESI exactly what's in the file, so data for that date shouldn't be uploaded</li>
 *     <li><code>2023-01-02</code> and <code>2023-01-03</code> will have updated entries from the ESI. Only these two dates will be updated.</li>
 *     <li><code>2023-01-03</code> doesn't exist in the previous history, it will be new.</li>
 *     <li>Pair <code>(10000001, 999)</code> exists in <code>2023-01-02</code>, but isn't returned by the ESI (404). It should remain in the dataset.</li>
 *     <li>Pair <code>(10000001, 21)</code> exists in <code>2023-01-02</code>, but is updated by the ESI. The new dataset should reflect.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMarketHistoryTest.BUCKET_NAME + "/data/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/esi")
@SetEnvironmentVariable(key = "ESI_MARKET_HISTORY_EXPLORATION_GROUPS", value = "1")
public class ScrapeMarketHistoryTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	ScrapeMarketHistory scrapeMarketHistory;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	final String lastModified = "Tue, 03 Jan 2023 13:47:30 GMT";

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

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
	void shouldScrapeMarketHistory() {
		scrapeMarketHistory
				.setMinDate(LocalDate.parse("2023-01-01"))
				.setToday(LocalDate.parse("2023-01-04"))
				.run()
				.blockingAwait();

		assertEquals(
				List.of(
						"data/" + ArchivePathFactory.MARKET_HISTORY.createArchivePath(LocalDate.parse("2023-01-02")),
						"data/" + ArchivePathFactory.MARKET_HISTORY.createArchivePath(LocalDate.parse("2023-01-03")),
						"data/market-history/totals.json"),
				mockS3Adapter.getAllPutKeys(BUCKET_NAME, dataClient).stream()
						.sorted()
						.toList());

		assertEquals(
				loadExpectedArchive(LocalDate.parse("2023-01-02")), loadUploadedArchive(LocalDate.parse("2023-01-02")));
		assertEquals(
				loadExpectedArchive(LocalDate.parse("2023-01-03")), loadUploadedArchive(LocalDate.parse("2023-01-03")));

		var totalPairs = new String(mockS3Adapter
				.getTestObject(BUCKET_NAME, "data/market-history/totals.json", dataClient)
				.orElseThrow());
		assertEquals("{\"2023-01-01\":4,\"2023-01-02\":5,\"2023-01-03\":4,\"2023-01-04\":0}", totalPairs);

		var allRequests = testDataUtil.takeAllRequests(server);

		var allPaths =
				allRequests.stream().map(r -> r.getRequestUrl().encodedPath()).toList();
		// Requested by ActiveOrdersRegionTypeSource.
		assertTrue(allPaths.contains("/esi/markets/10000001/types/"));
		assertTrue(allPaths.contains("/esi/markets/10000002/types/"));
		assertTrue(allPaths.contains("/esi/markets/10000100/types/"));

		var requestedPairs = getRequestedMarketHistoryPairs(allRequests);
		// Present in previous files.
		assertTrue(requestedPairs.contains(new RegionTypePair(10000001, 999)));
		// Traded in other regions, added by TopTradedRegionTypeSource.
		assertTrue(requestedPairs.contains(new RegionTypePair(10000002, 999)));
		// Reported as active, added by ActiveOrdersRegionTypeSource.
		assertTrue(requestedPairs.contains(new RegionTypePair(10000001, 1000)));
		// Present in orders file, added by HistoricalOrdersRegionTypeSource.
		assertTrue(requestedPairs.contains(new RegionTypePair(10000001, 18)));
		assertTrue(requestedPairs.contains(new RegionTypePair(11000031, 74216)));
		// Present in ref data, added by ExplorerRegionTypeSource.
		assertTrue(requestedPairs.contains(new RegionTypePair(10000100, 999)));
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var typeId = request.getRequestUrl().queryParameter("type_id");
				var segments = request.getRequestUrl().pathSegments();
				if (path.equals("/data/market-history/totals.json")) {
					return mockResponse("{\"2023-01-01\":4,\"2023-01-02\":3,\"2023-01-03\":4}");
				}
				if (path.equals("/data/")) {
					return mockResponse(ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/data.html"));
				}
				if (path.equals("/data/market-history/")) {
					return mockResponse(
							ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/market-history.html"));
				}
				if (path.equals("/data/market-history/2023/")) {
					return mockResponse(
							ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/market-history-2023.html"));
				}
				if (path.equals("/data/market-history/2023/market-history-2023-01-01.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2023-01-01"));
				}
				if (path.equals("/data/market-history/2023/market-history-2023-01-02.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2023-01-02"));
				}
				if (path.equals("/data/market-orders/history/2023/2023-01-01/")) {
					return mockResponse(
							ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/market-orders.html"));
				}
				if (path.equals(
						"/data/market-orders/history/2023/2023-01-01/market-orders-2023-01-01_00-16-34.v3.csv.bz2")) {
					return mockOrderDate(LocalDate.parse("2023-01-01"));
				}
				if (path.equals("/esi/markets/10000001/types/")) {
					return mockResponse("[20,21,1000]");
				}
				if (path.equals("/esi/markets/10000002/types/")) {
					return mockResponse("[20,21]");
				}
				if (path.equals("/esi/markets/10000100/types/")) {
					return mockResponse("[]");
				}
				if (path.startsWith("/esi/markets/") && segments.get(3).equals("history")) {
					var regionId = segments.get(2);
					return mockHistory(regionId, typeId);
				}
				var refdataPath = "/data/" + ArchivePathFactory.REFERENCE_DATA.createLatestPath();
				if (path.equals(refdataPath)) {
					return mockRefdata();
				}
				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(500);
			} catch (Exception e) {
				fail("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	@NotNull
	private MockResponse mockResponse(String body) {
		return new MockResponse().setResponseCode(200).setBody(body).addHeader("last-modified", lastModified);
	}

	@NotNull
	private MockResponse mockResponse(byte[] body) {
		return new MockResponse()
				.setResponseCode(200)
				.setBody(new Buffer().write(body))
				.addHeader("last-modified", lastModified);
	}

	@NotNull
	@SneakyThrows
	private MockResponse mockResponse(InputStream in) {
		try (in) {
			var body = IOUtils.toByteArray(in);
			return mockResponse(body).addHeader("last-modified", lastModified);
		}
	}

	@SneakyThrows
	private MockResponse mockHistoricalDate(LocalDate date) {
		if (date.equals(LocalDate.parse("2023-01-03"))) {
			return new MockResponse().setResponseCode(404);
		}
		var file = ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/data-" + date.toString() + ".csv");
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.copy(file, out);
		}
		return mockResponse(new ByteArrayInputStream(compressed.toByteArray()));
	}

	@SneakyThrows
	private MockResponse mockOrderDate(LocalDate date) {
		if (!date.equals(LocalDate.parse("2023-01-01"))) {
			return new MockResponse().setResponseCode(404);
		}
		var file = ResourceUtil.loadContextual(
				ScrapeMarketHistoryTest.class, "/market-orders-" + date.toString() + ".csv");
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.copy(file, out);
		}
		return mockResponse(new ByteArrayInputStream(compressed.toByteArray()));
	}

	@SneakyThrows
	private MockResponse mockHistory(String regionId, String typeId) {
		var notFound = List.of(
				List.of("10000001", "999"),
				List.of("10000002", "999"),
				List.of("10000001", "18"),
				List.of("11000031", "74216"),
				List.of("10000001", "1000"),
				List.of("10000100", "999"));

		if (notFound.contains(List.of(regionId, typeId))) {
			return new MockResponse().setResponseCode(404);
		}
		try {
			return mockResponse(ResourceUtil.loadContextual(
					ScrapeMarketHistoryTest.class, String.format("/%s-%s.json", regionId, typeId)));
		} catch (FileNotFoundException e) {
			log.warn("Unaccounted for market history - region: {}, type: {}", regionId, typeId);
			return new MockResponse().setResponseCode(404);
		}
	}

	private MockResponse mockRefdata() throws JsonProcessingException {
		return testDataUtil.mockResponse(testDataUtil.createXzTar(Map.of(
				"regions.json",
						objectMapper.writeValueAsBytes(Map.of(
								10000001,
								Region.builder()
										.regionId(10000001L)
										.universeId("eve")
										.build(),
								10000002,
								Region.builder()
										.regionId(10000002L)
										.universeId("eve")
										.build(),
								10000100,
								Region.builder()
										.regionId(10000100L)
										.universeId("eve")
										.build())),
				"types.json",
						objectMapper.writeValueAsBytes(Map.of(
								999,
								InventoryType.builder()
										.typeId(999L)
										.marketGroupId(1L)
										.build())))));
	}

	@SneakyThrows
	private String loadExpectedArchive(LocalDate date) {
		return IOUtils.toString(
						ResourceUtil.loadContextual(
								ScrapeMarketHistoryTest.class, "/expected-" + date.toString() + ".csv"),
						StandardCharsets.UTF_8)
				.replaceAll("\r\n", "\n");
	}

	@SneakyThrows
	private String loadUploadedArchive(LocalDate date) {
		var bytes = mockS3Adapter
				.getTestObject(
						BUCKET_NAME, "data/" + ArchivePathFactory.MARKET_HISTORY.createArchivePath(date), dataClient)
				.orElseThrow(() -> new RuntimeException(date.toString()));
		return IOUtils.toString(new BZip2CompressorInputStream(new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8)
				.replaceAll("\r\n", "\n");
	}

	@NotNull
	private List<RegionTypePair> getRequestedMarketHistoryPairs(List<RecordedRequest> allRequests) {
		return allRequests.stream()
				.map(r -> r.getRequestUrl())
				.filter(url -> url.encodedPath().startsWith("/esi/markets/"))
				.filter(url -> url.encodedPath().endsWith("/history/"))
				.map(url -> {
					var region = Integer.parseInt(url.pathSegments().get(2));
					var type = Integer.parseInt(url.queryParameter("type_id"));
					return new RegionTypePair(region, type);
				})
				.toList();
	}
}
