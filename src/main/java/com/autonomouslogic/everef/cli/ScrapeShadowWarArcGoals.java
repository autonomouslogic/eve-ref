package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes shadow war arc goals data from the ESI API.
 */
@Log4j2
public class ScrapeShadowWarArcGoals implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeShadowWarArcGoals() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting shadow war arc goals scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/wars/arc_goals/?datasource=tranquility",
				SHADOW_WAR_ARC_GOALS,
				scrapeTime);

		log.info("Completed shadow war arc goals scrape");
	}
}
