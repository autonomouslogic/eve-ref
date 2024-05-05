package com.autonomouslogic.everef.cli.structures;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.ALL_BOOLEANS;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.ALL_CUSTOM_PROPERTIES;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.FIRST_SEEN;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_STRUCTURE_GET;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.STRUCTURE_ID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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

	@Setter
	private ZonedDateTime scrapeTime;

	@Inject
	protected StructureStore() {}

	public void put(ObjectNode node) {
		store.put(node.get(STRUCTURE_ID).asLong(), node);
	}

	public ObjectNode getOrInitStructure(long structureId) {
		var node = (ObjectNode) store.get(structureId);
		if (node == null) {
			node = objectMapper.createObjectNode();
			node.put(STRUCTURE_ID, structureId);
			node.put(FIRST_SEEN, scrapeTime.toInstant().toString());
			for (var prop : ALL_BOOLEANS) {
				node.put(prop, false);
			}
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
			var ids = getAllIds();
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
		var json = getOrInitStructure(structureId);
		json.put(prop, val);
		store.put(structureId, json);
	}

	public void updateTimestamp(long structureId, @NonNull String prop, @NonNull Instant time) {
		var json = getOrInitStructure(structureId);
		var current = Optional.ofNullable(json.get(prop))
				.filter(JsonNode::isTextual)
				.map(JsonNode::asText)
				.map(Instant::parse);
		if (current.isEmpty() || current.get().isBefore(time)) {
			json.put(prop, time.toString());
			store.put(structureId, json);
		}
	}

	public int removeAllIf(@NonNull Predicate<ObjectNode> predicate) {
		int r = 0;
		for (long structureId : getAllIds()) {
			var node = (ObjectNode) store.get(structureId);
			if (predicate.test(node)) {
				store.remove(structureId);
				r++;
			}
		}
		return r;
	}

	public List<Long> getAllIds() {
		var ids = new ArrayList<>(store.keySet());

		ids.sort(Long::compareTo);
		return ids;
	}
}
