package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for WarsFetcher using MockWebServer to simulate ESI responses.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "WARS_FETCH_CONCURRENCY", value = "2")
public class WarsFetcherTest {
	private ObjectMapper objectMapper;
	private WarsApi warsApi;
	private WarsFetcher warsFetcher;
	private Map<Long, JsonNode> warsMap;
	private MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		warsApi = org.mockito.Mockito.mock(WarsApi.class);

		warsMap = new HashMap<>();
		warsFetcher = new WarsFetcher();
		warsFetcher.objectMapper = objectMapper;
		warsFetcher.warsApi = warsApi;
		warsFetcher.setWarsMap(warsMap);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFetchSingleWar() {
		warsFetcher.fetchWars(Set.of(1000L));

		assertTrue(warsMap.containsKey(1000L));
		var war = warsMap.get(1000L);
		assertEquals(1000L, war.get("id").asLong());
	}

	@Test
	@SneakyThrows
	void shouldSkipBadWar90591() {
		warsFetcher.fetchWars(Set.of(90591L));
		assertTrue(warsMap.isEmpty());
	}

	@Test
	@SneakyThrows
	void shouldSkipBadWar473095() {
		warsFetcher.fetchWars(Set.of(473095L));
		assertTrue(warsMap.isEmpty());
	}

	@Test
	@SneakyThrows
	void shouldSkipBadWarsInRange() {
		warsFetcher.fetchWars(Set.of(472167L, 472500L, 473147L));
		assertTrue(warsMap.isEmpty());
	}

	@Test
	@SneakyThrows
	void shouldSkipWar404NotFound() {
		warsFetcher.fetchWars(Set.of(404000L));
		assertTrue(warsMap.isEmpty());
	}

	@Test
	@SneakyThrows
	void shouldFetchMultipleWars() {
		warsFetcher.fetchWars(Set.of(1000L, 1001L, 1002L));

		assertEquals(3, warsMap.size());
		assertTrue(warsMap.containsKey(1000L));
		assertTrue(warsMap.containsKey(1001L));
		assertTrue(warsMap.containsKey(1002L));
	}

	@Test
	@SneakyThrows
	void shouldHandleEmptyWarIdSet() {
		warsFetcher.fetchWars(Set.of());
		assertTrue(warsMap.isEmpty());
	}

	/**
	 * Mock dispatcher that returns war data or 404 for unknown wars.
	 */
	private class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();

			if (path.matches("/latest/wars/\\d+/")) {
				try {
					var warIdStr = path.replaceAll("/latest/wars/(\\d+)/", "$1");
					var warId = Long.parseLong(warIdStr);

					if (warId == 404000L) {
						return new MockResponse().setResponseCode(404);
					}

					var war = objectMapper.createObjectNode();
					war.put("id", warId);
					war.put("declared", "2020-01-01T00:00:00Z");
					war.put("started", "2020-01-01T00:00:00Z");

					var aggressor = objectMapper.createObjectNode();
					aggressor.put("faction_id", 500001L);
					aggressor.put("faction_name", "Aggressor Faction");
					war.set("aggressor", aggressor);

					var defender = objectMapper.createObjectNode();
					defender.put("faction_id", 500002L);
					defender.put("faction_name", "Defender Faction");
					war.set("defender", defender);

					return new MockResponse().setBody(objectMapper.writeValueAsString(war));
				} catch (Exception e) {
					log.error("Error in dispatcher", e);
					return new MockResponse().setResponseCode(500);
				}
			}

			return new MockResponse().setResponseCode(404);
		}
	}
}
