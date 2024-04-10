package com.autonomouslogic.everef.cli.marketorders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.autonomouslogic.commons.ListUtil;
import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
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

@ExtendWith(MockitoExtension.class)
@Timeout(30)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMarketOrdersTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/base")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "SCRAPE_CHARACTER_OWNER_HASH", value = "scrape-owner-hash")
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
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				return switch (recordedRequest.getPath()) {
					case "/universe/regions/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("[10000001,10000002]");
					case "/universe/regions/10000001/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("{\"region_id\":10000001,\"name\":\"Derelik\",\"constellations\":[]}");
					case "/universe/regions/10000002/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("{\"region_id\":10000002,\"name\":\"The Forge\",\"constellations\":[]}");
					case "/markets/10000001/orders?order_type=all&datasource=tranquility&language=en" -> mockRegionOrders(
							10000001, 1, 2);
					case "/markets/10000001/orders?order_type=all&datasource=tranquility&language=en&page=2" -> mockRegionOrders(
							10000001, 2, 2);
					case "/markets/10000002/orders?order_type=all&datasource=tranquility&language=en" -> mockRegionOrders(
							10000002, 1, 2);
					case "/markets/10000002/orders?order_type=all&datasource=tranquility&language=en&page=2" -> mockRegionOrders(
							10000002, 2, 2);
					case "/base/structures/structures-latest.v2.json" -> mockStructures();
					case "/markets/structures/1000000000001/?datasource=tranquility&language=en" -> mockStructureOrders(recordedRequest,
						1000000000001L, 1, 2);
					case "/markets/structures/1000000000001/?datasource=tranquility&language=en&page=2" -> mockStructureOrders(recordedRequest,
						1000000000001L, 2, 2);
					case "/markets/structures/1000000000002/?datasource=tranquility&language=en" -> throw new RuntimeException("Fetch non-market structure");
					default -> new MockResponse().setResponseCode(404);
				};
			}
		});
		server.start(TestDataUtil.TEST_PORT);

		// Locations.
		when(locationPopulator.populate(any(), any())).thenAnswer(MockLocationPopulatorModule.mockPopulate());
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
				.map(Map::toString)
				.collect(Collectors.joining("\n"));
		var expected = ListUtil.concat(
						loadRegionOrderMaps(10000001, 1),
						loadRegionOrderMaps(10000001, 2),
						loadRegionOrderMaps(10000002, 1),
						loadRegionOrderMaps(10000002, 2))
				.stream()
				.sorted(Ordering.compound(List.of(
						Ordering.natural().onResultOf(m -> m.get("region_id")),
						Ordering.natural().onResultOf(m -> m.get("type_id")))))
				.peek(record -> record.put("constellation_id", "999"))
				.peek(record -> record.put("station_id", "999"))
				.map(Map::toString)
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

	@SneakyThrows
	private MockResponse mockRegionOrders(int regionId, int page, int pages) {
		var bytes = IOUtils.toByteArray(loadRegionOrders(regionId, page));
		return new MockResponse()
				.setResponseCode(200)
				.setBody(new String(bytes))
				.addHeader("X-Pages", Integer.toString(pages));
	}

	@SneakyThrows
	private MockResponse mockStructureOrders(RecordedRequest recordedRequest, long structureId, int page, int pages) {
		assertNotNull(recordedRequest.getHeaders().get("Authorization"));
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
		obj
			.put("1000000000001", objectMapper.createObjectNode()
				.put("structure_id", 1000000000001L)
				.put("is_market_structure", true)
				.put("solar_system_id", 1)
				.put("constellation_id", 1)
				.put("region_id", 1)
			);
		obj
			.put("1000000000002", objectMapper.createObjectNode()
				.put("structure_id", 1000000000002L)
				.put("is_market_structure", true)
				.put("solar_system_id", 1)
				.put("constellation_id", 1)
				.put("region_id", 1)
			);
		return new MockResponse()
			.setResponseCode(200)
			.setBody(objectMapper.writeValueAsString(obj
			));
	}

	@SneakyThrows
	private List<Map<String, String>> loadRegionOrderMaps(int regionId, int page) {
		return testDataUtil.readMapsFromJson(loadRegionOrders(regionId, page)).stream()
				.map(m -> {
					m.put("region_id", String.valueOf(regionId));
					return m;
				})
				.toList();
	}
}
