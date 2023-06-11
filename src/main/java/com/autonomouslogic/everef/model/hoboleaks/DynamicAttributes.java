package com.autonomouslogic.everef.model.hoboleaks;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class DynamicAttributes {
	@JsonProperty
	List<DynamicAttributeInputOutputMapping> inputOutputMapping;

	@JsonProperty(value = "attributeIDs")
	Map<Long, DynamicAttributeMinMax> attributeIds;
}
