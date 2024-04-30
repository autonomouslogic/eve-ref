package com.autonomouslogic.everef.cli.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.autonomouslogic.everef.cli.publiccontracts.ContractsFileBuilder;
import com.autonomouslogic.everef.cli.publiccontracts.ContractsScrapeMeta;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.EveConstants;
import com.autonomouslogic.everef.util.JsonNodeCsvWriter;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.io.FileInputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@SetEnvironmentVariable(key = "SCRAPE_CHARACTER_OWNER_HASH", value = "scrape-owner-hash")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "EVE_REF_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeStructuresTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "REF_DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@Log4j2
@ExtendWith(MockitoExtension.class)
public class ScrapeStructuresTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	ScrapeStructures scrapeStructures;

	@Mock
	LocationPopulator locationPopulator;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	Provider<JsonNodeCsvWriter> jsonNodeCsvWriterProvider;

	@Inject
	TempFiles tempFiles;

	@Inject
	DataIndexHelper dataIndexHelper;

	@Inject
	Provider<ContractsFileBuilder> contractsFileBuilderProvider;

	MockWebServer server;

	JsonNode previousScrape;
	Map<Long, Map<String, Object>> publicStructures;
	Map<Long, Map<String, Object>> nonPublicStructures;
	Map<Long, Map<String, Object>> sovereigntyStructures;
	Set<Long> marketStructures;
	ZonedDateTime time;
	List<JsonNode> marketOrders;
	List<JsonNode> publicContracts;

	@Inject
	ScrapeStructuresTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);
		lenient().when(locationPopulator.populate(any())).thenReturn(Completable.complete()); // @todo

		time = ZonedDateTime.parse("2021-01-01T00:00:00.123Z");
		scrapeStructures.setScrapeTime(time);
		previousScrape = null;
		publicStructures = new HashMap<>();
		nonPublicStructures = new HashMap<>();
		sovereigntyStructures = new HashMap<>();
		marketStructures = new HashSet<>();
		marketOrders = new ArrayList<>();
		publicContracts = new ArrayList<>();

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
	void shouldScrapePublicStructures() {
		publicStructures.put(1000000000001L, Map.of("name", "Test Structure 1"));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(publicStructure()));
	}

	@Test
	void shouldUpdatePublicStructures() {
		loadPreviousScrape(container(publicStructure()));
		publicStructures.put(1000000000001L, Map.of("name", "Test Structure 1 Updated"));
		time = time.plusDays(1);
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(publicStructure()
				.put("name", "Test Structure 1 Updated")
				.put("last_seen_public_structure", "2021-01-02T00:00:00Z")
				.put("last_structure_get", "2021-01-02T00:00:00Z")));
	}

	@Test
	void shouldNotPreserveExtraDataFromPreviousScrapes() {
		loadPreviousScrape(container(publicStructure()));
		((ObjectNode) previousScrape.get("1000000000001")).put("some_key", "some_value");
		publicStructures.put(1000000000001L, Map.of("name", "Test Structure 1"));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(publicStructure()));
	}

	@Test
	void shouldRescrapePreviousStructures() {
		loadPreviousScrape(container(publicStructure()));
		nonPublicStructures.put(1000000000001L, Map.of("name", "Test Structure 1 Updated"));
		time = time.plusDays(1);
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(publicStructure()
				.put("name", "Test Structure 1 Updated")
				.put("is_public_structure", false)
				.put("last_structure_get", "2021-01-02T00:00:00Z")));
	}

	@Test
	void shouldPreserveStructures() {
		loadPreviousScrape(container(publicStructure()));
		time = time.plusDays(1);
		scrapeStructures.run().blockingAwait();
		verifyScrape(
				container(publicStructure().put("is_public_structure", false).put("is_gettable_structure", false)));
	}

	@Test
	void shouldCheckMarkets() {
		publicStructures.put(
				1000000000001L, Map.of("name", "Test Structure 1", "type_id", EveConstants.KEEPSTAR_TYPE_ID));
		marketStructures.add(1000000000001L);
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(publicStructure()
				.put("type_id", EveConstants.KEEPSTAR_TYPE_ID)
				.put("is_market_structure", true)
				.put("last_seen_market_structure", "2021-01-01T00:00:00Z")));
	}

	@Test
	void shouldOnlyTryMarketsForStructureTypesWhereMarketModulesCanBeApplied() {
		publicStructures.put(
				1000000000001L, Map.of("name", "Test Structure 1", "type_id", EveConstants.ASTRAHUS_HUB_TYPE_ID));
		marketStructures.add(1000000000001L); // Will never be called.
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(publicStructure().put("type_id", EveConstants.ASTRAHUS_HUB_TYPE_ID)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"location_id", "station_id"})
	void shouldScrapeStructuresFromMarketOrders(String prop) {
		marketOrders.add(objectMapper.createObjectNode().put(prop, 1000000000001L));
		marketOrders.add(objectMapper.createObjectNode().put(prop, 60000001L));
		nonPublicStructures.put(1000000000001L, Map.of("name", "Test Structure 1"));
		nonPublicStructures.put(60000001L, Map.of("name", "Should not be scraped"));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(nonPublicStructure()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"location_id", "station_id"})
	void shouldPreserveLocationOnHiddenStructuresFromMarketOrders(String prop) {
		marketOrders.add(objectMapper
				.createObjectNode()
				.put(prop, 1000000000001L)
				.put("region_id", 10000001)
				.put("constellation_id", 20000001)
				.put("system_id", 30000001));
		marketOrders.add(objectMapper
				.createObjectNode()
				.put(prop, 60000001L)
				.put("region_id", 10000002)
				.put("constellation_id", 20000002)
				.put("system_id", 30000002));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(hiddenStructureWithLocation()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"start_location_id", "end_location_id"})
	void shouldScrapeStructuresFromPublicContracts(String prop) {
		publicContracts.add(
				objectMapper.createObjectNode().put(prop, 1000000000001L).put("contract_id", 1));
		publicContracts.add(objectMapper.createObjectNode().put(prop, 60000001L).put("contract_id", 2));
		nonPublicStructures.put(1000000000001L, Map.of("name", "Test Structure 1"));
		nonPublicStructures.put(60000001L, Map.of("name", "Should not be scraped"));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(nonPublicStructure()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"start_location_id", "station_id"})
	void shouldPreserveLocationOnHiddenStructuresFromPublicContracts(String prop) {
		publicContracts.add(objectMapper
				.createObjectNode()
				.put(prop, 1000000000001L)
				.put("contract_id", 1)
				.put("region_id", 10000001)
				.put("constellation_id", 20000001)
				.put("system_id", 30000001));
		publicContracts.add(objectMapper
				.createObjectNode()
				.put(prop, 60000001L)
				.put("contract_id", 2)
				.put("region_id", 10000002)
				.put("constellation_id", 20000002)
				.put("system_id", 30000002));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(hiddenStructureWithLocation()));
	}

	@Test
	void shouldNotPreserveEndLocationOnHiddenStructuresFromPublicContracts() {
		publicContracts.add(objectMapper
				.createObjectNode()
				.put("end_location", 1000000000001L)
				.put("contract_id", 1)
				.put("region_id", 10000001)
				.put("constellation_id", 20000001)
				.put("system_id", 30000001));
		publicContracts.add(
				objectMapper.createObjectNode().put("end_location", 60000001L).put("contract_id", 2));
		scrapeStructures.run().blockingAwait();
		verifyScrape(container(nonPublicStructure())); // @todo
	}

	@Test
	void shouldNotScrapeSovereigntyStructures() {
		sovereigntyStructures.put(
				1000000000001L,
				Map.of(
						"structure_id", 1000000000001L,
						"structure_type_id", 32226,
						"alliance_id", 1300000001,
						"solar_system_id", 300000001));
		nonPublicStructures.put(1000000000001L, Map.of("name", "Should not scrape"));
		scrapeStructures.run().blockingAwait();
		verifyScrape(noStructures());
	}

	@Test
	void shouldRemoveOldStructures() {
		loadPreviousScrape(container(oldStructure()));
		nonPublicStructures.put(1000000000001L, Map.of("name", "Should not scrape"));
		scrapeStructures.run().blockingAwait();
		verifyScrape(noStructures());
	}

	@Test
	void shouldExecuteDataIndex() {
		publicStructures.put(1000000000001L, Map.of("name", "Test Structure 1"));
		scrapeStructures
				.setScrapeTime(ZonedDateTime.parse("2020-01-02T03:04:05Z"))
				.run()
				.blockingAwait();
		Mockito.verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket("data-bucket")
								.path("base/structures/structures-latest.v2.json")
								.build(),
						S3Url.builder()
								.bucket("data-bucket")
								.path(
										"base/structures/history/2020/2020-01-02/structures-2020-01-02_03-04-05.v2.json.bz2")
								.build());
	}

	@SneakyThrows
	private void loadPreviousScrape(@NonNull ObjectNode node) {
		previousScrape = node;
	}

	@SneakyThrows
	private void verifyScrape(@NonNull JsonNode container) {
		var json = objectMapper.writeValueAsString(container);
		var expected = objectMapper.readTree(json);
		var archiveFile = "base/structures/structures-latest.v2.json";
		var content = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();
		var supplied = objectMapper.readTree(content);
		assertEquals(expected, supplied);
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();
				var lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(time);

				if (path.equals("/structures/structures-latest.v2.json")) {
					if (previousScrape == null) {
						return new MockResponse().setResponseCode(404);
					} else {
						return new MockResponse().setBody(objectMapper.writeValueAsString(previousScrape));
					}
				}

				if (path.equals("/universe/structures/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(publicStructures.keySet()))
							.addHeader("Last-Modified", lastModified);
				}

				if (path.startsWith("/universe/structures/")) {
					var id = Long.parseLong(segments.get(2));
					var structure = publicStructures.get(id);
					if (structure == null) {
						structure = nonPublicStructures.get(id);
					}
					if (structure == null) {
						return new MockResponse().setResponseCode(401);
					} else {
						return new MockResponse()
								.setBody(objectMapper.writeValueAsString(structure))
								.addHeader("Last-Modified", lastModified);
					}
				}

				if (path.startsWith("/markets/structures/")) {
					var id = Long.parseLong(segments.get(2));
					if (marketStructures.contains(id)) {
						return new MockResponse().setBody("[]").addHeader("Last-Modified", lastModified);
					} else {
						return new MockResponse().setResponseCode(403);
					}
				}

				if (path.equals("/sovereignty/structures/")) {
					return new MockResponse()
							.setBody(objectMapper.writeValueAsString(sovereigntyStructures.values()))
							.addHeader("Last-Modified", lastModified);
				}

				if (path.equals("/market-orders/market-orders-latest.v3.csv.bz2")) {
					if (marketOrders.isEmpty()) {
						return new MockResponse().setResponseCode(404);
					}
					var file = tempFiles.tempFile("market-orders", ".csv").toFile();
					jsonNodeCsvWriterProvider.get().setOut(file).writeAll(marketOrders);
					var archive = CompressUtil.compressBzip2(file);
					archive.deleteOnExit();
					return new MockResponse()
							.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(archive))));
				}

				if (path.equals("/public-contracts/public-contracts-latest.v2.tar.bz2")) {
					if (publicContracts.isEmpty()) {
						return new MockResponse().setResponseCode(404);
					}
					var archive = contractsFileBuilderProvider
							.get()
							.setContractsScrapeMeta(new ContractsScrapeMeta())
							.setContractsStore(publicContracts.stream()
									.collect(Collectors.toMap(
											node -> node.get("contract_id").asLong(), node -> node)))
							.setItemsStore(Map.of())
							.setBidsStore(Map.of())
							.setDynamicItemsStore(Map.of())
							.setNonDynamicItemsStore(Map.of())
							.setDogmaAttributesStore(Map.of())
							.setDogmaEffectsStore(Map.of())
							.buildFile()
							.blockingGet();
					return new MockResponse()
							.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(archive))));
				}

				if (path.equals("/types/35892")) {
					var obj = objectMapper.createObjectNode();
					obj.withArray("can_fit_types").add(EveConstants.KEEPSTAR_TYPE_ID);
					return new MockResponse().setBody(objectMapper.writeValueAsString(obj));
				}

				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	private ObjectNode container(ObjectNode... structures) {
		var container = objectMapper.createObjectNode();
		for (ObjectNode structure : structures) {
			var id = Objects.requireNonNull(structure.get("structure_id")).asText();
			container.set(id, structure);
		}
		return container;
	}

	private ObjectNode noStructures() {
		return container();
	}

	private ObjectNode publicStructure() {
		return objectMapper
				.createObjectNode()
				.put("name", "Test Structure 1")
				.put("structure_id", 1000000000001L)
				.put("is_public_structure", true)
				.put("is_gettable_structure", true)
				.put("last_structure_get", "2021-01-01T00:00:00Z")
				.put("last_seen_public_structure", "2021-01-01T00:00:00Z")
				.put("is_market_structure", false);
	}

	private ObjectNode oldStructure() {
		return publicStructure()
				.put("last_structure_get", "2020-11-01T00:00:00Z")
				.put("last_seen_public_structure", "2020-11-01T00:00:00Z");
	}

	private ObjectNode nonPublicStructure() {
		return objectMapper
				.createObjectNode()
				.put("name", "Test Structure 1")
				.put("structure_id", 1000000000001L)
				.put("is_public_structure", false)
				.put("is_gettable_structure", true)
				.put("last_structure_get", "2021-01-01T00:00:00Z")
				.put("is_market_structure", false);
	}

	private ObjectNode hiddenStructure() {
		return objectMapper
				.createObjectNode()
				.put("structure_id", 1000000000001L)
				.put("is_public_structure", false)
				.put("is_gettable_structure", false)
				.put("is_market_structure", false);
	}

	private ObjectNode hiddenStructureWithLocation() {
		return hiddenStructure()
				.put("region_id", 10000001)
				.put("constellation_id", 20000001)
				.put("solar_system_id", 30000001);
	}
}
