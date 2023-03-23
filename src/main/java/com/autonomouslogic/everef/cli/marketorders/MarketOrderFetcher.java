package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.scrape.ScrapeFetcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class MarketOrderFetcher {
	@Inject
	protected UniverseEsi universeEsi;
	
	@Inject
	protected ScrapeFetcher scrapeFetcher;

	@Setter
	private MVMap<Long, JsonNode> marketOrdersStore;

	@Inject
	protected MarketOrderFetcher() {
	}

	public Completable fetchMarketOrders() {
		universeEsi.getAllRegions()




		return Observable.defer(() -> Observable.fromIterable(getAllMarketRegions()))
			.toFlowable(BackpressureStrategy.BUFFER)
			.flatMap(region -> fetchMarketOrders(region).toFlowable(), 3)
			.ignoreElements();
	}

	private List<Region> getAllMarketRegions() {
		universeApi.getUniverseRegionsRegionId()


		List<Region> regions = universeDao.getAllRegions()
			.collect(Collectors.toList());
		return regions;
	}

	public Completable fetchMarketOrders(@NonNull Region region) {
		return Completable.defer(() -> {
			log.info(String.format("Fetching market orders from %s", region.getName().get("en")));
			EsiUrl esiUrl = new EsiUrl(String.format("/markets/%s/orders?order_type=all", region.getRegionId()));
			return RxUtil.toSingle(scrapeFetcher.fetchAllPages(esiUrl))
				.observeOn(Schedulers.computation())
				.flatMapObservable(responses -> Observable.fromIterable(responses))
				.flatMap(response -> Observable.defer(() -> {
					if (response.code() != 200) {
						log.info(String.format("Market orders returned %s for region %s", response.code(), region.getRegionId()));
						return Observable.empty();
					}
					JsonNode lastModified = eveRefDataUtil.lastModified(response);
					ArrayNode page = (ArrayNode) scrapeFetcher.decode(response);
					if (page.isNull() || page.size() == 0) {
						return Observable.empty();
					}
					List<ObjectNode> entries = new ArrayList<>();
					for (JsonNode entry : page) {
						ObjectNode entryObj = (ObjectNode) entry;
						logMarketOrderMetric(entryObj, region);
						entries.add(entryObj);
						entryObj.put("region_id", region.getRegionId());
						entryObj.set("http_last_modified", lastModified);
						locationPopulator.populateStation(entryObj, "location_id");
					}
					return Observable.fromIterable(entries);
				}))
				.flatMapCompletable(entry -> Completable.fromAction(() -> {
					long id = entry.get("order_id").asLong();
					marketOrdersStore.put(id, entry);
				}));
		})
			.andThen(Completable.fromAction(() -> String.format("Completed market orders from %s", region.getName().get("en"))));
	}

	private void logMarketOrderMetric(ObjectNode order, Region region) {
		String orderType = Optional.ofNullable(order.get("is_buy_order"))
			.map(JsonNode::asBoolean)
			.map(v -> v ? "buy" : "sell")
			.orElse("sell");
		esiMarketOrders
			.labels(Long.toString(region.getRegionId()), orderType)
			.inc();
	}
}
