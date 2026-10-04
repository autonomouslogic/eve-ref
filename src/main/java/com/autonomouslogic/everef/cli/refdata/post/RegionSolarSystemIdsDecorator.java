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
 * Computes the list of solar system IDs belonging to each region, since the SDE's own constellation/system lists
 * are dropped as redundant with {@code Constellation.region_id} / {@code SolarSystem.constellation_id}.
 */
@Log4j2
public class RegionSolarSystemIdsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected RegionSolarSystemIdsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating solar system IDs on regions");
			var solarSystems = storeHandler.getRefStore("solarSystems");
			var regions = storeHandler.getRefStore("regions");
			Map<Long, TreeSet<Long>> solarSystemsByRegion = new HashMap<>();
			for (var entry : solarSystems.entrySet()) {
				var solarSystem = (ObjectNode) entry.getValue();
				var regionIdNode = solarSystem.get("region_id");
				if (regionIdNode == null || regionIdNode.isNull()) {
					continue;
				}
				solarSystemsByRegion
						.computeIfAbsent(regionIdNode.asLong(), ignore -> new TreeSet<>())
						.add(entry.getKey());
			}
			for (var entry : regions.entrySet()) {
				var region = (ObjectNode) entry.getValue();
				var solarSystemIds = solarSystemsByRegion.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var solarSystemId : solarSystemIds) {
					array.add(solarSystemId);
				}
				region.set("solar_system_ids", (ArrayNode) array);
				regions.put(entry.getKey(), region);
			}
		});
	}
}
