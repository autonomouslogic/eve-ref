package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WarsFetcher {
	private static final Set<Long> BAD_WARS = Set.of(90591L, 473095L);
	private static final long BAD_WARS_RANGE_START = 472167L;
	private static final long BAD_WARS_RANGE_END = 473147L;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Setter
	private Map<Long, JsonNode> warsMap;

	@Inject
	protected WarsFetcher() {}

	public void fetchWars(Set<Long> warIds) {
		log.info("Fetching {} wars", warIds.size());

		var tasks = warIds.stream()
				.filter(this::shouldFetchWar)
				.map(warId -> (Callable<Void>) () -> {
					fetchWar(warId);
					return null;
				})
				.toList();

		VirtualThreads.parallel(tasks, Configs.WARS_FETCH_CONCURRENCY.getRequired());
	}

	private boolean shouldFetchWar(long warId) {
		if (BAD_WARS.contains(warId)) {
			log.debug("Skipping bad war {}", warId);
			return false;
		}
		if (warId >= BAD_WARS_RANGE_START && warId <= BAD_WARS_RANGE_END) {
			log.debug("Skipping bad war in range {}", warId);
			return false;
		}
		return true;
	}

	private void fetchWar(long warId) {
		try {
			var url = String.format("%swars/%d/", Configs.ESI_BASE_URL.getRequired(), warId);

			try (var response = okHttpWrapper.get(url)) {
				var code = response.code();

				if (code == 404 || code == 403 || code == 400) {
					log.debug("War {} not found or inaccessible ({})", warId, code);
					return;
				}

				if (code == 422) {
					log.debug("War {} returned 422 (unprocessable entity)", warId);
					return;
				}

				if (code != 200) {
					log.warn("Failed to fetch war {}: HTTP {}", warId, code);
					return;
				}

				var body = response.body();
				if (body == null) {
					log.warn("Empty response body for war {}", warId);
					return;
				}

				var warNode = objectMapper.readTree(body.string());

				okHttpWrapper
						.getLastModified(response)
						.ifPresent(date -> ((com.fasterxml.jackson.databind.node.ObjectNode) warNode)
								.put("http_last_modified", date.toInstant().toString()));

				// Preserve last_killmail_id from existing war if present
				var existingWar = warsMap.get(warId);
				if (existingWar != null && existingWar.has("last_killmail_id")) {
					((com.fasterxml.jackson.databind.node.ObjectNode) warNode)
							.set("last_killmail_id", existingWar.get("last_killmail_id"));
				}

				warsMap.put(warId, warNode);
				log.debug("Fetched war {}", warId);
			}
		} catch (Exception e) {
			log.error("Failed to fetch war {}: {}", warId, e.getMessage());
		}
	}
}
