package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SkillCreator {
	private static final int SKILL_CATEGORY_ID = 16;

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
			for (long typeId : types.keySet()) {
				var type = (ObjectNode) types.get(typeId);
				if (setIsSkill(typeId, type, helper)) {
					types.put(typeId, type);
				}
			}
		});
	}

	private boolean setIsSkill(long typeId, ObjectNode type, StoreDataHelper helper) {
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
}
