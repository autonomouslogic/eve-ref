package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.NonNull;
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

	public void renameField(ObjectNode root, String from, String to) {
		if (!root.has(from)) {
			return;
		}
		root.set(to, root.get(from));
		root.remove(from);
	}

	public static SimpleTransformer concat(SimpleTransformer... transformers) {
		if (transformers.length == 0) {
			return (json, language) -> json;
		}
		return (v, language) -> {
			for (var f : transformers) {
				v = f.transformJson(v, language);
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

	public void remove(ObjectNode root, String attr) {
		root.remove(attr);
	}

	public void listToCoordinate(@NonNull JsonNode obj, @NonNull String field) {
		if (obj.has(field)) {
			((ObjectNode) obj).set(field, listToCoordinate(obj.get(field)));
		}
	}

	public ObjectNode listToCoordinate(@NonNull JsonNode list) {
		if (!list.isArray()) {
			throw new IllegalArgumentException("List must be an array: " + list);
		}
		if (list.size() != 3) {
			throw new IllegalArgumentException("List must have exactly two elements: " + list);
		}
		return objectMapper.createObjectNode()
			.put("x", list.get(0).asLong())
			.put("y", list.get(1).asLong())
			.put("z", list.get(2).asLong());
	}
}
