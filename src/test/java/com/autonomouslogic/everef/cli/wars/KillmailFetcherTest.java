package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.KillmailsApi;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
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
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mockito;

/**
 * Integration tests for KillmailFetcher using MockWebServer to simulate ESI responses.
 */
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "KILLMAIL_LIST_CONCURRENCY", value = "2")
@SetEnvironmentVariable(key = "KILLMAIL_CONCURRENCY", value = "2")
public class KillmailFetcherTest {
	@Inject
	protected WarsApi warsApi;

	@Inject
	protected KillmailsApi killmailsApi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected KillmailFetcher killmailFetcher;

	@Inject
	protected ZkillboardHashCorrector zkillboardHashCorrector;

	private MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

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

	@Test
	@SneakyThrows
	void shouldCorrectInvalidHashViaZkillboard() {
		// Mock ZkillboardHashCorrector to return a corrected hash
		var mockCorrector = Mockito.mock(ZkillboardHashCorrector.class);
		when(mockCorrector.correctHash(1000L, "invalid_hash")).thenReturn(Optional.of("corrected_hash"));

		// Replace the real corrector with mock
		var fieldCorrector = KillmailFetcher.class.getDeclaredField("zkillboardHashCorrector");
		fieldCorrector.setAccessible(true);
		fieldCorrector.set(killmailFetcher, mockCorrector);

		// Use a dispatcher that returns 422 for invalid hash, 200 for corrected hash
		server.setDispatcher(new Hash422Dispatcher());

		var warId = 150000L;
		var killmailId = 1000L;

		// Fetch with invalid hash - should trigger 422 and correction
		killmailFetcher.fetchKillmailDetail(warId, killmailId, "invalid_hash");

		// Verify that the corrected killmail was cached
		var cached = killmailFetcher.getKillmail(killmailId);
		assertTrue(cached.isPresent());
		assertEquals(killmailId, cached.get().get("killmail_id").asLong());
		assertEquals("corrected_hash", cached.get().get("killmail_hash").asText());
	}

	/**
	 * Mock dispatcher that returns killmail data.
	 */
	private class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();
			log.debug("Received request: {}", path);

			// Return empty killmail list for wars
			if (path.matches(".*/wars/\\d+/killmails/?.*")) {
				return new MockResponse()
						.addHeader("Content-Type", "application/json")
						.setBody("[]");
			}

			// Return killmail detail
			if (path.matches(".*/killmails/\\d+/\\w+/?.*")) {
				try {
					var killmail = objectMapper.createObjectNode();
					killmail.put("killmail_id", 1000L);
					killmail.put("killmail_time", "2020-01-01T00:00:00Z");

					var victim = objectMapper.createObjectNode();
					victim.put("character_id", 123L);
					killmail.set("victim", victim);

					killmail.set("attackers", objectMapper.createArrayNode());

					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.setBody(objectMapper.writeValueAsString(killmail));
				} catch (Exception e) {
					log.error("Error in dispatcher", e);
					return new MockResponse().setResponseCode(500);
				}
			}

			return new MockResponse().setResponseCode(404);
		}
	}

	/**
	 * Dispatcher that returns 422 for invalid hash and 200 for corrected hash.
	 */
	private class Hash422Dispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();
			log.debug("Received request: {}", path);

			// Return empty killmail list for wars
			if (path.matches(".*/wars/\\d+/killmails/?.*")) {
				return new MockResponse()
						.addHeader("Content-Type", "application/json")
						.setBody("[]");
			}

			// Handle killmail requests - return 422 for invalid hash, 200 for corrected hash
			if (path.matches(".*/killmails/\\d+/\\w+/?.*")) {
				try {
					if (path.contains("invalid_hash")) {
						return new MockResponse().setResponseCode(422);
					}

					// Return valid killmail for corrected hash
					var killmail = objectMapper.createObjectNode();
					killmail.put("killmail_id", 1000L);
					killmail.put("killmail_time", "2020-01-01T00:00:00Z");

					var victim = objectMapper.createObjectNode();
					victim.put("character_id", 123L);
					killmail.set("victim", victim);

					killmail.set("attackers", objectMapper.createArrayNode());

					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.setBody(objectMapper.writeValueAsString(killmail));
				} catch (Exception e) {
					log.error("Error in dispatcher", e);
					return new MockResponse().setResponseCode(500);
				}
			}

			return new MockResponse().setResponseCode(404);
		}
	}
}
