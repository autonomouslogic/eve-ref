package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class DogmaAttribute {
	@JsonProperty
	Integer attributeId;

	@JsonProperty
	Integer categoryId;

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
	Integer maxAttributeId;

	@JsonProperty
	String name;

	@JsonProperty
	Boolean published;

	@JsonProperty
	Boolean stackable;

	@JsonProperty
	Map<String, String> tooltipDescriptionId;

	@JsonProperty
	Map<String, String> tooltipTitleId;

	@JsonProperty
	Integer unitId;
}