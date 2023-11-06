package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
	@Inject
	protected CsvMapper csvMapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	private final CsvSchema schema;

	@Inject
	protected MarketHistoryLoader(CsvMapper csvMapper) {
		schema = csvMapper
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

	public Flowable<Pair<LocalDate, List<JsonNode>>> loadYearFile(DataUrl url, Collection<LocalDate> excludedDates) {
		return downloadFile(url).flatMapPublisher(file -> {
			file.deleteOnExit();
			return parseYearFile(file, excludedDates).doFinally(() -> file.delete());
		});
	}

	private Single<File> downloadFile(DataUrl url) {
		return Single.defer(() -> {
			var file = tempFiles
					.tempFile("market-history", "-" + FilenameUtils.getName(url.getPath()))
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

	private Flowable<Pair<LocalDate, List<JsonNode>>> parseDailyFile(File file, LocalDate date) {
		return Flowable.defer(() -> {
					log.trace("Reading market history file: {}", file);
					List<JsonNode> list;
					try (var in = CompressUtil.uncompress(file)) {
						list = readCsvEntries(in);
					}
					log.trace("Read {} entries from {}", list.size(), file);
					return Flowable.just(Pair.of(date, list));
				})
				.compose(Rx.offloadFlowable());
	}

	private Flowable<Pair<LocalDate, List<JsonNode>>> parseYearFile(File file, Collection<LocalDate> excludedDates) {
		return Flowable.defer(() -> {
					log.trace("Reading yearly market history file: {}", file);
					return CompressUtil.loadArchive(file).flatMap(entry -> {
						var date = LocalDate.parse(entry.getLeft().getName().substring(0, 10));
						if (excludedDates != null && excludedDates.contains(date)) {
							return Flowable.empty();
						}
						var list = readCsvEntries(new ByteArrayInputStream(entry.getRight()));
						log.trace(
								"Read {} entries from {}#{}",
								list.size(),
								file,
								entry.getLeft().getName());
						return Flowable.just(Pair.of(date, list));
					});
				})
				.compose(Rx.offloadFlowable());
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
