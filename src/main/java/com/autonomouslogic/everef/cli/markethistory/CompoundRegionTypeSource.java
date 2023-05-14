package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.commons.ListUtil;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Resolved region-type pairs to be searched for market history.
 */
@Log4j2
class CompoundRegionTypeSource implements RegionTypeSource {
	private final List<RegionTypeSource> sources = new ArrayList<>();

	@Inject
	protected CompoundRegionTypeSource() {}

	public void addSource(RegionTypeSource source) {
		sources.add(source);
	}

	public Flowable<RegionTypePair> sourcePairs() {
		return sourcePairs(List.of());
	}

	@Override
	public Flowable<RegionTypePair> sourcePairs(List<RegionTypePair> currentPairs) {
		var flowable = Flowable.fromIterable(currentPairs);
		for (RegionTypeSource source : sources) {
			flowable = resolveSource(source, flowable);
		}
		return flowable;
	}

	private Flowable<RegionTypePair> resolveSource(RegionTypeSource source, Flowable<RegionTypePair> flowable) {
		return flowable.distinct().toList().flatMapPublisher(currentPairs -> {
			return source.sourcePairs(currentPairs).toList().flatMapPublisher(morePairs -> {
				var finalPairs = new HashSet<>(ListUtil.concat(currentPairs, morePairs));
				var previousRegions = countRegions(currentPairs);
				var currentRegions = countRegions(finalPairs);
				var newRegions = currentRegions - previousRegions;
				log.debug(
						"{} returned {} pairs, adding +{} new pairs, new total: {}, +{} new regions",
						source.getClass().getSimpleName(),
						morePairs.size(),
						finalPairs.size() - currentPairs.size(),
						finalPairs.size(),
						newRegions);
				return Flowable.fromIterable(finalPairs);
			});
		});
	}

	@Override
	public void addHistory(MarketHistoryEntry entry) {
		for (RegionTypeSource source : sources) {
			source.addHistory(entry);
		}
	}

	private static long countRegions(Collection<RegionTypePair> pairs) {
		return pairs.stream().mapToLong(p -> p.getRegionId()).distinct().count();
	}
}
