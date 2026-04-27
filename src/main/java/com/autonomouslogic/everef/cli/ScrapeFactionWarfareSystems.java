package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes faction warfare systems data from the ESI API.
 */
@Log4j2
public class ScrapeFactionWarfareSystems implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeFactionWarfareSystems() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting faction warfare systems scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/systems/?datasource=tranquility",
				FACTION_WARFARE_SYSTEMS,
				scrapeTime);

		log.info("Completed faction warfare systems scrape");
	}
}
