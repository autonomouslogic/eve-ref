package com.autonomouslogic.everef.refdata;

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
@Schema
public class Schematic {
	@JsonProperty
	Long schematicId;

	@JsonProperty
	Map<String, String> name;

	@JsonProperty
	Long cycleTime;

	@JsonProperty
	Map<Long, BlueprintMaterial> materials;

	@JsonProperty
	Map<Long, BlueprintMaterial> products;

	@JsonProperty
	List<Long> pinTypeIds;
}
