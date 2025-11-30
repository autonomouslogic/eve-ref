package com.autonomouslogic.everef.model.api.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class SearchResult {

	@Schema(description = "The original search input provided by the user")
	@JsonProperty
	String input;

	@Schema(description = "List of matching inventoryType")
	@JsonProperty("inventory_type")
	@Singular("inventoryType")
	List<SearchInventoryType> searchInventoryType;
}
