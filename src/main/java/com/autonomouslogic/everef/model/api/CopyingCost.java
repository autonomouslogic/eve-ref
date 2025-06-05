package com.autonomouslogic.everef.model.api;

import com.autonomouslogic.everef.util.MathUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
public class CopyingCost extends ActivityCost {
	@JsonProperty
	BigDecimal jobCostBase;

	@JsonProperty
	BigDecimal totalCostPerRun;

	public CopyingCost multiply(double mul) {
		return ((CopyingCost.Builder<?, ?>) super.multiply(this.toBuilder(), mul))
				.jobCostBase(MathUtil.round(jobCostBase.multiply(BigDecimal.valueOf(mul)), 2))
				.totalCostPerRun(MathUtil.round(totalCostPerRun.multiply(BigDecimal.valueOf(mul)), 2))
				.build();
	}
}
