package com.autonomouslogic.everef.util;

import static com.autonomouslogic.everef.util.ArchivePathFactory.REFERENCE_DATA;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import com.autonomouslogic.everef.openapi.refdata.api.RefdataApi;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.DogmaEffect;
import com.autonomouslogic.everef.refdata.Icon;
import com.autonomouslogic.everef.refdata.InventoryCategory;
import com.autonomouslogic.everef.refdata.InventoryGroup;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.refdata.MetaGroup;
import com.autonomouslogic.everef.refdata.Mutaplasmid;
import com.autonomouslogic.everef.refdata.RefDataMeta;
import com.autonomouslogic.everef.refdata.Region;
import com.autonomouslogic.everef.refdata.Schematic;
import com.autonomouslogic.everef.refdata.Skill;
import com.autonomouslogic.everef.refdata.Unit;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;

@Singleton
@Log4j2
public class RefDataUtil {
	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	@Named("yaml")
	protected ObjectMapper yamlMapper;

	@Inject
	protected RefdataApi refdataApi;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected Provider<LoadedRefData> loadedRefDataProvider;

	private List<RefDataConfig> cachedConfigs;

	@Inject
	protected RefDataUtil() {}

	public Single<File> downloadLatestReferenceData() {
		return Single.defer(() -> {
			var dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
			var url = dataBaseUrl.resolve(REFERENCE_DATA.createLatestPath());
			var file = tempFiles.tempFile("refdata", ".tar.xz").toFile();
			try (var response = okHttpWrapper.download(url.toString(), file)) {
				if (response.code() != 200) {
					return Single.error(new RuntimeException("Failed downloading reference data"));
				}
				return Single.just(file);
			}
		});
	}

	public Single<RefDataMeta> getMetaFromRefDataFile(File file) {
		return CompressUtil.loadArchive(file)
				.filter(e -> e.getKey().getName().equals("meta.json"))
				.map(e -> objectMapper.readValue(e.getRight(), RefDataMeta.class))
				.firstOrError();
	}

	public Flowable<ReferenceEntry> parseReferenceDataArchive(@NonNull File file) {
		return CompressUtil.loadArchive(file)
				.flatMap(pair -> {
					var filename = pair.getKey().getName();
					var type = FilenameUtils.getBaseName(filename);
					if (!filename.endsWith(".json")) {
						log.debug("Skipping non-JSON file {}", filename);
						return Flowable.empty();
					}
					log.debug("Parsing {}", filename);
					if (filename.equals("meta.json")) {
						return Flowable.just(createEntry(type, pair.getRight()));
					}
					var json = (ObjectNode) objectMapper.readTree(pair.getRight());
					var index = new ArrayList<Long>();
					var fileEntries = Flowable.fromIterable(() -> json.fields())
							.map(entry -> {
								var id = Long.parseLong(entry.getKey());
								index.add(id);
								var content = objectMapper.writeValueAsBytes(entry.getValue());
								return createEntry(type, id, content);
							})
							.doOnComplete(() -> log.debug("Finished parsing {}", filename));
					return fileEntries;
				})
				.doFinally(() -> file.delete());
	}

	public ReferenceEntry createEntry(@NonNull String type, @NonNull Long id, @NonNull byte[] content) {
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				type, id, subPath(type, id), content, Base64.encodeBase64String(md5), Hex.encodeHexString(md5));
	}

	public ReferenceEntry createEntry(@NonNull String type, @NonNull byte[] content) {
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				type, null, subPath(type), content, Base64.encodeBase64String(md5), Hex.encodeHexString(md5));
	}

	public ReferenceEntry createEntryForPath(@NonNull String path, @NonNull byte[] content) {
		var refDataUrl = (S3Url) urlParser.parse(Configs.REFERENCE_DATA_PATH.getRequired());
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				null,
				null,
				refDataUrl.getPath() + path,
				content,
				Base64.encodeBase64String(md5),
				Hex.encodeHexString(md5));
	}

	public String subPath(@NonNull String type, @NonNull Long id) {
		return subPath(type + "/" + id);
	}

	public String subPath(@NonNull String type) {
		return type;
	}

	public List<RefDataConfig> loadReferenceDataConfig() {
		if (cachedConfigs == null) {
			cachedConfigs = loadReferenceDataConfigInternal();
		}
		return cachedConfigs;
	}

	@SneakyThrows
	private List<RefDataConfig> loadReferenceDataConfigInternal() {
		var mapper = yamlMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		var type = mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, RefDataConfig.class);
		try (var in = ResourceUtil.loadResource("/refdata.yaml")) {
			Map<String, RefDataConfig> map = yamlMapper.readValue(in, type);
			return map.entrySet().stream()
					.map(entry -> {
						var config = entry.getValue();
						var id = entry.getKey();
						var outputFile = Optional.ofNullable(config.getOutputFile())
								.orElse(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey()));
						var outputStore =
								Optional.ofNullable(config.getOutputStore()).orElse(id);
						var builder = config.toBuilder()
								.id(id)
								.outputStore(outputStore)
								.outputFile(outputFile);
						return builder.build();
					})
					.toList();
		}
	}

	public RefDataConfig getSdeConfigForFilename(@NonNull String filename) {
		return getConfigForFilename(filename, RefDataConfig::getSde);
	}

	public RefDataConfig getEsiConfigForFilename(@NonNull String filename) {
		return getConfigForFilename(filename, RefDataConfig::getEsi);
	}

	public RefDataConfig getHoboleaksConfigForFilename(@NonNull String filename) {
		return getConfigForFilename(filename, RefDataConfig::getHoboleaks);
	}

	public RefDataConfig getConfigForFilename(
			@NonNull String filename, @NonNull Function<RefDataConfig, RefTypeConfig> typeConfigProvider) {
		for (RefDataConfig config : loadReferenceDataConfig()) {
			var type = typeConfigProvider.apply(config);
			if (type == null) {
				continue;
			}
			var file = type.getFile();
			if (file != null && file.equals(filename)) {
				return config;
			}
			var regex = type.getFileRegex();
			if (regex != null && regex.matcher(filename).matches()) {
				return config;
			}
		}
		return null;
	}

	@SneakyThrows
	public Flowable<Long> getAllTypeIdsForMarketGroup(long marketGroupId) {
		return Flowable.defer(() -> {
					var marketGroup = VirtualThreads.offload(() -> refdataApi.getMarketGroup(marketGroupId));
					var types = Optional.ofNullable(marketGroup.getTypeIds()).orElse(List.of());
					var children = Optional.ofNullable(marketGroup.getChildMarketGroupIds())
							.orElse(List.of());
					return Flowable.concatArray(
							Flowable.fromIterable(types),
							Flowable.fromIterable(children).flatMap(this::getAllTypeIdsForMarketGroup));
				})
				.distinct();
	}

	public Single<LoadedRefData> loadLatestRefData() {
		var loaded = loadedRefDataProvider.get();
		return downloadLatestReferenceData()
				.flatMapPublisher(this::parseReferenceDataArchive)
				.flatMapCompletable(
						refEntry -> Completable.fromAction(() -> {
							switch (refEntry.getType()) {
								case "categories":
									putToLoadedRefData(refEntry, InventoryCategory.class, loaded::putCategory);
									break;
								case "groups":
									putToLoadedRefData(refEntry, InventoryGroup.class, loaded::putGroup);
									break;
								case "market_groups":
									putToLoadedRefData(refEntry, MarketGroup.class, loaded::putMarketGroup);
									break;
								case "types":
									putToLoadedRefData(refEntry, InventoryType.class, loaded::putType);
									break;
								case "dogma_attributes":
									putToLoadedRefData(refEntry, DogmaAttribute.class, loaded::putDogmaAttribute);
									break;
								case "dogma_effects":
									putToLoadedRefData(refEntry, DogmaEffect.class, loaded::putDogmaEffect);
									break;
								case "meta_groups":
									putToLoadedRefData(refEntry, MetaGroup.class, loaded::putMetaGroup);
									break;
								case "mutaplasmids":
									putToLoadedRefData(refEntry, Mutaplasmid.class, loaded::putMutaplasmid);
									break;
								case "skills":
									putToLoadedRefData(refEntry, Skill.class, loaded::putSkill);
									break;
								case "units":
									putToLoadedRefData(refEntry, Unit.class, loaded::putUnit);
									break;
								case "blueprints":
									putToLoadedRefData(refEntry, Blueprint.class, loaded::putBlueprint);
									break;
								case "icons":
									putToLoadedRefData(refEntry, Icon.class, loaded::putIcon);
									break;
								case "regions":
									putToLoadedRefData(refEntry, Region.class, loaded::putRegion);
									break;
								case "schematics":
									putToLoadedRefData(refEntry, Schematic.class, loaded::putSchematic);
									break;
								case "meta":
								case "type_materials":
								case "type_dogma":
								case "masteries":
								case "type_bonus":
									// ignore.
									break;
								default:
									throw new IllegalStateException("Unknown ref data type: " + refEntry.getType());
							}
						}),
						false,
						Runtime.getRuntime().availableProcessors())
				.andThen(Single.just(loaded));
	}

	@SneakyThrows
	private <T> void putToLoadedRefData(ReferenceEntry refEntry, Class<T> clazz, BiConsumer<Long, T> putter) {
		T item = objectMapper.readValue(refEntry.getContent(), clazz);
		putter.accept(refEntry.getId(), item);
	}

	public OptionalDouble getTypeDogmaValue(InventoryType type, long dogmaAttributeId) {
		// @todo this exists in StoreHandlerHelper too
		var opt = Optional.ofNullable(type.getDogmaAttributes())
				.flatMap(attrs -> Optional.ofNullable(attrs.get(String.valueOf(dogmaAttributeId))))
				.flatMap(dogma -> Optional.ofNullable(dogma.getValue()));
		return opt.map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
	}

	public Optional<Double> getTypeDogmaValueBoxed(InventoryType type, long dogmaAttributeId) {
		return getTypeDogmaValue(type, dogmaAttributeId).stream().boxed().findFirst();
	}

	public OptionalDouble getTypeDogmaFirstValue(InventoryType type, long... dogmaAttributeIds) {
		for (long id : dogmaAttributeIds) {
			var opt = getTypeDogmaValue(type, id);
			if (opt.isPresent()) {
				return opt;
			}
		}
		return OptionalDouble.empty();
	}
}
