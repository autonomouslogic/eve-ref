package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BlueprintDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private StoreDataHelper helper;
	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> blueprints;

	@Inject
	protected BlueprintDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating blueprints");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			blueprints = storeHandler.getRefStore("blueprints");
			for (var blueprintsEntry : blueprints.entrySet()) {
				long blueprintTypeId = blueprintsEntry.getKey();
				setIsBlueprint(blueprintTypeId);
				var blueprintJson = (ObjectNode) blueprintsEntry.getValue();
				var blueprint = objectMapper.convertValue(blueprintJson, Blueprint.class);
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
		obj.put(
				Long.toString(blueprintTypeId),
				objectMapper
						.createObjectNode()
						.put("blueprint_type_id", blueprintTypeId)
						.put("blueprint_activity", activity));
		types.put(productTypeId, productType);
	}
}
