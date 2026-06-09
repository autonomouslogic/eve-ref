package com.autonomouslogic.everef.cli;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import com.autonomouslogic.everef.util.archive.StandardArchivePathFactory;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for ScrapeSystemStats command.
 */
@ExtendWith(MockitoExtension.class)
class ScrapeSystemStatsTest {
	@Mock
	private GenericHistoryScraper genericHistoryScraper;

	private ScrapeSystemStats command;

	@BeforeEach
	void setUp() {
		command = new ScrapeSystemStats();
		command.genericHistoryScraper = genericHistoryScraper;
	}

	@Test
	void shouldCallGenericScraperForAllDatasets() {
		command.run();

		verify(genericHistoryScraper, times(2))
				.fetchAndUpload(anyString(), any(StandardArchivePathFactory.class), any(ZonedDateTime.class));

		verify(genericHistoryScraper)
				.fetchAndUpload(
						eq("https://esi.evetech.net/latest/universe/system_jumps/?datasource=tranquility"),
						eq(ArchivePathFactories.SYSTEM_JUMPS),
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						eq("https://esi.evetech.net/latest/universe/system_kills/?datasource=tranquility"),
						eq(ArchivePathFactories.SYSTEM_KILLS),
						any(ZonedDateTime.class));
	}
}
