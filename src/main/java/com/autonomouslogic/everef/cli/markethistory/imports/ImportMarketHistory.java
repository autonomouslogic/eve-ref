package com.autonomouslogic.everef.cli.markethistory.imports;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.markethistory.AvailableMarketHistoryFile;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.cli.markethistory.scrape.ScrapeMarketHistoryBatchLoader;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImportMarketHistory implements Command {

	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected MarketHistoryDao marketHistoryDao;

	@Inject
	protected ScrapeMarketHistoryBatchLoader scrapeMarketHistoryBatchLoader;

	@Inject
	protected MarketHistoryFileResolver fileResolver;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	@Inject
	protected ObjectMapper objectMapper;

	private final LocalDate minDate = Configs.IMPORT_MARKET_HISTORY_MIN_DATE.getRequired();

	private final int insertSize = Configs.IMPORT_MARKET_HISTORY_INSERT_SIZE.getRequired();

	private final int insertConcurrency = Configs.IMPORT_MARKET_HISTORY_INSERT_CONCURRENCY.getRequired();

	@Inject
	protected ImportMarketHistory() {}

	@Override
	public Completable run() {
		return getResolveFilesToDownload()
				.filter(f -> f.isDateFile())
				.map(f -> f.getRange().getLeft())
				.toList()
				.flatMapCompletable(includedDates -> {
					return scrapeMarketHistoryBatchLoader
							.setIncludedDates(includedDates)
							.load()
							.map(pair -> objectMapper.convertValue(pair.getValue(), MarketHistoryEntry.class))
							.buffer(insertSize)
							.flatMapCompletable(entries -> marketHistoryDao.insert(entries), false, insertConcurrency);
				});
	}

	private Flowable<AvailableMarketHistoryFile> getResolveFilesToDownload() {
		return fileResolver
				.setMinDate(minDate)
				.resolveFilesToDownload()
				.sorted(Ordering.natural()
						.onResultOf(
								(AvailableMarketHistoryFile f) -> f.getRange().getLeft())
						.reverse());
	}
}
