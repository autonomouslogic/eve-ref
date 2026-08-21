package com.autonomouslogic.everef.cli.publiccontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * End-to-end tests for {@link ScrapePublicContracts}. Each test method is self-contained: it sets
 * up the ESI mock, runs the scrape, and asserts the resulting archive and ESI calls.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapePublicContractsTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "REF_DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
public class ScrapePublicContractsTest {
    static final String BUCKET_NAME = "data-bucket";

    private static final String ARCHIVE_FILE =
            "base/public-contracts/history/2020/2020-02-03/public-contracts-2020-02-03_04-05-06.v2.tar.bz2";
    private static final String LATEST_FILE = "base/public-contracts/public-contracts-latest.v2.tar.bz2";

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
    DataIndexHelper dataIndexHelper;

    @Inject
    ObjectMapper objectMapper;

    @Mock
    LocationPopulator locationPopulator;

    final String lastModified = "Mon, 03 Apr 2023 03:47:30 GMT";
    final Instant lastModifiedInstant = Instant.parse("2023-04-03T03:47:30Z");

    MockWebServer server;

    private Map<String, List<Map<String, String>>> records;
    private List<String> requestPaths;
    private byte[] content;

    @BeforeEach
    @SneakyThrows
    void before() {
        DaggerTestComponent.builder()
                .mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
                .build()
                .inject(this);
        when(locationPopulator.populate(any(), any())).thenAnswer(MockLocationPopulatorModule.mockPopulate());
        server = new MockWebServer();
        server.start(TestDataUtil.TEST_PORT);
    }

    @AfterEach
    @SneakyThrows
    void after() {
        server.close();
    }

    /**
     * No previous archive on S3. A single courier contract in one region. Courier contracts have no
     * items or bids, so only contracts.csv should have data.
     */
    @Test
    @SneakyThrows
    void singleCourierContractNoExistingArchive() {
        var contracts = List.of(contract(100));
        server.setDispatcher(dispatcher()
                .withRegion(10000001)
                .withContracts(10000001, contractsJson(contracts)));
        run();
        assertEquals(expectedContracts(contracts, 10000001), records.get("contracts.csv"));
        assertNoSubData();
        assertLatestFileMatches();
        assertRequestPaths(
                "/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
                "/public-contracts/public-contracts-latest.v2.tar.bz2",
                "/universe/regions/10000001/?datasource=tranquility",
                "/universe/regions/?datasource=tranquility");
        assertDataIndex();
    }

    /**
     * No previous archive on S3. Two courier contracts in one region, one page. Both should appear
     * in the archive with no items or bids.
     */
    @Test
    @SneakyThrows
    void twoCourierContractsNoExistingArchive() {
        var contracts = List.of(contract(200), contract(201));
        server.setDispatcher(dispatcher()
                .withRegion(10000001)
                .withContracts(10000001, contractsJson(contracts)));
        run();
        assertEquals(expectedContracts(contracts, 10000001), records.get("contracts.csv"));
        assertNoSubData();
        assertLatestFileMatches();
        assertRequestPaths(
                "/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
                "/public-contracts/public-contracts-latest.v2.tar.bz2",
                "/universe/regions/10000001/?datasource=tranquility",
                "/universe/regions/?datasource=tranquility");
        assertDataIndex();
    }

    /**
     * No previous archive on S3. One contract on each of two pages for the same region. Both should
     * appear in the archive, verifying that pagination fetches all pages.
     */
    @Test
    @SneakyThrows
    void paginatedCourierContractsNoExistingArchive() {
        var page1 = List.of(contract(300));
        var page2 = List.of(contract(301));
        server.setDispatcher(dispatcher()
                .withRegion(10000001)
                .withContracts(10000001, 1, contractsJson(page1))
                .withContracts(10000001, 2, contractsJson(page2)));
        run();
        var allContracts = new ArrayList<>(page1);
        allContracts.addAll(page2);
        assertEquals(expectedContracts(allContracts, 10000001), records.get("contracts.csv"));
        assertNoSubData();
        assertLatestFileMatches();
        assertRequestPaths(
                "/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
                "/latest/contracts/public/10000001?datasource=tranquility&language=en&page=2",
                "/public-contracts/public-contracts-latest.v2.tar.bz2",
                "/universe/regions/10000001/?datasource=tranquility",
                "/universe/regions/?datasource=tranquility");
        assertDataIndex();
    }

    // --- Run and capture ---

    @SneakyThrows
    private void run() {
        scrapePublicContracts
                .setScrapeTime(ZonedDateTime.parse("2020-02-03T04:05:06.89Z"))
                .run();
        content = mockS3Adapter.getTestObject(BUCKET_NAME, ARCHIVE_FILE, dataClient).orElseThrow();
        records = testDataUtil.readFileMapsFromBz2TarCsv(content);
        var raw = new ArrayList<RecordedRequest>();
        RecordedRequest req;
        while ((req = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) raw.add(req);
        requestPaths = raw.stream().map(RecordedRequest::getPath).sorted().distinct().toList();
    }

    // --- Assertion helpers ---

    private void assertNoSubData() {
        assertEquals(List.of(), records.get("contract_bids.csv"));
        assertEquals(List.of(), records.get("contract_items.csv"));
        assertEquals(List.of(), records.get("contract_dynamic_items.csv"));
        assertEquals(List.of(), records.get("contract_non_dynamic_items.csv"));
        assertEquals(List.of(), records.get("contract_dynamic_items_dogma_attributes.csv"));
        assertEquals(List.of(), records.get("contract_dynamic_items_dogma_effects.csv"));
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

    // --- Contract builders ---

    /**
     * Creates a default courier contract ObjectNode with the given contract ID.
     * Callers can override individual fields via {@link ObjectNode#put} before use.
     */
    private ObjectNode contract(long contractId) {
        return objectMapper
                .createObjectNode()
                .put("contract_id", contractId)
                .put("type", "courier")
                .put("collateral", 500000000)
                .put("price", 0)
                .put("reward", 1000000)
                .put("volume", 100.5)
                .put("date_issued", "2023-04-03T05:48:04Z")
                .put("date_expired", "2023-05-01T05:48:04Z")
                .put("days_to_complete", 3)
                .put("end_location_id", 60003760L)
                .put("issuer_corporation_id", 1000001)
                .put("issuer_id", 2000001L)
                .put("start_location_id", 60012547L)
                .put("title", "");
    }

    @SneakyThrows
    private String contractsJson(List<ObjectNode> contracts) {
        var array = objectMapper.createArrayNode();
        contracts.forEach(array::add);
        return objectMapper.writeValueAsString(array);
    }

    @SneakyThrows
    private List<Map<String, String>> expectedContracts(List<ObjectNode> contracts, long regionId) {
        var array = objectMapper.createArrayNode();
        contracts.forEach(array::add);
        var maps = testDataUtil.readMapsFromJson(
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(array)));
        maps.forEach(m -> {
            m.put("region_id", String.valueOf(regionId));
            m.put("constellation_id", "999");
            m.put("system_id", "999");
            m.put("station_id", "999");
            m.put("http_last_modified", lastModifiedInstant.toString());
        });
        maps.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("contract_id"))));
        return maps;
    }

    // --- Dispatcher builder ---

    private TestDispatcher dispatcher() {
        return new TestDispatcher();
    }

    class TestDispatcher extends Dispatcher {
        private static final Map<Long, String> REGION_NAMES =
                Map.of(10000001L, "Derelik", 10000002L, "The Forge");

        private final List<Long> regionIds = new ArrayList<>();
        // key: regionId, value: map of page -> JSON body (-1 = wildcard for all pages)
        private final Map<Long, Map<Integer, String>> contractsByRegionPage = new HashMap<>();
        private final Map<Long, String> itemsByContract = new HashMap<>();
        private final Map<Long, String> bidsByContract = new HashMap<>();
        // key: "typeId-itemId"
        private final Map<String, String> dynamicItemsByKey = new HashMap<>();
        private String metaGroupsBody;
        private Supplier<MockResponse> latestArchiveSupplier = () -> new MockResponse().setResponseCode(404);

        TestDispatcher withRegion(long id) {
            regionIds.add(id);
            return this;
        }

        TestDispatcher withContracts(long regionId, String jsonBody) {
            contractsByRegionPage.computeIfAbsent(regionId, k -> new HashMap<>()).put(-1, jsonBody);
            return this;
        }

        TestDispatcher withContracts(long regionId, int page, String jsonBody) {
            contractsByRegionPage.computeIfAbsent(regionId, k -> new HashMap<>()).put(page, jsonBody);
            return this;
        }

        TestDispatcher withItems(long contractId, String jsonBody) {
            itemsByContract.put(contractId, jsonBody);
            return this;
        }

        TestDispatcher withBids(long contractId, String jsonBody) {
            bidsByContract.put(contractId, jsonBody);
            return this;
        }

        TestDispatcher withDynamicItems(long typeId, long itemId, String jsonBody) {
            dynamicItemsByKey.put(typeId + "-" + itemId, jsonBody);
            return this;
        }

        TestDispatcher withMetaGroups(String jsonBody) {
            metaGroupsBody = jsonBody;
            return this;
        }

        TestDispatcher withLatestArchive(byte[] data) {
            latestArchiveSupplier = () -> new MockResponse()
                    .setResponseCode(200)
                    .setBody(new Buffer().write(data));
            return this;
        }

        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            try {
                var path = request.getRequestUrl().encodedPath();
                var segments = request.getRequestUrl().pathSegments();
                var page = request.getRequestUrl().queryParameter("page");
                var pageNum = page != null ? Integer.parseInt(page) : 1;

                switch (path) {
                    case "/universe/regions/", "/latest/universe/regions/":
                        return mockJson(regionIds.toString());
                    case "/public-contracts/public-contracts-latest.v2.tar.bz2":
                        return latestArchiveSupplier.get();
                    case "/meta_groups/15":
                        return metaGroupsBody != null
                                ? mockJson(metaGroupsBody)
                                : new MockResponse().setResponseCode(404);
                }
                if (path.startsWith("/universe/regions/") || path.startsWith("/latest/universe/regions/")) {
                    var segmentIndex = segments.contains("latest") ? 3 : 2;
                    var regionId = Long.parseLong(segments.get(segmentIndex));
                    var name = REGION_NAMES.getOrDefault(regionId, "Region " + regionId);
                    return mockJson(
                            "{\"region_id\":" + regionId + ",\"name\":\"" + name + "\",\"constellations\":[]}");
                }
                if (path.startsWith("/contracts/public/items/") || path.startsWith("/latest/contracts/public/items/")) {
                    var segmentIndex = segments.contains("latest") ? 4 : 3;
                    var contractId = Long.parseLong(segments.get(segmentIndex));
                    var body = itemsByContract.get(contractId);
                    return body != null ? mockJson(body) : new MockResponse().setResponseCode(404);
                }
                if (path.startsWith("/contracts/public/bids/") || path.startsWith("/latest/contracts/public/bids/")) {
                    var segmentIndex = segments.contains("latest") ? 4 : 3;
                    var contractId = Long.parseLong(segments.get(segmentIndex));
                    var body = bidsByContract.get(contractId);
                    return body != null ? mockJson(body) : new MockResponse().setResponseCode(404);
                }
                if (path.startsWith("/contracts/public/") || path.startsWith("/latest/contracts/public/")) {
                    var segmentIndex = segments.contains("latest") ? 3 : 2;
                    var regionId = Long.parseLong(segments.get(segmentIndex));
                    var pages = contractsByRegionPage.get(regionId);
                    if (pages == null) return new MockResponse().setResponseCode(404);
                    var body = pages.containsKey(pageNum) ? pages.get(pageNum) : pages.get(-1);
                    if (body == null) return new MockResponse().setResponseCode(404);
                    var totalPages = pages.containsKey(-1) ? 1 : pages.size();
                    return mockJson(body).addHeader("x-pages", String.valueOf(totalPages));
                }
                if (path.startsWith("/dogma/dynamic/items/") || path.startsWith("/latest/dogma/dynamic/items/")) {
                    var segmentIndex = segments.contains("latest") ? 4 : 3;
                    var typeId = Long.parseLong(segments.get(segmentIndex));
                    var itemId = Long.parseLong(segments.get(segmentIndex + 1));
                    var body = dynamicItemsByKey.get(typeId + "-" + itemId);
                    return body != null ? mockJson(body) : new MockResponse().setResponseCode(404);
                }

                log.error("Unaccounted URL: {}", path);
                return new MockResponse().setResponseCode(404);
            } catch (Exception e) {
                log.error("Error in dispatcher", e);
                return new MockResponse().setResponseCode(500);
            }
        }
    }

    // --- HTTP helpers ---

    @NotNull
    private MockResponse mockJson(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("last-modified", lastModified);
    }
}
