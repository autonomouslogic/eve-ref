package com.autonomouslogic.everef.industry;

import static com.autonomouslogic.everef.industry.IndustrySkills.GLOBAL_TIME_BONUSES;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.ManufacturingCost;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.util.MathUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class ManufactureCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

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
	private int runs;

	@Setter
	private int me;

	@Setter
	private int te;

	@Setter
	private IndustryStructure structure;

	@Setter
	private List<IndustryRig> rigs;

	@Inject
	protected ManufactureCalculator(RefDataService refDataService) {
		refData = refDataService.getLoadedRefData();
	}

	public ManufacturingCost calc() {
		Objects.requireNonNull(industryCostInput, "industryCostInput");
		Objects.requireNonNull(productType, "productType");
		Objects.requireNonNull(blueprint, "blueprint");
		if (runs <= 0) {
			throw new IllegalArgumentException("Runs must be positive");
		}

		var manufacturing = blueprint.getActivities().get("manufacturing");
		var time = manufacturingTime(manufacturing);
		var eiv = industryMath.eiv(manufacturing, industryCostInput.getRuns());
		var runs = industryCostInput.getRuns();
		var unitsPerRun =
				manufacturing.getProducts().get(productType.getTypeId()).getQuantity();
		var units = runs * unitsPerRun;
		var systemCostIndex = manufacturingSystemCostIndex(eiv);
		var systemCostBonuses = industryMath.systemCostBonuses(
				structure,
				productType,
				rigs,
				industryCostInput.getSecurity(),
				industryCostInput.getSystemCostBonus(),
				systemCostIndex,
				"manufacturing");
		var facilityTax = industryMath.facilityTax(industryCostInput, eiv);
		var sccSurcharge = industryMath.sccSurcharge(eiv);
		var alphaCloneTax = industryMath.alphaCloneTax(industryCostInput, eiv);
		var totalJobCost = systemCostIndex
				.add(facilityTax)
				.add(sccSurcharge)
				.add(alphaCloneTax)
				.add(systemCostBonuses);
		var materials = manufacturingMaterials(manufacturing);
		var totalMaterialCost = industryMath.totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return ManufacturingCost.builder()
				.productId(industryCostInput.getProductId())
				.blueprintId(blueprint.getBlueprintTypeId())
				.runs(runs)
			.me(me)
			.te(te)
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

	public BigDecimal manufacturingSystemCostIndex(BigDecimal eiv) {
		var index = industryCostInput.getManufacturingCost();
		return industryMath.systemCostIndex(eiv, index);
	}

	private Duration manufacturingTime(BlueprintActivity manufacturing) {
		var baseTime = (double) manufacturing.getTime();
		var teMod = industryMath.efficiencyModifier(te);
		var industryMod = 1.0 - GLOBAL_TIME_BONUSES.get("Industry") * industryCostInput.getIndustry();
		var advancedIndustryMod =
				1.0 - GLOBAL_TIME_BONUSES.get("Advanced Industry") * industryCostInput.getAdvancedIndustry();
		var specialSkillMod = skillMath.manufacturingSpecialisedSkillMod(manufacturing, industryCostInput);
		var structureMod = industryStructures.structureTimeModifier(structure);
		var rigMod = industryRigs.rigModifier(
				rigs, productType, industryCostInput.getSecurity(), IndustryRig::getTimeBonus, "manufacturing");
		var time =
				runs * baseTime * teMod * industryMod * advancedIndustryMod * specialSkillMod * structureMod * rigMod;
		var rounded = Math.round(time);
		return Duration.ofSeconds(rounded);
	}

	private Map<String, MaterialCost> manufacturingMaterials(BlueprintActivity manufacturing) {
		return industryMath.materials(manufacturing, this::manufacturingMaterialQuantity);
	}

	private long manufacturingMaterialQuantity(long base) {
		var meMod = industryMath.efficiencyModifier(me);
		var structureMod = industryStructures.structureManufacturingMaterialModifier(structure);
		var rigMod = industryRigs.rigModifier(
				rigs, productType, industryCostInput.getSecurity(), IndustryRig::getMaterialBonus, "manufacturing");
		var quantity = runs * base * meMod * structureMod * rigMod;
		var rounded = Math.max(runs, Math.ceil(Math.round(quantity * 100.0) / 100.0));
		return (long) rounded;
	}
}
