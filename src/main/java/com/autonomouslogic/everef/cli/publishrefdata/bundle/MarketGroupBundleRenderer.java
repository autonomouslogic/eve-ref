package com.autonomouslogic.everef.cli.publishrefdata.bundle;

import com.autonomouslogic.everef.cli.publishrefdata.RootMarketGroupIndexRenderer;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Renders the basic objects in the reference data collections.
 */
@Log4j2
public class MarketGroupBundleRenderer extends BundleRenderer {
	@Inject
	protected Provider<RootMarketGroupIndexRenderer> rootMarketGroupIndexRendererProvider;

	@Inject
	protected MarketGroupBundleRenderer() {}

	@Override
	protected Flowable<Pair<String, JsonNode>> renderInternal() {
		return Flowable.concat(
				createRootMarketGroupsBundle().toFlowable(),
				Flowable.fromIterable(getMarketGroupsMap().keySet()).flatMapMaybe(this::createMarketGroupBundle));
	}

	private Maybe<Pair<String, JsonNode>> createRootMarketGroupsBundle() {
		var rootGroupIds = rootMarketGroupIndexRendererProvider
				.get()
				.setDataStore(getDataStore())
				.render()
				.map(Pair::getRight)
				.flatMap(json -> Flowable.fromIterable(Lists.newArrayList(json.elements())))
				.map(JsonNode::asLong)
				.toList()
				.blockingGet();

		var bundleJson = generateBundle(rootGroupIds, List.of());

		if (bundleJson.isPresent()) {
			var path = refDataUtil.subPath("market_groups/root/bundle");
			return Maybe.just(Pair.of(path, bundleJson.get()));
		}
		return Maybe.empty();
	}

	private Maybe<Pair<String, JsonNode>> createMarketGroupBundle(long groupId) {
		var groupJson = getMarketGroupsMap().get(groupId);
		if (groupJson == null) {
			log.warn("Market group {} not found", groupId);
			return Maybe.empty();
		}
		var group = objectMapper.convertValue(groupJson, MarketGroup.class);
		var typeIds = group.getTypeIds();
		var childGroupIds = group.getChildMarketGroupIds();
		if ((typeIds == null || typeIds.isEmpty()) && (childGroupIds == null || childGroupIds.isEmpty())) {
			return Maybe.empty();
		}

		var bundleJson = generateBundle(childGroupIds, typeIds);

		if (bundleJson.isPresent()) {
			bundleJson.get().withObject("market_groups").put(Long.toString(groupId), groupJson);
			var path = refDataUtil.subPath("market_groups", group.getMarketGroupId()) + "/bundle";
			return Maybe.just(Pair.of(path, bundleJson.get()));
		}
		return Maybe.empty();
	}

	private Optional<ObjectNode> generateBundle(List<Long> childGroupIds, List<Long> typeIds) {
		var bundleJson = objectMapper.createObjectNode();
		var typesJson = objectMapper.createObjectNode();
		var marketGroupsJson = objectMapper.createObjectNode();
		var attributesJson = objectMapper.createObjectNode();
		var unitsJson = objectMapper.createObjectNode();
		var iconsJson = objectMapper.createObjectNode();
		var metaGroupsJson = objectMapper.createObjectNode();

		unitsJson.set("133", unitsMap.get(133L)); // ISK for the market price display.

		if (typeIds != null) {
			for (long typeId : typeIds) {
				var typeJson = getTypesMap().get(typeId);
				var type = objectMapper.convertValue(typeJson, InventoryType.class);
				if (typeJson != null) {
					typesJson.set(Long.toString(typeId), typeJson);
					bundleDogmaAttributes(type, attributesJson);
					bundleDogmaAttributesUnits(attributesJson, unitsJson); // @todo probably duplicate
				}
			}
		}

		bundleDogmaAttributesUnits(attributesJson, unitsJson);
		bundleDogmaAttributesIcons(attributesJson, iconsJson);
		bundleTypesMetaGroups(typesJson, metaGroupsJson);

		if (childGroupIds != null) {
			for (var childGroupId : childGroupIds) {
				var json = getMarketGroupsMap().get(childGroupId);
				if (json != null) {
					marketGroupsJson.set(Long.toString(childGroupId), json);
				}
			}
		}

		var valid = false;
		if (!typesJson.isEmpty()) {
			bundleJson.set("types", typesJson);
			valid = true;
		}
		if (!marketGroupsJson.isEmpty()) {
			bundleJson.set("market_groups", marketGroupsJson);
			valid = true;
		}
		if (!attributesJson.isEmpty()) {
			bundleJson.set("dogma_attributes", attributesJson);
			valid = true;
		}
		if (!unitsJson.isEmpty()) {
			bundleJson.set("units", unitsJson);
			valid = true;
		}
		if (!iconsJson.isEmpty()) {
			bundleJson.set("icons", iconsJson);
			valid = true;
		}
		if (!metaGroupsJson.isEmpty()) {
			bundleJson.set("meta_groups", metaGroupsJson);
			valid = true;
		}

		if (valid) {
			return Optional.of(bundleJson);
		} else {
			return Optional.empty();
		}
	}
}
