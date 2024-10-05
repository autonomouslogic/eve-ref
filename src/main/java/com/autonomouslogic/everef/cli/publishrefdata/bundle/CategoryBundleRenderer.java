package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class CategoryBundleRenderer extends BundleRenderer {
	@Inject
	protected CategoryBundleRenderer() {}

	@Override
	protected Flowable<Pair<String, JsonNode>> renderInternal() {
		return Flowable.fromIterable(getCategoriesMap().keySet()).flatMapMaybe(this::createCategoryBundle);
	}

	private Maybe<Pair<String, JsonNode>> createCategoryBundle(long categoryId) {
		var categoryJson = getCategoriesMap().get(categoryId);
		var category = objectMapper.convertValue(categoryJson, InventoryCategory.class);
		var groupIds = category.getGroupIds();
		if (groupIds == null || groupIds.isEmpty()) {
			return Maybe.empty();
		}

		var bundleJson = objectMapper.createObjectNode();
		var groupsJson = bundleJson.putObject("groups");

		for (long groupId : groupIds) {
			var groupJson = getGroupsMap().get(groupId);
			if (groupJson != null) {
				groupsJson.set(Long.toString(groupId), groupJson);
			}
		}

		var path = refDataUtil.subPath("categories", category.getCategoryId()) + "/bundle";
		return Maybe.just(Pair.of(path, bundleJson));
	}
}
