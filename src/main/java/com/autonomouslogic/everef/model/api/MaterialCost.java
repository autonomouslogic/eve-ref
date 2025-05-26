package com.autonomouslogic.everef.model.api;

import com.autonomouslogic.everef.util.MathUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class MaterialCost {
	@JsonProperty
	long typeId;

	@JsonProperty
	double quantity;

	@JsonProperty
	BigDecimal costPerUnit;

	@JsonProperty
	BigDecimal cost;

	public MaterialCost multiply(double mul) {
		return new MaterialCost(
				typeId, quantity * mul, costPerUnit, MathUtil.round(cost.multiply(new BigDecimal(mul)), 2));
	}

	public static Map<String, MaterialCost> multiply(Map<String, MaterialCost> costs, double mul) {
		var newCosts = new LinkedHashMap<String, MaterialCost>();
		costs.entrySet()
				.forEach(entry -> newCosts.put(entry.getKey(), entry.getValue().multiply(mul)));
		return newCosts;
	}
}
