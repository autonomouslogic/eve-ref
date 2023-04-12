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
import io.reactivex.rxjava3.functions.Supplier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class UniverseEsi {
	@Inject
	protected UniverseApi universeApi;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	private List<Integer> regionIds;
	private final Map<Integer, Optional<GetUniverseRegionsRegionIdOk>> regions = new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseConstellationsConstellationIdOk>> constellations =
			new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseSystemsSystemIdOk>> systems = new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseStationsStationIdOk>> stations = new ConcurrentHashMap<>();
	private final Map<Integer, Optional<GetUniverseTypesTypeIdOk>> types = new ConcurrentHashMap<>();

	@Inject
	protected UniverseEsi() {}

	public Flowable<Integer> getRegionIds() {
		return Flowable.defer(() -> {
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
					.compose(Rx.offloadFlowable(EsiHelper.ESI_SCHEDULER));
		});
	}

	public Maybe<GetUniverseRegionsRegionIdOk> getRegion(int regionId) {
		return getFromCacheOrFetch("region", GetUniverseRegionsRegionIdOk.class, regions, regionId, () -> {
			var source = UniverseApi.Datasource_getUniverseRegionsRegionId.valueOf(datasource);
			return universeApi.getUniverseRegionsRegionId(regionId, null, source, null, null);
		});
	}

	public Flowable<GetUniverseRegionsRegionIdOk> getAllRegions() {
		return getRegionIds().flatMapMaybe(regionId -> getRegion(regionId));
	}

	public Maybe<GetUniverseConstellationsConstellationIdOk> getConstellation(int constellationId) {
		return getFromCacheOrFetch(
				"constellation",
				GetUniverseConstellationsConstellationIdOk.class,
				constellations,
				constellationId,
				() -> {
					var source = UniverseApi.Datasource_getUniverseConstellationsConstellationId.valueOf(datasource);
					return universeApi.getUniverseConstellationsConstellationId(
							constellationId, null, source, null, null);
				});
	}

	public Maybe<GetUniverseSystemsSystemIdOk> getSystem(int systemId) {
		return getFromCacheOrFetch("system", GetUniverseSystemsSystemIdOk.class, systems, systemId, () -> {
			var source = UniverseApi.Datasource_getUniverseSystemsSystemId.valueOf(datasource);
			return universeApi.getUniverseSystemsSystemId(systemId, null, source, null, null);
		});
	}

	public Maybe<GetUniverseStationsStationIdOk> getNpcStation(long stationId) {
		// All NPC stations will have an ID below 100 million.
		if (stationId > 100_000_000) {
			log.trace(String.format("Ignoring request for non-NPC station %s", stationId));
			return Maybe.empty();
		}
		var intId = (int) stationId;
		return getFromCacheOrFetch("station", GetUniverseStationsStationIdOk.class, stations, intId, () -> {
			var source = UniverseApi.Datasource_getUniverseStationsStationId.valueOf(datasource);
			return universeApi.getUniverseStationsStationId(intId, source, null);
		});
	}

	public Maybe<GetUniverseTypesTypeIdOk> getType(int typeId) {
		return getFromCacheOrFetch("type", GetUniverseTypesTypeIdOk.class, types, typeId, () -> {
			var source = UniverseApi.Datasource_getUniverseTypesTypeId.valueOf(datasource);
			return universeApi.getUniverseTypesTypeId(typeId, null, source, null, null);
		});
	}

	@NotNull
	private <T> Maybe<T> getFromCacheOrFetch(
			String name, Class<T> type, Map<Integer, Optional<T>> cache, int id, Supplier<T> fetcher) {
		return Maybe.defer(() -> {
					if (cache.containsKey(id)) {
						return Maybe.fromOptional(cache.get(id));
					}
					return Maybe.defer(() -> {
								log.trace("Fetching {} {}", name, id);
								var obj = fetcher.get();
								var optional = Optional.ofNullable(obj);
								cache.put(id, optional);
								return Maybe.fromOptional(optional);
							})
							.compose(Rx.offloadMaybe(EsiHelper.ESI_SCHEDULER));
				})
				.onErrorResumeNext(
						e -> Maybe.error(new RuntimeException(String.format("Failed fetching %s %s", name, id), e)));
	}
}
