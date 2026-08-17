package com.autonomouslogic.everef.model.fuzzwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class FuzzworkAggregatedMarketType {
	@JsonProperty
	FuzzworkAggregatedMarketSegment buy;

	@JsonProperty
	FuzzworkAggregatedMarketSegment sell;
}
