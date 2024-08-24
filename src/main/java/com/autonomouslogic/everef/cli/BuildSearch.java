package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.model.SearchJsonEntry;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.refdata.MetaGroup;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

/**
 * Builds the JSON for UI search.
 * Will go away once there's an API.
 */
@Log4j2
public class BuildSearch implements Command {
	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected BuildSearch() {}

	public Completable run() {
		return refDataUtil
				.loadLatestRefData()
				.flatMapPublisher(this::buildSearch)
				.map(e -> e.toBuilder().text(e.getText().trim()).build())
				.filter(e -> !e.getText().isEmpty())
				.sorted(Ordering.natural().onResultOf(SearchJsonEntry::getText))
				.toList()
				.flatMapCompletable(entries -> Completable.fromAction(() -> {
					var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entries);
					log.info("Writing {} bytes", json.length());
					IOUtils.write(json, new FileOutputStream("/tmp/search.json"), StandardCharsets.UTF_8);
				}));
	}

	private Flowable<SearchJsonEntry> buildSearch(LoadedRefData loadedRefData) {
		return Flowable.fromStream(Streams.concat(
				loadedRefData.getAllTypes().flatMap(type -> inventoryType(type.getValue(), loadedRefData))));
	}

	private Stream<SearchJsonEntry> inventoryType(InventoryType item, LoadedRefData loadedRefData) {
		if (!item.getPublished() || item.getMarketGroupId() == null) {
			return Stream.empty();
		}

		String typeName;
		if (item.getMarketGroupId() != null) {
			typeName = Optional.ofNullable(getRootMarketGroup(item, loadedRefData))
					.flatMap(g -> Optional.ofNullable(g.getName().get("en")))
					.orElse("Inventory type");
		} else if (item.getGroupId() != null) {
			loadedRefData.getGroup(item.getGroupId()).getName().get("en");
			typeName = Optional.ofNullable(getRootMarketGroup(item, loadedRefData))
					.flatMap(g -> Optional.ofNullable(g.getName().get("en")))
					.orElse("Inventory type");
		} else {
			return Stream.empty();
		}
		var searchEntry = SearchJsonEntry.builder()
				.id(item.getTypeId())
				.link("/types/" + item.getTypeId())
				.type(typeName)
				.build();
		return item.getName().keySet().stream()
				.filter(lang -> lang.equals("en")) // only English for now
				.map(lang ->
						searchEntry.toBuilder().text(item.getName().get(lang)).build());

		//		return inventoryTypeDao.getAllInventoryTypes()
		//			.filter(type -> type.getName("en") != null)
		////			.filter(type -> type.getPublished())
		////			.filter(type -> type.getMarketGroupId() != null)
		//			.map(type -> {
		//				SearchJsonEntry entry = new SearchJsonEntry();
		//				entry.text = type.getName("en");
		//				entry.id = type.getTypeId();
		//				entry.link = linkHelper.link(type);
		//				if (type.getMarketGroup() != null) {
		//					entry.type = marketGroupDao.chain(type).get(0).getName().get("en");
		//				}
		//				else if (type.getInventoryGroup() != null) {
		//					entry.type = type.getInventoryGroup().get().getCategory().get().getName().get("en");
		//				}
		//				return entry;
		//			});
	}

	private MarketGroup getRootMarketGroup(InventoryType type, LoadedRefData loadedRefData) {
		if (type.getMarketGroupId() == null) {
			return null;
		}
		var marketGroup = loadedRefData.getMarketGroup(type.getMarketGroupId());
		if (marketGroup == null) {
			return null;
		}
		return getRootMarketGroup(marketGroup, loadedRefData);
	}

	private MarketGroup getRootMarketGroup(MarketGroup marketGroup, LoadedRefData loadedRefData) {
		if (marketGroup.getParentGroupId() == null) {
			return marketGroup;
		}
		var parentGroup = loadedRefData.getMarketGroup(marketGroup.getParentGroupId());
		if (parentGroup == null) {
			return marketGroup;
		}
		return getRootMarketGroup(parentGroup, loadedRefData);
	}

	private Flowable<SearchJsonEntry> marketGroup(
			ReferenceEntry refEntry, SearchJsonEntry searchEntry, MarketGroup item) {
		return Flowable.empty();
		//		return marketGroupDao.getAllMarketGroups()
		//			.map(group -> {
		//				SearchJsonEntry entry = new SearchJsonEntry();
		//				entry.text = marketGroupDao.chain(group).stream()
		//					.map(g -> g.getName().get("en"))
		//					.collect(Collectors.joining(" > "));
		//				entry.id = group.getMarketGroupId();
		//				entry.link = linkHelper.link(group);
		//				entry.type = "Market group";
		//				return entry;
		//			});
	}

	private Flowable<SearchJsonEntry> inventoryCategory(
			ReferenceEntry refEntry, SearchJsonEntry searchEntry, InventoryCategory item) {
		return Flowable.empty();
		//		return inventoryGroupDao.getAllCategories()
		//			.map(group -> {
		//				SearchJsonEntry entry = new SearchJsonEntry();
		//				entry.text = group.getName().get("en");
		//				entry.id = group.getCategoryId();
		//				entry.link = linkHelper.link(group);
		//				entry.type = "Inventory category";
		//				return entry;
		//			});
	}

	private Maybe<SearchJsonEntry> inventoryGroup(
			ReferenceEntry refEntry, SearchJsonEntry searchEntry, InventoryGroup item) {
		return Maybe.empty();
		//		return inventoryGroupDao.getAllGroups()
		//			.map(group -> {
		//				SearchJsonEntry entry = new SearchJsonEntry();
		//				entry.text = group.getName().get("en");
		//				entry.id = group.getGroupID();
		//				entry.link = linkHelper.link(group);
		//				entry.type = "Inventory group";
		//				return entry;
		//			});
	}

	private Flowable<SearchJsonEntry> metaGroup(ReferenceEntry refEntry, SearchJsonEntry searchEntry, MetaGroup item) {
		return Flowable.empty();
		//		return inventoryTypeDao.getAllMetaGroups()
		//			.map(group -> {
		//				SearchJsonEntry entry = new SearchJsonEntry();
		//				entry.text = group.getMetaGroupName();
		//				entry.id = group.getMetaGroupId();
		//				entry.link = linkHelper.metaGroup(group.getMetaGroupId());
		//				entry.type = "Meta group";
		//				return entry;
		//			});
	}
}
