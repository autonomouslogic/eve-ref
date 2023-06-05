package com.autonomouslogic.everef.cli.refdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

	public Optional<ObjectNode> getDogmaAttributeByName(String attributeName) {
		var attributes = storeHandler.getRefStore("dogmaAttributes");
		return attributes.values().stream()
				.filter(attribute -> {
					var name = attribute.get("name");
					if (name == null || !name.isTextual()) {
						return false;
					}
					return name.asText().equals(attributeName);
				})
				.map(json -> (ObjectNode) json)
				.findFirst();
	}

	public Optional<ObjectNode> getDogmaFromType(ObjectNode type, long attributeId) {
		var attrs = type.get("dogma_attributes");
		if (attrs == null || !attrs.isObject()) {
			return Optional.empty();
		}
		return Optional.ofNullable((ObjectNode) attrs.get(Long.toString(attributeId)));
	}
}
