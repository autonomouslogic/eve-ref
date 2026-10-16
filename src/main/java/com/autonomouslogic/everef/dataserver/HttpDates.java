package com.autonomouslogic.everef.dataserver;

import io.helidon.http.DateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Parses HTTP-date header values (RFC 9110 §5.6.7: IMF-fixdate, RFC 850 and asctime), truncated to whole
 * seconds since HTTP dates carry no sub-second precision.
 */
public class HttpDates {
	private HttpDates() {}

	/**
	 * Parses a single HTTP-date. Returns empty if the value isn't a valid HTTP-date.
	 */
	public static Optional<Instant> parse(String value) {
		if (value == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(DateTime.parse(value).toInstant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Parses a conditional date header's raw values per RFC 9110 §13.1.3/§13.1.4: the header is ignored
	 * (treated as absent) unless there is exactly one line and it parses as a valid HTTP-date.
	 */
	public static Optional<Instant> parseSingleValuedHeader(List<String> rawValues) {
		if (rawValues == null || rawValues.size() != 1) {
			return Optional.empty();
		}
		return parse(rawValues.get(0));
	}
}
