package com.autonomouslogic.everef.cli.publiccontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import java.nio.charset.StandardCharsets;
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
import org.apache.commons.io.IOUtils;
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
        server.setDispatcher(dispatcher()
                .withRegion(10000001)
                .withContracts(10000001, "/contracts-10000001-1.json"));
        run();
        assertEquals(expectedContracts("/contracts-10000001-1.json", 10000001), records.get("contracts.csv"));
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
        server.setDispatcher(dispatcher()
                .withRegion(10000001)
                .withContracts(10000001, "/contracts-two-couriers-10000001-1.json"));
        run();
        assertEquals(
                expectedContracts("/contracts-two-couriers-10000001-1.json", 10000001),
                records.get("contracts.csv"));
        assertNoSubData();
        assertLatestFileMatches();
        assertRequestPaths(
                "/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
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

    // --- Expected data builders ---

    @SneakyThrows
    private List<Map<String, String>> expectedContracts(String fixturePath, long regionId) {
        var contracts = testDataUtil.readMapsFromJson(ResourceUtil.loadContextual(getClass(), fixturePath));
        contracts.forEach(m -> {
            m.put("region_id", String.valueOf(regionId));
            m.put("constellation_id", "999");
            m.put("system_id", "999");
            m.put("station_id", "999");
            m.put("http_last_modified", lastModifiedInstant.toString());
        });
        contracts.sort(Comparator.comparingLong(m -> Long.parseLong(m.get("contract_id"))));
        return contracts;
    }

    // --- Dispatcher builder ---

    private TestDispatcher dispatcher() {
        return new TestDispatcher();
    }

    class TestDispatcher extends Dispatcher {
        private static final Map<Long, String> REGION_NAMES =
                Map.of(10000001L, "Derelik", 10000002L, "The Forge");

        private final List<Long> regionIds = new ArrayList<>();
        // key: regionId, value: map of page -> fixturePath (-1 = wildcard for all pages)
        private final Map<Long, Map<Integer, String>> contractsByRegionPage = new HashMap<>();
        private final Map<Long, String> itemsByContract = new HashMap<>();
        private final Map<Long, String> bidsByContract = new HashMap<>();
        // key: "typeId-itemId"
        private final Map<String, String> dynamicItemsByKey = new HashMap<>();
        private String metaGroupsFixture;
        private Supplier<MockResponse> latestArchiveSupplier = () -> new MockResponse().setResponseCode(404);

        TestDispatcher withRegion(long id) {
            regionIds.add(id);
            return this;
        }

        TestDispatcher withContracts(long regionId, String fixturePath) {
            contractsByRegionPage.computeIfAbsent(regionId, k -> new HashMap<>()).put(-1, fixturePath);
            return this;
        }

        TestDispatcher withContracts(long regionId, int page, String fixturePath) {
            contractsByRegionPage.computeIfAbsent(regionId, k -> new HashMap<>()).put(page, fixturePath);
            return this;
        }

        TestDispatcher withItems(long contractId, String fixturePath) {
            itemsByContract.put(contractId, fixturePath);
            return this;
        }

        TestDispatcher withBids(long contractId, String fixturePath) {
            bidsByContract.put(contractId, fixturePath);
            return this;
        }

        TestDispatcher withDynamicItems(long typeId, long itemId, String fixturePath) {
            dynamicItemsByKey.put(typeId + "-" + itemId, fixturePath);
            return this;
        }

        TestDispatcher withMetaGroups(String fixturePath) {
            metaGroupsFixture = fixturePath;
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
                        return metaGroupsFixture != null
                                ? mockJson(loadResource(metaGroupsFixture))
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
                    var fixture = itemsByContract.get(contractId);
                    return fixture != null
                            ? mockJson(loadResource(fixture))
                            : new MockResponse().setResponseCode(404);
                }
                if (path.startsWith("/contracts/public/bids/") || path.startsWith("/latest/contracts/public/bids/")) {
                    var segmentIndex = segments.contains("latest") ? 4 : 3;
                    var contractId = Long.parseLong(segments.get(segmentIndex));
                    var fixture = bidsByContract.get(contractId);
                    return fixture != null
                            ? mockJson(loadResource(fixture))
                            : new MockResponse().setResponseCode(404);
                }
                if (path.startsWith("/contracts/public/") || path.startsWith("/latest/contracts/public/")) {
                    var segmentIndex = segments.contains("latest") ? 3 : 2;
                    var regionId = Long.parseLong(segments.get(segmentIndex));
                    var pages = contractsByRegionPage.get(regionId);
                    if (pages == null) return new MockResponse().setResponseCode(404);
                    var fixture = pages.containsKey(pageNum) ? pages.get(pageNum) : pages.get(-1);
                    if (fixture == null) return new MockResponse().setResponseCode(404);
                    var totalPages = pages.containsKey(-1) ? 1 : pages.size();
                    return mockJson(loadResource(fixture)).addHeader("x-pages", String.valueOf(totalPages));
                }
                if (path.startsWith("/dogma/dynamic/items/") || path.startsWith("/latest/dogma/dynamic/items/")) {
                    var segmentIndex = segments.contains("latest") ? 4 : 3;
                    var typeId = Long.parseLong(segments.get(segmentIndex));
                    var itemId = Long.parseLong(segments.get(segmentIndex + 1));
                    var fixture = dynamicItemsByKey.get(typeId + "-" + itemId);
                    return fixture != null
                            ? mockJson(loadResource(fixture))
                            : new MockResponse().setResponseCode(404);
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

    @SneakyThrows
    private String loadResource(String path) {
        try (var in = ResourceUtil.loadContextual(getClass(), path)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
