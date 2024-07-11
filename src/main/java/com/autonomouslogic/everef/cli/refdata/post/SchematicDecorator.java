package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.refdata.Schematic;
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
public class SchematicDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private StoreDataHelper helper;
	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> schematics;

	@Inject
	protected SchematicDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating schematics");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			schematics = storeHandler.getRefStore("schematics");
			for (var schematicEntry : schematics.entrySet()) {
				long schematicId = schematicEntry.getKey();
				var schematicJson = (ObjectNode) schematicEntry.getValue();
				var schematic = objectMapper.convertValue(schematicJson, Schematic.class);
				var materials = Optional.ofNullable(schematic.getMaterials())
						.map(Map::values)
						.orElse(List.of());
				for (var material : materials) {
					addUsedBy(schematicId, material.getTypeId());
				}
				var products = Optional.ofNullable(schematic.getProducts())
						.map(Map::values)
						.orElse(List.of());
				for (var product : products) {
					addProducedBy(schematicId, product.getTypeId());
				}
			}
		});
	}

	private void addUsedBy(long schematicId, long materialTypeId) {
		var productType = (ObjectNode) types.get(materialTypeId);
		if (productType == null) {
			log.warn("Could not set type {} as being used by schematic {}, not found", materialTypeId, schematicId);
			return;
		}
		var obj = productType.withArray("/using_in_schematics");
		obj.add(schematicId);
		types.put(materialTypeId, productType);
	}

	private void addProducedBy(long schematicId, long productTypeId) {
		var productType = (ObjectNode) types.get(productTypeId);
		if (productType == null) {
			log.warn("Could not set type {} as being created by schematic {}, not found", productTypeId, schematicId);
			return;
		}
		var obj = productType.withArray("/produced_by_schematics");
		obj.add(schematicId);
		types.put(productTypeId, productType);
	}
}
