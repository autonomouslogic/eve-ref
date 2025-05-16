package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.SkillIndustryBonuses.GLOBAL_TIME_BONUSES;
import static com.autonomouslogic.everef.industry.SkillIndustryBonuses.SPECIAL_TIME_BONUSES;

import com.autonomouslogic.everef.model.api.ActivityCost;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class IndustryCostCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

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

		var manufacturing = blueprint.getActivities().get("manufacturing");
		//		var invention = findInventionActivity();
		var manufacturingCost = manufacturingCost(manufacturing);
		//		var inventionCost = inventionCost(invention);

		var builder = IndustryCost.builder()
				.manufacturing(String.valueOf(productType.getTypeId()), manufacturingCost)
				//				.invention(String.valueOf(productType.getTypeId()), inventionCost)
				.build();

		return builder;
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
				.totalCostPerUnit(totalCost.divide(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP))
				.build();
	}

	private Map<String, MaterialCost> manufacturingMaterials(BlueprintActivity manufacturing) {
		var materials = new LinkedHashMap<String, MaterialCost>();
		for (var material : manufacturing.getMaterials().values()) {
			long typeId = material.getTypeId();
			var price = marketPriceService.getEsiAveragePrice(typeId).orElse(0);
			var quantity = manufacturingMaterialQuantity(material.getQuantity());
			var cost = BigDecimal.valueOf(price)
					.multiply(BigDecimal.valueOf(quantity))
					.setScale(2, RoundingMode.HALF_UP);
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

	private double specialSkillBonus(
			BlueprintActivity manufacturing, SkillIndustryBonuses.SkillBonus bonus, int level) {
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
		return eiv.setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal manufacturingSystemCostIndex(BigDecimal eiv) {
		var index = industryCostInput.getManufacturingCost();
		var cost = eiv.multiply(index).setScale(0, RoundingMode.HALF_UP);
		return cost;
	}

	private BigDecimal sccSurcharge(BigDecimal eiv) {
		return IndustryConstants.SCC_SURCHARGE_RATE.multiply(eiv).setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal alphaCloneTax(BigDecimal eiv) {
		if (industryCostInput.getAlpha()) {
			return IndustryConstants.ALPHA_CLONE_TAX.multiply(eiv).setScale(0, RoundingMode.HALF_UP);
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal facilityTax(BigDecimal eiv) {
		return industryCostInput.getFacilityTax().multiply(eiv).setScale(0, RoundingMode.HALF_UP);
	}
}
