package com.autonomouslogic.everef.cli.structures;

import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.JsonNodeCsvReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MarketOrdersStructureSource implements StructureSource {
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

	@Setter
	private Instant timestamp;

	@Inject
	protected MarketOrdersStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return dataUtil.downloadLatestMarketOrders()
				.toMaybe()
				.onErrorResumeNext(e -> {
					log.warn("Failed to download market orders, ignoring: {}", e.getMessage());
					return Maybe.empty();
				})
				.flatMapPublisher(file -> {
					return jsonNodeCsvReaderProvider
							.get()
							.readCompressed(file)
							.flatMap(node -> Flowable.just(
									Optional.ofNullable(node.get("station_id")),
									Optional.ofNullable(node.get("location_id"))))
							.filter(Optional::isPresent)
							.map(Optional::get)
							.filter(node -> !node.isNull())
							.map(JsonNode::asLong)
							.filter(id -> id >= 70_000_000L)
							.distinct()
							.doOnNext(id -> {
								structureStore.getOrInitStructure(id);
							})
							.toList()
							.flatMapPublisher(ids -> {
								log.debug("Fetched {} market structure ids", ids.size());
								return Flowable.fromIterable(ids);
							});
				});
	}
}
