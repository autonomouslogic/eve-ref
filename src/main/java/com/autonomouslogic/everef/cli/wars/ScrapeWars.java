package com.autonomouslogic.everef.cli.wars;

import static com.autonomouslogic.everef.util.ArchivePathFactory.WARS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiRetryUtil;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Main orchestrator for wars scraping.
 * Loads state from wars-current.json, fetches wars and killmails from ESI,
 * builds incremental exports, and uploads them to S3.
 */
@Log4j2
public class ScrapeWars implements Command {
	@Inject
	@Named("data")
	protected S3AsyncClient dataS3Client;

	@Inject
	protected WarsApi warsApi;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected WarsFetcher warsFetcher;

	@Inject
	protected KillmailFetcher killmailFetcher;

	@Inject
	protected WarsFileBuilder fileBuilder;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected WarsStateLoader stateLoader;

	@Setter
	private ZonedDateTime scrapeTime;

	private Map<Long, JsonNode> warsMap = new ConcurrentHashMap<>();

	private S3Url dataUrl;

	@Inject
	protected ScrapeWars() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	@SneakyThrows
	@Override
	public void run() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
		}

		// Load state from wars-current.json
		warsMap = stateLoader.loadState();

		try {
			var scope = calculateFetchScope();
			fetchWars(scope);

			// Fetch killmails and build export
			var killmailsByWar = fetchKillmailsForExport(scope.getWarIds());

			// Build and upload exports
			buildAndUploadExports(killmailsByWar);

			// Update last_killmail_id for each war
			updateWarKillmailIds(killmailsByWar);

			// Upload updated wars-current.json
			uploadWarsCurrentJson();

			// Cleanup old data
			cleanupOldData();
		} finally {
			killmailFetcher.clearCache();
		}
	}

	private WarsFetchScope calculateFetchScope() {
		log.info("Calculating fetch scope");
		return WarsFetchScope.calculate(warsApi, esiHelper, warsMap);
	}

	private void fetchWars(WarsFetchScope scope) {
		log.info("Fetching {} wars", scope.getWarIds().size());
		warsFetcher.setWarsMap(warsMap);
		warsFetcher.fetchWars(scope.getWarIds());
	}

	private Map<Long, Long> fetchKillmailsForExport(Set<Long> warIds) {
		log.info("Fetching killmails for {} wars", warIds.size());

		Map<Long, Long> maxKillmailIds = new HashMap<>();

		for (Long warId : warIds) {
			var war = warsMap.get(warId);
			if (war == null) {
				continue;
			}

			long lastKillmailId =
					war.has("last_killmail_id") ? war.get("last_killmail_id").asLong(0L) : 0L;

			try {
				// Fetch killmail list from ESI
				var killmailList = esiHelper.fetchPages(
						page -> warsApi.getWarsWarIdKillmailsWithHttpInfo(Math.toIntExact(warId), null, null, page));

				log.debug("Found {} killmails for war {}", killmailList.size(), warId);

				long maxSuccessfulId = lastKillmailId;
				for (var km : killmailList) {
					long kmId = km.getKillmailId();
					if (kmId > lastKillmailId) {
						// This is a new killmail, fetch details
						try {
							fetchAndStoreKillmailDetail(warId, kmId, km.getKillmailHash());
							// Only update maxSuccessfulId if fetch succeeded
							maxSuccessfulId = Math.max(maxSuccessfulId, kmId);
						} catch (Exception e) {
							log.error("Failed to fetch killmail {} for war {}: {}", kmId, warId, e.getMessage());
							// Don't advance maxSuccessfulId - this killmail will be retried next run
						}
					}
				}

				if (maxSuccessfulId > lastKillmailId) {
					maxKillmailIds.put(warId, maxSuccessfulId);
				}
			} catch (Exception e) {
				log.error("Failed to fetch killmails for war {}: {}", warId, e.getMessage());
			}
		}

		return maxKillmailIds;
	}

	private void fetchAndStoreKillmailDetail(long warId, long killmailId, String hash) throws ApiException {
		// Use shared retry logic and fetch the killmail detail
		EsiRetryUtil.fetchWithRetry(
				"killmail " + killmailId,
				() -> {
					killmailFetcher.fetchKillmailDetail(warId, killmailId, hash);
					return null;
				},
				12,
				java.time.Duration.ofSeconds(5));
	}

	@SneakyThrows
	private void buildAndUploadExports(Map<Long, Long> maxKillmailIds) {
		log.info("Building and uploading exports");

		// Build incremental TAR.BZ2 archive with newly fetched killmails
		fileBuilder.setWarsMap(warsMap);
		var incrementalFile = fileBuilder.buildIncrementalExport(killmailFetcher.getAllCachedKillmails());
		try {
			s3Util.uploadLatestAndArchive(
					incrementalFile, dataUrl, WARS, scrapeTime, "application/x-bzip2", dataS3Client);
		} finally {
			incrementalFile.delete();
		}
	}

	@SneakyThrows
	private void uploadWarsCurrentJson() {
		log.info("Uploading wars-current.json");
		fileBuilder.setWarsMap(warsMap);
		var currentWarsFile = fileBuilder.buildCurrentWarsJson();

		try {
			var putRequest = s3Util.putPublicObjectRequest(
					currentWarsFile.length(),
					dataUrl.resolve("wars/wars-current.json"),
					"application/json",
					Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired());

			s3Adapter.putObject(putRequest, currentWarsFile, dataS3Client);
		} finally {
			currentWarsFile.delete();
		}
	}

	private void updateWarKillmailIds(Map<Long, Long> maxKillmailIds) {
		log.info("Updating killmail IDs for {} wars", maxKillmailIds.size());
		for (var entry : maxKillmailIds.entrySet()) {
			var warId = entry.getKey();
			var maxKillmailId = entry.getValue();
			var war = (ObjectNode) warsMap.get(warId);
			if (war != null) {
				war.put("last_killmail_id", maxKillmailId);
			}
		}
	}

	private void cleanupOldData() {
		log.info("Cleaning up old wars data");
		var retention = Configs.WARS_DATA_RETENTION.getRequired();
		var cutoff = scrapeTime.toInstant().minus(retention);

		// Remove old finished wars from warsMap
		var toRemove = warsMap.entrySet().stream()
				.filter(entry -> {
					var war = entry.getValue();
					// Skip unfinished wars
					if (!war.has("finished") || war.get("finished").isNull()) {
						return false;
					}
					try {
						var finishedTime = ZonedDateTime.parse(
										war.get("finished").asText())
								.toInstant();
						return finishedTime.isBefore(cutoff);
					} catch (Exception e) {
						log.warn("Failed to parse finished time for war {}: {}", entry.getKey(), e.getMessage());
						return false;
					}
				})
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		log.info("Removing {} old finished wars", toRemove.size());
		toRemove.forEach(warsMap::remove);
	}
}
