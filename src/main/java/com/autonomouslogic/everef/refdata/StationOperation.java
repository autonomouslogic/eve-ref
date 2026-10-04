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
public class StationOperation {
	@JsonProperty
	Long operationId;

	@JsonProperty
	Long activityId;

	@JsonProperty
	BigDecimal border;

	@JsonProperty
	BigDecimal corridor;

	@JsonProperty
	Map<String, String> description;

	@JsonProperty
	BigDecimal fringe;

	@JsonProperty
	BigDecimal hub;

	@JsonProperty
	BigDecimal manufacturingFactor;

	@JsonProperty
	Map<String, String> operationName;

	@JsonProperty
	BigDecimal ratio;

	@JsonProperty
	BigDecimal researchFactor;

	@JsonProperty
	List<Long> services;

	@JsonProperty
	@Schema(description = "The key is the race ID.")
	Map<String, Long> stationTypes;
}
