package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
	//	@JsonProperty
	//	Set<Long> productId;

	@JsonProperty
	@Schema(description = "The desired product type ID", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
	long productId;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The number of runs",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			minimum = "1",
			defaultValue = "1")
	int runs = 1;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The material efficiency of the blueprint",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			minimum = "0",
			maximum = "10",
			defaultValue = "10")
	int me = 10;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The time efficiency of the blueprint",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			minimum = "0",
			maximum = "20",
			defaultValue = "20")
	int te = 20;

	//	@JsonProperty
	//	@Schema(description = "The decryptor type ID used for invention", requiredMode =
	// Schema.RequiredMode.NOT_REQUIRED)
	//	Long decryptorId;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The security class of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "HIGH_SEC")
	SecurityClass security = SecurityClass.HIGH_SEC;

	//	@JsonProperty
	//	Long systemId;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The manufacturing cost index of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal manufacturingCost = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The researching time efficiency cost index of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal researchingTeCost = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The researching material efficiency cost index of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal researchingMeCost = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The copying cost index of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal copyingCost = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The invention cost index of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal inventionCost = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The reaction cost index of the system where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal reactionCost = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The facility tax rate of the station or structure where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1")
	BigDecimal facilityTax = BigDecimal.ZERO;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The SCC surcharge rate",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0.04",
			minimum = "0",
			maximum = "1")
	BigDecimal scc = new BigDecimal("0.04");

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The type of station or structure where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "STATION")
	StructureClass structureClass = StructureClass.STATION;

	@JsonProperty
	@Schema(
			description = "The size of structure where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	StructureSize structureSize;

	//	@JsonProperty
	//	Long structureTypeId;

	//	@JsonProperty
	//	Set<Long> rigTypeIds;

	//	@JsonProperty
	//	TechLevel materialEfficiencyRigTechLevel;

	//	@JsonProperty
	//	TechLevel timeEfficiencyRigTechLevel;

	//	@JsonProperty
	//	TechLevel inventionRigTechLevel;

	//	@JsonProperty
	//	TechLevel copyingRigTechLevel;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Industry skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int industry = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Research skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int research = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Science skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int science = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Advanced Industry skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int advancedIndustry = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Metallurgy skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int metallurgy = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The level of one of the datacore related skills the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int datacore1 = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The level of the other of the datacore related skills the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int datacore2 = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The level of relevant encryption skills the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int decryption = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "Whether installing character is an alpha clone or not",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "false")
	Boolean alpha = false;
}
