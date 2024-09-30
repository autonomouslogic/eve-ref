package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import com.autonomouslogic.everef.cli.publishrefdata.RefDataRenderer;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.DogmaTypeAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.refdata.Skill;
import com.autonomouslogic.everef.refdata.TraitBonus;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;

@Log4j2
public abstract class BundleRenderer implements RefDataRenderer {
	private static final Pattern showinfoPattern = Pattern.compile("showinfo:([0-9]+)");

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
	protected Map<Long, JsonNode> typesMap;

	@Getter
	protected Map<Long, JsonNode> dogmaMap;

	@Getter
	protected Map<Long, JsonNode> skillsMap;

	@Getter
	protected Map<Long, JsonNode> unitsMap;

	@Getter
	protected Map<Long, JsonNode> iconsMap;

	@Getter
	protected Map<Long, JsonNode> groupsMap;

	@Getter
	protected Map<Long, JsonNode> marketGroupsMap;

	public final Flowable<Pair<String, JsonNode>> render() {
		return Flowable.defer(() -> {
			log.info("Rendering bundle: {}", getClass().getSimpleName());
			typesMap = mvStoreUtil.openJsonMap(dataStore, "types", Long.class);
			dogmaMap = mvStoreUtil.openJsonMap(dataStore, "dogma_attributes", Long.class);
			skillsMap = mvStoreUtil.openJsonMap(dataStore, "skills", Long.class);
			unitsMap = mvStoreUtil.openJsonMap(dataStore, "units", Long.class);
			iconsMap = mvStoreUtil.openJsonMap(dataStore, "icons", Long.class);
			groupsMap = mvStoreUtil.openJsonMap(dataStore, "groups", Long.class);
			marketGroupsMap = mvStoreUtil.openJsonMap(dataStore, "market_groups", Long.class);
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
				.forEach(typeId -> addTypeToBundle(typeId, typesJson));
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

	protected void bundleDogmaAttributesUnits(ObjectNode attributesJson, ObjectNode unitsJson) {
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

	protected void bundleDogmaAttributesIcons(ObjectNode attributesJson, ObjectNode iconsJson) {
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

	protected void bundleDescription(InventoryType type, ObjectNode typesJson) {
		Optional.ofNullable(type.getDescription())
				.ifPresent(d -> d.values().forEach(text -> bundleShowInfo(text, typesJson)));
	}

	protected void bundleTraits(InventoryType type, ObjectNode typesJson) {
		var traits = type.getTraits();
		if (traits == null) {
			return;
		}
		var allTraitBonuses = Stream.<TraitBonus>empty();
		var role = traits.getRoleBonuses();
		var misc = traits.getMiscBonuses();
		var types = traits.getTypes();
		if (role != null) {
			allTraitBonuses = Stream.concat(allTraitBonuses, role.values().stream());
		}
		if (misc != null) {
			allTraitBonuses = Stream.concat(allTraitBonuses, misc.values().stream());
		}
		if (types != null) {
			for (Map.Entry<String, Map<String, TraitBonus>> typesEntry : types.entrySet()) {
				var typeId = Long.parseLong(typesEntry.getKey());
				addTypeToBundle(typeId, typesJson);
				for (Map.Entry<String, TraitBonus> importanceEntry :
						typesEntry.getValue().entrySet()) {
					allTraitBonuses = Stream.concat(allTraitBonuses, Stream.of(importanceEntry.getValue()));
				}
			}
		}
		allTraitBonuses
				.flatMap(e -> e.getBonusText().values().stream())
				.forEach(text -> bundleShowInfo(text, typesJson));
	}

	protected void bundleShowInfo(String text, ObjectNode typesJson) {
		var matcher = showinfoPattern.matcher(text);
		while (matcher.find()) {
			var typeId = Long.parseLong(matcher.group(1));
			addTypeToBundle(typeId, typesJson);
		}
	}

	protected void bundleReprocessing(InventoryType type, ObjectNode typesJson) {
		Optional.ofNullable(type.getTypeMaterials()).map(e -> e.values()).stream()
				.flatMap(c -> c.stream())
				.forEach(m -> addTypeToBundle(m.getMaterialTypeId(), typesJson));
	}

	protected void bundleProducedByBlueprint(InventoryType type, ObjectNode typesJson) {
		Optional.ofNullable(type.getProducedByBlueprints()).map(e -> e.values()).stream()
				.flatMap(c -> c.stream())
				.forEach(b -> addTypeToBundle(b.getBlueprintTypeId(), typesJson));
	}

	protected void bundleMarketGroup(InventoryType type, ObjectNode marketGroupsJson) {
		var id = type.getMarketGroupId();
		if (id != null) {
			bundleMarketGroup(id, marketGroupsJson);
		}
	}

	protected void bundleMarketGroup(long id, ObjectNode marketGroupsJson) {
		var json = getMarketGroupsMap().get(id);
		if (json != null) {
			marketGroupsJson.set(Long.toString(id), json);
			var group = objectMapper.convertValue(json, MarketGroup.class);
			if (group.getParentGroupId() != null) {
				bundleMarketGroup(group.getParentGroupId(), marketGroupsJson);
			}
		}
	}

	protected void addTypeToBundle(Long typeId, ObjectNode typesJson) {
		var json = typesMap.get(typeId);
		if (json != null) {
			typesJson.set(Long.toString(typeId), json);
		}
	}
}
