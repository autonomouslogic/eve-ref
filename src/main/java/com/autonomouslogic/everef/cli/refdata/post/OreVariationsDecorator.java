package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Populates ore variations on types.
 */
@Log4j2
public class OreVariationsDecorator implements PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private Map<Long, JsonNode> types;
	private StoreDataHelper helper;
	private DogmaAttribute oreBasicType;
	private DogmaAttribute asteroidMetaLevel;

	@Inject
	protected OreVariationsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populates ore variations");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			oreBasicType = helper.getDogmaAttributeByName("oreBasicType").orElseThrow();
			asteroidMetaLevel =
					helper.getDogmaAttributeByName("asteroidMetaLevel").orElseThrow();
			var variations = new HashMap<Long, Map<Integer, Set<Long>>>();
			resolveTypeVariations(variations);
			populateVariations(variations);
		});
	}

	private void resolveTypeVariations(@NonNull Map<Long, Map<Integer, Set<Long>>> variations) {
		for (var typeJson : types.values()) {
			var type = objectMapper.convertValue(typeJson, InventoryType.class);
			var oreType = helper.getDogmaFromType(type, oreBasicType.getAttributeId());
			var metaLevel = helper.getDogmaFromType(type, asteroidMetaLevel.getAttributeId());
			if (oreType.isEmpty() || metaLevel.isEmpty()) {
				continue;
			}
			var oreTypeId = oreType.get().getValue().longValue();
			var metaLevelValue = metaLevel.get().getValue().intValue();
			var basicOreJson = types.get(oreType.get().getValue().longValue());
			if (basicOreJson == null) {
				log.warn(
						"Basic ore type {} not found for type {}", oreType.get().getValue(), type.getTypeId());
				continue;
			}
			var typeVariations = variations.computeIfAbsent(oreTypeId, k -> new TreeMap<>());
			var metaVariations = typeVariations.computeIfAbsent(metaLevelValue, k -> new TreeSet<>());
			metaVariations.add(type.getTypeId());
		}
	}

	private void populateVariations(@NonNull Map<Long, Map<Integer, Set<Long>>> variations) {
		variations.forEach((baseOreTypeId, typeVariations) -> {
			var typeVariationsJson = objectMapper.valueToTree(typeVariations);
			typeVariations.forEach((metaLevel, metaVariations) -> {
				for (long typeId : metaVariations) {
					populateTypeVariations(typeId, typeVariationsJson);
				}
			});
		});
	}

	private void populateTypeVariations(long typeId, JsonNode typeVariationsJson) {
		var type = (ObjectNode) types.get(typeId);
		if (type == null) {
			throw new RuntimeException(String.format("Unable to populate variations for type %s - not found", typeId));
		}
		type.set("ore_variations", typeVariationsJson);
		type.put("is_ore", true);
		types.put(typeId, type);
	}
}
