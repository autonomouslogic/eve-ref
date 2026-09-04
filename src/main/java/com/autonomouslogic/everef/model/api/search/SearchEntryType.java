package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Entity type discriminator for a search result entry.")
@AllArgsConstructor
@Getter
public enum SearchEntryType {
	@JsonProperty("inventory_type")
	@Schema(description = "An inventory type (item, ship, module, etc.).")
	INVENTORY_TYPE("types", "types"),

	@JsonProperty("market_group")
	@Schema(description = "A market browser category.")
	MARKET_GROUP("market-groups", "market_groups"),

	@JsonProperty("category")
	@Schema(description = "A top-level inventory category.")
	CATEGORY("categories", "categories"),

	@JsonProperty("group")
	@Schema(description = "An inventory group within a category.")
	GROUP("groups", "groups");

	private final String eveRefType;
	private final String refDataType;
}
