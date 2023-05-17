package com.autonomouslogic.everef.util;

import java.time.Duration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ProgressReporterTest {
	@Test
	@Disabled
	@SneakyThrows
	void shouldReport() {
		var progress = new ProgressReporter("test", 100, Duration.ofSeconds(1));
		progress.start();
		for (int i = 0; i < 100; i++) {
			progress.increment();
			Thread.sleep(100);
		}
	}
}
