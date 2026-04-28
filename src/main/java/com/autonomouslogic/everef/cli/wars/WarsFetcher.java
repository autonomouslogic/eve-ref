package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiRetryUtil;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Fetches war details from ESI and stores them in the database.
 */
@Log4j2
public class WarsFetcher {
	// Bad war IDs to skip
	private static final Set<Long> BAD_WARS = Set.of(90591L, 473095L);
	private static final long BAD_WARS_RANGE_START = 472167L;
	private static final long BAD_WARS_RANGE_END = 473147L;

	@Inject
	protected WarsApi warsApi;

	@Inject
	protected ObjectMapper objectMapper;

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
			EsiRetryUtil.fetchWithRetry(
					"war " + warId,
					() -> {
						try {
							var response = warsApi.getWarsWarIdWithHttpInfo(Math.toIntExact(warId), null, null);
							var war = response.getData();
							var warNode = objectMapper.valueToTree(war);

							// Capture Last-Modified header if present
							var httpHeaders = response.getHeaders();
							if (httpHeaders != null && httpHeaders.containsKey("Last-Modified")) {
								var lastModified = httpHeaders.get("Last-Modified");
								if (!lastModified.isEmpty()) {
									((com.fasterxml.jackson.databind.node.ObjectNode) warNode)
											.put("http_last_modified", lastModified.get(0));
								}
							}

							warsMap.put(warId, warNode);
							log.debug("Fetched war {}", warId);
						} catch (ApiException e) {
							if (e.getCode() == 404 || e.getCode() == 403 || e.getCode() == 400) {
								log.debug("War {} not found or inaccessible ({})", warId, e.getCode());
							} else if (e.getCode() == 422) {
								log.debug("War {} returned 422 (unprocessable entity)", warId);
							} else {
								throw new RuntimeException(e);
							}
						}
						return null;
					},
					12,
					Duration.ofSeconds(5));
		} catch (ApiException e) {
			log.error("Failed to fetch war {} after retries: {}", warId, e.getMessage());
		}
	}
}
