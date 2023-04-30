package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

/**
 * Merges Ref Data objects from SDE, ESI, and Hoboleaks together into the final dataset.
 */
@Singleton
@Log4j2
public class ObjectMerger {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ObjectMerger() {}

	public JsonNode merge(JsonNode... nodes) {
		if (nodes.length == 0) {
			throw new NullPointerException();
		}
		checkNotNull(nodes[0], 0);
		if (nodes[0].isObject()) {
			return mergeObjects(castToObjects(nodes));
		} else if (nodes[0].isArray()) {
			return mergeArrays(castToArrays(nodes));
		} else {
			return nodes[nodes.length - 1];
		}
	}

	private ObjectNode mergeObjects(ObjectNode... objects) {
		var merged = objectMapper.createObjectNode();
		var fields = Stream.of(objects)
				.flatMap(o -> Streams.stream(o.fields()))
				.collect(Collectors.groupingBy(e -> e.getKey(), LinkedHashMap::new, Collectors.toList()));
		fields.forEach((key, entries) -> {
			var nodes = entries.stream().map(e -> e.getValue()).toArray(JsonNode[]::new);
			var mergedEntry = merge(nodes);
			merged.set(key, mergedEntry);
		});
		return merged;
	}

	private ArrayNode mergeArrays(ArrayNode... objs) {
		var merged = objectMapper.createArrayNode();
		for (var obj : objs) {
			merged.addAll(obj);
		}
		return merged;
	}

	private ObjectNode[] castToObjects(JsonNode[] nodes) {
		var objs = new ObjectNode[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			var node = nodes[i];
			checkNotNull(node, i);
			if (node.isObject()) {
				objs[i] = (ObjectNode) node;
			} else {
				throw new IllegalArgumentException("Arg " + i + " is not an object: " + node.getNodeType());
			}
		}
		return objs;
	}

	private ArrayNode[] castToArrays(JsonNode[] nodes) {
		var arrays = new ArrayNode[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			var node = nodes[i];
			checkNotNull(node, i);
			if (node.isArray()) {
				arrays[i] = (ArrayNode) node;
			} else {
				throw new IllegalArgumentException("Arg " + i + " is not an array");
			}
		}
		return arrays;
	}

	private void checkNotNull(JsonNode node, int arg) {
		if (node == null) {
			throw new NullPointerException("Arg " + arg + " is null");
		}
	}
}
