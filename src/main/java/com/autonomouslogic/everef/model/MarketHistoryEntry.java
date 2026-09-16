package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MarketHistoryEntry {

	@JsonProperty
	LocalDate date;

	@JsonProperty
	int regionId;

	@JsonProperty
	int typeId;

	@JsonProperty
	BigDecimal average;

	@JsonProperty
	BigDecimal highest;

	@JsonProperty
	BigDecimal lowest;

	@JsonProperty
	long volume;

	@JsonProperty
	int orderCount;

	@JsonProperty
	Instant httpLastModified;
}
