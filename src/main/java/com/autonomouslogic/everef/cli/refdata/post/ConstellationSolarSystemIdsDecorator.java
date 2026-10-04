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
 * Computes the list of solar system IDs belonging to each constellation, since the SDE's own list is dropped as
 * redundant with {@code SolarSystem.constellation_id}.
 */
@Log4j2
public class ConstellationSolarSystemIdsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected ConstellationSolarSystemIdsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating solar system IDs on constellations");
			var solarSystems = storeHandler.getRefStore("solarSystems");
			var constellations = storeHandler.getRefStore("constellations");
			Map<Long, TreeSet<Long>> solarSystemsByConstellation = new HashMap<>();
			for (var entry : solarSystems.entrySet()) {
				var solarSystem = (ObjectNode) entry.getValue();
				var constellationIdNode = solarSystem.get("constellation_id");
				if (constellationIdNode == null || constellationIdNode.isNull()) {
					continue;
				}
				solarSystemsByConstellation
						.computeIfAbsent(constellationIdNode.asLong(), ignore -> new TreeSet<>())
						.add(entry.getKey());
			}
			for (var entry : constellations.entrySet()) {
				var constellation = (ObjectNode) entry.getValue();
				var solarSystemIds = solarSystemsByConstellation.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var solarSystemId : solarSystemIds) {
					array.add(solarSystemId);
				}
				constellation.set("solar_system_ids", (ArrayNode) array);
				constellations.put(entry.getKey(), constellation);
			}
		});
	}
}
