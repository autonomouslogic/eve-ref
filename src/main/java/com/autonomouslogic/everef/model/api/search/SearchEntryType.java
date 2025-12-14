package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema
@AllArgsConstructor
@Getter
public enum SearchEntryType {
	@JsonProperty("inventory_type")
	INVENTORY_TYPE("types", "types"),
	@JsonProperty("market_group")
	MARKET_GROUP("market-groups", "market_groups"),
	@JsonProperty("category")
	CATEGORY("categories", "categories"),
	@JsonProperty("group")
	GROUP("groups", "groups");

	private final String eveRefType;
	private final String refDataType;
}
