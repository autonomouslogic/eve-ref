package com.autonomouslogic.everef.refdata.sde;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.functions.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class SdeTypeTransformer implements Function<ObjectNode, ObjectNode> {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected SdeTypeTransformer() {}

	@Override
	public ObjectNode apply(ObjectNode json) throws Throwable {
		if (json.has("traits")) {
			var traits = (ObjectNode) json.get("traits");
			handleArrayToObjectConversion(traits, "misc_bonuses", "importance");
			handleArrayToObjectConversion(traits, "role_bonuses", "importance");
			if (traits.has("types")) {
				var types = (ObjectNode) traits.get("types");
				types.fields().forEachRemaining(pair -> {
					handleArrayToObjectConversion(types, pair.getKey(), "importance");
				});
			}
		}
		return json;
	}

	private void handleArrayToObjectConversion(ObjectNode root, String field, String keyField) {
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
