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
@Schema(description = "A meta group")
public class MetaGroup {
	@JsonProperty
	Long metaGroupId;

	@JsonProperty
	Long iconId;

	@JsonProperty
	String iconSuffix;

	@JsonProperty
	Map<String, String> name;

	@JsonProperty
	Map<String, String> description;

	@JsonProperty
	@Schema(description = "The type IDs in this meta group. This is added by EVE Ref.")
	List<Long> typeIds;

	@JsonProperty
	List<Double> color;
}
