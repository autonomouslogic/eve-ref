package com.autonomouslogic.everef.cli;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import com.autonomouslogic.everef.util.archive.StandardArchivePathFactory;
import java.time.ZonedDateTime;
import lombok.SneakyThrows;
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
	@SneakyThrows
	void shouldCallGenericScraperForAllDatasets() {
		VirtualThreads.onVirtualThread(command::run);

		verify(genericHistoryScraper, times(3))
				.fetchAndUpload(anyString(), any(StandardArchivePathFactory.class), any(ZonedDateTime.class));

		verify(genericHistoryScraper)
				.fetchAndUpload(
						eq("https://esi.evetech.net/latest/sovereignty/map/?datasource=tranquility"),
						eq(ArchivePathFactories.SOVEREIGNTY_MAP),
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						eq("https://esi.evetech.net/latest/sovereignty/structures/?datasource=tranquility"),
						eq(ArchivePathFactories.SOVEREIGNTY_STRUCTURES),
						any(ZonedDateTime.class));
		verify(genericHistoryScraper)
				.fetchAndUpload(
						eq("https://esi.evetech.net/latest/sovereignty/campaigns/?datasource=tranquility"),
						eq(ArchivePathFactories.SOVEREIGNTY_CAMPAIGNS),
						any(ZonedDateTime.class));
	}
}
