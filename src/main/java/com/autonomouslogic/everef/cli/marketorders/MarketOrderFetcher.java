package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVMap;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MarketOrderFetcher {
	@Inject
	protected UniverseEsi universeEsi;
	@Inject
	protected EsiHelper esiHelper;

	@Setter
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrderFetcher() {
	}

	public Completable fetchMarketOrders() {
		return universeEsi.getAllRegions()
			.flatMap(region -> fetchMarketOrders(region), false, 1)
			.flatMapCompletable(this::saveMarketOrder);
	}

	public Flowable<ObjectNode> fetchMarketOrders(@NonNull GetUniverseRegionsRegionIdOk region) {
		var count = new AtomicInteger();
		return Flowable.defer(() -> {
			log.info(String.format("Fetching market orders from %s", region.getName()));
			var esiUrl = EsiUrl.builder().urlPath(String.format("/markets/%s/orders?order_type=all", region.getRegionId())).build();
			return esiHelper.fetchPagesOfJsonArrays(esiUrl)
				.map(entry -> {
					count.incrementAndGet();
					var obj = (ObjectNode) entry;
					obj.put("region_id", region.getRegionId());
					//obj.set("http_last_modified", lastModified); // @todo figure out how to get this in here cleanly.
					//locationPopulator.populateStation(obj, "location_id"); // @todo fix this
					return obj;
				});
		})
			.doOnComplete(() -> log.info(String.format("Fetched %d market orders from %s", count.get(), region.getName())));
	}

	private Completable saveMarketOrder(ObjectNode order) {
		return Completable.fromAction(() -> {
			long id = order.get("order_id").asLong();
			marketOrdersStore.put(id, order);
		});
	}
}
