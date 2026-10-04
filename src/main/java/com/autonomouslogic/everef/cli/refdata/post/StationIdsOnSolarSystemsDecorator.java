package com.autonomouslogic.everef.cli.refdata.post;

import io.reactivex.rxjava3.core.Completable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Computes the list of NPC station IDs belonging to each solar system, since the ESI's own list is dropped as
 * redundant with {@code NpcStation.solar_system_id}.
 */
@Log4j2
public class StationIdsOnSolarSystemsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected StationIdsOnSolarSystemsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating station IDs on solar systems");
			var npcStations = storeHandler.getRefStore("npcStations");
			var solarSystems = storeHandler.getRefStore("solarSystems");
			Map<Long, TreeSet<Long>> stationsBySolarSystem = new HashMap<>();
			for (var entry : npcStations.entrySet()) {
				var npcStation = (ObjectNode) entry.getValue();
				var solarSystemIdNode = npcStation.get("solar_system_id");
				if (solarSystemIdNode == null || solarSystemIdNode.isNull()) {
					continue;
				}
				stationsBySolarSystem
						.computeIfAbsent(solarSystemIdNode.asLong(), ignore -> new TreeSet<>())
						.add(entry.getKey());
			}
			for (var entry : solarSystems.entrySet()) {
				var solarSystem = (ObjectNode) entry.getValue();
				var stationIds = stationsBySolarSystem.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var stationId : stationIds) {
					array.add(stationId);
				}
				solarSystem.set("station_ids", (ArrayNode) array);
				solarSystems.put(entry.getKey(), solarSystem);
			}
		});
	}
}
