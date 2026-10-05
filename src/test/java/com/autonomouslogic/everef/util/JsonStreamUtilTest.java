package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JsonStreamUtilTest {
	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	@SneakyThrows
	void shouldStreamAllTopLevelEntries() {
		var json = "{\"1\":{\"a\":1},\"2\":[1,2,3],\"3\":\"hello\"}";

		List<Pair<String, JsonNode>> entries;
		try (var stream = JsonStreamUtil.streamObjectEntries(mapper, toStream(json))) {
			entries = stream.collect(Collectors.toList());
		}

		assertEquals(3, entries.size());
		assertEquals("1", entries.get(0).getLeft());
		assertEquals(1, entries.get(0).getRight().get("a").asInt());
		assertEquals("2", entries.get(1).getLeft());
		assertTrue(entries.get(1).getRight().isArray());
		assertEquals(3, entries.get(1).getRight().size());
		assertEquals("3", entries.get(2).getLeft());
		assertEquals("hello", entries.get(2).getRight().asString());
	}

	@Test
	@SneakyThrows
	void shouldReturnEmptyStreamForEmptyObject() {
		List<Pair<String, JsonNode>> entries;
		try (var stream = JsonStreamUtil.streamObjectEntries(mapper, toStream("{}"))) {
			entries = stream.collect(Collectors.toList());
		}

		assertEquals(0, entries.size());
	}

	@Test
	void shouldThrowWhenRootIsNotAnObject() {
		assertThrows(
				IllegalStateException.class, () -> JsonStreamUtil.streamObjectEntries(mapper, toStream("[1,2,3]")));
	}

	@Test
	@SneakyThrows
	void shouldNotCloseUnderlyingStreamOnEarlyClose() {
		var json = "{\"1\":1,\"2\":2,\"3\":3}";
		var spied = spy(toStream(json));

		try (var stream = JsonStreamUtil.streamObjectEntries(mapper, spied)) {
			stream.limit(1).collect(Collectors.toList());
		}

		verify(spied, never()).close();
	}

	private static ByteArrayInputStream toStream(String json) {
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
