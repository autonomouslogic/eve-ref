package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Schematic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SchematicDecorator extends PostDecorator {
	private static final int HARVESTED_TYPE_DOGMA_ATTRIBUTE_ID = 709;
	private static final int PLANET_TYPE_RESTRICTION_DOGMA_ATTRIBUTE_ID = 1632;

	@Inject
	protected ObjectMapper objectMapper;

	private StoreDataHelper helper;
	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> schematics;

	@Inject
	protected SchematicDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			helper = new StoreDataHelper(storeHandler, objectMapper);
			log.info("Decorating schematics");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			schematics = storeHandler.getRefStore("schematics");
			for (var schematicEntry : schematics.entrySet()) {
				var schematicJson = (ObjectNode) schematicEntry.getValue();
				var schematic = objectMapper.convertValue(schematicJson, Schematic.class);
				handleMaterials(schematic);
				handleProducts(schematic);
				handleInstallableSchematics(schematic);
			}
			for (Map.Entry<Long, JsonNode> typeEntry : types.entrySet()) {
				var type = objectMapper.convertValue(typeEntry.getValue(), InventoryType.class);
				handleHarvestedBy(type);
				handleBuildableOnPlanets(type);
			}
		});
	}

	private void handleMaterials(Schematic schematic) {
		var schematicId = schematic.getSchematicId();
		var materials =
				Optional.ofNullable(schematic.getMaterials()).map(Map::values).orElse(List.of());
		for (var material : materials) {
			addUsedBy(schematicId, material.getTypeId());
		}
	}

	private void handleProducts(Schematic schematic) {
		var schematicId = schematic.getSchematicId();
		var products =
				Optional.ofNullable(schematic.getProducts()).map(Map::values).orElse(List.of());
		for (var product : products) {
			addProducedBy(schematicId, product.getTypeId());
		}
	}

	private void addUsedBy(long schematicId, long materialTypeId) {
		var productType = (ObjectNode) types.get(materialTypeId);
		if (productType == null) {
			log.warn("Could not set type {} as being used by schematic {}, not found", materialTypeId, schematicId);
			return;
		}
		var obj = productType.withArray("/used_by_schematic_ids");
		obj.add(schematicId);
		types.put(materialTypeId, productType);
	}

	private void addProducedBy(long schematicId, long productTypeId) {
		var productType = (ObjectNode) types.get(productTypeId);
		if (productType == null) {
			log.warn("Could not set type {} as being created by schematic {}, not found", productTypeId, schematicId);
			return;
		}
		var obj = productType.withArray("/produced_by_schematic_ids");
		obj.add(schematicId);
		types.put(productTypeId, productType);
	}

	private void handleInstallableSchematics(Schematic schematic) {
		for (long pinTypeId : schematic.getPinTypeIds()) {
			var typeJson = types.get(pinTypeId);
			if (typeJson == null) {
				log.warn(
						"Could not set type {} as being able to install schematic {}, pin type not found",
						pinTypeId,
						schematic.getSchematicId());
				continue;
			}
			((ArrayNode) typeJson.withArray("/installable_schematic_ids")).add(schematic.getSchematicId());
			types.put(pinTypeId, typeJson);
		}
	}

	private void handleHarvestedBy(InventoryType extractorType) {
		var harvestedTypeId = helper.getDogmaFromType(extractorType, HARVESTED_TYPE_DOGMA_ATTRIBUTE_ID)
				.map(t -> t.getValue().longValue());
		if (harvestedTypeId.isEmpty()) {
			return;
		}
		var harvestedTypeNode = types.get(harvestedTypeId.get());
		if (harvestedTypeNode == null) {
			log.warn(
					"Could not set type {} as being harvested by extractor {}, not found",
					harvestedTypeId.get(),
					extractorType.getTypeId());
			return;
		}
		((ArrayNode) harvestedTypeNode.withArray("/harvested_by_pin_type_ids")).add(extractorType.getTypeId());
		types.put(harvestedTypeId.get(), harvestedTypeNode);
	}

	private void handleBuildableOnPlanets(InventoryType extractorType) {
		var planetTypeId = helper.getDogmaFromType(extractorType, PLANET_TYPE_RESTRICTION_DOGMA_ATTRIBUTE_ID)
				.map(t -> t.getValue().longValue());
		if (planetTypeId.isEmpty()) {
			return;
		}
		var planetNode = types.get(planetTypeId.get());
		if (planetNode == null) {
			log.warn(
					"Could not set extractor type {} as being buildable on planet type {}, planet not found",
					extractorType.getTypeId(),
					planetTypeId.get());
			return;
		}
		((ArrayNode) planetNode.withArray("/buildable_pin_type_ids")).add(extractorType.getTypeId());
		types.put(planetTypeId.get(), planetNode);
	}
}
