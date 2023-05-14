package com.autonomouslogic.everef.cli.markethistory;

import static java.time.ZoneOffset.UTC;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.model.MarketHistoryEntry;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
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

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Setter
	private LocalDate minDate;

	@Inject
	protected MarketHistoryLoader() {}

	@Inject
	protected void init() {
		dataCrawler.setPrefix(ArchivePathFactory.MARKET_HISTORY.getFolder() + "/");
	}

	public Flowable<Pair<LocalDate, JsonNode>> load() {
		log.info("Loading market history - minDate: {}", minDate);
		var f = crawlFiles();
		if (minDate != null) {
			f = f.filter(p -> !p.getLeft().isBefore(minDate));
		}
		return f.toList()
				.flatMapPublisher(l -> {
					return Flowable.fromIterable(l);
				})
				.switchIfEmpty(Flowable.error(new RuntimeException("No market history files found.")))
				.flatMap(
						p -> {
							return downloadFile(p.getRight()).flatMapPublisher(file -> {
								file.deleteOnExit();
								return parseFile(file)
										.map(entry -> {
											return Pair.of(p.getLeft(), entry);
										})
										.doFinally(() -> file.delete());
							});
						},
						false,
						DOWNLOAD_CONCURRENCY)
				.switchIfEmpty(Flowable.error(new RuntimeException("No market data found in history files.")));
	}

	private Flowable<Pair<LocalDate, DataUrl>> crawlFiles() {
		return dataCrawler.crawl().flatMap(url -> {
			var path = url.getPath();
			var prefix = dataBaseUrl.getPath();
			if (path.startsWith(prefix)) {
				path = StringUtils.removeStart(path, prefix);
			}
			var time = ArchivePathFactory.MARKET_HISTORY.parseArchiveTime(path);
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

	private Flowable<JsonNode> parseFile(File file) {
		return Flowable.defer(() -> {
					log.trace("Reading market history file: {}", file);
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
					log.trace("Read {} entries from: {}", list.size(), file);
					return Flowable.fromIterable(list);
				})
				.compose(Rx.offloadFlowable());
	}
}
