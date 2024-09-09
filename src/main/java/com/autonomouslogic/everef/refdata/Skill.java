package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details about skill types. These are created entirely by EVE Ref.")
public class Skill {
	@JsonProperty
	Long typeId;

	@JsonProperty
	@Schema(description = "The dogma attribute ID of the primary training attribute.")
	Long primaryDogmaAttributeId;

	@JsonProperty
	@Schema(description = "The dogma attribute ID of the secondary training attribute.")
	Long secondaryDogmaAttributeId;

	@JsonProperty
	@Schema(description = "The character attribute ID of the primary training attribute.")
	Long primaryCharacterAttributeId;

	@JsonProperty
	@Schema(description = "The character attribute ID of the secondary training attribute.")
	Long secondaryCharacterAttributeId;

	@JsonProperty
	Integer trainingTimeMultiplier;

	@JsonProperty
	Boolean canNotBeTrainedOnTrial;

	@JsonProperty
	@Schema(description = "The other skills required for this skill.")
	Map<Long, Integer> requiredSkills;

	@JsonProperty
	@Schema(
			description =
					"Which type IDs this skill can be used to reprocess. This is found by cross-referencing dogma attribute reprocessingSkillType [790] on the skills.")
	List<Long> reprocessableTypeIds;
}
