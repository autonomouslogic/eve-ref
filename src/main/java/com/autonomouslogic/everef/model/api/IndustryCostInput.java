package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema
public class IndustryCostInput {
	@JsonProperty
	Set<Long> productTypeIds;

	@JsonProperty
	int runs;

	@JsonProperty
	Long decryptorTypeId;

	@JsonProperty
	SecurityClass systemSecurityClass;

	@JsonProperty
	Long systemId;

	@JsonProperty
	BigDecimal systemCostIndex;

	@JsonProperty
	Long structureTypeId;

	@JsonProperty
	Set<Long> rigTypeIds;

	@JsonProperty
	TechLevel meRigTechLevel;

	@JsonProperty
	TechLevel teRigTechLevel;

	@JsonProperty
	TechLevel inventionRigTechLevel;

	@JsonProperty
	TechLevel copyingRigTechLevel;
}
