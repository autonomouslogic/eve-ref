package com.autonomouslogic.everef.esi;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

/**
 * Populates location data on scraped data.
 * Resolves locations and populates the system, constellation, and region.
 */
@Singleton
@Log4j2
public class LocationPopulator {
	@Inject
	protected UniverseEsi universeEsi;

	@Inject
	protected LocationPopulator() {}

	//	public Completable populateStation(ObjectNode record, String locationKey) {
	//		return Completable.defer(() -> {
	//			if (!record.has(locationKey)) {
	//				return Completable.complete();
	//			}
	//			var locationId = record.get(locationKey).asInt(-1);
	//			if (locationId < 0) {
	//				return Completable.complete();
	//			}
	//			return populateStation(record, locationId);
	//		});
	//	}

	//	public Completable populateStation(ObjectNode record, int stationId) {
	//		return universeEsi.getNpcStation(stationId).flatMapCompletable(station -> populateStation(record, station));
	//	}

	//	public Completable populateStation(ObjectNode record, GetUniverseStationsStationIdOk station) {
	//		return Completable.defer(() -> {
	//			if (!record.has("station_id")) {
	//				record.put("station_id", station.getStationId());
	//			}
	//			return populateSystem(record, station.getSystemId());
	//		});
	//	}

	//	public Completable populateSystem(ObjectNode record, int systemId) {
	//		return universeEsi.getNpcStation(stationId).flatMapCompletable(station -> populateStation(record, station));
	//	}

	//	public Completable populateSystem(ObjectNode record, GetUniverseSystemsSystemIdOk system) {
	//		return Completable.defer(() -> {
	//			if (!record.has("system_id")) {
	//				record.put("station_id", station.getStationId());
	//			}
	//			return populateSystem(record, station.getSystemId());
	//		});
	//	}

	//	public void populateSystem(ObjectNode record) {
	//		if (!record.has("system_id")) {
	//			return;
	//		}
	//		SolarSystem solarSystem = universeDao.getSystem(record.get("system_id").asLong(-1));
	//		if (solarSystem != null) {
	//			Constellation constellation = solarSystem.getConstellation().get();
	//			Region region = constellation.getRegion().get();
	//			Universe universe = region.getUniverse().get();
	//			record.put("system_id", solarSystem.getSystemId());
	//			if (!record.has("constellation_id")) {
	//				record.put("constellation_id", constellation.getConstellationId());
	//			}
	//			if (!record.has("region_id")) {
	//				record.put("region_id", region.getRegionId());
	//			}
	//			if (!record.has("universe_id")) {
	//				record.put("universe_id", universe.getUniverseId());
	//			}
	//		}
	//	}
}
