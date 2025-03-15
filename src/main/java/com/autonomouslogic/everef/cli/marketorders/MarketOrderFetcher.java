package com.autonomouslogic.everef.cli.marketorders;

import static com.autonomouslogic.everef.util.EveConstants.NPC_STATION_MAX_ID;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiException;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.util.DataUtil;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
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

	private final String scrapeOwnerHash = Configs.SCRAPE_CHARACTER_OWNER_HASH.getRequired();

	@Inject
	protected MarketOrderFetcher() {}

	public Completable fetchMarketOrders() {
		return Flowable.concatArray(fetchPublicOrders(), fetchStructureOrders())
				.parallel(32)
				.runOn(VirtualThreads.SCHEDULER)
				.flatMap(order ->
						locationPopulator.populate(order, "location_id").andThen(Flowable.just(order)))
				.doOnNext(this::verifyOrderLocation)
				.sequential()
				.flatMapCompletable(this::saveMarketOrder)
				.onErrorResumeNext(e -> Completable.error(new RuntimeException("Failed fetching market orders", e)));
	}

	private Flowable<ObjectNode> fetchPublicOrders() {
		return universeEsi
				.getAllRegions()
				.parallel(4)
				.runOn(VirtualThreads.SCHEDULER)
				.flatMap(region -> {
					return fetchOrders(
									String.format("/markets/%s/orders?order_type=all", region.getRegionId()),
									region.getName(),
									false)
							.map(order -> {
								if (!order.has("station_id")) {
									order.put("station_id", order.get("location_id"));
								}
								order.put("region_id", region.getRegionId());
								return order;
							});
				})
				.sequential();
	}

	private Flowable<ObjectNode> fetchStructureOrders() {
		return dataUtil.downloadLatestStructures().flatMapPublisher(structures -> {
			return Flowable.fromIterable(structures.values())
					.filter(s -> s.isMarketStructure())
					.parallel(4)
					.runOn(VirtualThreads.SCHEDULER)
					.flatMap(structure -> {
						return fetchOrders(
										String.format("/markets/structures/%s/", structure.getStructureId()),
										Long.toString(structure.getStructureId()),
										true)
								.map(order -> {
									Optional.ofNullable(structure.getSolarSystemId())
											.ifPresent(v -> order.put("system_id", v));
									Optional.ofNullable(structure.getConstellationId())
											.ifPresent(v -> order.put("constellation_id", v));
									Optional.ofNullable(structure.getRegionId())
											.ifPresent(v -> order.put("region_id", v));
									return order;
								});
					})
					.filter(order -> {
						long id = order.get("order_id").asLong();
						return !marketOrdersStore.containsKey(id);
					})
					.sequential();
		});
	}

	private Flowable<ObjectNode> fetchOrders(@NonNull String url, @NonNull String locationName, boolean auth) {
		var count = new AtomicInteger();
		return Flowable.defer(() -> {
					log.info(String.format("Fetching market orders from %s", locationName));
					var esiUrl = EsiUrl.builder().urlPath(url).build();
					Maybe<String> token = auth ? getAccessToken() : Maybe.empty();
					return esiHelper
							.fetchPagesOfJsonArrays(esiUrl, esiHelper::populateLastModified, token)
							.map(entry -> {
								var n = count.incrementAndGet();
								if (n % 10_000 == 0) {
									log.debug(String.format("Fetched %d market orders from %s", n, locationName));
								}
								return (ObjectNode) entry;
							});
				})
				.doOnComplete(
						() -> log.info(String.format("Fetched %d market orders from %s", count.get(), locationName)))
				.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(2), e -> {
					if (e instanceof EsiException) {
						var code = ((EsiException) e).getCode();
						if (code == 403) {
							log.debug("Received {} on {}, ignoring", code, locationName);
							return false;
						}
					}
					log.warn("Retrying {}: {}", locationName, ExceptionUtils.getMessage(e));
					return true;
				}))
				.onErrorResumeNext(e -> {
					if (e instanceof EsiException) {
						var code = ((EsiException) e).getCode();
						if (code == 403) {
							return Flowable.empty();
						}
					}
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
		var locationId = order.get("location_id").asLong();
		if (locationId > NPC_STATION_MAX_ID) {
			// The structure JSON can contain structures with accessible markets, but which aren't otherwise gettable.
			return;
		}
		if (JsonUtil.isNullOrEmpty(order.get("region_id"))) {
			throw new IllegalStateException("region_id is null: " + order);
		}
		if (JsonUtil.isNullOrEmpty(order.get("constellation_id"))) {
			throw new IllegalStateException("constellation_id is null: " + order);
		}
		if (JsonUtil.isNullOrEmpty(order.get("system_id"))) {
			throw new IllegalStateException("system_id is null: " + order);
		}
		//		if (JsonUtil.isNullOrEmpty(order.get("station_id"))) {
		//			throw new IllegalStateException("station_id is null: " + order);
		//		}
	}

	private Maybe<String> getAccessToken() {
		return esiAuthHelper.getTokenStringForOwnerHash(scrapeOwnerHash).toMaybe();
	}
}
