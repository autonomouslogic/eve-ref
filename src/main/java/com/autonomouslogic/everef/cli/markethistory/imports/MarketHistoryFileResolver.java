package com.autonomouslogic.everef.cli.markethistory.imports;

import com.autonomouslogic.everef.cli.markethistory.MarketHistoryUtil;
import com.autonomouslogic.everef.db.MarketHistoryDao;
import com.autonomouslogic.everef.url.HttpUrl;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
class MarketHistoryFileResolver {

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
	public Flowable<Pair<LocalDate, HttpUrl>> resolveFilesToDownload() {
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
			Pair<LocalDate, HttpUrl> file, Map<LocalDate, Integer> totalPairs, Map<LocalDate, Integer> presentPairs) {
		var date = file.getLeft();
		if (date.isBefore(minDate)) {
			return false;
		}
		if (totalPairs.containsKey(date)) {
			var total = totalPairs.get(date);
			var present = presentPairs.get(date);
			if (present == null) {
				return true;
			}
			if (total > present) {
				log.debug("Date {} will be updated from {} to {} pairs", date, present, total);
				return true;
			}
			return false;
		}
		return !presentPairs.containsKey(date);
	}

	private static void logResolvedFiles(List<Pair<LocalDate, HttpUrl>> files) {
		var days = files.stream().map(f -> f.getLeft()).distinct().sorted().toList();
		log.info("Found {} files to download", files.size());
		if (!days.isEmpty()) {
			log.info("Day files to be imported: ({}) {}", days.size(), days);
		}
	}
}
