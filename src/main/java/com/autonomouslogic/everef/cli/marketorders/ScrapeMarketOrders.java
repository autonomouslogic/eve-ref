package com.autonomouslogic.everef.cli.marketorders;

import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.MARKET_ORDERS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
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
	protected S3Util s3Util;

	@Inject
	protected MarketOrderFetcher marketOrderFetcher;

	@Inject
	protected MarketOrdersWriter marketOrdersWriter;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected EsiAuthHelper esiAuthHelper;

	private S3Url dataUrl;
	private Map<Long, JsonNode> marketOrdersStore;

	@Setter
	private ZonedDateTime scrapeTime;

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
		esiAuthHelper.getTokenStringForOwnerHash(scrapeOwnerHash);
	}

	private void fetchOrders() {
		log.info("Fetching market orders");
		marketOrderFetcher.fetchMarketOrders();
	}

	private File writeOrders() {
		marketOrdersStore.values().stream()
				.filter(order -> order.get("region_id") == null)
				.forEach(order -> log.info(order));

		log.info("Writing market orders");
		return marketOrdersWriter.writeOrders();
	}

	@SneakyThrows
	private void uploadFile(File outputFile) {
		log.debug(String.format("Uploading completed file from %s", outputFile));
		s3Util.uploadLatestAndArchive(outputFile, dataUrl, MARKET_ORDERS, scrapeTime, "application/x-bzip2", s3Client);
	}
}
