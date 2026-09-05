package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A single search result entry.")
public class SearchEntry {
	@JsonProperty
	@Schema(
			description =
					"Display name of the entity. For market groups the full ancestry is shown joined with \" > \" (e.g. \"Ships > Battleships\"). For all other types this is the plain English name.")
	String title;

	@JsonProperty
	@Schema(description = "Language of the title field. Currently always \"en\".")
	String language;

	@JsonProperty
	@Schema(
			description =
					"Numeric ID of the entity. Interpretation depends on type: type ID for inventory_type, market group ID for market_group, category ID for category, and group ID for group.")
	long id;

	@JsonProperty
	@Schema(
			description =
					"Human-readable label for the entity type. For inventory_type entries this is the name of the root market group (e.g. \"Manufacture & Research\"), or \"Inventory type\" when no market group exists. For market_group entries this is always \"Market group\". For category entries this is always \"Inventory category\". For group entries this is always \"Inventory group\".")
	String typeName;

	@JsonProperty
	SearchEntryType type;

	@JsonProperty
	SearchEntryUrls urls;

	@JsonIgnore
	@Hidden
	String query;

	@Schema(description = "Relevance score of the search result. Lower is better.")
	@JsonProperty
	long relevance;
}
