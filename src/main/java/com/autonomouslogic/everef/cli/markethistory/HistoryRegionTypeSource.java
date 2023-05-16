package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypeMarketCap;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.MarketCapCalc;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Provides region-type pairs based on previously observed pairs.
 */
@Log4j2
class HistoryRegionTypeSource implements RegionTypeSource {
	private static final Ordering<RegionTypeMarketCap> ORDERING =
			Ordering.natural().onResultOf(RegionTypeMarketCap::getCap).reverse();

	@Inject
	protected MarketCapCalc marketCapCalc;

	@Inject
	protected HistoryRegionTypeSource() {}

	@Override
	public void addHistory(MarketHistoryEntry entry) {
		marketCapCalc.add(entry);
	}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		var sortedCaps = marketCapCalc.getRegionTypeMarketCaps().stream()
				.sorted(ORDERING)
				.toList();
		sortedCaps.stream().limit(20).forEach(cap -> log.trace("Top cap: {}", cap));
		var pairs = sortedCaps.stream()
				.map(cap -> new RegionTypePair(cap.getRegionId(), cap.getTypeId()))
				.toList();
		return Flowable.fromIterable(pairs);
	}
}
