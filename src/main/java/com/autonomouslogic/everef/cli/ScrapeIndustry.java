package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes industry-related data from the ESI API.
 */
@Log4j2
public class ScrapeIndustry implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeIndustry() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting industry scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/industry/systems/?datasource=tranquility",
				ArchivePathFactories.INDUSTRY_SYSTEMS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/industry/facilities/?datasource=tranquility",
				ArchivePathFactories.INDUSTRY_FACILITIES,
				scrapeTime);

		log.info("Completed industry scrape");
	}
}
