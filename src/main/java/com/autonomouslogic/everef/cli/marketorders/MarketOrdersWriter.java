package com.autonomouslogic.everef.cli.marketorders;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.h2.mvstore.MVMap;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class MarketOrdersWriter {
	@Setter
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrdersWriter() {
	}

	public Single<File> writeOrders() {
		return prepareSortedIds()
			.flatMap(ids -> Single.fromCallable(() -> {
				log.info(String.format("Preparing to write %s market orders.", marketOrdersStore.size()));
				Instant start = Instant.now();
				File csv = Files.createTempFile(getClass().getSimpleName() + "_", ".csv").toFile();
				csv.deleteOnExit();
				Iterable<JsonNode> iterable = Iterables.transform(ids, id -> marketOrdersStore.get(id));
				new JsonNodeCsvBuilder()
					.setOut(csv)
					.writeAll(iterable);
				File compressed = new File(csv.getPath() + ".bz2");
				compressed.deleteOnExit();
				OutputStream out = new BZip2CompressorOutputStream(new FileOutputStream(compressed));
				IOUtils.copy(new FileInputStream(csv), out);
				out.close();
				Duration time = Duration.between(start, Instant.now());
				log.info(String.format("Market orders written in %s", time));
				return compressed;
			})
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
		);
	}

	private Ordering<Long> ordering(String field) {
		return Ordering.natural().onResultOf(id -> marketOrdersStore.get(id).get(field).asLong());
	}

	private Single<List<Long>> prepareSortedIds() {
		return Single.fromCallable(() -> {
			log.debug(String.format("Preparing sorted IDs."));
			Instant start = Instant.now();

			List<Long> ids = new ArrayList<>(marketOrdersStore.keyList());
			ids.sort(Ordering.compound(Arrays.asList(
				ordering("region_id"),
				ordering("type_id"),
				ordering("is_buy_order"),
				ordering("system_id"),
				ordering("order_id")
			)));

			Duration time = Duration.between(start, Instant.now());
			log.info(String.format("Sorted IDs prepared in %s", time));
			return ids;
		})
			.subscribeOn(Schedulers.io())
			.observeOn(Schedulers.computation());
	}
}
