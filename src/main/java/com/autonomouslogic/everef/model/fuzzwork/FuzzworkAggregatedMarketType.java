package com.autonomouslogic.everef.model.fuzzwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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
