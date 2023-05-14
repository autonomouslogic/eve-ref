package com.autonomouslogic.everef.cli.markethistory;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;

/**
 * A specific source for region-type pairs to be searched for market history.
 */
interface RegionTypeSource {
	default void addHistory(JsonNode entry) {}

	Flowable<RegionTypePair> sourcePairs(List<RegionTypePair> currentPairs);
}
