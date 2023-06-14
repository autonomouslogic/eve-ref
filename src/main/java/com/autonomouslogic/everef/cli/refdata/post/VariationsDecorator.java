package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Mutaplasmid;
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
import org.h2.mvstore.MVMap;

/**
 * Populates variations on types.
 *
 */
@Log4j2
public class VariationsDecorator {
	private static final int TECH_1_META_GROUP = 1;
	private static final int ABYSSAL_META_GROUP = 15;

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private MVMap<Long, JsonNode> types;
	private MVMap<Long, JsonNode> mutaplasmids;

	@Inject
	protected VariationsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populates variations");
			types = storeHandler.getRefStore("types");
			mutaplasmids = storeHandler.getRefStore("mutaplasmids");
			if (mutaplasmids.isEmpty()) {
				throw new RuntimeException("Out of order");
			}
			// tech 1 ID -> meta group ID -> list of type IDs
			var variations = new HashMap<Long, Map<Integer, Set<Long>>>();
			resolveTypeVariations(variations);
			resolveDynamicVariations(variations);
			populateVariations(variations);
		});
	}

	private void resolveTypeVariations(@NonNull Map<Long, Map<Integer, Set<Long>>> variations) {
		for (var typeJson : types.values()) {
			var type = objectMapper.convertValue(typeJson, InventoryType.class);

			var parentTypeId = type.getVariationParentTypeId();
			if (parentTypeId == null) {
				continue;
			}
			addMetaGroupVariation(parentTypeId, parentTypeId, TECH_1_META_GROUP, variations);

			var metaGroupId = type.getMetaGroupId();
			if (metaGroupId == null) {
				throw new RuntimeException("No meta group ID for type " + type.getTypeId());
			}
			addMetaGroupVariation(parentTypeId, type.getTypeId(), metaGroupId, variations);
		}
	}

	private void resolveDynamicVariations(@NonNull Map<Long, Map<Integer, Set<Long>>> variations) {
		for (var json : mutaplasmids.values()) {
			var mutaplasmid = objectMapper.convertValue(json, Mutaplasmid.class);
			for (var typeMapping : mutaplasmid.getTypeMappings()) {
				var resultingTypeId = typeMapping.getResultingTypeId();
				for (var applicableTypeId : typeMapping.getApplicableTypeIds()) {
					var applicableType = objectMapper.convertValue(types.get(applicableTypeId), InventoryType.class);
					var parentType = applicableType.getVariationParentTypeId() == null
							? applicableType
							: objectMapper.convertValue(
									types.get(applicableType.getVariationParentTypeId()), InventoryType.class);
					addMetaGroupVariation(
							parentType.getTypeId(), parentType.getTypeId(), TECH_1_META_GROUP, variations);
					addMetaGroupVariation(parentType.getTypeId(), resultingTypeId, ABYSSAL_META_GROUP, variations);
				}
			}
		}
	}

	private static void addMetaGroupVariation(
			long parentTypeId, long typeId, int metaGroupId, Map<Long, Map<Integer, Set<Long>>> variations) {
		var typeVariations = variations.computeIfAbsent(parentTypeId, k -> new TreeMap<>());
		var metaGroupVariations = typeVariations.computeIfAbsent(metaGroupId, k -> new TreeSet<>());
		metaGroupVariations.add(typeId);
	}

	private void populateVariations(@NonNull Map<Long, Map<Integer, Set<Long>>> variations) {
		variations.forEach((parentTypeId, typeVariations) -> {
			var typeVariationsJson = objectMapper.valueToTree(typeVariations);
			populateTypeVariations(parentTypeId, typeVariationsJson);
			typeVariations.forEach((metaGroupId, metaGroupVariations) -> {
				for (long typeId : metaGroupVariations) {
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
		type.put("type_variations", typeVariationsJson);
		types.put(typeId, type);
	}
}