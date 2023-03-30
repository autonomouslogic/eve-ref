package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.evemarket.dao.InventoryTypeDao;
import com.autonomouslogic.evemarket.model.InventoryType;
import com.autonomouslogic.evemarket.scrape.EsiUrl;
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

import static com.autonomouslogic.evemarket.util.MetricsService.METRICS_NAMESPACE;

/**
 * Fetches Abyssal traits for items.
 */
@Slf4j
public class ContractAbyssalFetcher {
	@Inject
	protected ScrapeFetcher scrapeFetcher;
	@Inject
	protected ObjectMapper objectMapper;
	@Inject
	protected InventoryTypeDao inventoryTypeDao;
	@Inject
	protected EveRefDataUtil eveRefDataUtil;

	@Setter
	private MVMap<Long, JsonNode> dynamicItemsStore;
	@Setter
	private MVMap<Long, JsonNode> nonDynamicItemsStore;
	@Setter
	private MVMap<String, JsonNode> dogmaEffectsStore;
	@Setter
	private MVMap<String, JsonNode> dogmaAttributesStore;

	private Counter esiAbyssalItems = Counter.build()
		.name("esi_abyssal_items_total")
		.help("Number of Abyssal items fetched from the ESI.")
		.namespace(METRICS_NAMESPACE)
		.register();

	@Inject
	protected ContractAbyssalFetcher() {
	}

	public Completable apply(long contractId, Observable<ObjectNode> in) {
		return in
			.filter(item -> isPotentialAbyssalItem(item))
			.filter(item -> isItemNotSeen(item))
			.toFlowable(BackpressureStrategy.BUFFER)
			.flatMapCompletable(item -> {
				long itemId = item.get("item_id").longValue();
				long typeId = item.get("type_id").longValue();
				return resolveDynamicItem(contractId, typeId, itemId);
			}, false, 1);
	}

	public boolean isPotentialAbyssalItem(ObjectNode item) {
		if (isNullOrEmpty(item, "item_id")) {
			return false;
		}
		if (isNullOrEmpty(item, "type_id")) {
			return false;
		}
		if (isFalse(item, "is_included")) {
			return false;
		}
		if (isGreaterThan(item, "quantity", 1)) {
			return false;
		}
		long typeId = item.get("type_id").asLong();
		InventoryType type = inventoryTypeDao.getInventoryType(typeId);
		if (type == null) {
			return false;
		}
		Long metaGroupId = type.getMetaGroupId();
		if (metaGroupId == null) {
			return false;
		}
		return metaGroupId == 15;
	}

	private boolean isNullOrEmpty(ObjectNode node, String field) {
		if (!node.has(field)) {
			return true;
		}
		JsonNode val = node.get(field);
		if (val == null || val.isNull()) {
			return true;
		}
		String txt = val.asText();
		return txt == null || txt.equals("");
	}

	private boolean isTrue(ObjectNode node, String field) {
		if (!node.has(field)) {
			return false;
		}
		JsonNode val = node.get(field);
		if (val == null || val.isNull()) {
			return false;
		}
		return val.asBoolean();
	}

	private boolean isFalse(ObjectNode node, String field) {
		if (!node.has(field)) {
			return false;
		}
		JsonNode val = node.get(field);
		if (val == null || val.isNull()) {
			return false;
		}
		return !val.asBoolean();
	}

	private boolean isGreaterThan(ObjectNode node, String field, int bound) {
		if (!node.has(field)) {
			return false;
		}
		JsonNode val = node.get(field);
		if (val == null || val.isNull()) {
			return false;
		}
		return val.asInt() > bound;
	}

	private boolean isItemNotSeen(ObjectNode item) {
		long itemId = item.get("item_id").longValue();
		if (dynamicItemsStore.containsKey(itemId)) {
			return false;
		}
		if (nonDynamicItemsStore.containsKey(itemId)) {
			return false;
		}
		return true;
	}

	private Completable resolveDynamicItem(long contractId, long typeId, long itemId) {
		EsiUrl esiUrl = new EsiUrl(String.format("/dogma/dynamic/items/%s/%s/",
			typeId,
			itemId
		));
		return RxUtil.toSingle(scrapeFetcher.fetch(esiUrl, null))
			.flatMapCompletable(response -> Completable.fromAction(() -> {
				int statusCode = response.code();
				if (statusCode == 520) {
					nonDynamicItemsStore.put(itemId, objectMapper.createObjectNode()
						.put("contract_id", contractId)
					);
					return;
				}
				if (statusCode == 200) {
					esiAbyssalItems.inc();
					ObjectNode dynamicItem = (ObjectNode) scrapeFetcher.decode(response);
					JsonNode lastModified = eveRefDataUtil.lastModified(response);
					saveDynamicItem(contractId, itemId, dynamicItem, lastModified);
					return;
				}
				log.warn(String.format("Unknown status code seen for contract %s item %s type %s: %s", contractId, itemId, typeId, statusCode));
			}));
	}

	private void saveDynamicItem(long contractId, long itemId, ObjectNode dynamicItem, JsonNode lastModified) {
		dynamicItem.put("item_id", itemId);
		dynamicItem.put("contract_id", contractId);
		dynamicItem.set("http_last_modified", lastModified);
		ArrayNode dogmaAttributes = (ArrayNode) dynamicItem.get("dogma_attributes");
		ArrayNode dogmaEffects = (ArrayNode) dynamicItem.get("dogma_effects");
		dynamicItem.remove("dogma_attributes");
		dynamicItem.remove("dogma_effects");
		dynamicItemsStore.put(itemId, dynamicItem);

		// Save sub values.
		for (JsonNode dogmaAttribute : dogmaAttributes) {
			saveDogmaAttribute(contractId, itemId, (ObjectNode) dogmaAttribute, lastModified);
		}
		for (JsonNode dogmaEffect : dogmaEffects) {
			saveDogmaEffect(contractId, itemId, (ObjectNode) dogmaEffect, lastModified);
		}
	}

	private void saveDogmaAttribute(long contractId, long itemId, ObjectNode dogmaAttribute, JsonNode lastModified) {
		dogmaAttribute.put("contract_id", contractId);
		dogmaAttribute.put("item_id", itemId);
		dogmaAttribute.set("http_last_modified", lastModified);
		long attributeId = dogmaAttribute.get("attribute_id").longValue();
		String id = String.format("%s-%s", itemId, attributeId);
		dogmaAttributesStore.put(id, dogmaAttribute);
	}

	private void saveDogmaEffect(long contractId, long itemId, ObjectNode dogmaEffect, JsonNode lastModified) {
		dogmaEffect.put("contract_id", contractId);
		dogmaEffect.put("item_id", itemId);
		dogmaEffect.set("http_last_modified", lastModified);
		long effectId = dogmaEffect.get("effect_id").longValue();
		String id = String.format("%s-%s", itemId, effectId);
		dogmaEffectsStore.put(id, dogmaEffect);
	}
}
