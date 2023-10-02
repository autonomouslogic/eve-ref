package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypePair;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Combines region-type pairs from multiple sources.
 */
@Log4j2
class CompoundRegionTypeSource implements RegionTypeSource {
	@Setter
	@NonNull
	private MarketHistorySourceStats stats;

	private final int rateLimit = Configs.ESI_RATE_LIMIT_PER_S.getRequired();

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
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		var flowable = Flowable.fromIterable(currentPairs);
		for (RegionTypeSource source : sources) {
			flowable = resolveSource(source, flowable);
		}
		return flowable;
	}

	private Flowable<RegionTypePair> resolveSource(RegionTypeSource source, Flowable<RegionTypePair> flowable) {
		return flowable.toList().flatMapPublisher(currentPairsList -> {
			final var currentPairs = new LinkedHashSet<>(currentPairsList);
			return source.sourcePairs(currentPairs).toList().flatMapPublisher(sourcePairsList -> {
				final var sourcePairs = new LinkedHashSet<>(sourcePairsList);
				var finalPairs = new LinkedHashSet<RegionTypePair>();
				finalPairs.addAll(currentPairs);
				if (source.isAdditive()) {
					var newPairs = sourcePairs.stream()
							.filter(p -> !currentPairs.contains(p))
							.toList();
					stats.sourceAll(source, newPairs);
					finalPairs.addAll(newPairs);
				} else {
					finalPairs.removeAll(sourcePairs);
				}
				var previousRegions = countRegions(currentPairs);
				var currentRegions = countRegions(finalPairs);
				var newRegions = currentRegions - previousRegions;
				var newPairsCount = finalPairs.size() - currentPairs.size();
				var runtime = Duration.ofMillis((long) (1000.0 * newPairsCount / rateLimit))
						.truncatedTo(ChronoUnit.SECONDS);
				log.debug(
						"{} returned {} pairs, adding {} new pairs, new total is {}, {} new regions, est. runtime {}",
						source.getClass().getSimpleName(),
						sourcePairs.size(),
						newPairsCount,
						finalPairs.size(),
						newRegions,
						runtime);
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
