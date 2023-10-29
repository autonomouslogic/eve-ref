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
