package com.autonomouslogic.everef.esi;

import static com.autonomouslogic.everef.util.EveConstants.NPC_STATION_MAX_ID;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.api.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseConstellationsConstellationIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseStationsStationIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseSystemsSystemIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseTypesTypeIdOk;
import com.autonomouslogic.everef.util.VirtualThreads;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class UniverseEsi {
	@Inject
	protected UniverseApi universeApi;

	private final EsiConstants.Datasource datasource =
			EsiConstants.Datasource.valueOf(Configs.ESI_DATASOURCE.getRequired());

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
						var regions = VirtualThreads.offload(
								() -> universeApi.getUniverseRegions(datasource.toString(), null));
						regionIds = regions;
						return Flowable.fromIterable(regions);
					});
				})
				.observeOn(VirtualThreads.SCHEDULER);
	}

	public Maybe<GetUniverseRegionsRegionIdOk> getRegion(int regionId) {
		return getFromCacheOrFetch("region", GetUniverseRegionsRegionIdOk.class, regions, regionId, () -> {
			return VirtualThreads.offload(
					() -> universeApi.getUniverseRegionsRegionId(regionId, null, datasource.toString(), null, null));
		});
	}

	public Flowable<GetUniverseRegionsRegionIdOk> getAllRegions() {
		return getRegionIds()
				.parallel(8)
				.runOn(VirtualThreads.SCHEDULER)
				.flatMap(regionId -> getRegion(regionId).toFlowable())
				.sequential();
	}

	public Maybe<GetUniverseConstellationsConstellationIdOk> getConstellation(int constellationId) {
		return getFromCacheOrFetch(
				"constellation",
				GetUniverseConstellationsConstellationIdOk.class,
				constellations,
				constellationId,
				() -> {
					return VirtualThreads.offload(() -> universeApi.getUniverseConstellationsConstellationId(
							constellationId, null, datasource.toString(), null, null));
				});
	}

	public Maybe<GetUniverseSystemsSystemIdOk> getSystem(int systemId) {
		return getFromCacheOrFetch("system", GetUniverseSystemsSystemIdOk.class, systems, systemId, () -> {
			return VirtualThreads.offload(
					() -> universeApi.getUniverseSystemsSystemId(systemId, null, datasource.toString(), null, null));
		});
	}

	public Maybe<GetUniverseStationsStationIdOk> getNpcStation(long stationId) {
		if (stationId > NPC_STATION_MAX_ID) {
			log.trace(String.format("Ignoring request for non-NPC station %s", stationId));
			return Maybe.empty();
		}
		var intId = (int) stationId;
		return getFromCacheOrFetch("station", GetUniverseStationsStationIdOk.class, stations, intId, () -> {
			return VirtualThreads.offload(
					() -> universeApi.getUniverseStationsStationId(intId, datasource.toString(), null));
		});
	}

	public Maybe<GetUniverseTypesTypeIdOk> getType(int typeId) {
		return getFromCacheOrFetch("type", GetUniverseTypesTypeIdOk.class, types, typeId, () -> {
			return VirtualThreads.offload(
					() -> universeApi.getUniverseTypesTypeId(typeId, null, datasource.toString(), null, null));
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
						if (cache.containsKey(id)) {
							return Maybe.fromOptional(cache.get(id));
						}
						log.trace("Fetching {} {}", name, id);
						var obj = fetcher.get();
						var optional = Optional.ofNullable(obj);
						cache.put(id, optional);
						return Maybe.fromOptional(optional);
					})
					.retry(2, e -> {
						log.warn("Retrying {} {}: {}", name, id, ExceptionUtils.getRootCauseMessage(e));
						return true;
					})
					.observeOn(VirtualThreads.SCHEDULER)
					.onErrorResumeNext(e ->
							Maybe.error(new RuntimeException(String.format("Failed fetching %s %s", name, id), e)));
		});
	}
}
