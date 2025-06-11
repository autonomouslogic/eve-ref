package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.api.ClientException;
import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.InventionCost;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.BlueprintMaterial;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.util.MathUtil;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class InventionCalculator {
	@Inject
	protected IndustryMath industryMath;

	@Inject
	protected IndustryStructures industryStructures;

	@Inject
	protected IndustryRigs industryRigs;

	@Inject
	protected IndustryDecryptors industryDecryptors;

	@Inject
	protected IndustrySkills industrySkills;

	@Inject
	protected MarketPriceService marketPriceService;

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
	private int runs;

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
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");
		if (runs <= 0) {
			throw new IllegalArgumentException("Runs must be positive");
		}

		var invention = blueprint.getActivities().get("invention");

		var decryptorOpt = Optional.ofNullable(decryptor);
		var me =
				decryptorOpt.map(d -> industryDecryptors.getBlueprintMe(d)).orElse(IndustryConstants.INVENTION_BASE_ME);
		var te =
				decryptorOpt.map(d -> industryDecryptors.getBlueprintTe(d)).orElse(IndustryConstants.INVENTION_BASE_TE);
		var manufacturing = inventionBlueprintProductActivity(productType);
		var time = inventionTime(invention);
		var eiv = industryMath.eiv(manufacturing, runs);
		var jcb = industryMath.jobCostBase(eiv);
		var prob = inventionProbability(invention);
		var runsPerCopy = invention
						.getProducts()
						.get(productType.getTypeId())
						.getQuantity()
						.intValue()
				+ decryptorOpt.map(IndustryDecryptor::getRunModifier).orElse(0);
		var unitsPerRun = manufacturing.getProducts().values().stream()
				.findFirst()
				.orElseThrow(() -> new ClientException(
						String.format("Blueprint %d has no product", blueprint.getBlueprintTypeId())))
				.getQuantity()
				.intValue();
		var productVolume = industryMath.typeVolume(productType, runs);
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
		var materialsVolume = industryMath.materialVolume(materials);
		var totalMaterialCost = industryMath.totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return InventionCost.builder()
				.productId(productType.getTypeId())
				.blueprintId(blueprint.getBlueprintTypeId())
				.runsPerCopy(runsPerCopy)
				.unitsPerRun(unitsPerRun)
				.probability(prob)
				.me(me)
				.te(te)
				.runs(runs)
				.expectedCopies(expectedCopies)
				.expectedRuns(expectedRuns)
				.expectedUnits(expectedUnits)
				.materials(materials)
				.materialsVolume(materialsVolume)
				.productVolume(productVolume)
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
		return base * runs;
	}

	public Map<String, MaterialCost> inventionMaterials(BlueprintActivity invention) {
		if (decryptor != null) {
			invention = invention.toBuilder()
					.material(
							decryptor.getTypeId(),
							BlueprintMaterial.builder()
									.typeId(decryptor.getTypeId())
									.quantity(1L)
									.build())
					.build();
		}
		var materials = industryMath.materials(
				invention, this::inventionMaterialQuantity, industryCostInput.getMaterialPrices());
		var materialsWithCost = marketPriceService.materialCosts(materials, industryCostInput.getMaterialPrices());
		return materialsWithCost;
	}

	private Duration inventionTime(BlueprintActivity invention) {
		var baseTime = (double) invention.getTime();
		var advancedIndustryMod = industrySkills.advancedIndustrySkillIndustryJobTimeBonusMod(industryCostInput);
		var structureMod = industryStructures.structureTimeModifier(structure);
		var rigMod = industryRigs.rigModifier(
				rigs, productType, industryCostInput.getSecurity(), IndustryRig::getTimeBonus, "invention");
		var time = runs * baseTime * advancedIndustryMod * structureMod * rigMod;
		var rounded = (long) Math.round(time);
		return Duration.ofSeconds(rounded);
	}

	private BlueprintActivity inventionBlueprintProductActivity(InventoryType product) {
		if (!Optional.ofNullable(product.getBlueprint()).orElse(false)) {
			throw new IllegalArgumentException(product.getName().get("en") + " is not a blueprint");
		}
		var blueprint = Objects.requireNonNull(refData.getBlueprint(product.getTypeId()));
		return Optional.ofNullable(blueprint.getActivities().get("manufacturing"))
				.orElseThrow();
	}

	private double inventionProbability(BlueprintActivity invention) {
		var baseProb = invention.getProducts().get(productType.getTypeId()).getProbability();
		var datacoreSkills = inventionDatacoreSkills(invention);
		var encryptionSkill = inventionEncryptionSkill(invention);
		var decryptorMod = Optional.ofNullable(decryptor)
				.map(IndustryDecryptor::getProbabilityModifier)
				.orElse(1.0);
		return baseProb
				* (1 + ((datacoreSkills.get(0) + datacoreSkills.get(1)) / 30.0) + encryptionSkill / 40.0)
				* decryptorMod;
	}

	private List<Integer> inventionDatacoreSkills(BlueprintActivity invention) {
		var skills = industrySkills.datacoreSkills(industryCostInput, invention).stream()
				.map(p -> p.getRight())
				.toList();
		if (skills.size() != 2) {
			throw new IllegalArgumentException("Invalid skill count for invention: " + skills.size());
		}
		return skills;
	}

	private int inventionEncryptionSkill(BlueprintActivity invention) {
		var skills = industrySkills.encryptionSkills(industryCostInput, invention);
		if (skills.isEmpty()) {
			return 0;
		}
		if (skills.size() != 1) {
			throw new IllegalArgumentException("Invalid skill count for encryption: " + skills.size());
		}
		return skills.getFirst().getRight();
	}
}
