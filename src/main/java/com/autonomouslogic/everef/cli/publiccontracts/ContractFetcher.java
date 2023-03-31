package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseRegionsRegionIdOk;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVMap;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Fetches all the public contracts for all the regions.
 */
@Slf4j
public class ContractFetcher {
	@Inject
	protected ObjectMapper objectMapper;
	@Inject
	protected LocationPopulator locationPopulator;
	@Inject
	protected UniverseEsi universeEsi;
	@Inject
	@Getter
	protected ContractAbyssalFetcher contractAbyssalFetcher;
	@Inject
	protected EsiHelper esiHelper;
	@Inject
	protected OkHttpHelper okHttpHelper;
	@Inject
	@Named("esi")
	protected OkHttpClient okHttpClient;

	@Setter
	@NonNull
	private MVMap<Long, JsonNode> contractsStore;
	@Setter
	@NonNull
	private MVMap<Long, JsonNode> itemsStore;
	@Setter
	@NonNull
	private MVMap<Long, JsonNode> bidsStore;

	private final Set<Long> contractsWithItems = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected ContractFetcher() {
	}

	public Flowable<Long> fetchPublicContracts() {
		return buildKnownItemIndex()
			.andThen(universeEsi.getAllRegions()
				.flatMap(region -> fetchContractsForRegion(region), 3));
	}

	private Flowable<Long> fetchContractsForRegion(GetUniverseRegionsRegionIdOk region) {
		var count = new AtomicInteger();
		return Flowable.defer(() -> {
				log.info(String.format("Fetching public from %s", region.getName()));
				var esiUrl = EsiUrl.builder()
					.urlPath(String.format("/contracts/public/%s", region.getRegionId()))
					.build();
				return esiHelper
					.fetchPagesOfJsonArrays(esiUrl, (entry, response) -> {
						var lastModified = okHttpHelper.getLastModified(response);
						var obj = (ObjectNode) entry;
						lastModified.ifPresent(date -> obj.put(
							"http_last_modified", date.toInstant().toString()));
						return obj;
					})
					.flatMap(entry -> {
						var obj = (ObjectNode) entry;
						var contractId = obj.get("contract_id").asLong();
						obj.put("region_id", region.getRegionId());
						return
							locationPopulator.populate(obj, "start_location_id")
								.andThen(Completable.fromAction(() ->
									contractsStore.put(contractId, obj)))
								.andThen(resolveItemsAndBids(obj))
								.andThen(Flowable.just(contractId));
					}, 32)
					.doOnNext(ignore -> {
						var n = count.incrementAndGet();
						if (n % 1_000 == 0) {
							log.debug(String.format("Fetched %d public contracts from %s", n, region.getName()));
						}
					});
			})
			.doOnComplete(() ->
				log.info(String.format("Fetched %d public contracts from %s", count.get(), region.getName())));
	}

	private Completable resolveItemsAndBids(ObjectNode contract) {
		return Completable.defer(() -> {
			var contractId = contract.get("contract_id").longValue();
			var type = contract.get("type").textValue();
			var tasks = new ArrayList<Completable>();
			if (("item_exchange".endsWith(type) || "auction".equals(type))) {
				tasks.add(fetchContractItems(contractId));
			}
			if ("auction".equals(type)) {
				tasks.add(fetchContractBids(contractId));
			}
			return tasks.isEmpty() ? Completable.complete() : Completable.merge(tasks);
		});
	}

	private Completable fetchContractItems(long contractId) {
		return Completable.defer(() -> {
			if (contractsWithItems.contains(contractId)) {
				return Completable.complete();
			}
			var observable = fetchContractSub("items", "record_id", itemsStore, contractId);
			return contractAbyssalFetcher.apply(contractId, observable);
		});
	}

	private Completable fetchContractBids(long contractId) {
		return fetchContractSub("bids", "bid_id", bidsStore, contractId)
			.ignoreElements();
	}

	private Observable<ObjectNode> fetchContractSub(String sub, String primaryKey, Map<Long, JsonNode> mapStore, long contractId) {
		EsiUrl esiUrl = new EsiUrl(String.format("/contracts/public/%s/%s", sub, contractId));
		return RxUtil.toSingle(scrapeFetcher.fetchAllPages(esiUrl))
			.flatMapObservable(responses -> Observable.fromIterable(responses))
			.flatMap(response -> Observable.defer(() -> {
				//if (response.getStatusCode() == 404 || response.getStatusCode() == 403) {
				if (response.code() != 200) {
					log.info(String.format("Public contract %s returned %s for contract %s", sub, response.code(), contractId));
					return Observable.empty();
				}
				JsonNode lastModified = eveRefDataUtil.lastModified(response);
				JsonNode pageNode = scrapeFetcher.decode(response);
				if (pageNode == null || pageNode.isNull() || !pageNode.isArray() || !(pageNode instanceof ArrayNode)) {
					log.info(String.format("Public contract %s did not return array for contract %s: ", sub, contractId, pageNode));
					return Observable.empty();
				}
				ArrayNode page = (ArrayNode) pageNode;
				if (page.isNull() || page.size() == 0) {
					return Observable.empty();
				}
				List<ObjectNode> entries = new ArrayList<>();
				for (JsonNode entry : page) {
					ObjectNode entryObj = (ObjectNode) entry;
					entries.add(entryObj);
					entryObj.put("contract_id", contractId);
					entryObj.set("http_last_modified", lastModified);
					long id = entry.get(primaryKey).asLong();
					mapStore.put(id, entryObj);
				}
				return Observable.fromIterable(entries);
			}));
	}

	/**
	 * Builds a list of contracts where the items are already known.
	 * This is used to avoid fetching items for contracts where we already have them.
	 */
	private Completable buildKnownItemIndex() {
		return Completable.fromAction(() -> {
			log.debug("Building item contract index.");
			itemsStore.values().forEach(item -> contractsWithItems.add(item.get("contract_id").asLong()));
			log.debug(String.format("Built list of %s known contracts with items.", contractsWithItems.size()));
		});
	}
}
