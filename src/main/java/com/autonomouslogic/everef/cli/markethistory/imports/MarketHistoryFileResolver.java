package com.autonomouslogic.everef.cli.markethistory.imports;

import com.autonomouslogic.everef.cli.markethistory.AvailableMarketHistoryFile;
import com.autonomouslogic.everef.cli.markethistory.MarketHistoryUtil;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
public class MarketHistoryFileResolver {

	@Inject
	protected MarketHistoryUtil marketHistoryUtil;

	@Inject
	protected MarketHistoryDao marketHistoryDao;

	@Setter
	private LocalDate minDate;

	@Inject
	protected MarketHistoryFileResolver() {}

	/**
	 * Examines available and present pairs, as well as all files on the data site, and decides which ones to download and import.
	 * @return
	 */
	public Flowable<AvailableMarketHistoryFile> resolveFilesToDownload() {
		return marketHistoryUtil
				.downloadTotalPairs()
				.flatMapPublisher(totalPairs -> {
					return marketHistoryDao.fetchDailyPairs(minDate).flatMapPublisher(present -> {
						return marketHistoryUtil.crawlAvailableFiles().filter(file -> {
							return filterAvailableFile(file, totalPairs, present);
						});
					});
				})
				.toList()
				.flatMapPublisher(files -> {
					logResolvedFiles(files);
					return Flowable.fromIterable(files);
				});
	}

	/**
	 * @param file file seen on the data site
	 * @param totalPairs total pairs seen on the data site
	 * @param presentPairs pairs already present in the database
	 * @return
	 */
	private boolean filterAvailableFile(
			AvailableMarketHistoryFile file, Map<LocalDate, Integer> totalPairs, Map<LocalDate, Integer> presentPairs) {
		var start = file.getRange().getLeft();
		var end = file.getRange().getRight();
		if (end.isBefore(minDate)) {
			return false;
		}
		if (file.isDateFile()) {
			if (totalPairs.containsKey(start)) {
				var total = totalPairs.get(start);
				var present = presentPairs.get(start);
				if (present == null) {
					return true;
				}
				if (total > present) {
					log.debug("Date {} will be updated from {} to {} pairs", start, present, total);
					return true;
				}
				return false;
			}
			return !presentPairs.containsKey(start);
		} else if (file.isYearFile() || file.isCcpQuantBackfillFile()) {
			return !containsRange(file.getRange(), presentPairs.keySet());
		}
		return false;
	}

	private boolean containsRange(Pair<LocalDate, LocalDate> range, Set<LocalDate> localDates) {
		return localDates.stream().anyMatch(d -> !d.isBefore(range.getLeft()) && !d.isAfter(range.getRight()));
	}

	private static void logResolvedFiles(List<AvailableMarketHistoryFile> files) {
		var days = files.stream()
				.filter(f -> f.isDateFile())
				.map(f -> f.getRange().getLeft())
				.distinct()
				.sorted()
				.toList();
		var years = files.stream()
				.filter(f -> f.isYearFile())
				.map(f -> f.getRange().getLeft().getYear())
				.distinct()
				.sorted()
				.toList();
		var ccpBackfill = files.stream().anyMatch(f -> f.isCcpQuantBackfillFile());
		log.debug("Found {} files to download", files.size());
		if (!days.isEmpty()) {
			log.debug("Day files to be imported: ({}) {}", days.size(), days);
		}
		if (!years.isEmpty()) {
			log.debug("Year files to be imported: ({}) {}", years.size(), years);
		}
		if (ccpBackfill) {
			log.debug("CCP backfill file to be imported");
		}
	}
}
