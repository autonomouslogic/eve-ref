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
}
