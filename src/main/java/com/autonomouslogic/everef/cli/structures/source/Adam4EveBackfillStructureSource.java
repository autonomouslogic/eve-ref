package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.FIRST_SEEN;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_SEEN_PUBLIC_STRUCTURE;
import static com.autonomouslogic.everef.cli.structures.ScrapeStructures.LAST_STRUCTURE_GET;

import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import javax.inject.Inject;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * Backfill importer for a list of public structure IDs.
 */
@Log4j2
@Deprecated
public class Adam4EveBackfillStructureSource implements StructureSource {
	private static final String SOURCE_URL = "https://static.adam4eve.eu/IDs/playerStructure_extended.csv";

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected ObjectMapper objectMapper;

	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Setter
	private Instant timestamp;

	private final Duration structureTimeout = Configs.STRUCTURE_TIMEOUT.getRequired();

	@Inject
	protected Adam4EveBackfillStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return download()
				.flatMapPublisher(this::parse)
				.toList()
				.flatMapPublisher(records -> {
					log.debug("Examining {} structures from Adam4Eve backfill", records.size());
					return Flowable.fromIterable(records);
				})
				.flatMap(record -> {
					var structureId = Long.parseLong(record.get("structureID"));
					var typeId = Optional.ofNullable(record.get("typeID"))
							.filter(s -> !s.isBlank())
							.map(Long::parseLong)
							.orElse(null);
					var solarSystemId = Optional.ofNullable(record.get("solarSystemID"))
							.filter(s -> !s.isBlank())
							.map(Long::parseLong)
							.orElse(null);
					var regionId = Optional.ofNullable(record.get("regionID"))
							.filter(s -> !s.isBlank())
							.map(Long::parseLong)
							.orElse(null);
					var name = record.get("name");
					var ownerId = Optional.ofNullable(record.get("ownerID"))
							.filter(s -> !s.isBlank())
							.map(Long::parseLong)
							.orElse(null);
					var firstSeen = Optional.ofNullable(record.get("firstSeen"))
							.filter(s -> !s.isBlank())
							.map(LocalDate::parse)
							.map(d -> d.atStartOfDay(ZoneOffset.UTC).toInstant())
							.orElse(null);
					var lastSeen = Optional.ofNullable(record.get("lastSeen"))
							.filter(s -> !s.isBlank())
							.map(LocalDate::parse)
							.map(d -> d.atStartOfDay(ZoneOffset.UTC).toInstant())
							.orElse(null);
					var node = structureStore.getOrInitStructure(structureId);
					if (!node.has("type_id")) {
						node.put("type_id", typeId);
					}
					if (!node.has("solar_system_id")) {
						node.put("solar_system_id", solarSystemId);
					}
					if (!node.has("region_id")) {
						node.put("region_id", regionId);
					}
					if (!node.has("name")) {
						node.put("name", name);
					}
					if (!node.has("owner_id")) {
						node.put("owner_id", ownerId);
					}
					if (firstSeen != null) {
						if (!node.has(FIRST_SEEN)) {
							node.put(FIRST_SEEN, firstSeen.toString());
						} else {
							var t = Instant.parse(node.get(FIRST_SEEN).asText());
							if (firstSeen.isBefore(t)) {
								node.put(FIRST_SEEN, firstSeen.toString());
							}
						}
					}
					structureStore.put(node);
					structureStore.updateTimestamp(structureId, LAST_STRUCTURE_GET, Instant.EPOCH);
					structureStore.updateTimestamp(structureId, LAST_SEEN_PUBLIC_STRUCTURE, Instant.EPOCH);
					if (lastSeen != null && lastSeen.isBefore(timestamp.minus(structureTimeout))) {
						return Flowable.empty();
					}
					return Flowable.just(structureId);
				})
				.distinct()
				.toList()
				.flatMapPublisher(ids -> {
					log.info("Fetched {} structure ids from Adam4Eve", ids.size());
					log.trace("Seen structure IDs: {}", ids);
					return Flowable.fromIterable(ids);
				});
	}

	private Single<File> download() {
		return Single.fromCallable(() -> {
			var file =
					tempFiles.tempFile("adam4eve-playerStructure_IDs", ".csv").toFile();
			try (var response = okHttpWrapper.download(SOURCE_URL, file)) {
				if (response.code() != 200) {
					throw new RuntimeException();
				}
				return file;
			}
		});
	}

	private Flowable<CSVRecord> parse(File file) {
		return Flowable.defer(() -> {
					var records = new ArrayList<CSVRecord>();
					try (var reader = new FileReader(file, StandardCharsets.UTF_8)) {
						CSVFormat.RFC4180
								.builder()
								.setDelimiter(';')
								.setHeader()
								.setSkipHeaderRecord(true)
								.build()
								.parse(reader)
								.stream()
								.forEach(records::add);
					}
					return Flowable.fromIterable(records);
				})
				.compose(Rx.offloadFlowable());
	}
}
