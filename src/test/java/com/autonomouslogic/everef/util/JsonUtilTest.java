package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class JsonUtilTest {
	@Test
	void shouldCheckNulls() {
		assertTrue(JsonUtil.isNull(NullNode.getInstance()));
		assertTrue(JsonUtil.isNull(null));
		assertFalse(JsonUtil.isNull(StringNode.valueOf("")));
		assertFalse(JsonUtil.isNull(StringNode.valueOf("null")));
		assertFalse(JsonUtil.isNull(BooleanNode.getFalse()));
		assertFalse(JsonUtil.isNull(LongNode.valueOf(0)));
	}

	@Test
	void shouldCheckEmpty() {
		assertTrue(JsonUtil.isNullOrEmpty(NullNode.getInstance()));
		assertTrue(JsonUtil.isNullOrEmpty(null));
		assertTrue(JsonUtil.isNullOrEmpty(StringNode.valueOf("")));
		assertFalse(JsonUtil.isNullOrEmpty(StringNode.valueOf("null")));
		assertFalse(JsonUtil.isNullOrEmpty(BooleanNode.getFalse()));
		assertFalse(JsonUtil.isNullOrEmpty(LongNode.valueOf(0)));
	}

	@Test
	void shouldConvertToBooleans() {
		assertFalse(JsonUtil.toBoolean(BooleanNode.getFalse()));
		assertTrue(JsonUtil.toBoolean(BooleanNode.getTrue()));
		assertFalse(JsonUtil.toBoolean(StringNode.valueOf("false")));
		assertTrue(JsonUtil.toBoolean(StringNode.valueOf("true")));
		assertFalse(JsonUtil.toBoolean(NullNode.getInstance()));
		assertFalse(JsonUtil.toBoolean(null));
		assertFalse(JsonUtil.toBoolean(StringNode.valueOf("")));
		assertFalse(JsonUtil.toBoolean(StringNode.valueOf("null")));
		assertFalse(JsonUtil.toBoolean(LongNode.valueOf(0)));
		assertTrue(JsonUtil.toBoolean(LongNode.valueOf(1)));
		assertFalse(JsonUtil.toBoolean(StringNode.valueOf("other")));
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
