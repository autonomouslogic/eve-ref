package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.IndustryStructure;
import com.autonomouslogic.everef.model.api.CopyingCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.BlueprintActivity;
import com.autonomouslogic.everef.util.MathUtil;
import java.time.Duration;
import java.util.List;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;

public class CopyingCalculator {
	@Inject
	protected IndustrySkills industrySkills;

	@Inject
	protected IndustryMath industryMath;

	@Inject
	protected IndustryRigs industryRigs;

	@Inject
	protected IndustryStructures industryStructures;

	@Setter
	@NonNull
	private IndustryCostInput industryCostInput;

	@Setter
	private IndustryStructure structure;

	@Setter
	private List<IndustryRig> rigs;

	@Setter
	@NonNull
	private Blueprint blueprint;

	@Setter
	private int runs;

	@Inject
	protected CopyingCalculator() {}

	public CopyingCost calc() {
		var copying = blueprint.getActivities().get("copying");
		var manufacturing = blueprint.getActivities().get("manufacturing");
		if (copying == null) {
			throw new RuntimeException(
					String.format("Blueprint %s doesn't have a copying activity", blueprint.getBlueprintTypeId()));
		}
		if (manufacturing == null) {
			throw new RuntimeException(String.format(
					"Blueprint %s doesn't have a manufacturing activity", blueprint.getBlueprintTypeId()));
		}
		var eiv = industryMath.eiv(manufacturing, runs);
		var jcb = industryMath.jobCostBase(eiv);
		var facilityTax = industryMath.facilityTax(industryCostInput, jcb);
		var sccSurcharge = industryMath.sccSurcharge(jcb);
		var alphaCloneTax = industryMath.alphaCloneTax(industryCostInput, jcb);
		var systemCostIndex = industryMath.copyingSystemCostIndex(industryCostInput, jcb);
		var systemCostBonuses = industryMath.systemCostBonuses(
				structure,
				null,
				rigs,
				industryCostInput.getSecurity(),
				industryCostInput.getSystemCostBonus(),
				systemCostIndex,
				"copying");
		var totalJobCost = systemCostIndex
				.add(facilityTax)
				.add(sccSurcharge)
				.add(alphaCloneTax)
				.add(systemCostBonuses);
		var time = copyingTime(copying);
		return CopyingCost.builder()
				.productId(blueprint.getBlueprintTypeId())
				.runs(runs)
				.estimatedItemValue(eiv)
				.jobCostBase(jcb)
				.time(time)
				.systemCostIndex(systemCostIndex)
				.systemCostBonuses(systemCostBonuses)
				.facilityTax(facilityTax)
				.sccSurcharge(sccSurcharge)
				.alphaCloneTax(alphaCloneTax)
				.totalJobCost(totalJobCost)
				.totalCost(totalJobCost)
				.totalCostPerRun(MathUtil.round(MathUtil.divide(totalJobCost, runs), 2))
				.build();
	}

	private Duration copyingTime(BlueprintActivity copying) {
		var baseTime = (double) copying.getTime();
		var rigMod = industryRigs.rigModifier(
				rigs, null, industryCostInput.getSecurity(), IndustryRig::getTimeBonus, "copying");
		var structureMod = industryStructures.structureTimeModifier(structure);
		var advancedIndustryMod = industrySkills.advancedIndustrySkillIndustryJobTimeBonusMod(industryCostInput);
		var scienceMod = industrySkills.copySpeedBonus(industryCostInput);
		var time = runs * baseTime * advancedIndustryMod * scienceMod * structureMod * rigMod;
		var rounded = (long) Math.round(time);
		return Duration.ofSeconds(rounded);
	}
}
