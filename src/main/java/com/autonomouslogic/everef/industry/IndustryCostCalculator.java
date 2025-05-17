package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustryConstants.JOB_COST_BASE_RATE;
import static com.autonomouslogic.everef.industry.IndustrySkills.ENCRYPTION_SKILLS;
import static com.autonomouslogic.everef.industry.IndustrySkills.GLOBAL_TIME_BONUSES;
import static com.autonomouslogic.everef.industry.IndustrySkills.SPECIAL_TIME_BONUSES;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.ActivityCost;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.InventionCost;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.util.EveConstants;
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
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class IndustryCostCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

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

	private ActivityCost manufacturingCost(BlueprintActivity manufacturing) {
		var time = manufacturingTime(manufacturing);
		var eiv = manufacturingEiv(manufacturing);
		var quantity = industryCostInput.getRuns();
		var systemCostIndex = manufacturingSystemCostIndex(eiv);
		var facilityTax = facilityTax(eiv);
		var sccSurcharge = sccSurcharge(eiv);
		var alphaCloneTax = alphaCloneTax(eiv);
		var totalJobCost = systemCostIndex.add(facilityTax).add(sccSurcharge).add(alphaCloneTax);
		var materials = manufacturingMaterials(manufacturing);
		var totalMaterialCost = totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return ActivityCost.builder()
				.productId(industryCostInput.getProductId())
				.quantity(quantity)
				.materials(materials)
				.time(time)
				.timePerUnit(time.dividedBy(quantity).truncatedTo(ChronoUnit.MILLIS))
				.estimatedItemValue(eiv)
				.systemCostIndex(systemCostIndex)
				.facilityTax(facilityTax)
				.sccSurcharge(sccSurcharge)
				.alphaCloneTax(alphaCloneTax)
				.totalJobCost(totalJobCost)
				.totalMaterialCost(totalMaterialCost)
				.totalCost(totalCost)
				.totalCostPerUnit(MathUtil.round(MathUtil.divide(totalCost, quantity), 2))
				.build();
	}

	private Map<String, MaterialCost> manufacturingMaterials(BlueprintActivity manufacturing) {
		return materials(manufacturing, this::manufacturingMaterialQuantity);
	}

	private Map<String, MaterialCost> inventionMaterials(BlueprintActivity invention) {
		return materials(invention, Function.identity());
	}

	private Map<String, MaterialCost> materials(BlueprintActivity activity, Function<Long, Long> quantityMod) {
		var materials = new LinkedHashMap<String, MaterialCost>();
		for (var material : activity.getMaterials().values()) {
			long typeId = material.getTypeId();
			var price = marketPriceService.getEsiAveragePrice(typeId).orElse(0);
			var quantity = quantityMod.apply(material.getQuantity());
			var cost = MathUtil.round(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity)), 2);
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

	private long manufacturingMaterialQuantity(long base) {
		var runs = industryCostInput.getRuns();
		var meMod = 1.0 - industryCostInput.getMe() / 100.0;
		return (long) Math.max(runs, Math.ceil(Math.round(runs * base * meMod * 100.0) / 100.0));
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
		var teMod = 1.0 - industryCostInput.getTe() / 100.0;
		var runs = industryCostInput.getRuns();
		var industryMod = 1.0 - GLOBAL_TIME_BONUSES.get("Industry") * industryCostInput.getIndustry();
		var advancedIndustryMod =
				1.0 - GLOBAL_TIME_BONUSES.get("Advanced Industry") * industryCostInput.getAdvancedIndustry();
		var specialSkillMod = manufacturingSpecialisedSkillMod(manufacturing);
		return Duration.ofSeconds(
				(long) Math.round(runs * baseTime * teMod * industryMod * advancedIndustryMod * specialSkillMod));
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
		return Duration.ofSeconds((long) Math.round(runs * baseTime * advancedIndustryMod));
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
			eiv = eiv.add(
					BigDecimal.valueOf(material.getQuantity()).multiply(BigDecimal.valueOf(adjPrice.getAsDouble())));
		}
		return MathUtil.round(eiv);
	}

	private BigDecimal jobCostBase(BigDecimal eiv) {
		return MathUtil.round(eiv.multiply(JOB_COST_BASE_RATE));
	}

	private BigDecimal manufacturingSystemCostIndex(BigDecimal eiv) {
		var index = industryCostInput.getManufacturingCost();
		return systemCostIndex(eiv, index);
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
		var time = inventionTime(invention);
		var eiv = manufacturingEiv(inventionBlueprintProductActivity(productType));
		var jcb = jobCostBase(eiv);
		var prob = inventionProbability(invention);
		var runs = industryCostInput.getRuns() * inventionRuns(inventionBlueprintProductActivity(productType));
		var quantity = runs * prob;
		var systemCostIndex = inventionSystemCostIndex(jcb);
		var facilityTax = facilityTax(jcb);
		var sccSurcharge = sccSurcharge(jcb);
		var alphaCloneTax = alphaCloneTax(jcb);
		var totalJobCost = systemCostIndex.add(facilityTax).add(sccSurcharge).add(alphaCloneTax);
		var materials = inventionMaterials(invention);
		var totalMaterialCost = totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return InventionCost.builder()
				.productId(industryCostInput.getProductId())
				.probability(prob)
				.me(IndustryConstants.INVENTION_BASE_ME)
				.te(IndustryConstants.INVENTION_BASE_TE)
				.quantity(quantity)
				.runs(runs)
				.materials(materials)
				.time(time)
				.timePerUnit(MathUtil.divide(time, quantity).truncatedTo(ChronoUnit.MILLIS))
				.estimatedItemValue(eiv)
				.systemCostIndex(systemCostIndex)
				.jobCostBase(jcb)
				.facilityTax(facilityTax)
				.sccSurcharge(sccSurcharge)
				.alphaCloneTax(alphaCloneTax)
				.totalJobCost(totalJobCost)
				.totalMaterialCost(totalMaterialCost)
				.totalCost(totalCost)
				.totalCostPerUnit(MathUtil.round(MathUtil.divide(totalCost, quantity), 2))
				.build();
	}

	private int inventionRuns(BlueprintActivity blueprintActivity) {
		var products = blueprintActivity.getProducts();
		if (products.size() > 1) {
			throw new IllegalArgumentException();
		}
		var productId = products.values().stream().findFirst().orElseThrow().getTypeId();
		var product = refData.getType(productId);
		var categoryId = Optional.ofNullable(product.getCategoryId()).orElseThrow();
		if (categoryId == EveConstants.SHIP_CATEGORY_ID) {
			return IndustryConstants.INVENTION_BASE_SHIP_AND_RIG_RUNS;
		}
		var group = refData.getGroup(product.getGroupId());
		if (group.getName().get("en").startsWith("Rig ")) {
			return IndustryConstants.INVENTION_BASE_SHIP_AND_RIG_RUNS;
		}
		return IndustryConstants.INVENTION_BASE_RUNS;
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
		int encryptionSkill = inventionEncryptionSkill(invention);
		return baseProb * (1 + ((datacoreSkills.get(0) + datacoreSkills.get(1)) / 30.0) + encryptionSkill / 40.0);
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
