package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Flowable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class RootCategoryBundleRenderer extends BundleRenderer {
	@Inject
	protected RootCategoryBundleRenderer() {}

	@Override
	protected Flowable<Pair<String, JsonNode>> renderInternal() {
		var bundleJson = objectMapper.createObjectNode();
		var categoriesJson = bundleJson.putObject("categories");
		for (var entry : getCategoriesMap().entrySet()) {
			categoriesJson.set(Long.toString(entry.getKey()), entry.getValue());
		}
		var path = refDataUtil.subPath("categories/bundle");
		return Flowable.just(Pair.of(path, bundleJson));
	}
}
