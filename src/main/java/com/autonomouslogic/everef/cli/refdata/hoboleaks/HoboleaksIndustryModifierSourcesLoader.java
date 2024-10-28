package com.autonomouslogic.everef.cli.refdata.hoboleaks;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
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
		activities.fields().forEachRemaining(entry -> {
			var activity = entry.getKey();
			var activityDetails = entry.getValue();
			activityDetails.fields().forEachRemaining(activityEntry -> {
				var bonusName = activityEntry.getKey();
				var array = (ArrayNode) activityEntry.getValue();
				for (var node : array) {
					if (node.has("filterID")) {
						var filterId = node.get("filterID").longValue();
						processTypeFilter(typeId, filterId);
					}
				}
			});
		});
	}

	private void processTypeFilter(long typeId, long filterId) {
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
			var categoryIds = (ArrayNode) filter.get("categoryIDs");
			if (!categoryIds.isEmpty()) {
				type.withArrayProperty("engineering_rig_affected_category_ids").addAll(categoryIds);
			}
		}
		if (filter.has("groupIDs")) {
			var groupIds = (ArrayNode) filter.get("groupIDs");
			if (!groupIds.isEmpty()) {
				type.withArrayProperty("engineering_rig_affected_group_ids").addAll(groupIds);
			}
		}
		types.put(typeId, type);
	}
}
