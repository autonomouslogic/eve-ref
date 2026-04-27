package com.autonomouslogic.everef.cli.wars;

import static com.autonomouslogic.everef.util.ArchivePathFactory.WARS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.KillmailsApi;
import com.autonomouslogic.everef.openapi.esi.api.WarsApi;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Main orchestrator for wars scraping.
 * Manages the MVStore database, fetches wars and killmails from ESI,
 * builds incremental exports, and uploads them to S3.
 */
@Log4j2
public class ScrapeWars implements Command {
	@Inject
	@Named("data")
	protected S3AsyncClient dataS3Client;

	@Inject
	@Named("wars-db")
	protected S3AsyncClient warsDbS3Client;

	@Inject
	protected WarsApi warsApi;

	@Inject
	protected KillmailsApi killmailsApi;

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

	@Setter
	private ZonedDateTime scrapeTime;

	private MVStore mvStore;
	private MVMap<Long, JsonNode> warsMap;
	private MVMap<Long, JsonNode> killmailsMap;
	private MVMap<Long, Boolean> pendingKillmailsMap;
	private MVMap<String, String> metaMap;
	private String mvStoreFileName;

	private S3Url dataUrl;
	private S3Url warsDbUrl;

	@Inject
	protected ScrapeWars() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		var warsDbPath = Configs.WARS_DB_PATH.getRequired();
		warsDbUrl = (S3Url) urlParser.parse(warsDbPath);
	}

	@SneakyThrows
	@Override
	public void run() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
		}

		downloadDatabase();
		initializeMaps();

		try {
			var scope = calculateFetchScope();
			fetchWars(scope);
			fetchKillmails(scope);
			buildAndUploadExports();
			updateMetadata();
			cleanupOldData();
		} finally {
			compactAndClose();
			uploadDatabase();
		}
	}

	@SneakyThrows
	private void downloadDatabase() {
		log.info("Downloading wars database from S3");
		try {
			var dbFile = File.createTempFile("wars", ".mvstore");
			dbFile.deleteOnExit();

			// Download the database file
			var request = s3Util.getObjectRequest(warsDbUrl);

			try {
				s3Adapter.getObject(request, dbFile.toPath(), warsDbS3Client);
				openMVStore(dbFile);
			} catch (Exception e) {
				log.info("Database does not exist on S3, creating new one");
				createNewMVStore();
			}
		} catch (Exception e) {
			log.error("Failed to download database", e);
			throw e;
		}
	}

	@SneakyThrows
	private void openMVStore(File dbFile) {
		mvStoreFileName = dbFile.getAbsolutePath();
		mvStore = new MVStore.Builder().fileName(mvStoreFileName).open();
		log.info("Opened existing MVStore database");
	}

	private void createNewMVStore() {
		var tempFile = new File(System.getProperty("java.io.tmpdir"), "wars-" + System.nanoTime() + ".mvstore");
		mvStoreFileName = tempFile.getAbsolutePath();
		mvStore = new MVStore.Builder().fileName(mvStoreFileName).open();
		log.info("Created new MVStore database");
	}

	private void initializeMaps() {
		warsMap = mvStore.openMap("wars");
		killmailsMap = mvStore.openMap("kill_mails");
		pendingKillmailsMap = mvStore.openMap("pending_killmails");
		metaMap = mvStore.openMap("meta");

		log.info("Initialized MVStore maps");
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

	private void fetchKillmails(WarsFetchScope scope) {
		log.info("Fetching killmails for {} wars", scope.getWarIds().size());
		killmailFetcher.setKillmailsMap(killmailsMap);
		killmailFetcher.setPendingKillmailsMap(pendingKillmailsMap);
		killmailFetcher.fetchKillmails(scope.getWarIds());
	}

	@SneakyThrows
	private void buildAndUploadExports() {
		log.info("Building and uploading exports");

		var lastExportStr = metaMap.get("last_export");
		var lastExportTime = lastExportStr != null ? Instant.parse(lastExportStr) : Instant.EPOCH;

		// Build incremental TAR.BZ2 archive
		var incrementalFile = fileBuilder.buildIncrementalExport(lastExportTime);
		s3Util.uploadLatestAndArchive(incrementalFile, dataUrl, WARS, scrapeTime, "application/x-bzip2", dataS3Client);

		// Build and upload current wars JSON
		var currentWarsFile = fileBuilder.buildCurrentWarsJson();
		var currentPath = dataUrl.resolve("wars/wars-current.json");
		var currentRequest = s3Util.putPublicObjectRequest(
				currentWarsFile.length(),
				currentPath,
				"application/json",
				Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired());

		s3Adapter.putObject(currentRequest, currentWarsFile, dataS3Client);

		// Cleanup temp files
		incrementalFile.delete();
		currentWarsFile.delete();
	}

	private void updateMetadata() {
		log.info("Updating metadata");
		metaMap.put("last_export", scrapeTime.toInstant().toString());
		pendingKillmailsMap.clear();
	}

	private void cleanupOldData() {
		log.info("Cleaning up old wars data");
		var retention = Configs.WARS_DATA_RETENTION.getRequired();
		var cutoff = scrapeTime.toInstant().minus(retention);

		// Find wars to delete
		var warsToDelete = new java.util.ArrayList<Long>();
		for (var entry : warsMap.entrySet()) {
			if (isWarBeforeCutoff(entry.getValue(), cutoff)) {
				warsToDelete.add(entry.getKey());
			}
		}

		log.info("Deleting {} old wars", warsToDelete.size());
		for (var warId : warsToDelete) {
			warsMap.remove(warId);
		}

		// Delete orphaned killmails
		var killmailsToDelete = new java.util.ArrayList<Long>();
		for (var entry : killmailsMap.entrySet()) {
			var km = entry.getValue();
			var warId = km.get("war_id");
			if (warId != null && !warsMap.containsKey(warId.asLong())) {
				// Only delete if not pending
				if (!pendingKillmailsMap.containsKey(entry.getKey())) {
					killmailsToDelete.add(entry.getKey());
				}
			}
		}

		log.info("Deleting {} orphaned killmails", killmailsToDelete.size());
		for (var kmId : killmailsToDelete) {
			killmailsMap.remove(kmId);
		}
	}

	private void compactAndClose() {
		if (mvStore == null) {
			return;
		}

		log.info("Compacting and closing MVStore");
		mvStore.commit();
		// Compact step - MVStore uses internal compaction mechanisms
		mvStore.close();
	}

	@SneakyThrows
	private void uploadDatabase() {
		if (mvStore != null && mvStoreFileName != null) {
			log.info("Uploading wars database to S3");
			var dbFile = new File(mvStoreFileName);
			if (dbFile.exists()) {
				var request = s3Util.putObjectRequest(dbFile.length(), warsDbUrl, "application/octet-stream");

				s3Adapter.putObject(request, dbFile, warsDbS3Client);

				log.info("Uploaded wars database");
			}
		}
	}

	private boolean isWarBeforeCutoff(JsonNode war, Instant cutoff) {
		// Check all timestamp fields
		var timestamps = new String[] {"declared", "started", "retracted", "finished"};
		var hasAnyTimestamp = false;

		for (var field : timestamps) {
			var timestamp = war.get(field);
			if (timestamp != null && !timestamp.isNull()) {
				hasAnyTimestamp = true;
				try {
					var instant = Instant.parse(timestamp.asText());
					if (instant.isBefore(cutoff)) {
						continue;
					} else {
						// At least one timestamp is after cutoff
						return false;
					}
				} catch (Exception e) {
					log.warn("Failed to parse timestamp {}: {}", field, timestamp);
				}
			}
		}

		// All timestamps are before cutoff (or all are missing)
		return hasAnyTimestamp;
	}
}
