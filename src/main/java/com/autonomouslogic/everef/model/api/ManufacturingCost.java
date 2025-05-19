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
public class ManufacturingCost extends ActivityCost {
	@JsonProperty
	@Schema(description = "The source blueprint of the manufacture")
	long blueprintId;

	@Schema(description = "Total number of item produced")
	@JsonProperty
	long units;

	@Schema(description = "Total number of item produced")
	@JsonProperty
	long unitsPerRun;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration timePerRun;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration timePerUnit;

	@JsonProperty
	BigDecimal totalCostPerRun;

	@JsonProperty
	BigDecimal totalCostPerUnit;
}
