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
public class TraitBonus {
	@JsonProperty
	Double bonus;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> bonusText;

	@JsonProperty
	Integer importance;

	@JsonProperty
	Boolean isPositive;

	@JsonProperty
	Long unitId;
}
