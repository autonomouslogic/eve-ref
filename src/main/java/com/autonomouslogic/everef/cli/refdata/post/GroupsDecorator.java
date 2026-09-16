package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.InventoryGroup;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * References inventory groups on inventory categories.
 */
@Log4j2
public class GroupsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected GroupsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Referencing groups on categories");
			var groups = storeHandler.getRefStore("groups");
			var categories = storeHandler.getRefStore("categories");
			for (var groupJson : groups.values()) {
				var group = jsonMapper.convertValue(groupJson, InventoryGroup.class);
				var categoryId = group.getCategoryId();
				var categoryJson = categories.get(categoryId);
				if (categoryJson == null) {
					continue;
				}
				((ArrayNode) categoryJson.withArray("group_ids")).add(group.getGroupId());
				categories.put(categoryId, categoryJson);
			}
		});
	}
}
