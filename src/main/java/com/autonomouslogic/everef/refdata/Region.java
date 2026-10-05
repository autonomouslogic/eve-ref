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
public class Region {
	@JsonProperty
	Long regionId;

	@JsonProperty
	String universeId;

	@JsonProperty
	Long wormholeClassId;

	@JsonProperty
	Long nebulaId;

	@JsonProperty
	Long nameId;

	@JsonProperty
	Long descriptionId;

	@JsonProperty
	Long factionId;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> name;

	@JsonProperty
	@Schema(description = "The key is the language code.")
	Map<String, String> description;

	@JsonProperty
	Coordinate position;

	@JsonProperty
	@Schema(description = "Computed from the solar systems in this region.")
	List<Long> solarSystemIds;

	@JsonProperty
	@Schema(description = "Computed from the constellations in this region.")
	List<Long> constellationIds;

	@JsonProperty
	@Schema(description = "The region IDs this region is connected to via stargates in its solar systems.")
	List<Long> stargatesToRegionIds;
}
