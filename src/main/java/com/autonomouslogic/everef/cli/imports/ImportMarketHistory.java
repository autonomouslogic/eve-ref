package com.autonomouslogic.everef.cli.imports;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.markethistory.MarketHistoryUtil;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImportMarketHistory implements Command {

	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected MarketHistoryDao marketHistoryDao;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	@Inject
	protected MarketHistoryUtil marketHistoryUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	private LocalDate minDate = Configs.IMPORT_MARKET_HISTORY_MIN_DATE.getRequired();

	@Inject
	protected ImportMarketHistory() {}

	@Override
	public Completable run() {
		var crawl = marketHistoryUtil.crawlAvailableFiles().toList().blockingGet();
		return resolveDatesToDownload().flatMapCompletable(includedDates -> {
			return marketHistoryLoader
					.setIncludedDates(includedDates)
					.load()
					.map(pair -> objectMapper.convertValue(pair.getValue(), MarketHistoryEntry.class))
					.buffer(100)
					.flatMapCompletable(entries -> marketHistoryDao.insert(entries), false, 1);
		});
	}

	private Single<Set<LocalDate>> resolveDatesToDownload() {
		return marketHistoryUtil.downloadTotalPairs().flatMap(available -> {
			return marketHistoryDao.fetchDailyPairs(minDate).flatMap(present -> {
				var date = minDate;
				var today = LocalDate.now(ZoneOffset.UTC);
				var dates = new LinkedHashSet<LocalDate>();
				while (!date.isAfter(today)) {
					if (!present.containsKey(date)) {
						dates.add(date);
					} else if (available.containsKey(date) && available.get(date) > present.get(date)) {
						dates.add(date);
					}
					date = date.plusDays(1);
				}
				return Single.just(dates);
			});
		});
	}
}
