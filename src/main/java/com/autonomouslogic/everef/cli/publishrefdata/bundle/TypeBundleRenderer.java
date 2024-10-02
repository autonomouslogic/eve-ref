package com.autonomouslogic.everef.cli.publishrefdata.bundle;

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
public class TypeBundleRenderer extends BundleRenderer {

	@Inject
	protected TypeBundleRenderer() {}

	@Override
	protected Flowable<Pair<String, JsonNode>> renderInternal() {
		return Flowable.fromIterable(getTypesMap().keySet()).flatMapMaybe(this::createTypeBundle);
	}

	private Maybe<Pair<String, JsonNode>> createTypeBundle(long typeId) {
		var typeJson = getTypesMap().get(typeId);
		var type = objectMapper.convertValue(typeJson, InventoryType.class);

		var bundleJson = objectMapper.createObjectNode();
		var typesJson = bundleJson.putObject("types");
		var attributesJson = objectMapper.createObjectNode();
		var skillsJson = objectMapper.createObjectNode();
		var unitsJson = objectMapper.createObjectNode();
		var iconsJson = objectMapper.createObjectNode();
		var marketGroupsJson = objectMapper.createObjectNode();
		var categoriesJson = objectMapper.createObjectNode();
		var groupsJson = objectMapper.createObjectNode();
		var metaGroupsJson = objectMapper.createObjectNode();

		typesJson.set(Long.toString(typeId), typeJson);
		bundleDogmaAttributes(type, attributesJson);
		bundleMarketGroup(type, marketGroupsJson);
		bundleInventoryGroup(type.getGroupId(), groupsJson, categoriesJson);
		bundleTraits(type, typesJson);
		bundleDescription(type, typesJson);
		bundleReprocessing(type, typesJson);
		bundleProducedByBlueprint(type, typesJson);
		bundleVariations(type, typesJson, attributesJson);
		bundleRequiredSkills(type, skillsJson, typesJson);
		bundleDogmaAttributesUnits(attributesJson, unitsJson);
		bundleDogmaAttributesIcons(attributesJson, iconsJson);
		bundleTypesMetaGroups(typesJson, metaGroupsJson);

		if (type.getMarketGroupId() != null) {
			unitsJson.set("133", unitsMap.get(133L)); // ISK for the market price display.
		}

		if (!attributesJson.isEmpty()) {
			bundleJson.set("dogma_attributes", attributesJson);
		}
		if (!skillsJson.isEmpty()) {
			bundleJson.set("skills", skillsJson);
		}
		if (!unitsJson.isEmpty()) {
			bundleJson.set("units", unitsJson);
		}
		if (!iconsJson.isEmpty()) {
			bundleJson.set("icons", iconsJson);
		}
		if (!marketGroupsJson.isEmpty()) {
			bundleJson.set("market_groups", marketGroupsJson);
		}
		if (!categoriesJson.isEmpty()) {
			bundleJson.set("categories", categoriesJson);
		}
		if (!groupsJson.isEmpty()) {
			bundleJson.set("groups", groupsJson);
		}
		if (!metaGroupsJson.isEmpty()) {
			bundleJson.set("meta_groups", metaGroupsJson);
		}

		var path = refDataUtil.subPath("types", type.getTypeId()) + "/bundle";
		return Maybe.just(Pair.of(path, bundleJson));
	}
}
