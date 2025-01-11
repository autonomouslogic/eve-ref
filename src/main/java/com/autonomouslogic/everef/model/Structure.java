package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class Structure {
	@JsonProperty
	String name;

	@JsonProperty
	Long ownerId;

	@JsonProperty
	Long solarSystemId;

	@JsonProperty
	Long constellationId;

	@JsonProperty
	Long regionId;

	@JsonProperty
	Long typeId;

	@JsonProperty
	Long structureId;

	@JsonProperty("is_gettable_structure")
	boolean gettableStructure;

	@JsonProperty
	Instant lastStructureGet;

	@JsonProperty("is_public_structure")
	boolean publicStructure;

	@JsonProperty
	Instant lastSeenPublicStructure;

	@JsonProperty("is_market_structure")
	boolean marketStructure;

	@JsonProperty
	Instant lastSeenMarketStructure;
}
