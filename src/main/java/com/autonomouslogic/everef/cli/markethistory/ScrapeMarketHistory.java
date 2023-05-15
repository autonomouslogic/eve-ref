package com.autonomouslogic.everef.cli.markethistory;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_HISTORY;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.mvstore.JsonNodeDataType;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.mvstore.StoreMapSet;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.ProgressReporter;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.Setter;
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
	protected TempFiles tempFiles;

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

	@Inject
	protected Provider<MarketHistoryFileBuilder> marketHistoryFileBuilderProvider;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private final int esiConcurrency = Configs.ESI_MARKET_HISTORY_CONCURRENCY.getRequired();

	@Setter
	private LocalDate today = LocalDate.now(ZoneOffset.UTC);

	@Setter
	private LocalDate minDate = today.minusDays(1).minus(Configs.ESI_MARKET_HISTORY_LOOKBACK.getRequired());

	private S3Url dataUrl;
	private MVStore mvStore;
	private StoreMapSet mapSet;
	private Map<LocalDate, Integer> historyEntries;
	private CompoundRegionTypeSource regionTypeSource;

	private ProgressReporter progressReporter;

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
										esiConcurrency),
						uploadArchives(),
						uploadTotalPairs())
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
			log.info("Sourcing pairs");
			return regionTypeSource.sourcePairs().toList().flatMapPublisher(pairs -> {
				log.info("Sourced {} pairs", pairs.size());
				progressReporter = new ProgressReporter(getName(), pairs.size(), Duration.ofMinutes(1));
				progressReporter.start();
				return Flowable.fromIterable(pairs);
			});
		});
	}

	private Flowable<JsonNode> fetchMarketHistory(RegionTypePair pair) {
		return Flowable.defer(() -> {
			log.trace("Fetching market history for {}", pair);
			return marketHistoryFetcher.fetchMarketHistory(pair).doFinally(() -> progressReporter.increment());
		});
	}

	private Completable saveMarketHistory(JsonNode entry) {
		return Completable.defer(() -> {
			var pair = RegionTypePair.fromHistory(entry);
			var date = LocalDate.parse(entry.get("date").asText());
			var id = pair.toString();
			if (!mapSet.hasMap(date.toString())) {
				return Completable.error(new RuntimeException(String.format("No map for date %s", date)));
			}
			mapSet.put(date.toString(), id, entry);
			return Completable.complete();
		});
	}

	private Completable uploadArchives() {
		return Completable.defer(() -> {
			log.info("Uploading archives");
			return Flowable.fromIterable(mapSet.getMapNames())
					.flatMapCompletable(date -> uploadArchive(LocalDate.parse(date)));
		});
	}

	private Completable uploadArchive(LocalDate date) {
		return Completable.defer(() -> {
					var existingCount = historyEntries.get(date);
					var entries = mapSet.getOrCreateMap(date.toString());
					if (existingCount == entries.size()) {
						log.debug(String.format("Skipping upload for %s, no new entries", date));
						return Completable.complete();
					}
					historyEntries.put(date, entries.size());
					log.info(String.format("Writing archive for %s", date));
					var archive = marketHistoryFileBuilderProvider.get().writeEntries(entries.values());
					var archivePath = dataUrl.resolve(MARKET_HISTORY.createArchivePath(date));
					var archivePut = s3Util.putPublicObjectRequest(
							archive.length(), archivePath, "application/x-bzip2", archiveCacheTime);
					return s3Adapter.putObject(archivePut, archive, s3Client).ignoreElement();
				})
				.compose(Rx.offloadCompletable());
	}

	private Completable uploadTotalPairs() {
		return Completable.defer(() -> {
					var file =
							tempFiles.tempFile("market-history-pairs", ".json").toFile();
					file.deleteOnExit();
					try (var out = new FileOutputStream(file)) {
						objectMapper.writeValue(out, historyEntries);
					}
					var archivePath =
							dataUrl.resolve(MARKET_HISTORY.getFolder() + "/").resolve("pairs.json");
					var archivePut = s3Util.putPublicObjectRequest(
							file.length(), archivePath, "application/json", latestCacheTime);
					return s3Adapter.putObject(archivePut, file, s3Client).ignoreElement();
				})
				.compose(Rx.offloadCompletable());
	}
}
