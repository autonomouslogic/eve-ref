package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.REFERENCE_DATA;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.HashUtil;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Publishes the reference data.
 */
@Log4j2
public class PublishRefData implements Command {
	private static final int UPLOAD_CONCURRENCY = 32;

	@Inject
	@Named("refdata")
	protected S3AsyncClient s3Client;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected ObjectMapper objectMapper;

	private S3Url refDataUrl;
	private URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
	private AtomicInteger uploadCounter = new AtomicInteger();

	private final Duration cacheTime = Configs.REFERENCE_DATA_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected PublishRefData() {}

	@Inject
	protected void init() {
		var refDataPathUrl = urlParser.parse(Configs.REFERENCE_DATA_PATH.getRequired());
		if (!refDataPathUrl.getProtocol().equals("s3")) {
			throw new IllegalArgumentException("Reference data path must be an S3 path");
		}
		refDataUrl = (S3Url) refDataPathUrl;
		if (!refDataUrl.getPath().equals("")) {
			throw new IllegalArgumentException("Reference data path must be run at the root of the bucket");
		}
	}

	@SneakyThrows
	@Override
	public Completable run() {
		var skipped = new AtomicInteger();
		return listBucketContents()
				.flatMapCompletable(existing -> {
					return downloadLatestReferenceData()
							.flatMapPublisher(this::parseFile)
							.filter(entry -> {
								if (existing.containsKey(entry.getMd5Hex())) {
									skipped.incrementAndGet();
									existing.remove(entry.getMd5Hex());
									return false;
								}
								return true;
							})
							.flatMapCompletable(this::uploadFile, false, UPLOAD_CONCURRENCY);
				})
				.doOnComplete(() -> log.info("Skipped {} entries", skipped.get()));
	}

	private Single<Map<String, ObjectListing>> listBucketContents() {
		return Flowable.defer(() -> {
					log.info("Listing existing contents");
					ListObjectsV2Request request = ListObjectsV2Request.builder()
							.bucket(refDataUrl.getBucket())
							.prefix(refDataUrl.getPath())
							.build();
					return s3Adapter
							.listObjects(request, s3Client)
							.flatMap(response -> Flowable.fromIterable(response.contents()))
							.filter(obj -> !(obj.key().endsWith("index.html")))
							.filter(obj -> obj.size() != null
									&& obj.size() > 0) // S3 doesn't list directories, but Backblaze does.
							.map(obj -> createObjectListing(obj));
				})
				.toList()
				.map(objects -> {
					log.info("Listed {} objects", objects.size());
					return objects.stream().collect(Collectors.toMap(ObjectListing::getMd5Hex, Function.identity()));
				});
	}

	private Single<File> downloadLatestReferenceData() {
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

	private Flowable<ReferenceEntry> parseFile(File file) {
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

	private Completable uploadFile(ReferenceEntry entry) {
		return Completable.defer(() -> {
			log.trace("Uploading {} - {} bytes", entry, entry.getContent().length);
			var latestPath = S3Url.builder()
					.bucket(refDataUrl.getBucket())
					.path(entry.getPath())
					.build();
			var latestPut = s3Util
					.putPublicObjectRequest(entry.getContent().length, latestPath, "application/json", cacheTime)
					.toBuilder()
					.contentMD5(entry.getMd5B64())
					.build();
			return s3Adapter
					.putObject(latestPut, entry.getContent(), s3Client)
					.ignoreElement()
					.andThen(Completable.fromAction(() -> {
						var uploads = uploadCounter.incrementAndGet();
						if (uploads % 10000 == 0) {
							log.info("Uploaded {} files", uploads);
						}
					}));
		});
	}

	@Value
	private static class ReferenceEntry {
		String type;
		Long id;

		@NonNull
		String path;

		@NonNull
		@ToString.Exclude
		byte[] content;

		@NonNull
		String md5B64;

		@NonNull
		String md5Hex;
	}

	private String subPath(@NonNull String type, @NonNull Long id) {
		return subPath(type + "/" + id);
	}

	private String subPath(String type) {
		return refDataUrl.getPath() + type;
	}

	private ReferenceEntry createEntry(String type, Long id, byte[] content) {
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				type, id, subPath(type, id), content, Base64.encodeBase64String(md5), Hex.encodeHexString(md5));
	}

	private ReferenceEntry createEntry(String type, byte[] content) {
		var md5 = HashUtil.md5(content);
		return new ReferenceEntry(
				type, null, subPath(type), content, Base64.encodeBase64String(md5), Hex.encodeHexString(md5));
	}

	private ObjectListing createObjectListing(S3Object obj) {
		return ObjectListing.builder()
				.path(obj.key())
				.md5Hex(StringUtils.remove(obj.eTag(), '"'))
				.build();
	}

	@Value
	@Builder
	public static class ObjectListing {
		String path;
		String md5Hex;
	}
}
