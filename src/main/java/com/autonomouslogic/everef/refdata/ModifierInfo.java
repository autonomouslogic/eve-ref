package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
public class ModifierInfo {
	@JsonProperty
	String domain;

	@JsonProperty
	String func;

	@JsonProperty
	Integer groupId;

	@JsonProperty
	Integer modifiedAttributeId;

	@JsonProperty
	Integer modifyingAttributeId;

	@JsonProperty
	Integer skillTypeId;

	@JsonProperty
	Integer effectId;

	@JsonProperty
	Integer operator;
}
