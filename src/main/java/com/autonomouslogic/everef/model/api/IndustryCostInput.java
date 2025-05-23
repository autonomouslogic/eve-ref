package com.autonomouslogic.everef.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
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
	@Schema(description = "The decryptor type ID to use", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long decryptorId;

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
			description = "Bonus to apply to system cost, such as the faction warfare bonus",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "0",
			minimum = "0",
			maximum = "1",
			example = "-0.50")
	BigDecimal systemCostBonus = BigDecimal.ZERO;

	@JsonProperty
	@Schema(
			description =
					"The type ID of the structure where the job is installed. If not set, an NPC station is assumed.",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long structureTypeId;

	@JsonProperty
	@Schema(
			description = "The type IDs of the rigs installed on the sture structure where the job is installed",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	List<Long> rigId;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "Whether installing character is an alpha clone or not",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "false")
	Boolean alpha = false;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "Where to get material prices from",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "ESI_AVG_PRICE")
	PriceSource materialPrices = PriceSource.ESI_AVG_PRICE;

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
			description = "The Advanced Small Ship Construction  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int advancedSmallShipConstruction = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Advanced Industrial Ship Construction  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int advancedIndustrialShipConstruction = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Advanced Medium Ship Construction  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int advancedMediumShipConstruction = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Advanced Large Ship Construction  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int advancedLargeShipConstruction = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The High Energy Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int highEnergyPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Plasma Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int plasmaPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Nanite Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int naniteEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Hydromagnetic Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int hydromagneticPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Amarr Starship Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int amarrStarshipEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Minmatar Starship Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int minmatarStarshipEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Graviton Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int gravitonPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Laser Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int laserPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Electromagnetic Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int electromagneticPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Rocket Science  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int rocketScience = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Gallente Starship Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int gallenteStarshipEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Nuclear Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int nuclearPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Mechanical Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int mechanicalEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Electronic Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int electronicEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Caldari Starship Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int caldariStarshipEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Quantum Physics  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int quantumPhysics = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Molecular Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int molecularEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Triglavian Quantum Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int triglavianQuantumEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Advanced Capital Ship Construction  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int advancedCapitalShipConstruction = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Upwell Starship Engineering  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int upwellStarshipEngineering = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Mutagenic Stabilization  skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int mutagenicStabilization = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Amarr Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int amarrEncryptionMethods = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Caldari Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int caldariEncryptionMethods = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Gallente Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int gallenteEncryptionMethods = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Minmatar Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int minmatarEncryptionMethods = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Sleeper Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int sleeperEncryptionMethods = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Triglavian Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int triglavianEncryptionMethods = 5;

	@JsonProperty
	@lombok.Builder.Default
	@Schema(
			description = "The Upwell Encryption Methods skill level the installing character",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED,
			defaultValue = "5",
			minimum = "0",
			maximum = "5")
	int upwellEncryptionMethods = 5;
}
