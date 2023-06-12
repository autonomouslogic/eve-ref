package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
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
import org.h2.mvstore.MVMap;

/**
 * Populates variations on types.
 *
 */
@Log4j2
public class VariationsDecorator {
	private static final int TECH_1_META_GROUP = 1;

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private MVMap<Long, JsonNode> types;

	@Inject
	protected VariationsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populates variations");
			types = storeHandler.getRefStore("types");
			// tech 1 ID -> meta group ID -> list of type IDs
			var variations = new HashMap<Long, Map<Integer, Set<Long>>>();
			resolveTypeVariations(variations);
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
			var typeVariations = variations.computeIfAbsent(parentTypeId, k -> new TreeMap<>());

			var parentTypeMetaGroupVariations = typeVariations.computeIfAbsent(TECH_1_META_GROUP, k -> new TreeSet<>());
			parentTypeMetaGroupVariations.add(parentTypeId);

			var metaGroupId = type.getMetaGroupId();
			if (metaGroupId == null) {
				throw new RuntimeException("No meta group ID for type " + type.getTypeId());
			}
			var metaGroupVariations = typeVariations.computeIfAbsent(metaGroupId, k -> new TreeSet<>());
			metaGroupVariations.add(type.getTypeId());
		}
	}

	private void populateVariations(@NonNull Map<Long, Map<Integer, Set<Long>>> variations) {
		variations.forEach((parentTypeId, typeVariations) -> {
			var typeVariationsJson = objectMapper.valueToTree(typeVariations);
			populateTypeVariations(parentTypeId, typeVariationsJson);
			typeVariations.forEach((metaGroupId, metaGroupVariations) -> {
				if (metaGroupVariations.size() > 1) {
					log.info(
							"Base type {} meta group {} has {} variations",
							parentTypeId,
							metaGroupId,
							metaGroupVariations.size());
				}
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
