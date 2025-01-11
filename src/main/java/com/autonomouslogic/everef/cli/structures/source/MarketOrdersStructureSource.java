package com.autonomouslogic.everef.cli.structures.source;

import static com.autonomouslogic.everef.util.EveConstants.NPC_STATION_MAX_ID;

import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.JsonNodeCsvReader;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
public class MarketOrdersStructureSource implements StructureSource {
	@Inject
	protected DataUtil dataUtil;

	@Inject
	protected Provider<JsonNodeCsvReader> jsonNodeCsvReaderProvider;

	@Setter
	@Accessors(chain = false)
	private StructureStore structureStore;

	@Inject
	protected MarketOrdersStructureSource() {}

	@Override
	public Flowable<Long> getStructures() {
		return Flowable.defer(() -> {
			log.info("Loading structures from market orders");
			return download().flatMapPublisher(this::process);
		});
	}

	private Maybe<File> download() {
		return dataUtil.downloadLatestMarketOrders().toMaybe().onErrorResumeNext(e -> {
			log.warn("Failed to download market orders, ignoring: {}", e.getMessage());
			return Maybe.empty();
		});
	}

	private Flowable<Long> process(File file) {
		return Flowable.defer(() -> {
					return jsonNodeCsvReaderProvider
							.get()
							.readCompressed(file)
							.flatMap(node -> Flowable.just(
											JsonUtil.getNonBlankLongField(node, "station_id"),
											JsonUtil.getNonBlankLongField(node, "location_id"))
									.filter(Optional::isPresent)
									.map(Optional::get)
									.filter(id -> id >= NPC_STATION_MAX_ID)
									.distinct()
									.map(id -> Pair.of(id, node)))
							.map(pair -> {
								var id = pair.getKey();
								var market = pair.getValue();
								var structure = structureStore.getOrInitStructure(id);
								JsonUtil.getNonBlankLongField(market, "region_id")
										.ifPresent(regionId -> structure.put("region_id", regionId));
								JsonUtil.getNonBlankLongField(market, "constellation_id")
										.ifPresent(regionId -> structure.put("constellation_id", regionId));
								JsonUtil.getNonBlankLongField(market, "system_id")
										.ifPresent(regionId -> structure.put("solar_system_id", regionId));
								structureStore.put(structure);
								return (Long) id;
							})
							.distinct()
							.toList()
							.flatMapPublisher(ids -> {
								log.info("Fetched {} structure ids from market orders", ids.size());
								log.trace("Seen structure IDs: {}", ids);
								return Flowable.fromIterable(ids);
							});
				})
				.compose(Rx.offloadFlowable());
	}
}
