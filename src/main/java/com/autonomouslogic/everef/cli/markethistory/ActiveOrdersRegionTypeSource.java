package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.esi.MarketEsi;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Provides region-type pairs based currently active types in that region as reported by the ESI's active orders endpoint.
 */
@Log4j2
class ActiveOrdersRegionTypeSource implements RegionTypeSource {
	@Inject
	protected MarketEsi marketEsi;

	private final Set<Integer> regions = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected ActiveOrdersRegionTypeSource() {}

	@Override
	public void addHistory(MarketHistoryEntry entry) {
		regions.add(RegionTypePair.fromHistory(entry).getRegionId());
	}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return Flowable.fromIterable(regions).flatMap(regionId -> marketEsi
				.getActiveMarketOrderTypes(regionId)
				.map(typeId -> new RegionTypePair(regionId, typeId)));
	}
}
