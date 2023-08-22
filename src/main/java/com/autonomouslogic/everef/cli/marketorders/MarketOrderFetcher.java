package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.h2.mvstore.MVMap;

@Slf4j
public class MarketOrderFetcher {
	@Inject
	protected UniverseEsi universeEsi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected LocationPopulator locationPopulator;

	@Setter
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrderFetcher() {}

	public Completable fetchMarketOrders() {
		return universeEsi
				.getAllRegions()
				.flatMap(region -> fetchMarketOrders(region), false, 4)
				.flatMap(order ->
						locationPopulator.populate(order, "location_id").andThen(Flowable.just(order)))
				.doOnNext(this::verifyOrderLocation)
				.flatMapCompletable(this::saveMarketOrder)
				.onErrorResumeNext(e -> Completable.error(new RuntimeException("Failed fetching market orders", e)));
	}

	public Flowable<ObjectNode> fetchMarketOrders(@NonNull GetUniverseRegionsRegionIdOk region) {
		var count = new AtomicInteger();
		return Flowable.defer(() -> {
					log.info(String.format("Fetching market orders from %s", region.getName()));
					var esiUrl = EsiUrl.builder()
							.urlPath(String.format("/markets/%s/orders?order_type=all", region.getRegionId()))
							.build();
					return esiHelper
							.fetchPagesOfJsonArrays(esiUrl, esiHelper::populateLastModified)
							.map(entry -> {
								var n = count.incrementAndGet();
								if (n % 10_000 == 0) {
									log.debug(String.format("Fetched %d market orders from %s", n, region.getName()));
								}
								var obj = (ObjectNode) entry;
								obj.put("region_id", region.getRegionId());
								return obj;
							});
				})
				.doOnComplete(() ->
						log.info(String.format("Fetched %d market orders from %s", count.get(), region.getName())))
				.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(1), e -> {
					log.warn("Retrying region {}: {}", region.getName(), ExceptionUtils.getMessage(e));
					return true;
				}))
				.onErrorResumeNext(e -> {
					return Flowable.error(new RuntimeException(
							String.format("Failed fetching market orders from %s", region.getName()), e));
				});
	}

	private Completable saveMarketOrder(ObjectNode order) {
		return Completable.fromAction(() -> {
			long id = order.get("order_id").asLong();
			marketOrdersStore.put(id, order);
		});
	}

	private void verifyOrderLocation(ObjectNode order) {
		if (JsonUtil.isNullOrEmpty(order.get("region_id"))) {
			throw new IllegalStateException("region_id is null: " + order);
		}
		if (JsonUtil.isNullOrEmpty(order.get("constellation_id"))) {
			throw new IllegalStateException("constellation_id is null: " + order);
		}
		if (JsonUtil.isNullOrEmpty(order.get("system_id"))) {
			throw new IllegalStateException("system_id is null: " + order);
		}
		if (JsonUtil.isNullOrEmpty(order.get("station_id"))) {
			throw new IllegalStateException("station_id is null: " + order);
		}
	}
}
