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
 * Computes the list of constellation IDs belonging to each region, since the SDE's own list is dropped as redundant
 * with {@code Constellation.region_id}.
 */
@Log4j2
public class RegionConstellationIdsDecorator extends PostDecorator {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected RegionConstellationIdsDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating constellation IDs on regions");
			var constellations = storeHandler.getRefStore("constellations");
			var regions = storeHandler.getRefStore("regions");
			Map<Long, TreeSet<Long>> constellationsByRegion = new HashMap<>();
			for (var entry : constellations.entrySet()) {
				var constellation = (ObjectNode) entry.getValue();
				var regionIdNode = constellation.get("region_id");
				if (regionIdNode == null || regionIdNode.isNull()) {
					continue;
				}
				constellationsByRegion
						.computeIfAbsent(regionIdNode.asLong(), ignore -> new TreeSet<>())
						.add(entry.getKey());
			}
			for (var entry : regions.entrySet()) {
				var region = (ObjectNode) entry.getValue();
				var constellationIds = constellationsByRegion.getOrDefault(entry.getKey(), new TreeSet<>());
				var array = jsonMapper.createArrayNode();
				for (var constellationId : constellationIds) {
					array.add(constellationId);
				}
				region.set("constellation_ids", (ArrayNode) array);
				regions.put(entry.getKey(), region);
			}
		});
	}
}
