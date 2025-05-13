package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.service.RefDataService;

import javax.inject.Inject;

public class IndustryCostCalculator {
	@Inject
	protected RefDataService refDataService;

	@Inject
	protected IndustryCostCalculator() {}

	public IndustryCost calc(IndustryCostInput input) {
		
	}
}
