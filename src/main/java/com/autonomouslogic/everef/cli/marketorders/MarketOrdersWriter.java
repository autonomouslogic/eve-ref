package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.everef.util.JsonNodeCsvWriter;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.h2.mvstore.MVMap;

@Slf4j
public class MarketOrdersWriter {
	@Inject
	protected TempFiles tempFiles;

	@Setter
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrdersWriter() {}

	public Single<File> writeOrders() {
		return prepareSortedIds().flatMap(ids -> Single.fromCallable(() -> {
					log.info(String.format("Preparing to write %s market orders.", marketOrdersStore.size()));
					var start = Instant.now();
					var csv = tempFiles.tempFile("market-orders", ".csv").toFile();
					var iterable = Iterables.transform(ids, id -> marketOrdersStore.get(id));
					new JsonNodeCsvWriter().setOut(csv).writeAll(iterable);
					File compressed = new File(csv.getPath() + ".bz2");
					//compressed.deleteOnExit();
					log.info(String.format("Compressing orders to %s", compressed.getPath()));
					OutputStream out = new BZip2CompressorOutputStream(new FileOutputStream(compressed));
					IOUtils.copy(new FileInputStream(csv), out);
					out.close();
					Duration time = Duration.between(start, Instant.now());
					log.info(String.format("Market orders written in %s", time));
					return compressed;
				})
				.compose(Rx.offloadSingle()));
	}

	private Single<List<Long>> prepareSortedIds() {
		return Single.fromCallable(() -> {
					log.debug("Preparing sorted IDs.");
					var start = Instant.now();

					var ids = new ArrayList<>(marketOrdersStore.keyList());
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
	}
}
