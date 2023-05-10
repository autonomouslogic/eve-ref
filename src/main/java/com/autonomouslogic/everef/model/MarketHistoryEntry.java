package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MarketHistoryEntry {
	BigDecimal average;
	BigDecimal date;
	BigDecimal highest;
	BigDecimal lowest;
	int orderCount;
	long volume;
	long regionId;
	long typeId;
	Instant httpLastModified;
}
