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
 * Computes the list of region IDs each region is directly connected to via stargates in its solar systems.
 */
@Log4j2
public class StargatesToRegionIdsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected StargatesToRegionIdsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating stargate connected region IDs on regions");
			var stargates = storeHandler.getRefStore("stargates");
			var solarSystems = storeHandler.getRefStore("solarSystems");
			var regions = storeHandler.getRefStore("regions");

			Map<Long, Long> regionIdBySolarSystem = new HashMap<>();
			for (var entry : solarSystems.entrySet()) {
				var solarSystem = (ObjectNode) entry.getValue();
				var regionIdNode = solarSystem.get("region_id");
				if (regionIdNode == null || regionIdNode.isNull()) {
					continue;
				}
				regionIdBySolarSystem.put(entry.getKey(), regionIdNode.asLong());
			}

			Map<Long, TreeSet<Long>> connectedRegionsByRegion = new HashMap<>();
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

				var sourceRegionId = regionIdBySolarSystem.get(solarSystemIdNode.asLong());
				var destinationRegionId = regionIdBySolarSystem.get(destinationSystemIdNode.asLong());
				if (sourceRegionId == null
						|| destinationRegionId == null
						|| sourceRegionId.equals(destinationRegionId)) {
					continue;
				}

				connectedRegionsByRegion
						.computeIfAbsent(sourceRegionId, ignore -> new TreeSet<>())
						.add(destinationRegionId);
			}

			for (var entry : regions.entrySet()) {
				var region = (ObjectNode) entry.getValue();
				var connectedRegionIds = connectedRegionsByRegion.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var connectedRegionId : connectedRegionIds) {
					array.add(connectedRegionId);
				}
				region.set("stargates_to_region_ids", (ArrayNode) array);
				regions.put(entry.getKey(), region);
			}
		});
	}
}
