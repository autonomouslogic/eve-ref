package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Loads wars state from wars-current.json in S3.
 */
@Log4j2
public class WarsStateLoader {
	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected WarsStateLoader() {}

	/**
	 * Load wars state from wars-current.json.
	 *
	 * @return map of war ID to war data, or empty map if file doesn't exist
	 */
	public Map<Long, JsonNode> loadState() {
		var url = Configs.DATA_BASE_URL.getRequired().resolve("wars").resolve("wars-current.json").toString();
		var file = tempFiles.tempFile("wars-current", ".json").toFile();

		log.info("Downloading wars state from {}", url);

		try (var response = okHttpWrapper.download(url, file)) {
			if (response.code() == 404) {
				log.warn("No existing wars-current.json found, starting fresh");
				return new HashMap<>();
			}

			if (response.code() != 200) {
				throw new RuntimeException(
						String.format("Failed downloading wars-current.json: HTTP %d", response.code()));
			}

			// Parse JSON
			var root = objectMapper.readTree(file);

			// Extract wars (all root-level entries)
			Map<Long, JsonNode> warsMap = new HashMap<>();
			root.fields().forEachRemaining(entry -> {
				try {
					long warId = Long.parseLong(entry.getKey());
					warsMap.put(warId, entry.getValue());
				} catch (NumberFormatException e) {
					log.warn("Invalid war ID in state file: {}", entry.getKey());
				}
			});

			log.info("Loaded {} unfinished wars", warsMap.size());

			return warsMap;

		} catch (Exception e) {
			throw new RuntimeException("Failed loading wars state", e);
		} finally {
			file.delete();
		}
	}
}
