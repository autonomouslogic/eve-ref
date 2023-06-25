package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "An inventory category")
public class InventoryCategory {
	@JsonProperty
	Long categoryId;

	@JsonProperty
	Map<String, String> name;

	@JsonProperty
	Boolean published;

	@JsonProperty
	Long iconId;

	@JsonProperty
	@Schema(description = "The group IDs in this category. This is added by EVE Ref.")
	List<Long> groupIds;
}
