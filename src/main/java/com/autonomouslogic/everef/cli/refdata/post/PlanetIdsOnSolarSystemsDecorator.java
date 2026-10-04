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
 * Computes the list of planet IDs belonging to each solar system, since the SDE's own list is dropped as
 * redundant with {@code Planet.solar_system_id}.
 */
@Log4j2
public class PlanetIdsOnSolarSystemsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected PlanetIdsOnSolarSystemsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating planet IDs on solar systems");
			var planets = storeHandler.getRefStore("planets");
			var solarSystems = storeHandler.getRefStore("solarSystems");
			Map<Long, TreeSet<Long>> planetsBySolarSystem = new HashMap<>();
			for (var entry : planets.entrySet()) {
				var planet = (ObjectNode) entry.getValue();
				var solarSystemIdNode = planet.get("solar_system_id");
				if (solarSystemIdNode == null || solarSystemIdNode.isNull()) {
					continue;
				}
				planetsBySolarSystem
						.computeIfAbsent(solarSystemIdNode.asLong(), ignore -> new TreeSet<>())
						.add(entry.getKey());
			}
			for (var entry : solarSystems.entrySet()) {
				var solarSystem = (ObjectNode) entry.getValue();
				var planetIds = planetsBySolarSystem.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var planetId : planetIds) {
					array.add(planetId);
				}
				solarSystem.set("planet_ids", (ArrayNode) array);
				solarSystems.put(entry.getKey(), solarSystem);
			}
		});
	}
}
