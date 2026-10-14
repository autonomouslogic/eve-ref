package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.JsonNodeCsvWriter;
import com.autonomouslogic.everef.util.TempFiles;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

@Slf4j
public class MarketOrdersWriter {
	@Inject
	protected TempFiles tempFiles;

	@Setter
	private Map<Long, JsonNode> marketOrdersStore;

	@Setter
	private Map<Long, OrderSortKey> sortKeysStore;

	@Inject
	protected MarketOrdersWriter() {}

	public File writeOrders() {
		var ids = prepareSortedIds();
		log.info(String.format("Writing %s market orders.", marketOrdersStore.size()));
		var start = Instant.now();
		var csv = tempFiles.tempFile("market-orders", ".csv").toFile();
		var iterable = Iterables.transform(ids, id -> marketOrdersStore.get(id));
		new JsonNodeCsvWriter().setOut(csv).writeAll(iterable);
		var compressed = CompressUtil.compressBzip2(csv);
		compressed.deleteOnExit();
		var time = Duration.between(start, Instant.now());
		log.info(String.format("Market orders written in %s", time));
		return compressed;
	}

	/**
	 * Sorts purely off the in-memory {@link #sortKeysStore}, avoiding deserializing the full
	 * orders (held in an MVStore) just to compare them.
	 */
	private List<Long> prepareSortedIds() {
		log.debug("Preparing sorted IDs.");
		var start = Instant.now();

		var ids = new ArrayList<>(sortKeysStore.keySet());
		ids.sort(Ordering.compound(List.of(
				ordering(OrderSortKey::regionId),
				ordering(OrderSortKey::typeId),
				ordering(OrderSortKey::isBuyOrder),
				ordering(OrderSortKey::systemId),
				ordering(OrderSortKey::orderId))));

		var time = Duration.between(start, Instant.now());
		log.info(String.format("Sorted IDs prepared in %s", time));
		return ids;
	}

	private Ordering<Long> ordering(Function<OrderSortKey, Long> extractor) {
		return Ordering.natural().nullsLast().onResultOf(id -> extractor.apply(sortKeysStore.get(id)));
	}
}
