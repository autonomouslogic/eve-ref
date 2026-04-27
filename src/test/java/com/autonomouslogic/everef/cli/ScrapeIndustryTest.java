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
 * Tests for ScrapeIndustry command.
 */
@ExtendWith(MockitoExtension.class)
class ScrapeIndustryTest {
	@Mock
	private GenericHistoryScraper genericHistoryScraper;

	private ScrapeIndustry command;

	@BeforeEach
	void setUp() {
		command = new ScrapeIndustry();
		command.genericHistoryScraper = genericHistoryScraper;
	}

	@Test
	void shouldCallGenericScraperForAllDatasets() {
		command.run();

		verify(genericHistoryScraper, times(2))
				.fetchAndUpload(anyString(), any(ArchivePathFactory.class), any(ZonedDateTime.class));

		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/industry/systems/?datasource=tranquility",
						INDUSTRY_SYSTEMS,
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/industry/facilities/?datasource=tranquility",
						INDUSTRY_FACILITIES,
						any(ZonedDateTime.class));
	}
}
