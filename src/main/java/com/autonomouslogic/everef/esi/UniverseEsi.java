package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseConstellationsConstellationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseStationsStationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseSystemsSystemIdOk;
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

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	@Inject
	protected UniverseEsi() {}

	public Flowable<Integer> getRegionIds() {
		return Flowable.defer(() -> {
					log.trace("Fetching region ids");
					var d = UniverseApi.Datasource_getUniverseRegions.valueOf(datasource);
					var regions = universeApi.getUniverseRegions(d, null);
					return Flowable.fromIterable(regions);
				})
				.compose(Rx.offloadFlowable());
	}

	public Maybe<GetUniverseRegionsRegionIdOk> getRegion(int regionId) {
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching region %s", regionId));
					var source = UniverseApi.Datasource_getUniverseRegionsRegionId.valueOf(datasource);
					var region = universeApi.getUniverseRegionsRegionId(regionId, null, source, null, null);
					return Maybe.fromOptional(Optional.ofNullable(region));
				})
				.compose(Rx.offloadMaybe());
	}

	public Flowable<GetUniverseRegionsRegionIdOk> getAllRegions() {
		return getRegionIds().flatMapMaybe(regionId -> getRegion(regionId));
	}

	public Maybe<GetUniverseStationsStationIdOk> getNpcStation(int stationId) {
		return Maybe.defer(() -> {
					// All NPC stations will have an ID below 100 million.
					if (stationId > 100_000_000) {
						log.trace(String.format("Ignoring request for non-NPC station %s", stationId));
						return Maybe.empty();
					}
					log.trace(String.format("Fetching station %s", stationId));
					var source = UniverseApi.Datasource_getUniverseStationsStationId.valueOf(datasource);
					var station = universeApi.getUniverseStationsStationId(stationId, source, null);
					return Maybe.fromOptional(Optional.ofNullable(station));
				})
				.compose(Rx.offloadMaybe());
	}

	public Maybe<GetUniverseSystemsSystemIdOk> getSystem(int systemId) {
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching system %s", systemId));
					var source = UniverseApi.Datasource_getUniverseSystemsSystemId.valueOf(datasource);
					var system = universeApi.getUniverseSystemsSystemId(systemId, null, source, null, null);
					return Maybe.fromOptional(Optional.ofNullable(system));
				})
				.compose(Rx.offloadMaybe());
	}

	public Maybe<GetUniverseConstellationsConstellationIdOk> getConstellation(int constellationId) {
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching constellation %s", constellationId));
					var source = UniverseApi.Datasource_getUniverseConstellationsConstellationId.valueOf(datasource);
					var constellation = universeApi.getUniverseConstellationsConstellationId(
							constellationId, null, source, null, null);
					return Maybe.fromOptional(Optional.ofNullable(constellation));
				})
				.compose(Rx.offloadMaybe());
	}
}
