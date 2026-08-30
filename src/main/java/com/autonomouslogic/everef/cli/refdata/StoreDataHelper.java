package com.autonomouslogic.everef.cli.refdata;

import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.DogmaTypeAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@RequiredArgsConstructor
public class StoreDataHelper {
	private final StoreHandler storeHandler;
	private final JsonMapper jsonMapper;

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
				.map(json -> jsonMapper.convertValue(json, DogmaAttribute.class));
	}

	public Optional<DogmaTypeAttribute> getDogmaFromType(InventoryType type, long attributeId) {
		var attrs = type.getDogmaAttributes();
		if (attrs == null || attrs.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(attrs.get(Long.toString(attributeId)));
	}

	public List<InventoryType> getTypesWithDogmaAttribute(long attributeId) {
		var attributeIdString = Long.toString(attributeId);
		return storeHandler.getRefStore("types").values().stream()
				.map(n -> jsonMapper.convertValue(n, InventoryType.class))
				.filter(type -> {
					return Optional.ofNullable(type.getDogmaAttributes())
							.map(attrs -> attrs.containsKey(attributeIdString))
							.orElse(false);
				})
				.toList();
	}
}
