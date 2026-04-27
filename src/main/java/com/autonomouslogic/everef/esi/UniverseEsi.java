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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
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

	private final Semaphore semaphore = new Semaphore(32);

	@Inject
	protected UniverseEsi() {}

	public List<Integer> getRegionIds() {
		if (regionIds != null) {
			return regionIds;
		}
		log.trace("Fetching region ids");
		regionIds = VirtualThreads.run(() -> universeApi.getUniverseRegions(datasource.toString(), null));
		return regionIds;
	}

	public Optional<GetUniverseRegionsRegionIdOk> getRegion(int regionId) {
		return getFromCacheOrFetch("region", GetUniverseRegionsRegionIdOk.class, regions, regionId, () -> {
			return VirtualThreads.run(
					() -> universeApi.getUniverseRegionsRegionId(regionId, null, datasource.toString(), null, null));
		});
	}

	public List<GetUniverseRegionsRegionIdOk> getAllRegions() {
		return VirtualThreads.parallel(
						getRegionIds().stream()
								.map(id -> (Callable<Optional<GetUniverseRegionsRegionIdOk>>) () -> getRegion(id))
								.toList(),
						8)
				.stream()
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	public Optional<GetUniverseConstellationsConstellationIdOk> getConstellation(int constellationId) {
		return getFromCacheOrFetch(
				"constellation",
				GetUniverseConstellationsConstellationIdOk.class,
				constellations,
				constellationId,
				() -> {
					return VirtualThreads.run(() -> universeApi.getUniverseConstellationsConstellationId(
							constellationId, null, datasource.toString(), null, null));
				});
	}

	public Optional<GetUniverseSystemsSystemIdOk> getSystem(int systemId) {
		return getFromCacheOrFetch("system", GetUniverseSystemsSystemIdOk.class, systems, systemId, () -> {
			return VirtualThreads.run(
					() -> universeApi.getUniverseSystemsSystemId(systemId, null, datasource.toString(), null, null));
		});
	}

	public Optional<GetUniverseStationsStationIdOk> getNpcStation(long stationId) {
		if (stationId > NPC_STATION_MAX_ID) {
			log.trace(String.format("Ignoring request for non-NPC station %s", stationId));
			return Optional.empty();
		}
		var intId = (int) stationId;
		return getFromCacheOrFetch("station", GetUniverseStationsStationIdOk.class, stations, intId, () -> {
			return VirtualThreads.run(
					() -> universeApi.getUniverseStationsStationId(intId, datasource.toString(), null));
		});
	}

	public Optional<GetUniverseTypesTypeIdOk> getType(int typeId) {
		return getFromCacheOrFetch("type", GetUniverseTypesTypeIdOk.class, types, typeId, () -> {
			return VirtualThreads.run(
					() -> universeApi.getUniverseTypesTypeId(typeId, null, datasource.toString(), null, null));
		});
	}

	@NotNull
	private <T> Optional<T> getFromCacheOrFetch(
			String name, Class<T> type, Map<Integer, Optional<T>> cache, int id, Supplier<T> fetcher) {
		if (cache.containsKey(id)) {
			return cache.get(id);
		}
		return fetchWithRetry(name, id, fetcher, cache);
	}

	private <T> Optional<T> fetchWithRetry(
			String name, int id, Supplier<T> fetcher, final Map<Integer, Optional<T>> cache) {
		int maxRetries = 2;
		Exception lastException = null;

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				semaphore.acquire();
				if (cache.containsKey(id)) {
					return cache.get(id);
				}
				log.trace("Fetching {} {}", name, id);
				var obj = fetcher.get();
				var optional = Optional.ofNullable(obj);
				cache.put(id, optional);
				return optional;
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					log.warn("Retrying {} {}: {}", name, id, ExceptionUtils.getRootCauseMessage(e));
				}
			} finally {
				semaphore.release();
			}
		}

		throw new RuntimeException(String.format("Failed fetching %s %s", name, id), lastException);
	}
}
