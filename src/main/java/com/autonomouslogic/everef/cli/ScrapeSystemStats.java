package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes system statistics from the ESI API.
 */
@Log4j2
public class ScrapeSystemStats implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeSystemStats() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting system stats scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/universe/system_jumps/?datasource=tranquility",
				SYSTEM_JUMPS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/universe/system_kills/?datasource=tranquility",
				SYSTEM_KILLS,
				scrapeTime);

		log.info("Completed system stats scrape");
	}
}
