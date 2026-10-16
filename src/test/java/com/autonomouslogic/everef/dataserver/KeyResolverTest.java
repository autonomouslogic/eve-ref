package com.autonomouslogic.everef.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class KeyResolverTest {
	@ParameterizedTest
	@CsvSource({
		"/, index.html",
		"/dir/, dir/index.html",
		"/a/b/, a/b/index.html",
		"/file.txt, file.txt",
		"/a/b.txt, a/b.txt",
		"/file..txt, file..txt",
	})
	void shouldResolveValidPaths(String rawPath, String expectedKey) {
		var result = KeyResolver.resolve(rawPath);
		assertTrue(result.isPresent(), rawPath);
		assertEquals(expectedKey, result.get());
	}

	@ParameterizedTest
	@ValueSource(
			strings = {
				"/../x",
				"/%2e%2e/x",
				"/a/./b",
				"/a/../b",
				"/a%2Fb",
				"/a%5Cb",
				"/a%00b",
				"//x",
				"/a//b",
				// invalid percent-encoding
				"/%zz",
				"/%4",
				"/%4z",
				// invalid UTF-8 continuation byte
				"/%C3%28",
			})
	void shouldRejectUnsafeOrMalformedPaths(String rawPath) {
		var result = KeyResolver.resolve(rawPath);
		assertTrue(result.isEmpty(), rawPath);
	}

	@Test
	void shouldRejectSoloDotOrDotDotSegment() {
		assertTrue(KeyResolver.resolve("/.").isEmpty());
		assertTrue(KeyResolver.resolve("/..").isEmpty());
	}

	@Test
	void shouldAllowPlusAsLiteralCharacter() {
		var result = KeyResolver.resolve("/a+b.txt");
		assertTrue(result.isPresent());
		assertEquals("a+b.txt", result.get());
	}

	@Test
	void shouldDecodeSpaceFromPercentEncoding() {
		var result = KeyResolver.resolve("/a%20b.txt");
		assertTrue(result.isPresent());
		assertEquals("a b.txt", result.get());
	}

	@Test
	void shouldDecodeMultiByteUtf8() {
		var result = KeyResolver.resolve("/%C3%A6.txt");
		assertTrue(result.isPresent());
		assertEquals("æ.txt", result.get());
	}

	@Test
	void shouldRejectKeyLongerThan1024Utf8Bytes() {
		var longSegment = "a".repeat(1025);
		var result = KeyResolver.resolve("/" + longSegment);
		assertTrue(result.isEmpty());
	}

	@Test
	void shouldAllowKeyOfExactly1024Utf8Bytes() {
		var segment = "a".repeat(1024);
		var result = KeyResolver.resolve("/" + segment);
		assertTrue(result.isPresent());
		assertEquals(1024, result.get().getBytes(StandardCharsets.UTF_8).length);
	}
}
