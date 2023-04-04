package com.autonomouslogic.everef.cli.publiccontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.commons.ListUtil;
import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.DataIndex;
import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MetaGroupScraperTest;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseTypesTypeIdOk;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
// @Timeout(5)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapePublicContractsTest.BUCKET_NAME + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_PATH", value = "http://localhost:" + ScrapePublicContractsTest.PORT)
@SetEnvironmentVariable(key = "EVE_REF_BASE_PATH", value = "http://localhost:" + ScrapePublicContractsTest.PORT)
public class ScrapePublicContractsTest {
	static final String BUCKET_NAME = "data-bucket";
	static final int PORT = 30150;

	@Inject
	ScrapePublicContracts scrapePublicContracts;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	DataIndex dataIndex;

	@Inject
	ObjectMapper objectMapper;

	@Mock
	LocationPopulator locationPopulator;

	final String lastModified = "Mon, 03 Apr 2023 03:47:30 GMT";
	final Instant lastModifiedInstant = Instant.parse("2023-04-03T03:47:30Z");
	final GetUniverseTypesTypeIdOk type = new GetUniverseTypesTypeIdOk(
			"", 0, "", true, 0, 0.0f, List.of(), List.of(), 0, 0, 0, 0.0f, 0.0f, 0, 0.0f, 0.0f);

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockDataIndexModule(new MockDataIndexModule().setDefaultMock(true))
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		when(locationPopulator.populate(any(), any())).thenAnswer(MockLocationPopulatorModule.mockPopulate());

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldScrapePublicContracts() {
		scrapePublicContracts
				.setScrapeTime(ZonedDateTime.parse("2020-02-03T04:05:06.89Z"))
				.run()
				.blockingAwait();

		// Get saved file.
		var archiveFile = "public-contracts/history/2020/2020-02-03/public-contracts-2020-02-03_04-05-06.v2.tar.bz2";
		var latestFile = "public-contracts/public-contracts-latest.v2.tar.bz2";
		var content = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();
		// Assert records.
		var records = testDataUtil.readFileMapsFromBz2TarCsv(content);

		var contracts = concat(ListUtil.concat(
						loadRegionContractMaps(10000001, 1),
						loadRegionContractMaps(10000001, 2),
						loadRegionContractMaps(10000002, 1),
						loadRegionContractMaps(10000002, 2))
				.stream()
				.sorted(Ordering.natural().onResultOf(m -> m.get("contract_id")))
				.toList());
		assertEquals(contracts, concat(records.get("contracts.csv")));

		//			.stream()
		//			.map(Map::toString)
		//			.collect(Collectors.joining("\n"));
		//		var expected = ListUtil.concat(
		//				loadRegionOrderMaps(10000001, 1),
		//				loadRegionOrderMaps(10000001, 2),
		//				loadRegionOrderMaps(10000002, 1),
		//				loadRegionOrderMaps(10000002, 2))
		//			.stream()
		//			.sorted(Ordering.compound(List.of(
		//				Ordering.natural().onResultOf(m -> m.get("region_id")),
		//				Ordering.natural().onResultOf(m -> m.get("type_id")))))
		//			.peek(record -> record.put("constellation_id", "999"))
		//			.peek(record -> record.put("station_id", "999"))
		//			.map(Map::toString)
		//			.collect(Collectors.joining("\n"));
		//		assertEquals(expected, records);
		// Assert the two files are the same.
		mockS3Adapter.assertSameContent(BUCKET_NAME, archiveFile, latestFile, dataClient);
		// Data index.
		verify(dataIndex).run();
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();

				switch (path) {
					case "/universe/regions/":
						return mockResponse("[10000001,10000002]");
					case "/universe/regions/10000001/":
						return mockResponse("{\"region_id\":10000001,\"name\":\"Derelik\",\"constellations\":[]}");
					case "/universe/regions/10000002/":
						return mockResponse("{\"region_id\":10000002,\"name\":\"The Forge\",\"constellations\":[]}");
					case "/meta-groups/15":
						return metaGroup15();
				}
				var page = Optional.ofNullable(request.getRequestUrl().queryParameter("page"))
						.map(Integer::parseInt)
						.orElse(1);
				if (path.startsWith("/contracts/public/items/")) {
					var contractId = Long.parseLong(segments.get(3));
					return mockResponse(loadContractItems(contractId));
				}
				if (path.startsWith("/contracts/public/bids/")) {
					var contractId = Long.parseLong(segments.get(3));
					return mockResponse(loadContractBids(contractId));
				}
				if (path.startsWith("/contracts/public/")) {
					var contractId = Long.parseLong(segments.get(2));
					return mockResponse(loadRegionContracts(contractId, page)).addHeader("x-pages", "2");
				}
				if (path.startsWith("/universe/types/")) {
					return mockResponse(objectMapper.writeValueAsString(type));
				}
				if (path.startsWith("/dogma/dynamic/items/")) {
					var typeId = Long.parseLong(segments.get(3));
					var itemId = Long.parseLong(segments.get(4));
					return mockResponse(loadDynamicItems(typeId, itemId));
				}
				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	@NotNull
	private MockResponse mockResponse(String body) {
		return new MockResponse().setResponseCode(200).setBody(body).addHeader("last-modified", lastModified);
	}

	@NotNull
	@SneakyThrows
	private MockResponse mockResponse(InputStream in) {
		try (in) {
			var body = IOUtils.toString(in, StandardCharsets.UTF_8);
			return mockResponse(body);
		}
	}

	@SneakyThrows
	MockResponse metaGroup15() {
		var html = IOUtils.toString(
				ResourceUtil.loadContextual(MetaGroupScraperTest.class, "/meta-groups-15.html"),
				StandardCharsets.UTF_8);
		return mockResponse(html);
	}

	@SneakyThrows
	private InputStream loadRegionContracts(long regionId, int page) {
		return ResourceUtil.loadContextual(
				ScrapePublicContractsTest.class, String.format("/contracts-%s-%s.json", regionId, page));
	}

	@SneakyThrows
	private InputStream loadContractItems(long contractId) {
		return ResourceUtil.loadContextual(
				ScrapePublicContractsTest.class, String.format("/items-%s.json", contractId));
	}

	@SneakyThrows
	private InputStream loadContractBids(long contractId) {
		return ResourceUtil.loadContextual(ScrapePublicContractsTest.class, String.format("/bids-%s.json", contractId));
	}

	@SneakyThrows
	private InputStream loadDynamicItems(long typeId, long itemId) {
		return ResourceUtil.loadContextual(
				ScrapePublicContractsTest.class, String.format("/dynamic-items-%s-%s.json", typeId, itemId));
	}

	private List<Map<String, String>> loadRegionContractMaps(int regionId, int page) {
		return testDataUtil.readMapsFromJson(loadRegionContracts(regionId, page)).stream()
				.map(m -> {
					m.put("region_id", String.valueOf(regionId));
					m.put("constellation_id", String.valueOf(999));
					m.put("system_id", String.valueOf(999));
					m.put("station_id", String.valueOf(999));
					m.put("http_last_modified", lastModifiedInstant.toString());
					m.computeIfAbsent("buyout", k -> "");
					m.computeIfAbsent("collateral", k -> "");
					return m;
				})
				.toList();
	}

	private String concat(List<Map<String, String>> maps) {
		return maps.stream()
				.map(m -> {
					var newMap = new LinkedHashMap<String, String>();
					m.keySet().stream().sorted().forEach(k -> newMap.put(k, m.get(k)));
					return newMap;
				})
				.map(Map::toString)
				.collect(Collectors.joining("\n"));
	}
}
