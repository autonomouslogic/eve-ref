package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.InventoryType;
import javax.inject.Inject;
import lombok.Setter;

public class IndustryCostCalculator {
	@Setter
	private IndustryCostInput industryCostInput;

	@Setter
	private InventoryType productType;

	@Setter
	private Blueprint blueprint;

	@Inject
	protected IndustryCostCalculator() {}

	public IndustryCost calc() {
		return IndustryCost.builder().build();
	}
}
