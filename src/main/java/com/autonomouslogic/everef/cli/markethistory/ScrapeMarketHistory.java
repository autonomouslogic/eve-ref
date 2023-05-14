package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.mvstore.JsonNodeDataType;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.mvstore.StoreMapSet;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.S3Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
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
	protected Provider<StoreMapSet> storeMapSetProvider;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MarketHistoryFetcher marketHistoryFetcher;

	@Inject
	protected Provider<MarketHistoryLoader> marketHistoryLoaderProvider;

	@Inject
	protected Provider<CompoundRegionTypeSource> compoundRegionTypeSourceProvider;

	@Inject
	protected Provider<ActiveOrdersRegionTypeSource> activeOrdersRegionTypeSourceProvider;

	@Inject
	protected Provider<HistoryRegionTypeSource> historyRegionTypeSourceProvider;

	private LocalDate minDate = LocalDate.now(ZoneOffset.UTC).minus(Configs.ESI_MARKET_HISTORY_LOOKBACK.getRequired());
	private S3Url dataUrl;
	private MVStore mvStore;
	private StoreMapSet mapSet;
	private Map<LocalDate, Integer> historyEntries;
	private CompoundRegionTypeSource regionTypeSource;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final int esiConcurrency = Configs.ESI_MARKET_HISTORY_CONCURRENCY.getRequired();

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
						initSources(),
						initMvStore(),
						loadMarketHistory(),
						loadPairs()
								.flatMapCompletable(
										pair -> fetchMarketHistory(pair)
												.flatMapCompletable(entry -> saveMarketHistory(entry)),
										false,
										esiConcurrency))
				.doFinally(this::closeMvStore);
	}

	private Completable initSources() {
		return Completable.fromAction(() -> {
			regionTypeSource = compoundRegionTypeSourceProvider.get();
			regionTypeSource.addSource(activeOrdersRegionTypeSourceProvider.get());
			regionTypeSource.addSource(historyRegionTypeSourceProvider.get());
		});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			mvStore = mvStoreUtil.createTempStore("market-history");
			mapSet = storeMapSetProvider.get().setMvStore(mvStore);
			var date = minDate;
			var today = LocalDate.now(ZoneOffset.UTC);
			while (!date.isAfter(today)) {
				mapSet.getOrCreateMap(date.toString());
				date = date.plusDays(1);
			}
		});
	}

	private void closeMvStore() {
		try {
			mvStore.close();
		} catch (Exception e) {
			log.debug("Failed closing MVStore, ignoring");
		}
	}

	private Completable loadMarketHistory() {
		return Completable.defer(() -> {
			return marketHistoryLoaderProvider
					.get()
					.setMinDate(minDate)
					.load()
					.doOnNext(p -> {
						var entry = p.getRight();
						var id = RegionTypePair.fromHistory(entry).toString();
						mapSet.put(p.getLeft().toString(), id, entry);
						regionTypeSource.addHistory(entry);
					})
					.ignoreElements()
					.andThen(Completable.fromAction(() -> {
						historyEntries = new TreeMap<>();
						mapSet.forEachMap((date, map) -> {
							historyEntries.put(LocalDate.parse(date), map.size());
						});
					}));
		});
	}

	private Flowable<RegionTypePair> loadPairs() {
		return Flowable.defer(() -> {
			return regionTypeSource.sourcePairs().toList().flatMapPublisher(pairs -> {
				log.info("Sourced {} pairs", pairs.size());
				return Flowable.fromIterable(pairs);
			});
		});
	}

	private Flowable<JsonNode> fetchMarketHistory(RegionTypePair pair) {
		return Flowable.defer(() -> {
			log.trace("Fetching market history for {}", pair);
			return marketHistoryFetcher.fetchMarketHistory(pair);
		});
	}

	private Completable saveMarketHistory(JsonNode entry) {
		return Completable.defer(() -> {
			var pair = RegionTypePair.fromHistory(entry);
			var date = LocalDate.parse(entry.get("date").asText());
			var id = pair.toString();
			mapSet.put(date.toString(), id, entry);
			return Completable.complete();
		});
	}
}
