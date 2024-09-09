package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Populates which types can be reprocessed using what skills.
 * This is found by cross-referencing dogma attribute reprocessingSkillType [790] on the skills.
 */
@Log4j2
public class ReprocessableTypesDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private Map<Long, JsonNode> types;
	private Map<Long, JsonNode> skills;
	private StoreDataHelper helper;
	private DogmaAttribute reprocessingSkillType;

	@Inject
	protected ReprocessableTypesDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populates reprocessable types");
			helper = new StoreDataHelper(storeHandler, objectMapper);
			types = storeHandler.getRefStore("types");
			skills = storeHandler.getRefStore("skills");
			reprocessingSkillType =
					helper.getDogmaAttributeByName("reprocessingSkillType").orElseThrow();
			crossReferenceTypes();
		});
	}

	private void crossReferenceTypes() {
		for (var typeJson : types.values()) {
			var type = objectMapper.convertValue(typeJson, InventoryType.class);
			var dogmaValue = helper.getDogmaFromType(type, reprocessingSkillType.getAttributeId());
			if (dogmaValue.isEmpty()) {
				continue;
			}
			var skillTypeId = dogmaValue.get().getValue().longValue();
			var skillJson = skills.get(skillTypeId);
			if (skillJson == null) {
				log.warn("Skill {} not found for type {}", skillTypeId, type.getTypeId());
				continue;
			}
			skillJson.withArrayProperty("reprocessable_type_ids").add(type.getTypeId());
			skills.put(skillTypeId, skillJson);
		}
	}
}
