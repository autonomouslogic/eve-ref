package com.autonomouslogic.everef.model.fuzzwork;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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
