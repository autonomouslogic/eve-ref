package com.autonomouslogic.everef.cli.markethistory;

import static java.time.ZoneOffset.UTC;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Loads market history from the data site.
 */
@Log4j2
public class MarketHistoryLoader {
	@Inject
	protected DataCrawler dataCrawler;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected CsvMapper csvMapper;

	private final int downloadConcurrency = Configs.MARKET_HISTORY_LOAD_CONCURRENCY.getRequired();

	@Setter
	private LocalDate minDate;

	@Getter
	private final Map<LocalDate, Integer> fileTotals = new TreeMap<>();

	@Inject
	protected MarketHistoryLoader() {}

	@Inject
	protected void init() {
		dataCrawler.setPrefix(ArchivePathFactory.MARKET_HISTORY.getFolder() + "/");
	}

	public Flowable<Pair<LocalDate, JsonNode>> load() {
		log.info("Loading market history - minDate: {}", minDate);
		var totalEntries = new AtomicInteger();
		var f = crawlFiles();
		if (minDate != null) {
			f = f.filter(p -> !p.getLeft().isBefore(minDate));
		}
		return f.sorted(Ordering.natural().onResultOf(Pair::getLeft))
				.switchIfEmpty(Flowable.error(new RuntimeException("No market history files found.")))
				.flatMap(
						p -> {
							return downloadFile(p.getRight(), p.getLeft()).flatMapPublisher(file -> {
								file.deleteOnExit();
								return parseFile(file, p.getLeft())
										.map(entry -> {
											totalEntries.incrementAndGet();
											return Pair.of(p.getLeft(), entry);
										})
										.doFinally(() -> file.delete());
							});
						},
						false,
						downloadConcurrency)
				.doOnComplete(() -> log.info("Loaded {} market history entries", totalEntries.get()))
				.switchIfEmpty(Flowable.error(new RuntimeException("No market data found in history files.")));
	}

	private Flowable<Pair<LocalDate, DataUrl>> crawlFiles() {
		return dataCrawler.crawl().flatMap(url -> {
			var path = url.getPath();
			var time = ArchivePathFactory.MARKET_HISTORY.parseArchiveTime(path);
			if (time == null) {
				return Flowable.empty();
			}
			log.trace("Found market history file: {}", url);
			return Flowable.just(Pair.of(time.atZone(UTC).toLocalDate(), url));
		});
	}

	private Single<File> downloadFile(DataUrl url, LocalDate date) {
		return Single.defer(() -> {
			log.info("Downloading market history file for {}", date);
			var file = tempFiles
					.tempFile("market-history", ArchivePathFactory.MARKET_HISTORY.getSuffix())
					.toFile();
			return okHttpHelper.download(url.toString(), file, okHttpClient).flatMap(response -> {
				if (response.code() != 200) {
					return Single.error(
							new RuntimeException("Failed to download " + url + " with code " + response.code()));
				}
				return Single.just(file);
			});
		});
	}

	private Flowable<JsonNode> parseFile(File file, LocalDate date) {
		return Flowable.defer(() -> {
					log.trace("Reading market history file for {}: {}", date, file);
					var in = CompressUtil.uncompress(file);
					var schema = csvMapper
							.schemaFor(MarketHistoryEntry.class)
							.withHeader()
							.withStrictHeaders(true)
							.withColumnReordering(true);
					MappingIterator<JsonNode> iterator =
							csvMapper.readerFor(JsonNode.class).with(schema).readValues(in);
					var list = new ArrayList<JsonNode>();
					iterator.forEachRemaining(list::add);
					iterator.close();
					log.trace("Read {} entries for {} from {}", list.size(), date, file);
					synchronized (fileTotals) {
						fileTotals.put(date, list.size());
					}
					return Flowable.fromIterable(list);
				})
				.compose(Rx.offloadFlowable());
	}
}
