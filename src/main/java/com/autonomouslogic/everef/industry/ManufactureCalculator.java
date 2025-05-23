package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.ManufacturingCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.util.MathUtil;
import lombok.NonNull;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static com.autonomouslogic.everef.industry.IndustrySkills.GLOBAL_TIME_BONUSES;

public class ManufactureCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

	private final LoadedRefData refData;

	@Setter
	@NonNull
	private IndustryMath industryMath;

	@Setter
	@NonNull
	private IndustryCostInput industryCostInput;

	@Setter
	@NonNull
	private InventoryType productType;

	@Setter
	@NonNull
	private Blueprint blueprint;

	protected ManufactureCalculator(RefDataService refDataService) {
		refData = refDataService.getLoadedRefData();
	}

	public ManufacturingCost manufacturingCost() {
		Objects.requireNonNull(industryMath, "industryMath");
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");

		var manufacturing = blueprint.getActivities().get("manufacturing");
		var time = manufacturingTime(manufacturing);
		var eiv = manufacturingEiv(manufacturing);
		var runs = industryCostInput.getRuns();
		var unitsPerRun =
			manufacturing.getProducts().get(productType.getTypeId()).getQuantity();
		var units = runs * unitsPerRun;
		var systemCostIndex = industryMath.manufacturingSystemCostIndex(eiv);
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
}
