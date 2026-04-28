package com.autonomouslogic.everef.cli;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.autonomouslogic.everef.util.ArchivePathFactory;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScrapeFactionWarfareTest {
	@Mock
	private GenericHistoryScraper genericHistoryScraper;

	private ScrapeFactionWarfare command;

	@BeforeEach
	void setUp() {
		command = new ScrapeFactionWarfare();
		command.genericHistoryScraper = genericHistoryScraper;
	}

	@Test
	void shouldCallGenericScraperForAllDatasets() {
		command.run();
		verify(genericHistoryScraper, times(6))
				.fetchAndUpload(anyString(), any(ArchivePathFactory.class), any(ZonedDateTime.class));
	}
}
