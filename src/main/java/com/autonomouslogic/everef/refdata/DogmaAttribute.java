package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class DogmaAttribute {
	@JsonProperty
	Long attributeId;

	@JsonProperty
	Long categoryId;

	@JsonProperty
	Integer chargeRechargeTimeId;

	@JsonProperty
	Integer dataType;

	@JsonProperty
	Double defaultValue;

	@JsonProperty
	Map<String, String> description;

	@JsonProperty
	Map<String, String> displayName;

	@JsonProperty
	Boolean displayWhenZero;

	@JsonProperty
	Boolean highIsGood;

	@JsonProperty
	Integer iconId;

	@JsonProperty
	Long minAttributeId;

	@JsonProperty
	Long maxAttributeId;

	@JsonProperty
	String name;

	@JsonProperty
	Boolean published;

	@JsonProperty
	Boolean stackable;

	@JsonProperty
	Map<String, String> tooltipDescription;

	@JsonProperty
	Map<String, String> tooltipTitle;

	@JsonProperty
	Long unitId;
}
