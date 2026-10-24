package com.autonomouslogic.everef.cli.marketorders;

import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.MARKET_ORDERS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import tools.jackson.databind.JsonNode;

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

	@Inject
	protected MVStoreUtil mvStoreUtil;

	private S3Url dataUrl;
	private MVStore mvStore;
	private Map<Long, JsonNode> marketOrdersStore;
	private Map<Long, OrderSortKey> sortKeysStore;

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
		try {
			initScrapeTime();
			initStore();
			initLogin();
			fetchOrders();
			var file = writeOrders();
			uploadFile(file);
		} finally {
			closeMvStore();
		}
	}

	private void initScrapeTime() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
		}
	}

	private void initStore() {
		log.debug("Opening MVStore");
		mvStore = mvStoreUtil.createTempStore("market-orders");
		marketOrdersStore = mvStoreUtil.openJsonMap(mvStore, "market_orders", Long.class);
		sortKeysStore = new ConcurrentHashMap<>();
		marketOrderFetcher.setMarketOrdersStore(marketOrdersStore);
		marketOrderFetcher.setSortKeysStore(sortKeysStore);
		marketOrdersWriter.setMarketOrdersStore(marketOrdersStore);
		marketOrdersWriter.setSortKeysStore(sortKeysStore);
	}

	private void closeMvStore() {
		try {
			mvStore.close();
		} catch (Exception e) {
			log.debug("Failed closing MVStore, ignoring");
		}
	}

	private void initLogin() {
		esiAuthHelper.getTokenStringForOwnerHash(scrapeOwnerHash);
	}

	private void fetchOrders() {
		log.info("Fetching market orders");
		marketOrderFetcher.fetchMarketOrders();
	}

	private File writeOrders() {
		sortKeysStore.entrySet().stream()
				.filter(e -> e.getValue().regionId() == null)
				.forEach(e -> log.info(marketOrdersStore.get(e.getKey())));

		log.info("Writing market orders");
		return marketOrdersWriter.writeOrders();
	}

	@SneakyThrows
	private void uploadFile(File outputFile) {
		log.debug(String.format("Uploading completed file from %s", outputFile));
		s3Util.uploadLatestAndArchive(outputFile, dataUrl, MARKET_ORDERS, scrapeTime, "application/x-bzip2", s3Client);
	}
}
