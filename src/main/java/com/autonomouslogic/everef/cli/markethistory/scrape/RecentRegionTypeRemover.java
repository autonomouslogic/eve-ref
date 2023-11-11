package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.LastCutoff;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
class RecentRegionTypeRemover implements RegionTypeSource {
	private final Instant cutoffTime;

	private final Set<RegionTypePair> pairs = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected RecentRegionTypeRemover(LastCutoff lastCutoff) {
		// The Expires header will be around the cut-off time. A cut-off on the 17th cannot contain data for the 17th.
		// Therefore, we need to subtract a day from the cut-off time to get a realistic picture of what was already
		// fetched.
		cutoffTime = lastCutoff.getEsiRefresh().minus(Duration.ofDays(1));
		log.debug("Removing entries after {}", cutoffTime);
	}

	@Override
	public void addHistory(MarketHistoryEntry entry) {
		var lastModified = entry.getHttpLastModified();
		if (lastModified != null && !lastModified.isBefore(cutoffTime)) {
			pairs.add(RegionTypePair.fromHistory(entry));
		}
	}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return Flowable.fromIterable(pairs);
	}

	@Override
	public boolean isAdditive() {
		return false;
	}
}
