package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.util.CompressUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

	/**
	 * Build an incremental export TAR archive containing wars and killmails.
	 *
	 * @param killmailsMap map of killmail ID to killmail data
	 * @return the compressed TAR.BZ2 file
	 */
	@SneakyThrows
	public File buildIncrementalExport(Map<Long, JsonNode> killmailsMap) {
		var warsToExport = collectWarsToExport(killmailsMap);
		var killmailsByWar = collectKillmailsToExport(warsToExport, killmailsMap);

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

	private Set<Long> collectWarsToExport(Map<Long, JsonNode> killmailsMap) {
		var warsToExport = new TreeSet<Long>();

		for (var entry : killmailsMap.entrySet()) {
			var km = entry.getValue();
			var warId = km.get("war_id");
			if (warId != null) {
				warsToExport.add(warId.asLong());
			}
		}

		return warsToExport;
	}

	private Map<Long, List<Long>> collectKillmailsToExport(Set<Long> warsToExport, Map<Long, JsonNode> killmailsMap) {
		var killmailsByWar = new java.util.TreeMap<Long, List<Long>>();

		for (var entry : killmailsMap.entrySet()) {
			var kmId = entry.getKey();
			var km = entry.getValue();
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

		// Sort killmails by ID within each war
		killmailsByWar.forEach((k, v) -> v.sort(Long::compareTo));

		return killmailsByWar;
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
