package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseRegionsRegionIdOk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
	@Named("esi")
	protected OkHttpWrapper okHttpWrapper;

	@Setter
	@NonNull
	private Map<Long, JsonNode> contractsStore;

	@Setter
	@NonNull
	private Map<Long, JsonNode> itemsStore;

	@Setter
	@NonNull
	private Map<Long, JsonNode> bidsStore;

	private final Set<Long> contractsWithItems = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Inject
	protected ContractFetcher() {}

	@SneakyThrows
	public List<Long> fetchPublicContracts() {
		buildKnownItemIndex();
		var regions = universeEsi.getAllRegions();

		// Process regions in parallel (3 at a time)
		var tasks = regions.stream()
				.map(region -> (Callable<List<Long>>) () -> fetchContractsForRegion(region))
				.toList();

		VirtualThreads.checkIsVirtual();
		var allContractIds = VirtualThreads.callAll(tasks.iterator(), 3);

		// Flatten the list of lists
		return allContractIds.stream().flatMap(List::stream).toList();
	}

	private List<Long> fetchContractsForRegion(GetUniverseRegionsRegionIdOk region) {
		return fetchWithRetry(
				"public contracts for " + region.getName(),
				() -> fetchContractsForRegionInner(region),
				12,
				Duration.ofSeconds(5));
	}

	@SneakyThrows
	private List<Long> fetchContractsForRegionInner(GetUniverseRegionsRegionIdOk region) {
		var count = new AtomicInteger();
		log.info("Fetching public contracts from {}", region.getName());

		// Fetch all contracts from ESI
		var contracts = fetchContractsFromEsi(region);

		// Process contracts in parallel (32 at a time)
		var tasks = contracts.stream()
				.map(contract -> (Callable<Long>) () -> {
					var contractId = populateLocation(region, contract);
					var n = count.incrementAndGet();
					if (n % 1_000 == 0) {
						log.debug("Fetched {} public contracts from {}", n, region.getName());
					}
					return contractId;
				})
				.toList();

		VirtualThreads.checkIsVirtual();
		var contractIds = VirtualThreads.callAll(tasks.iterator(), 32);

		log.info("Fetched {} public contracts from {}", count.get(), region.getName());
		return contractIds;
	}

	private List<ObjectNode> fetchContractsFromEsi(GetUniverseRegionsRegionIdOk region) {
		var esiUrl = EsiUrl.builder()
				.urlPath(String.format("/contracts/public/%s", region.getRegionId()))
				.build();
		return esiHelper
				.fetchPagesOfJsonArrays(esiUrl, esiHelper::populateLastModified)
				.map(obj -> (ObjectNode) obj)
				.toList()
				.blockingGet();
	}

	private Long populateLocation(GetUniverseRegionsRegionIdOk region, ObjectNode entry) {
		var contractId = entry.get("contract_id").asLong();
		entry.put("region_id", region.getRegionId());

		locationPopulator.populate(entry, "start_location_id").blockingAwait();
		contractsStore.put(ContractsFileBuilder.CONTRACT_ID.apply(entry), entry);
		resolveItemsAndBids(entry);

		return contractId;
	}

	private void resolveItemsAndBids(ObjectNode contract) {
		var contractId = contract.get("contract_id").longValue();
		var type = contract.get("type").textValue();

		if ("item_exchange".equals(type) || "auction".equals(type)) {
			fetchContractItems(contractId);
		}
		if ("auction".equals(type)) {
			fetchContractBids(contractId);
		}
	}

	private void fetchContractItems(long contractId) {
		if (contractsWithItems.contains(contractId)) {
			return;
		}
		var items = fetchContractSub("items", ContractsFileBuilder.ITEM_ID, itemsStore, contractId);
		contractAbyssalFetcher.apply(contractId, Flowable.fromIterable(items)).blockingAwait();
	}

	private void fetchContractBids(long contractId) {
		fetchContractSub("bids", ContractsFileBuilder.BID_ID, bidsStore, contractId);
	}

	private List<ObjectNode> fetchContractSub(
			String sub, Function<JsonNode, Long> idExtractor, Map<Long, JsonNode> mapStore, long contractId) {
		var esiUrl = EsiUrl.builder()
				.urlPath(String.format("/contracts/public/%s/%s", sub, contractId))
				.build();

		var results = esiHelper
				.fetchPagesOfJsonArrays(esiUrl, esiHelper::populateLastModified)
				.toList()
				.blockingGet();

		if (results.isEmpty()) {
			log.info("Public contract {} did not return anything for contract {}", sub, contractId);
		}

		results.forEach(entry -> {
			var obj = (ObjectNode) entry;
			obj.put("contract_id", contractId);
			mapStore.put(idExtractor.apply(obj), obj);
		});

		return results.stream().map(node -> (ObjectNode) node).toList();
	}

	/**
	 * Builds a list of contracts where the items are already known.
	 * This is used to avoid fetching items for contracts where we already have them.
	 */
	private void buildKnownItemIndex() {
		log.debug("Building item contract index.");
		itemsStore
				.values()
				.forEach(item -> contractsWithItems.add(item.get("contract_id").asLong()));
		log.debug("Built list of {} known contracts with items.", contractsWithItems.size());
	}

	private <T> T fetchWithRetry(String description, Supplier<T> fetcher, int maxRetries, Duration delay) {
		Exception lastException = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				return fetcher.get();
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					log.warn("Retrying {}: {}", description, ExceptionUtils.getMessage(e));
					try {
						Thread.sleep(delay.toMillis());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Interrupted during retry", ie);
					}
				}
			}
		}
		throw new RuntimeException("Failed " + description + " after " + maxRetries + " retries", lastException);
	}
}
