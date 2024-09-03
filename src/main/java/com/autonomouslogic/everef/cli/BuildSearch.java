package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.SearchJsonEntry;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;

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
	protected S3Util s3Util;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	@Named("static")
	protected S3AsyncClient s3Client;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected TempFiles tempFiles;

	private S3Url staticUrl;

	private final Duration cacheControlMaxAge = Configs.STATIC_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected BuildSearch() {}

	@Inject
	protected void init() {
		staticUrl = (S3Url) urlParser.parse(Configs.STATIC_PATH.getRequired());
	}

	public Completable run() {
		return refDataUtil
				.loadLatestRefData()
				.flatMapPublisher(this::buildSearch)
				.compose(cleanEntries())
				// .sorted(Ordering.natural().onResultOf(SearchJsonEntry::getText))
				.compose(writeToFile())
				.flatMapCompletable(this::uploadFile);
	}

	private Flowable<SearchJsonEntry> buildSearch(LoadedRefData loadedRefData) {
		return Flowable.fromStream(Streams.concat(
				loadedRefData.getAllTypes().flatMap(pair -> inventoryType(pair.getValue(), loadedRefData)),
				loadedRefData.getAllMarketGroups().flatMap(pair -> marketGroup(pair.getValue(), loadedRefData)),
				loadedRefData.getAllCategories().flatMap(pair -> inventoryCategory(pair.getValue(), loadedRefData)),
				loadedRefData.getAllGroups().flatMap(pair -> inventoryGroup(pair.getValue(), loadedRefData))));
	}

	private Stream<SearchJsonEntry> inventoryType(InventoryType type, LoadedRefData loadedRefData) {
		//		if (!item.getPublished() || item.getMarketGroupId() == null) {
		//			return Stream.empty();
		//		}

		String typeName;
		if (type.getMarketGroupId() != null) {
			typeName = Optional.ofNullable(getRootMarketGroup(type, loadedRefData))
					.flatMap(g -> Optional.ofNullable(g.getName().get("en")))
					.orElse("Inventory type");
		} else if (type.getGroupId() != null) {
			loadedRefData.getGroup(type.getGroupId()).getName().get("en");
			typeName = Optional.ofNullable(getRootMarketGroup(type, loadedRefData))
					.flatMap(g -> Optional.ofNullable(g.getName().get("en")))
					.orElse("Inventory type");
		} else {
			return Stream.empty();
		}
		var searchEntry = SearchJsonEntry.builder()
				.id(type.getTypeId())
				.link("/types/" + type.getTypeId())
				.type(typeName)
				.build();
		return type.getName().keySet().stream()
				.filter(lang -> lang.equals("en")) // only English for now, otherwise the search file becomes massive
				.map(lang ->
						searchEntry.toBuilder().text(type.getName().get(lang)).build());
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

	private Stream<SearchJsonEntry> marketGroup(MarketGroup group, LoadedRefData loadedRefData) {
		var text = marketGroupChain(group, loadedRefData).collect(Collectors.joining(" > "));
		return Stream.of(SearchJsonEntry.builder()
				.text(text)
				.id(group.getMarketGroupId())
				.link("/market-groups/" + group.getMarketGroupId())
				.type("Market group")
				.build());
	}

	private Stream<String> marketGroupChain(MarketGroup group, LoadedRefData loadedRefData) {
		var name = group.getName().get("en");
		if (group.getParentGroupId() == null) {
			return Stream.of(name);
		}
		var parentGroup = loadedRefData.getMarketGroup(group.getParentGroupId());
		if (parentGroup == null) {
			return Stream.of(name);
		}
		return Stream.concat(marketGroupChain(parentGroup, loadedRefData), Stream.of(name));
	}

	private Stream<SearchJsonEntry> inventoryCategory(InventoryCategory category, LoadedRefData loadedRefData) {
		return Stream.of(SearchJsonEntry.builder()
				.text(category.getName().get("en"))
				.id(category.getCategoryId())
				.link("/categories/" + category.getCategoryId())
				.type("Inventory category")
				.build());
	}

	private Stream<SearchJsonEntry> inventoryGroup(InventoryGroup group, LoadedRefData loadedRefData) {
		return Stream.of(SearchJsonEntry.builder()
				.text(group.getName().get("en"))
				.id(group.getGroupId())
				.link("/groups/" + group.getGroupId())
				.type("Inventory group")
				.build());
	}

	private FlowableTransformer<SearchJsonEntry, SearchJsonEntry> cleanEntries() {
		return new FlowableTransformer<>() {
			@Override
			public @NonNull Publisher<SearchJsonEntry> apply(@NonNull Flowable<SearchJsonEntry> upstream) {
				return upstream.filter(e -> e.getText() != null)
						.map(e -> e.toBuilder().text(e.getText().trim()).build())
						.filter(e -> !e.getText().isEmpty());
			}
		};
	}

	private FlowableTransformer<SearchJsonEntry, File> writeToFile() {
		return new FlowableTransformer<>() {
			@Override
			public @NonNull Publisher<File> apply(@NonNull Flowable<SearchJsonEntry> upstream) {
				return Flowable.defer(() -> {
					var file = tempFiles.tempFile("search", ".json").toFile();
					log.debug("Preparing to output to {}", file);
					var generator =
							objectMapper.writer().createGenerator(new FileOutputStream(file), JsonEncoding.UTF8);
					var counter = new AtomicInteger();
					generator.writeStartArray();
					return upstream.doOnNext(entry -> {
								generator.writeObject(entry);
								counter.incrementAndGet();
							})
							.doOnComplete(() -> {
								generator.writeEndArray();
								generator.close();
								log.info("Wrote {} entries to {}", counter.get(), file);
							})
							.ignoreElements()
							.andThen(Flowable.just(file));
				});
			}
		};
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	private Completable uploadFile(File outputFile) {
		return Completable.defer(() -> {
			var path = staticUrl.resolve("search.json");
			var put = s3Util.putObjectRequest(outputFile.length(), path, "application/json", cacheControlMaxAge);
			log.info(String.format("Uploading search file to %s", path));
			return s3Adapter.putObject(put, outputFile, s3Client).ignoreElement();
		});
	}
}
