package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;

/**
 * A specific source for region-type pairs to be searched for market history.
 */
public interface RegionTypeSource {
	default void addHistory(MarketHistoryEntry entry) {}

	Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs);

	default boolean isAdditive() {
		return true;
	}
}
