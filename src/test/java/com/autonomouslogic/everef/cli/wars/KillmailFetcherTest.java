package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.KillmailsApi;
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
 * Integration tests for KillmailFetcher using MockWebServer to simulate ESI responses.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "KILLMAIL_LIST_CONCURRENCY", value = "2")
@SetEnvironmentVariable(key = "KILLMAIL_DETAIL_CONCURRENCY", value = "2")
public class KillmailFetcherTest {
	private ObjectMapper objectMapper;
	private WarsApi warsApi;
	private KillmailsApi killmailsApi;
	private EsiHelper esiHelper;
	private KillmailFetcher killmailFetcher;
	private Map<Long, JsonNode> killmailsMap;
	private Map<Long, Boolean> pendingKillmailsMap;
	private MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		warsApi = org.mockito.Mockito.mock(WarsApi.class);
		killmailsApi = org.mockito.Mockito.mock(KillmailsApi.class);
		esiHelper = org.mockito.Mockito.mock(EsiHelper.class);

		killmailsMap = new HashMap<>();
		pendingKillmailsMap = new HashMap<>();

		killmailFetcher = new KillmailFetcher();
		killmailFetcher.objectMapper = objectMapper;
		killmailFetcher.warsApi = warsApi;
		killmailFetcher.killmailsApi = killmailsApi;
		killmailFetcher.esiHelper = esiHelper;
		killmailFetcher.zkillboardHashCorrector = new ZkillboardHashCorrector();
		killmailFetcher.zkillboardHashCorrector.objectMapper = objectMapper;

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
	void shouldSkipOldWarsWithoutKillmails() {
		var warId = 100000L; // < 149786 and not in special list
		killmailFetcher.fetchKillmails(Set.of(warId));

		assertTrue(killmailFetcher.getAllCachedKillmails().isEmpty());
	}

	@Test
	@SneakyThrows
	void shouldFetchKillmailsForNewWar() {
		var warId = 150000L; // >= 149786
		killmailFetcher.fetchKillmails(Set.of(warId));

		// Should attempt to fetch killmails for new wars
		assertTrue(server.getRequestCount() > 0);
	}

	@Test
	@SneakyThrows
	void shouldFetchKillmailsForSpecialEarlyWar48074() {
		var warId = 48074L;
		killmailFetcher.fetchKillmails(Set.of(warId));

		// Should attempt to fetch killmails for special early war
		assertTrue(server.getRequestCount() > 0);
	}

	@Test
	@SneakyThrows
	void shouldFetchKillmailsForSpecialEarlyWar138678() {
		var warId = 138678L;
		killmailFetcher.fetchKillmails(Set.of(warId));

		assertTrue(server.getRequestCount() > 0);
	}

	@Test
	@SneakyThrows
	void shouldFetchKillmailsForSpecialEarlyWar144630() {
		var warId = 144630L;
		killmailFetcher.fetchKillmails(Set.of(warId));

		assertTrue(server.getRequestCount() > 0);
	}

	@Test
	@SneakyThrows
	void shouldFetchKillmailsForSpecialEarlyWar149785() {
		var warId = 149785L;
		killmailFetcher.fetchKillmails(Set.of(warId));

		assertTrue(server.getRequestCount() > 0);
	}

	@Test
	@SneakyThrows
	void shouldHandleEmptyWarIdSet() {
		killmailFetcher.fetchKillmails(Set.of());
		assertTrue(killmailFetcher.getAllCachedKillmails().isEmpty());
	}

	/**
	 * Mock dispatcher that returns killmail data.
	 */
	private class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();

			// Return empty killmail list for wars
			if (path.matches("/latest/wars/\\d+/killmails/")) {
				return new MockResponse().setBody("[]");
			}

			// Return killmail detail
			if (path.matches("/latest/killmails/\\d+/\\w+/")) {
				try {
					var killmail = objectMapper.createObjectNode();
					killmail.put("killmail_id", 1000L);
					killmail.put("killmail_time", "2020-01-01T00:00:00Z");

					var victim = objectMapper.createObjectNode();
					victim.put("character_id", 123L);
					killmail.set("victim", victim);

					killmail.set("attackers", objectMapper.createArrayNode());

					return new MockResponse().setBody(objectMapper.writeValueAsString(killmail));
				} catch (Exception e) {
					log.error("Error in dispatcher", e);
					return new MockResponse().setResponseCode(500);
				}
			}

			return new MockResponse().setResponseCode(404);
		}
	}
}
