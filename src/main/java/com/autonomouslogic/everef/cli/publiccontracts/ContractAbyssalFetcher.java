package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.openapi.refdata.apis.RefdataApi;
import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

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
	protected RefdataApi refdataApi;

	@Setter
	private Map<Long, JsonNode> dynamicItemsStore;

	@Setter
	private Map<Long, JsonNode> nonDynamicItemsStore;

	@Setter
	private Map<String, JsonNode> dogmaEffectsStore;

	@Setter
	private Map<String, JsonNode> dogmaAttributesStore;

	List<Long> abyssalTypeIds;

	@Inject
	protected ContractAbyssalFetcher() {}

	public Completable apply(long contractId, Flowable<ObjectNode> in) {
		return Completable.defer(() -> {
			abyssalTypeIds = refdataApi.getMetaGroup(15L).join().getTypeIds();
			log.debug("Loaded {} abyssal type IDs", abyssalTypeIds.size());
			return in.flatMap(item -> isPotentialAbyssalItem(item).flatMapPublisher(isAbyssal -> {
						return isAbyssal ? Flowable.just(item) : Flowable.empty();
					}))
					.filter(item -> isItemNotSeen(item))
					.flatMapCompletable(
							item -> {
								long itemId = item.get("item_id").longValue();
								long typeId = item.get("type_id").longValue();
								return resolveDynamicItem(contractId, typeId, itemId);
							},
							false,
							1);
		});
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
			if (!abyssalTypeIds.contains(item.get("type_id").asLong())) {
				return Single.just(false);
			}
			return verifyType(item);
		});
	}

	private Single<Boolean> verifyType(ObjectNode item) {
		var typeId = item.get("type_id").asInt();
		return universeEsi.getType(typeId).isEmpty().map(empty -> !empty);
	}

	private boolean isItemNotSeen(ObjectNode item) {
		long dynamicId = ContractsFileBuilder.DYNAMIC_ITEM_ID.apply(item);
		if (dynamicItemsStore.containsKey(dynamicId)) {
			return false;
		}
		long nonDynamicId = ContractsFileBuilder.NON_DYNAMIC_ITEM_ID.apply(item);
		return !nonDynamicItemsStore.containsKey(nonDynamicId);
	}

	private Completable resolveDynamicItem(long contractId, long typeId, long itemId) {
		var esiUrl = EsiUrl.builder()
				.urlPath(String.format("/dogma/dynamic/items/%s/%s/", typeId, itemId))
				.build();
		return esiHelper
				.fetch(esiUrl)
				.toFlowable()
				.compose(esiHelper.standardErrorHandling(esiUrl))
				.flatMapCompletable(response -> Completable.fromAction(() -> {
					int statusCode = response.code();
					if (statusCode == 520) {
						var nonDynamicItem = objectMapper
								.createObjectNode()
								.put("item_id", itemId)
								.put("type_id", typeId)
								.put("contract_id", contractId);
						nonDynamicItemsStore.put(
								ContractsFileBuilder.NON_DYNAMIC_ITEM_ID.apply(nonDynamicItem), nonDynamicItem);
						return;
					}
					if (statusCode == 200) {
						var dynamicItem = (ObjectNode) esiHelper.decodeResponse(response);
						var lastModified = okHttpHelper
								.getLastModified(response)
								.map(ZonedDateTime::toInstant)
								.orElse(null);
						saveDynamicItem(contractId, itemId, dynamicItem, lastModified);
					} else {
						log.warn(
								"Unknown status code seen for contract {} item {} type {}: {}",
								contractId,
								itemId,
								typeId,
								statusCode);
					}
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
		dynamicItemsStore.put(ContractsFileBuilder.DYNAMIC_ITEM_ID.apply(dynamicItem), dynamicItem);

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
		dogmaAttributesStore.put(ContractsFileBuilder.DOGMA_ATTRIBUTE_ID.apply(dogmaAttribute), dogmaAttribute);
	}

	private void saveDogmaEffect(long contractId, long itemId, ObjectNode dogmaEffect, Instant lastModified) {
		dogmaEffect.put("contract_id", contractId);
		dogmaEffect.put("item_id", itemId);
		dogmaEffect.put("http_last_modified", lastModified.toString());
		dogmaEffectsStore.put(ContractsFileBuilder.DOGMA_EFFECT_ID.apply(dogmaEffect), dogmaEffect);
	}
}
