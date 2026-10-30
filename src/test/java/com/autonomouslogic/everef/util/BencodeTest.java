package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class BencodeTest {
	private static Object decode(String s) {
		return Bencode.decode(s.getBytes(StandardCharsets.ISO_8859_1));
	}

	@ParameterizedTest
	@CsvSource({
		"i0e,0",
		"i42e,42",
		"i-42e,-42",
		"i9223372036854775807e,9223372036854775807",
		"i-9223372036854775808e,-9223372036854775808"
	})
	void shouldDecodeIntegers(String input, long expected) {
		assertEquals(expected, decode(input));
	}

	@Test
	void shouldDecodeByteStrings() {
		assertArrayEquals("spam".getBytes(StandardCharsets.US_ASCII), (byte[]) decode("4:spam"));
	}

	@Test
	void shouldDecodeEmptyByteString() {
		assertArrayEquals(new byte[0], (byte[]) decode("0:"));
	}

	@Test
	void shouldDecodeByteStringsContainingDelimiters() {
		assertArrayEquals("i1e:d".getBytes(StandardCharsets.US_ASCII), (byte[]) decode("5:i1e:d"));
	}

	@Test
	void shouldDecodeBinaryByteStrings() {
		var bytes = new byte[256];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) i;
		}
		assertArrayEquals(bytes, (byte[]) Bencode.decode(concat("256:".getBytes(StandardCharsets.US_ASCII), bytes)));
	}

	@Test
	void shouldDecodeLists() {
		var list = (List<?>) decode("l4:spami42ee");
		assertEquals(2, list.size());
		assertArrayEquals("spam".getBytes(StandardCharsets.US_ASCII), (byte[]) list.get(0));
		assertEquals(42L, list.get(1));
	}

	@Test
	void shouldDecodeEmptyList() {
		assertEquals(List.of(), decode("le"));
	}

	@Test
	void shouldDecodeDictionaries() {
		var map = (Map<?, ?>) decode("d3:cow3:moo4:spami7ee");
		assertEquals(List.of("cow", "spam"), List.copyOf(map.keySet()));
		assertArrayEquals("moo".getBytes(StandardCharsets.US_ASCII), (byte[]) map.get("cow"));
		assertEquals(7L, map.get("spam"));
	}

	@Test
	void shouldDecodeEmptyDictionary() {
		assertEquals(Map.of(), decode("de"));
	}

	@Test
	void shouldDecodeNestedStructures() {
		var map = (Map<?, ?>) decode("d4:listld1:ai1eeli2ei3eee4:dictd5:innerdeee");
		var list = (List<?>) map.get("list");
		assertEquals(Map.of("a", 1L), list.get(0));
		assertEquals(List.of(2L, 3L), list.get(1));
		assertEquals(Map.of("inner", Map.of()), map.get("dict"));
	}

	@Test
	void shouldRecoverBinaryDictionaryKeys() {
		var hash = HexFormat.of().parseHex("60a2240f3d29b71e679d9f1bc55cd474f567b346");
		var input =
				concat("d20:".getBytes(StandardCharsets.US_ASCII), hash, "i1ee".getBytes(StandardCharsets.US_ASCII));
		var map = (Map<?, ?>) Bencode.decode(input);
		var key = (String) map.keySet().iterator().next();
		assertArrayEquals(hash, key.getBytes(StandardCharsets.ISO_8859_1));
		assertEquals(1L, map.get(key));
	}

	@Test
	void shouldDecodeTrackerScrapeResponse() {
		var hash1 = HexFormat.of().parseHex("60a2240f3d29b71e679d9f1bc55cd474f567b346");
		var hash2 = HexFormat.of().parseHex("2ee41593454ef093e57f06dab430760ff0264d0c");
		var input = concat(
				"d5:filesd20:".getBytes(StandardCharsets.US_ASCII),
				hash1,
				"d8:completei11e10:downloadedi11e10:incompletei20ee20:".getBytes(StandardCharsets.US_ASCII),
				hash2,
				"d8:completei1e10:downloadedi0e10:incompletei25eeee".getBytes(StandardCharsets.US_ASCII));
		var files = (Map<?, ?>) ((Map<?, ?>) Bencode.decode(input)).get("files");
		assertEquals(2, files.size());
		var stats1 = (Map<?, ?>) files.get(new String(hash1, StandardCharsets.ISO_8859_1));
		var stats2 = (Map<?, ?>) files.get(new String(hash2, StandardCharsets.ISO_8859_1));
		assertEquals(Map.of("complete", 11L, "downloaded", 11L, "incomplete", 20L), stats1);
		assertEquals(Map.of("complete", 1L, "downloaded", 0L, "incomplete", 25L), stats2);
	}

	@Test
	void shouldDecodeFailureReason() {
		var map = (Map<?, ?>) decode("d14:failure reason11:not allowede");
		assertInstanceOf(byte[].class, map.get("failure reason"));
		assertEquals("not allowed", new String((byte[]) map.get("failure reason"), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@ValueSource(
			strings = {
				"",
				"i",
				"i42",
				"ie",
				"iabce",
				"i1.5e",
				"4:spa",
				"4spam",
				"-1:a",
				"l",
				"li1e",
				"d",
				"d3:cow",
				"d3:cowe",
				"di1ei2ee",
				"x",
				"e",
				"i1ei2e",
				"4:spamextra",
				"99999999999:a"
			})
	void shouldRejectMalformedInput(String input) {
		assertThrows(IllegalArgumentException.class, () -> decode(input));
	}

	@SneakyThrows
	private static byte[] concat(byte[]... parts) {
		var out = new ByteArrayOutputStream();
		for (var part : parts) {
			out.write(part);
		}
		return out.toByteArray();
	}
}
