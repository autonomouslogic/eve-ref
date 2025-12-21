package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.MarketApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiResponse;
import com.autonomouslogic.everef.openapi.esi.model.MarketsPricesGetInner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class EsiMarketPriceService {
	@Inject
	protected MarketApi marketApi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ScheduledExecutorService scheduler;

	private String esiMarketPricesEtag;
	private ScheduledFuture<?> marketPricesFuture;
	private Map<Long, MarketPrice> prices = new ConcurrentHashMap<>();

	@Inject
	protected EsiMarketPriceService() {}

	public void init() {
		updateEsiMarketPrices();
		marketPricesFuture = scheduler.scheduleAtFixedRate(
				() -> {
					try {
						updateEsiMarketPrices();
					} catch (Exception e) {
						log.warn("Failed to update market prices, ignoring", e);
					}
				},
				10,
				10,
				TimeUnit.MINUTES);
	}

	@SneakyThrows
	private void updateEsiMarketPrices() {
		log.info("Updating market prices");
		ApiResponse<List<MarketsPricesGetInner>> res;
		try {
			res = marketApi.getMarketsPricesWithHttpInfo(esiHelper.getCompatibilityDate(), null, esiMarketPricesEtag, null);
		} catch (ApiException e) {
			if (e.getCode() == 304) {
				log.debug("No market prices update needed");
				return;
			} else {
				throw e;
			}
		}
		for (MarketsPricesGetInner price : res.getData()) {
			prices.computeIfAbsent(price.getTypeId().longValue(), ignore -> new MarketPrice())
					.setEsiAdjustedPrice(
							Optional.ofNullable(price.getAdjustedPrice()).orElse(0.0))
					.setEsiAveragePrice(
							Optional.ofNullable(price.getAveragePrice()).orElse(0.0));
		}
		esiMarketPricesEtag = res.getHeaders().get("ETag").getFirst();
		log.debug("Finished updating market prices");
	}

	public OptionalDouble getEsiAdjustedPrice(long typeId) {
		if (!prices.containsKey(typeId)) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(prices.get(typeId).getEsiAdjustedPrice());
	}

	public OptionalDouble getEsiAveragePrice(long typeId) {
		if (!prices.containsKey(typeId)) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(prices.get(typeId).getEsiAveragePrice());
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

	@Data
	private static final class MarketPrice {
		private double esiAdjustedPrice;
		private double esiAveragePrice;
	}
}
