package com.autonomouslogic.everef.esi;

import static com.autonomouslogic.everef.util.EveConstants.NPC_STATION_MAX_ID;

import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
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

	public Completable populate(ObjectNode record) {
		return populate(record, "station_id");
	}

	public Completable populate(ObjectNode record, String stationKey) {
		return Completable.concatArray(
				populateStation(record, stationKey),
				populateSystem(record),
				populateConstellation(record),
				populateRegion(record));
	}

	private Completable populateStation(ObjectNode record, String stationKey) {
		return Completable.fromAction(() -> {
			if (record.has("station_id")) {
				return;
			}
			var stationId = record.get(stationKey);
			if (!JsonUtil.isNullOrEmpty(stationId) && stationId.asLong() < NPC_STATION_MAX_ID) {
				record.set("station_id", stationId);
			}
		});
	}

	private Completable populateSystem(ObjectNode record) {
		return Completable.defer(() -> {
			if (record.has("system_id")) {
				return Completable.complete();
			}
			var stationId = record.get("station_id");
			if (JsonUtil.isNullOrEmpty(stationId)) {
				return Completable.complete();
			}
			var station = universeEsi.getNpcStation(stationId.asLong());
			if (station.isPresent()) {
				record.put("system_id", station.get().getSystemId());
			}
			return Completable.complete();
		});
	}

	private Completable populateConstellation(ObjectNode record) {
		return Completable.defer(() -> {
			if (record.has("constellation_id")) {
				return Completable.complete();
			}
			var systemId = record.get("system_id");
			if (systemId == null) {
				systemId = record.get("solar_system_id");
			}
			if (JsonUtil.isNullOrEmpty(systemId)) {
				return Completable.complete();
			}
			var system = universeEsi.getSystem(systemId.asInt());
			if (system.isPresent()) {
				record.put("constellation_id", system.get().getConstellationId());
			}
			return Completable.complete();
		});
	}

	private Completable populateRegion(ObjectNode record) {
		return Completable.defer(() -> {
			if (record.has("region_id")) {
				return Completable.complete();
			}
			var constellationId = record.get("constellation_id");
			if (JsonUtil.isNullOrEmpty(constellationId)) {
				return Completable.complete();
			}
			var constellation = universeEsi.getConstellation(constellationId.asInt());
			if (constellation.isPresent()) {
				record.put("region_id", constellation.get().getRegionId());
			}
			return Completable.complete();
		});
	}
}
