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
import java.util.List;
import java.util.concurrent.TimeUnit;
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
        server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                try {
                    var path = request.getRequestUrl().encodedPath();
                    var segments = request.getRequestUrl().pathSegments();
                    switch (path) {
                        case "/universe/regions/", "/latest/universe/regions/":
                            return mockJson("[10000001]");
                        case "/universe/regions/10000001/", "/latest/universe/regions/10000001/":
                            return mockJson(
                                    "{\"region_id\":10000001,\"name\":\"Derelik\",\"constellations\":[]}");
                        case "/public-contracts/public-contracts-latest.v2.tar.bz2":
                            return new MockResponse().setResponseCode(404);
                    }
                    if (path.startsWith("/contracts/public/") || path.startsWith("/latest/contracts/public/")) {
                        var segmentIndex = segments.contains("latest") ? 3 : 2;
                        var regionId = segments.get(segmentIndex);
                        return mockJson(loadResource("/contracts-" + regionId + "-1.json"))
                                .addHeader("x-pages", "1");
                    }
                    log.error("Unaccounted URL: {}", path);
                    return new MockResponse().setResponseCode(404);
                } catch (Exception e) {
                    log.error("Error in dispatcher", e);
                    return new MockResponse().setResponseCode(500);
                }
            }
        });

        scrapePublicContracts
                .setScrapeTime(ZonedDateTime.parse("2020-02-03T04:05:06.89Z"))
                .run();

        var archiveFile =
                "base/public-contracts/history/2020/2020-02-03/public-contracts-2020-02-03_04-05-06.v2.tar.bz2";
        var latestFile = "base/public-contracts/public-contracts-latest.v2.tar.bz2";
        var content = mockS3Adapter.getTestObject(BUCKET_NAME, archiveFile, dataClient).orElseThrow();
        var records = testDataUtil.readFileMapsFromBz2TarCsv(content);

        // Contracts.
        var expectedContracts = testDataUtil.readMapsFromJson(
                ResourceUtil.loadContextual(getClass(), "/contracts-10000001-1.json"));
        expectedContracts.forEach(m -> {
            m.put("region_id", "10000001");
            m.put("constellation_id", "999");
            m.put("system_id", "999");
            m.put("station_id", "999");
            m.put("http_last_modified", lastModifiedInstant.toString());
        });
        assertEquals(expectedContracts, records.get("contracts.csv"));

        // No bids, items, or dynamic data.
        assertEquals(List.of(), records.get("contract_bids.csv"));
        assertEquals(List.of(), records.get("contract_items.csv"));
        assertEquals(List.of(), records.get("contract_dynamic_items.csv"));
        assertEquals(List.of(), records.get("contract_non_dynamic_items.csv"));
        assertEquals(List.of(), records.get("contract_dynamic_items_dogma_attributes.csv"));
        assertEquals(List.of(), records.get("contract_dynamic_items_dogma_effects.csv"));

        // Archive and latest files are identical.
        mockS3Adapter.assertSameContent(BUCKET_NAME, archiveFile, latestFile, dataClient);

        // ESI requests made.
        var requests = new ArrayList<RecordedRequest>();
        RecordedRequest request;
        while ((request = server.takeRequest(1, TimeUnit.MILLISECONDS)) != null) {
            requests.add(request);
        }
        var requestPaths = requests.stream()
                .map(RecordedRequest::getPath)
                .sorted()
                .distinct()
                .toList();
        assertEquals(
                List.of(
                        "/latest/contracts/public/10000001?datasource=tranquility&language=en&page=1",
                        "/public-contracts/public-contracts-latest.v2.tar.bz2",
                        "/universe/regions/10000001/?datasource=tranquility",
                        "/universe/regions/?datasource=tranquility"),
                requestPaths);

        // Data index updated.
        Mockito.verify(dataIndexHelper)
                .updateIndex(
                        S3Url.builder()
                                .bucket("data-bucket")
                                .path("base/public-contracts/public-contracts-latest.v2.tar.bz2")
                                .build(),
                        S3Url.builder()
                                .bucket("data-bucket")
                                .path(
                                        "base/public-contracts/history/2020/2020-02-03/public-contracts-2020-02-03_04-05-06.v2.tar.bz2")
                                .build());
    }

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
