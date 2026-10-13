package com.autonomouslogic.everef.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {
	@Test
	void shouldLeaveAllowedCharactersAlone() {
		var value = "Mozilla/5.0 (X11; Linux x86_64) a-b_c.d~e:f/g?h=i&j%20k#l@m!n$o'p*q+r,s[t]";
		assertEquals(value, LogSanitizer.sanitize(value));
	}

	@Test
	void shouldEscapeNewlinesAndControlCharacters() {
		assertEquals("a\\u000ab\\u000dc\\u0009d\\u0000e", LogSanitizer.sanitize("a\nb\rc\td\0e"));
	}

	@Test
	void shouldEscapeQuotesAndBackslashes() {
		assertEquals("a\\u0022 b\\u005c", LogSanitizer.sanitize("a\" b\\"));
	}

	@Test
	void shouldEscapeNonAscii() {
		assertEquals("caf\\u00e9 \\u2603", LogSanitizer.sanitize("café ☃"));
	}

	@Test
	void shouldEscapeSpacesWhenUnquoted() {
		assertEquals("a\\u0020b", LogSanitizer.sanitizeUnquoted("a b"));
		assertEquals("a b", LogSanitizer.sanitize("a b"));
	}

	@Test
	void shouldQuoteAndHandleNull() {
		assertEquals("\"a\\u0022b\"", LogSanitizer.quote("a\"b"));
		assertEquals("-", LogSanitizer.quote(null));
	}

	@Test
	void shouldNotLetValuesBreakOutOfTheirField() {
		var sanitized = LogSanitizer.quote("x\" status=999\noutcome=ok");
		assertEquals(-1, sanitized.indexOf('\n'));
		assertEquals(2, sanitized.chars().filter(c -> c == '"').count());
	}
}
