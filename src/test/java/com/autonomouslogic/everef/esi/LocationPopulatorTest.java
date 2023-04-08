package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.openapi.esi.models.GetUniverseConstellationsConstellationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseConstellationsConstellationIdPosition;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseStationsStationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseStationsStationIdPosition;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseSystemsSystemIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseSystemsSystemIdPosition;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import javax.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_PATH", value = "http://localhost:" + TestDataUtil.TEST_PORT)
public class LocationPopulatorTest {
	@Inject
	LocationPopulator locationPopulator;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer server;
	String region;
	String constellation;
	String system;
	String station;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();

		region = objectMapper.writeValueAsString(new GetUniverseRegionsRegionIdOk(List.of(), "Region", 100, ""));
		constellation = objectMapper.writeValueAsString(new GetUniverseConstellationsConstellationIdOk(
				200, "Constellation", new GetUniverseConstellationsConstellationIdPosition(0, 0, 0), 100, List.of()));
		system = objectMapper.writeValueAsString(new GetUniverseSystemsSystemIdOk(
				200,
				"System",
				new GetUniverseSystemsSystemIdPosition(0, 0, 0),
				0,
				300,
				List.of(),
				"",
				0,
				List.of(),
				List.of()));
		station = objectMapper.writeValueAsString(new GetUniverseStationsStationIdOk(
				0,
				"Station",
				0,
				new GetUniverseStationsStationIdPosition(0, 0, 0),
				0,
				0,
				List.of(),
				400,
				300,
				0,
				null,
				null));

		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				return switch (recordedRequest.getPath()) {
					case "/universe/regions/100/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody(region);
					case "/universe/constellations/200/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody(constellation);
					case "/universe/systems/300/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody(system);
					case "/universe/stations/400/?datasource=tranquility" -> new MockResponse()
							.setResponseCode(200)
							.setBody(station);
					default -> new MockResponse().setResponseCode(404);
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

	private ObjectNode baseRecord() {
		return objectMapper
				.createObjectNode()
				.put("region_id", 100)
				.put("constellation_id", 200)
				.put("system_id", 300)
				.put("station_id", 400);
	}

	@Test
	void shouldPopulateStationIdFromCustomKey() {
		var record = baseRecord();
		record.remove("station_id");
		record.put("custom_id", 401);
		locationPopulator.populate(record, "custom_id").blockingAwait();
		assertEquals(401, record.get("station_id").asInt());
	}

	@Test
	void shouldPopulateSystemIdFromStation() {
		var record = baseRecord();
		record.remove("system_id");
		locationPopulator.populate(record).blockingAwait();
		assertEquals(300, record.get("system_id").asInt());
	}

	@Test
	void shouldPopulateConstellationIdFromSystem() {
		var record = baseRecord();
		record.remove("constellation_id");
		locationPopulator.populate(record).blockingAwait();
		assertEquals(200, record.get("constellation_id").asInt());
	}

	@Test
	void shouldPopulateRegionIdFromConstellation() {
		var record = baseRecord();
		record.remove("region_id");
		locationPopulator.populate(record).blockingAwait();
		assertEquals(100, record.get("region_id").asInt());
	}

	@Test
	void shouldPopulateRegionIdFromStation() {
		var record = baseRecord();
		record.remove("region_id");
		record.remove("constellation_id");
		record.remove("system_id");
		locationPopulator.populate(record).blockingAwait();
		assertEquals(100, record.get("region_id").asInt());
	}

	@Test
	void shouldLeavePopulatedLocations() {
		var record = objectMapper
				.createObjectNode()
				.put("region_id", 999)
				.put("constellation_id", 999)
				.put("system_id", 999)
				.put("station_id", 999);
		var original = record.deepCopy();
		locationPopulator.populate(record).blockingAwait();
		assertEquals(original, record);
	}

	@Test
	void shouldLeaveEmptyRecords() {
		var record = objectMapper.createObjectNode();
		var original = record.deepCopy();
		locationPopulator.populate(record).blockingAwait();
		assertEquals(original, record);
	}
}
