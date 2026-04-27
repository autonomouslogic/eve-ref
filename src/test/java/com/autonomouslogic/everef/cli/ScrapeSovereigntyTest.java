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
 * Tests for ScrapeSovereignty command.
 */
@ExtendWith(MockitoExtension.class)
class ScrapeSovereigntyTest {
	@Mock
	private GenericHistoryScraper genericHistoryScraper;

	private ScrapeSovereignty command;

	@BeforeEach
	void setUp() {
		command = new ScrapeSovereignty();
		command.genericHistoryScraper = genericHistoryScraper;
	}

	@Test
	void shouldCallGenericScraperForAllDatasets() {
		command.run();

		verify(genericHistoryScraper, times(3))
				.fetchAndUpload(anyString(), any(ArchivePathFactory.class), any(ZonedDateTime.class));

		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/sovereignty/map/?datasource=tranquility",
						SOVEREIGNTY_MAP,
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/sovereignty/structures/?datasource=tranquility",
						SOVEREIGNTY_STRUCTURES,
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						"https://esi.evetech.net/latest/sovereignty/campaigns/?datasource=tranquility",
						SOVEREIGNTY_CAMPAIGNS,
						any(ZonedDateTime.class));
	}
}
