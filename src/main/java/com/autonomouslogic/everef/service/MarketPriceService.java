package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.openapi.esi.api.MarketApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiResponse;
import com.autonomouslogic.everef.openapi.esi.model.GetMarketsPrices200Ok;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class MarketPriceService {
	@Inject
	protected MarketApi marketApi;

	@Inject
	protected ScheduledExecutorService scheduler;

	private String marketPricesEtag;
	private ScheduledFuture<?> marketPricesFuture;
	private Map<Long, Double> prices = new ConcurrentHashMap<>();

	@Inject
	protected MarketPriceService() {}

	public void init() {
		updateMarketPrices();
		marketPricesFuture = scheduler.scheduleAtFixedRate(
				() -> {
					try {
						updateMarketPrices();
					} catch (Exception e) {
						log.warn("Failed to update market prices, ignoring", e);
					}
				},
				10,
				10,
				TimeUnit.SECONDS);
	}

	@SneakyThrows
	private void updateMarketPrices() {
		log.debug("Updating market prices");
		ApiResponse<List<GetMarketsPrices200Ok>> res;
		try {
			res = marketApi.getMarketsPricesWithHttpInfo(null, marketPricesEtag);
		} catch (ApiException e) {
			if (e.getCode() == 304) {
				log.debug("No market prices update needed");
				return;
			} else {
				throw e;
			}
		}
		for (GetMarketsPrices200Ok price : res.getData()) {
			prices.put(price.getTypeId().longValue(), price.getAdjustedPrice());
		}
		marketPricesEtag = res.getHeaders().get("ETag").getFirst();
		log.debug("Finished updating market prices");
	}

	public OptionalDouble getEsiAdjustedPrice(long typeId) {
		if (!prices.containsKey(typeId)) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(prices.get(typeId));
	}

	public boolean isReady() {
		return !prices.isEmpty();
	}

	public void stop() {
		if (marketPricesFuture != null) {
			marketPricesFuture.cancel(true);
			marketPricesFuture = null;
		}
	}
}
