package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

class JsonUtilTest {
	@Test
	void shouldCheckNulls() {
		assertTrue(JsonUtil.isNull(NullNode.getInstance()));
		assertTrue(JsonUtil.isNull(null));
		assertFalse(JsonUtil.isNull(TextNode.valueOf("")));
		assertFalse(JsonUtil.isNull(TextNode.valueOf("null")));
		assertFalse(JsonUtil.isNull(BooleanNode.getFalse()));
		assertFalse(JsonUtil.isNull(LongNode.valueOf(0)));
	}

	@Test
	void shouldCheckEmpty() {
		assertTrue(JsonUtil.isNullOrEmpty(NullNode.getInstance()));
		assertTrue(JsonUtil.isNullOrEmpty(null));
		assertTrue(JsonUtil.isNullOrEmpty(TextNode.valueOf("")));
		assertFalse(JsonUtil.isNullOrEmpty(TextNode.valueOf("null")));
		assertFalse(JsonUtil.isNullOrEmpty(BooleanNode.getFalse()));
		assertFalse(JsonUtil.isNullOrEmpty(LongNode.valueOf(0)));
	}

	@Test
	void shouldConvertToBooleans() {
		assertFalse(JsonUtil.toBoolean(BooleanNode.getFalse()));
		assertTrue(JsonUtil.toBoolean(BooleanNode.getTrue()));
		assertFalse(JsonUtil.toBoolean(TextNode.valueOf("false")));
		assertTrue(JsonUtil.toBoolean(TextNode.valueOf("true")));
		assertFalse(JsonUtil.toBoolean(NullNode.getInstance()));
		assertFalse(JsonUtil.toBoolean(null));
		assertFalse(JsonUtil.toBoolean(TextNode.valueOf("")));
		assertFalse(JsonUtil.toBoolean(TextNode.valueOf("null")));
		assertFalse(JsonUtil.toBoolean(LongNode.valueOf(0)));
		assertTrue(JsonUtil.toBoolean(LongNode.valueOf(1)));
		assertFalse(JsonUtil.toBoolean(TextNode.valueOf("other")));
	}

	@Test
	void shouldCompareLongs() {
		assertEquals(0, JsonUtil.compareLongs(LongNode.valueOf(0), LongNode.valueOf(0)));
		assertEquals(1, JsonUtil.compareLongs(LongNode.valueOf(1), LongNode.valueOf(0)));
		assertEquals(-1, JsonUtil.compareLongs(LongNode.valueOf(0), LongNode.valueOf(1)));
		assertEquals(1, JsonUtil.compareLongs(LongNode.valueOf(1), null));
		assertEquals(1, JsonUtil.compareLongs(LongNode.valueOf(1), NullNode.getInstance()));
		assertEquals(-1, JsonUtil.compareLongs(null, LongNode.valueOf(1)));
		assertEquals(-1, JsonUtil.compareLongs(NullNode.getInstance(), LongNode.valueOf(1)));

		assertEquals(0, JsonUtil.compareLongs(LongNode.valueOf(0), 0));
		assertEquals(-1, JsonUtil.compareLongs(LongNode.valueOf(0), 1));
		assertEquals(0, JsonUtil.compareLongs(0, LongNode.valueOf(0)));
		assertEquals(1, JsonUtil.compareLongs(1, LongNode.valueOf(0)));
	}
}
