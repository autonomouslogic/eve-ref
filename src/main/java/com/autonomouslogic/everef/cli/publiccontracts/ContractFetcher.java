package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.evemarket.dao.UniverseDao;
import com.autonomouslogic.evemarket.model.Region;
import com.autonomouslogic.evemarket.scrape.EsiUrl;
import com.autonomouslogic.evemarket.scrape.LocationPopulator;
import com.autonomouslogic.evemarket.scrape.ScrapeFetcher;
import com.autonomouslogic.evemarket.util.EveRefDataUtil;
import com.autonomouslogic.evemarket.util.RxUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.prometheus.client.Counter;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.autonomouslogic.evemarket.util.MetricsService.METRICS_NAMESPACE;

/**
 * Fetches all the public contracts for all the regions.
 */
@Slf4j
public class ContractFetcher {
	@Inject
	protected UniverseDao universeDao;
	@Inject
	protected ScrapeFetcher scrapeFetcher;
	@Inject
	protected EveRefDataUtil eveRefDataUtil;
	@Inject
	protected ObjectMapper objectMapper;
	@Inject
	protected LocationPopulator locationPopulator;

	@Setter
	private MVMap<Long, JsonNode> contractsStore;
	@Setter
	private MVMap<Long, JsonNode> itemsStore;
	@Setter
	private MVMap<Long, JsonNode> bidsStore;
	@Setter
	private ContractAbyssalFetcher abyssalFetcher;

	private Set<Long> contractsWithItems;

	private Counter esiContracts = Counter.build()
		.name("esi_contracts_total")
		.help("Number of contracts fetched from the ESI.")
		.labelNames("contract_type")
		.namespace(METRICS_NAMESPACE)
		.register();

	private Counter esiContractsNew = Counter.build()
		.name("esi_contracts_new_total")
		.labelNames("contract_type")
		.help("Number of fetched contracts which were not already known.")
		.namespace(METRICS_NAMESPACE)
		.register();

	private Counter esiContractsCached = Counter.build()
		.name("esi_contracts_cached_total")
		.labelNames("contract_type")
		.help("Number of fetched contracts which were already known.")
		.namespace(METRICS_NAMESPACE)
		.register();

	private Counter esiBids = Counter.build()
		.name("esi_bids_total")
		.help("Number of total bids fetched from the ESI.")
		.namespace(METRICS_NAMESPACE)
		.register();

	private Counter esiItems = Counter.build()
		.name("esi_items_total")
		.help("Number of total items fetched from the ESI.")
		.namespace(METRICS_NAMESPACE)
		.register();

	@Inject
	protected ContractFetcher() {
	}

	public Observable<Long> fetch() {
		return Completable.complete()
			.andThen(buildItemIndex())
			.andThen(fetchRegions())
			.toFlowable(BackpressureStrategy.BUFFER)
			.flatMap(region -> fetchContractsForRegion(region).toFlowable(BackpressureStrategy.BUFFER), 3)
			.toObservable();
	}

	private Observable<Region> fetchRegions() {
		return Observable.fromIterable(() -> universeDao.getAllRegions().iterator());
	}

	private Observable<Long> fetchContractsForRegion(Region region) {
		log.info(String.format("Fetching public contracts for %s.", region.getName().get("en")));
		EsiUrl esiUrl = new EsiUrl(String.format("/contracts/public/%s",
			region.getRegionId()
		));
		return RxUtil.toSingle(scrapeFetcher.fetchAllPages(esiUrl))
			.flatMapObservable(responses -> Observable.fromIterable(responses))
			.flatMap(response -> {
				List<Long> contractIds = new ArrayList<>();
				List<ObjectNode> entries = new ArrayList<>();
				JsonNode page = scrapeFetcher.decode(response);
				JsonNode lastModified = eveRefDataUtil.lastModified(response);
				if (page.isNull()) {
					return Observable.empty();
				}
				if (page.has("error")) {
					log.error(String.format("Region %s reported %s", region.getRegionId(), page.get("error").toString()));
					return Observable.empty();
				}
				for (JsonNode contract : page) {
					ObjectNode contractObject = (ObjectNode) contract;
					long contractId = contract.get("contract_id").asLong();
					String type = contractObject.get("type").textValue();
					logContractMetrics(contractId, type);
					contractIds.add(contractId);
					contractObject.put("region_id", region.getRegionId());
					populateStation(contractObject);
					contractObject.set("http_last_modified", lastModified);
					contractsStore.put(contractId, contract);
					entries.add(contractObject);
				}
				return Observable.fromIterable(entries)
					.flatMapCompletable(this::resolveItemsAndBids)
					.andThen(Observable.fromIterable(contractIds));
			})
			.doOnComplete(() -> log.info(String.format("Contract fetch complete for %s.", region.getName().get("en"))));
	}

	private void logContractMetrics(long contractId, String contractType) {
		esiContracts
			.labels(contractType)
			.inc();
		if (contractsStore.containsKey(contractId)) {
			esiContractsCached
				.labels(contractType)
				.inc();
		}
		else {
			esiContractsNew
				.labels(contractType)
				.inc();
		}
	}

	private void populateStation(ObjectNode contract) {
		locationPopulator.populateStation(contract, "start_location_id");
	}

	private Completable resolveItemsAndBids(ObjectNode contract) {
		return Completable.defer(() -> {
			long contractId = contract.get("contract_id").longValue();
			String type = contract.get("type").textValue();
			Completable completable = Completable.complete();
			if (("item_exchange".endsWith(type) || "auction".equals(type))) {
				completable = completable.andThen(fetchContractItems(contractId));
			}
			if ("auction".equals(type)) {
				completable = completable.andThen(fetchContractBids(contractId));
			}
			return completable;
		});
	}

	private Completable fetchContractItems(long contractId) {
		return Completable.defer(() -> {
			if (contractsWithItems.contains(contractId)) {
				return Completable.complete();
			}
			Observable<ObjectNode> observable = fetchContractSub("items", "record_id", itemsStore, contractId)
				.doOnNext(o -> esiItems.inc());
			return abyssalFetcher.apply(contractId, observable);
		});
	}

	private Completable fetchContractBids(long contractId) {
		return fetchContractSub("bids", "bid_id", bidsStore, contractId)
			.doOnNext(o -> esiBids.inc())
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

	private Completable buildItemIndex() {
		return Completable.fromAction(() -> {
			log.info("Building item contract index.");
			contractsWithItems = new HashSet<>();
			itemsStore.values().forEach(item -> contractsWithItems.add(item.get("contract_id").asLong()));
			log.info(String.format("Built list of %s known contracts with items.", contractsWithItems.size()));
		});
	}
}
