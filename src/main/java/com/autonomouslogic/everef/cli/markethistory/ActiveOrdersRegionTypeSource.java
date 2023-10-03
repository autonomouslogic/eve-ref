package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.esi.MarketEsi;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.RefDataUtil;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Provides region-type pairs based currently active types in that region as reported by the ESI's active orders endpoint.
 */
@Log4j2
class ActiveOrdersRegionTypeSource implements RegionTypeSource {
	@Inject
	protected MarketEsi marketEsi;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ActiveOrdersRegionTypeSource() {}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return refDataUtil.allRegions().flatMap(region -> {
			var regionId = region.getRegionId().intValue();
			return marketEsi.getActiveMarketOrderTypes(regionId).map(typeId -> new RegionTypePair(regionId, typeId));
		});
	}
}
