package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({"materialTypeId", "activity", "quantity"})
@Schema(description = "Details a material used in a blueprint. This is added by EVE Ref.")
public class UsedInBlueprint {
	@JsonProperty
	@Schema(description = "The material type ID.")
	Long materialTypeId;

	@JsonProperty
	@Schema(description = "The activity this material is used in.")
	String activity;

	@JsonProperty
	@Schema(description = "The quantity.")
	Long quantity;
}
