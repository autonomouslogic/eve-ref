package com.autonomouslogic.everef.cli.marketorders;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_ORDERS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Scraps all public market orders and uploads them.
 */
@Log4j2
public class ScrapeMarketOrders implements Command {
	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected MarketOrderFetcher marketOrderFetcher;

	@Inject
	protected MarketOrdersWriter marketOrdersWriter;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Inject
	protected EsiAuthHelper esiAuthHelper;

	private S3Url dataUrl;
	private Map<Long, JsonNode> marketOrdersStore;

	@Setter
	private ZonedDateTime scrapeTime;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final String scrapeOwnerHash = Configs.SCRAPE_CHARACTER_OWNER_HASH.getRequired();

	@Inject
	protected ScrapeMarketOrders() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	@SneakyThrows
	@Override
	public void run() {
		initScrapeTime();
		initStore();
		initLogin();
		fetchOrders();
		var file = writeOrders();
		uploadFile(file);
	}

	private void initScrapeTime() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
		}
	}

	private void initStore() {
		marketOrdersStore = new HashMap<>();
		marketOrderFetcher.setMarketOrdersStore(marketOrdersStore);
		marketOrdersWriter.setMarketOrdersStore(marketOrdersStore);
	}

	private void initLogin() {
		esiAuthHelper
				.getTokenStringForOwnerHash(scrapeOwnerHash)
				.ignoreElement()
				.blockingAwait();
	}

	private void fetchOrders() {
		log.info("Fetching market orders");
		marketOrderFetcher.fetchMarketOrders().blockingAwait();
	}

	private File writeOrders() {
		marketOrdersStore.values().stream()
				.filter(order -> order.get("region_id") == null)
				.forEach(order -> log.info(order));

		log.info("Writing market orders");
		return marketOrdersWriter.writeOrders().blockingGet();
	}

	private void uploadFile(File outputFile) {
		log.debug(String.format("Uploading completed file from %s", outputFile));
		var latestPath = dataUrl.resolve(MARKET_ORDERS.createLatestPath());
		var archivePath = dataUrl.resolve(MARKET_ORDERS.createArchivePath(scrapeTime));
		var latestPut =
				s3Util.putPublicObjectRequest(outputFile.length(), latestPath, "application/x-bzip2", latestCacheTime);
		var archivePut = s3Util.putPublicObjectRequest(
				outputFile.length(), archivePath, "application/x-bzip2", archiveCacheTime);
		log.info(String.format("Uploading latest file to %s", latestPath));
		log.info(String.format("Uploading archive file to %s", archivePath));
		Completable.mergeArray(
						s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
						s3Adapter.putObject(archivePut, outputFile, s3Client).ignoreElement())
				.blockingAwait();
		dataIndexHelper.updateIndex(latestPath, archivePath).blockingAwait();
	}
}
