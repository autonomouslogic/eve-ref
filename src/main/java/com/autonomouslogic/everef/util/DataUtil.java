package com.autonomouslogic.everef.util;

import static com.autonomouslogic.everef.util.ArchivePathFactory.ESI;
import static com.autonomouslogic.everef.util.ArchivePathFactory.HOBOLEAKS;
import static com.autonomouslogic.everef.util.ArchivePathFactory.MARKET_ORDERS;
import static com.autonomouslogic.everef.util.ArchivePathFactory.PUBLIC_CONTRACTS;
import static com.autonomouslogic.everef.util.ArchivePathFactory.STRUCTURES;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.model.Structure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

@Singleton
@Log4j2
public class DataUtil {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected Provider<DataCrawler> dataCrawlerProvider;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected OkHttpClient okHttpClient;

	private URI dataBaseUrl;

	@Inject
	protected DataUtil() {}

	@Inject
	protected void init() {
		dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
	}

	public Single<File> downloadLatestSde() {
		return dataCrawlerProvider
				.get()
				.setPrefix("/ccp/sde")
				.crawl()
				.filter(url -> url.toString().endsWith("-TRANQUILITY.zip"))
				.sorted()
				.lastElement()
				.switchIfEmpty(Single.error(new RuntimeException("No SDE found")))
				.flatMap(url -> {
					log.info("Using SDE at: {}", url);
					var file = tempFiles.tempFile("sde", ".zip").toFile();
					return okHttpHelper
							.download(url.toString(), file, okHttpClient)
							.flatMap(response -> {
								if (response.code() != 200) {
									return Single.error(
											new RuntimeException("Failed downloading ESI: " + response.code()));
								}
								return Single.just(file);
							});
				});
	}

	public Single<File> downloadLatestEsi() {
		return download(ESI, "esi", ".tar.xz");
	}

	public Single<File> downloadLatestHoboleaks() {
		return download(HOBOLEAKS, "hoboleaks", ".tar.xz");
	}

	public Single<File> downloadLatestPublicContracts() {
		return download(PUBLIC_CONTRACTS, "public-contracts-latest", ".tar.bz2");
	}

	public Single<File> downloadLatestMarketOrders() {
		return download(MARKET_ORDERS, "market-orders-latest", ".v3.csv.bz2");
	}

	public Single<Map<String, Structure>> downloadLatestStructures() {
		return download(STRUCTURES, "structures", ".json").map(file -> {
			var type =
					objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Structure.class);
			Map<String, Structure> parsed = objectMapper.readValue(file, type);
			return parsed;
		});
	}

	private Single<File> download(ArchivePathFactory archive, String name, String suffix) {
		return Single.defer(() -> {
			var url = dataBaseUrl + "/" + archive.createLatestPath();
			var file = tempFiles.tempFile(name, suffix).toFile();
			return okHttpHelper.download(url, file, okHttpClient).flatMap(response -> {
				if (response.code() != 200) {
					return Single.error(
							new RuntimeException(String.format("Failed downloading %s: %s", name, response.code())));
				}
				return Single.just(file);
			});
		});
	}

	@SneakyThrows
	public JsonNode loadJsonResource(String path) {
		return objectMapper.readTree(ResourceUtil.loadResource(path));
	}
}
