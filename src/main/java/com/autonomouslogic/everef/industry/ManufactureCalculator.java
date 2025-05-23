package com.autonomouslogic.everef.industry;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import lombok.NonNull;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ManufactureCalculator {
	@Inject
	protected MarketPriceService marketPriceService;

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

	protected ManufactureCalculator(RefDataService refDataService) {
		refData = refDataService.getLoadedRefData();
	}
}
