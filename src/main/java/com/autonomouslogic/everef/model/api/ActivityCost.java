package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
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
public class ActivityCost {
	@JsonProperty
	Long productTypeId;

	@JsonProperty
	Integer quantity;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration time;

	@JsonProperty
	@Singular
	Map<String, MaterialCost> materials;

	@JsonProperty
	BigDecimal estimatedItemValue;

	@JsonProperty
	BigDecimal systemCostIndex;

	@JsonProperty
	BigDecimal facilityTax;

	@JsonProperty
	BigDecimal sccSurcharge;
}
