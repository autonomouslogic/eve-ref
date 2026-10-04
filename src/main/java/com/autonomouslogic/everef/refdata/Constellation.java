package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
public class Constellation {
	@JsonProperty
	Long constellationId;

	@JsonProperty
	Long regionId;

	@JsonProperty
	Long factionId;

	@JsonProperty
	Long wormholeClassId;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> name;

	@JsonProperty
	Coordinate position;

	@JsonProperty
	@Schema(description = "Computed from the solar systems in this constellation.")
	List<Long> solarSystemIds;
}
