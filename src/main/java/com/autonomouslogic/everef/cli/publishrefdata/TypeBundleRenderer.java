package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.DogmaTypeAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Skill;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class TypeBundleRenderer implements RefDataRenderer {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected RefDataUtil refDataUtil;

	@Setter
	@NonNull
	private MVStore dataStore;

	private Map<Long, JsonNode> typesMap;
	private Map<Long, JsonNode> dogmaMap;
	private Map<Long, JsonNode> skillsMap;
	private Map<Long, JsonNode> unitsMap;
	private Map<Long, JsonNode> iconsMap;

	@Inject
	protected TypeBundleRenderer() {}

	public Flowable<Pair<String, JsonNode>> render() {
		return Flowable.defer(() -> {
			log.info("Creating type bundles");
			typesMap = mvStoreUtil.openJsonMap(dataStore, "types", Long.class);
			dogmaMap = mvStoreUtil.openJsonMap(dataStore, "dogma_attributes", Long.class);
			skillsMap = mvStoreUtil.openJsonMap(dataStore, "skills", Long.class);
			unitsMap = mvStoreUtil.openJsonMap(dataStore, "units", Long.class);
			iconsMap = mvStoreUtil.openJsonMap(dataStore, "icons", Long.class);
			return Flowable.fromIterable(typesMap.keySet()).flatMapMaybe(this::createBundle);
		});
	}

	private Maybe<Pair<String, JsonNode>> createBundle(long typeId) {
		var typeJson = typesMap.get(typeId);
		var type = objectMapper.convertValue(typeJson, InventoryType.class);

		var bundleJson = objectMapper.createObjectNode();
		var typesJson = bundleJson.putObject("types");
		var attributesJson = objectMapper.createObjectNode();
		var skillsJson = objectMapper.createObjectNode();
		var unitsJson = objectMapper.createObjectNode();
		var iconsJson = objectMapper.createObjectNode();

		typesJson.set(Long.toString(typeId), typeJson);
		bundleDogmaAttributes(type, attributesJson);
		bundleVariations(type, typesJson);
		bundleRequiredSkills(type, skillsJson, typesJson);
		bundleUnits(unitsJson, attributesJson);
		bundleIcons(iconsJson, attributesJson);

		var valid = false;
		if (!attributesJson.isEmpty()) {
			bundleJson.set("dogma_attributes", attributesJson);
			valid = true;
		}
		if (!skillsJson.isEmpty()) {
			bundleJson.set("skills", skillsJson);
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

		if (valid) {
			var path = refDataUtil.subPath("types", type.getTypeId()) + "/bundle";
			return Maybe.just(Pair.of(path, bundleJson));
		}
		return Maybe.empty();
	}

	private void bundleDogmaAttributes(InventoryType type, ObjectNode attributesJson) {
		Optional.ofNullable(type.getDogmaAttributes()).stream()
				.flatMap(e -> e.values().stream())
				.map(DogmaTypeAttribute::getAttributeId)
				.forEach(typeAttr -> {
					var attr = dogmaMap.get(typeAttr);
					if (attr != null) {
						attributesJson.set(Long.toString(typeAttr), attr);
					}
				});
	}

	private void bundleVariations(InventoryType type, ObjectNode typesJson) {
		Optional.ofNullable(type.getTypeVariations()).stream()
				.flatMap(e -> e.values().stream())
				.flatMap(e -> e.stream())
				.distinct()
				.forEach(typeId -> {
					var json = typesMap.get(typeId);
					if (json != null) {
						typesJson.set(Long.toString(typeId), json);
					}
				});
	}

	private void bundleRequiredSkills(InventoryType type, ObjectNode skillsJson, ObjectNode typesJson) {
		Optional.ofNullable(type.getRequiredSkills()).stream()
				.flatMap(e -> e.keySet().stream())
				.distinct()
				.forEach(skillId -> bundleSkill(skillId, skillsJson, typesJson));
	}

	private void bundleSkill(long skillId, ObjectNode skillsJson, ObjectNode typesJson) {
		var skillJson = skillsMap.get(skillId);
		if (skillJson == null) {
			return;
		}
		skillsJson.set(Long.toString(skillId), skillJson);
		var skill = objectMapper.convertValue(skillJson, Skill.class);
		Optional.ofNullable(typesMap.get(skill.getTypeId()))
				.ifPresent(typeJson -> typesJson.set(Long.toString(skill.getTypeId()), typeJson));
		Optional.ofNullable(skill.getRequiredSkills()).stream()
				.flatMap(e -> e.keySet().stream())
				.filter(id -> id != skillId)
				.filter(id -> !skillsJson.has(Long.toString(id)))
				.distinct()
				.forEach(id -> bundleSkill(id, skillsJson, typesJson));
	}

	private void bundleUnits(ObjectNode unitsJson, ObjectNode attributesJson) {
		if (attributesJson.isEmpty()) {
			return;
		}
		attributesJson.fields().forEachRemaining(entry -> {
			var dogma = objectMapper.convertValue(entry.getValue(), DogmaAttribute.class);
			var unitId = dogma.getUnitId();
			var unitJson = unitsMap.get(unitId);
			if (unitJson == null) {
				return;
			}
			unitsJson.set(Long.toString(unitId), unitJson);
		});
	}

	private void bundleIcons(ObjectNode iconsJson, ObjectNode attributesJson) {
		if (attributesJson.isEmpty()) {
			return;
		}
		attributesJson.fields().forEachRemaining(entry -> {
			var dogma = objectMapper.convertValue(entry.getValue(), DogmaAttribute.class);
			var iconId = dogma.getIconId();
			if (iconId == null) {
				return;
			}
			var iconJson = iconsMap.get(iconId.longValue());
			if (iconJson == null) {
				return;
			}
			iconsJson.set(Long.toString(iconId), iconJson);
		});
	}
}
