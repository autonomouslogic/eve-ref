package com.autonomouslogic.everef.model.api;

import com.autonomouslogic.everef.util.MathUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema
public class CostSummary {
	@JsonProperty
	@Schema(implementation = String.class)
	Duration time;

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

	protected CostSummary multiply(double mul) {
		var builder = toBuilder();
		if (time != null) {
			builder = builder.time(MathUtil.multiply(time, mul));
		}
		if (facilityTax != null) {
			builder = builder.totalMaterialCost(MathUtil.round(facilityTax.multiply(BigDecimal.valueOf(mul)), 2));
		}
		if (sccSurcharge != null) {
			builder = builder.totalMaterialCost(MathUtil.round(sccSurcharge.multiply(BigDecimal.valueOf(mul)), 2));
		}
		if (alphaCloneTax != null) {
			builder = builder.totalMaterialCost(MathUtil.round(alphaCloneTax.multiply(BigDecimal.valueOf(mul)), 2));
		}
		if (totalJobCost != null) {
			builder = builder.totalMaterialCost(MathUtil.round(totalJobCost.multiply(BigDecimal.valueOf(mul)), 2));
		}
		if (totalCost != null) {
			builder = builder.totalMaterialCost(MathUtil.round(totalCost.multiply(BigDecimal.valueOf(mul)), 2));
		}
		if (totalMaterialCost != null) {
			builder = builder.totalMaterialCost(MathUtil.round(totalMaterialCost.multiply(BigDecimal.valueOf(mul)), 2));
		}
		return builder.build();
	}
}
