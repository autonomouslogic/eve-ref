package com.autonomouslogic.everef.cli.markethistory;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_HISTORY;
import static java.time.ZoneOffset.UTC;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Log4j2
public class MarketHistoryUtil {
	private static final Pair<LocalDate, LocalDate> marketFile2016Range =
			Pair.of(LocalDate.parse("2016-08-01"), LocalDate.parse("2016-12-31"));

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected Provider<DataCrawler> dataCrawlerProvider;

	private final AvailableMarketHistoryFile ccpQuantBackfillFile;
	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Inject
	protected MarketHistoryUtil(UrlParser urlParser) {
		ccpQuantBackfillFile = AvailableMarketHistoryFile.builder()
				.ccpQuantBackfillFile(true)
				.httpUrl((HttpUrl) urlParser.parse(
						"https://data.everef.net/ccp/ccp_quant/EVEOnline_priceHistory_20031001_20180305.7z"))
				.range(Pair.of(LocalDate.parse("2003-10-01"), LocalDate.parse("2018-03-05")))
				.build();
	}

	/**
	 * Fetches the total pairs from the data site.
	 * @return
	 */
	public Single<Map<LocalDate, Integer>> downloadTotalPairs() {
		return Single.defer(() -> {
			log.info("Downloading total pairs file");
			var url = dataBaseUrl.resolve(MARKET_HISTORY.getFolder() + "/").resolve("totals.json");
			var file = tempFiles.tempFile("market-history-pairs", ".json").toFile();
			return okHttpHelper.download(url.toString(), file, okHttpClient).flatMap(response -> {
				log.trace("Pairs file downloaded");
				if (response.code() == 404) {
					log.warn("Total pairs file not found");
					return Single.just(Map.of());
				}
				if (response.code() != 200) {
					return Single.error(new RuntimeException("Failed downloading pairs file"));
				}
				var type =
						objectMapper.getTypeFactory().constructMapType(TreeMap.class, LocalDate.class, Integer.class);
				Map<LocalDate, Integer> totals = objectMapper.readValue(file, type);
				log.info("Pairs file loaded");
				file.delete();
				return Single.just(totals);
			});
		});
	}

	/**
	 * Returns all the files available on the data site.
	 * @return
	 */
	public Flowable<AvailableMarketHistoryFile> crawlAvailableFiles() {
		var files = dataCrawlerProvider
				.get()
				.setPrefix(ArchivePathFactory.MARKET_HISTORY.getFolder())
				.crawl()
				.flatMap(url -> {
					var path = url.getPath();
					var datestamp = ArchivePathFactory.MARKET_HISTORY.parseArchiveTime(path);
					var yearstamp = ArchivePathFactory.MARKET_HISTORY_YEAR.parseArchiveTime(path);
					AvailableMarketHistoryFile file = null;
					if (datestamp != null) {
						var date = datestamp.atZone(UTC).toLocalDate();
						file = AvailableMarketHistoryFile.builder()
								.date(date)
								.httpUrl((HttpUrl) url)
								.range(Pair.of(date, date))
								.build();
					} else if (yearstamp != null) {
						var year = yearstamp.atZone(UTC).toLocalDate().getYear();
						// The year file for 2016 is partial.
						var range = year == 2016
								? marketFile2016Range
								: Pair.of(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
						file = AvailableMarketHistoryFile.builder()
								.year(year)
								.httpUrl((HttpUrl) url)
								.range(range)
								.build();
					}
					if (file == null) {
						return Flowable.empty();
					}
					log.trace("Found market history file: {}", file);
					return Flowable.just(file);
				});
		return Flowable.concatArray(Flowable.just(ccpQuantBackfillFile), files);
	}
}
