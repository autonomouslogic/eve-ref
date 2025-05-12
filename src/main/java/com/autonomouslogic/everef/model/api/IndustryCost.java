package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class IndustryCost {
	@JsonProperty
	long typeId;

	@JsonProperty
	@Singular
	Map<String, ActivityCost> manufacturingCosts;

	@JsonProperty
	@Singular
	Map<String, ActivityCost> inventionCosts;

	@JsonProperty
	@Singular
	Map<String, ActivityCost> copyingCosts;

	@JsonProperty
	@Singular
	Map<String, ActivityCost> meResearchCosts;

	@JsonProperty
	@Singular
	Map<String, ActivityCost> teResearchCosts;
}
