package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.ids.IdRanges;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Populates ore variations on types.
 */
@Log4j2
public class UniverseIdDecorator extends PostDecorator {
	@Inject
	protected ObjectMapper objectMapper;

	private Map<Long, JsonNode> regions;

	@Inject
	protected UniverseIdDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Populating universe IDs on regions");
			regions = storeHandler.getRefStore("regions");
			for (var entry : regions.entrySet()) {
				long id = entry.getKey();
				var region = (ObjectNode) entry.getValue();
				var range = IdRanges.REGION_IDS.forId(id);
				if (range != null) {
					region.put("universe_id", range.getName());
					regions.put(id, region);
				}
			}
		});
	}
}
