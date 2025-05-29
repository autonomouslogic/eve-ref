package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.fuzzwork.FuzzworkAggregatedMarketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;

@Singleton
public class FuzzworkMarketService {
	@Inject
	protected OkHttpClient httpClient;

	@Inject
	protected OkHttpWrapper httpWrapper;

	@Inject
	protected ObjectMapper objectMapper;

	private final URI basePath = Configs.FUZZWORK_MARKET_API_BASE_PATH.getRequired();
	private final MapType responseType;

	@Inject
	protected FuzzworkMarketService(ObjectMapper objectMapper) {
		responseType = objectMapper
				.getTypeFactory()
				.constructMapType(HashMap.class, String.class, FuzzworkAggregatedMarketType.class);
	}

	@SneakyThrows
	public Map<String, FuzzworkAggregatedMarketType> fetchAggregateStationPrices(
			long station, @NonNull List<Long> typeIds) {
		var url = basePath + "aggregates/?station=" + station + "&types="
				+ typeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		var response = httpWrapper.get(url);
		try (var in = response.body().byteStream()) {
			return objectMapper.readValue(in, responseType);
		}
	}
}
