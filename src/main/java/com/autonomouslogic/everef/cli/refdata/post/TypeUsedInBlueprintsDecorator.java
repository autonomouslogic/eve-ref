package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintMaterial;
import com.autonomouslogic.everef.refdata.UsedInBlueprint;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Populates on inventory types which blueprints they're used in.
 */
@Log4j2
public class TypeUsedInBlueprintsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> blueprints;

	@Inject
	protected TypeUsedInBlueprintsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating type usedInBlueprints");
			types = storeHandler.getRefStore("types");
			blueprints = storeHandler.getRefStore("blueprints");
			for (var entry : blueprints.entrySet()) {
				var blueprintJson = (ObjectNode) entry.getValue();
				var blueprint = jsonMapper.convertValue(blueprintJson, Blueprint.class);
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
		var usedInJson = jsonMapper.valueToTree(UsedInBlueprint.builder()
				.materialTypeId(typeId)
				.quantity(material.getQuantity())
				.activity(activity)
				.build());
		typeJson.withObject("used_in_blueprints")
				.withObject(Long.toString(blueprintTypeId))
				.set(activity, usedInJson);
		types.put(typeId, typeJson);
	}
}
