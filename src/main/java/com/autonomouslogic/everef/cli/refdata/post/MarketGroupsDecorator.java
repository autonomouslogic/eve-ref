package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.refdata.MarketGroup;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

/**
 * References market groups and types on other market groups.
 */
@Log4j2
public class MarketGroupsDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected MarketGroupsDecorator() {}

	public Completable create() {
		return Completable.concatArray(referenceChildGroups());
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
				if (parentId == null) {
					continue;
				}
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
}
