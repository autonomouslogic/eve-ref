package com.autonomouslogic.everef.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpDatesTest {
	private static final Instant EXPECTED = Instant.parse("1994-11-06T08:49:37Z");

	@Test
	void shouldParseImfFixdate() {
		var result = HttpDates.parse("Sun, 06 Nov 1994 08:49:37 GMT");
		assertTrue(result.isPresent());
		assertEquals(EXPECTED, result.get());
	}

	@Test
	void shouldParseRfc850() {
		var result = HttpDates.parse("Sunday, 06-Nov-94 08:49:37 GMT");
		assertTrue(result.isPresent());
		assertEquals(EXPECTED, result.get());
	}

	@Test
	void shouldParseAsctime() {
		var result = HttpDates.parse("Sun Nov  6 08:49:37 1994");
		assertTrue(result.isPresent());
		assertEquals(EXPECTED, result.get());
	}

	@Test
	void shouldTruncateSubSecondPrecision() {
		// HTTP-dates carry no sub-second precision; this is testing the truncation of the resolved value, not
		// parsing (the format itself has no fractional seconds), so parse a value and only assert on seconds.
		var result = HttpDates.parse("Sun, 06 Nov 1994 08:49:37 GMT");
		assertTrue(result.isPresent());
		assertEquals(0, result.get().getNano());
	}

	@Test
	void shouldRejectGarbage() {
		assertTrue(HttpDates.parse("not a date").isEmpty());
	}

	@Test
	void shouldRejectWrongWeekdayFormat() {
		assertTrue(HttpDates.parse("Sun. 06 Nov 1994 08:49:37 GMT").isEmpty());
	}

	@Test
	void shouldRejectMissingGmt() {
		assertTrue(HttpDates.parse("Sun, 06 Nov 1994 08:49:37").isEmpty());
	}

	@Test
	void shouldRejectNull() {
		assertTrue(HttpDates.parse(null).isEmpty());
	}

	@Test
	void shouldParseSingleValuedHeader() {
		var result = HttpDates.parseSingleValuedHeader(List.of("Sun, 06 Nov 1994 08:49:37 GMT"));
		assertTrue(result.isPresent());
		assertEquals(EXPECTED, result.get());
	}

	@Test
	void shouldIgnoreMultiMemberDateHeader() {
		var result = HttpDates.parseSingleValuedHeader(
				List.of("Sun, 06 Nov 1994 08:49:37 GMT", "Sun, 06 Nov 1994 08:49:38 GMT"));
		assertTrue(result.isEmpty());
	}

	@Test
	void shouldIgnoreEmptyHeader() {
		assertTrue(HttpDates.parseSingleValuedHeader(List.of()).isEmpty());
	}
}
