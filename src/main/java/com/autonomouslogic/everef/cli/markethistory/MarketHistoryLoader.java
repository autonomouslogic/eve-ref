package com.autonomouslogic.everef.cli.markethistory;

import static java.time.ZoneOffset.UTC;

import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Loads market history from the data site.
 */
@Log4j2
class MarketHistoryLoader {
	private static final int DOWNLOAD_CONCURRENCY = 32;

	@Inject
	protected DataCrawler dataCrawler;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected CsvMapper csvMapper;

	@Setter
	private LocalDate minDate;

	@Inject
	protected MarketHistoryLoader() {}

	@Inject
	protected void init() {
		dataCrawler.setPrefix("/" + ArchivePathFactory.MARKET_HISTORY.getFolder() + "/");
	}

	public Flowable<Pair<LocalDate, MarketHistoryEntry>> load() {
		log.info("Loading market history - minDate: {}", minDate);
		var f = crawlFiles();
		if (minDate != null) {
			f = f.filter(p -> !p.getLeft().isBefore(minDate));
		}
		return f.flatMap(
				p -> {
					return downloadFile(p.getRight()).flatMapPublisher(file -> {
						return parseFile(file).map(entry -> {
							return Pair.of(p.getLeft(), entry);
						});
					});
				},
				false,
				DOWNLOAD_CONCURRENCY);
	}

	private Flowable<Pair<LocalDate, DataUrl>> crawlFiles() {
		return dataCrawler.crawl().flatMap(url -> {
			var time = ArchivePathFactory.MARKET_HISTORY.parseArchiveTime(url.getPath());
			if (time == null) {
				return Flowable.empty();
			}
			log.trace("Found market history file: {}", url);
			return Flowable.just(Pair.of(time.atZone(UTC).toLocalDate(), url));
		});
	}

	private Single<File> downloadFile(DataUrl url) {
		return Single.defer(() -> {
			log.debug("Downloading market history file: {}", url);
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

	private Flowable<MarketHistoryEntry> parseFile(File file) {
		return Flowable.defer(() -> {
					log.trace("Reading market history file: {}", file);
					var in = CompressUtil.uncompress(file);
					var schema = csvMapper
							.schemaFor(MarketHistoryEntry.class)
							.withHeader()
							.withStrictHeaders(true)
							.withColumnReordering(true);
					MappingIterator<MarketHistoryEntry> iterator = csvMapper
							.readerFor(MarketHistoryEntry.class)
							.with(schema)
							.readValues(in);
					var list = new ArrayList<MarketHistoryEntry>();
					iterator.forEachRemaining(list::add);
					iterator.close();
					log.trace("Read {} entries from: {}", list.size(), file);
					return Flowable.fromIterable(list);
				})
				.compose(Rx.offloadFlowable());
	}
}
