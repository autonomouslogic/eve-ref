package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.autonomouslogic.everef.util.ArchivePathFactory;
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
				.fetchAndUpload(anyString(), any(ArchivePathFactory.class), any(ZonedDateTime.class));

		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/universe/system_jumps/?datasource=tranquility",
						SYSTEM_JUMPS,
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/universe/system_kills/?datasource=tranquility",
						SYSTEM_KILLS,
						any(ZonedDateTime.class));
	}
}
