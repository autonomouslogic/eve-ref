package com.autonomouslogic.everef.cli.marketorders;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

	@Inject
	protected DataUtil dataUtil;

	@Inject
	protected EsiAuthHelper esiAuthHelper;

	@Setter
	private Map<Long, JsonNode> marketOrdersStore;

	private final String  ownerHah = Configs.SCRAPE_CHARACTER_OWNER_HASH.getRequired();

	@Inject
	protected MarketOrderFetcher() {}

	public Completable fetchOrders() {
		return Flowable.mergeArray(
				//fetchMarketOrders(),
			fetchStructureOrders()
			)
				.flatMap(order ->
						locationPopulator.populate(order, "location_id").andThen(Flowable.just(order)))
				.doOnNext(this::verifyOrderLocation)
				.flatMapCompletable(this::saveMarketOrder)
				.onErrorResumeNext(e -> Completable.error(new RuntimeException("Failed fetching market orders", e)));
	}

	private Flowable<ObjectNode> fetchMarketOrders() {
		return universeEsi
			.getAllRegions()
			.flatMap(region -> {
				return fetchMarketOrders(String.format("/markets/%s/orders?order_type=all", region.getRegionId()), region.getName())
					.map(order -> {
						return order.put("region_id", region.getRegionId());
					});
			}, false, 4);
	}

	private Flowable<ObjectNode> fetchStructureOrders() {
		return dataUtil.downloadLatestStructures().flatMapPublisher(structures -> {
			return Flowable.fromIterable(structures.values())
				.filter(s -> s.isMarketStructure())
				.flatMap(structure -> {
					return fetchMarketOrders(String.format("/markets/structures/%s/", structure.getStructureId()), Long.toString(structure.getStructureId()))
						.map(order -> {
							return order
								.put("system_id", structure.getSolarSystemId())
								.put("constellation_id", structure.getConstellationId())
								.put("region_id", structure.getRegionId())
								;
						});
				}, false, 4);
		});
	}

	private Flowable<ObjectNode> fetchMarketOrders(@NonNull String url, @NonNull String locationName) {
		var count = new AtomicInteger();
		return Flowable.defer(() -> {
					log.info(String.format("Fetching market orders from %s", locationName));
					var esiUrl = EsiUrl.builder()
							.urlPath(url)
							.build();
					return esiHelper
							.fetchPagesOfJsonArrays(esiUrl, esiHelper::populateLastModified)
							.map(entry -> {
								var n = count.incrementAndGet();
								if (n % 10_000 == 0) {
									log.debug(String.format("Fetched %d market orders from %s", n, locationName));
								}
								return (ObjectNode) entry;
							});
				})
				.doOnComplete(() ->
						log.info(String.format("Fetched %d market orders from %s", count.get(), locationName)))
				.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(2), e -> {
					log.warn("Retrying {}: {}", locationName, ExceptionUtils.getMessage(e));
					return true;
				}))
				.onErrorResumeNext(e -> {
					return Flowable.error(new RuntimeException(
							String.format("Failed fetching market orders from %s", locationName), e));
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
