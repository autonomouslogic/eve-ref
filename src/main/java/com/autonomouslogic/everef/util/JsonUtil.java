package com.autonomouslogic.everef.util;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonUtil {
	public static boolean isNull(JsonNode node) {
		return node == null || node.isNull();
	}

	public static boolean isNullOrEmpty(JsonNode node) {
		return isNull(node) || node.asText().isEmpty();
	}
}
