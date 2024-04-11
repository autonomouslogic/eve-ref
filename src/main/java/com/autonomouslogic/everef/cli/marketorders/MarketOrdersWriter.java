package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.JsonNodeCsvWriter;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketOrdersWriter {
	@Inject
	protected TempFiles tempFiles;

	@Setter
	private Map<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrdersWriter() {}

	public Single<File> writeOrders() {
		return prepareSortedIds().flatMap(ids -> Single.fromCallable(() -> {
					log.info(String.format("Preparing to write %s market orders.", marketOrdersStore.size()));
					var start = Instant.now();
					var csv = tempFiles.tempFile("market-orders", ".csv").toFile();
					var iterable = Iterables.transform(ids, id -> marketOrdersStore.get(id));
					new JsonNodeCsvWriter().setOut(csv).writeAll(iterable);
					var compressed = CompressUtil.compressBzip2(csv);
					compressed.deleteOnExit();
					var time = Duration.between(start, Instant.now());
					log.info(String.format("Market orders written in %s", time));
					return compressed;
				})
				.compose(Rx.offloadSingle()));
	}

	private Single<List<Long>> prepareSortedIds() {
		return Single.fromCallable(() -> {
					log.debug("Preparing sorted IDs.");
					var start = Instant.now();

					var ids = new ArrayList<>(marketOrdersStore.keySet());
					ids.sort(Ordering.compound(List.of(
							ordering("region_id"),
							ordering("type_id"),
							ordering("is_buy_order"),
							ordering("system_id"),
							ordering("order_id"))));

					var time = Duration.between(start, Instant.now());
					log.info(String.format("Sorted IDs prepared in %s", time));
					return ids;
				})
				.compose(Rx.offloadSingle());
	}

	private Ordering<Long> ordering(String field) {
		return Ordering.natural()
				.onResultOf(id -> marketOrdersStore.get(id).get(field).asLong());

//			.onResultOf(id -> Optional.ofNullable(marketOrdersStore.get(id).get(field))
//
//			);
	}
}
