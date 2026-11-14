package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.fuzzwork.FuzzworkAggregatedMarketType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.MapType;

@Singleton
@Log4j2
public class FuzzworkMarketService {
	@Inject
	protected OkHttpClient httpClient;

	@Inject
	protected OkHttpWrapper httpWrapper;

	@Inject
	protected JsonMapper jsonMapper;

	private final URI basePath = Configs.FUZZWORK_MARKET_BASE_PATH.getRequired();
	private final RateLimiter rateLimiter = RateLimiter.create(Configs.FUZZWORK_RATE_LIMIT_PER_S.getRequired());
	private final MapType responseType;
	private final Cache<String, FuzzworkAggregatedMarketType> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(5))
			.softValues()
			.build();

	@Inject
	protected FuzzworkMarketService(JsonMapper jsonMapper) {
		responseType = jsonMapper
				.getTypeFactory()
				.constructMapType(HashMap.class, String.class, FuzzworkAggregatedMarketType.class);
	}

	@SneakyThrows
	public Map<String, FuzzworkAggregatedMarketType> fetchAggregateStationPrices(
			long stationId, @NonNull List<Long> typeIds) {
		var result = new LinkedHashMap<String, FuzzworkAggregatedMarketType>();
		var missingTypeIds = new ArrayList<Long>();
		for (var typeId : typeIds) {
			var cached = cache.getIfPresent(stationCacheKey(stationId, typeId));
			if (cached != null) {
				result.put(String.valueOf(typeId), cached);
			} else {
				missingTypeIds.add(typeId);
			}
		}
		if (!missingTypeIds.isEmpty()) {
			var response = internalFetchAggregateStationPrices(stationId, missingTypeIds);
			cacheAggregateStationPrices(stationId, response);
			result.putAll(response);
		}
		return result;
	}

	@SneakyThrows
	private void cacheAggregateStationPrices(long station, @NonNull Map<String, FuzzworkAggregatedMarketType> types) {
		types.forEach((typeId, value) -> {
			var key = stationCacheKey(station, typeId);
			cache.put(key, value);
		});
	}

	@SneakyThrows
	private Map<String, FuzzworkAggregatedMarketType> internalFetchAggregateStationPrices(
			long station, @NonNull List<Long> typeIds) {
		var url = basePath + "aggregates/?station=" + station + "&types="
				+ typeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		var maxAttempts = Configs.FUZZWORK_MAX_ATTEMPTS.getRequired();
		var retryDelay = Configs.FUZZWORK_RETRY_DELAY.getRequired();
		for (var attempt = 1; ; attempt++) {
			var waitTime = rateLimiter.acquire();
			log.debug(String.format("Fetching (waited %.1fs, attempt %d) %s", waitTime, attempt, url));
			try (var response = httpWrapper.get(url)) {
				if (!response.isSuccessful()) {
					throw new IOException(String.format("Fuzzwork returned HTTP %d for %s", response.code(), url));
				}
				try (var in = response.body().byteStream()) {
					return jsonMapper.readValue(in, responseType);
				}
			} catch (IOException | StreamReadException e) {
				if (attempt >= maxAttempts) {
					throw e;
				}
				log.warn("Fuzzwork request failed on attempt {}/{}, retrying: {}", attempt, maxAttempts, e.toString());
				Thread.sleep(retryDelay);
			}
		}
	}

	private static String stationCacheKey(long stationId, String typeId) {
		return String.format("%s-%s", stationId, typeId);
	}

	private static String stationCacheKey(long stationId, long typeId) {
		return String.format("%s-%s", stationId, typeId);
	}
}
