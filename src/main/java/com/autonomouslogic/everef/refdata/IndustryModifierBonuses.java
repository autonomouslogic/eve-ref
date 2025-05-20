package com.autonomouslogic.everef.refdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndustryModifierBonuses {
	@JsonProperty
	@Schema(description = "List of affected categories or groups when accounting for materials.")
	List<Long> material;

	@JsonProperty
	@Schema(description = "List of affected categories or groups when accounting for job cost.")
	List<Long> cost;

	@JsonProperty
	@Schema(description = "List of affected categories or groups when accounting for time.")
	List<Long> time;
}
