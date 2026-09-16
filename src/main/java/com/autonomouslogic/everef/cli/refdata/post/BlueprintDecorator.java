package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.refdata.Blueprint;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Log4j2
public class BlueprintDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	private StoreDataHelper helper;
	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> blueprints;

	@Inject
	protected BlueprintDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating blueprints");
			helper = new StoreDataHelper(storeHandler, jsonMapper);
			types = storeHandler.getRefStore("types");
			blueprints = storeHandler.getRefStore("blueprints");
			for (var blueprintsEntry : blueprints.entrySet()) {
				long blueprintTypeId = blueprintsEntry.getKey();
				setIsBlueprint(blueprintTypeId);
				var blueprintJson = (ObjectNode) blueprintsEntry.getValue();
				var blueprint = jsonMapper.convertValue(blueprintJson, Blueprint.class);
				for (var activitiesEntry : blueprint.getActivities().entrySet()) {
					var products = Optional.ofNullable(
									activitiesEntry.getValue().getProducts())
							.map(Map::values)
							.orElse(List.of());
					for (var product : products) {
						addProducedBy(blueprintTypeId, product.getTypeId(), activitiesEntry.getKey());
					}
				}
			}
		});
	}

	private void setIsBlueprint(long typeId) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			log.warn("Could not set type {} as being a blueprint, type not found", typeId);
			return;
		}
		type.put("is_blueprint", true);
		types.put(typeId, type);
	}

	private void addProducedBy(long blueprintTypeId, long productTypeId, String activity) {
		var productType = (ObjectNode) types.get(productTypeId);
		if (productType == null) {
			log.warn(
					"Could not set type {} as being created by blueprint {}, type not found",
					productTypeId,
					blueprintTypeId);
			return;
		}
		var obj = productType.withObject("/produced_by_blueprints");
		obj.set(
				Long.toString(blueprintTypeId),
				jsonMapper
						.createObjectNode()
						.put("blueprint_type_id", blueprintTypeId)
						.put("blueprint_activity", activity));
		types.put(productTypeId, productType);
	}
}
