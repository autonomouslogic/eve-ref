package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import java.util.function.Function;
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

		typesJson.set(Long.toString(typeId), typeJson);
		bundleDogmaAttributes(type, attributesJson);
		bundleTraits(type, typesJson);
		bundleVariations(type, typesJson);
		bundleRequiredSkills(type, skillsJson, typesJson);
		bundleUnits(unitsJson, attributesJson);
		bundleIcons(iconsJson, attributesJson);

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

		var path = refDataUtil.subPath("types", type.getTypeId()) + "/bundle";
		return Maybe.just(Pair.of(path, bundleJson));
	}

	private void bundleTraits(InventoryType type, ObjectNode typesJson) {
		Optional.ofNullable(type.getTraits())
				.flatMap(traits -> Optional.ofNullable(traits.getTypes()))
				.map(types -> types.keySet().stream())
				.stream()
				.flatMap(Function.identity())
				.map(Long::parseLong)
				.forEach(typeId -> {
					var json = getTypesMap().get(typeId);
					if (json != null) {
						typesJson.set(Long.toString(typeId), json);
					}
				});
	}
}
