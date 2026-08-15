package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class BlueprintActivity {
	@JsonProperty
	Long time;

	@JsonProperty
	@Singular
	Map<Long, BlueprintMaterial> materials;

	@JsonProperty
	@Singular
	Map<Long, BlueprintMaterial> products;

	@JsonProperty
	@Singular
	Map<Long, Integer> requiredSkills;
}
