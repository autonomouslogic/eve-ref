package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
public class NpcStation {
	@JsonProperty
	Long stationId;

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
	Long operationId;

	@JsonProperty
	Long ownerId;

	@JsonProperty
	Coordinate position;

	@JsonProperty
	BigDecimal reprocessingEfficiency;

	@JsonProperty
	Long reprocessingHangarFlag;

	@JsonProperty
	BigDecimal reprocessingStationsTake;

	@JsonProperty
	Boolean useOperationName;
}
