package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class IndustrySkills {
	@JsonProperty
	@lombok.Builder.Default
	int industry = 5;

	@JsonProperty
	@lombok.Builder.Default
	int research = 5;

	@JsonProperty
	@lombok.Builder.Default
	int science = 5;

	@JsonProperty
	@lombok.Builder.Default
	int advancedIndustry = 5;

	@JsonProperty
	@lombok.Builder.Default
	int metallurgy = 5;

	@JsonProperty
	@lombok.Builder.Default
	int datacore1 = 5;

	@JsonProperty
	@lombok.Builder.Default
	int datacore2 = 5;

	@JsonProperty
	@lombok.Builder.Default
	int decryption = 5;
}
