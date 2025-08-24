package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.EveConstants;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Adds PLEX region for PLEX and removes PLEX from all other regions.
 */
@Log4j2
class PlexRegionTypeSource implements RegionTypeSource {
	@Inject
	protected PlexRegionTypeSource() {}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		var filtered = currentPairs.stream()
				.filter(pair -> (pair.getRegionId() == EveConstants.GPMR_01_REGION_ID)
						== (pair.getTypeId() == EveConstants.PLEX_TYPE_ID));
		var concat = Stream.concat(
				Stream.of(new RegionTypePair(EveConstants.GPMR_01_REGION_ID, (int) EveConstants.PLEX_TYPE_ID)),
				filtered);
		return Flowable.fromStream(concat);
	}
}
