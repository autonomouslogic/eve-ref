package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.GenerateMapData.MapRegion;
import com.autonomouslogic.everef.refdata.Coordinate;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.List;
import javax.inject.Inject;
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
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
public class GenerateMapDataTest {
	@Inject
	protected GenerateMapData generateMapData;

	@Inject
	protected MockScrapeBuilder mockScrapeBuilder;

	@Inject
	protected JsonMapper jsonMapper;

	MockWebServer server;

	File refDataFile;

	@Inject
	protected GenerateMapDataTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TEST_PORT);

		refDataFile = mockScrapeBuilder.createTestRefdata();

		new File(GenerateMapData.MAP_REGIONS_FILE).delete();
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldGenerateMapData() {
		VirtualThreads.onVirtualThread(generateMapData::run);

		// stargatesToRegionIds omitted: the test fixtures have no connections, and NON_EMPTY drops them.
		var expected = List.of(
				MapRegion.builder()
						.regionId(10000001L)
						.universeId("eve")
						.name("Derelik")
						.position(coordinate("-77361951922776930", "50878032664301930", "-64433101266115400"))
						.build(),
				MapRegion.builder()
						.regionId(11000001L)
						.universeId("wormhole")
						.name("A-R00001")
						.position(coordinate("7637617076349301000", "1539385485286039300", "-9497611206336487000"))
						.build(),
				MapRegion.builder()
						.regionId(12000001L)
						.universeId("abyssal")
						.name("ADR01")
						.position(coordinate("5332454814931104000", "6032700368271842000", "-8324663071957517000"))
						.build(),
				MapRegion.builder()
						.regionId(14000001L)
						.universeId("void")
						.name("VR-01")
						.position(coordinate("-3900972456350441500", "2574944990858207000", "-8266927768219926000"))
						.build(),
				MapRegion.builder()
						.regionId(19000001L)
						.universeId("hidden")
						.name("GPMR-01")
						.position(coordinate("1.0", "1.0", "1.0"))
						.build());

		assertMapRegionsFile(expected);
	}

	private static Coordinate coordinate(String x, String y, String z) {
		return Coordinate.builder()
				.x(new BigDecimal(x))
				.y(new BigDecimal(y))
				.z(new BigDecimal(z))
				.build();
	}

	@SneakyThrows
	private void assertMapRegionsFile(List<MapRegion> expected) {
		var file = new File(GenerateMapData.MAP_REGIONS_FILE);
		var actual = jsonMapper.readValue(
				file, jsonMapper.getTypeFactory().constructCollectionType(List.class, MapRegion.class));
		assertEquals(expected, actual);
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				log.info("Path: {}", path);
				if (path.equals("/reference-data/reference-data-latest.tar.xz")) {
					return new MockResponse()
							.setResponseCode(200)
							.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(refDataFile))));
				}
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
