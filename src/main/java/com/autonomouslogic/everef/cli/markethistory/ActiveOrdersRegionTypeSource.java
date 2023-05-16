package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.esi.MarketEsi;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Proviedes region-type pairs based on previously observed pairs.
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
	public Flowable<RegionTypePair> sourcePairs(List<RegionTypePair> currentPairs) {
		return Flowable.fromIterable(regions).flatMap(regionId -> marketEsi
				.getActiveMarketOrderTypes(regionId)
				.map(typeId -> new RegionTypePair(regionId, typeId)));
	}
}
