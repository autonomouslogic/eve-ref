package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.DogmaTypeAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StoreDataHelper {
	private final StoreHandler storeHandler;
	private final ObjectMapper objectMapper;

	public Optional<Long> getCategoryForType(long typeId) {
		var types = storeHandler.getRefStore("types");
		var groups = storeHandler.getRefStore("groups");
		return Optional.ofNullable(types.get(typeId))
				.flatMap(type -> Optional.ofNullable(type.get("group_id")).map(JsonNode::asLong))
				.flatMap(groupId -> Optional.ofNullable(groups.get(groupId)))
				.flatMap(group -> Optional.ofNullable(group.get("category_id")).map(JsonNode::asLong));
	}

	public Optional<DogmaAttribute> getDogmaAttributeByName(String attributeName) {
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
				.findFirst()
				.map(json -> objectMapper.convertValue(json, DogmaAttribute.class));
	}

	public Optional<DogmaTypeAttribute> getDogmaFromType(InventoryType type, long attributeId) {
		var attrs = type.getDogmaAttributes();
		if (attrs == null || attrs.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(attrs.get(Long.toString(attributeId)));
	}
}
