package com.autonomouslogic.everef.model.hoboleaks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class DynamicAttributeMinMax {
	@JsonProperty
	double min;

	@JsonProperty
	double max;
}
