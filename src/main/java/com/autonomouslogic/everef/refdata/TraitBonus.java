package com.autonomouslogic.everef.refdata;

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
@Schema
public class TraitBonus {
	@JsonProperty
	double bonus;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> bonusText;

	@JsonProperty
	int importance;

	@JsonProperty
	Boolean isPositive;

	@JsonProperty
	int unitId;
}
