package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for WarsFileBuilder, covering archive and JSON export creation.
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
public class WarsFileBuilderTest {
	private ObjectMapper objectMapper;
	private WarsFileBuilder fileBuilder;
	private Map<Long, JsonNode> warsMap;
	private Map<Long, JsonNode> killmailsMap;
	private Map<Long, Boolean> pendingKillmailsMap;

	@BeforeEach
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

		warsMap = new HashMap<>();
		killmailsMap = new HashMap<>();
		pendingKillmailsMap = new HashMap<>();

		fileBuilder = new WarsFileBuilder();
		fileBuilder.objectMapper = objectMapper;
		fileBuilder.setWarsMap(warsMap);
		fileBuilder.setKillmailsMap(killmailsMap);
		fileBuilder.setPendingKillmailsMap(pendingKillmailsMap);
	}

	@Test
	@SneakyThrows
	void shouldBuildEmptyArchive() {
		var file = fileBuilder.buildIncrementalExport(Instant.now());

		assertNotNull(file);
		assertTrue(file.exists());
		assertTrue(file.getName().endsWith(".tar.bz2"));
		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldBuildArchiveWithWar() {
		var warId = 1000L;
		var war = createMockWar(warId);
		warsMap.put(warId, war);

		var file = fileBuilder.buildIncrementalExport(Instant.parse("2020-01-01T00:00:00Z"));

		var entries = extractTarEntries(file);
		assertTrue(entries.contains("wars/1000.json"));

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldIncludeKillmailsInArchive() {
		var warId = 1000L;
		var war = createMockWar(warId);
		warsMap.put(warId, war);

		var km1 = createMockKillmail(100L, warId, Instant.parse("2020-01-02T00:00:00Z"));
		killmailsMap.put(100L, km1);
		pendingKillmailsMap.put(100L, true);

		var file = fileBuilder.buildIncrementalExport(Instant.parse("2020-01-01T00:00:00Z"));

		var entries = extractTarEntries(file);
		assertTrue(entries.contains("wars/1000.json"));
		assertTrue(entries.contains("wars/1000/killmails/100.json"));

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldFilterKillmailsByTime() {
		var warId = 1000L;
		var war = createMockWar(warId);
		warsMap.put(warId, war);

		// Old killmail
		var oldKm = createMockKillmail(100L, warId, Instant.parse("2020-01-01T00:00:00Z"));
		killmailsMap.put(100L, oldKm);

		// New killmail
		var newKm = createMockKillmail(101L, warId, Instant.parse("2020-01-03T00:00:00Z"));
		killmailsMap.put(101L, newKm);

		var exportTime = Instant.parse("2020-01-02T00:00:00Z");
		var file = fileBuilder.buildIncrementalExport(exportTime);

		var entries = extractTarEntries(file);
		// Old killmail shouldn't be included
		assertTrue(!entries.contains("wars/1000/killmails/100.json"));
		// New killmail should be included
		assertTrue(entries.contains("wars/1000/killmails/101.json"));

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldIncludePendingKillmails() {
		var warId = 1000L;
		var war = createMockWar(warId);
		warsMap.put(warId, war);

		// Old killmail but pending
		var oldKm = createMockKillmail(100L, warId, Instant.parse("2020-01-01T00:00:00Z"));
		killmailsMap.put(100L, oldKm);
		pendingKillmailsMap.put(100L, true);

		var exportTime = Instant.parse("2020-01-02T00:00:00Z");
		var file = fileBuilder.buildIncrementalExport(exportTime);

		var entries = extractTarEntries(file);
		// Pending killmail should be included even if old
		assertTrue(entries.contains("wars/1000/killmails/100.json"));

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldBuildCurrentWarsJson() {
		var unfinishedWar = createMockWar(1000L);
		warsMap.put(1000L, unfinishedWar);

		var finishedWar = createMockWar(1001L);
		((com.fasterxml.jackson.databind.node.ObjectNode) finishedWar)
				.put("finished", Instant.now().toString());
		warsMap.put(1001L, finishedWar);

		var file = fileBuilder.buildCurrentWarsJson();

		assertNotNull(file);
		assertTrue(file.exists());

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldHandleMultipleWars() {
		var war1 = createMockWar(1000L);
		var war2 = createMockWar(1001L);
		warsMap.put(1000L, war1);
		warsMap.put(1001L, war2);

		var file = fileBuilder.buildIncrementalExport(Instant.parse("2020-01-01T00:00:00Z"));

		var entries = extractTarEntries(file);
		assertEquals(
				2,
				entries.stream()
						.filter(e -> e.startsWith("wars/") && e.endsWith(".json"))
						.count());

		file.delete();
	}

	/**
	 * Creates a mock war JsonNode.
	 */
	private com.fasterxml.jackson.databind.node.ObjectNode createMockWar(long warId) {
		var war = objectMapper.createObjectNode();
		war.put("id", warId);
		war.put("declared", Instant.parse("2020-01-01T00:00:00Z").toString());
		return war;
	}

	/**
	 * Creates a mock killmail JsonNode.
	 */
	private com.fasterxml.jackson.databind.node.ObjectNode createMockKillmail(
			long killmailId, long warId, Instant time) {
		var km = objectMapper.createObjectNode();
		km.put("killmail_id", killmailId);
		km.put("war_id", warId);
		km.put("killmail_time", time.toString());
		return km;
	}

	/**
	 * Extracts all entry names from a TAR.BZ2 file.
	 */
	@SneakyThrows
	private Set<String> extractTarEntries(java.io.File tarBz2File) {
		var entries = new HashSet<String>();

		try (ArchiveInputStream tar = CompressUtil.uncompressArchive(tarBz2File)) {
			ArchiveEntry entry;
			while ((entry = tar.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					entries.add(entry.getName());
				}
			}
		}

		return entries;
	}
}
