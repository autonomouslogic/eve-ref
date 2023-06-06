package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
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
public class SkillCreator {
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
	private ObjectNode primaryAttributeDogma;
	private ObjectNode secondaryAttributeDogma;
	private ObjectNode skillTimeConstantDogma;
	private ObjectNode canNotBeTrainedOnTrialDogma;

	@Inject
	protected SkillCreator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Creating skills");
			helper = new StoreDataHelper(storeHandler);
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

			for (long typeId : types.keySet()) {
				var save = false;
				var type = (ObjectNode) types.get(typeId);
				var isSkill = isSkill(typeId, type, helper);
				if (isSkill) {
					type.put("is_skill", true);
					save = true;
				}
				var requiredSkills = createRequiredSkills(type, requiredSkillDogma, helper);
				if (requiredSkills != null) {
					type.put("required_skills", objectMapper.valueToTree(requiredSkills));
					save = true;
				}
				if (save) {
					types.put(typeId, type);
				}

				if (isSkill) {
					var primaryAttributeId =
							primaryAttributeDogma.get("attribute_id").asLong();
					var secondaryAttributeId =
							secondaryAttributeDogma.get("attribute_id").asLong();
					var primaryAttributeDogmaId = helper.getDogmaFromType(type, primaryAttributeId)
							.orElseThrow()
							.get("value")
							.asLong();
					var secondaryAttributeDogmaId = helper.getDogmaFromType(type, secondaryAttributeId)
							.orElseThrow()
							.get("value")
							.asLong();
					var primaryAttributeDogma = dogmaAttributes.get(primaryAttributeDogmaId);
					var secondaryAttributeDogma = dogmaAttributes.get(secondaryAttributeDogmaId);
					var primaryCharacterAttributeId = ATTRIBUTE_ID_MAP.get(
							primaryAttributeDogma.get("name").asText());
					var secondaryCharacterAttributeId = ATTRIBUTE_ID_MAP.get(
							secondaryAttributeDogma.get("name").asText());

					var skillTimeConstantId =
							skillTimeConstantDogma.get("attribute_id").asLong();
					var mult = helper.getDogmaFromType(type, skillTimeConstantId)
							.orElseThrow()
							.get("value")
							.asInt();
					var canNotBeTrainedOnTrial = helper.getDogmaFromType(
											type,
											canNotBeTrainedOnTrialDogma
													.get("attribute_id")
													.asLong())
									.map(v -> v.get("value").asLong())
									.orElseGet(() -> canNotBeTrainedOnTrialDogma
											.get("default_value")
											.asLong())
							== 1L;

					var skill = Skill.builder()
							.typeId(typeId)
							.primaryDogmaAttributeId(primaryAttributeDogmaId)
							.secondaryDogmaAttributeId(secondaryAttributeDogmaId)
							.primaryCharacterAttributeId(primaryCharacterAttributeId)
							.secondaryCharacterAttributeId(secondaryCharacterAttributeId)
							.trainingTimeMultiplier(mult)
							.canNotBeTrainedOnTrial(canNotBeTrainedOnTrial);
					log.trace("Created skill: {}", skill.build());
					storeHandler.getRefStore("skills").put(typeId, objectMapper.valueToTree(skill.build()));
				}
			}
		});
	}

	private boolean isSkill(long typeId, @NonNull ObjectNode type, @NonNull StoreDataHelper helper) {
		var categoryId = helper.getCategoryForType(typeId);
		if (categoryId.isEmpty()) {
			return false;
		}
		var isSkill = categoryId.get() == SKILL_CATEGORY_ID;
		if (!isSkill) {
			return false;
		}
		return true;
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
						return Optional.of(Pair.of(
								req.get().get("attribute_id").asLong(),
								level.get().get("attribute_id").asLong()));
					}
				})
				.takeWhile(Optional::isPresent)
				.map(o -> o.get())
				.toList();
	}

	private ObjectNode createRequiredSkills(
			@NotNull ObjectNode type,
			@NotNull List<Pair<Long, Long>> requiredSkillsDogma,
			@NotNull StoreDataHelper helper) {
		var requiredSkills = new LinkedHashMap<Long, Long>();
		for (Pair<Long, Long> dogmaPair : requiredSkillsDogma) {
			var skill = helper.getDogmaFromType(type, dogmaPair.getLeft());
			var level = helper.getDogmaFromType(type, dogmaPair.getRight());
			if (skill.isEmpty() || level.isEmpty()) {
				continue;
			}
			var skillId = skill.get().get("value").asLong();
			var skillLevel = level.get().get("value").asLong();
			requiredSkills.put(skillId, skillLevel);
		}
		return requiredSkills.isEmpty() ? null : objectMapper.valueToTree(requiredSkills);
	}
}
