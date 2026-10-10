package com.autonomouslogic.everef.dataserver;

import java.io.ByteArrayOutputStream;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Resolves a client's raw, undecoded request path into a B2 object key, or rejects it as a bad request.
 * Kept separate from {@code DataProxyHandler} so the path-safety and key-encoding logic can be unit tested
 * without HTTP. See data-server-plan.md step 3.
 */
public class KeyResolver {
	/**
	 * B2's file-name limit, in UTF-8 bytes.
	 */
	public static final int MAX_KEY_BYTES = 1024;

	private KeyResolver() {}

	/**
	 * Resolves a raw (undecoded, unnormalized) request path to a B2 key.
	 *
	 * @return the resolved key, or empty if the path is invalid and the request should be rejected with 400.
	 */
	public static Optional<String> resolve(String rawPath) {
		if (rawPath == null || !rawPath.startsWith("/")) {
			return Optional.empty();
		}
		// Split keeping empty trailing segments, so "/a/" -> ["", "a", ""] and "/" -> ["", ""].
		var rawSegments = rawPath.split("/", -1);
		// rawSegments[0] is always "" (before the leading slash).
		var decodedSegments = new java.util.ArrayList<String>(rawSegments.length - 1);
		for (int i = 1; i < rawSegments.length; i++) {
			var raw = rawSegments[i];
			var isLast = i == rawSegments.length - 1;
			if (raw.isEmpty()) {
				// An empty segment is only valid as the final (trailing-slash) segment.
				if (!isLast) {
					return Optional.empty();
				}
				continue;
			}
			var decoded = decodeSegment(raw);
			if (decoded.isEmpty()) {
				return Optional.empty();
			}
			var segment = decoded.get();
			if (segment.equals(".") || segment.equals("..")) {
				return Optional.empty();
			}
			if (segment.indexOf('/') >= 0 || segment.indexOf('\\') >= 0 || segment.indexOf('\0') >= 0) {
				return Optional.empty();
			}
			decodedSegments.add(segment);
		}
		var endsWithSlash = rawSegments[rawSegments.length - 1].isEmpty();
		String key;
		if (decodedSegments.isEmpty()) {
			key = "index.html";
		} else if (endsWithSlash) {
			key = String.join("/", decodedSegments) + "/index.html";
		} else {
			key = String.join("/", decodedSegments);
		}
		if (key.getBytes(StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
			return Optional.empty();
		}
		return Optional.of(key);
	}

	/**
	 * Percent-decodes a single path segment as raw bytes, then strictly decodes those bytes as UTF-8.
	 * Deliberately does not use {@code URLDecoder}, which turns {@code +} into a space.
	 */
	private static Optional<String> decodeSegment(String raw) {
		var bytes = new ByteArrayOutputStream(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			var c = raw.charAt(i);
			if (c == '%') {
				if (i + 2 >= raw.length()) {
					return Optional.empty();
				}
				var hi = Character.digit(raw.charAt(i + 1), 16);
				var lo = Character.digit(raw.charAt(i + 2), 16);
				if (hi < 0 || lo < 0) {
					return Optional.empty();
				}
				bytes.write((hi << 4) | lo);
				i += 2;
			} else if (c < 128) {
				bytes.write(c);
			} else {
				return Optional.empty();
			}
		}
		try {
			var decoder = StandardCharsets.UTF_8
					.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			return Optional.of(decoder.decode(java.nio.ByteBuffer.wrap(bytes.toByteArray()))
					.toString());
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
