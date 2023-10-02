package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.reactivex.rxjava3.core.Flowable;
import java.io.File;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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
	private static final List<String> validUniverseIds = List.of("eve", "wormhole");

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
		var todaysGroup = today.toEpochDay() % groups;
		return refDataUtil.downloadLatestReferenceData().flatMapPublisher(file -> {
			var regions = loadRegions(file).cache();
			var types = loadTypes(file).cache();
			return regions.flatMap(region -> types.flatMap(type -> {
				var hash = hash(type, region);
				var group = hash % groups;
				if (group == todaysGroup) {
					return Flowable.just(new RegionTypePair(
							region.getRegionId().intValue(), type.getTypeId().intValue()));
				}
				return Flowable.empty();
			}));
		});
	}

	private Flowable<InventoryType> loadTypes(File file) {
		return refDataUtil
				.loadReferenceDataArchive(file, "types", InventoryType.class)
				.filter(type -> type.getMarketGroupId() != null);
	}

	private Flowable<Region> loadRegions(File file) {
		return refDataUtil
				.loadReferenceDataArchive(file, "regions", Region.class)
				.filter(region -> region.getUniverseId() != null)
				.filter(region -> validUniverseIds.contains(region.getUniverseId()));
	}

	private long hash(InventoryType type, Region region) {
		return Math.abs(Hashing.murmur3_128()
				.newHasher()
				.putLong(type.getTypeId())
				.putLong(region.getRegionId())
				.hash()
				.padToLong());
	}
}
