package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.BlueprintMaterial;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVMap;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Log4j2
public class BlueprintDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private MVMap<Long, JsonNode> types;
	private MVMap<Long, JsonNode> blueprints;

	@Inject
	protected BlueprintDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating blueprints");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			blueprints = storeHandler.getRefStore("blueprints");
			for (var blueprintsEntry : blueprints.entrySet()) {
				long typeId = blueprintsEntry.getKey();
				setIsBlueprint(typeId);
				var blueprintJson = (ObjectNode) blueprintsEntry.getValue();
				var blueprint = objectMapper.convertValue(blueprintJson, Blueprint.class);
				for (var activitiesEntry : blueprint.getActivities().entrySet()) {
					var products = Optional.ofNullable(activitiesEntry.getValue().getProducts())
						.map(Map::values)
						.orElse(List.of());
					for (var product : products) {
						addProducedBy(typeId, product.getTypeId(), activitiesEntry.getKey());
					}
				}
			}
		});
	}

	private void setIsBlueprint(long typeId) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			log.warn("Could not set type {} as blueprint, not found", typeId);
			return;
		}
		type.put("is_blueprint", true);
		types.put(typeId, type);
	}

	private void addProducedBy(long typeId, long productTypeId, String activity) {
		var productType = (ObjectNode) types.get(productTypeId);
		if (productType == null) {
			log.warn("Could not set type {} as being created by blueprint, not found", typeId);
			return;
		}
		var array = productType.withArray("creating_blueprints");
		array.add(objectMapper.createObjectNode()
			.put("blueprint_type_id", productTypeId)
			.put("blueprint_activity", activity)
		);
		types.put(productTypeId, productType);
	}
}
