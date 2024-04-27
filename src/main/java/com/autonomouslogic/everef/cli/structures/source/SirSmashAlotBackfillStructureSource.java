package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_SEEN_PUBLIC_STRUCTURE;

import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Flowable;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * Backfill importer the backfil provided from Sir SmashAlot.
 */
@Log4j2
@Deprecated
public class SirSmashAlotBackfillStructureSource implements StructureSource {
	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Inject
	protected SirSmashAlotBackfillStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return parse(new File("/tmp/corporationOfficeStructures.csv"))
				.toList()
				.flatMapPublisher(records -> {
					log.debug("Found {} structures from Sir SmashAlot backfill", records.size());
					return Flowable.fromIterable(records);
				})
				.map(record -> {
					var structureId = Long.parseLong(record.get("station_id"));
					var typeId = Long.parseLong(record.get("type_id"));
					var solarSystemId = Long.parseLong(record.get("solar_system_id"));
					var name = record.get("name");
					var ownerId = Long.parseLong(record.get("owner_id"));
					var node = structureStore.getOrInitStructure(structureId);
					if (!node.has("type_id")) {
						node.put("type_id", typeId);
					}
					if (!node.has("solar_system_id")) {
						node.put("solar_system_id", solarSystemId);
					}
					if (!node.has("name")) {
						node.put("name", name);
					}
					if (!node.has("owner_id")) {
						node.put("owner_id", ownerId);
					}
					structureStore.put(node);
					structureStore.updateTimestamp(structureId, LAST_SEEN_PUBLIC_STRUCTURE, Instant.EPOCH);
					return structureId;
				});
	}

	private Flowable<CSVRecord> parse(File file) {
		return Flowable.defer(() -> {
					var records = new ArrayList<CSVRecord>();
					try (var reader = new FileReader(file, StandardCharsets.UTF_8)) {
						CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader).stream()
								.forEach(records::add);
					}
					return Flowable.fromIterable(records);
				})
				.compose(Rx.offloadFlowable());
	}
}
