package com.autonomouslogic.everef.model.fuzzwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class FuzzworkAggregatedMarketSegment {
	@JsonProperty
	BigDecimal weightedAverage;

	@JsonProperty
	BigDecimal max;

	@JsonProperty
	BigDecimal min;

	@JsonProperty
	BigDecimal stddev;

	@JsonProperty
	BigDecimal median;

	@JsonProperty
	BigDecimal volume;

	@JsonProperty
	int orderCount;

	@JsonProperty
	BigDecimal percentile;
}
