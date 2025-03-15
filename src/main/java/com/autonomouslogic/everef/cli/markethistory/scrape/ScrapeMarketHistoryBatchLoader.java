package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.cli.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryUtil;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Loads the batch market history needed for the scrape.
 */
@Log4j2
class ScrapeMarketHistoryBatchLoader {
	@Inject
	protected MarketHistoryUtil marketHistoryUtil;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	private final int downloadConcurrency = Configs.MARKET_HISTORY_LOAD_CONCURRENCY.getRequired();

	@Setter
	private LocalDate minDate;

	@Setter
	private Collection<LocalDate> includedDates;

	@Getter
	private final Map<LocalDate, Integer> fileTotals = new TreeMap<>();

	@Inject
	protected ScrapeMarketHistoryBatchLoader() {}

	/**
	 * @return
	 */
	public Flowable<Pair<LocalDate, JsonNode>> load() {
		log.info("Loading market history - minDate: {} - includedDates: {}", minDate, includedDates);
		var totalEntries = new AtomicInteger();
		var f = crawlFiles();
		if (minDate != null) {
			f = f.filter(p -> !p.getLeft().isBefore(minDate));
		}
		if (includedDates != null) {
			f = f.filter(p -> includedDates.contains(p.getLeft()));
		}
		return f.sorted(Ordering.natural().onResultOf(Pair::getLeft))
				.switchIfEmpty(Flowable.error(new RuntimeException("No market history files found.")))
				.parallel(downloadConcurrency)
				.runOn(VirtualThreads.SCHEDULER)
				.flatMap(p -> {
					return marketHistoryLoader
							.loadDailyFile(p.getRight(), p.getLeft())
							.flatMap(entry -> Flowable.fromIterable(entry.getRight()))
							.map(entry -> {
								totalEntries.incrementAndGet();
								return Pair.of(p.getLeft(), entry);
							})
							.toList()
							.flatMapPublisher(entries -> {
								synchronized (fileTotals) {
									fileTotals.put(p.getLeft(), entries.size());
								}
								return Flowable.fromIterable(entries);
							});
				})
				.sequential()
				.doOnComplete(() -> log.info("Loaded {} market history entries", totalEntries.get()))
				.switchIfEmpty(Flowable.error(new RuntimeException("No market data found in history files.")));
	}

	private Flowable<Pair<LocalDate, DataUrl>> crawlFiles() {
		return marketHistoryUtil.crawlAvailableFiles().map(f -> Pair.of(f.getLeft(), f.getRight()));
	}
}
