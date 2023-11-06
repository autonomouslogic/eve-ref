package com.autonomouslogic.everef.cli.markethistory.imports;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.flyway.FlywayMigrate;
import com.autonomouslogic.everef.cli.markethistory.AvailableMarketHistoryFile;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.util.Rx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
public class ImportMarketHistory implements Command {
	private static final Ordering<AvailableMarketHistoryFile> IMPORT_ORDERING = Ordering.natural()
			.onResultOf((AvailableMarketHistoryFile f) -> f.getRange().getLeft())
			.reverse();

	@Inject
	protected FlywayMigrate flywayMigrate;

	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected MarketHistoryDao marketHistoryDao;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	@Inject
	protected MarketHistoryFileResolver fileResolver;

	@Inject
	protected ObjectMapper objectMapper;

	private final LocalDate minDate = Configs.IMPORT_MARKET_HISTORY_MIN_DATE.getRequired();

	private final int insertSize = Configs.IMPORT_MARKET_HISTORY_INSERT_SIZE.getRequired();

	private final int insertConcurrency = Configs.IMPORT_MARKET_HISTORY_INSERT_CONCURRENCY.getRequired();

	private final int loadConcurrency = Configs.MARKET_HISTORY_LOAD_CONCURRENCY.getRequired();

	@Inject
	protected ImportMarketHistory() {}

	@Override
	public Completable run() {
		return Completable.concatArray(flywayMigrate.autoRun(), runImport());
	}

	private Completable runImport() {
		log.info("==== insertConcurrency: {}", insertConcurrency);
		return marketHistoryDao.fetchDailyPairs(minDate).flatMapCompletable(dailyPairs -> {
			return resolveFilesToDownload()
					.filter(f -> f.isDateFile() || f.isYearFile()) // @todo remove
					.flatMap(availableFile -> loadFile(dailyPairs, availableFile), false, loadConcurrency)
					.flatMapCompletable(dateList -> insertDayEntries(dateList), false, insertConcurrency);
		});
	}

	private Flowable<Pair<LocalDate, List<JsonNode>>> loadFile(
			Map<LocalDate, Integer> dailyPairs, AvailableMarketHistoryFile availableFile) {
		if (availableFile.isDateFile()) {
			return marketHistoryLoader.loadDailyFile(availableFile.getHttpUrl(), availableFile.getDate());
		} else if (availableFile.isYearFile()) {
			return marketHistoryLoader.loadYearFile(availableFile.getHttpUrl(), dailyPairs.keySet());
		} else {
			return Flowable.error(new RuntimeException("Unknown file type: " + availableFile));
		}
	}

	private Completable insertDayEntries(Pair<LocalDate, List<JsonNode>> dateList) {
		return Completable.fromAction(() -> {
					var date = dateList.getLeft();
					var nodes = dateList.getRight();
					log.info("Inserting {} entries for {}", nodes.size(), date);
					dbAccess.context().transaction(trx -> {
						Flowable.fromIterable(nodes)
								.map(node -> objectMapper.convertValue(node, MarketHistoryEntry.class))
								.buffer(insertSize)
								.flatMapCompletable(entries -> marketHistoryDao.insert(trx.dsl(), entries), false, 1)
								.blockingAwait();
					});
					log.debug("Completed {}", date);
				})
				.compose(Rx.offloadCompletable());
	}

	private Flowable<AvailableMarketHistoryFile> resolveFilesToDownload() {
		return fileResolver.setMinDate(minDate).resolveFilesToDownload().sorted(IMPORT_ORDERING);
	}
}
