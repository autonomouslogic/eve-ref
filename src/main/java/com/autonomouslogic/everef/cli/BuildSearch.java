package com.autonomouslogic.everef.cli;

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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Flow;

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

//	@Inject
//	protected UrlParser urlParser;
//
//	@Inject
//	protected ObjectMapper objectMapper;
//
//	@Inject
//	protected S3Adapter s3Adapter;
//
//	@Inject
//	protected S3Util s3Util;
//
//	@Inject
//	@Named("data")
//	protected S3AsyncClient s3Client;
//
//	@Inject
//	protected OkHttpClient okHttpClient;
//
//	@Inject
//	protected OkHttpHelper okHttpHelper;
//
//	@Inject
//	protected TempFiles tempFiles;
//
//	@Inject
//	protected DataIndexHelper dataIndexHelper;
//
//	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
//	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
//
//	private S3Url dataPath;
//	private HttpUrl dataUrl;
//	private HttpUrl hoboUrl;

	@Inject
	protected BuildSearch() {}

	public Completable run() {
		return refDataUtil
			.downloadLatestReferenceData()
			.flatMapPublisher(file -> refDataUtil.parseReferenceDataArchive(file))
			.flatMap(this::handleEntry, false, Runtime.getRuntime().availableProcessors())
			.sorted(Ordering.natural().onResultOf(SearchJsonEntry::getText))
			.ignoreElements();
	}

	private Flowable<SearchJsonEntry> handleEntry(ReferenceEntry refEntry) {
		return Flowable.defer(() -> {
			var search = SearchJsonEntry.builder()
				.id(refEntry.getId())
				.link(url(refEntry))
				.build();
			return handle(refEntry, search);
		});
	}

	private Flowable<SearchJsonEntry> handle(ReferenceEntry refEntry, SearchJsonEntry searchEntry) {
		switch (refEntry.getType()) {
			case "meta":
				return Flowable.empty();
			case "inventoryType":
				return inventoryType(refEntry, searchEntry, parse(refEntry, InventoryType.class));
			case "marketGroup":
				return marketGroup(refEntry, searchEntry, parse(refEntry, MarketGroup.class));
			case "categories":
				return inventoryCategory(refEntry, searchEntry, parse(refEntry, InventoryCategory.class));
			case "inventoryGroup":
				return inventoryGroup(refEntry, searchEntry, parse(refEntry, InventoryGroup.class));
			case "metaGroup":
				return metaGroup(refEntry, searchEntry, parse(refEntry, MetaGroup.class));
			default:
				log.warn("Unknown reference data entry type {}", refEntry.getType());
				return Flowable.empty();
		}
	}

	private Flowable<SearchJsonEntry> inventoryType(ReferenceEntry refEntry, SearchJsonEntry searchEntry, InventoryType item) {
		return Flowable.defer(() -> {
			if (item.getMarketGroupId() != null) {
//				entry.type = marketGroupDao.chain(type).get(0).getName().get("en");
				searchEntry = searchEntry.toBuilder().type(Integer.toString(item.getMarketGroupId())).build();
			}
			else if (item.getGroupId() != null) {
//				entry.type = type.getInventoryGroup().get().getCategory().get().getName().get("en");
			}


			Flowable.fromIterable(item.getName().keySet())

			if (item.getName().get("en") == null) {
				return Flowable.empty();
			}
		});
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

	private Flowable<SearchJsonEntry> marketGroup(ReferenceEntry refEntry, SearchJsonEntry searchEntry, MarketGroup item) {
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

	private Flowable<SearchJsonEntry> inventoryCategory(ReferenceEntry refEntry, SearchJsonEntry searchEntry, InventoryCategory item) {
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

	private Maybe<SearchJsonEntry> inventoryGroup(ReferenceEntry refEntry, SearchJsonEntry searchEntry, InventoryGroup item) {
		return Flowable.empty();
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

	private String url(ReferenceEntry refEntry) {
		return String.format("/%s/%s", refEntry.getType(), refEntry.getId());
	}

	@SneakyThrows
	private <T> T parse(ReferenceEntry refEntry, Class<T> clazz) {
		return objectMapper.readValue(refEntry.getContent(), clazz);
	}
}
