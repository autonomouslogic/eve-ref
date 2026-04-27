package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes universe structures data from the ESI API.
 */
@Log4j2
public class ScrapeUniverseStructures implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeUniverseStructures() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting universe structures scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/universe/structures/?datasource=tranquility",
				UNIVERSE_STRUCTURES,
				scrapeTime);

		log.info("Completed universe structures scrape");
	}
}
