package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
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

		scrapeWars.setScrapeTime(ZonedDateTime.parse("2026-05-20T12:00:00Z"));
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFetchWars() {
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);

		scrapeWars.run();

		var root = getWarsCurrent();
		assertTrue(root.has("1000"));
		assertEquals(1, root.size());
		var war = root.get("1000");
		assertEquals("2026-01-01T00:00:00Z", war.get("declared").asText());
		assertEquals("2026-05-20T12:00:00Z", war.get("http_last_modified").asText());
		assertFalse(war.has("last_killmail_id"));

		var archive = getAndVerifyArchiveUpload();
		assertEquals(war, archive.getWar(1000));

		assertEquals(1, archive.getWarsCount());
		assertEquals(0, archive.getKillmailsCount());
	}

	@Test
	@SneakyThrows
	void shouldFetchWarsAndKillmails() {
		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);
		dispatcher.addKillmailForWar(1000L, 500001L, "hash1");
		dispatcher.addKillmailForWar(1000L, 500002L, "hash2");

		scrapeWars.run();

		var root = getWarsCurrent();
		assertTrue(root.has("1000"));
		assertEquals(1, root.size());

		var archive = getAndVerifyArchiveUpload();
		var war = (ObjectNode) root.get("1000").deepCopy();
		assertEquals(500002, war.get("last_killmail_id").asInt());

		war.remove("last_killmail_id");
		assertEquals(war.toString(), archive.getWar(1000).toString());

		assertEquals(
				buildExpectedKillmail(500001L, "hash1", 1000L).toString(),
				archive.getKillmail(1000, 500001).toString());
		assertEquals(
				buildExpectedKillmail(500002L, "hash2", 1000L).toString(),
				archive.getKillmail(1000, 500002).toString());

		assertEquals(1, archive.getWarsCount());
		assertEquals(2, archive.getKillmailsCount());
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

		var archive = getAndVerifyArchiveUpload();

		assertEquals(1000, archive.getWar(1000).get("war_id").asInt());
		assertEquals(1001, archive.getWar(1001).get("war_id").asInt());
		assertEquals(1002, archive.getWar(1002).get("war_id").asInt());

		assertEquals(
				500001, archive.getKillmail(1000, 500001).get("killmail_id").asInt());
		assertEquals(
				500002, archive.getKillmail(1000, 500002).get("killmail_id").asInt());
		assertEquals(
				500003, archive.getKillmail(1001, 500003).get("killmail_id").asInt());

		assertEquals(3, archive.getWarsCount());
		assertEquals(3, archive.getKillmailsCount());
	}

	@Test
	@SneakyThrows
	void shouldUpdateWarsToFinished() {
		var existingJson = ("""
				{
					"1001": {
						"war_id": 1001,
						"started": "2025-12-01T00:00:00Z",
						"declared": "2025-12-01T00:00:00Z"
					}
				}
				""").getBytes();
		dispatcher.setWarsCurrentJson(existingJson);

		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);
		dispatcher.addWar(1001L, "2026-01-01T00:00:00Z", "2026-03-01T00:00:00Z");

		dispatcher.addKillmailForWar(1001L, 500001L, "hash1");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertTrue(root.has("1000"));
		assertFalse(root.has("1001"));

		var archive = getAndVerifyArchiveUpload();

		assertFalse(archive.getWar(1000).has("finished"));
		assertEquals(
				"2026-03-01T00:00:00Z", archive.getWar(1001).get("finished").asText());

		assertNotNull(archive.getKillmail(1001, 500001));

		assertEquals(2, archive.getWarsCount());
		assertEquals(1, archive.getKillmailsCount());
	}

	@Test
	@SneakyThrows
	void shouldNotFetchOldKillmailsForWars() {
		var existingJson = ("""
				{
					"1000": {
						"war_id": 1000,
						"started": "2025-12-01T00:00:00Z",
						"declared": "2025-12-01T00:00:00Z",
						"last_killmail_id": 500000
					}
				}
				""").getBytes();
		dispatcher.setWarsCurrentJson(existingJson);

		dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);

		dispatcher.addKillmailForWar(1000, 499999, "hash1");
		dispatcher.addKillmailForWar(1000, 500000, "hash2");
		dispatcher.addKillmailForWar(1000, 500001, "hash3");

		scrapeWars.run();

		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		var root = objectMapper.readTree(warsCurrentJson.get());

		assertTrue(root.has("1000"));

		var archive = getAndVerifyArchiveUpload();

		assertNotNull(archive.getWar(1000));
		assertNotNull(archive.getKillmail(1000, 500001));

		assertEquals(1, archive.getWarsCount());
		assertEquals(1, archive.getKillmailsCount());
	}

	//		@Test
	//		@SneakyThrows
	//		void shouldUpdateWarsCurrentJsonWithNewWars() {
	//			var existingWars = objectMapper.createObjectNode();
	//			var war999 = objectMapper.createObjectNode();
	//			war999.put("id", 999L);
	//			war999.put("declared", "2025-12-01T00:00:00Z");
	//			war999.put("started", "2025-12-01T00:00:00Z");
	//			existingWars.set("999", war999);
	//
	//			var existingJson = objectMapper.writeValueAsBytes(existingWars);
	//			dispatcher.setWarsCurrentJson(existingJson);
	//
	//			dispatcher.addWar(999L, "2025-12-01T00:00:00Z", null);
	//			dispatcher.addWar(1000L, "2026-01-01T00:00:00Z", null);
	//
	//			scrapeWars.run();
	//
	//			var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
	//			var root = objectMapper.readTree(warsCurrentJson.get());
	//
	//			assertTrue(root.has("999"));
	//			assertTrue(root.has("1000"));
	//		}

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

	@Test
	@SneakyThrows
	void shouldIncludeUnknownExtraFields() {
		dispatcher.addWar(1000, "2026-01-01T00:00:00Z", null);
		dispatcher.addKillmailForWar(1000, 500001L, "hash1");

		scrapeWars.run();

		var root = getWarsCurrent();
		var war = root.get("1000");
		assertEquals("extra", war.get("extra").asText());

		var archive = getAndVerifyArchiveUpload();
		assertEquals("extra", archive.getWar(1000).get("extra").asText());
		assertEquals("extra", archive.getKillmail(1000, 500001).get("extra").asText());
	}

	private class TestDispatcher extends Dispatcher {
		private final Map<Long, WarData> wars = new HashMap<>();
		private final Map<Long, List<KillmailData>> killmailsByWar = new HashMap<>();
		private String warsCurrentJson = null;

		void addWar(long warId, String declared, String finished) {
			var war = new WarData();
			war.warId = warId;
			war.declared = declared;
			war.started = declared;
			war.finished = finished;
			war.aggressorId = 500001L;
			war.defenderId = 500002L;
			wars.put(warId, war);
		}

		void addKillmailForWar(long warId, long killmailId, String hash) {
			killmailsByWar.computeIfAbsent(warId, k -> new ArrayList<>()).add(new KillmailData(killmailId, hash));
		}

		void setWarsCurrentJson(byte[] json) {
			this.warsCurrentJson = new String(json);
		}

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			var path = request.getRequestUrl().encodedPath();
			var url = request.getRequestUrl().toString();
			log.debug("Received request: {}", url);

			try {
				// Handle /wars-current.json endpoint
				if (path.matches("^/wars-current\\.json$")) {
					if (warsCurrentJson != null) {
						return new MockResponse()
								.addHeader("Content-Type", "application/json")
								.setBody(warsCurrentJson);
					}
					return new MockResponse().setResponseCode(404);
				}

				// Handle /wars/ endpoint - return list of all war IDs
				if (path.matches("^/latest/wars/?$")) {
					var warIds = wars.entrySet().stream()
							.filter(e -> e.getValue().finished == null)
							.map(Map.Entry::getKey)
							.sorted()
							.map(Long::intValue)
							.toList();
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
					war.put("extra", "extra");

					return new MockResponse()
							.addHeader("Content-Type", "application/json")
							.addHeader("Last-Modified", "Wed, 20 May 2026 12:00:00 GMT")
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
				if (path.matches("^/latest/killmails/\\d+/[a-z0-9]+/?$")) {
					var killmailIdStr = path.replaceAll("^/latest/killmails/(\\d+)/([a-z0-9]+)/?$", "$1");
					var killmailId = Long.parseLong(killmailIdStr);

					var killmail = objectMapper.createObjectNode();
					killmail.put("killmail_id", killmailId);
					killmail.put("killmail_time", "2026-05-20T10:00:00Z");
					killmail.put("solar_system_id", 30002652L);
					killmail.put("extra", "extra");

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
							.addHeader("Last-Modified", "Wed, 20 May 2026 11:00:00 GMT")
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

	private JsonNode getWarsCurrent() throws IOException {
		var warsCurrentJson = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-current.json", dataClient);
		assertTrue(warsCurrentJson.isPresent());

		var root = objectMapper.readTree(warsCurrentJson.get());
		return root;
	}

	private TestWarsArchive getAndVerifyArchiveUpload() {
		var latestData = mockS3Adapter.getTestObject(DATA_BUCKET, "wars/wars-latest.tar.bz2", dataClient);
		var archiveData = mockS3Adapter.getTestObject(
				DATA_BUCKET, "wars/history/2026/wars-2026-05-20_12-00-00.tar.bz2", dataClient);
		assertTrue(latestData.isPresent());
		assertTrue(archiveData.isPresent());
		assertArrayEquals(archiveData.get(), latestData.get());

		return extractArchive(latestData.get());
	}

	private ObjectNode buildExpectedKillmail(long killmailId, String hash, long warId) {
		var killmail = objectMapper.createObjectNode();
		killmail.put("killmail_id", killmailId);
		killmail.put("killmail_time", "2026-05-20T10:00:00Z");
		killmail.put("solar_system_id", 30002652L);
		killmail.put("extra", "extra");

		var victim = objectMapper.createObjectNode();
		victim.put("character_id", 2012501001L);
		victim.put("corporation_id", 98654321L);
		victim.put("ship_type_id", 587L);
		victim.put("damage_taken", 15000);
		killmail.set("victim", victim);

		var attackers = objectMapper.createArrayNode();
		var attacker = objectMapper.createObjectNode();
		attacker.put("character_id", 2012501002L);
		attacker.put("corporation_id", 98654322L);
		attacker.put("damage_done", 15000L);
		attacker.put("final_blow", true);
		attackers.add(attacker);
		killmail.set("attackers", attackers);

		killmail.put("war_id", warId);
		killmail.put("killmail_hash", hash);
		killmail.put("http_last_modified", "2026-05-20T11:00:00Z");

		return killmail;
	}

	@SneakyThrows
	private TestWarsArchive extractArchive(byte[] compressed) {
		var archive = new TestWarsArchive();
		try (var in = new TarArchiveInputStream(new BZip2CompressorInputStream(new ByteArrayInputStream(compressed)))) {
			TarArchiveEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				// Read only the bytes for this entry, respecting TAR boundaries
				byte[] entryBytes = new byte[(int) entry.getSize()];
				int bytesRead = in.read(entryBytes);
				if (bytesRead == entry.getSize()) {
					var json = (ObjectNode) objectMapper.readTree(entryBytes);
					var path = entry.getName();

					// Parse path: wars/{warId}.json or wars/{warId}/killmails/{killmailId}.json
					if (path.startsWith("wars/") && path.endsWith(".json")) {
						var parts = path.substring(5, path.length() - 5)
								.split("/"); // Remove "wars/" prefix and ".json" suffix
						if (parts.length == 1) {
							// War file: wars/{warId}.json
							long warId = Long.parseLong(parts[0]);
							archive.addWar(warId, json);
						} else if (parts.length == 3 && "killmails".equals(parts[1])) {
							// Killmail file: wars/{warId}/killmails/{killmailId}.json
							long warId = Long.parseLong(parts[0]);
							long killmailId = Long.parseLong(parts[2]);
							archive.addKillmail(warId, killmailId, json);
						}
					}
				}
			}
		}
		assertNotEquals(0, archive.wars.size() + archive.killmailsByWar.size(), "Archive is empty");
		return archive;
	}

	private static class TestWarsArchive {
		private final Map<Long, ObjectNode> wars = new HashMap<>();
		private final Map<Long, Map<Long, ObjectNode>> killmailsByWar = new HashMap<>();

		public void addWar(long warId, ObjectNode war) {
			if (wars.containsKey(warId)) {
				throw new IllegalArgumentException("War " + warId + " already exists in archive");
			}
			wars.put(warId, war);
		}

		public void addKillmail(long warId, long killmailId, ObjectNode killmail) {
			var warKillmails = killmailsByWar.computeIfAbsent(warId, k -> new HashMap<>());
			if (warKillmails.containsKey(killmailId)) {
				throw new IllegalArgumentException(
						"Killmail " + killmailId + " for war " + warId + " already exists in archive");
			}
			warKillmails.put(killmailId, killmail);
		}

		public ObjectNode getWar(long warId) {
			var optional = Optional.ofNullable(wars.get(warId));
			assertTrue(optional.isPresent(), "War " + warId + " not found in archive");
			return optional.get();
		}

		public ObjectNode getKillmail(long warId, long killmailId) {
			var optional =
					Optional.ofNullable(killmailsByWar.get(warId)).flatMap(m -> Optional.ofNullable(m.get(killmailId)));
			assertTrue(optional.isPresent(), "Killmail " + killmailId + " for war " + warId + " not found in archive");
			return optional.get();
		}

		public int getWarsCount() {
			return wars.size();
		}

		public int getKillmailsCount() {
			return killmailsByWar.values().stream().mapToInt(Map::size).sum();
		}
	}
}
