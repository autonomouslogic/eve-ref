package com.autonomouslogic.everef.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type_id", "name", "probability_modifier", "me_modifier", "te_modifier", "run_modifier"})
@Schema
public class IndustryDecryptor {
	@JsonProperty
	long typeId;

	@JsonProperty
	String name;

	@JsonProperty
	double probabilityModifier;

	@JsonProperty
	int meModifier;

	@JsonProperty
	int teModifier;

	@JsonProperty
	int runModifier;
}
