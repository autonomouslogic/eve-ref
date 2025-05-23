package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustryConstants.JOB_COST_BASE_RATE;
import static com.autonomouslogic.everef.industry.IndustrySkills.ENCRYPTION_SKILLS;
import static com.autonomouslogic.everef.industry.IndustrySkills.GLOBAL_TIME_BONUSES;
import static com.autonomouslogic.everef.industry.IndustrySkills.SPECIAL_TIME_BONUSES;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryDecryptor;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.InventionCost;
import com.autonomouslogic.everef.model.api.ManufacturingCost;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.util.MathUtil;
import com.autonomouslogic.everef.util.StreamUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;

import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class IndustryCostCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

	@Inject
	protected Provider<ManufactureCalculator> manufactureCalculatorProvider;

	@Setter
	@NonNull
	private LoadedRefData refData;

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
	protected IndustryCostCalculator() {}

	public IndustryCost calc() {
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");

		var builder = IndustryCost.builder();
		if (Optional.ofNullable(productType.getBlueprint()).orElse(false)) {
			var invention = blueprint.getActivities().get("invention");
			var inventionCost = inventionCost(invention);
			builder.invention(String.valueOf(productType.getTypeId()), inventionCost);
		} else {
			var manufacturing = blueprint.getActivities().get("manufacturing");
			var manufacturingCost = manufacturingCost(manufacturing);
			builder.manufacturing(String.valueOf(productType.getTypeId()), manufacturingCost);
		}
		return builder.build();
	}

	private ManufacturingCost manufacturingCost(BlueprintActivity manufacturing) {
		var time = manufacturingTime(manufacturing);
		var eiv = manufacturingEiv(manufacturing);
		var runs = industryCostInput.getRuns();
		var unitsPerRun =
				manufacturing.getProducts().get(productType.getTypeId()).getQuantity();
		var units = runs * unitsPerRun;
		var systemCostIndex = manufacturingSystemCostIndex(eiv);
		var systemCostBonuses = systemCostBonuses(systemCostIndex, "manufacturing");
		var facilityTax = facilityTax(eiv);
		var sccSurcharge = sccSurcharge(eiv);
		var alphaCloneTax = alphaCloneTax(eiv);
		var totalJobCost = systemCostIndex
				.add(facilityTax)
				.add(sccSurcharge)
				.add(alphaCloneTax)
				.add(systemCostBonuses);
		var materials = manufacturingMaterials(manufacturing);
		var totalMaterialCost = totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return ManufacturingCost.builder()
				.productId(industryCostInput.getProductId())
				.blueprintId(blueprint.getBlueprintTypeId())
				.runs(runs)
				.units(units)
				.unitsPerRun(unitsPerRun)
				.materials(materials)
				.time(time)
				.timePerRun(time.dividedBy(runs).truncatedTo(ChronoUnit.MILLIS))
				.timePerUnit(time.dividedBy(units).truncatedTo(ChronoUnit.MILLIS))
				.estimatedItemValue(eiv)
				.systemCostIndex(systemCostIndex)
				.systemCostBonuses(systemCostBonuses)
				.facilityTax(facilityTax)
				.sccSurcharge(sccSurcharge)
				.alphaCloneTax(alphaCloneTax)
				.totalJobCost(totalJobCost)
				.totalMaterialCost(totalMaterialCost)
				.totalCost(totalCost)
				.totalCostPerRun(MathUtil.round(MathUtil.divide(totalCost, runs), 2))
				.totalCostPerUnit(MathUtil.round(MathUtil.divide(totalCost, units), 2))
				.build();
	}

	private Map<String, MaterialCost> manufacturingMaterials(BlueprintActivity manufacturing) {
		return materials(manufacturing, this::manufacturingMaterialQuantity);
	}

	private Map<String, MaterialCost> inventionMaterials(BlueprintActivity invention) {
		var materials = materials(invention, this::inventionMaterialQuantity);
		if (decryptor != null) {
			materials.put(
					String.valueOf(decryptor.getTypeId()),
					MaterialCost.builder()
							.typeId(decryptor.getTypeId())
							.quantity(industryCostInput.getRuns())
							.cost(materialCost(decryptor.getTypeId(), industryCostInput.getRuns()))
							.build());
		}
		return materials;
	}

	private Map<String, MaterialCost> materials(BlueprintActivity activity, Function<Long, Long> quantityMod) {
		var materials = new LinkedHashMap<String, MaterialCost>();
		for (var material : activity.getMaterials().values()) {
			long typeId = material.getTypeId();
			var quantity = quantityMod.apply(material.getQuantity());
			var cost = materialCost(typeId, quantity);
			materials.put(
					String.valueOf(typeId),
					MaterialCost.builder()
							.typeId(typeId)
							.quantity(quantity)
							.cost(cost)
							.build());
		}
		return materials;
	}

	private BigDecimal materialCost(long typeId, long quantity) {
		var price = marketPriceService.getEsiAveragePrice(typeId).orElse(0);
		var cost = MathUtil.round(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity)), 2);
		return cost;
	}

	private long manufacturingMaterialQuantity(long base) {
		var runs = industryCostInput.getRuns();
		var meMod = materialEfficiencyModifier();
		var structureMod = structureManufacturingMaterialModifier();
		var rigMod = rigModifier(IndustryRig::getMaterialBonus, "manufacturing");
		var quantity = runs * base * meMod * structureMod * rigMod;
		var rounded = Math.max(runs, Math.ceil(Math.round(quantity * 100.0) / 100.0));
		return (long) rounded;
	}

	private double materialEfficiencyModifier() {
		return 1.0 - industryCostInput.getMe() / 100.0;
	}

	private double timeEfficiencyModifier() {
		return 1.0 - industryCostInput.getTe() / 100.0;
	}

	private double structureManufacturingMaterialModifier() {
		if (structure == null) {
			return 1.0;
		}
		return structure.getMaterialModifier();
	}

	private double structureTimeModifier() {
		if (structure == null) {
			return 1.0;
		}
		return structure.getTimeModifier();
	}

	private double structureCostModifier() {
		if (structure == null) {
			return 1.0;
		}
		return structure.getCostModifier();
	}

	private double rigModifier(Function<IndustryRig, Double> bonusGetter, String activity) {
		var bonus = 0.0;
		if (rigs != null) {
			for (var rig : rigs) {
				bonus += rigBonus(rig, bonusGetter, activity);
			}
		}
		return 1.0 + bonus;
	}

	private double rigBonus(IndustryRig rig, Function<IndustryRig, Double> bonusGetter, String activity) {
		var globalActivities = rig.getGlobalActivities();
		if (globalActivities != null && globalActivities.contains(activity)) {
			return bonusGetter.apply(rig) * getRigSecurityModifier(rig);
		}

		var categories = rig.getManufacturingCategories();
		var groups = rig.getManufacturingGroups();
		var category = productType.getCategoryId();
		var group = productType.getGroupId();
		if ((categories != null && categories.contains(category)) || (groups != null && groups.contains(group))) {
			return bonusGetter.apply(rig) * getRigSecurityModifier(rig);
		}

		return 0.0;
	}

	private double getRigSecurityModifier(IndustryRig rig) {
		if (rig == null) {
			return 1.0;
		}
		var sec = industryCostInput.getSecurity();
		if (sec == null) {
			return 1.0;
		}
		return switch (sec) {
			case HIGH_SEC -> rig.getHighSecMod();
			case LOW_SEC -> rig.getLowSecMod();
			case NULL_SEC, WORMHOLE -> rig.getNullSecMod();
			default -> throw new RuntimeException("Unknown security: " + sec);
		};
	}

	private long inventionMaterialQuantity(long base) {
		return base * industryCostInput.getRuns();
	}

	private BigDecimal totalMaterialCost(Map<String, MaterialCost> materials) {
		var total = BigDecimal.ZERO;
		for (var materialCost : materials.values()) {
			total = total.add(materialCost.getCost());
		}
		return total;
	}

	private Duration manufacturingTime(BlueprintActivity manufacturing) {
		var baseTime = (double) manufacturing.getTime();
		var teMod = timeEfficiencyModifier();
		var runs = industryCostInput.getRuns();
		var industryMod = 1.0 - GLOBAL_TIME_BONUSES.get("Industry") * industryCostInput.getIndustry();
		var advancedIndustryMod =
				1.0 - GLOBAL_TIME_BONUSES.get("Advanced Industry") * industryCostInput.getAdvancedIndustry();
		var specialSkillMod = manufacturingSpecialisedSkillMod(manufacturing);
		var structureMod = structureTimeModifier();
		var rigMod = rigModifier(IndustryRig::getTimeBonus, "manufacturing");
		var time =
				runs * baseTime * teMod * industryMod * advancedIndustryMod * specialSkillMod * structureMod * rigMod;
		var rounded = Math.round(time);
		return Duration.ofSeconds(rounded);
	}

	private double manufacturingSpecialisedSkillMod(BlueprintActivity manufacturing) {
		return sumSkillMod(
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Small Ship Construction"),
						industryCostInput.getAdvancedSmallShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Industrial Ship Construction"),
						industryCostInput.getAdvancedIndustrialShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Medium Ship Construction"),
						industryCostInput.getAdvancedMediumShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Large Ship Construction"),
						industryCostInput.getAdvancedLargeShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("High Energy Physics"),
						industryCostInput.getHighEnergyPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Plasma Physics"),
						industryCostInput.getPlasmaPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Nanite Engineering"),
						industryCostInput.getNaniteEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Hydromagnetic Physics"),
						industryCostInput.getHydromagneticPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Amarr Starship Engineering"),
						industryCostInput.getAmarrStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Minmatar Starship Engineering"),
						industryCostInput.getMinmatarStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Graviton Physics"),
						industryCostInput.getGravitonPhysics()),
				specialSkillBonus(
						manufacturing, SPECIAL_TIME_BONUSES.get("Laser Physics"), industryCostInput.getLaserPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Electromagnetic Physics"),
						industryCostInput.getElectromagneticPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Rocket Science"),
						industryCostInput.getRocketScience()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Gallente Starship Engineering"),
						industryCostInput.getGallenteStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Nuclear Physics"),
						industryCostInput.getNuclearPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Mechanical Engineering"),
						industryCostInput.getMechanicalEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Electronic Engineering"),
						industryCostInput.getElectronicEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Caldari Starship Engineering"),
						industryCostInput.getCaldariStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Quantum Physics"),
						industryCostInput.getQuantumPhysics()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Molecular Engineering"),
						industryCostInput.getMolecularEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Triglavian Quantum Engineering"),
						industryCostInput.getTriglavianQuantumEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Advanced Capital Ship Construction"),
						industryCostInput.getAdvancedCapitalShipConstruction()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Upwell Starship Engineering"),
						industryCostInput.getUpwellStarshipEngineering()),
				specialSkillBonus(
						manufacturing,
						SPECIAL_TIME_BONUSES.get("Mutagenic Stabilization"),
						industryCostInput.getMutagenicStabilization()));
	}

	private Duration inventionTime(BlueprintActivity invention) {
		var baseTime = (double) invention.getTime();
		var runs = industryCostInput.getRuns();
		var advancedIndustryMod =
				1.0 - GLOBAL_TIME_BONUSES.get("Advanced Industry") * industryCostInput.getAdvancedIndustry();
		var structureMod = structureTimeModifier();
		var rigMod = rigModifier(IndustryRig::getTimeBonus, "invention");
		var time = runs * baseTime * advancedIndustryMod * structureMod * rigMod;
		var rounded = (long) Math.round(time);
		return Duration.ofSeconds(rounded);
	}

	private double specialSkillBonus(BlueprintActivity manufacturing, IndustrySkills.SkillBonus bonus, int level) {
		if (manufacturing.getRequiredSkills().containsKey(bonus.getSkillId())) {
			return bonus.getBonus() * level;
		}
		return 0.0;
	}

	private double sumSkillMod(double... bonuses) {
		var mod = 1.0;
		for (double bonus : bonuses) {
			mod *= 1.0 - bonus;
		}
		return mod;
	}

	private BigDecimal manufacturingEiv(BlueprintActivity activityCost) {
		var eiv = BigDecimal.ZERO;
		for (var material : activityCost.getMaterials().values()) {
			var adjPrice = marketPriceService.getEsiAdjustedPrice(material.getTypeId());
			if (adjPrice.isEmpty()) {
				throw new RuntimeException("typeId: " + material.getTypeId());
			}
			var quantity = BigDecimal.valueOf(material.getQuantity());
			var price = BigDecimal.valueOf(adjPrice.getAsDouble());
			var total = quantity.multiply(price);
			eiv = eiv.add(total);
		}
		return MathUtil.round(eiv.multiply(BigDecimal.valueOf(industryCostInput.getRuns())));
	}

	private BigDecimal jobCostBase(BigDecimal eiv) {
		return MathUtil.round(eiv.multiply(JOB_COST_BASE_RATE));
	}

	private BigDecimal manufacturingSystemCostIndex(BigDecimal eiv) {
		var index = industryCostInput.getManufacturingCost();
		return systemCostIndex(eiv, index);
	}

	private BigDecimal systemCostBonuses(BigDecimal systemCostIndex, String activity) {
		var structureMod = structureCostModifier();
		var rigMod = rigModifier(IndustryRig::getCostBonus, activity);
		var costMod = 1.0 + industryCostInput.getSystemCostBonus().doubleValue();
		var mod = structureMod * rigMod * costMod;
		var modified = systemCostIndex.multiply(BigDecimal.valueOf(mod), MathUtil.MATH_CONTEXT);
		var bonus = modified.subtract(systemCostIndex);
		return MathUtil.round(bonus);
	}

	private BigDecimal inventionSystemCostIndex(BigDecimal jcb) {
		var index = industryCostInput.getInventionCost();
		return systemCostIndex(jcb, index);
	}

	private static @NotNull BigDecimal systemCostIndex(BigDecimal value, BigDecimal index) {
		var cost = MathUtil.round(value.multiply(index));
		return cost;
	}

	private BigDecimal sccSurcharge(BigDecimal val) {
		return MathUtil.round(IndustryConstants.SCC_SURCHARGE_RATE.multiply(val));
	}

	private BigDecimal alphaCloneTax(BigDecimal val) {
		if (industryCostInput.getAlpha()) {
			return MathUtil.round(IndustryConstants.ALPHA_CLONE_TAX.multiply(val));
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal facilityTax(BigDecimal val) {
		return MathUtil.round(industryCostInput.getFacilityTax().multiply(val));
	}

	private InventionCost inventionCost(BlueprintActivity invention) {
		var decryptorOpt = Optional.ofNullable(decryptor);
		var manufacturing = inventionBlueprintProductActivity(productType);
		var time = inventionTime(invention);
		var eiv = manufacturingEiv(manufacturing);
		var jcb = jobCostBase(eiv);
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
		var systemCostIndex = inventionSystemCostIndex(jcb);
		var systemCostBonuses = systemCostBonuses(systemCostIndex, "invention");
		var facilityTax = facilityTax(jcb);
		var sccSurcharge = sccSurcharge(jcb);
		var alphaCloneTax = alphaCloneTax(jcb);
		var totalJobCost = systemCostIndex
				.add(facilityTax)
				.add(sccSurcharge)
				.add(alphaCloneTax)
				.add(systemCostBonuses);
		var materials = inventionMaterials(invention);
		var totalMaterialCost = totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return InventionCost.builder()
				.productId(industryCostInput.getProductId())
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
