package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.MetaGroupScraper;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVMap;

/**
 * Fetches Abyssal traits for items.
 */
@Log4j2
public class ContractAbyssalFetcher {
	private static final int ABYSSAL_META_GROUP = 15;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected UniverseEsi universeEsi;

	@Inject
	protected MetaGroupScraper metaGroupScraper;

	@Setter
	private MVMap<Long, JsonNode> dynamicItemsStore;

	@Setter
	private MVMap<Long, JsonNode> nonDynamicItemsStore;

	@Setter
	private MVMap<String, JsonNode> dogmaEffectsStore;

	@Setter
	private MVMap<String, JsonNode> dogmaAttributesStore;

	@Inject
	protected ContractAbyssalFetcher() {}

	public Completable apply(long contractId, Flowable<ObjectNode> in) {
		return in.filter(item -> isItemNotSeen(item))
				.flatMap(item -> isPotentialAbyssalItem(item).flatMapPublisher(isAbyssal -> {
					return isAbyssal ? Flowable.just(item) : Flowable.empty();
				}))
				.flatMapCompletable(
						item -> {
							long itemId = item.get("item_id").longValue();
							long typeId = item.get("type_id").longValue();
							return resolveDynamicItem(contractId, typeId, itemId);
						},
						false,
						1);
	}

	public Single<Boolean> isPotentialAbyssalItem(ObjectNode item) {
		return Single.defer(() -> {
			if (JsonUtil.isNull(item.get("item_id"))) {
				return Single.just(false);
			}
			if (JsonUtil.isNullOrEmpty(item.get("type_id"))) {
				return Single.just(false);
			}
			if (!JsonUtil.toBoolean(item.get("is_included"))) {
				return Single.just(false);
			}
			if (JsonUtil.compareLongs(item.get("quantity"), 1) > 0) {
				return Single.just(false);
			}
			return verifyMetaGroup(item).flatMap(b -> {
				if (!b) {
					return Single.just(b);
				}
				return verifyType(item);
			});
		});
	}

	private Single<Boolean> verifyType(ObjectNode item) {
		var typeId = item.get("type_id").asInt();
		return universeEsi.getType(typeId).isEmpty().map(empty -> !empty);
	}

	private Single<Boolean> verifyMetaGroup(ObjectNode item) {
		var typeId = item.get("type_id").asInt();
		return metaGroupScraper
				.scrapeTypeIds(ABYSSAL_META_GROUP)
				.filter(id -> id == typeId)
				.isEmpty()
				.map(empty -> !empty);
	}

	private boolean isItemNotSeen(ObjectNode item) {
		long itemId = item.get("item_id").longValue();
		if (dynamicItemsStore.containsKey(itemId)) {
			return false;
		}
		if (nonDynamicItemsStore.containsKey(itemId)) { // @todo these need to be stored in the CSV to be loaded later.
			return false;
		}
		return true;
	}

	private Completable resolveDynamicItem(long contractId, long typeId, long itemId) {
		var esiUrl = EsiUrl.builder()
				.urlPath(String.format("/dogma/dynamic/items/%s/%s/", typeId, itemId))
				.build();
		return esiHelper
				.fetch(esiUrl)
				.flatMapCompletable(response -> Completable.fromAction(() -> {
					int statusCode = response.code();
					if (statusCode == 520) {
						nonDynamicItemsStore.put(
								itemId,
								objectMapper
										.createObjectNode()
										.put("item_id", itemId)
										.put("type_id", typeId)
										.put("contract_id", contractId));
						return;
					}
					if (statusCode == 200) {
						var dynamicItem = (ObjectNode) esiHelper.decode(response);
						var lastModified = okHttpHelper
								.getLastModified(response)
								.map(ZonedDateTime::toInstant)
								.orElse(null);
						saveDynamicItem(contractId, itemId, dynamicItem, lastModified);
						return;
					}
					log.warn(String.format(
							"Unknown status code seen for contract %s item %s type %s: %s",
							contractId, itemId, typeId, statusCode));
				}));
	}

	private void saveDynamicItem(long contractId, long itemId, ObjectNode dynamicItem, Instant lastModified) {
		dynamicItem.put("item_id", itemId);
		dynamicItem.put("contract_id", contractId);
		dynamicItem.put("http_last_modified", lastModified.toString());
		var dogmaAttributes = (ArrayNode) dynamicItem.get("dogma_attributes");
		var dogmaEffects = (ArrayNode) dynamicItem.get("dogma_effects");
		dynamicItem.remove("dogma_attributes");
		dynamicItem.remove("dogma_effects");
		dynamicItemsStore.put(itemId, dynamicItem);

		// Save sub values.
		for (var dogmaAttribute : dogmaAttributes) {
			saveDogmaAttribute(contractId, itemId, (ObjectNode) dogmaAttribute, lastModified);
		}
		for (var dogmaEffect : dogmaEffects) {
			saveDogmaEffect(contractId, itemId, (ObjectNode) dogmaEffect, lastModified);
		}
	}

	private void saveDogmaAttribute(long contractId, long itemId, ObjectNode dogmaAttribute, Instant lastModified) {
		dogmaAttribute.put("contract_id", contractId);
		dogmaAttribute.put("item_id", itemId);
		dogmaAttribute.put("http_last_modified", lastModified.toString());
		var attributeId = dogmaAttribute.get("attribute_id").longValue();
		var id = String.format("%s-%s", itemId, attributeId);
		dogmaAttributesStore.put(id, dogmaAttribute);
	}

	private void saveDogmaEffect(long contractId, long itemId, ObjectNode dogmaEffect, Instant lastModified) {
		dogmaEffect.put("contract_id", contractId);
		dogmaEffect.put("item_id", itemId);
		dogmaEffect.put("http_last_modified", lastModified.toString());
		var effectId = dogmaEffect.get("effect_id").longValue();
		var id = String.format("%s-%s", itemId, effectId);
		dogmaEffectsStore.put(id, dogmaEffect);
	}
}
