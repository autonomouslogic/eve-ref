package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
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
		fetchWithRetry(
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
	}

	/**
	 * Retry a supplier with exponential backoff.
	 *
	 * @param description description of what's being retried
	 * @param supplier the supplier to retry
	 * @param maxRetries maximum number of retries
	 * @param delay delay between retries
	 * @return the result of the supplier
	 */
	protected <T> T fetchWithRetry(String description, Supplier<T> supplier, int maxRetries, Duration delay) {
		int attempts = 0;
		while (true) {
			try {
				attempts++;
				return supplier.get();
			} catch (Exception e) {
				if (e instanceof ApiException) {
					var apiException = (ApiException) e;
					if (apiException.getCode() >= 500 || apiException.getCode() == 429) {
						if (attempts >= maxRetries) {
							log.error("Max retries exceeded for {}", description);
							throw new RuntimeException(e);
						}
						log.debug(
								"Retry {}/{} for {} (code: {})",
								attempts,
								maxRetries,
								description,
								apiException.getCode());
						try {
							Thread.sleep(delay.toMillis());
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							throw new RuntimeException(ie);
						}
					} else {
						throw new RuntimeException(e);
					}
				} else {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
