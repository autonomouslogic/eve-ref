package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
public class Moon {
	@JsonProperty
	Long moonId;

	@JsonProperty
	Long solarSystemId;

	@JsonProperty
	Long typeId;

	@JsonProperty
	Long orbitId;

	@JsonProperty
	Long orbitIndex;

	@JsonProperty
	Long celestialIndex;

	@JsonProperty
	@Schema(description = "The key is the language code. Only present on moons with a renamed, unique name.")
	Map<String, String> uniqueName;

	@JsonProperty
	Coordinate position;

	@JsonProperty
	BigDecimal radius;

	@JsonProperty
	List<Long> npcStationIds;

	@JsonProperty
	PlanetAttributes attributes;

	@JsonProperty
	PlanetStatistics statistics;
}
