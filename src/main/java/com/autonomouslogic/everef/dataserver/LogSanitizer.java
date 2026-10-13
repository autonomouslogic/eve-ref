package com.autonomouslogic.everef.dataserver;

import java.util.regex.Pattern;

/**
 * Makes client-supplied values safe to write into the key=value access log. Anything outside a small allowlist,
 * including backslashes, quotes, control characters, newlines and non-ASCII, is replaced by a {@code \}{@code uXXXX}
 * escape, so a client can never forge extra fields or log lines and the escaping is unambiguous.
 */
public class LogSanitizer {
	private static final Pattern DISALLOWED = Pattern.compile("[^A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%\\- ]");
	private static final Pattern DISALLOWED_NO_SPACE = Pattern.compile("[^A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%\\-]");

	private LogSanitizer() {}

	/**
	 * Escapes a value that is written inside double quotes, so spaces are allowed.
	 */
	public static String sanitize(String value) {
		return escape(DISALLOWED, value);
	}

	/**
	 * Escapes a value that is written unquoted, so spaces are escaped too.
	 */
	public static String sanitizeUnquoted(String value) {
		return escape(DISALLOWED_NO_SPACE, value);
	}

	/**
	 * Sanitizes and double-quotes a possibly-missing value. Missing values are logged as {@code -}.
	 */
	public static String quote(String value) {
		return value == null ? "-" : "\"" + sanitize(value) + "\"";
	}

	private static String escape(Pattern disallowed, String value) {
		return disallowed
				.matcher(value)
				.replaceAll(m -> String.format("\\\\u%04x", (int) m.group().charAt(0)));
	}
}
