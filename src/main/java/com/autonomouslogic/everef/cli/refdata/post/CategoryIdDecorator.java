package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.InventoryType;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Populates category IDs on types.
 */
@Log4j2
public class CategoryIdDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	private Map<Long, JsonNode> groups;
	private Map<Long, JsonNode> types;

	@Inject
	protected CategoryIdDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Decorating categories");
			types = storeHandler.getRefStore("types");
			groups = storeHandler.getRefStore("groups");
			types.forEach((typeId, typeJson) -> {
				var type = jsonMapper.convertValue(typeJson, InventoryType.class);
				var groupJson = groups.get(type.getGroupId());
				if (groupJson == null) {
					log.warn("Group {} not found for type {}", type.getGroupId(), typeId);
					return;
				}
				var group = jsonMapper.convertValue(groups.get(type.getGroupId()), InventoryType.class);
				((ObjectNode) typeJson).put("category_id", group.getCategoryId());
				types.put(typeId, typeJson);
			});
		});
	}
}
