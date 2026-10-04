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
public class PlanetStatistics {
	@JsonProperty
	BigDecimal density;

	@JsonProperty
	BigDecimal eccentricity;

	@JsonProperty
	BigDecimal escapeVelocity;

	@JsonProperty
	Boolean locked;

	@JsonProperty
	BigDecimal massDust;

	@JsonProperty
	BigDecimal massGas;

	@JsonProperty
	BigDecimal orbitPeriod;

	@JsonProperty
	BigDecimal orbitRadius;

	@JsonProperty
	BigDecimal pressure;

	@JsonProperty
	BigDecimal rotationRate;

	@JsonProperty
	String spectralClass;

	@JsonProperty
	BigDecimal surfaceGravity;

	@JsonProperty
	BigDecimal temperature;
}
