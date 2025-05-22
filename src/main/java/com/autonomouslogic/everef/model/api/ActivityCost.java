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
	long productId;

	@JsonProperty
	int runs;

	@JsonProperty
	@Schema(implementation = String.class)
	Duration time;

	@JsonProperty
	@Singular
	Map<String, MaterialCost> materials;

	@JsonProperty
	@Schema(description = "The estimated item value (EIV). This may not be completely accurate.")
	BigDecimal estimatedItemValue;

	@JsonProperty
	@Schema(
			description =
					"""
		The system cost index amount.
		Note that this will always be a slightly off, as the ESI does not report the full precision of the system cost index rates.
		See https://github.com/esi/esi-issues/issues/1411""")
	BigDecimal systemCostIndex;

	@JsonProperty
	@Schema(description = "Bonuses to system cost from structures, rigs, etc.")
	BigDecimal systemCostBonuses;

	@JsonProperty
	@Schema(description = "The facility amount")
	BigDecimal facilityTax;

	@JsonProperty
	@Schema(description = "The SCC surcharge amount")
	BigDecimal sccSurcharge;

	@JsonProperty
	@Schema(description = "The alpha clone tax amount")
	BigDecimal alphaCloneTax;

	@JsonProperty
	@Schema(description = "The total amount of ISK required to start the job")
	BigDecimal totalJobCost;

	@JsonProperty
	BigDecimal totalMaterialCost;

	@JsonProperty
	BigDecimal totalCost;
}
