package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiRetryUtil;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;

/**
 * Fetches killmail details from ESI.
 */
@Log4j2
public class KillmailFetcher {
	/**
	 * First war with killmails is 48074, with one killmail.
	 * After that it's 138678, also with one killmail
	 * After that it's 144630, with two killmails.
	 * After that it's 149785, with quite a few killmails.
	 */
	private static final List<Long> KILLMAIL_INITIAL_WARS = List.of(48074L, 138678L, 144630L, 149785L);

	private static final long KILLMAIL_FULL_CHECK = KILLMAIL_INITIAL_WARS.get(KILLMAIL_INITIAL_WARS.size() - 1) + 1;

	@Inject
	protected WarsApi warsApi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	@Named("esi")
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

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
		if (warId >= KILLMAIL_FULL_CHECK) {
			return true;
		}
		// Only specific early wars have killmails
		return KILLMAIL_INITIAL_WARS.contains(warId);
	}

	private void fetchKillmailsForWar(long warId) {
		try {
			log.debug("Fetching killmail list for war {}", warId);

			// Fetch the list of killmails for this war
			var killmailList = esiHelper.fetchPages(
					page -> warsApi.getWarsWarIdKillmailsWithHttpInfo(Math.toIntExact(warId), null, null, page));

			log.debug("Found {} killmails for war {}", killmailList.size(), warId);

			// Process killmail details in parallel
			var tasks = killmailList.stream()
					.map(km -> (Callable<Void>) () -> {
						fetchKillmailDetail(warId, km.getKillmailId(), km.getKillmailHash());
						return null;
					})
					.toList();

			VirtualThreads.parallel(tasks, Configs.KILLMAIL_CONCURRENCY.getRequired());
		} catch (Exception e) {
			if (e instanceof ApiException && ((ApiException) e).getCode() == 404) {
				log.debug("War {} has no killmails", warId);
			} else {
				log.error("Failed to fetch killmails for war {}: {}", warId, e.getMessage());
			}
		}
	}

	/**
	 * Fetches and caches a killmail detail.
	 *
	 * @throws KillmailNotFoundException if killmail doesn't exist (404)
	 */
	public void fetchKillmailDetail(long warId, long killmailId, String hash) throws ApiException {
		// Only fetch if not already cached
		if (!killmailsCache.containsKey(killmailId)) {
			var killmail = fetchKillmailWithRetry(warId, killmailId, hash);
			if (killmail != null) {
				killmailsCache.put(killmailId, killmail);
			}
		}
	}

	private JsonNode fetchKillmailWithRetry(long warId, long killmailId, String hash) throws ApiException {
		return EsiRetryUtil.fetchWithRetry(
				"killmail " + killmailId,
				() -> {
					try {
						var url = String.format(
								"%slatest/killmails/%d/%s/", Configs.ESI_BASE_URL.getRequired(), killmailId, hash);

						try (var response = okHttpWrapper.get(url)) {
							var code = response.code();

							if (code == 404) {
								log.debug("Killmail {} not found", killmailId);
								throw new KillmailNotFoundException(killmailId);
							}

							if (code == 422) {
								var msg = "Killmail " + killmailId + " has invalid hash, unable to fetch";
								log.warn(msg);
								Sentry.captureException(
										new RuntimeException(msg), scope -> scope.setLevel(SentryLevel.WARNING));
								return null;
							}

							if (code != 200) {
								throw new RuntimeException("HTTP " + code);
							}

							var body = response.body();
							if (body == null) {
								throw new RuntimeException("Empty response body");
							}

							var kmNode = objectMapper.readTree(body.string());

							((ObjectNode) kmNode).put("war_id", warId);
							((ObjectNode) kmNode).put("killmail_hash", hash);

							okHttpWrapper.getLastModified(response).ifPresent(date -> ((ObjectNode) kmNode)
									.put("http_last_modified", date.toInstant().toString()));

							log.debug("Fetched killmail {} for war {}", killmailId, warId);
							return kmNode;
						}
					} catch (KillmailNotFoundException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				},
				12,
				Duration.ofSeconds(5));
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
	 * Get all cached killmails.
	 *
	 * @return map of killmail ID to killmail data
	 */
	public Map<Long, JsonNode> getAllCachedKillmails() {
		return new HashMap<>(killmailsCache);
	}
}
