package com.autonomouslogic.everef.cli.structures;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Map;
import javax.inject.Inject;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

/**
 * Returns the structure IDs from the old scrape.
 */
@Log4j2
public class OldStructureSource implements StructureSource {
	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Setter
	private Map<String, ObjectNode> previousScrape;

	@Inject
	protected OldStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
			if (previousScrape == null) {
				return Flowable.empty();
			}
			var ids = previousScrape.keySet().stream().map(Long::parseLong).toList();
			log.debug("Fetched {} previous structure ids", ids.size());
			return Flowable.fromIterable(ids);
		});
	}
}
