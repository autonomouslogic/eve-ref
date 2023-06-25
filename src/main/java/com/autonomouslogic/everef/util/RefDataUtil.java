package com.autonomouslogic.everef.util;

import static com.autonomouslogic.everef.util.ArchivePathFactory.REFERENCE_DATA;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.model.refdata.RefDataConfig;
import com.autonomouslogic.everef.model.refdata.RefTypeConfig;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;

@Singleton
@Log4j2
public class RefDataUtil {
	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	@Named("yaml")
	protected ObjectMapper yamlMapper;

	@Inject
	protected UrlParser urlParser;

	private List<RefDataConfig> cachedConfigs;

	@Inject
	protected RefDataUtil() {}

	public Single<File> downloadLatestReferenceData() {
		return Single.defer(() -> {
			var dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
			var url = dataBaseUrl + "/" + REFERENCE_DATA.createLatestPath();
			var file = tempFiles.tempFile("refdata", ".tar.xz").toFile();
			return okHttpHelper.download(url, file, okHttpClient).flatMap(response -> {
				if (response.code() != 200) {
					return Single.error(new RuntimeException("Failed downloading ESI"));
				}
				return Single.just(file);
			});
		});
	}

	public Flowable<ReferenceEntry> parseReferenceDataArchive(@NonNull File file) {
		return CompressUtil.loadArchive(file).flatMap(pair -> {
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
			var indexEntry = Flowable.defer(() -> {
				index.sort(Long::compareTo);
				log.debug("Creating {} index with {} entries", type, index.size());
				return Flowable.just(createEntry(type, objectMapper.writeValueAsBytes(index)));
			});
			var marketGroupRootIndex = !filename.equals("market_groups.json")
					? Flowable.<ReferenceEntry>empty()
					: Flowable.fromIterable(() -> json.fields())
							.map(e -> e.getValue())
							.filter(e -> e.get("parent_group_id") == null)
							.map(e -> e.get("market_group_id").asLong())
							.sorted()
							.toList()
							.flatMapPublisher(ind -> {
								return Flowable.just(
										createEntry("market_groups/root", objectMapper.writeValueAsBytes(ind)));
							});
			return Flowable.concat(fileEntries, indexEntry, marketGroupRootIndex);
		});
	}

	private ReferenceEntry createEntry(@NonNull String type, @NonNull Long id, @NonNull byte[] content) {
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				type, id, subPath(type, id), content, Base64.encodeBase64String(md5), Hex.encodeHexString(md5));
	}

	private ReferenceEntry createEntry(@NonNull String type, @NonNull byte[] content) {
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				type, null, subPath(type), content, Base64.encodeBase64String(md5), Hex.encodeHexString(md5));
	}

	private String subPath(@NonNull String type, @NonNull Long id) {
		return subPath(type + "/" + id);
	}

	private String subPath(@NonNull String type) {
		var refDataUrl = (S3Url) urlParser.parse(Configs.REFERENCE_DATA_PATH.getRequired());
		return refDataUrl.getPath() + type;
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
			if (type.getFile().equals(filename)) {
				return config;
			}
		}
		return null;
	}
}
