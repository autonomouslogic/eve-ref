package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes warzone insurgency data from the ESI API.
 */
@Log4j2
public class ScrapeWarzoneInsurgency implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeWarzoneInsurgency() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting warzone insurgency scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://www.eveonline.com/api/warzone/insurgency",
				ArchivePathFactories.WARZONE_INSURGENCY,
				scrapeTime);

		log.info("Completed warzone insurgency scrape");
	}
}
