package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.WARZONE_LEADERBOARD;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes warzone leaderboard data from the ESI API.
 */
@Log4j2
public class ScrapeWarzoneLeaderboard implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeWarzoneLeaderboard() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting warzone insurgency scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://www.eveonline.com/api/warzone/leaderboard", WARZONE_LEADERBOARD, scrapeTime);

		log.info("Completed warzone leaderboard scrape");
	}
}
