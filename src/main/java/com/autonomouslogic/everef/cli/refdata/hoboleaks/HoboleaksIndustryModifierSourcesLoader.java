package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Reads the Hoboleaks industry modifier sources to create entries on the rig types for affected types.
 */
@Log4j2
public class HoboleaksIndustryModifierSourcesLoader {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Setter
	@NonNull
	private ObjectNode modifierSources;

	@Setter
	@NonNull
	private ObjectNode targetFilters;

	@Inject
	public HoboleaksIndustryModifierSourcesLoader() {}

	@SneakyThrows
	public Completable load() {
		if (modifierSources == null || targetFilters == null) {
			return Completable.complete();
		}
		log.info("Loading Hoboleaks industry modifier sources");
		var types = storeHandler.getHoboleaksStore("types");
		modifierSources.fields().forEachRemaining(entry -> {
			var typeId = Long.parseLong(entry.getKey());
			var activities = entry.getValue();
			processTypeActivities(typeId, activities);
		});
		return Completable.complete();
	}

	private void processTypeActivities(long typeId, JsonNode activities) {
		for (Map.Entry<String, JsonNode> entry : activities.properties()) {
			var hasBonus = false;
			var activity = entry.getKey();
			var activityDetails = entry.getValue();
			for (Map.Entry<String, JsonNode> activityEntry : activityDetails.properties()) {
				hasBonus |= processTypeActivity(
						typeId, activityEntry.getKey(), (ArrayNode) activityEntry.getValue(), activity);
			}
			if (!hasBonus) {
				addGlobalActivity(typeId, activity);
			}
		}
	}

	private boolean processTypeActivity(long typeId, String bonusType, ArrayNode bonuses, String activity) {
		boolean hasFilter = false;
		for (var bonusEntry : bonuses) {
			if (bonusEntry.has("filterID")) {
				hasFilter = true;
				var filterId = bonusEntry.get("filterID").longValue();
				processTypeFilter(typeId, activity, filterId);
			}
		}
		return hasFilter;
	}

	private void processTypeFilter(long typeId, String activity, long filterId) {
		var types = storeHandler.getHoboleaksStore("types");
		var type = types.get(typeId);
		if (type == null) {
			type = objectMapper.createObjectNode().put("type_id", typeId);
		}
		var filter = targetFilters.get(Long.toString(filterId));
		if (filter == null) {
			log.warn("No filter found for filter ID {}", filterId);
		}
		if (filter.has("categoryIDs")) {
			var categoryIds = Streams.stream(filter.get("categoryIDs").elements())
					.map(e -> e.asLong())
					.toList();
			if (!categoryIds.isEmpty()) {
				var array = type.withObjectProperty("engineering_rig_affected_category_ids")
						.withArrayProperty(activity);
				JsonUtil.addToArraySetSorted(categoryIds, array);
			}
		}
		if (filter.has("groupIDs")) {
			var groupIds = Streams.stream(filter.get("groupIDs").elements())
					.map(e -> e.asLong())
					.toList();
			if (!groupIds.isEmpty()) {
				var array = type.withObjectProperty("engineering_rig_affected_group_ids")
						.withArrayProperty(activity);
				JsonUtil.addToArraySetSorted(groupIds, array);
			}
		}
		types.put(typeId, type);
	}

	private void addGlobalActivity(long typeId, String activity) {
		var types = storeHandler.getHoboleaksStore("types");
		var type = types.get(typeId);
		if (type == null) {
			type = objectMapper.createObjectNode().put("type_id", typeId);
		}
		type.withArrayProperty("engineering_rig_global_activities").add(activity);
		types.put(typeId, type);
	}
}
