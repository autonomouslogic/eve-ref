package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseConstellationsConstellationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseStationsStationIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseSystemsSystemIdOk;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseTypesTypeIdOk;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class UniverseEsi {
	@Inject
	protected UniverseApi universeApi;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	private List<Integer> regionIds;
	private final Map<Integer, Optional<GetUniverseRegionsRegionIdOk>> regions = new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseStationsStationIdOk>> stations = new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseSystemsSystemIdOk>> systems = new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseConstellationsConstellationIdOk>> constellations =
			new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseTypesTypeIdOk>> types = new ConcurrentHashMap<>();

	@Inject
	protected UniverseEsi() {}

	public Flowable<Integer> getRegionIds() {
		if (regionIds != null) {
			return Flowable.fromIterable(regionIds);
		}
		return Flowable.defer(() -> {
					log.trace("Fetching region ids");
					var d = UniverseApi.Datasource_getUniverseRegions.valueOf(datasource);
					var regions = universeApi.getUniverseRegions(d, null);
					regionIds = regions;
					return Flowable.fromIterable(regions);
				})
				.compose(Rx.offloadFlowable());
	}

	public Maybe<GetUniverseRegionsRegionIdOk> getRegion(int regionId) {
		if (regions.containsKey(regionId)) {
			return Maybe.fromOptional(regions.get(regionId));
		}
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching region %s", regionId));
					var source = UniverseApi.Datasource_getUniverseRegionsRegionId.valueOf(datasource);
					var region = universeApi.getUniverseRegionsRegionId(regionId, null, source, null, null);
					var optional = Optional.ofNullable(region);
					regions.put(regionId, optional);
					return Maybe.fromOptional(optional);
				})
				.compose(Rx.offloadMaybe());
	}

	public Flowable<GetUniverseRegionsRegionIdOk> getAllRegions() {
		return getRegionIds().flatMapMaybe(regionId -> getRegion(regionId));
	}

	public Maybe<GetUniverseStationsStationIdOk> getNpcStation(int stationId) {
		// All NPC stations will have an ID below 100 million.
		if (stationId > 100_000_000) {
			log.trace(String.format("Ignoring request for non-NPC station %s", stationId));
			return Maybe.empty();
		}
		if (stations.containsKey(stationId)) {
			return Maybe.fromOptional(stations.get(stationId));
		}
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching station %s", stationId));
					var source = UniverseApi.Datasource_getUniverseStationsStationId.valueOf(datasource);
					var station = universeApi.getUniverseStationsStationId(stationId, source, null);
					var optional = Optional.ofNullable(station);
					stations.put(stationId, optional);
					return Maybe.fromOptional(optional);
				})
				.compose(Rx.offloadMaybe());
	}

	public Maybe<GetUniverseSystemsSystemIdOk> getSystem(int systemId) {
		if (systems.containsKey(systemId)) {
			return Maybe.fromOptional(systems.get(systemId));
		}
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching system %s", systemId));
					var source = UniverseApi.Datasource_getUniverseSystemsSystemId.valueOf(datasource);
					var system = universeApi.getUniverseSystemsSystemId(systemId, null, source, null, null);
					var optional = Optional.ofNullable(system);
					systems.put(systemId, optional);
					return Maybe.fromOptional(optional);
				})
				.compose(Rx.offloadMaybe());
	}

	public Maybe<GetUniverseConstellationsConstellationIdOk> getConstellation(int constellationId) {
		if (constellations.containsKey(constellationId)) {
			return Maybe.fromOptional(constellations.get(constellationId));
		}
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching constellation %s", constellationId));
					var source = UniverseApi.Datasource_getUniverseConstellationsConstellationId.valueOf(datasource);
					var constellation = universeApi.getUniverseConstellationsConstellationId(
							constellationId, null, source, null, null);
					var optional = Optional.ofNullable(constellation);
					constellations.put(constellationId, optional);
					return Maybe.fromOptional(optional);
				})
				.compose(Rx.offloadMaybe());
	}

	public Maybe<GetUniverseTypesTypeIdOk> getType(int typeId) {
		if (types.containsKey(typeId)) {
			return Maybe.fromOptional(types.get(typeId));
		}
		return Maybe.defer(() -> {
					log.trace(String.format("Fetching type %s", typeId));
					var source = UniverseApi.Datasource_getUniverseTypesTypeId.valueOf(datasource);
					var type = universeApi.getUniverseTypesTypeId(typeId, null, source, null, null);
					var optional = Optional.ofNullable(type);
					types.put(typeId, optional);
					return Maybe.fromOptional(optional);
				})
				.compose(Rx.offloadMaybe());
	}
}
