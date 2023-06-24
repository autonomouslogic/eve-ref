package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVMap;
import org.jetbrains.annotations.NotNull;

@Log4j2
public class SkillDecorator implements PostDecorator {
	private static final int SKILL_CATEGORY_ID = 16;
	private static final Map<String, Integer> ATTRIBUTE_ID_MAP =
			Map.of("intelligence", 1, "charisma", 2, "perception", 3, "memory", 4, "willpower", 5);

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private MVMap<Long, JsonNode> types;
	private MVMap<Long, JsonNode> skills;
	private MVMap<Long, JsonNode> dogmaAttributes;
	private DogmaAttribute primaryAttributeDogma;
	private DogmaAttribute secondaryAttributeDogma;
	private DogmaAttribute skillTimeConstantDogma;
	private DogmaAttribute canNotBeTrainedOnTrialDogma;

	@Inject
	protected SkillDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Creating skills");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			skills = storeHandler.getRefStore("skills");
			dogmaAttributes = storeHandler.getRefStore("dogmaAttributes");
			primaryAttributeDogma =
					helper.getDogmaAttributeByName("primaryAttribute").orElseThrow();
			secondaryAttributeDogma =
					helper.getDogmaAttributeByName("secondaryAttribute").orElseThrow();
			skillTimeConstantDogma =
					helper.getDogmaAttributeByName("skillTimeConstant").orElseThrow();
			canNotBeTrainedOnTrialDogma =
					helper.getDogmaAttributeByName("canNotBeTrainedOnTrial").orElseThrow();

			var requiredSkillDogma = getRequiredSkillDogmas(helper);

			for (var entry : types.entrySet()) {
				long typeId = entry.getKey();
				var save = false;
				var typeJson = (ObjectNode) entry.getValue();
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				var isSkill = isSkill(typeId, helper);
				if (isSkill) {
					typeJson.put("is_skill", true);
					save = true;
				}
				var requiredSkills = createRequiredSkills(type, requiredSkillDogma, helper);
				if (requiredSkills != null) {
					typeJson.put("required_skills", objectMapper.valueToTree(requiredSkills));
					save = true;
				}
				if (save) {
					types.put(typeId, typeJson);
				}

				if (isSkill) {
					try {
						createSkill(typeId, type, requiredSkills);
					} catch (Exception e) {
						throw new RuntimeException("Failed creating skill for type " + typeId, e);
					}
				}
			}
		});
	}

	private boolean isSkill(long typeId, @NonNull StoreDataHelper helper) {
		var categoryId = helper.getCategoryForType(typeId);
		if (categoryId.isEmpty()) {
			return false;
		}
		return categoryId.get() == SKILL_CATEGORY_ID;
	}

	private List<Pair<Long, Long>> getRequiredSkillDogmas(@NonNull StoreDataHelper helper) {
		return Stream.iterate(1, integer -> integer + 1)
				.map(new Function<Integer, Optional<Pair<Long, Long>>>() {
					@Override
					public Optional<Pair<Long, Long>> apply(Integer i) {
						var req = helper.getDogmaAttributeByName("requiredSkill" + i);
						var level = helper.getDogmaAttributeByName("requiredSkill" + i + "Level");
						if (req.isEmpty() || level.isEmpty()) {
							return Optional.empty();
						}
						return Optional.of(Pair.of((long) req.get().getAttributeId(), (long)
								level.get().getAttributeId()));
					}
				})
				.takeWhile(Optional::isPresent)
				.map(o -> o.get())
				.toList();
	}

	private Map<Long, Integer> createRequiredSkills(
			@NotNull InventoryType type,
			@NotNull List<Pair<Long, Long>> requiredSkillsDogma,
			@NotNull StoreDataHelper helper) {
		var requiredSkills = new LinkedHashMap<Long, Integer>();
		for (Pair<Long, Long> dogmaPair : requiredSkillsDogma) {
			var skill = helper.getDogmaFromType(type, dogmaPair.getLeft());
			var level = helper.getDogmaFromType(type, dogmaPair.getRight());
			if (skill.isEmpty() || level.isEmpty()) {
				continue;
			}
			var skillId = (long) skill.get().getValue();
			var skillLevel = (int) level.get().getValue();
			requiredSkills.put(skillId, skillLevel);
		}
		return requiredSkills.isEmpty() ? null : requiredSkills;
	}

	private void createSkill(long typeId, @NonNull InventoryType type, Map<Long, Integer> requiredSkills) {
		var primaryTypeDogma = helper.getDogmaFromType(type, primaryAttributeDogma.getAttributeId());
		var secondaryTypeDogma = helper.getDogmaFromType(type, secondaryAttributeDogma.getAttributeId());
		if (primaryTypeDogma.isEmpty() || secondaryTypeDogma.isEmpty()) {
			log.warn(
					"Not creating skill for {} [{}], either primary or secondary attribute dogma is missing.",
					type.getName().get("en"),
					typeId);
			return;
		}

		var primaryDogmaId = (long) primaryTypeDogma.get().getValue();
		var secondaryDogmaId = (long) secondaryTypeDogma.get().getValue();

		var primaryDogma = objectMapper.convertValue(dogmaAttributes.get(primaryDogmaId), DogmaAttribute.class);
		var secondaryDogma = objectMapper.convertValue(dogmaAttributes.get(secondaryDogmaId), DogmaAttribute.class);

		var primaryCharacterAttributeId = ATTRIBUTE_ID_MAP.get(primaryDogma.getName());
		var secondaryCharacterAttributeId = ATTRIBUTE_ID_MAP.get(secondaryDogma.getName());

		var skillTimeConstantId = skillTimeConstantDogma.getAttributeId();
		var mult = (int)
				helper.getDogmaFromType(type, skillTimeConstantId).orElseThrow().getValue();

		// The default value is 1, but all skills which don't specify this attribute can be trained on alpha.
		// Therefore, we default to a 0.
		var canNotBeTrainedOnTrial = helper.getDogmaFromType(type, canNotBeTrainedOnTrialDogma.getAttributeId())
						.map(v -> (int) v.getValue())
						.orElse(0)
				== 1L;

		var skill = Skill.builder()
				.typeId(typeId)
				.primaryDogmaAttributeId(primaryDogmaId)
				.secondaryDogmaAttributeId(secondaryDogmaId)
				.primaryCharacterAttributeId(primaryCharacterAttributeId)
				.secondaryCharacterAttributeId(secondaryCharacterAttributeId)
				.trainingTimeMultiplier(mult)
				.canNotBeTrainedOnTrial(canNotBeTrainedOnTrial)
				.requiredSkills(requiredSkills);
		log.trace("Created skill: {}", typeId);
		skills.put(typeId, objectMapper.valueToTree(skill.build()));
	}
}
