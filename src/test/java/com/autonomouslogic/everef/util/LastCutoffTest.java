package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class LastCutoffTest {
	LastCutoff lastCutoff = new LastCutoff();

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/util/LastCutoffTest/cutoffs.csv")
	void shouldCalculateCutoff(ZonedDateTime now, LocalTime cutoff, ZonedDateTime expected) {
		assertEquals(expected, lastCutoff.getCutoff(now, cutoff));
	}
}
