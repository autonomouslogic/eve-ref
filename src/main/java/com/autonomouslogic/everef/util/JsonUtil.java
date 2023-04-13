package com.autonomouslogic.everef.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

public class JsonUtil {
	public static boolean isNull(JsonNode node) {
		return node == null || node.isNull();
	}

	public static boolean isNullOrEmpty(JsonNode node) {
		return isNull(node) || node.asText().isEmpty();
	}

	public static boolean toBoolean(JsonNode node) {
		return !isNull(node) && node.asBoolean();
	}

	public static int compareLongs(JsonNode a, long b) {
		return compareLongs(a, LongNode.valueOf(b));
	}

	public static int compareLongs(long a, JsonNode b) {
		return compareLongs(LongNode.valueOf(a), b);
	}

	public static int compareLongs(JsonNode a, JsonNode b) {
		var aNull = isNull(a);
		var bNull = isNull(b);
		if (aNull && bNull) {
			return 0;
		} else if (aNull) {
			return -1;
		} else if (bNull) {
			return 1;
		} else {
			return Long.compare(a.asLong(), b.asLong());
		}
	}
}
