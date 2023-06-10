package com.autonomouslogic.everef.model.hoboleaks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class DynamicAttributeInputOutputMapping {
	@JsonProperty
	long resultingType;

	@JsonProperty
	List<Long> applicableTypes;
}
