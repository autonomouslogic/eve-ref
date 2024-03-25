package com.autonomouslogic.everef.cli.structures;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.ALL_CUSTOM_PROPERTIES;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_INFORMATION_UPDATE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
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
			newNode.set(prop, node.get(prop));
		}
		newNode.put(LAST_INFORMATION_UPDATE, timestamp.toString());
		store.put(structureId, newNode);
	}

	public Flowable<Pair<Long, ObjectNode>> allStructures() {
		return Flowable.defer(() -> {
			var ids = new ArrayList<>(store.keySet());
			ids.sort(Long::compareTo);
			return Flowable.fromIterable(ids).map(id -> Pair.of(id, (ObjectNode) store.get(id)));
		});
	}
}
