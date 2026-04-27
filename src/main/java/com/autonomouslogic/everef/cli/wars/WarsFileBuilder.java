package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * Builds TAR.BZ2 archives and JSON exports of wars and killmails.
 */
@Log4j2
public class WarsFileBuilder {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected WarsFileBuilder() {}

	@Setter
	private Map<Long, JsonNode> warsMap;

	@Setter
	private Map<Long, JsonNode> killmailsMap;

	@Setter
	private Map<Long, Boolean> pendingKillmailsMap;

	/**
	 * Build an incremental export TAR archive containing wars and killmails.
	 *
	 * @param lastExportTime the timestamp of the last export (to filter killmails)
	 * @return the compressed TAR.BZ2 file
	 */
	@SneakyThrows
	public File buildIncrementalExport(Instant lastExportTime) {
		var warsToExport = collectWarsToExport(lastExportTime);
		var killmailsByWar = collectKillmailsToExport(warsToExport, lastExportTime);

		var tarFile = File.createTempFile("wars", ".tar");
		tarFile.deleteOnExit();
		var modTime = System.currentTimeMillis();

		try (var tar = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(tarFile)))) {
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

			for (var warId : warsToExport) {
				var war = warsMap.get(warId);
				if (war != null) {
					// Write wars/{warId}.json
					var warJson = objectMapper.writeValueAsBytes(war);
					var warPath = "wars/" + warId + ".json";
					writeEntry(tar, warPath, warJson, modTime);

					// Write killmails for this war
					var killmails = killmailsByWar.getOrDefault(warId, List.of());
					for (var kmId : killmails) {
						var km = killmailsMap.get(kmId);
						if (km != null) {
							var kmJson = objectMapper.writeValueAsBytes(km);
							var kmPath = "wars/" + warId + "/killmails/" + kmId + ".json";
							writeEntry(tar, kmPath, kmJson, modTime);
						}
					}
				}
			}
		}

		var compressedFile = CompressUtil.compressBzip2(tarFile);
		tarFile.delete();
		return compressedFile;
	}

	/**
	 * Build a JSON file containing only unfinished wars.
	 *
	 * @return the JSON file
	 */
	@SneakyThrows
	public File buildCurrentWarsJson() {
		var currentWars = new java.util.TreeMap<Long, JsonNode>();

		for (var entry : warsMap.entrySet()) {
			var war = entry.getValue();
			// Only include unfinished wars
			if (!war.has("finished") || war.get("finished").isNull()) {
				currentWars.put(entry.getKey(), war);
			}
		}

		var jsonFile = File.createTempFile("wars-current", ".json");
		jsonFile.deleteOnExit();
		var json = objectMapper.createObjectNode();

		for (var entry : currentWars.entrySet()) {
			json.set(entry.getKey().toString(), entry.getValue());
		}

		var content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(json);
		java.nio.file.Files.write(jsonFile.toPath(), content);

		return jsonFile;
	}

	private Set<Long> collectWarsToExport(Instant lastExportTime) {
		var warsToExport = new TreeSet<Long>();

		for (var entry : killmailsMap.entrySet()) {
			var kmId = entry.getKey();
			var km = entry.getValue();

			// Check if killmail should be exported
			if (shouldExportKillmail(km, lastExportTime, kmId)) {
				var warId = km.get("war_id");
				if (warId != null) {
					warsToExport.add(warId.asLong());
				}
			}
		}

		return warsToExport;
	}

	private Map<Long, List<Long>> collectKillmailsToExport(Set<Long> warsToExport, Instant lastExportTime) {
		var killmailsByWar = new java.util.TreeMap<Long, List<Long>>();

		for (var entry : killmailsMap.entrySet()) {
			var kmId = entry.getKey();
			var km = entry.getValue();

			if (shouldExportKillmail(km, lastExportTime, kmId)) {
				var warId = km.get("war_id");
				if (warId != null) {
					var wid = warId.asLong();
					if (warsToExport.contains(wid)) {
						killmailsByWar
								.computeIfAbsent(wid, k -> new java.util.ArrayList<>())
								.add(kmId);
					}
				}
			}
		}

		// Sort killmails by ID within each war
		killmailsByWar.forEach((k, v) -> v.sort(Long::compareTo));

		return killmailsByWar;
	}

	private boolean shouldExportKillmail(JsonNode km, Instant lastExportTime, long kmId) {
		// Check if it's in pending set
		if (pendingKillmailsMap.containsKey(kmId)) {
			return true;
		}

		// Check if killmail_time is after lastExportTime
		var kmTime = km.get("killmail_time");
		if (kmTime != null && kmTime.isTextual()) {
			try {
				var instant = Instant.parse(kmTime.asText());
				if (instant.isAfter(lastExportTime)) {
					return true;
				}
			} catch (Exception e) {
				log.warn("Failed to parse killmail_time for {}: {}", kmId, kmTime);
			}
		}

		return false;
	}

	@SneakyThrows
	private void writeEntry(TarArchiveOutputStream tar, String path, byte[] data, long modTime) {
		var entry = new TarArchiveEntry(path);
		entry.setSize(data.length);
		entry.setModTime(modTime);

		tar.putArchiveEntry(entry);
		tar.write(data);
		tar.closeArchiveEntry();
	}
}
