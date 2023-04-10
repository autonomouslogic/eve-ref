package com.autonomouslogic.everef.cli.marketorders;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_ORDERS;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.mvstore.JsonNodeDataType;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.ObjectDataType;
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
	protected JsonNodeDataType jsonNodeDataType;

	@Inject
	protected MarketOrderFetcher marketOrderFetcher;

	@Inject
	protected MarketOrdersWriter marketOrdersWriter;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected TempFiles tempFiles;

	private S3Url dataUrl;
	private MVStore mvStore;
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Setter
	private ZonedDateTime scrapeTime;

	private final Duration latestCacheTime = Configs.MARKET_ORDERS_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.MARKET_ORDERS_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected ScrapeMarketOrders() {}

	@Inject
	protected void init() {
		var dataPathUrl = urlParser.parse(Configs.DATA_PATH.getRequired());
		if (!dataPathUrl.getProtocol().equals("s3")) {
			throw new IllegalArgumentException("Data path must be an S3 path");
		}
		dataUrl = (S3Url) dataPathUrl;
		if (!dataUrl.getPath().equals("")) {
			throw new IllegalArgumentException("Data index must be run at the root of the bucket");
		}
	}

	@SneakyThrows
	@Override
	public Completable run() {
		return Completable.concatArray(
						initScrapeTime(),
						initMvStore(),
						fetchOrders(),
						writeOrders().flatMapCompletable(this::uploadFile))
				.doFinally(this::closeMvStore);
	}

	private Completable initScrapeTime() {
		return Completable.fromAction(() -> {
			if (scrapeTime == null) {
				scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
			}
		});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			mvStore = mvStoreUtil.createTempStore("market-orders");
			marketOrdersStore = mvStore.openMap(
					"market-orders",
					new MVMap.Builder<Long, JsonNode>()
							.keyType(new ObjectDataType())
							.valueType(jsonNodeDataType));
			marketOrderFetcher.setMarketOrdersStore(marketOrdersStore);
			marketOrdersWriter.setMarketOrdersStore(marketOrdersStore);
		});
	}

	private void closeMvStore() {
		try {
			mvStore.close();
		} catch (Exception e) {
			log.debug("Failed closing MVStore, ignoring");
		}
	}

	private Completable fetchOrders() {
		return Completable.defer(() -> {
			log.info("Fetching market orders");
			return marketOrderFetcher.fetchMarketOrders();
		});
	}

	private Single<File> writeOrders() {
		return Single.defer(() -> {
			log.info("Writing market orders");
			return marketOrdersWriter.writeOrders();
		});
	}

	private Completable uploadFile(File outputFile) {
		return Completable.defer(() -> {
			log.debug(String.format("Uploading completed file from %s", outputFile));
			var latestPath = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path(dataUrl.getPath() + MARKET_ORDERS.createLatestPath())
					.build();
			var archivePath = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path(dataUrl.getPath() + MARKET_ORDERS.createArchivePath(scrapeTime))
					.build();
			var latestPut = s3Util.putPublicObjectRequest(
					outputFile.length(), latestPath, "application/x-bzip2", latestCacheTime);
			var archivePut = s3Util.putPublicObjectRequest(
					outputFile.length(), archivePath, "application/x-bzip2", archiveCacheTime);
			log.info(String.format("Uploading latest file to %s", latestPath));
			log.info(String.format("Uploading archive file to %s", archivePath));
			return Completable.mergeArray(
					s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
					s3Adapter.putObject(archivePut, outputFile, s3Client).ignoreElement());
		});
	}
}
