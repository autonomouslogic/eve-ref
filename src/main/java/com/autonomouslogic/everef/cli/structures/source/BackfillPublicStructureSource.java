package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_SEEN_PUBLIC_STRUCTURE;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_STRUCTURE_GET;

import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.List;
import javax.inject.Inject;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

/**
 * Backfill importer for a list of public structure IDs.
 */
@Log4j2
@Deprecated
public class BackfillPublicStructureSource implements StructureSource {
	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Setter
	private Instant timestamp;

	@Inject
	protected BackfillPublicStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
					List<Long> ids;
					try (var in = new FileInputStream("/tmp/structure-ids2.txt")) {
						ids = IOUtils.readLines(in).stream()
								.filter(line -> !line.isBlank())
								.filter(line -> !line.contains("rows affected"))
								.filter(line -> !line.contains("structureID"))
								.filter(line -> !line.contains("----"))
								.map(Long::parseLong)
								.toList();
					}
					log.debug("Found {} public structure ids from backfill", ids.size());
					return Flowable.fromIterable(ids)
							.observeOn(Schedulers.computation())
							.doOnNext(id -> {
								structureStore.getOrInitStructure(id);
								structureStore.updateTimestamp(id, LAST_STRUCTURE_GET, Instant.EPOCH);
								structureStore.updateTimestamp(id, LAST_SEEN_PUBLIC_STRUCTURE, Instant.EPOCH);
							});
				})
				.compose(Rx.offloadFlowable());
	}
}
