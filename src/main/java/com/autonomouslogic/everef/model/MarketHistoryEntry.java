package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MarketHistoryEntry {
	@JsonProperty
	BigDecimal average;

	@JsonProperty
	LocalDate date;

	@JsonProperty
	BigDecimal highest;

	@JsonProperty
	BigDecimal lowest;

	@JsonProperty
	int orderCount;

	@JsonProperty
	long volume;

	@JsonProperty
	long regionId;

	@JsonProperty
	long typeId;

	@JsonProperty
	Instant httpLastModified;
}
