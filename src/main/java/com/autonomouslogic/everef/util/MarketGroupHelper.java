package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;

public class MarketGroupHelper {
    public static MarketGroup getRootMarketGroup(InventoryType type, LoadedRefData loadedRefData) {
        if (type.getMarketGroupId() == null) {
            return null;
        }
        var marketGroup = loadedRefData.getMarketGroup(type.getMarketGroupId());
        if (marketGroup == null) {
            return null;
        }
        return MarketGroupHelper.getRootMarketGroup(marketGroup, loadedRefData);
    }

    public static MarketGroup getRootMarketGroup(MarketGroup marketGroup, LoadedRefData loadedRefData) {
        if (marketGroup.getParentGroupId() == null) {
            return marketGroup;
        }
        var parentGroup = loadedRefData.getMarketGroup(marketGroup.getParentGroupId());
        if (parentGroup == null) {
            return marketGroup;
        }
        return MarketGroupHelper.getRootMarketGroup(parentGroup, loadedRefData);
    }
}
