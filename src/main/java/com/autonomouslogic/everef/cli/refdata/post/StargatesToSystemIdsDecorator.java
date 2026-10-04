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
 * Computes the list of solar system IDs each solar system is connected to via its stargates.
 */
@Log4j2
public class StargatesToSystemIdsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected StargatesToSystemIdsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating stargate connected system IDs on solar systems");
			var stargates = storeHandler.getRefStore("stargates");
			var solarSystems = storeHandler.getRefStore("solarSystems");
			Map<Long, TreeSet<Long>> connectedSystemsBySolarSystem = new HashMap<>();
			for (var entry : stargates.entrySet()) {
				var stargate = (ObjectNode) entry.getValue();
				var solarSystemIdNode = stargate.get("solar_system_id");
				var destinationNode = (ObjectNode) stargate.get("destination");
				if (solarSystemIdNode == null || solarSystemIdNode.isNull() || destinationNode == null) {
					continue;
				}
				var destinationSystemIdNode = destinationNode.get("solar_system_id");
				if (destinationSystemIdNode == null || destinationSystemIdNode.isNull()) {
					continue;
				}
				connectedSystemsBySolarSystem
						.computeIfAbsent(solarSystemIdNode.asLong(), ignore -> new TreeSet<>())
						.add(destinationSystemIdNode.asLong());
			}
			for (var entry : solarSystems.entrySet()) {
				var solarSystem = (ObjectNode) entry.getValue();
				var connectedSystemIds = connectedSystemsBySolarSystem.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var connectedSystemId : connectedSystemIds) {
					array.add(connectedSystemId);
				}
				solarSystem.set("stargates_to_system_ids", (ArrayNode) array);
				solarSystems.put(entry.getKey(), solarSystem);
			}
		});
	}
}
