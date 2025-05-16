package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Populates on inventory types which blueprints they're used in.
 */
@Log4j2
public class DogmaDefinedOnTypesDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private Map<Long, JsonNode> dogmaAttrs;
	private Map<Long, JsonNode> dogmaEffects;
	private Map<Long, JsonNode> types;

	@Inject
	protected DogmaDefinedOnTypesDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating type usedInBlueprints");
			dogmaAttrs = storeHandler.getRefStore("dogmaAttributes");
			dogmaEffects = storeHandler.getRefStore("dogmaEffects");
			types = storeHandler.getRefStore("types");
			for (var entry : types.entrySet()) {
				var typeJson = (ObjectNode) entry.getValue();
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				handleAttrs(type);
				handleEffects(type);
			}
		});
	}

	private void handleAttrs(InventoryType type) {
		var attributes = type.getDogmaAttributes();
		if (attributes == null || attributes.isEmpty()) {
			return;
		}
		for (var entry : attributes.entrySet()) {
			var typeAttr = entry.getValue();
			addDefinedOn(dogmaAttrs, "attribute", type.getTypeId(), typeAttr.getAttributeId());
		}
	}

	private void handleEffects(InventoryType type) {
		var effects = type.getDogmaEffects();
		if (effects == null || effects.isEmpty()) {
			return;
		}
		for (var entry : effects.entrySet()) {
			var typeEffect = entry.getValue();
			addDefinedOn(dogmaEffects, "effect", type.getTypeId(), typeEffect.getEffectId());
		}
	}

	private void addDefinedOn(Map<Long, JsonNode> map, String type, long typeId, long attributeId) {
		var json = map.get(attributeId);
		if (json == null) {
			log.warn("Unable to add type {} on dogma {} {}, attribute not found", typeId, type, attributeId);
			return;
		}
		((ArrayNode) json.withArray("defined_on_type_ids")).add(typeId);
		map.put(attributeId, json);
	}
}
