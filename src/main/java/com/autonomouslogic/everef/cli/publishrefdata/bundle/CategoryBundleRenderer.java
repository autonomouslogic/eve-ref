package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import com.autonomouslogic.everef.refdata.InventoryCategory;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import tools.jackson.databind.JsonNode;

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
		var category = jsonMapper.convertValue(categoryJson, InventoryCategory.class);
		var groupIds = category.getGroupIds();

		var bundleJson = jsonMapper.createObjectNode();
		bundleJson.withObject("categories").set(Long.toString(categoryId), categoryJson);
		var groupsJson = jsonMapper.createObjectNode();

		if (groupIds != null) {
			for (long groupId : groupIds) {
				var groupJson = getGroupsMap().get(groupId);
				if (groupJson != null) {
					groupsJson.set(Long.toString(groupId), groupJson);
				}
			}
		}

		if (!groupsJson.isEmpty()) {
			bundleJson.set("groups", groupsJson);
		}

		var path = refDataUtil.subPath("categories", category.getCategoryId()) + "/bundle";
		return Maybe.just(Pair.of(path, bundleJson));
	}
}
