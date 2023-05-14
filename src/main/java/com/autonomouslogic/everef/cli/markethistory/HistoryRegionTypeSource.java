package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
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
class HistoryRegionTypeSource implements RegionTypeSource {
	private final Set<RegionTypePair> pairs = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected HistoryRegionTypeSource() {}

	@Override
	public void addHistory(MarketHistoryEntry entry) {
		pairs.add(RegionTypePair.fromHistory(entry));
	}

	@Override
	public Flowable<RegionTypePair> sourcePairs(List<RegionTypePair> currentPairs) {
		return Flowable.fromIterable(pairs);
	}
}
