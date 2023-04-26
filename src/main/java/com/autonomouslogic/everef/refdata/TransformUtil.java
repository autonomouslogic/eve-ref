package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class TransformUtil {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TransformUtil() {}

	public void arrayToObject(ObjectNode root, String field, String keyField) {
		if (!root.has(field)) {
			return;
		}
		if (!root.get(field).isArray()) {
			throw new RuntimeException(String.format(
					"%s is not an array: %s", field, root.get(field).getNodeType()));
		}
		var array = (ArrayNode) root.get(field);
		var obj = objectMapper.createObjectNode();
		for (JsonNode entry : array) {
			var key = entry.get(keyField).asText();
			obj.set(key, entry);
		}
		root.set(field, obj);
	}
}
