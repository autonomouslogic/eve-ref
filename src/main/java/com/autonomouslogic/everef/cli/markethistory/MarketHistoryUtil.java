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

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Inject
	protected MarketHistoryUtil() {}

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
	public Flowable<Pair<LocalDate, HttpUrl>> crawlAvailableFiles() {
		return dataCrawlerProvider
				.get()
				.setPrefix(ArchivePathFactory.MARKET_HISTORY.getFolder())
				.crawl()
				.flatMap(url -> {
					var path = url.getPath();
					var datestamp = ArchivePathFactory.MARKET_HISTORY.parseArchiveTime(path);
					if (datestamp != null) {
						var date = datestamp.atZone(UTC).toLocalDate();
						log.trace("Found market history file for {}: {}", datestamp, url);
						return Flowable.just(Pair.of(date, (HttpUrl) url));
					}
					return Flowable.empty();
				});
	}
}
