package com.autonomouslogic.everef.util;

import static com.autonomouslogic.everef.util.ArchivePathFactory.REFERENCE_DATA;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NonNull;
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

	private S3Url refDataUrl;

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Inject
	protected RefDataUtil() {}

	@Inject
	protected void init() {
		refDataUrl = (S3Url) urlParser.parse(Configs.REFERENCE_DATA_PATH.getRequired());
	}

	public Single<File> downloadLatestReferenceData() {
		return Single.defer(() -> {
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
			return Flowable.concat(fileEntries, indexEntry);
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
		return refDataUrl.getPath() + type;
	}
}
