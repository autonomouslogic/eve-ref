package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A market group")
public class MarketGroup {
	@JsonProperty
	Long marketGroupId;

	@JsonProperty
	Long parentGroupId;

	@JsonProperty
	Map<String, String> name;

	@JsonProperty
	Map<String, String> description;

	@JsonProperty
	Long iconId;

	@JsonProperty
	Boolean hasTypes;
}
