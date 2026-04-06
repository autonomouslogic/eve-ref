package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.data.LoadedRefData;
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
		var loadedRefData = refDataService.getLoadedRefData();
		return getRootMarketGroup(type, loadedRefData);
	}

	public MarketGroup getRootMarketGroup(InventoryType type, LoadedRefData loadedRefData) {
		if (type.getMarketGroupId() == null) {
			return null;
		}
		var marketGroup = loadedRefData.getMarketGroup(type.getMarketGroupId());
		if (marketGroup == null) {
			return null;
		}
		return getRootMarketGroup(marketGroup, loadedRefData);
	}

	public MarketGroup getRootMarketGroup(MarketGroup marketGroup) {
		var loadedRefData = refDataService.getLoadedRefData();
		return getRootMarketGroup(marketGroup, loadedRefData);
	}

	public MarketGroup getRootMarketGroup(MarketGroup marketGroup, LoadedRefData loadedRefData) {
		if (marketGroup.getParentGroupId() == null) {
			return marketGroup;
		}
		var parentGroup = loadedRefData.getMarketGroup(marketGroup.getParentGroupId());
		if (parentGroup == null) {
			return marketGroup;
		}
		return getRootMarketGroup(parentGroup, loadedRefData);
	}
}
