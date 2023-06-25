package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.reactivex.rxjava3.core.Completable;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

/**
 * References market groups and types on other market groups.
 */
@Log4j2
public class MarketGroupsDecorator implements PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	@Inject
	protected MarketGroupsDecorator() {}

	public Completable create() {
		return Completable.concatArray(referenceChildGroups(), referenceTypesOnGroups());
	}

	@NotNull
	private Completable referenceChildGroups() {
		return Completable.fromAction(() -> {
			log.info("Referencing market groups");
			var groups = storeHandler.getRefStore("marketGroups");
			for (var groupJson : groups.values()) {
				var group = objectMapper.convertValue(groupJson, MarketGroup.class);
				var groupId = group.getMarketGroupId();
				var parentId = group.getParentGroupId();
				var parentJson = groups.get(parentId);
				if (parentJson == null) {
					log.warn("Unable to reference market group {} on parent {}, parent not found", groupId, parentId);
					continue;
				}
				((ArrayNode) parentJson.withArray("child_market_group_ids")).add(groupId);
				groups.put(parentId, parentJson);
			}
		});
	}

	@NotNull
	private Completable referenceTypesOnGroups() {
		return Completable.fromAction(() -> {
			log.info("Referencing types on market groups");
			var types = storeHandler.getRefStore("types");
			var groups = storeHandler.getRefStore("marketGroups");
			for (var typeJson : types.values()) {
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				var typeId = type.getTypeId();
				var groupId = type.getMarketGroupId();
				if (groupId == null) {
					continue;
				}
				var groupJson = groups.get((long) groupId);
				if (groupJson == null) {
					log.warn("Unable to reference type {} on market group {}, market group not found", typeId, groupId);
					continue;
				}
				((ArrayNode) groupJson.withArray("type_ids")).add(typeId);
				groups.put((long) groupId, groupJson);
			}
		});
	}
}
