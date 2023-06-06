package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Details about skill types. These are created by EVE Ref.")
public class Skill {
	@JsonProperty
	long typeId;

	@JsonProperty
	long primaryDogmaAttributeId;

	@JsonProperty
	long secondaryDogmaAttributeId;

	;

	@JsonProperty
	long primaryCharacterAttributeId;

	@JsonProperty
	long secondaryCharacterAttributeId;

	@JsonProperty
	int trainingTimeMultiplier;

	@JsonProperty
	boolean canNotBeTrainedOnTrial;
}
