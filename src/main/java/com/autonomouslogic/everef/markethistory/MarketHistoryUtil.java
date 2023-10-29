package com.autonomouslogic.everef.markethistory;

import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_HISTORY;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

@Singleton
@Log4j2
public class MarketHistoryUtil {
	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected ObjectMapper objectMapper;

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Inject
	protected MarketHistoryUtil() {}

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
}
