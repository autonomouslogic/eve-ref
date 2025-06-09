package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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
