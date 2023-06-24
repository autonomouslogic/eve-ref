package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details about skill types. These are created by EVE Ref.")
public class Skill {
	@JsonProperty
	long typeId;

	@JsonProperty
	@Schema(description = "The dogma attribute ID of the primary training attribute.")
	long primaryDogmaAttributeId;

	@JsonProperty
	@Schema(description = "The dogma attribute ID of the secondary training attribute.")
	long secondaryDogmaAttributeId;

	@JsonProperty
	@Schema(description = "The character attribute ID of the primary training attribute.")
	long primaryCharacterAttributeId;

	@JsonProperty
	@Schema(description = "The character attribute ID of the secondary training attribute.")
	long secondaryCharacterAttributeId;

	@JsonProperty
	int trainingTimeMultiplier;

	@JsonProperty
	boolean canNotBeTrainedOnTrial;

	@JsonProperty
	@Schema(description = "The other skills required for this skill.")
	Map<Long, Integer> requiredSkills;
}
