package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Populates which standup rigs modifies individual types.
 */
@Log4j2
public class IndustryModifierSourcesDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private Map<Long, JsonNode> types;

	@Inject
	protected IndustryModifierSourcesDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating industry modifier sources");
			types = storeHandler.getRefStore("types");

			// Build an index of category and group IDs to engineering rig type IDs.
			var categoryRigs = new HashMap<Long, Set<Long>>();
			var groupRigs = new HashMap<Long, Set<Long>>();
			types.forEach((typeId, typeJson) -> {
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				indexType(type.getEngineeringRigAffectedCategoryIds(), typeId, categoryRigs);
				indexType(type.getEngineeringRigAffectedGroupIds(), typeId, groupRigs);
			});

			// Decorate types with the engineering rig sources.
			types.forEach((typeId, typeJson) -> {
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				var categoryId = type.getCategoryId();
				var groupId = type.getGroupId();
				var rigIds = Stream.concat(
								Optional.ofNullable(categoryRigs.get(categoryId)).orElse(Set.of()).stream(),
								Optional.ofNullable(groupRigs.get(groupId)).orElse(Set.of()).stream())
						.toList();
				if (!rigIds.isEmpty()) {
					var array = typeJson.withArrayProperty("engineering_rig_source_type_ids");
					JsonUtil.addToArraySetSorted(rigIds, array);
				}
				types.put(typeId, typeJson);
			});
		});
	}

	private static void indexType(List<Long> types, long typeId, Map<Long, Set<Long>> index) {
		Optional.ofNullable(types)
				.ifPresent(categoryIds -> categoryIds.forEach(categoryId -> {
					index.computeIfAbsent(categoryId, k -> new HashSet<>()).add(typeId);
				}));
	}
}
