package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Set;
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
public class IndustryCostInput {
	@JsonProperty
	Set<Long> productTypeIds;

	@JsonProperty
	int runs;

	@JsonProperty
	int materialEfficiency;

	@JsonProperty
	int timeEfficiency;

	@JsonProperty
	Long decryptorTypeId;

	@JsonProperty
	SecurityClass securityClass;

	//	@JsonProperty
	//	Long systemId;

	@JsonProperty
	BigDecimal manufacturingCostIndex;

	@JsonProperty
	BigDecimal researchingTimeEfficiencyCostIndex;

	@JsonProperty
	BigDecimal researchingMaterialEfficiencyCostIndex;

	@JsonProperty
	BigDecimal copyingCostIndex;

	@JsonProperty
	BigDecimal inventionCostIndex;

	@JsonProperty
	BigDecimal reactionCostIndex;

	@JsonProperty
	BigDecimal facilityTaxRate;

	@JsonProperty
	BigDecimal sccSurchargeRate;

	@JsonProperty
	Long structureTypeId;

	//	@JsonProperty
	//	Set<Long> rigTypeIds;

	@JsonProperty
	TechLevel materialEfficiencyRigTechLevel;

	@JsonProperty
	TechLevel timeEfficiencyRigTechLevel;

	@JsonProperty
	TechLevel inventionRigTechLevel;

	@JsonProperty
	TechLevel copyingRigTechLevel;

	@JsonProperty
	@lombok.Builder.Default
	int industryLevel = 5;

	@JsonProperty
	@lombok.Builder.Default
	int researchLevel = 5;

	@JsonProperty
	@lombok.Builder.Default
	int scienceLevel = 5;

	@JsonProperty
	@lombok.Builder.Default
	int advancedIndustryLevel = 5;

	@JsonProperty
	@lombok.Builder.Default
	int metallurgyLevel = 5;

	@JsonProperty
	@lombok.Builder.Default
	int datacore1Level = 5;

	@JsonProperty
	@lombok.Builder.Default
	int datacore2Level = 5;

	@JsonProperty
	@lombok.Builder.Default
	int decryptionLevel = 5;
}
