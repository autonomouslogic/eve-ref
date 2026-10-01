package com.autonomouslogic.everef.http;

import java.time.Duration;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Parsing for the {@code X-Esi-Error-Limit-Remain} and {@code X-Esi-Error-Limit-Reset} headers, shared by
 * {@link EsiLimitExceededInterceptor} and {@link EsiErrorLimitBudgetInterceptor}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EsiErrorLimitHeaders {
	public static final String RESET_TIME_HEADER = "X-Esi-Error-Limit-Reset";
	public static final String REMAIN_HEADER = "X-Esi-Error-Limit-Remain";
	public static final String ESI_420_TEXT = "This software has exceeded the error limit for ESI.";

	public static Optional<Integer> parseRemain(String value) {
		return parseNonNegativeInt(value);
	}

	public static Optional<Duration> parseReset(String value) {
		return parseNonNegativeInt(value).map(Duration::ofSeconds);
	}

	private static Optional<Integer> parseNonNegativeInt(String value) {
		if (value == null) {
			return Optional.empty();
		}
		try {
			var parsed = Integer.parseInt(value.trim());
			return parsed < 0 ? Optional.empty() : Optional.of(parsed);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
