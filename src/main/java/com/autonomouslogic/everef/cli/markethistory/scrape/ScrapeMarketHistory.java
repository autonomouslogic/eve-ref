package com.autonomouslogic.everef.cli.markethistory.scrape;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_HISTORY;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryUtil;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.mvstore.JsonNodeDataType;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.mvstore.StoreMapSet;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.ProgressReporter;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.FileOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;
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
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Inject
	protected MarketHistoryFetcher marketHistoryFetcher;

	@Inject
	protected MarketHistoryUtil marketHistoryUtil;

	@Inject
	protected Provider<ScrapeMarketHistoryBatchLoader> marketHistoryLoaderProvider;

	@Inject
	protected Provider<CompoundRegionTypeSource> compoundRegionTypeSourceProvider;

	@Inject
	protected Provider<HistoricalOrdersRegionTypeSource> historicalOrdersRegionTypeSourceProvider;

	@Inject
	protected Provider<ActiveOrdersRegionTypeSource> activeOrdersRegionTypeSourceProvider;

	@Inject
	protected Provider<TopTradedRegionTypeSource> topTradedRegionTypeSourceProvider;

	@Inject
	protected Provider<HistoryRegionTypeSource> historyRegionTypeSourceProvider;

	@Inject
	protected Provider<ExplorerRegionTypeSource> explorerRegionTypeSourceProvider;

	@Inject
	protected Provider<RecentRegionTypeRemover> recentRegionTypeRemoverProvider;

	@Inject
	protected Provider<MarketHistoryFileBuilder> marketHistoryFileBuilderProvider;

	private MarketHistorySourceStats stats = new MarketHistorySourceStats();

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final int esiConcurrency = Configs.ESI_MARKET_HISTORY_CONCURRENCY.getRequired();
	private final int chunkSize = Configs.ESI_MARKET_HISTORY_CHUNK_SIZE.getRequired();
	private final int saveConcurrency = Configs.MARKET_HISTORY_SAVE_CONCURRENCY.getRequired();

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Setter
	private LocalDate today = LocalDate.now(ZoneOffset.UTC);

	@Setter
	private LocalDate minDate = today.minusDays(1).minus(Configs.ESI_MARKET_HISTORY_LOOKBACK.getRequired());

	private S3Url dataUrl;
	private MVStore mvStore;
	private StoreMapSet mapSet;
	private Map<LocalDate, Integer> totals;
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
		marketHistoryFetcher.setStats(stats);
		return Completable.concatArray(
						initSources(),
						initMvStore(),
						downloadTotalPairs(),
						loadMarketHistory(),
						loadPairs().buffer(chunkSize).flatMapCompletable(this::processChunk, false, 1),
						Completable.fromAction(() -> stats.logStats()))
				.doFinally(this::closeMvStore)
				.retry(1, e -> {
					log.warn("Error scraping market history, retrying from the beginning", e);
					return true;
				});
	}

	@NotNull
	private CompletableSource processChunk(List<RegionTypePair> chunk) {
		return Completable.defer(() -> {
			log.info("Processing chunk of {} pairs", chunk.size());
			return Completable.concatArray(
					Flowable.fromIterable(chunk)
							.flatMapCompletable(
									pair -> fetchMarketHistory(pair)
											.flatMapCompletable(entry -> saveMarketHistory((ObjectNode) entry)),
									false,
									esiConcurrency),
					uploadArchives(),
					uploadTotalPairs());
		});
	}

	private Completable initSources() {
		return Completable.fromAction(() -> {
			regionTypeSource = compoundRegionTypeSourceProvider.get().setStats(stats);
			regionTypeSource.addSource(historyRegionTypeSourceProvider.get()); // should probably be first.
			regionTypeSource.addSource(activeOrdersRegionTypeSourceProvider.get());
			regionTypeSource.addSource(
					historicalOrdersRegionTypeSourceProvider.get().setToday(today));
			regionTypeSource.addSource(topTradedRegionTypeSourceProvider.get());
			regionTypeSource.addSource(explorerRegionTypeSourceProvider.get().setToday(today));
			regionTypeSource.addSource(recentRegionTypeRemoverProvider.get()); // must be last.
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
			final var loader = marketHistoryLoaderProvider.get();
			return loader.setMinDate(minDate)
					.load()
					.doOnNext(p -> {
						var entry = p.getRight();
						var id = RegionTypePair.fromHistory(entry).toString();
						mapSet.put(p.getLeft().toString(), id, entry);
						regionTypeSource.addHistory(objectMapper.treeToValue(entry, MarketHistoryEntry.class));
					})
					.ignoreElements()
					.andThen(Completable.fromAction(() -> {
						mapSet.forEachMap((dateString, map) -> {
							var date = LocalDate.parse(dateString);
							var entries = map.size();
							var currentEntries = totals.get(date);
							log.debug("Loaded entries for {}: {}", date, map.size());
							if (currentEntries != null && entries < currentEntries) {
								log.warn(
										"Entries loaded for {}: {} is less than the current total {} ({})",
										date,
										entries,
										currentEntries,
										entries - currentEntries);
							}
							totals.put(date, map.size());
							var fileEntries = loader.getFileTotals().get(date);
							if (fileEntries != null && fileEntries != map.size()) {
								throw new IllegalStateException(String.format(
										"File loaded for %s contained %s entries, but %s were loaded",
										date, fileEntries, map.size()));
							}
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
			log.debug("Fetching market history for {}", pair);
			return marketHistoryFetcher.fetchMarketHistory(pair).doFinally(() -> progressReporter.increment());
		});
	}

	private Completable saveMarketHistory(ObjectNode entry) {
		return Completable.defer(() -> {
					var pair = RegionTypePair.fromHistory(entry);
					var date = LocalDate.parse(entry.get("date").asText());
					var id = pair.toString();
					if (!mapSet.hasMap(date.toString())) {
						return Completable.error(new RuntimeException(String.format("No map for date %s", date)));
					}
					var previous = (ObjectNode) mapSet.get(date.toString(), id);
					if (previous != null) {
						var entryToText = objectMapper.createObjectNode();
						entry.fieldNames()
								.forEachRemaining(name ->
										entryToText.put(name, entry.get(name).asText()));
						previous.set("http_last_modified", entryToText.get("http_last_modified"));
						if (previous.equals(entryToText)) {
							return Completable.complete();
						}
					}
					mapSet.put(date.toString(), id, entry);
					return Completable.complete();
				})
				.onErrorResumeNext(e -> Completable.error(new RuntimeException("Failed saving market history", e)));
	}

	private Completable uploadArchives() {
		return Completable.defer(() -> {
			log.info("Uploading archives");
			return Flowable.fromIterable(mapSet.getMapNames())
					.flatMapMaybe(date -> uploadArchive(LocalDate.parse(date)), false, saveConcurrency)
					.sorted()
					.toList()
					.flatMapCompletable(archives -> dataIndexHelper.updateIndex(archives));
		});
	}

	private Maybe<S3Url> uploadArchive(LocalDate date) {
		return Maybe.defer(() -> {
					var existingCount = totals.get(date);
					var entries = mapSet.getOrCreateMap(date.toString());
					if (existingCount == entries.size()) {
						log.debug(String.format("Skipping upload for %s, no new entries", date));
						return Maybe.empty();
					}
					if (entries.size() < existingCount) {
						return Maybe.error(new IllegalStateException(String.format(
								"Entries for %s have shrunk from %s to %s (%s)",
								date, existingCount, entries.size(), entries.size() - existingCount)));
					}
					totals.put(date, entries.size());
					log.debug("Writing archive for {} - {} entries", date, entries.size());
					var archive = marketHistoryFileBuilderProvider.get().writeEntries(entries.values());
					var archivePath = dataUrl.resolve(MARKET_HISTORY.createArchivePath(date));
					var archivePut = s3Util.putPublicObjectRequest(
							archive.length(), archivePath, "application/x-bzip2", latestCacheTime);
					log.info(String.format("Uploading archive for %s", date));
					return s3Adapter
							.putObject(archivePut, archive, s3Client)
							.ignoreElement()
							.andThen(Completable.fromAction(() -> {
								log.debug("Uploaded archive for {}", date);
								archive.delete();
							}))
							.andThen(Maybe.just(archivePath));
				})
				.compose(Rx.offloadMaybe());
	}

	private Completable uploadTotalPairs() {
		return Completable.defer(() -> {
					log.debug("Building total pairs file");
					var file =
							tempFiles.tempFile("market-history-pairs", ".json").toFile();
					try (var out = new FileOutputStream(file)) {
						objectMapper.writeValue(out, totals);
					}
					var archivePath =
							dataUrl.resolve(MARKET_HISTORY.getFolder() + "/").resolve("totals.json");
					var archivePut = s3Util.putPublicObjectRequest(
							file.length(), archivePath, "application/json", latestCacheTime);
					log.info("Uploading total pairs file");
					return s3Adapter
							.putObject(archivePut, file, s3Client)
							.ignoreElement()
							.andThen(Completable.fromAction(() -> file.delete()));
				})
				.compose(Rx.offloadCompletable());
	}

	private Completable downloadTotalPairs() {
		return marketHistoryUtil
				.downloadTotalPairs()
				.flatMapCompletable(pairs -> Completable.fromAction(() -> totals = pairs));
	}
}
