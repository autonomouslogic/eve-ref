package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * References inventory types on inventory groups.
 */
@Log4j2
public class TypesDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TypesDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Referencing types on groups");
			var types = storeHandler.getRefStore("types");
			var groups = storeHandler.getRefStore("groups");
			var marketGroups = storeHandler.getRefStore("marketGroups");
			var metaGroups = storeHandler.getRefStore("metaGroups");
			for (var typeJson : types.values()) {
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				addTypeToGroup(type, groups, type.getGroupId());
				addTypeToGroup(type, marketGroups, type.getMarketGroupId());
				addTypeToGroup(type, metaGroups, type.getMetaGroupId());
			}
		});
	}

	private static void addTypeToGroup(InventoryType type, Map<Long, JsonNode> groups, Long groupId) {
		if (groupId == null) {
			return;
		}
		var groupJson = groups.get(groupId);
		if (groupJson == null) {
			return;
		}
		((ArrayNode) groupJson.withArray("type_ids")).add(type.getTypeId());
		groups.put(groupId, groupJson);
	}
}
