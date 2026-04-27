package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.KillmailsApi;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Fetches killmail details from ESI with hash correction via Zkillboard fallback.
 */
@Log4j2
public class KillmailFetcher {
	// Early wars that have killmails in the legacy data
	private static final Set<Long> EARLY_WARS_WITH_KILLMAILS = Set.of(48074L, 138678L, 144630L, 149785L);
	private static final long EARLY_WARS_CUTOFF = 149786L;

	@Inject
	protected WarsApi warsApi;

	@Inject
	protected KillmailsApi killmailsApi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ZkillboardHashCorrector zkillboardHashCorrector;

	@Setter
	private Map<Long, JsonNode> killmailsMap;

	@Setter
	private Map<Long, Boolean> pendingKillmailsMap;

	@Inject
	protected KillmailFetcher() {}

	public void fetchKillmails(Set<Long> warIds) {
		log.info("Fetching killmails for {} wars", warIds.size());

		// Process wars in parallel with lower concurrency (2 wars at a time)
		var warsToFetch = warIds.stream().filter(this::shouldFetchKillmails).toList();

		var tasks = warsToFetch.stream()
				.map(warId -> (Callable<Void>) () -> {
					fetchKillmailsForWar(warId);
					return null;
				})
				.toList();

		VirtualThreads.parallel(tasks, Configs.KILLMAIL_LIST_CONCURRENCY.getRequired());
	}

	private boolean shouldFetchKillmails(long warId) {
		// Normal wars >= 149786 should have killmails
		if (warId >= EARLY_WARS_CUTOFF) {
			return true;
		}
		// Only specific early wars have killmails
		return EARLY_WARS_WITH_KILLMAILS.contains(warId);
	}

	private void fetchKillmailsForWar(long warId) {
		try {
			log.debug("Fetching killmail list for war {}", warId);

			// Fetch the list of killmails for this war
			var killmailList = esiHelper.fetchPages(
					page -> warsApi.getWarsWarIdKillmailsWithHttpInfo(Math.toIntExact(warId), null, null, page));
			var killmailIds =
					killmailList.stream().map(km -> (long) km.getKillmailId()).toList();

			log.debug("Found {} killmails for war {}", killmailIds.size(), warId);

			// Process killmail details in parallel (4 at a time)
			var tasks = killmailIds.stream()
					.map(kmId -> (Callable<Void>) () -> {
						fetchKillmailDetail(warId, kmId);
						return null;
					})
					.toList();

			VirtualThreads.parallel(tasks, Configs.KILLMAIL_DETAIL_CONCURRENCY.getRequired());
		} catch (Exception e) {
			if (e instanceof ApiException && ((ApiException) e).getCode() == 404) {
				log.debug("War {} has no killmails", warId);
			} else {
				log.error("Failed to fetch killmails for war {}: {}", warId, e.getMessage());
			}
		}
	}

	private void fetchKillmailDetail(long warId, long killmailId) {
		// Skip if already fetched
		if (killmailsMap.containsKey(killmailId)) {
			return;
		}

		fetchWithRetry(
				"killmail " + killmailId,
				() -> {
					try {
						// First, get the killmail hash from the war killmails endpoint
						var kmResponse =
								warsApi.getWarsWarIdKillmailsWithHttpInfo(Math.toIntExact(warId), null, null, 1);
						String hash = null;
						for (var km : kmResponse.getData()) {
							if (km.getKillmailId() == killmailId) {
								hash = km.getKillmailHash();
								break;
							}
						}

						if (hash == null) {
							log.debug("Could not find hash for killmail {}", killmailId);
							return null;
						}

						try {
							var kmDetail = killmailsApi.getKillmailsKillmailIdKillmailHash(
									hash, Math.toIntExact(killmailId), null, null);
							var kmNode = objectMapper.valueToTree(kmDetail);

							// Add war_id and hash to the killmail
							((ObjectNode) kmNode).put("war_id", warId);
							((ObjectNode) kmNode).put("killmail_hash", hash);

							killmailsMap.put(killmailId, kmNode);
							pendingKillmailsMap.put(killmailId, true);
							log.debug("Fetched killmail {} for war {}", killmailId, warId);
						} catch (ApiException e) {
							if (e.getCode() == 422) {
								// Try to correct the hash
								var corrected = zkillboardHashCorrector.correctHash(killmailId, hash);
								if (corrected.isPresent()) {
									var correctedHash = corrected.get();
									log.debug("Retrying killmail {} with corrected hash", killmailId);
									var kmDetail = killmailsApi.getKillmailsKillmailIdKillmailHash(
											correctedHash, Math.toIntExact(killmailId), null, null);
									var kmNode = objectMapper.valueToTree(kmDetail);
									((ObjectNode) kmNode).put("war_id", warId);
									((ObjectNode) kmNode).put("killmail_hash", correctedHash);
									killmailsMap.put(killmailId, kmNode);
									pendingKillmailsMap.put(killmailId, true);
									log.debug("Fetched killmail {} with corrected hash", killmailId);
								} else {
									log.debug("Could not correct hash for killmail {}", killmailId);
								}
							} else {
								throw e;
							}
						}
					} catch (ApiException e) {
						if (e.getCode() == 404) {
							log.debug("Killmail {} not found", killmailId);
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
