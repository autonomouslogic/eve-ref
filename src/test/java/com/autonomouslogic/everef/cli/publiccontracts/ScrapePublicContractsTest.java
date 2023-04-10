package com.autonomouslogic.everef.cli.publiccontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autonomouslogic.commons.ListUtil;
import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MetaGroupScraperTest;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseTypesTypeIdOk;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
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

/**
 * Special cases for testing:
 * <ul>
 *     <li>Contract ID 1000 exists in the latest file with corresponding records in all the other files, but not in the
 *         returned contracts. None of them should be included in the final output.</li>
 *     <li>Item ID 2000 is returned as a valid Abyssal item, but the ESI returns a 520. Should be stored in the
 *         non-dynamic items list.</li>
 *     <li>Non-dynamic item 3000 is in the latest file, and should not be fetched from the ESI again.</li>
 *     <li>Contract ID 4000 (item exchange) is in the latest file with items, items should not be fetched again.</li>
 *     <li>Contract ID 5000 (auction) is in the latest file with bids and items, items should not be fetched again, but
 *         bids should. This contract also contains one bid in the latest file, but two bids in the ESI.</li>
 *     <li>Item ID 6000 has dogma in the latest file, so should not be fetched again. Item 6000 is deliberately
 *         missing from the latest items in order to check if existing dynamic items are skipped properly.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
// @Timeout(5)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapePublicContractsTest.BUCKET_NAME + "/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_PATH", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "EVE_REF_BASE_PATH", value = "http://localhost:" + TestDataUtil.TEST_PORT)
public class ScrapePublicContractsTest {
	static final String BUCKET_NAME = "data-bucket";

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
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		when(locationPopulator.populate(any(), any())).thenAnswer(MockLocationPopulatorModule.mockPopulate());

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
		assertMeta(content);
		assertContracts(records.get("contracts.csv"));
		assertBids(records.get("contract_bids.csv"));
		assertItems(records.get("contract_items.csv"));
		assertDynamicItems(records.get("contract_dynamic_items.csv"));
		assertNonDynamicItems(records.get("contract_non_dynamic_items.csv"));
		assertDogmaAttributes(records.get("contract_dynamic_items_dogma_attributes.csv"));
		assertDogmaEffects(records.get("contract_dynamic_items_dogma_effects.csv"));

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

		// Verify requests.
		var requests = new ArrayList<RecordedRequest>();
		RecordedRequest request;
		while ((request = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) {
			requests.add(request);
		}
		var requestPaths = requests.stream()
				.map(RecordedRequest::getPath)
				.sorted()
				.distinct()
				.toList(); // @todo remove distinct()
		assertEquals(
				List.of(
						"/contracts/public/10000001?datasource=tranquility&language=en",
						"/contracts/public/10000001?datasource=tranquility&language=en&page=2",
						"/contracts/public/10000002?datasource=tranquility&language=en",
						"/contracts/public/10000002?datasource=tranquility&language=en&page=2",
						"/contracts/public/bids/190319637?datasource=tranquility&language=en",
						"/contracts/public/bids/190442405?datasource=tranquility&language=en",
						"/contracts/public/bids/5000?datasource=tranquility&language=en",
						"/contracts/public/items/189863474?datasource=tranquility&language=en",
						"/contracts/public/items/190123693?datasource=tranquility&language=en",
						"/contracts/public/items/190123973?datasource=tranquility&language=en",
						"/contracts/public/items/190124106?datasource=tranquility&language=en",
						"/contracts/public/items/190160355?datasource=tranquility&language=en",
						"/contracts/public/items/190319637?datasource=tranquility&language=en",
						"/contracts/public/items/190442405?datasource=tranquility&language=en",
						"/contracts/public/items/190753200?datasource=tranquility&language=en",
						"/contracts/public/items/3000?datasource=tranquility&language=en",
						"/contracts/public/items/6000?datasource=tranquility&language=en",
						"/dogma/dynamic/items/47804/1027826381003/?datasource=tranquility&language=en",
						"/dogma/dynamic/items/47804/2000/?datasource=tranquility&language=en",
						"/dogma/dynamic/items/49734/1040731418725/?datasource=tranquility&language=en",
						"/meta-groups/15",
						"/public-contracts/public-contracts-latest.v2.tar.bz2",
						"/universe/regions/10000001/?datasource=tranquility",
						"/universe/regions/10000002/?datasource=tranquility",
						"/universe/regions/?datasource=tranquility",
						"/universe/types/47804/?datasource=tranquility",
						"/universe/types/49734/?datasource=tranquility"),
				requestPaths);
	}

	@SneakyThrows
	private void assertMeta(byte[] content) {
		ObjectNode meta = null;
		try (var tar = new TarArchiveInputStream(new BZip2CompressorInputStream(new ByteArrayInputStream(content)))) {
			ArchiveEntry entry;
			while ((entry = tar.getNextEntry()) != null) {
				if (!entry.getName().equals("meta.json")) {
					continue;
				}
				meta = (ObjectNode) objectMapper.readTree(tar);
				break;
			}
		}
		assertEquals("tranquility", meta.get("datasource").asText());
		assertEquals("2020-02-03T04:05:06Z", meta.get("scrape_start").asText());
		assertNotNull(Instant.parse(meta.get("scrape_end").asText()));
	}

	private void assertContracts(List<Map<String, String>> records) {
		var contracts = concat(ListUtil.concat(
						loadRegionContractMaps(10000001, 1),
						loadRegionContractMaps(10000001, 2),
						loadRegionContractMaps(10000002, 1),
						loadRegionContractMaps(10000002, 2))
				.stream()
				.sorted(Ordering.natural().onResultOf(m -> Long.parseLong(m.get("contract_id"))))
				.toList());
		assertEquals(contracts, concat(records));
	}

	private void assertBids(List<Map<String, String>> records) {
		var bids =
				concat(ListUtil.concat(
								loadContractBidsMap(190319637),
								loadContractBidsMap(190442405),
								loadContractBidsMap(5000))
						.stream()
						.sorted(Ordering.natural().onResultOf(m -> Long.parseLong(m.get("bid_id"))))
						.toList());
		assertEquals(bids, concat(records));
	}

	private void assertItems(List<Map<String, String>> records) {
		var items = ListUtil.concat(
						loadContractItemsMap(189863474),
						loadContractItemsMap(190123693),
						loadContractItemsMap(190123973),
						loadContractItemsMap(190124106),
						loadContractItemsMap(190160355),
						loadContractItemsMap(190319637),
						loadContractItemsMap(190442405),
						loadContractItemsMap(190753200),
						loadContractItemsMap(3000),
						loadContractItemsMap(4000),
						loadContractItemsMap(5000),
						loadContractItemsMap(6000))
				.stream()
				.sorted(Ordering.natural().onResultOf(m -> Long.parseLong(m.get("record_id"))))
				.toList();
		assertEquals(concat(items), concat(records));
	}

	private void assertDynamicItems(List<Map<String, String>> records) {
		var dynamicItems = ListUtil.concat(
						loadDynamicItemsMap(47804, 1027826381003L, 190124106),
						loadDynamicItemsMap(49734, 1040731418725L, 190160355),
						loadDynamicItemsMap(47801, 6000L, 6000))
				.stream()
				.sorted(Ordering.natural().onResultOf(m -> Long.parseLong(m.get("item_id"))))
				.toList();
		assertEquals(concat(dynamicItems), concat(records));
	}

	private void assertNonDynamicItems(List<Map<String, String>> records) {
		assertEquals(
				concat(List.of(
						Map.ofEntries(
								Map.entry("contract_id", "190124106"),
								Map.entry("item_id", "2000"),
								Map.entry("type_id", "47804")),
						Map.ofEntries(
								Map.entry("contract_id", "3000"),
								Map.entry("item_id", "3000"),
								Map.entry("type_id", "47804")))),
				concat(records));
	}

	private void assertDogmaAttributes(List<Map<String, String>> records) {
		var dogmaAttributes = ListUtil.concat(
						loadDogmaAttributesMap(47804, 1027826381003L, 190124106),
						loadDogmaAttributesMap(49734, 1040731418725L, 190160355),
						loadDogmaAttributesMap(47801, 6000L, 6000))
				.stream()
				.sorted(Ordering.natural()
						.onResultOf(m -> Long.toHexString(Long.parseLong(m.get("item_id"))) + "-"
								+ Long.toHexString(Long.parseLong(m.get("attribute_id")))))
				.toList();
		assertEquals(concat(dogmaAttributes), concat(records));
	}

	private void assertDogmaEffects(List<Map<String, String>> records) {
		var dogmaEffects = ListUtil.concat(
						loadDogmaEffectsMap(47804, 1027826381003L, 190124106),
						loadDogmaEffectsMap(49734, 1040731418725L, 190160355),
						loadDogmaEffectsMap(47801, 6000L, 6000))
				.stream()
				.sorted(Ordering.natural()
						.onResultOf(m -> Long.toHexString(Long.parseLong(m.get("item_id"))) + "-"
								+ Long.toHexString(Long.parseLong(m.get("effect_id")))))
				.toList();
		assertEquals(concat(dogmaEffects), concat(records));
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
					case "/public-contracts/public-contracts-latest.v2.tar.bz2":
						return mockLatestFile();
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
					if (itemId == 2000L) {
						return new MockResponse().setResponseCode(520);
					}
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

	private List<Map<String, String>> loadContractBidsMap(int contractId) {
		return testDataUtil.readMapsFromJson(loadContractBids(contractId)).stream()
				.map(m -> {
					m.put("contract_id", String.valueOf(contractId));
					m.put("http_last_modified", lastModifiedInstant.toString());
					return m;
				})
				.toList();
	}

	private List<Map<String, String>> loadContractItemsMap(int contractId) {
		return testDataUtil.readMapsFromJson(loadContractItems(contractId)).stream()
				.map(m -> {
					m.put("contract_id", String.valueOf(contractId));
					m.put("http_last_modified", lastModifiedInstant.toString());
					m.computeIfAbsent("is_blueprint_copy", k -> "");
					m.computeIfAbsent("material_efficiency", k -> "");
					m.computeIfAbsent("time_efficiency", k -> "");
					m.computeIfAbsent("runs", k -> "");
					m.computeIfAbsent("item_id", k -> "");
					return m;
				})
				.toList();
	}

	@SneakyThrows
	private List<Map<String, String>> loadDynamicItemsMap(int typeId, long itemId, int contractId) {
		var json = objectMapper.readTree(loadDynamicItems(typeId, itemId));
		((ObjectNode) json).remove("dogma_attributes");
		((ObjectNode) json).remove("dogma_effects");
		var bytes =
				objectMapper.writeValueAsBytes(objectMapper.createArrayNode().add(json));
		return testDataUtil.readMapsFromJson(new ByteArrayInputStream(bytes)).stream()
				.map(m -> {
					m.put("item_id", String.valueOf(itemId));
					m.put("contract_id", String.valueOf(contractId));
					m.put("http_last_modified", lastModifiedInstant.toString());
					return m;
				})
				.toList();
	}

	@SneakyThrows
	private List<Map<String, String>> loadDogmaAttributesMap(int typeId, long itemId, int contractId) {
		var json = objectMapper.readTree(loadDynamicItems(typeId, itemId));
		var attrs = json.get("dogma_attributes");
		var bytes = objectMapper.writeValueAsBytes(attrs);
		return testDataUtil.readMapsFromJson(new ByteArrayInputStream(bytes)).stream()
				.map(m -> {
					m.put("item_id", String.valueOf(itemId));
					m.put("contract_id", String.valueOf(contractId));
					m.put("http_last_modified", lastModifiedInstant.toString());
					return m;
				})
				.toList();
	}

	@SneakyThrows
	private List<Map<String, String>> loadDogmaEffectsMap(int typeId, long itemId, int contractId) {
		var json = objectMapper.readTree(loadDynamicItems(typeId, itemId));
		var effects = json.get("dogma_effects");
		var bytes = objectMapper.writeValueAsBytes(effects);
		return testDataUtil.readMapsFromJson(new ByteArrayInputStream(bytes)).stream()
				.map(m -> {
					m.put("item_id", String.valueOf(itemId));
					m.put("contract_id", String.valueOf(contractId));
					m.put("http_last_modified", lastModifiedInstant.toString());
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

	@SneakyThrows
	private MockResponse mockLatestFile() {
		var bytes = new ByteArrayOutputStream();
		var tar = new TarArchiveOutputStream(new BZip2CompressorOutputStream(bytes));
		tar.putArchiveEntry(new TarArchiveEntry(""));
		writeLatestEntry(tar, "contract_bids.csv");
		writeLatestEntry(tar, "contract_dynamic_items.csv");
		writeLatestEntry(tar, "contract_dynamic_items_dogma_attributes.csv");
		writeLatestEntry(tar, "contract_dynamic_items_dogma_effects.csv");
		writeLatestEntry(tar, "contract_items.csv");
		writeLatestEntry(tar, "contract_non_dynamic_items.csv");
		writeLatestEntry(tar, "contracts.csv");
		writeLatestEntry(tar, "meta.json");
		tar.close();
		return new MockResponse().setResponseCode(200).setBody(new Buffer().write(bytes.toByteArray()));
	}

	@SneakyThrows
	private void writeLatestEntry(TarArchiveOutputStream tar, String file) {
		var entry = new TarArchiveEntry(file);
		byte[] bytes;
		try (var in = ResourceUtil.loadContextual(ScrapePublicContractsTest.class, "/latest/" + file)) {
			bytes = IOUtils.toByteArray(in);
		}
		entry.setSize(bytes.length);
		tar.putArchiveEntry(entry);
		tar.write(bytes);
		tar.closeArchiveEntry();
	}
}
