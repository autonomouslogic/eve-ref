package com.autonomouslogic.everef.cli.marketorders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * End-to-end tests for {@link ScrapeMarketOrders}. Each test method is self-contained: it sets up the
 * ESI mock, runs the scrape, and asserts the resulting archive and ESI calls.
 */
@ExtendWith(MockitoExtension.class)
@Timeout(30)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMarketOrdersTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/base")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "SCRAPE_CHARACTER_OWNER_HASH", value = "scrape-owner-hash")
public class ScrapeMarketOrdersTest {
	static final String BUCKET_NAME = "data-bucket";

	private static final String ARCHIVE_FILE =
			"base/market-orders/history/2020/2020-01-02/market-orders-2020-01-02_03-04-05.v3.csv.bz2";
	private static final String LATEST_FILE = "base/market-orders/market-orders-latest.v3.csv.bz2";
	private static final String STRUCTURES_FILE = "/base/structures/structures-latest.v2.json";

	@Inject
	ScrapeMarketOrders scrapeMarketOrders;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	DataIndexHelper dataIndexHelper;

	@Inject
	JsonMapper objectMapper;

	@Mock
	LocationPopulator locationPopulator;

	MockWebServer server;

	private List<Map<String, String>> records;
	private List<RecordedRequest> requests;
	private List<String> requestPaths;
	private byte[] content;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		lenient().when(locationPopulator.populate(any(), any())).thenAnswer(MockLocationPopulatorModule.mockPopulate());

		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	// ############ Region orders

	/**
	 * A single region with one order on one page. Only that order should appear in the archive and
	 * region market orders are fetched without authentication.
	 */
	@Test
	@SneakyThrows
	void singleRegionOrder() {
		var order = regionOrder(6001, 60000001, 34);
		server.setDispatcher(dispatcher().withRegion(10000001).withRegionOrders(10000001, ordersJson(order)));
		run();

		assertRecords(expectedRegionOrders(10000001, List.of(order)));
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A single region with two orders on one page. Both orders should appear in the archive.
	 */
	@Test
	@SneakyThrows
	void twoOrdersInOneRegion() {
		var order1 = regionOrder(6001, 60000001, 34);
		var order2 = regionOrder(6002, 60000002, 35);
		server.setDispatcher(dispatcher().withRegion(10000001).withRegionOrders(10000001, ordersJson(order1, order2)));
		run();

		assertRecords(expectedRegionOrders(10000001, List.of(order1, order2)));
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A single region with one order on each of two pages. Both pages should be fetched and both orders
	 * should appear in the archive.
	 */
	@Test
	@SneakyThrows
	void paginatedRegionOrders() {
		var page1 = regionOrder(6001, 60000001, 34);
		var page2 = regionOrder(6002, 60000002, 35);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, 1, ordersJson(page1))
				.withRegionOrders(10000001, 2, ordersJson(page2)));
		run();

		assertRecords(expectedRegionOrders(10000001, List.of(page1, page2)));
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * Two regions, each with one order. Both regions should be scraped and both orders should appear in
	 * the archive.
	 */
	@Test
	@SneakyThrows
	void ordersInTwoRegions() {
		var order1 = regionOrder(6001, 60000001, 34);
		var order2 = regionOrder(6002, 60000002, 35);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegion(10000002)
				.withRegionOrders(10000001, ordersJson(order1))
				.withRegionOrders(10000002, ordersJson(order2)));
		run();

		var expected = new ArrayList<Map<String, String>>();
		expected.addAll(expectedRegionOrders(10000001, List.of(order1)));
		expected.addAll(expectedRegionOrders(10000002, List.of(order2)));
		assertRecords(expected);
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/10000002/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/10000002/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * Orders are written sorted by region, type, buy/sell, system, then order id. This asserts the
	 * exact on-disk order rather than treating the records as an unordered set.
	 */
	@Test
	@SneakyThrows
	void ordersWrittenInSortedOrder() {
		var sellTypeA = regionOrder(6003, 60000003, 34);
		var buyTypeA = regionOrder(6001, 60000001, 34).put("is_buy_order", true);
		var sellTypeB = regionOrder(6002, 60000002, 35);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson(sellTypeA, buyTypeA, sellTypeB)));
		run();

		var orderIds = records.stream().map(m -> m.get("order_id")).toList();
		assertEquals(List.of("6003", "6001", "6002"), orderIds);
		assertDataIndex();
	}

	// ############ Structure orders

	/**
	 * A single market structure with one order. Structure market orders are fetched with the scrape
	 * owner's access token and the structure's location data is applied to the order.
	 */
	@Test
	@SneakyThrows
	void structureMarketOrder() {
		var structureId = 1000000000001L;
		var order = structureOrder(6101, structureId, 34);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson())
				.withStructures(structuresJson(marketStructure(structureId, 30000001, 20000001, 10000010)))
				.withStructureOrders(structureId, ordersJson(order)));
		run();

		assertRecords(expectedStructureOrders(List.of(order), 30000001, 20000001, 10000010));
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/structures/1000000000001/?datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A market structure with one order on each of two pages. Both pages should be fetched and both
	 * orders should appear in the archive.
	 */
	@Test
	@SneakyThrows
	void paginatedStructureOrders() {
		var structureId = 1000000000001L;
		var page1 = structureOrder(6101, structureId, 34);
		var page2 = structureOrder(6102, structureId, 35);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson())
				.withStructures(structuresJson(marketStructure(structureId, 30000001, 20000001, 10000010)))
				.withStructureOrders(structureId, 1, ordersJson(page1))
				.withStructureOrders(structureId, 2, ordersJson(page2)));
		run();

		assertRecords(expectedStructureOrders(List.of(page1, page2), 30000001, 20000001, 10000010));
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/structures/1000000000001/?datasource=tranquility&language=en&page=1",
				"/latest/markets/structures/1000000000001/?datasource=tranquility&language=en&page=2",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A market structure with no location data (no system, constellation, or region). The scrape must
	 * not fail; the order is written with the location fields left unresolved.
	 */
	@Test
	@SneakyThrows
	void structureOrderWithoutLocation() {
		var structureId = 1000000000004L;
		var order = structureOrder(6104, structureId, 34);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson())
				.withStructures(structuresJson(marketStructureNoLocation(structureId)))
				.withStructureOrders(structureId, ordersJson(order)));
		run();

		assertRecords(expectedStructureOrdersNoLocation(List.of(order)));
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/structures/1000000000004/?datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A structure that is not a market structure must not be scraped for orders.
	 */
	@Test
	@SneakyThrows
	void nonMarketStructureNotScraped() {
		var structureId = 1000000000002L;
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson())
				.withStructures(structuresJson(nonMarketStructure(structureId, 30000001, 20000001, 10000010))));
		run();

		assertRecords(List.of());
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A market structure whose market orders endpoint returns 403 Forbidden. The forbidden response is
	 * ignored and the scrape succeeds with no orders from that structure.
	 */
	@Test
	@SneakyThrows
	void forbiddenStructureIgnored() {
		var structureId = 1000000000003L;
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson())
				.withStructures(structuresJson(marketStructure(structureId, 30000001, 20000001, 10000010)))
				.withStructureOrderError(structureId, 403));
		run();

		assertRecords(List.of());
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/structures/1000000000003/?datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	/**
	 * A structure market returns an order whose id is already present from a region scrape. The region
	 * order must be kept and the duplicate structure order must be dropped.
	 */
	@Test
	@SneakyThrows
	void structureOrderDoesNotOverwriteRegionOrder() {
		var structureId = 1000000000001L;
		var regionOrder = regionOrder(6001, 60000001, 34);
		var duplicateStructureOrder = structureOrder(6001, structureId, 99);
		var newStructureOrder = structureOrder(6102, structureId, 35);
		server.setDispatcher(dispatcher()
				.withRegion(10000001)
				.withRegionOrders(10000001, ordersJson(regionOrder))
				.withStructures(structuresJson(marketStructure(structureId, 30000001, 20000001, 10000010)))
				.withStructureOrders(structureId, ordersJson(duplicateStructureOrder, newStructureOrder)));
		run();

		var expected = new ArrayList<Map<String, String>>();
		expected.addAll(expectedRegionOrders(10000001, List.of(regionOrder)));
		expected.addAll(expectedStructureOrders(List.of(newStructureOrder), 30000001, 20000001, 10000010));
		assertRecords(expected);
		assertLatestFileMatches();
		assertAuth();
		assertRequestPaths(
				STRUCTURES_FILE,
				"/latest/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=1",
				"/latest/markets/structures/1000000000001/?datasource=tranquility&language=en&page=1",
				"/universe/regions/10000001/?datasource=tranquility",
				"/universe/regions/?datasource=tranquility");
		assertDataIndex();
	}

	// --- Run and capture ---

	@SneakyThrows
	private void run() {
		VirtualThreads.onVirtualThread(() -> scrapeMarketOrders
				.setScrapeTime(ZonedDateTime.parse("2020-01-02T03:04:05Z"))
				.run());
		content = mockS3Adapter
				.getTestObject(BUCKET_NAME, ARCHIVE_FILE, dataClient)
				.orElseThrow();
		records = testDataUtil.readMapsFromBz2Csv(content);
		requests = new ArrayList<>();
		RecordedRequest req;
		while ((req = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) requests.add(req);
		requestPaths = requests.stream().map(RecordedRequest::getPath).sorted().toList();
	}

	// --- Assertion helpers ---

	/** Compares records order-independently; use {@link #ordersWrittenInSortedOrder()} for order. */
	private void assertRecords(List<Map<String, String>> expected) {
		assertEquals(sortedStrings(expected), sortedStrings(records));
	}

	private List<String> sortedStrings(List<Map<String, String>> rows) {
		return rows.stream().map(m -> new TreeMap<>(m).toString()).sorted().toList();
	}

	/** Region market orders are unauthenticated; structure market orders use the owner's token. */
	private void assertAuth() {
		for (var request : requests) {
			var path = request.getRequestUrl().encodedPath();
			var auth = request.getHeaders().get("Authorization");
			if (path.contains("/markets/structures/")) {
				assertEquals("Bearer oauth2-token", auth, path);
			} else if (path.matches(".*/markets/\\d+/orders")) {
				assertNull(auth, path);
			}
		}
	}

	private void assertLatestFileMatches() {
		mockS3Adapter.assertSameContent(BUCKET_NAME, ARCHIVE_FILE, LATEST_FILE, dataClient);
	}

	private void assertRequestPaths(String... paths) {
		assertEquals(List.of(paths), requestPaths);
	}

	private void assertDataIndex() {
		Mockito.verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder().bucket(BUCKET_NAME).path(LATEST_FILE).build(),
						S3Url.builder().bucket(BUCKET_NAME).path(ARCHIVE_FILE).build());
	}

	// --- Order builders ---

	/** A base market order without location resolution fields. */
	private ObjectNode order(long orderId, long locationId, int typeId) {
		return objectMapper
				.createObjectNode()
				.put("duration", 90)
				.put("is_buy_order", false)
				.put("issued", "2023-03-11T17:59:02Z")
				.put("location_id", locationId)
				.put("min_volume", 1)
				.put("order_id", orderId)
				.put("price", 1000000)
				.put("range", "region")
				.put("type_id", typeId)
				.put("volume_remain", 10)
				.put("volume_total", 10);
	}

	/** A region order as returned by the public markets endpoint, which includes the system id. */
	private ObjectNode regionOrder(long orderId, long locationId, int typeId) {
		return order(orderId, locationId, typeId).put("system_id", 30000001);
	}

	/** A structure order as returned by the structure markets endpoint; location_id is the structure. */
	private ObjectNode structureOrder(long orderId, long structureId, int typeId) {
		return order(orderId, structureId, typeId);
	}

	@SneakyThrows
	private String ordersJson(ObjectNode... orders) {
		var array = objectMapper.createArrayNode();
		for (var order : orders) array.add(order);
		return objectMapper.writeValueAsString(array);
	}

	@SneakyThrows
	private List<Map<String, String>> readMaps(List<ObjectNode> orders) {
		var array = objectMapper.createArrayNode();
		orders.forEach(array::add);
		return testDataUtil.readMapsFromJson(new ByteArrayInputStream(objectMapper.writeValueAsBytes(array)));
	}

	private List<Map<String, String>> expectedRegionOrders(long regionId, List<ObjectNode> orders) {
		var maps = readMaps(orders);
		maps.forEach(m -> {
			m.put("region_id", String.valueOf(regionId));
			m.put("station_id", m.get("location_id"));
			m.put("constellation_id", "999");
		});
		return maps;
	}

	private List<Map<String, String>> expectedStructureOrders(
			List<ObjectNode> orders, long systemId, long constellationId, long regionId) {
		var maps = readMaps(orders);
		maps.forEach(m -> {
			m.put("system_id", String.valueOf(systemId));
			m.put("constellation_id", String.valueOf(constellationId));
			m.put("region_id", String.valueOf(regionId));
			m.put("station_id", "999");
		});
		return maps;
	}

	private List<Map<String, String>> expectedStructureOrdersNoLocation(List<ObjectNode> orders) {
		var maps = readMaps(orders);
		maps.forEach(m -> {
			m.put("system_id", "999");
			m.put("constellation_id", "999");
			m.put("region_id", "999");
			m.put("station_id", "999");
		});
		return maps;
	}

	// --- Structure builders ---

	private ObjectNode marketStructure(long structureId, long systemId, long constellationId, long regionId) {
		return objectMapper
				.createObjectNode()
				.put("structure_id", structureId)
				.put("is_market_structure", true)
				.put("solar_system_id", systemId)
				.put("constellation_id", constellationId)
				.put("region_id", regionId);
	}

	private ObjectNode marketStructureNoLocation(long structureId) {
		return objectMapper.createObjectNode().put("structure_id", structureId).put("is_market_structure", true);
	}

	private ObjectNode nonMarketStructure(long structureId, long systemId, long constellationId, long regionId) {
		return objectMapper
				.createObjectNode()
				.put("structure_id", structureId)
				.put("is_market_structure", false)
				.put("solar_system_id", systemId)
				.put("constellation_id", constellationId)
				.put("region_id", regionId);
	}

	@SneakyThrows
	private String structuresJson(ObjectNode... structures) {
		var obj = objectMapper.createObjectNode();
		for (var structure : structures) {
			obj.set(String.valueOf(structure.get("structure_id").asLong()), structure);
		}
		return objectMapper.writeValueAsString(obj);
	}

	// --- Dispatcher builder ---

	private TestDispatcher dispatcher() {
		return new TestDispatcher();
	}

	class TestDispatcher extends Dispatcher {
		private final List<Long> regionIds = new ArrayList<>();
		// key: regionId/structureId, value: map of page -> JSON body (-1 = wildcard for all pages)
		private final Map<Long, Map<Integer, String>> regionOrdersByPage = new HashMap<>();
		private final Map<Long, Map<Integer, String>> structureOrdersByPage = new HashMap<>();
		private final Map<Long, Integer> structureOrderErrorCodes = new HashMap<>();
		private String structuresBody = "{}";

		TestDispatcher withRegion(long id) {
			regionIds.add(id);
			return this;
		}

		TestDispatcher withRegionOrders(long regionId, String jsonBody) {
			regionOrdersByPage.computeIfAbsent(regionId, k -> new HashMap<>()).put(-1, jsonBody);
			return this;
		}

		TestDispatcher withRegionOrders(long regionId, int page, String jsonBody) {
			regionOrdersByPage.computeIfAbsent(regionId, k -> new HashMap<>()).put(page, jsonBody);
			return this;
		}

		TestDispatcher withStructures(String jsonBody) {
			structuresBody = jsonBody;
			return this;
		}

		TestDispatcher withStructureOrders(long structureId, String jsonBody) {
			structureOrdersByPage
					.computeIfAbsent(structureId, k -> new HashMap<>())
					.put(-1, jsonBody);
			return this;
		}

		TestDispatcher withStructureOrders(long structureId, int page, String jsonBody) {
			structureOrdersByPage
					.computeIfAbsent(structureId, k -> new HashMap<>())
					.put(page, jsonBody);
			return this;
		}

		TestDispatcher withStructureOrderError(long structureId, int statusCode) {
			structureOrderErrorCodes.put(structureId, statusCode);
			return this;
		}

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			try {
				var url = request.getRequestUrl();
				var path = url.encodedPath();
				var segments = url.pathSegments();
				var pageParam = url.queryParameter("page");
				var pageNum = pageParam != null ? Integer.parseInt(pageParam) : 1;

				switch (path) {
					case "/universe/regions/", "/latest/universe/regions/":
						return mockJson(regionIds.toString());
					case STRUCTURES_FILE:
						return mockJson(structuresBody);
				}
				if (path.startsWith("/universe/regions/") || path.startsWith("/latest/universe/regions/")) {
					var idx = segments.contains("latest") ? 3 : 2;
					var regionId = Long.parseLong(segments.get(idx));
					return mockJson("{\"region_id\":" + regionId + ",\"name\":\"Region " + regionId
							+ "\",\"constellations\":[]}");
				}
				if (path.startsWith("/markets/structures/") || path.startsWith("/latest/markets/structures/")) {
					var idx = segments.contains("latest") ? 3 : 2;
					var structureId = Long.parseLong(segments.get(idx));
					if (structureOrderErrorCodes.containsKey(structureId)) {
						return new MockResponse().setResponseCode(structureOrderErrorCodes.get(structureId));
					}
					return ordersResponse(structureOrdersByPage.get(structureId), pageNum);
				}
				if (path.startsWith("/markets/") || path.startsWith("/latest/markets/")) {
					var idx = segments.contains("latest") ? 2 : 1;
					var regionId = Long.parseLong(segments.get(idx));
					return ordersResponse(regionOrdersByPage.get(regionId), pageNum);
				}

				log.error("Unaccounted URL: {}", path);
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}

		private MockResponse ordersResponse(Map<Integer, String> pages, int pageNum) {
			if (pages == null) return new MockResponse().setResponseCode(404);
			var body = pages.containsKey(pageNum) ? pages.get(pageNum) : pages.get(-1);
			if (body == null) return new MockResponse().setResponseCode(404);
			var totalPages = pages.containsKey(-1) ? 1 : pages.size();
			return mockJson(body).addHeader("X-Pages", String.valueOf(totalPages));
		}
	}

	// --- HTTP helpers ---

	@NotNull
	private MockResponse mockJson(String body) {
		return new MockResponse().setResponseCode(200).setBody(body);
	}
}
