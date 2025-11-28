package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@Schema
@JsonPropertyOrder({"r", "g", "b"})
public class Color {
	@JsonProperty("r")
	double r;

	@JsonProperty("g")
	double g;

	@JsonProperty("b")
	double b;
}
