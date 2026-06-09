package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes incursions data from the ESI API.
 */
@Log4j2
public class ScrapeIncursions implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeIncursions() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting incursions scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/incursions/?datasource=tranquility",
				ArchivePathFactories.INCURSIONS,
				scrapeTime);

		log.info("Completed incursions scrape");
	}
}
