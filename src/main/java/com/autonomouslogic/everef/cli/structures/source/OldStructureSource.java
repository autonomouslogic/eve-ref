package com.autonomouslogic.everef.cli.structures.source;

import com.autonomouslogic.everef.cli.structures.StructureStore;
import io.reactivex.rxjava3.core.Flowable;
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

	@Inject
	protected OldStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
			var ids = structureStore.getAllIds();
			log.info("Fetched {} previous structure ids", ids.size());
			log.trace("Seen structure IDs: {}", ids);
			return Flowable.fromIterable(ids);
		});
	}
}
