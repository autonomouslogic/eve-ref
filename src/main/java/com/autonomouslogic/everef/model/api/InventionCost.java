package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class InventionCost extends ActivityCost {
	@JsonProperty
	double probability;

	@Schema(description = "The number of invention runs")
	@JsonProperty
	int runs;

	@Schema(description = "The number of runs on each successfully invented copy")
	@JsonProperty
	int runsPerCopy;

	@JsonProperty
	int unitsPerRun;

	@JsonProperty
	double expectedCopies;

	@JsonProperty
	double expectedRuns;

	@JsonProperty
	double expectedUnits;

	@Schema(description = "The material efficiency of the invented blueprint")
	@JsonProperty
	int me;

	@Schema(description = "The time efficiency of the invented blueprint")
	@JsonProperty
	int te;

	@JsonProperty
	BigDecimal jobCostBase;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration avgTimePerCopy;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration avgTimePerRun;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration avgTimePerUnit;

	@JsonProperty
	BigDecimal avgCostPerCopy;

	@JsonProperty
	BigDecimal avgCostPerRun;

	@JsonProperty
	BigDecimal avgCostPerUnit;
}
