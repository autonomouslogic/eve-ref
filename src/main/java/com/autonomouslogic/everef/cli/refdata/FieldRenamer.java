package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility for merging and renaming of JSON objects in the Ref Data build.
 */
@Singleton
public class FieldRenamer implements SimpleTransformer {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected FieldRenamer() {}

	/**
	 * Renames field names from the SDE format to the Ref Data format.
	 * @param field
	 * @return
	 */
	public String fieldRenameFromSde(String field) {
		if (field.endsWith("ID")) {
			field = field.substring(0, field.length() - 2) + "Id";
		}
		else if (field.endsWith("IDs")) {
			field = field.substring(0, field.length() - 3) + "Ids";
		}
		return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field);
	}

	/**
	 * Recursively renames all the fields in a JSON node from the SDE format to the Ref Data format.
	 * @param node
	 * @return
	 */
	public JsonNode fieldRenameFromSde(JsonNode node) {
		if (node.isObject()) {
			var obj = (ObjectNode) node;
			var newObj = objectMapper.createObjectNode();
			obj.fields().forEachRemaining(entry -> {
				var newField = fieldRenameFromSde(entry.getKey());
				var newNode = fieldRenameFromSde(entry.getValue());
				newObj.set(newField, newNode);
			});
			return newObj;
		} else if (node.isArray()) {
			var array = (ArrayNode) node;
			var n = array.size();
			for (int i = 0; i < n; i++) {
				array.set(i, fieldRenameFromSde(array.get(i)));
			}
			return array;
		} else {
			return node;
		}
	}

	@Override
	public ObjectNode transformJson(ObjectNode json, String language) throws Throwable {
		return (ObjectNode) fieldRenameFromSde(json);
	}
}
