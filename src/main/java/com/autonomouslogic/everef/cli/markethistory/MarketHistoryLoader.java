package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

/**
 * Loads and parses files from the data site.
 */
@Log4j2
public class MarketHistoryLoader {
	@Inject
	protected CsvMapper csvMapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected MarketHistoryLoader() {}

	public Flowable<JsonNode> loadDailyFile(DataUrl url) {
		return downloadFile(url).flatMapPublisher(file -> {
			file.deleteOnExit();
			return parseDailyFile(file, LocalDate.EPOCH).doFinally(() -> file.delete());
		});
	}

	private Single<File> downloadFile(DataUrl url) {
		return Single.defer(() -> {
			var file = tempFiles
					.tempFile("market-history", ArchivePathFactory.MARKET_HISTORY.getSuffix())
					.toFile();
			return okHttpHelper.download(url.toString(), file, okHttpClient).flatMap(response -> {
				if (response.code() != 200) {
					return Single.error(
							new RuntimeException("Failed to download " + url + " with code " + response.code()));
				}
				return Single.just(file);
			});
		});
	}

	private Flowable<JsonNode> parseDailyFile(File file, LocalDate date) {
		return Flowable.defer(() -> {
					log.trace("Reading market history file for {}: {}", date, file);
					var in = CompressUtil.uncompress(file);
					var schema = csvMapper
							.schemaFor(MarketHistoryEntry.class)
							.withHeader()
							.withStrictHeaders(true)
							.withColumnReordering(true);
					MappingIterator<JsonNode> iterator =
							csvMapper.readerFor(JsonNode.class).with(schema).readValues(in);
					var list = new ArrayList<JsonNode>();
					iterator.forEachRemaining(list::add);
					iterator.close();
					log.trace("Read {} entries for {} from {}", list.size(), date, file);
					return Flowable.fromIterable(list);
				})
				.compose(Rx.offloadFlowable());
	}
}
