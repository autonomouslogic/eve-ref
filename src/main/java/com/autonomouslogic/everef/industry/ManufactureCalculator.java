package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.ManufacturingCost;
import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.EsiMarketPriceService;
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
	protected EsiMarketPriceService esiMarketPriceService;

	@Inject
	protected IndustryMath industryMath;

	@Inject
	protected IndustryStructures industryStructures;

	@Inject
	protected IndustryRigs industryRigs;

	@Inject
	protected IndustrySkills industrySkills;

	private final LoadedRefData refData;

	@Setter
	@NonNull
	private String activity;

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
		Objects.requireNonNull(activity, "activity");
		if (!activity.equals("manufacturing") && !activity.equals("reaction")) {
			throw new IllegalArgumentException("Unknown activity: " + activity);
		}
		if (runs <= 0) {
			throw new IllegalArgumentException("Runs must be positive");
		}
		if (me < 0) {
			throw new IllegalArgumentException("ME must not be negative");
		}
		if (te < 0) {
			throw new IllegalArgumentException("TE must not be negative");
		}

		var blueprintActivity = blueprint.getActivities().get(activity);
		var time = activityTime(blueprintActivity);
		var eiv = industryMath.eiv(blueprintActivity, runs);
		var unitsPerRun =
				blueprintActivity.getProducts().get(productType.getTypeId()).getQuantity();
		var units = runs * unitsPerRun;
		var productVolume = industryMath.typeVolume(productType, units);
		var systemCostIndex = systemCostIndex(eiv);
		var systemCostBonuses = industryMath.systemCostBonuses(
				structure,
				productType,
				rigs,
				industryCostInput.getSecurity(),
				industryCostInput.getSystemCostBonus(),
				systemCostIndex,
				activity);
		var facilityTax = industryMath.facilityTax(industryCostInput, eiv);
		var sccSurcharge = industryMath.sccSurcharge(eiv);
		var alphaCloneTax = industryMath.alphaCloneTax(industryCostInput, eiv);
		var totalJobCost = systemCostIndex
				.add(facilityTax)
				.add(sccSurcharge)
				.add(alphaCloneTax)
				.add(systemCostBonuses);
		var materials = manufacturingMaterials(blueprintActivity);
		var materialsVolume = industryMath.materialVolume(materials);
		var totalMaterialCost = industryMath.totalMaterialCost(materials);
		var totalCost = totalJobCost.add(totalMaterialCost);
		return ManufacturingCost.builder()
				.productId(productType.getTypeId())
				.blueprintId(blueprint.getBlueprintTypeId())
				.runs(runs)
				.me(me)
				.te(te)
				.units(units)
				.unitsPerRun(unitsPerRun)
				.productVolume(productVolume)
				.materials(materials)
				.materialsVolume(materialsVolume)
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

	public BigDecimal systemCostIndex(BigDecimal eiv) {
		var index =
				switch (activity) {
					case "manufacturing" -> industryCostInput.getManufacturingCost();
					case "reaction" -> industryCostInput.getReactionCost();
					default -> throw new IllegalStateException(activity);
				};
		return industryMath.systemCostIndex(eiv, index);
	}

	private Duration activityTime(BlueprintActivity blueprintActivity) {
		var baseTime = (double) blueprintActivity.getTime();
		var teMod = industryMath.efficiencyModifier(te);
		var skillMod = skillMod(blueprintActivity);
		var structureMod = industryStructures.structureTimeModifier(structure);
		var rigMod = industryRigs.rigModifier(
				rigs, productType, industryCostInput.getSecurity(), IndustryRig::getTimeBonus, activity);
		var time = runs * baseTime * teMod * skillMod * structureMod * rigMod;
		var rounded = Math.round(time);
		return Duration.ofSeconds(rounded);
	}

	private double skillMod(BlueprintActivity blueprintActivity) {
		return switch (activity) {
			case "manufacturing" -> manufacturingSkillMod(blueprintActivity);
			case "reaction" -> reactionSkillMod();
			default -> throw new IllegalStateException(activity);
		};
	}

	private double manufacturingSkillMod(BlueprintActivity blueprintActivity) {
		var industryMod = industrySkills.manufacturingTimeBonusMod(industryCostInput);
		var advancedIndustryMod = industrySkills.advancedIndustrySkillIndustryJobTimeBonusMod(industryCostInput);
		var specialSkillMod = industrySkills.manufacturingSpecialisedSkillMod(industryCostInput, blueprintActivity);
		return industryMod * advancedIndustryMod * specialSkillMod;
	}

	private double reactionSkillMod() {
		var reactionsMod = industrySkills.reactionTimeBonusMod(industryCostInput);
		return reactionsMod;
	}

	private Map<String, MaterialCost> manufacturingMaterials(BlueprintActivity manufacturing) {
		return industryMath.materials(manufacturing, this::materialQuantity, industryCostInput.getMaterialPrices());
	}

	private long materialQuantity(long base) {
		var meMod = industryMath.efficiencyModifier(me);
		var structureMod = activity.equals("manufacturing")
				? industryStructures.structureManufacturingMaterialModifier(structure)
				: 1.0;
		var rigMod = industryRigs.rigModifier(
				rigs, productType, industryCostInput.getSecurity(), IndustryRig::getMaterialBonus, activity);
		var quantity = runs * base * meMod * structureMod * rigMod;
		var rounded = Math.max(runs, Math.ceil(Math.round(quantity * 100.0) / 100.0));
		return (long) rounded;
	}
}
