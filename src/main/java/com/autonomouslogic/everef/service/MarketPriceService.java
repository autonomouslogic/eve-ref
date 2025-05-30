package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.model.api.MaterialCost;
import com.autonomouslogic.everef.model.api.PriceSource;
import com.autonomouslogic.everef.util.MathUtil;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

@Singleton
public class MarketPriceService {
	@Inject
	protected EsiMarketPriceService esiMarketPriceService;

	@Inject
	protected FuzzworkMarketService fuzzworkMarketService;

	@Inject
	protected MarketPriceService() {}

	public Map<Long, BigDecimal> getMarketPrices(@NonNull List<Long> typeIds, @NonNull PriceSource priceSource) {
		if (priceSource.isEsi()) {
			return getEsiPrices(typeIds);
		} else if (priceSource.isFuzzwork()) {
			return getFuzzworkPrices(typeIds, priceSource);
		}
		throw new RuntimeException(String.format("Unknown price source: %s", priceSource));
	}

	private Map<Long, BigDecimal> getFuzzworkPrices(List<Long> typeIds, PriceSource priceSource) {
		var mapped = new LinkedHashMap<Long, BigDecimal>();
		fuzzworkMarketService
				.fetchAggregateStationPrices(priceSource.getStationId(), typeIds)
				.forEach((key, type) -> {
					var price = Optional.ofNullable(type)
							.flatMap(p -> switch (priceSource) {
								case FUZZWORK_JITA_BUY_AVG, FUZZWORK_JITA_BUY_MAX -> Optional.ofNullable(p.getBuy());
								case FUZZWORK_JITA_SELL_AVG, FUZZWORK_JITA_SELL_MIN -> Optional.ofNullable(p.getSell());
								default ->
									throw new RuntimeException(String.format("Unknown price source: %s", priceSource));
							})
							.flatMap(p -> switch (priceSource) {
								case FUZZWORK_JITA_BUY_AVG, FUZZWORK_JITA_SELL_AVG ->
									Optional.ofNullable(p.getWeightedAverage());
								case FUZZWORK_JITA_BUY_MAX -> Optional.ofNullable(p.getMax());
								case FUZZWORK_JITA_SELL_MIN -> Optional.ofNullable(p.getMin());
								default ->
									throw new RuntimeException(String.format("Unknown price source: %s", priceSource));
							})
							.orElse(BigDecimal.ZERO);
					mapped.put(Long.valueOf(key), price);
				});
		return mapped;
	}

	private @NotNull Map<Long, BigDecimal> getEsiPrices(@NonNull List<Long> typeIds) {
		return typeIds.stream()
				.map(typeId -> Pair.of(
						typeId,
						MathUtil.round(
								BigDecimal.valueOf(esiMarketPriceService
										.getEsiAveragePrice(typeId)
										.orElse(0)),
								2)))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

	public Map<String, MaterialCost> materialCosts(Map<String, MaterialCost> materials, PriceSource priceSource) {
		var typeIds = materials.keySet().stream().map(Long::valueOf).toList();
		var prices = getMarketPrices(typeIds, priceSource);
		var newMaterials = new LinkedHashMap<String, MaterialCost>();
		materials.keySet().forEach(key -> {
			var material = materials.get(key);
			var costPerUnit = prices.getOrDefault(Long.valueOf(key), BigDecimal.ZERO);
			var cost = MathUtil.round(costPerUnit.multiply(BigDecimal.valueOf(material.getQuantity())), 2);
			newMaterials.put(
					key,
					material.toBuilder().costPerUnit(costPerUnit).cost(cost).build());
		});
		return newMaterials;
	}
}
