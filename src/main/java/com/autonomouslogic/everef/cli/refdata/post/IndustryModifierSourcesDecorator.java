package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.IndustryModifierActivities;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

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
				var affectedCategories = Optional.ofNullable(rig.getEngineeringRigAffectedCategoryIds());
				var affectedgroups = Optional.ofNullable(rig.getEngineeringRigAffectedGroupIds());
				setRigOnTypes(
						rig,
						affectedCategories.map(c -> c.getResearchMaterial()),
						affectedgroups.map(c -> c.getResearchMaterial()),
						typeActivites,
						IndustryModifierActivities.Builder::researchMaterial);
				setRigOnTypes(
						rig,
						affectedCategories.map(c -> c.getResearchTime()),
						affectedgroups.map(c -> c.getResearchTime()),
						typeActivites,
						IndustryModifierActivities.Builder::researchTime);
				setRigOnTypes(
						rig,
						affectedCategories.map(c -> c.getManufacturing()),
						affectedgroups.map(c -> c.getManufacturing()),
						typeActivites,
						IndustryModifierActivities.Builder::manufacturing);
				setRigOnTypes(
						rig,
						affectedCategories.map(c -> c.getInvention()),
						affectedgroups.map(c -> c.getInvention()),
						typeActivites,
						IndustryModifierActivities.Builder::invention);
				setRigOnTypes(
						rig,
						affectedCategories.map(c -> c.getCopying()),
						affectedgroups.map(c -> c.getCopying()),
						typeActivites,
						IndustryModifierActivities.Builder::copying);
			});

			typeActivites.forEach((typeId, builder) -> {
				var typeJson = types.get(typeId);
				if (typeJson == null) {
					return;
				}
				var activities = builder.build();
				activities = activities.toBuilder()
					.clearResearchMaterial()
						.researchMaterial(sortList(activities.getResearchMaterial()))
					.clearResearchTime()
						.researchTime(sortList(activities.getResearchTime()))
					.clearManufacturing()
						.manufacturing(sortList(activities.getManufacturing()))
					.clearInvention()
						.invention(sortList(activities.getInvention()))
					.clearCopying()
						.copying(sortList(activities.getCopying()))
						.build();
				var activitiesJson = objectMapper.convertValue(activities, ObjectNode.class);
				((ObjectNode) typeJson).set("engineering_rig_source_type_ids", activitiesJson);
				types.put(typeId, typeJson);
			});
		});
	}

	private void setRigOnTypes(
			InventoryType rig,
			Optional<List<Long>> categories,
			Optional<List<Long>> groups,
			Map<Long, IndustryModifierActivities.Builder> typeActivites,
			BiConsumer<IndustryModifierActivities.Builder, Long> activitiesSetter) {
		var rigTypeId = rig.getTypeId();
		var rigCategories = categories.stream().flatMap(Collection::stream);
		var rigGroups = groups.stream().flatMap(Collection::stream);
		var typeIds = Stream.concat(
				rigCategories.flatMap(this::resolveGroups).flatMap(this::resolveTypes),
				rigGroups.flatMap(this::resolveTypes));
		typeIds.forEach(typeId -> {
			var activities = typeActivites.computeIfAbsent(typeId, t -> IndustryModifierActivities.builder());
			activities.manufacturing(rigTypeId);
			activitiesSetter.accept(activities, rigTypeId);
		});
	}

	private Stream<Long> resolveGroups(long categoryId) {
		var categoryJson = categories.get(categoryId);
		if (categoryJson == null) {
			return Stream.empty();
		}
		var category = objectMapper.convertValue(categoryJson, InventoryCategory.class);
		return Optional.ofNullable(category.getGroupIds()).stream().flatMap(Collection::stream);
	}

	private Stream<Long> resolveTypes(long groupId) {
		var groupJson = groups.get(groupId);
		if (groupJson == null) {
			return Stream.empty();
		}
		var group = objectMapper.convertValue(groupJson, InventoryGroup.class);
		return Optional.ofNullable(group.getTypeIds()).stream().flatMap(Collection::stream);
	}


	private static @NotNull List<Long> sortList(List<Long> activities) {
		return activities.stream()
			.distinct()
			.sorted()
			.toList();
	}
}
