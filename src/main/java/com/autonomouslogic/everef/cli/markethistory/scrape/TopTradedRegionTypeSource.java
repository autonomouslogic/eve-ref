package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.model.RegionTypeMarketCap;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.MarketCapCalc;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Flowable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Provides region-type pairs based on global trade volume.
 * It calculates the total traded cap for every type and provides those types in sorted order in every region.
 */
@Log4j2
class TopTradedRegionTypeSource implements RegionTypeSource {
	@Inject
	protected MarketCapCalc marketCapCalc;

	@Inject
	protected TopTradedRegionTypeSource() {}

	@Override
	public void addHistory(MarketHistoryEntry entry) {
		marketCapCalc.add(entry);
	}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return Flowable.defer(() -> {
					var regions = getRegions();
					var typesPerRegion = getDesiredTypesPerRegion(regions.size());
					var typeCaps = getTypeCaps();
					return Flowable.fromIterable(regions).flatMap(region -> Flowable.fromIterable(typeCaps)
							.map(Pair::getLeft)
							.map(typeId -> new RegionTypePair(region, typeId))
							.filter(pair -> !currentPairs.contains(pair))
							.take(typesPerRegion));
				})
				.onErrorResumeNext(e -> {
					log.warn("Failed handling top traded items, ignoring", e);
					return Flowable.empty();
				});
	}

	private Set<Integer> getRegions() {
		return marketCapCalc.getRegionTypeMarketCaps().stream()
				.map(RegionTypeMarketCap::getRegionId)
				.collect(Collectors.toSet());
	}

	private List<Pair<Integer, BigDecimal>> getTypeCaps() {
		return marketCapCalc.getRegionTypeMarketCaps().stream()
				.collect(Collectors.groupingBy(
						RegionTypeMarketCap::getTypeId,
						Collectors.reducing(BigDecimal.ZERO, RegionTypeMarketCap::getCap, BigDecimal::add)))
				.entrySet()
				.stream()
				.map(e -> Pair.of(e.getKey(), e.getValue()))
				.sorted(Ordering.natural()
						.onResultOf(
								(com.google.common.base.Function<Pair<Integer, BigDecimal>, BigDecimal>) Pair::getRight)
						.reversed())
				.toList();
	}

	private int getDesiredTypesPerRegion(int regionCount) {
		var rateLimit = (double) Configs.ESI_RATE_LIMIT_PER_S.getRequired();
		var maxTime = Configs.MARKET_HISTORY_TOP_TRADED_MAX_TIME.getRequired();
		var pairs = maxTime.toMillis() / 1000.0 * rateLimit;
		var typesPerRegion = (int) Math.floor(pairs / regionCount);
		log.debug(
				"Max runtime: {}, regions: {}, rate limit: {} = {} types per region",
				maxTime,
				regionCount,
				rateLimit,
				typesPerRegion);
		return typesPerRegion;
	}
}
