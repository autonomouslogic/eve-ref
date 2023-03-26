package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;

@Slf4j
public class MarketOrderFetcher {
	@Inject
	protected UniverseEsi universeEsi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Setter
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrderFetcher() {}

	public Completable fetchMarketOrders() {
		return universeEsi
				.getAllRegions()
				.flatMap(region -> fetchMarketOrders(region), false, 1)
				.doOnNext(this::validateOrder)
				.flatMapCompletable(this::saveMarketOrder);
	}

	public Flowable<ObjectNode> fetchMarketOrders(@NonNull GetUniverseRegionsRegionIdOk region) {
		var count = new AtomicInteger();
		return Flowable.defer(() -> {
					log.info(String.format("Fetching market orders from %s", region.getName()));
					var esiUrl = EsiUrl.builder()
							.urlPath(String.format("/markets/%s/orders?order_type=all", region.getRegionId()))
							.build();
					return esiHelper
							.fetchPagesOfJsonArrays(esiUrl, (entry, response) -> {
								var lastModified = okHttpHelper.getLastModified(response);
								var obj = (ObjectNode) entry;
								lastModified.ifPresent(date -> obj.put(
										"http_last_modified", date.toInstant().toString()));
								return obj;
							})
							.map(entry -> {
								count.incrementAndGet();
								var obj = (ObjectNode) entry;
								obj.put("region_id", region.getRegionId());
								// locationPopulator.populateStation(obj, "location_id"); // @todo fix this
								return obj;
							});
				})
				.doOnComplete(() ->
						log.info(String.format("Fetched %d market orders from %s", count.get(), region.getName())));
	}

	private Completable saveMarketOrder(ObjectNode order) {
		return Completable.fromAction(() -> {
			long id = order.get("order_id").asLong();
			marketOrdersStore.put(id, order);
		});
	}

	private void validateOrder(ObjectNode order) {
		if (JsonUtil.isNullOrEmpty(order.get("region_id"))) {
			throw new IllegalStateException("region_id is null");
		}
		if (JsonUtil.isNullOrEmpty(order.get("constellation_id"))) {
			throw new IllegalStateException("constellation_id is null");
		}
		if (JsonUtil.isNullOrEmpty(order.get("system_id"))) {
			throw new IllegalStateException("system_id is null");
		}
		if (JsonUtil.isNullOrEmpty(order.get("station_id"))) {
			throw new IllegalStateException("station_id is null");
		}
	}
}
