package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
		description = "Details about how much a dogma attribute can be modified by a mutaplasmid. "
				+ "These are created by EVE Ref and derived from Hoboleaks.")
public class MutaplasmidDogmaModifications {
	@JsonProperty
	Double min;

	@JsonProperty
	Double max;

	@JsonProperty
	Boolean highIsGood;
}
