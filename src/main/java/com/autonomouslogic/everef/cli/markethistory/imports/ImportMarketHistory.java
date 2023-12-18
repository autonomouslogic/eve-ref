package com.autonomouslogic.everef.cli.markethistory.imports;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.flyway.FlywayMigrate;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryLoader;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.util.Rx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
public class ImportMarketHistory implements Command {
	private static final Ordering<Pair<LocalDate, HttpUrl>> IMPORT_ORDERING = Ordering.natural()
			.onResultOf((Pair<LocalDate, HttpUrl> f) -> f.getLeft())
			.reverse();

	@Inject
	protected FlywayMigrate flywayMigrate;

	@Inject
	protected MarketHistoryDao marketHistoryDao;

	@Inject
	protected MarketHistoryLoader marketHistoryLoader;

	@Inject
	protected MarketHistoryFileResolver fileResolver;

	@Inject
	protected ObjectMapper objectMapper;

	private final Optional<LocalDate> minDate = Configs.IMPORT_MARKET_HISTORY_MIN_DATE.get();

	private final Period lookback = Configs.ESI_MARKET_HISTORY_LOOKBACK.getRequired();

	private final int loadConcurrency = Configs.MARKET_HISTORY_LOAD_CONCURRENCY.getRequired();

	private final int insertConcurrency = Configs.INSERT_CONCURRENCY.getRequired();

	@Inject
	protected ImportMarketHistory() {
		if (Configs.INSERT_CONCURRENCY.getRequired() > 1) {
			throw new RuntimeException(
					"INSERT_CONCURRENCY > 1 not supported, transactions are not safe - see https://github.com/autonomouslogic/eve-ref/issues/356");
		}
	}

	@Override
	public Completable run() {
		return Completable.concatArray(flywayMigrate.autoRun(), runImport());
	}

	private Completable runImport() {
		return resolveFilesToDownload()
				.flatMap(availableFile -> loadFile(availableFile), false, loadConcurrency)
				.flatMapCompletable(dateList -> insertDayEntries(dateList), false, insertConcurrency);
	}

	private Flowable<Pair<LocalDate, List<JsonNode>>> loadFile(Pair<LocalDate, HttpUrl> availableFile) {
		return marketHistoryLoader.loadDailyFile(availableFile.getRight(), availableFile.getLeft());
	}

	private Completable insertDayEntries(Pair<LocalDate, List<JsonNode>> dateList) {
		return Completable.fromAction(() -> {
					var date = dateList.getLeft();
					var nodes = dateList.getRight();
					log.info("Inserting {} entries for {}", nodes.size(), date);
					var entries = dateList.getRight().stream()
							.map(node -> objectMapper.convertValue(node, MarketHistoryEntry.class))
							.toList();
					marketHistoryDao.insert(entries);
					log.debug("Completed {}", date);
				})
				.compose(Rx.offloadCompletable());
	}

	private Flowable<Pair<LocalDate, HttpUrl>> resolveFilesToDownload() {
		return resolveMinDate().flatMapPublisher(resolvedMinDate -> {
			log.info("Importing market data from {}", resolvedMinDate);
			return fileResolver
					.setMinDate(resolvedMinDate)
					.resolveFilesToDownload()
					.sorted(IMPORT_ORDERING);
		});
	}

	protected Single<LocalDate> resolveMinDate() {
		return Single.defer(() -> {
			if (minDate.isPresent()) {
				log.trace("Using min date from config: {}", minDate.get());
				return Single.just(minDate.get());
			} else {
				return marketHistoryDao
						.fetchLatestDate()
						.switchIfEmpty(Single.fromCallable(() -> {
							var date = LocalDate.now(ZoneOffset.UTC);
							log.trace("No entries found, using min date from today: {}", date);
							return date;
						}))
						.flatMap(latest -> Single.fromCallable(() -> {
							var date = latest.minus(lookback);
							log.trace("Last seen date {}, using min date: {}", latest, date);
							return date;
						}));
			}
		});
	}
}
