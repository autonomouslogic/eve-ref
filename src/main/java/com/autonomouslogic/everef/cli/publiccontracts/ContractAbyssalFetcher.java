package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.openapi.refdata.api.RefdataApi;
import com.autonomouslogic.everef.openapi.refdata.invoker.ApiException;
import com.autonomouslogic.everef.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
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
	private static final long ABYSSAL_META_GROUP = 15;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected UniverseEsi universeEsi;

	@Inject
	protected RefdataApi refdataApi;

	@Setter
	private Map<Long, JsonNode> dynamicItemsStore;

	@Setter
	private Map<String, JsonNode> dogmaEffectsStore;

	@Setter
	private Map<String, JsonNode> dogmaAttributesStore;

	private List<Long> abyssalTypeIds;

	@Inject
	protected ContractAbyssalFetcher() {}

	/**
	 * For cached contracts where items are already known, retry fetching dogma for abyssal items
	 * that are missing dynamic data (e.g., a prior dogma call failed). Skips verifyType since the
	 * item was already stored in the archive and its type is known to be valid. Does nothing if no
	 * candidates are found, avoiding the meta groups API call entirely for non-abyssal contracts.
	 */
	public void retryMissingDogmaForCachedItems(long contractId, List<ObjectNode> cachedItems) {
		// Regular items never have item_id set; abyssal items always do. This filters non-abyssal
		// items without needing to call initAbyssalTypes() (which requires a network request).
		var candidates = cachedItems.stream()
				.filter(item -> !JsonUtil.isNull(item.get("item_id")))
				.filter(item -> !JsonUtil.isNullOrEmpty(item.get("type_id")))
				.filter(item -> JsonUtil.toBoolean(item.get("is_included")))
				.filter(item -> JsonUtil.compareLongs(item.get("quantity"), 1) <= 0)
				.filter(this::isItemNotSeen)
				.toList();
		if (candidates.isEmpty()) {
			return;
		}
		try {
			initAbyssalTypes();
		} catch (ApiException e) {
			log.warn(
					"Failed to load abyssal type IDs, skipping dogma retry for contract {}: {}",
					contractId,
					e.getMessage());
			return;
		}
		Flowable.fromIterable(candidates)
				.filter(item -> abyssalTypeIds.contains(item.get("type_id").asLong()))
				.flatMapCompletable(
						item -> {
							long itemId = item.get("item_id").asLong();
							long typeId = item.get("type_id").asLong();
							return resolveDynamicItem(contractId, typeId, itemId);
						},
						false,
						1)
				.blockingAwait();
	}

	public Completable apply(long contractId, Flowable<ObjectNode> in) {
		return Completable.defer(() -> {
			initAbyssalTypes();
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
		var typeOpt = universeEsi.getType(typeId);
		return Single.just(typeOpt.isPresent());
	}

	private boolean isItemNotSeen(ObjectNode item) {
		long dynamicId = ContractsFileBuilder.DYNAMIC_ITEM_ID.apply(item);
		return !dynamicItemsStore.containsKey(dynamicId);
	}

	private Completable resolveDynamicItem(long contractId, long typeId, long itemId) {
		var esiUrl = EsiUrl.builder()
				.urlPath(String.format("/dogma/dynamic/items/%s/%s/", typeId, itemId))
				.build();
		var r = esiHelper.fetch(esiUrl);
		return Completable.fromAction(() -> {
					int statusCode = r.code();
					if (statusCode == 200) {
						var dynamicItem = (ObjectNode) esiHelper.decodeResponse(r);
						var lastModified = okHttpWrapper
								.getLastModified(r)
								.map(ZonedDateTime::toInstant)
								.orElse(null);
						saveDynamicItem(contractId, itemId, dynamicItem, lastModified);
					} else {
						var msg = String.format("Failed to fetch dynamic item: %d", statusCode);
						log.warn(
								"Failed to fetch dynamic item for contract {} item {} type {}: {}",
								contractId,
								itemId,
								typeId,
								statusCode);
						if (statusCode == 520) {
							log.debug(
									"Dogma data not yet available (520) for contract {} item {} type {}, will retry next run",
									contractId,
									itemId,
									typeId);
						}
						Sentry.captureException(new RuntimeException(msg), (io.sentry.ScopeCallback) scope -> {
							scope.setLevel(SentryLevel.WARNING);
							scope.setExtra("contract_id", String.valueOf(contractId));
							scope.setExtra("item_id", String.valueOf(itemId));
							scope.setExtra("type_id", String.valueOf(typeId));
						});
					}
				})
				.doFinally(() -> r.close());
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

	private void initAbyssalTypes() throws ApiException {
		if (abyssalTypeIds != null) {
			return;
		}
		abyssalTypeIds = refdataApi.getMetaGroup(ABYSSAL_META_GROUP).getTypeIds();
		log.trace("Loaded {} abyssal type IDs", abyssalTypeIds.size());
	}
}
