package com.autonomouslogic.everef.cli.markethistory.scrape;

import com.autonomouslogic.everef.model.RegionTypePair;
import com.autonomouslogic.everef.util.EveConstants;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Collection;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Removes PLEX from all non-PLEX regions.
 */
@Log4j2
class PlexRegionTypeRemover implements RegionTypeSource {
	@Inject
	protected PlexRegionTypeRemover() {}

	@Override
	public Flowable<RegionTypePair> sourcePairs(Collection<RegionTypePair> currentPairs) {
		return Flowable.fromStream(currentPairs.stream()
				.filter(pair -> (pair.getRegionId() == EveConstants.GPMR_01_REGION_ID)
						!= (pair.getTypeId() == EveConstants.PLEX_TYPE_ID)));
	}

	@Override
	public boolean isAdditive() {
		return false;
	}
}
