package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import java.util.Collection;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Randomly explores all valid region-type pairs.
 * It does this by producing a consistent hash of each region-type pair and putting them into a group of buckets.
 * Each day, a new bucket is selected.
 * Each bucket contains a unique group of pairs.
 * The use of consistent hashing makes it resilient to adding and removing of regions and types.
 */
@Log4j2
class ExplorerRegionTypeSource implements RegionTypeSource {
	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ObjectMapper objectMapper;

	private final int groups = Configs.ESI_MARKET_HISTORY_EXPLORATION_GROUPS.getRequired();

	@Setter
	@NonNull
	private LocalDate today;

	@Inject
	protected ExplorerRegionTypeSource() {}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return refDataUtil.marketRegions().toList().flatMapPublisher(regions -> {
			return refDataUtil.marketTypes().toList().flatMapPublisher(types -> {
				log.debug(
						"Exploring {} regions and {} types for a total space of {}",
						regions.size(),
						types.size(),
						regions.size() * types.size());
				var todaysGroup = today.toEpochDay() % groups;
				var pairs = regions.stream()
						.flatMap(region -> types.stream()
								.map(type -> new RegionTypePair(
										region.getRegionId().intValue(),
										type.getTypeId().intValue())))
						.filter(pair -> {
							var hash = hash(pair.getTypeId(), pair.getRegionId());
							var group = hash % groups;
							return group == todaysGroup;
						});
				return Flowable.fromStream(pairs);
			});
		});
	}

	private long hash(long typeId, long regionId) {
		return Math.abs(Hashing.murmur3_128()
				.newHasher()
				.putLong(typeId)
				.putLong(regionId)
				.hash()
				.padToLong());
	}
}
