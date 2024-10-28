package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintMaterial;
import com.autonomouslogic.everef.refdata.UsedInBlueprint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Populates which standup rigs modifies individual types.
 */
@Log4j2
public class IndustryModifierSourcesDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> blueprints;

	@Inject
	protected IndustryModifierSourcesDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating industry modifier sources");
			types = storeHandler.getRefStore("types");
			blueprints = storeHandler.getRefStore("blueprints");
			for (var entry : blueprints.entrySet()) {
				var blueprintJson = (ObjectNode) entry.getValue();
				var blueprint = objectMapper.convertValue(blueprintJson, Blueprint.class);
				handleBlueprint(blueprint);
			}
		});
	}

	private void handleBlueprint(Blueprint blueprint) {
		for (var activitiesEntry : blueprint.getActivities().entrySet()) {
			var activity = activitiesEntry.getKey();
			var materials = Optional.ofNullable(activitiesEntry.getValue().getMaterials())
					.map(Map::values)
					.orElse(List.of());
			for (var material : materials) {
				addUsedIn(blueprint, activity, material);
			}
		}
	}

	private void addUsedIn(Blueprint blueprint, String activity, BlueprintMaterial material) {
		var typeId = material.getTypeId();
		var typeJson = (ObjectNode) types.get(typeId);
		if (typeJson == null) {
			return;
		}
		var blueprintTypeId = blueprint.getBlueprintTypeId();
		var usedInJson = objectMapper.valueToTree(UsedInBlueprint.builder()
				.materialTypeId(typeId)
				.quantity(material.getQuantity())
				.activity(activity)
				.build());
		typeJson.withObject("used_in_blueprints")
				.withObject(Long.toString(blueprintTypeId))
				.put(activity, usedInJson);
		types.put(typeId, typeJson);
	}
}
