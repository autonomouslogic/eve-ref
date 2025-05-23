package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.util.MathUtil;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.math.BigDecimal;

public class IndustryMath {
	@Setter
	@NonNull
	private IndustryCostInput industryCostInput;

	@Inject
	protected IndustryMath(){}

	public BigDecimal manufacturingSystemCostIndex(BigDecimal eiv) {
		var index = industryCostInput.getManufacturingCost();
		return systemCostIndex(eiv, index);
	}

	public double materialEfficiencyModifier() {
		return 1.0 - industryCostInput.getMe() / 100.0;
	}

	public double timeEfficiencyModifier() {
		return 1.0 - industryCostInput.getTe() / 100.0;
	}

	public BigDecimal inventionSystemCostIndex(BigDecimal jcb) {
		var index = industryCostInput.getInventionCost();
		return systemCostIndex(jcb, index);
	}

	public BigDecimal systemCostIndex(BigDecimal value, BigDecimal index) {
		var cost = MathUtil.round(value.multiply(index));
		return cost;
	}
}
