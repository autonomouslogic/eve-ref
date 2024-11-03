package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Populates category IDs on types.
 */
@Log4j2
public class CategoryIdDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

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
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				var groupJson = groups.get(type.getGroupId());
				if (groupJson == null) {
					log.warn("Group {} not found for type {}", type.getGroupId(), typeId);
					return;
				}
				var group = objectMapper.convertValue(groups.get(type.getGroupId()), InventoryType.class);
				((ObjectNode) typeJson).put("category_id", group.getGroupId());
				types.put(typeId, typeJson);
			});
		});
	}
}
