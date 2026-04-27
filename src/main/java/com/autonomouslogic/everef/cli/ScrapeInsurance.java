package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes insurance pricing data from the ESI API.
 */
@Log4j2
public class ScrapeInsurance implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeInsurance() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting insurance scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/insurance/prices/?datasource=tranquility",
				INSURANCE_PRICES,
				scrapeTime);

		log.info("Completed insurance scrape");
	}
}
