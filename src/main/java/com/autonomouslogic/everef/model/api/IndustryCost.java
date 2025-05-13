package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class IndustryCost {
	@JsonProperty
	IndustryCostInput input;

	@JsonProperty
	@Singular("manufacturing")
	Map<String, ActivityCost> manufacturing;

	@JsonProperty
	@Singular("invention")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	Map<String, InventionCost> invention;

	@JsonProperty
	@Singular("copying")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	Map<String, ActivityCost> copying;

	@JsonProperty
	@Singular("meResearch")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	Map<String, ActivityCost> meResearch;

	@JsonProperty
	@Singular("teResearch")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	Map<String, ActivityCost> teResearch;
}
