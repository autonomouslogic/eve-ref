package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Integration tests for ScrapeWars using MockWebServer (ESI) and MockS3Adapter (S3).
 */
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/latest/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeWarsTest.DATA_BUCKET + "/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "DATA_LATEST_CACHE_CONTROL_MAX_AGE", value = "PT5M")
@SetEnvironmentVariable(key = "WARS_FETCH_CONCURRENCY", value = "2")
@SetEnvironmentVariable(key = "KILLMAIL_LIST_CONCURRENCY", value = "2")
@SetEnvironmentVariable(key = "KILLMAIL_CONCURRENCY", value = "2")
public class ScrapeWarsTest {
	static final String DATA_BUCKET = "data-bucket";

	@Inject
	protected ScrapeWars scrapeWars;

	@Inject
	protected WarsApi warsApi;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	protected S3AsyncClient dataClient;

	private MockWebServer server;
	private TestDispatcher dispatcher;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		dispatcher = new TestDispatcher();
		server = new MockWebServer();
		server.setDispatcher(dispatcher);
		server.start(TestDataUtil.TEST_PORT);

		// Initialize scrape time for consistent test behavior
		scrapeWars.setScrapeTime(ZonedDateTime.of(2026, 5, 20, 12, 0, 0, 0, ZoneOffset.UTC));
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFetchWarsAndKillmails() {
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);
		dispatcher.addWar(1001L, "2026-02-01T00:00:00Z", null);
		dispatcher.addKillmailForWar(1000L, 500001L, "hash1");
		dispatcher.addKillmailForWar(1000L, 500002L, "hash2");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		assertTrue(warsCurrentJson.isPresent());

		var root = objectMapper.readTree(warsCurrentJson.get());
		assertTrue(root.has("1000"));
		assertTrue(root.has("1001"));

		var putKeys = mockS3Adapter.getAllPutKeys(DATA_BUCKET, dataClient);
		var hasIncrementalExport = putKeys.stream()
				.anyMatch(key -> key.matches(".*wars-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.tar\\.bz2"));
		assertTrue(hasIncrementalExport);
	}

	@Test
	@SneakyThrows
	void shouldFetchOnlyUnfinishedWars() {
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);
		dispatcher.addWar(1001L, "2026-02-01T00:00:00Z", "2026-03-01T00:00:00Z");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertTrue(root.has("1000"));
		assertFalse(root.has("1001"));
	}

	@Test
	@SneakyThrows
	void shouldUpdateWarsCurrentJsonWithNewWars() {
		var existingWars = objectMapper.createObjectNode();
		var war999 = objectMapper.createObjectNode();
		war999.put("id", 999L);
		war999.put("declared", "2025-12-01T00:00:00Z");
		war999.put("started", "2025-12-01T00:00:00Z");
		existingWars.set("999", war999);

		var existingJson = objectMapper.writeValueAsBytes(existingWars);
		mockS3Adapter.putTestObject(DATA_BUCKET, "wars/wars-current.json", existingJson, dataClient);

		dispatcher.addWar(999L, "2025-12-01T00:00:00Z", null);
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertTrue(root.has("999"));
		assertTrue(root.has("1000"));
	}

	@Test
	@SneakyThrows
	void shouldHandleNoKillmails() {
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		assertTrue(warsCurrentJson.isPresent());

		var root = objectMapper.readTree(warsCurrentJson.get());
		assertTrue(root.has("1000"));
	}

	@Test
	@SneakyThrows
	void shouldFetchMultipleWarsAndKillmails() {
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);
		dispatcher.addWar(1001L, "2026-02-01T00:00:00Z", null);
		dispatcher.addWar(1002L, "2026-03-01T00:00:00Z", null);

		dispatcher.addKillmailForWar(1000L, 500001L, "hash1");
		dispatcher.addKillmailForWar(1000L, 500002L, "hash2");
		dispatcher.addKillmailForWar(1001L, 500003L, "hash3");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertEquals(3, root.size());
		assertTrue(root.has("1000"));
		assertTrue(root.has("1001"));
		assertTrue(root.has("1002"));
	}

	@Test
	@SneakyThrows
	void shouldHandleEarlyWarsWithLimitedKillmails() {
		dispatcher.addWar(48074L, "2003-01-01T00:00:00Z", null);
		dispatcher.addWar(50000L, "2003-05-01T00:00:00Z", null);

		dispatcher.addKillmailForWar(48074L, 500001L, "hash1");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertTrue(root.has("48074"));
		assertTrue(root.has("50000"));
	}

	@Test
	@SneakyThrows
	void shouldSkipKillmailsNotInEarlyWarsList() {
		dispatcher.addWar(50000L, "2003-05-01T00:00:00Z", null);
		dispatcher.addWar(149786L, "2009-01-01T00:00:00Z", null);

		dispatcher.addKillmailForWar(50000L, 500001L, "hash1");
		dispatcher.addKillmailForWar(149786L, 500002L, "hash2");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertTrue(root.has("50000"));
		assertTrue(root.has("149786"));
	}

	private class TestDispatcher extends Dispatcher {
		private final Map<Long, WarData> wars = new HashMap<>();
		private final Map<Long, List<KillmailData>> killmailsByWar = new HashMap<>();

		void addWar(Long warId, String declared, String finished) {
			var war = new WarData();
			war.warId = warId;
			war.declared = declared;
			war.started = declared;
			war.finished = finished;
			war.aggressorId = 500001L;
			war.defenderId = 500002L;
			wars.put(warId, war);
		}

		void addKillmailForWar(Long warId, Long killmailId, String hash) {
			killmailsByWar.computeIfAbsent(warId, k -> new ArrayList<>()).add(new KillmailData(killmailId, hash));
		}

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();
			var url = request.getRequestUrl().toString();
			log.debug("Received request: {}", url);

			try {
				// Handle /wars/ endpoint - return list of all war IDs
				if (path.matches("^/latest/wars/?$")) {
					var warIds =
							wars.keySet().stream().sorted().map(Long::intValue).toList();
					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.addHeader("X-Pages", "1")
							.setBody(objectMapper.writeValueAsString(warIds));
				}

				// Handle /wars/{warId}/ endpoint
				if (path.matches("^/latest/wars/\\d+/?$")) {
					var warIdStr = path.replaceAll("^/latest/wars/(\\d+)/?$", "$1");
					var warId = Long.parseLong(warIdStr);

					if (!wars.containsKey(warId)) {
						return new MockResponse().setResponseCode(404);
					}

					var warData = wars.get(warId);
					var war = objectMapper.createObjectNode();
					war.put("war_id", warId);
					war.put("declared", warData.declared);
					war.put("started", warData.started);
					if (warData.finished != null) {
						war.put("finished", warData.finished);
					}
					war.put("aggressor_id", warData.aggressorId);
					war.put("defender_id", warData.defenderId);
					war.put("mutual", false);
					war.put("open_for_allies", true);
					war.put("reward", 0L);

					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.addHeader("Last-Modified", "Tue, 20 May 2026 12:00:00 GMT")
							.setBody(objectMapper.writeValueAsString(war));
				}

				// Handle /wars/{warId}/killmails/ endpoint
				if (path.matches("^/latest/wars/\\d+/killmails/?.*")) {
					var warIdStr = path.replaceAll("^/latest/wars/(\\d+)/killmails.*", "$1");
					var warId = Long.parseLong(warIdStr);

					if (!wars.containsKey(warId)) {
						return new MockResponse().setResponseCode(404);
					}

					var killmails = killmailsByWar.getOrDefault(warId, List.of());
					var killmailList = killmails.stream()
							.map(km -> {
								var obj = objectMapper.createObjectNode();
								obj.put("killmail_id", km.killmailId);
								obj.put("killmail_hash", km.hash);
								return obj;
							})
							.toList();

					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.addHeader("X-Pages", "1")
							.setBody(objectMapper.writeValueAsString(killmailList));
				}

				// Handle /killmails/{killmailId}/{hash}/ endpoint
				if (path.matches("^/latest/killmails/\\d+/[a-f0-9]+/?$")) {
					var killmailIdStr = path.replaceAll("^/latest/killmails/(\\d+)/([a-f0-9]+)/?$", "$1");
					var killmailId = Long.parseLong(killmailIdStr);

					var killmail = objectMapper.createObjectNode();
					killmail.put("killmail_id", killmailId);
					killmail.put("killmail_time", "2026-05-20T10:00:00Z");
					killmail.put("solar_system_id", 30002652L);

					var victim = objectMapper.createObjectNode();
					victim.put("character_id", 2012501001L);
					victim.put("corporation_id", 98654321L);
					victim.put("ship_type_id", 587L);
					victim.put("damage_taken", 15000.0);
					killmail.set("victim", victim);

					var attackers = objectMapper.createArrayNode();
					var attacker = objectMapper.createObjectNode();
					attacker.put("character_id", 2012501002L);
					attacker.put("corporation_id", 98654322L);
					attacker.put("damage_done", 15000L);
					attacker.put("final_blow", true);
					attackers.add(attacker);
					killmail.set("attackers", attackers);

					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.addHeader("Last-Modified", "Tue, 20 May 2026 11:00:00 GMT")
							.setBody(objectMapper.writeValueAsString(killmail));
				}

				log.warn("Unhandled request: {}", path);
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}

		static class WarData {
			long warId;
			String declared;
			String started;
			String finished;
			long aggressorId;
			long defenderId;
		}

		static class KillmailData {
			long killmailId;
			String hash;

			KillmailData(long killmailId, String hash) {
				this.killmailId = killmailId;
				this.hash = hash;
			}
		}
	}
}
