package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
public class SkillCreator {
	private static final int SKILL_CATEGORY_ID = 16;

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected SkillCreator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Creating skills");
			var helper = new StoreDataHelper(storeHandler);
			var types = storeHandler.getRefStore("types");
			var requiredSkillDogma = getRequiredSkillDogmas(helper);

			for (long typeId : types.keySet()) {
				var type = (ObjectNode) types.get(typeId);
				if (setIsSkill(typeId, type, helper)) {
					types.put(typeId, type);
				}
				if (setRequiredSkills(typeId, type, requiredSkillDogma, helper)) {
					types.put(typeId, type);
				}
			}
		});
	}

	private boolean setIsSkill(long typeId, @NonNull ObjectNode type, @NonNull StoreDataHelper helper) {
		var categoryId = helper.getCategoryForType(typeId);
		if (categoryId.isEmpty()) {
			return false;
		}
		var isSkill = categoryId.get() == SKILL_CATEGORY_ID;
		if (!isSkill) {
			return false;
		}
		type.put("is_skill", true);
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

	private boolean setRequiredSkills(
			long typeId,
			@NonNull ObjectNode type,
			@NonNull List<Pair<Long, Long>> requiredSkillsDogma,
			@NonNull StoreDataHelper helper) {
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
		if (requiredSkills.isEmpty()) {
			return false;
		}
		type.put("required_skills", objectMapper.valueToTree(requiredSkills));
		return true;
	}
}
