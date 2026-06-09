package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes market prices data from the ESI API.
 */
@Log4j2
public class ScrapeMarketsPrices implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeMarketsPrices() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting markets prices scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/markets/prices/?datasource=tranquility",
				ArchivePathFactories.MARKETS_PRICES,
				scrapeTime);

		log.info("Completed markets prices scrape");
	}
}
