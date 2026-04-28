package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiRetryUtil;
import com.autonomouslogic.everef.openapi.esi.api.KillmailsApi;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
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

	private Map<Long, JsonNode> killmailsCache = new ConcurrentHashMap<>();

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

			log.debug("Found {} killmails for war {}", killmailList.size(), warId);

			// Process killmail details in parallel (4 at a time)
			var tasks = killmailList.stream()
					.map(km -> (Callable<Void>) () -> {
						fetchKillmailDetail(warId, km.getKillmailId(), km.getKillmailHash());
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

	public void fetchKillmailDetail(long warId, long killmailId, String hash) {
		// Use computeIfAbsent for atomic check-and-fetch
		killmailsCache.computeIfAbsent(killmailId, id -> {
			try {
				return fetchKillmailWithRetry(warId, killmailId, hash);
			} catch (Exception e) {
				// Return null if fetch fails - entry won't be cached
				log.debug("Failed to fetch killmail {}: {}", killmailId, e.getMessage());
				return null;
			}
		});
	}

	private JsonNode fetchKillmailWithRetry(long warId, long killmailId, String hash) {
		try {
			return EsiRetryUtil.fetchWithRetry(
					"killmail " + killmailId,
					() -> {
						try {
							try {
								var kmDetail = killmailsApi.getKillmailsKillmailIdKillmailHash(
										hash, Math.toIntExact(killmailId), null, null);
								var kmNode = objectMapper.valueToTree(kmDetail);

								// Add war_id and hash to the killmail
								((ObjectNode) kmNode).put("war_id", warId);
								((ObjectNode) kmNode).put("killmail_hash", hash);

								log.debug("Fetched killmail {} for war {}", killmailId, warId);
								return kmNode;
							} catch (ApiException e) {
								if (e.getCode() == 422) {
									// Try to correct the hash via Zkillboard
									var corrected = zkillboardHashCorrector.correctHash(killmailId, hash);
									if (corrected.isPresent()) {
										var correctedHash = corrected.get();
										log.debug("Retrying killmail {} with corrected hash", killmailId);
										// Recursively call to get retry logic for corrected hash
										return fetchKillmailWithRetry(warId, killmailId, correctedHash);
									} else {
										log.debug("Could not correct hash for killmail {}", killmailId);
										return null;
									}
								} else {
									throw e;
								}
							}
						} catch (ApiException e) {
							if (e.getCode() == 404) {
								log.debug("Killmail {} not found", killmailId);
								return null;
							} else {
								throw new RuntimeException(e);
							}
						}
					},
					12,
					Duration.ofSeconds(5));
		} catch (ApiException e) {
			log.debug("Failed to fetch killmail {} after retries: {}", killmailId, e.getMessage());
			return null;
		}
	}

	/**
	 * Get a cached killmail if available.
	 *
	 * @param killmailId the killmail ID
	 * @return the killmail data if cached, empty otherwise
	 */
	public Optional<JsonNode> getKillmail(long killmailId) {
		return Optional.ofNullable(killmailsCache.get(killmailId));
	}

	/**
	 * Clear the killmail cache.
	 */
	public void clearCache() {
		killmailsCache.clear();
	}

	/**
	 * Get all cached killmails.
	 *
	 * @return map of killmail ID to killmail data
	 */
	public Map<Long, JsonNode> getAllCachedKillmails() {
		return new HashMap<>(killmailsCache);
	}
}
