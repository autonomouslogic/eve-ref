package com.autonomouslogic.everef.cli.marketorders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.commons.ListUtil;
import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseSystemsSystemIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseSystemsSystemIdPosition;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Timeout(30)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMarketOrdersTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/base")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "SCRAPE_CHARACTER_OWNER_HASH", value = "scrape-owner-hash")
@Log4j2
public class ScrapeMarketOrdersTest {
	static final String BUCKET_NAME = "data-bucket";

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
	ObjectMapper objectMapper;

	@Mock
	LocationPopulator locationPopulator;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				// .mockLocationPopulatorModule(new
				// MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			@SneakyThrows
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				return switch (recordedRequest.getPath()) {
					case "/universe/regions/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("[10000001,10000002]");
					case "/universe/systems/30000001/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody(objectMapper.writeValueAsString(new GetUniverseSystemsSystemIdOk(
									20000001,
									"test",
									new GetUniverseSystemsSystemIdPosition(0, 0, 0),
									0,
									30000001,
									null,
									null,
									null,
									null,
									null)));
					case "/universe/regions/10000001/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("{\"region_id\":10000001,\"name\":\"Derelik\",\"constellations\":[]}");
					case "/universe/regions/10000002/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("{\"region_id\":10000002,\"name\":\"The Forge\",\"constellations\":[]}");
					case "/markets/10000001/orders?order_type=all&datasource=tranquility&language=en" -> mockRegionOrders(
							recordedRequest, 10000001, 1, 2);
					case "/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=2" -> mockRegionOrders(
							recordedRequest, 10000001, 2, 2);
					case "/markets/10000002/orders?order_type=all&datasource=tranquility&language=en" -> mockRegionOrders(
							recordedRequest, 10000002, 1, 2);
					case "/markets/10000002/orders?order_type=all&datasource=tranquility&language=en&page=2" -> mockRegionOrders(
							recordedRequest, 10000002, 2, 2);
					case "/base/structures/structures-latest.v2.json" -> mockStructures();
					case "/markets/structures/1000000000001/?datasource=tranquility&language=en" -> mockStructureOrders(
							recordedRequest, 1000000000001L, 1, 2);
					case "/markets/structures/1000000000001/?datasource=tranquility&language=en&page=2" -> mockStructureOrders(
							recordedRequest, 1000000000001L, 2, 2);
					case "/markets/structures/1000000000002/?datasource=tranquility&language=en" -> throw new RuntimeException(
							"Fetch non-market structure");
					case "/markets/structures/1000000000003/?datasource=tranquility&language=en" -> new MockResponse()
							.setResponseCode(403);
					case "/markets/structures/1000000000004/?datasource=tranquility&language=en" -> mockStructureOrders(
							recordedRequest, 1000000000004L, 1, 1);
					default -> {
						log.warn("Unaccounted for URL: {}", recordedRequest.getPath());
						yield new MockResponse().setResponseCode(404);
					}
				};
			}
		});
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldScrapeMarketOrders() {
		// Run.
		scrapeMarketOrders
				.setScrapeTime(ZonedDateTime.parse("2020-01-02T03:04:05Z"))
				.run()
				.blockingAwait();
		// Get saved file.
		var archiveFile = "base/market-orders/history/2020/2020-01-02/market-orders-2020-01-02_03-04-05.v3.csv.bz2";
		var latestFile = "base/market-orders/market-orders-latest.v3.csv.bz2";
		var content = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();
		// Assert records.
		var records = testDataUtil.readMapsFromBz2Csv(content).stream()
				.map(m -> new TreeMap<>(m).toString())
				.collect(Collectors.joining("\n"));
		var expected = ListUtil.concat(
						loadRegionOrderMaps(10000001, 1),
						loadRegionOrderMaps(10000001, 2),
						loadRegionOrderMaps(10000002, 1),
						loadRegionOrderMaps(10000002, 2),
						loadStructureOrderMaps(1000000000001L, 1),
						loadStructureOrderMaps(1000000000001L, 2),
						loadStructureOrderMaps(1000000000004L, 1))
				.stream()
				.sorted(Ordering.compound(List.of(
						Ordering.natural()
								.nullsLast()
								.onResultOf(m -> StringUtils.isNotBlank(m.get("region_id"))
										? Long.parseLong(m.get("region_id"))
										: null),
						Ordering.natural()
								.nullsLast()
								.onResultOf(m -> StringUtils.isNotBlank(m.get("type_id"))
										? Long.parseLong(m.get("type_id"))
										: null))))
				.map(m -> new TreeMap<>(m).toString())
				.collect(Collectors.joining("\n"));
		assertEquals(expected, records);
		// Assert the two files are the same.
		mockS3Adapter.assertSameContent(BUCKET_NAME, archiveFile, latestFile, dataClient);

		// Assert data index.
		Mockito.verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket("data-bucket")
								.path("base/market-orders/market-orders-latest.v3.csv.bz2")
								.build(),
						S3Url.builder()
								.bucket("data-bucket")
								.path(
										"base/market-orders/history/2020/2020-01-02/market-orders-2020-01-02_03-04-05.v3.csv.bz2")
								.build());
	}

	@Test
	@Disabled
	void _shouldScrapeMarketOrders() {}

	@Test
	@Disabled
	void shouldSaveKeysInSpecificOrder() {}

	@Test
	@Disabled
	void shouldScrapeMultiplePages() {}

	@Test
	@Disabled
	void shouldScrapeStructureOrders() {}

	@Test
	@Disabled
	void shouldScrapeMultiplePageStructureOrders() {}

	@Test
	@Disabled
	void shouldNotOverwriteExistingMarketOrderWithStructureOrder() {}

	@Test
	@Disabled
	void shouldIgnore403ForbiddenOnStructureMarkets() {}

	@Test
	@Disabled
	void shouldNotScrapeStructuresWithoutMarkets() {}

	@Test
	@Disabled
	void shouldNotFailIfStructureDoesntHaveLocation() {}

	@SneakyThrows
	private MockResponse mockRegionOrders(RecordedRequest recordedRequest, int regionId, int page, int pages) {
		assertNull(recordedRequest.getHeaders().get("Authorization"));
		var bytes = IOUtils.toByteArray(loadRegionOrders(regionId, page));
		return new MockResponse()
				.setResponseCode(200)
				.setBody(new String(bytes))
				.addHeader("X-Pages", Integer.toString(pages));
	}

	@SneakyThrows
	private MockResponse mockStructureOrders(RecordedRequest recordedRequest, long structureId, int page, int pages) {
		assertEquals("Bearer oauth2-token", recordedRequest.getHeaders().get("Authorization"));
		var bytes = IOUtils.toByteArray(loadStructureOrders(structureId, page));
		return new MockResponse()
				.setResponseCode(200)
				.setBody(new String(bytes))
				.addHeader("X-Pages", Integer.toString(pages));
	}

	@SneakyThrows
	private InputStream loadRegionOrders(int regionId, int page) {
		return ResourceUtil.loadContextual(getClass(), "/market-orders-" + regionId + "-" + page + ".json");
	}

	@SneakyThrows
	private InputStream loadStructureOrders(long structureId, int page) {
		return ResourceUtil.loadContextual(getClass(), "/structure-orders-" + structureId + "-" + page + ".json");
	}

	@SneakyThrows
	private MockResponse mockStructures() {
		var obj = objectMapper.createObjectNode();
		obj.put(
				"1000000000001",
				objectMapper
						.createObjectNode()
						.put("structure_id", 1000000000001L)
						.put("is_market_structure", true)
						.put("solar_system_id", 30000001)
						.put("constellation_id", 20000001)
						.put("region_id", 10000010));
		obj.put(
				"1000000000002",
				objectMapper
						.createObjectNode()
						.put("structure_id", 1000000000002L)
						.put("is_market_structure", false)
						.put("solar_system_id", 30000001)
						.put("constellation_id", 20000001)
						.put("region_id", 10000010));
		obj.put(
				"1000000000003",
				objectMapper
						.createObjectNode()
						.put("structure_id", 1000000000003L)
						.put("is_market_structure", true)
						.put("solar_system_id", 30000001)
						.put("constellation_id", 20000001)
						.put("region_id", 10000010));
		obj.put(
				"1000000000004",
				objectMapper
						.createObjectNode()
						.put("structure_id", 1000000000004L)
						.put("is_market_structure", true));
		return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(obj));
	}

	@SneakyThrows
	private List<Map<String, String>> loadRegionOrderMaps(int regionId, int page) {
		return testDataUtil.readMapsFromJson(loadRegionOrders(regionId, page)).stream()
				.map(m -> {
					m.put("region_id", String.valueOf(regionId));
					m.put("constellation_id", "20000001");
					m.put("station_id", m.get("location_id"));
					return m;
				})
				.toList();
	}

	@SneakyThrows
	private List<Map<String, String>> loadStructureOrderMaps(long structureId, int page) {
		return testDataUtil.readMapsFromJson(loadStructureOrders(structureId, page)).stream()
				.map(m -> {
					if (m.get("location_id").equals("1000000000004")) {
						m.put("region_id", "");
						m.put("constellation_id", "");
						m.put("system_id", "");
						return m;
					}
					m.put("region_id", "10000010");
					m.put("constellation_id", "20000001");
					m.put("system_id", "30000001");
					return m;
				})
				.toList();
	}
}
