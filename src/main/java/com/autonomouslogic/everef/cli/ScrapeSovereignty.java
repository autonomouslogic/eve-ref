package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes sovereignty-related data from the ESI API.
 */
@Log4j2
public class ScrapeSovereignty implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeSovereignty() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting sovereignty scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/sovereignty/map/?datasource=tranquility", SOVEREIGNTY_MAP, scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/sovereignty/structures/?datasource=tranquility",
				SOVEREIGNTY_STRUCTURES,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/sovereignty/campaigns/?datasource=tranquility",
				SOVEREIGNTY_CAMPAIGNS,
				scrapeTime);

		log.info("Completed sovereignty scrape");
	}
}
