package com.autonomouslogic.everef.esi;

import static com.autonomouslogic.everef.util.EveConstants.NPC_STATION_MAX_ID;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.api.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.model.UniverseConstellationsConstellationIdGet;
import com.autonomouslogic.everef.openapi.esi.model.UniverseRegionsRegionIdGet;
import com.autonomouslogic.everef.openapi.esi.model.UniverseStationsStationIdGet;
import com.autonomouslogic.everef.openapi.esi.model.UniverseSystemsSystemIdGet;
import com.autonomouslogic.everef.openapi.esi.model.UniverseTypesTypeIdGet;
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

	@Inject
	protected EsiHelper esiHelper;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	private List<Long> regionIds;
	private final Map<Long, Optional<UniverseRegionsRegionIdGet>> regions = new ConcurrentHashMap<>();
	private final Map<Long, Optional<UniverseConstellationsConstellationIdGet>> constellations =
			new ConcurrentHashMap<>();
	private final Map<Long, Optional<UniverseSystemsSystemIdGet>> systems = new ConcurrentHashMap<>();
	private final Map<Long, Optional<UniverseStationsStationIdGet>> stations = new ConcurrentHashMap<>();
	private final Map<Long, Optional<UniverseTypesTypeIdGet>> types = new ConcurrentHashMap<>();

	@Inject
	protected UniverseEsi() {}

	public Flowable<Long> getRegionIds() {
		return Flowable.defer(() -> {
					if (regionIds != null) {
						return Flowable.fromIterable(regionIds);
					}
					return Flowable.defer(() -> {
						log.trace("Fetching region ids");
						var regions = VirtualThreads.offload(() -> universeApi.getUniverseRegions(
								esiHelper.getCompatibilityDate(), null, null, datasource));
						regionIds = regions;
						return Flowable.fromIterable(regions);
					});
				})
				.observeOn(VirtualThreads.SCHEDULER);
	}

	public Maybe<UniverseRegionsRegionIdGet> getRegion(long regionId) {
		return getFromCacheOrFetch("region", UniverseRegionsRegionIdGet.class, regions, regionId, () -> {
			return VirtualThreads.offload(() -> universeApi.getUniverseRegionsRegionId(
					regionId, esiHelper.getCompatibilityDate(), null, null, datasource));
		});
	}

	public Flowable<UniverseRegionsRegionIdGet> getAllRegions() {
		return getRegionIds()
				.parallel(8)
				.runOn(VirtualThreads.SCHEDULER)
				.flatMap(regionId -> getRegion(regionId).toFlowable())
				.sequential();
	}

	public Maybe<UniverseConstellationsConstellationIdGet> getConstellation(long constellationId) {
		return getFromCacheOrFetch(
				"constellation",
				UniverseConstellationsConstellationIdGet.class,
				constellations,
				constellationId,
				() -> {
					return VirtualThreads.offload(() -> universeApi.getUniverseConstellationsConstellationId(
							constellationId, esiHelper.getCompatibilityDate(), null, null, datasource));
				});
	}

	public Maybe<UniverseSystemsSystemIdGet> getSystem(long systemId) {
		return getFromCacheOrFetch("system", UniverseSystemsSystemIdGet.class, systems, systemId, () -> {
			return VirtualThreads.offload(() -> universeApi.getUniverseSystemsSystemId(
					systemId, esiHelper.getCompatibilityDate(), null, null, datasource));
		});
	}

	public Maybe<UniverseStationsStationIdGet> getNpcStation(long stationId) {
		if (stationId > NPC_STATION_MAX_ID) {
			log.trace(String.format("Ignoring request for non-NPC station %s", stationId));
			return Maybe.empty();
		}
		return getFromCacheOrFetch("station", UniverseStationsStationIdGet.class, stations, stationId, () -> {
			return VirtualThreads.offload(() -> universeApi.getUniverseStationsStationId(
					stationId, esiHelper.getCompatibilityDate(), null, null, datasource));
		});
	}

	public Maybe<UniverseTypesTypeIdGet> getType(long typeId) {
		return getFromCacheOrFetch("type", UniverseTypesTypeIdGet.class, types, typeId, () -> {
			return VirtualThreads.offload(() -> universeApi.getUniverseTypesTypeId(
					typeId, esiHelper.getCompatibilityDate(), null, null, datasource));
		});
	}

	@NotNull
	private <T> Maybe<T> getFromCacheOrFetch(
			String name, Class<T> type, Map<Long, Optional<T>> cache, long id, Supplier<T> fetcher) {
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
