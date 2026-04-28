package com.autonomouslogic.everef.cli.wars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	@BeforeEach
	void setup() {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

		warsMap = new HashMap<>();

		fileBuilder = new WarsFileBuilder();
		fileBuilder.objectMapper = objectMapper;
		fileBuilder.setWarsMap(warsMap);
	}

	@Test
	@SneakyThrows
	void shouldBuildEmptyArchive() {
		var killmailsMap = new HashMap<Long, JsonNode>();
		var file = fileBuilder.buildIncrementalExport(killmailsMap);

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

		var killmailsMap = new HashMap<Long, JsonNode>();
		var file = fileBuilder.buildIncrementalExport(killmailsMap);

		var entries = extractTarEntries(file);
		assertTrue(entries.isEmpty()); // No killmails to export

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldIncludeKillmailsInArchive() {
		var warId = 1000L;
		var war = createMockWar(warId);
		warsMap.put(warId, war);

		var killmailsMap = new HashMap<Long, JsonNode>();
		var km1 = createMockKillmail(100L, warId);
		killmailsMap.put(100L, km1);

		var file = fileBuilder.buildIncrementalExport(killmailsMap);

		var entries = extractTarEntries(file);
		assertTrue(entries.contains("wars/1000.json"));
		assertTrue(entries.contains("wars/1000/killmails/100.json"));

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldHandleMultipleWars() {
		var war1 = createMockWar(1000L);
		var war2 = createMockWar(1001L);
		warsMap.put(1000L, war1);
		warsMap.put(1001L, war2);

		var killmailsMap = new HashMap<Long, JsonNode>();
		var km1 = createMockKillmail(100L, 1000L);
		var km2 = createMockKillmail(101L, 1001L);
		killmailsMap.put(100L, km1);
		killmailsMap.put(101L, km2);

		var file = fileBuilder.buildIncrementalExport(killmailsMap);

		var entries = extractTarEntries(file);
		assertEquals(
				2, entries.stream().filter(e -> e.matches("wars/\\d+\\.json")).count());

		file.delete();
	}

	@Test
	@SneakyThrows
	void shouldBuildCurrentWarsJson() {
		var unfinishedWar = createMockWar(1000L);
		warsMap.put(1000L, unfinishedWar);

		var finishedWar = createMockWar(1001L);
		((com.fasterxml.jackson.databind.node.ObjectNode) finishedWar)
				.put("finished", java.time.Instant.now().toString());
		warsMap.put(1001L, finishedWar);

		var file = fileBuilder.buildCurrentWarsJson();

		assertNotNull(file);
		assertTrue(file.exists());

		// Verify it only contains unfinished war
		var content = objectMapper.readTree(file);
		assertTrue(content.has("1000"));
		assertTrue(!content.has("1001")); // Finished war should not be included

		file.delete();
	}

	/**
	 * Creates a mock war JsonNode.
	 */
	private com.fasterxml.jackson.databind.node.ObjectNode createMockWar(long warId) {
		var war = objectMapper.createObjectNode();
		war.put("id", warId);
		war.put("declared", java.time.Instant.parse("2020-01-01T00:00:00Z").toString());
		return war;
	}

	/**
	 * Creates a mock killmail JsonNode.
	 */
	private com.fasterxml.jackson.databind.node.ObjectNode createMockKillmail(long killmailId, long warId) {
		var km = objectMapper.createObjectNode();
		km.put("killmail_id", killmailId);
		km.put("war_id", warId);
		km.put("killmail_time", java.time.Instant.now().toString());
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
