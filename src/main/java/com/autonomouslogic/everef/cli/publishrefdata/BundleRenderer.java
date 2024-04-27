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
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

@Log4j2
public abstract class BundleRenderer implements RefDataRenderer {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected RefDataUtil refDataUtil;

	@Setter
	@Getter
	@NonNull
	private MVStore dataStore;

	@Getter
	private Map<Long, JsonNode> typesMap;

	@Getter
	private Map<Long, JsonNode> dogmaMap;

	@Getter
	private Map<Long, JsonNode> skillsMap;

	@Getter
	private Map<Long, JsonNode> unitsMap;

	@Getter
	private Map<Long, JsonNode> iconsMap;

	@Getter
	private Map<Long, JsonNode> groupsMap;

	public final Flowable<Pair<String, JsonNode>> render() {
		return Flowable.defer(() -> {
			log.info("Rendering bundle: {}", getClass().getSimpleName());
			typesMap = mvStoreUtil.openJsonMap(dataStore, "types", Long.class);
			dogmaMap = mvStoreUtil.openJsonMap(dataStore, "dogma_attributes", Long.class);
			skillsMap = mvStoreUtil.openJsonMap(dataStore, "skills", Long.class);
			unitsMap = mvStoreUtil.openJsonMap(dataStore, "units", Long.class);
			iconsMap = mvStoreUtil.openJsonMap(dataStore, "icons", Long.class);
			groupsMap = mvStoreUtil.openJsonMap(dataStore, "groups", Long.class);
			return renderInternal();
		});
	}

	protected abstract Flowable<Pair<String, JsonNode>> renderInternal();

	protected void bundleDogmaAttributes(InventoryType type, ObjectNode attributesJson) {
		Optional.ofNullable(type.getDogmaAttributes()).stream()
				.flatMap(e -> e.values().stream())
				.map(DogmaTypeAttribute::getAttributeId)
				.forEach(typeAttr -> {
					var attr = getDogmaMap().get(typeAttr);
					if (attr != null) {
						attributesJson.set(Long.toString(typeAttr), attr);
					}
				});
	}

	protected void bundleVariations(InventoryType type, ObjectNode typesJson) {
		Optional.ofNullable(type.getTypeVariations()).stream()
				.flatMap(e -> e.values().stream())
				.flatMap(e -> e.stream())
				.distinct()
				.forEach(typeId -> {
					var json = getTypesMap().get(typeId);
					if (json != null) {
						typesJson.set(Long.toString(typeId), json);
					}
				});
	}

	protected void bundleRequiredSkills(InventoryType type, ObjectNode skillsJson, ObjectNode typesJson) {
		Optional.ofNullable(type.getRequiredSkills()).stream()
				.flatMap(e -> e.keySet().stream())
				.distinct()
				.forEach(skillId -> bundleSkill(skillId, skillsJson, typesJson));
	}

	protected void bundleSkill(long skillId, ObjectNode skillsJson, ObjectNode typesJson) {
		var skillJson = getSkillsMap().get(skillId);
		if (skillJson == null) {
			return;
		}
		skillsJson.set(Long.toString(skillId), skillJson);
		var skill = objectMapper.convertValue(skillJson, Skill.class);
		Optional.ofNullable(getTypesMap().get(skill.getTypeId()))
				.ifPresent(typeJson -> typesJson.set(Long.toString(skill.getTypeId()), typeJson));
		Optional.ofNullable(skill.getRequiredSkills()).stream()
				.flatMap(e -> e.keySet().stream())
				.filter(id -> id != skillId)
				.filter(id -> !skillsJson.has(Long.toString(id)))
				.distinct()
				.forEach(id -> bundleSkill(id, skillsJson, typesJson));
	}

	protected void bundleUnits(ObjectNode unitsJson, ObjectNode attributesJson) {
		if (attributesJson.isEmpty()) {
			return;
		}
		attributesJson.fields().forEachRemaining(entry -> {
			var dogma = objectMapper.convertValue(entry.getValue(), DogmaAttribute.class);
			var unitId = dogma.getUnitId();
			var unitJson = getUnitsMap().get(unitId);
			if (unitJson == null) {
				return;
			}
			unitsJson.set(Long.toString(unitId), unitJson);
		});
	}

	protected void bundleIcons(ObjectNode iconsJson, ObjectNode attributesJson) {
		if (attributesJson.isEmpty()) {
			return;
		}
		attributesJson.fields().forEachRemaining(entry -> {
			var dogma = objectMapper.convertValue(entry.getValue(), DogmaAttribute.class);
			var iconId = dogma.getIconId();
			if (iconId == null) {
				return;
			}
			var iconJson = getIconsMap().get(iconId.longValue());
			if (iconJson == null) {
				return;
			}
			iconsJson.set(Long.toString(iconId), iconJson);
		});
	}
}
