package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StoreDataHelper {
	private final StoreHandler storeHandler;

	public Optional<Long> getCategoryForType(long typeId) {
		var types = storeHandler.getRefStore("types");
		var groups = storeHandler.getRefStore("groups");
		return Optional.ofNullable(types.get(typeId))
				.flatMap(type -> Optional.ofNullable(type.get("group_id")).map(JsonNode::asLong))
				.flatMap(groupId -> Optional.ofNullable(groups.get(groupId)))
				.flatMap(group -> Optional.ofNullable(group.get("category_id")).map(JsonNode::asLong));
	}
}
