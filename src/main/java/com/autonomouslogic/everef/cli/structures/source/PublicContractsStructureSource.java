package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.util.EveConstants.NPC_STATION_MAX_ID;

import com.autonomouslogic.everef.cli.structures.StructureScrapeHelper;
import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.openapi.esi.api.UniverseApi;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.JsonNodeCsvReader;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.Rx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
public class PublicContractsStructureSource implements StructureSource {
	@Inject
	protected UniverseApi universeApi;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected StructureScrapeHelper structureScrapeHelper;

	@Inject
	protected DataUtil dataUtil;

	@Inject
	protected Provider<JsonNodeCsvReader> jsonNodeCsvReaderProvider;

	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Inject
	protected PublicContractsStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
			log.info("Loading structures from public contracts");
			return download().flatMapPublisher(this::process);
		});
	}

	private Maybe<File> download() {
		return dataUtil.downloadLatestPublicContracts().toMaybe().onErrorResumeNext(e -> {
			log.warn("Failed to download public contracts, ignoring: {}", e.getMessage());
			return Maybe.empty();
		});
	}

	private Flowable<Long> process(@NonNull File file) {
		return Flowable.defer(() -> CompressUtil.loadArchive(file)
						.compose(Rx.offloadFlowable())
						.filter(entry -> entry.getKey().getName().equals("contracts.csv"))
						.flatMap(entry -> Flowable.fromStream(
								jsonNodeCsvReaderProvider.get().readAll(entry.getValue())))
						.flatMap(contract ->
								Flowable.concat(handleEndLocation(contract), handleStartLocation(contract))))
				.distinct()
				.toList()
				.flatMapPublisher(ids -> {
					log.info("Fetched {} structure ids from public contracts", ids.size());
					log.trace("Seen structure IDs: {}", ids);
					return Flowable.fromIterable(ids);
				});
	}

	private @NotNull Flowable<Long> handleStartLocation(@NonNull JsonNode contract) {
		return Flowable.just(
						JsonUtil.getNonBlankLongField(contract, "station_id"),
						JsonUtil.getNonBlankLongField(contract, "start_location_id"))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(id -> id >= NPC_STATION_MAX_ID)
				.distinct()
				.flatMap(id -> {
					var structure = structureStore.getOrInitStructure(id);
					JsonUtil.getNonBlankLongField(contract, "region_id")
							.ifPresent(regionId -> structure.put("region_id", regionId));
					JsonUtil.getNonBlankLongField(contract, "constellation_id")
							.ifPresent(regionId -> structure.put("constellation_id", regionId));
					JsonUtil.getNonBlankLongField(contract, "system_id")
							.ifPresent(regionId -> structure.put("solar_system_id", regionId));
					structureStore.put(structure);
					return Flowable.just(id);
				});
	}

	private @NotNull Flowable<Long> handleEndLocation(@NonNull JsonNode contract) {
		return Flowable.just(JsonUtil.getNonBlankLongField(contract, "end_location_id"))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(id -> id >= NPC_STATION_MAX_ID)
				.distinct()
				.flatMap(id -> {
					structureStore.getOrInitStructure(id);
					return Flowable.just(id);
				});
	}
}
