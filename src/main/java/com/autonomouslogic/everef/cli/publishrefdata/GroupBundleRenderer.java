package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class GroupBundleRenderer extends BundleRenderer {
	@Inject
	protected GroupBundleRenderer() {}

	@Override
	protected Flowable<Pair<String, JsonNode>> renderInternal() {
		return Flowable.fromIterable(getGroupsMap().keySet()).flatMapMaybe(this::createGroupBundle);
	}

	private Maybe<Pair<String, JsonNode>> createGroupBundle(long groupId) {
		var groupJson = getGroupsMap().get(groupId);
		var group = objectMapper.convertValue(groupJson, InventoryGroup.class);
		var typeIds = group.getTypeIds();
		if (typeIds == null || typeIds.isEmpty()) {
			return Maybe.empty();
		}

		var bundleJson = objectMapper.createObjectNode();
		var typesJson = bundleJson.putObject("types");
		var attributesJson = objectMapper.createObjectNode();
		var unitsJson = objectMapper.createObjectNode();

		for (long typeId : typeIds) {
			var typeJson = getTypesMap().get(typeId);
			var type = objectMapper.convertValue(typeJson, InventoryType.class);
			if (typeJson != null) {
				typesJson.set(Long.toString(typeId), typeJson);
				bundleDogmaAttributes(type, attributesJson);
				bundleUnits(unitsJson, attributesJson);
			}
		}

		var valid = false;
		if (!attributesJson.isEmpty()) {
			bundleJson.set("dogma_attributes", attributesJson);
			valid = true;
		}
		if (!unitsJson.isEmpty()) {
			bundleJson.set("units", unitsJson);
			valid = true;
		}

		if (valid) {
			var path = refDataUtil.subPath("groups", group.getGroupId()) + "/bundle";
			return Maybe.just(Pair.of(path, bundleJson));
		}
		return Maybe.empty();
	}
}
