package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
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
public class MarketGroupBundleRenderer extends BundleRenderer {
	@Inject
	protected MarketGroupBundleRenderer() {}

	@Override
	protected Flowable<Pair<String, JsonNode>> renderInternal() {
		return Flowable.fromIterable(getGroupsMap().keySet()).flatMapMaybe(this::createGroupBundle);
	}

	private Maybe<Pair<String, JsonNode>> createGroupBundle(long groupId) {
		var groupJson = getMarketGroupsMap().get(groupId);
		var group = objectMapper.convertValue(groupJson, MarketGroup.class);
		var typeIds = group.getTypeIds();
		var childGroupIds = group.getChildMarketGroupIds();
		if ((typeIds == null || typeIds.isEmpty()) && (childGroupIds == null || childGroupIds.isEmpty())) {
			return Maybe.empty();
		}

		var bundleJson = objectMapper.createObjectNode();
		var typesJson = objectMapper.createObjectNode();
		var marketGroupsJson = objectMapper.createObjectNode();
		var attributesJson = objectMapper.createObjectNode();
		var unitsJson = objectMapper.createObjectNode();
		var iconsJson = objectMapper.createObjectNode();
		var metaGroupsJson = objectMapper.createObjectNode();

		unitsJson.set("133", unitsMap.get(133L)); // ISK for the market price display.

		for (long typeId : typeIds) {
			var typeJson = getTypesMap().get(typeId);
			var type = objectMapper.convertValue(typeJson, InventoryType.class);
			if (typeJson != null) {
				typesJson.set(Long.toString(typeId), typeJson);
				bundleDogmaAttributes(type, attributesJson);
				bundleDogmaAttributesUnits(attributesJson, unitsJson);
			}
		}

		bundleDogmaAttributesUnits(attributesJson, unitsJson);
		bundleDogmaAttributesIcons(attributesJson, iconsJson);
		bundleTypesMetaGroups(typesJson, metaGroupsJson);

		for (var childGroupId : childGroupIds) {
			var json = getMarketGroupsMap().get(childGroupId);
			if (json != null) {
				marketGroupsJson.set(Long.toString(childGroupId), json);
			}
		}

		var valid = false;
		if (!typesJson.isEmpty()) {
			bundleJson.set("types", typesJson);
			valid = true;
		}
		if (!attributesJson.isEmpty()) {
			bundleJson.set("dogma_attributes", attributesJson);
			valid = true;
		}
		if (!unitsJson.isEmpty()) {
			bundleJson.set("units", unitsJson);
			valid = true;
		}
		if (!iconsJson.isEmpty()) {
			bundleJson.set("icons", iconsJson);
			valid = true;
		}
		if (!metaGroupsJson.isEmpty()) {
			bundleJson.set("meta_groups", metaGroupsJson);
			valid = true;
		}

		if (valid) {
			var path = refDataUtil.subPath("market_groups", group.getMarketGroupId()) + "/bundle";
			return Maybe.just(Pair.of(path, bundleJson));
		}
		return Maybe.empty();
	}
}
