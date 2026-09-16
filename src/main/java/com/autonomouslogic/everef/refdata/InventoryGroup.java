package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "An inventory group")
public class InventoryGroup {
	@JsonProperty
	Long groupId;

	@JsonProperty
	Long categoryId;

	@JsonProperty
	Long iconId;

	@JsonProperty
	Map<String, String> name;

	@JsonProperty
	Boolean anchorable;

	@JsonProperty
	Boolean anchored;

	@JsonProperty
	Boolean fittableNonSingleton;

	@JsonProperty
	Boolean published;

	@JsonProperty
	Boolean useBasePrice;

	@JsonProperty
	@Schema(description = "The type IDs in this group. This is added by EVE Ref.")
	List<Long> typeIds;
}
