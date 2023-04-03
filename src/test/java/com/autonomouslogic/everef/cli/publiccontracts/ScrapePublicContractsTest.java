package com.autonomouslogic.everef.cli.publiccontracts;

import static org.mockito.Mockito.verify;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.DataIndex;
import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.MetaGroupScraperTest;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
@Timeout(5)
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

	@Mock
	LocationPopulator locationPopulator;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockDataIndexModule(new MockDataIndexModule().setDefaultMock(true))
				.mockLocationPopulatorModule(new MockLocationPopulatorModule().setLocationPopulator(locationPopulator))
				.build()
				.inject(this);

		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
				return switch (request.getPath()) {
					case "/universe/regions/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("[10000001,10000002]");
					case "/universe/regions/10000001/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("{\"region_id\":10000001,\"name\":\"Derelik\",\"constellations\":[]}");
					case "/universe/regions/10000002/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody("{\"region_id\":10000002,\"name\":\"The Forge\",\"constellations\":[]}");
					case "/contracts/public/10000001?datasource=tranquility&language=en" -> new MockResponse()
							.setResponseCode(200)
							.setBody("[]");
					case "/contracts/public/10000002?datasource=tranquility&language=en" -> new MockResponse()
							.setResponseCode(200)
							.setBody("[]");
					case "/meta-groups/15" -> metaGroup15();
					default -> new MockResponse().setResponseCode(404);
				};
			}
		});
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
		//		// Assert records.
		//		var records = testDataUtil.readMapsFromBz2Csv(content).stream()
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

	@SneakyThrows
	MockResponse metaGroup15() {
		var html = IOUtils.toString(
				ResourceUtil.loadContextual(MetaGroupScraperTest.class, "/meta-groups-15.html"),
				StandardCharsets.UTF_8);
		return new MockResponse().setResponseCode(200).setBody(html);
	}
}
