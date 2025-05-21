package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.IndustryModifierActivities;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

			var typeActivites = new HashMap<Long, IndustryModifierActivities.Builder>();
			types.forEach((rigTypeId, rigTypeJson) -> {
				var rig = objectMapper.convertValue(rigTypeJson, InventoryType.class);

				var rigCategories = Optional.ofNullable(rig.getEngineeringRigAffectedCategoryIds()).flatMap(l -> Optional.ofNullable(l.getManufacturing()))
						.stream().flatMap(Collection::stream);
				var rigGroups = Optional.ofNullable(rig.getEngineeringRigAffectedGroupIds()).flatMap(l -> Optional.ofNullable(l.getManufacturing()))
						.stream().flatMap(Collection::stream);
				var typeIds = Stream.concat(
					rigCategories.flatMap(this::resolveGroups).flatMap(this::resolveTypes),
					rigGroups.flatMap(this::resolveTypes)
				);
				var x = typeIds.toList();

				x.forEach(typeId -> {
					var activities = typeActivites.computeIfAbsent(typeId, t -> IndustryModifierActivities.builder());
					activities.manufacturing(rigTypeId);
				});
			});
			log.info(typeActivites.hashCode());


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

	private Stream<Long> resolveGroups(long categoryId) {
		var categoryJson = categories.get(categoryId);
		if (categoryJson == null) {return Stream.empty();}
		var category = objectMapper.convertValue(categoryJson, InventoryCategory.class);
		return Optional.ofNullable(category.getGroupIds()).stream().flatMap(Collection::stream);
	}

	private Stream<Long> resolveTypes(long groupId) {
		var groupJson = categories.get(groupId);
		if (groupJson == null) {return Stream.empty();}
		var group = objectMapper.convertValue(groupJson, InventoryGroup.class);
		return Optional.ofNullable(group.getTypeIds()).stream().flatMap(Collection::stream);
	}

	private void addToBonuses(long rigTypeId, IndustryModifierActivities affectedIds, Map<Long, IndustryModifierActivities.Builder> affectedTypes) {
		var typeActivities = affectedTypes.computeIfAbsent(rigTypeId, t -> IndustryModifierActivities.builder());
		affectedIds.getResearchMaterial().forEach(typeActivities::researchMaterial);
		affectedIds.getResearchTime().forEach(typeActivities::researchTime);
		affectedIds.getManufacturing().forEach(typeActivities::manufacturing);
		affectedIds.getInvention().forEach(typeActivities::invention);
		affectedIds.getCopying().forEach(typeActivities::copying);
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
