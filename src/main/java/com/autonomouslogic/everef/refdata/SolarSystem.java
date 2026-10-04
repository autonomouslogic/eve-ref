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
public class SolarSystem {
	@JsonProperty
	Long solarSystemId;

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
	List<Long> stargateIds;

	@JsonProperty
	@Schema(description = "The solar system IDs this system is connected to via its stargates.")
	List<Long> stargatesToSystemIds;

	@JsonProperty
	List<Long> planetIds;

	@JsonProperty
	List<Long> stationIds;

	@JsonProperty
	Long starId;

	@JsonProperty
	Coordinate position;

	@JsonProperty
	Coordinate2D position2D;

	@JsonProperty
	BigDecimal radius;

	@JsonProperty
	String securityClass;

	@JsonProperty
	BigDecimal securityStatus;

	@JsonProperty
	BigDecimal luminosity;

	@JsonProperty
	Boolean border;

	@JsonProperty
	Boolean corridor;

	@JsonProperty
	Boolean fringe;

	@JsonProperty
	Boolean hub;

	@JsonProperty
	Boolean international;

	@JsonProperty
	Boolean regional;

	@JsonProperty
	String visualEffect;

	@JsonProperty
	List<Long> disallowedAnchorCategories;

	@JsonProperty
	List<Long> disallowedAnchorGroups;
}
