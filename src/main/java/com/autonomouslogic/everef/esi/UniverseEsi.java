package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class UniverseEsi {
	@Inject
	protected UniverseApi universeApi;

	@Inject
	protected UniverseEsi() {}

	public Flowable<Integer> getRegionIds() {
		return Flowable.defer(() -> {
					log.trace("Fetching region ids");
					return Flowable.fromIterable(universeApi.getUniverseRegions(null, null));
				})
				.compose(Rx.offloadFlowable());
	}

	public Maybe<GetUniverseRegionsRegionIdOk> getRegion(int regionId) {
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching region %s", regionId));
					var region = universeApi.getUniverseRegionsRegionId(regionId, null, null, null, null);
					return Maybe.fromOptional(Optional.ofNullable(region));
				})
				.compose(Rx.offloadMaybe());
	}

	public Flowable<GetUniverseRegionsRegionIdOk> getAllRegions() {
		return getRegionIds().flatMapMaybe(regionId -> getRegion(regionId));
	}
}
