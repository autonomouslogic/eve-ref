package com.autonomouslogic.everef.model.hoboleaks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class DynamicAttributes {
	@JsonProperty
	DynamicAttributeInputOutputMapping inputOutputMapping;

	@JsonProperty(value = "attributeIDs")
	Map<Long, DynamicAttributeMinMax> attributeIds;
}
