package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Loads and parses files from the data site.
 */
@Log4j2
public class MarketHistoryLoader {
	protected final CsvMapper csvMapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	private final CsvSchema schema;

	@Inject
	protected MarketHistoryLoader(CsvMapper csvMapper) {
		this.csvMapper = csvMapper.copy().configure(CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS, false);
		schema = this.csvMapper
				.schemaFor(MarketHistoryEntry.class)
				.withHeader()
				.withStrictHeaders(true)
				.withColumnReordering(true);
	}

	public Flowable<Pair<LocalDate, List<JsonNode>>> loadDailyFile(DataUrl url, LocalDate date) {
		return downloadFile(url).flatMapPublisher(file -> {
			file.deleteOnExit();
			return parseDailyFile(file, date).doFinally(() -> file.delete());
		});
	}

	private Single<File> downloadFile(DataUrl url) {
		return Single.defer(() -> {
			var file = tempFiles
					.tempFile("market-history", "-" + FilenameUtils.getName(url.getPath()))
					.toFile();
			return okHttpHelper
					.download(url.toString(), file, okHttpClient)
					.flatMap(response -> {
						if (response.code() != 200) {
							return Single.error(new RuntimeException(
									"Failed to download " + url + " with code " + response.code()));
						}
						return Single.just(file);
					})
					.toFlowable()
					.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(2), e -> {
						log.warn("Retrying download of download {}: {}", url, e.getMessage());
						return true;
					}))
					.firstElement()
					.toSingle();
		});
	}

	private Flowable<Pair<LocalDate, List<JsonNode>>> parseDailyFile(File file, LocalDate date) {
		return Flowable.defer(() -> VirtualThreads.offload(() -> {
			log.trace("Reading market history file: {}", file);
			List<JsonNode> list;
			try (var in = CompressUtil.uncompress(file)) {
				list = readCsvEntries(in);
			}
			log.trace("Read {} entries from {}", list.size(), file);
			return Flowable.just(Pair.of(date, list));
		}));
	}

	@NotNull
	private List<JsonNode> readCsvEntries(InputStream in) throws IOException {
		MappingIterator<JsonNode> iterator =
				csvMapper.readerFor(JsonNode.class).with(schema).readValues(in);
		var list = new ArrayList<JsonNode>();
		iterator.forEachRemaining(list::add);
		iterator.close();
		return list;
	}
}
