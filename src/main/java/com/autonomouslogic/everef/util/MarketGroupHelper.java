package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.service.RefDataService;
import jakarta.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MarketGroupHelper {
	@Inject
	protected RefDataService refDataService;

	@Inject
	public MarketGroupHelper() {}

	public MarketGroup getRootMarketGroup(InventoryType type) {
		if (type.getMarketGroupId() == null) {
			return null;
		}
		var loadedRefData = refDataService.getLoadedRefData();
		var marketGroup = loadedRefData.getMarketGroup(type.getMarketGroupId());
		if (marketGroup == null) {
			return null;
		}
		return getRootMarketGroup(marketGroup);
	}

	public MarketGroup getRootMarketGroup(MarketGroup marketGroup) {
		if (marketGroup.getParentGroupId() == null) {
			return marketGroup;
		}
		var loadedRefData = refDataService.getLoadedRefData();
		var parentGroup = loadedRefData.getMarketGroup(marketGroup.getParentGroupId());
		if (parentGroup == null) {
			return marketGroup;
		}
		return getRootMarketGroup(parentGroup);
	}
}
