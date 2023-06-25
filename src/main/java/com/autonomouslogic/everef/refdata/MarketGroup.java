package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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

	@JsonProperty
	@Schema(description = "The market group IDs which are direct children of this group. This is added by EVE Ref.")
	List<Long> childMarketGroupIds;

	@JsonProperty
	@Schema(description = "The type IDs in this market group. This is added by EVE Ref.")
	List<Long> typeIds;
}
