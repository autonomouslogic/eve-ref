package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustrySkills.ENCRYPTION_SKILLS;
import static com.autonomouslogic.everef.industry.IndustrySkills.GLOBAL_TIME_BONUSES;
import static com.autonomouslogic.everef.industry.IndustrySkills.SPECIAL_TIME_BONUSES;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.InventionCost;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.util.MathUtil;
import com.autonomouslogic.everef.util.StreamUtil;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class InventionCalculator {
	@Inject
	protected IndustryMath industryMath;

	@Inject
	protected SkillMath skillMath;

	@Inject
	protected IndustryStructures industryStructures;

	@Inject
	protected IndustryRigs industryRigs;

	private final LoadedRefData refData;

	@Setter
	@NonNull
	private IndustryCostInput industryCostInput;

	@Setter
	@NonNull
	private InventoryType productType;

	@Setter
	@NonNull
	private Blueprint blueprint;

	@Setter
	private IndustryDecryptor decryptor;

	@Setter
	private IndustryStructure structure;

	@Setter
	private List<IndustryRig> rigs;

	@Inject
	protected InventionCalculator(RefDataService refDataService) {
		refData = refDataService.getLoadedRefData();
	}

	public InventionCost calc() {
		var invention = blueprint.getActivities().get("invention");

		var decryptorOpt = Optional.ofNullable(decryptor);
		var manufacturing = inventionBlueprintProductActivity(productType);
		var time = inventionTime(invention);
		var eiv = industryMath.eiv(manufacturing, industryCostInput.getRuns());
		var jcb = industryMath.jobCostBase(eiv);
		var runs = industryCostInput.getRuns();
		var prob = inventionProbability(invention);
		var runsPerCopy = invention
						.getProducts()
						.get(productType.getTypeId())
						.getQuantity()
						.intValue()
				+ decryptorOpt.map(IndustryDecryptor::getRunModifier).orElse(0);
		var unitsPerRun = manufacturing.getProducts().values().stream()
				.findFirst()
				.orElseThrow()
				.getQuantity()
				.intValue();
		var expectedCopies = runs * prob;
		var expectedRuns = expectedCopies * runsPerCopy;
		var expectedUnits = expectedRuns * unitsPerRun;
		var systemCostIndex = industryMath.inventionSystemCostIndex(industryCostInput, jcb);
		var systemCostBonuses = industryMath.systemCostBonuses(
				structure,
				productType,
				rigs,
				industryCostInput.getSecurity(),
				industryCostInput.getSystemCostBonus(),
				systemCostIndex,
				"invention");
		var facilityTax = industryMath.facilityTax(industryCostInput, jcb);
		var sccSurcharge = industryMath.sccSurcharge(jcb);
		var alphaCloneTax = industryMath.alphaCloneTax(industryCostInput, jcb);
		var totalJobCost = systemCostIndex
				.add(facilityTax)
				.add(sccSurcharge)
				.add(alphaCloneTax)
				.add(systemCostBonuses);
		var materials = inventionMaterials(invention);
		var totalMaterialCost = industryMath.totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return InventionCost.builder()
				.productId(productType.getTypeId())
				.blueprintId(blueprint.getBlueprintTypeId())
				.runsPerCopy(runsPerCopy)
				.unitsPerRun(unitsPerRun)
				.probability(prob)
				.me(IndustryConstants.INVENTION_BASE_ME
						+ decryptorOpt.map(IndustryDecryptor::getMeModifier).orElse(0))
				.te(IndustryConstants.INVENTION_BASE_TE
						+ decryptorOpt.map(IndustryDecryptor::getTeModifier).orElse(0))
				.runs(runs)
				.expectedCopies(expectedCopies)
				.expectedRuns(expectedRuns)
				.expectedUnits(expectedUnits)
				.materials(materials)
				.time(time)
				.avgTimePerCopy(MathUtil.divide(time, expectedCopies).truncatedTo(ChronoUnit.MILLIS))
				.avgTimePerRun(MathUtil.divide(time, expectedRuns).truncatedTo(ChronoUnit.MILLIS))
				.avgTimePerUnit(MathUtil.divide(time, expectedUnits).truncatedTo(ChronoUnit.MILLIS))
				.estimatedItemValue(eiv)
				.systemCostIndex(systemCostIndex)
				.systemCostBonuses(systemCostBonuses)
				.jobCostBase(jcb)
				.facilityTax(facilityTax)
				.sccSurcharge(sccSurcharge)
				.alphaCloneTax(alphaCloneTax)
				.totalJobCost(totalJobCost)
				.totalMaterialCost(totalMaterialCost)
				.totalCost(totalCost)
				.avgCostPerCopy(MathUtil.round(MathUtil.divide(totalCost, expectedCopies), 2))
				.avgCostPerRun(MathUtil.round(MathUtil.divide(totalCost, expectedRuns), 2))
				.avgCostPerUnit(MathUtil.round(MathUtil.divide(totalCost, expectedUnits), 2))
				.build();
	}

	private long inventionMaterialQuantity(long base) {
		return base * industryCostInput.getRuns();
	}

	public Map<String, MaterialCost> inventionMaterials(BlueprintActivity invention) {
		var materials = industryMath.materials(invention, this::inventionMaterialQuantity);
		if (decryptor != null) {
			materials.put(
					String.valueOf(decryptor.getTypeId()),
					MaterialCost.builder()
							.typeId(decryptor.getTypeId())
							.quantity(industryCostInput.getRuns())
							.cost(industryMath.materialCost(decryptor.getTypeId(), industryCostInput.getRuns()))
							.build());
		}
		return materials;
	}

	private Duration inventionTime(BlueprintActivity invention) {
		var baseTime = (double) invention.getTime();
		var runs = industryCostInput.getRuns();
		var advancedIndustryMod =
				1.0 - GLOBAL_TIME_BONUSES.get("Advanced Industry") * industryCostInput.getAdvancedIndustry();
		var structureMod = industryStructures.structureTimeModifier(structure);
		var rigMod = industryRigs.rigModifier(
				rigs, productType, industryCostInput.getSecurity(), IndustryRig::getTimeBonus, "invention");
		var time = runs * baseTime * advancedIndustryMod * structureMod * rigMod;
		var rounded = (long) Math.round(time);
		return Duration.ofSeconds(rounded);
	}

	private BlueprintActivity inventionBlueprintProductActivity(InventoryType product) {
		if (!Optional.ofNullable(product.getBlueprint()).orElse(false)) {
			throw new IllegalArgumentException(product.getName().get("en") + "is not a blueprint");
		}
		var blueprint = Objects.requireNonNull(refData.getBlueprint(product.getTypeId()));
		return Optional.ofNullable(blueprint.getActivities().get("manufacturing"))
				.orElseThrow();
	}

	private double inventionProbability(BlueprintActivity invention) {
		var baseProb = invention.getProducts().get(productType.getTypeId()).getProbability();
		List<Integer> datacoreSkills = inventionDatacoreSkills(invention);
		var encryptionSkill = inventionEncryptionSkill(invention);
		var decryptorMod = Optional.ofNullable(decryptor)
				.map(IndustryDecryptor::getProbabilityModifier)
				.orElse(1.0);
		return baseProb
				* (1 + ((datacoreSkills.get(0) + datacoreSkills.get(1)) / 30.0) + encryptionSkill / 40.0)
				* decryptorMod;
	}

	private List<Integer> inventionDatacoreSkills(BlueprintActivity invention) {
		var skills = StreamUtil.concat(
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Advanced Small Ship Construction"),
								industryCostInput.getAdvancedSmallShipConstruction()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Advanced Industrial Ship Construction"),
								industryCostInput.getAdvancedIndustrialShipConstruction()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Advanced Medium Ship Construction"),
								industryCostInput.getAdvancedMediumShipConstruction()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Advanced Large Ship Construction"),
								industryCostInput.getAdvancedLargeShipConstruction()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("High Energy Physics"),
								industryCostInput.getHighEnergyPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Plasma Physics"),
								industryCostInput.getPlasmaPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Nanite Engineering"),
								industryCostInput.getNaniteEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Hydromagnetic Physics"),
								industryCostInput.getHydromagneticPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Amarr Starship Engineering"),
								industryCostInput.getAmarrStarshipEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Minmatar Starship Engineering"),
								industryCostInput.getMinmatarStarshipEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Graviton Physics"),
								industryCostInput.getGravitonPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Laser Physics"),
								industryCostInput.getLaserPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Electromagnetic Physics"),
								industryCostInput.getElectromagneticPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Rocket Science"),
								industryCostInput.getRocketScience()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Gallente Starship Engineering"),
								industryCostInput.getGallenteStarshipEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Nuclear Physics"),
								industryCostInput.getNuclearPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Mechanical Engineering"),
								industryCostInput.getMechanicalEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Electronic Engineering"),
								industryCostInput.getElectronicEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Caldari Starship Engineering"),
								industryCostInput.getCaldariStarshipEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Quantum Physics"),
								industryCostInput.getQuantumPhysics()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Molecular Engineering"),
								industryCostInput.getMolecularEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Triglavian Quantum Engineering"),
								industryCostInput.getTriglavianQuantumEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Advanced Capital Ship Construction"),
								industryCostInput.getAdvancedCapitalShipConstruction()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Upwell Starship Engineering"),
								industryCostInput.getUpwellStarshipEngineering()),
						datacoreSkill(
								invention,
								SPECIAL_TIME_BONUSES.get("Mutagenic Stabilization"),
								industryCostInput.getMutagenicStabilization()))
				.toList();
		if (skills.size() != 2) {
			throw new IllegalArgumentException("Invalid skill count for invention: " + skills.size());
		}
		return skills;
	}

	private Stream<Integer> datacoreSkill(BlueprintActivity invention, IndustrySkills.SkillBonus bonus, int level) {
		if (invention.getRequiredSkills().containsKey(bonus.getSkillId())) {
			return Stream.of(level);
		}
		return Stream.empty();
	}

	private int inventionEncryptionSkill(BlueprintActivity invention) {
		var skills = invention.getRequiredSkills().keySet();
		if (skills.contains(ENCRYPTION_SKILLS.get("Amarr Encryption Methods"))) {
			return industryCostInput.getAmarrEncryptionMethods();
		}
		if (skills.contains(ENCRYPTION_SKILLS.get("Caldari Encryption Methods"))) {
			return industryCostInput.getCaldariEncryptionMethods();
		}
		if (skills.contains(ENCRYPTION_SKILLS.get("Gallente Encryption Methods"))) {
			return industryCostInput.getGallenteEncryptionMethods();
		}
		if (skills.contains(ENCRYPTION_SKILLS.get("Minmatar Encryption Methods"))) {
			return industryCostInput.getMinmatarEncryptionMethods();
		}
		if (skills.contains(ENCRYPTION_SKILLS.get("Sleeper Encryption Methods"))) {
			return industryCostInput.getSleeperEncryptionMethods();
		}
		if (skills.contains(ENCRYPTION_SKILLS.get("Triglavian Encryption Methods"))) {
			return industryCostInput.getTriglavianEncryptionMethods();
		}
		if (skills.contains(ENCRYPTION_SKILLS.get("Upwell Encryption Methods"))) {
			return industryCostInput.getUpwellEncryptionMethods();
		}
		throw new IllegalArgumentException("No encryption skill found");
	}
}
