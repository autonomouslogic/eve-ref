package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.mvstore.JsonNodeDataType;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.S3Util;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Scrapes market history and stores it in daily files.
 */
@Log4j2
public class ScrapeMarketHistory implements Command {
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
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected Provider<DataCrawler> dataCrawlerProvider;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	private S3Url dataUrl;
	private MVStore mvStore;
	private MVMap<Long, JsonNode> marketOrdersStore;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected ScrapeMarketHistory() {}

	@Inject
	protected void init() {
		dataUrl = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	@SneakyThrows
	@Override
	public Completable run() {
		return Completable.concatArray(
						initMvStore(), resolveRegionTypePair().toList().flatMapCompletable(pairs -> {
							return Completable.complete();
						}))
				.doFinally(this::closeMvStore);
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			mvStore = mvStoreUtil.createTempStore("market-history");
			marketOrdersStore = mvStoreUtil.openJsonMap(mvStore, "market-orders", Long.class);
		});
	}

	private void closeMvStore() {
		try {
			mvStore.close();
		} catch (Exception e) {
			log.debug("Failed closing MVStore, ignoring");
		}
	}

	private Flowable<RegionTypePair> resolveRegionTypePair() {
		return Flowable.defer(() -> {
			return dataCrawlerProvider
					.get()
					.setPrefix("/" + ArchivePathFactory.MARKET_HISTORY.getFolder() + "/")
					.crawl()
					.flatMap(url -> {
						var time = ArchivePathFactory.MARKET_HISTORY.parseArchiveTime(url.getPath());
						if (time == null) {
							return Flowable.empty();
						}
						log.info(time);
						return Flowable.empty();
					});
		});
	}

	@Value
	private static class RegionTypePair {
		long regionId;
		long typeId;
	}
}
