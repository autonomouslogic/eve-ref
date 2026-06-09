package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes faction warfare data from the ESI API.
 */
@Log4j2
public class ScrapeFactionWarfare implements Command {
	@Inject
	protected GenericHistoryScraper genericHistoryScraper;

	@Inject
	protected ScrapeFactionWarfare() {}

	@Override
	public void run() {
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		log.info("Starting faction warfare scrape");

		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/stats/?datasource=tranquility",
				ArchivePathFactories.FACTION_WARFARE_STATS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/wars/?datasource=tranquility",
				ArchivePathFactories.FACTION_WARFARE_WARS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/leaderboards/?datasource=tranquility",
				ArchivePathFactories.FACTION_WARFARE_LEADERBOARDS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/leaderboards/characters/?datasource=tranquility",
				ArchivePathFactories.FACTION_WARFARE_LEADERBOARDS_CHARACTERS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/leaderboards/corporations/?datasource=tranquility",
				ArchivePathFactories.FACTION_WARFARE_LEADERBOARDS_CORPORATIONS,
				scrapeTime);
		genericHistoryScraper.fetchAndUpload(
				"https://esi.evetech.net/latest/fw/systems/?datasource=tranquility",
				ArchivePathFactories.FACTION_WARFARE_SYSTEMS,
				scrapeTime);

		log.info("Completed faction warfare scrape");
	}
}
