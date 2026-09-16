package com.autonomouslogic.everef.cli;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.util.archive.StandardArchivePathFactory;
import java.time.ZonedDateTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScrapeInsuranceTest {
	@Mock
	private GenericHistoryScraper genericHistoryScraper;

	private ScrapeInsurance command;

	@BeforeEach
	void setUp() {
		command = new ScrapeInsurance();
		command.genericHistoryScraper = genericHistoryScraper;
	}

	@Test
	@SneakyThrows
	void shouldCallGenericScraperForAllDatasets() {
		VirtualThreads.onVirtualThread(command::run);
		verify(genericHistoryScraper, times(1))
				.fetchAndUpload(anyString(), any(StandardArchivePathFactory.class), any(ZonedDateTime.class));
	}
}
