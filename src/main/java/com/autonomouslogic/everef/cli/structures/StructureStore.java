package com.autonomouslogic.everef.cli.structures;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.ALL_BOOLEANS;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.ALL_CUSTOM_PROPERTIES;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_STRUCTURE_GET;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

public class StructureStore {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private Map<Long, JsonNode> store;

	@Inject
	protected StructureStore() {}

	public void put(ObjectNode node) {
		store.put(node.get(ScrapeStructures.STRUCTURE_ID).asLong(), node);
	}

	public ObjectNode getOrInitStructure(long structureId) {
		var node = (ObjectNode) store.get(structureId);
		if (node == null) {
			node = objectMapper.createObjectNode();
			node.put(ScrapeStructures.STRUCTURE_ID, structureId);
			store.put(structureId, node);
		}
		return node;
	}

	public void updateStructure(long structureId, ObjectNode newNode, Instant timestamp) {
		var node = getOrInitStructure(structureId);
		for (var prop : ALL_CUSTOM_PROPERTIES) {
			var val = node.get(prop);
			if (val != null) {
				newNode.set(prop, val);
			}
		}
		newNode.put(LAST_STRUCTURE_GET, timestamp.toString());
		store.put(structureId, newNode);
	}

	public Flowable<Pair<Long, ObjectNode>> allStructures() {
		return Flowable.defer(() -> {
			var ids = new ArrayList<>(store.keySet());
			ids.sort(Long::compareTo);
			return Flowable.fromIterable(ids).map(id -> Pair.of(id, (ObjectNode) store.get(id)));
		});
	}

	public void resetBooleans() {
		store.forEach((id, node) -> {
			for (var prop : ALL_BOOLEANS) {
				((ObjectNode) node).put(prop, false);
				store.put(id, node);
			}
		});
	}

	public void updateBoolean(long structureId, @NonNull String prop, boolean val) {
		var json = (ObjectNode) store.get(structureId);
		json.put(prop, val);
		store.put(structureId, json);
	}

	public void updateTimestamp(long structureId, @NonNull String prop, @NonNull Instant time) {
		var json = (ObjectNode) store.get(structureId);
		var current = Optional.ofNullable(json.get(prop))
				.filter(JsonNode::isTextual)
				.map(JsonNode::asText)
				.map(Instant::parse);
		if (current.isEmpty() || current.get().isBefore(time)) {
			json.put(prop, time.toString());
			store.put(structureId, json);
		}
	}
}
