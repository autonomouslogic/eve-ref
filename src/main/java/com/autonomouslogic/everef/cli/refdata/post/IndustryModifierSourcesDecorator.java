package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.IndustryModifierActivities;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;

import lombok.Value;
import lombok.extern.log4j.Log4j2;

/**
 * Populates which standup rigs modifies individual types.
 */
@Log4j2
public class IndustryModifierSourcesDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private Map<Long, JsonNode> categories;
	private Map<Long, JsonNode> groups;
	private Map<Long, JsonNode> types;

	@Inject
	protected IndustryModifierSourcesDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating industry modifier sources");
			categories = storeHandler.getRefStore("categories");
			groups = storeHandler.getRefStore("groups");
			types = storeHandler.getRefStore("types");

			// Create a flattened table to bonuses.
			var bonuses = new ArrayList<ModifierEntry>();
			types.forEach((typeId, typeJson) -> {
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				addToBonuses(type.getEngineeringRigAffectedCategoryIds(), bonuses);
				addToBonuses(type.getEngineeringRigAffectedGroupIds(), bonuses);
			});
			bonuses.sort(Ordering.natural().onResultOf(ModifierEntry::getRigId));


//			// Prepare the modifier activities for the types.
//			var categoryRigs = new HashMap<Long, IndustryModifierActivities>();
//			var groupRigs = new HashMap<Long, IndustryModifierActivities>();
//			types.forEach((typeId, typeJson) -> {
//				var type = objectMapper.convertValue(typeJson, InventoryType.class);
//				indexAffectedTypes(type.getEngineeringRigAffectedCategoryIds(), typeId, categoryRigs);
//				indexAffectedTypes(type.getEngineeringRigAffectedGroupIds(), typeId, groupRigs);
//			});
//
//			// Decorate types with the engineering rig sources.
//			types.forEach((typeId, typeJson) -> {
//				var type = objectMapper.convertValue(typeJson, InventoryType.class);
//				var categoryId = type.getCategoryId();
//				var groupId = type.getGroupId();
//				var rigIds = Stream.concat(
//								Optional.ofNullable(categoryRigs.get(categoryId)).orElse(Set.of()).stream(),
//								Optional.ofNullable(groupRigs.get(groupId)).orElse(Set.of()).stream())
//						.toList();
//				if (!rigIds.isEmpty()) {
//					var array = typeJson.withArrayProperty("engineering_rig_source_type_ids");
//					JsonUtil.addToArraySetSorted(rigIds, array);
//				}
//				types.put(typeId, typeJson);
//			});
		});
	}

	private void addToBonuses(IndustryModifierActivities engineeringRigAffectedCategoryIds, List<ModifierEntry> bonuses) {

	}

//	private static void indexAffectedTypes(IndustryModifierActivities activities, long typeId, Map<Long, IndustryModifierActivities> index) {
//		Optional.ofNullable(types)
//				.ifPresent(categoryIds -> categoryIds.forEach(categoryId -> {
//					index.computeIfAbsent(categoryId, k -> new HashSet<>()).add(typeId);
//				}));
//	}

	@Value
	private static class ModifierEntry {
		long affectedTypeId;
		String activity;
		String bonusType;
		long rigId;
	}
}
