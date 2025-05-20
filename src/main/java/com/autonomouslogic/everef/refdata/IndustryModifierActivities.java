package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndustryModifierActivities {
	@JsonProperty
	@Schema(description = "List of affected categories or groups when doing material efficiency research.")
	List<Long> researchMaterial;

	@JsonProperty
	@Schema(description = "List of affected categories or groups when doing time efficiency research.")
	List<Long> researchTime;

	@JsonProperty
	@Schema(description = "List of affected categories or groups when doing manufacturing.")
	List<Long> manufacturing;

	@JsonProperty
	@Schema(description = "List of affected categories or groups when doing invention.")
	List<Long> invention;

	@JsonProperty
	@Schema(description = "List of affected categories or groups when doing copying.")
	List<Long> copying;
}
