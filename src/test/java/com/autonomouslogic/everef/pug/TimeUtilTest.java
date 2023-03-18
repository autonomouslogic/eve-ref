package com.autonomouslogic.everef.pug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class TimeUtilTest {
	private TimeUtil timeUtil = new TimeUtil();

	@Test
	public void shouldFormatInstants() {
		assertEquals("2020-01-02 03:04:05 UTC", timeUtil.isoLike(Instant.parse("2020-01-02T03:04:05Z")));
		assertEquals("2020-01-02 23:59:58 UTC", timeUtil.isoLike(Instant.parse("2020-01-02T23:59:58Z")));
	}
}
