package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
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

	public void setPath(ObjectNode root, JsonNode value, String... path) {
		var step = path[0];
		if (path.length == 1) {
			root.set(step, value);
		} else {
			JsonNode stepNode = root.get(step);
			if (stepNode == null || !stepNode.isObject()) {
				stepNode = objectMapper.createObjectNode();
				root.set(step, stepNode);
			}
			String[] subPath = new String[path.length - 1];
			System.arraycopy(path, 1, subPath, 0, path.length - 1);
			setPath((ObjectNode) stepNode, value, subPath);
		}
	}

	public static SimpleTransformer concat(SimpleTransformer... transformers) {
		if (transformers.length == 0) {
			throw new NullPointerException();
		}
		return (v) -> {
			for (var f : transformers) {
				v = f.transformJson(v);
			}
			return v;
		};
	}

	public ObjectNode orderKeys(ObjectNode json) {
		var fields = new ArrayList<String>(json.size());
		json.fieldNames().forEachRemaining(fields::add);
		fields.sort(String::compareTo);
		var newJson = objectMapper.createObjectNode();
		for (var field : fields) {
			newJson.set(field, json.get(field));
		}
		return newJson;
	}
}
