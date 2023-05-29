package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "An inventory group")
public class InventoryGroup {
	@JsonProperty
	Long categoryId;

	@JsonProperty
	Long groupId;

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
}
