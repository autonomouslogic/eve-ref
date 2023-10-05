package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.openapi.refdata.apis.RefdataApi;
import com.autonomouslogic.everef.refdata.RefDataMeta;
import com.autonomouslogic.everef.s3.ListedS3Object;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

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

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected RefdataApi refdataApi;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected Provider<BasicFileRenderer> basicFileRendererProvider;

	private S3Url refDataUrl;
	private URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
	private AtomicInteger uploadCounter = new AtomicInteger();
	private File refDataFile;
	private RefDataMeta latestMeta;
	private RefDataMeta currentMeta;
	private MVStore dataStore;
	private MVStore fileStore;
	private Map<String, JsonNode> fileMap;
	private ReferenceEntry metaEntry;

	private final Duration cacheTime = Configs.REFERENCE_DATA_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected PublishRefData() {}

	@Inject
	protected void init() {
		refDataUrl = (S3Url) urlParser.parse(Configs.REFERENCE_DATA_PATH.getRequired());
	}

	@SneakyThrows
	@Override
	public Completable run() {
		return Completable.mergeArray(loadLatestFile(), loadCurrentMeta()).andThen(Completable.defer(() -> {
			if (!shouldUpdate()) {
				log.info("No update needed");
				return Completable.complete();
			}
			return listBucketContents().flatMapCompletable(existing -> {
				return Completable.concatArray(
						initMvStore(), loadData(), renderFiles(), uploadFiles(existing), closeMvStore());
			});
		}));
	}

	private Completable loadLatestFile() {
		return refDataUtil
				.downloadLatestReferenceData()
				.flatMap(file -> {
					refDataFile = file;
					return refDataUtil.getMetaFromRefDataFile(file).doOnSuccess(meta -> latestMeta = meta);
				})
				.ignoreElement();
	}

	@SneakyThrows
	private Completable loadCurrentMeta() {
		return Rx3Util.toMaybe(refdataApi.getMeta())
				.doOnSuccess(meta -> currentMeta = meta)
				.ignoreElement();
	}

	private boolean shouldUpdate() {
		if (Configs.FORCE_REF_DATA.getRequired()) {
			return true;
		}
		if (latestMeta == null || currentMeta == null) {
			return true;
		}
		return !currentMeta.equals(latestMeta);
	}

	private Single<Map<String, ListedS3Object>> listBucketContents() {
		return Flowable.defer(() -> {
					log.info("Listing existing contents");
					return s3Adapter
							.listObjects(refDataUrl, s3Client)
							.filter(obj -> !(obj.getUrl().getPath().endsWith("index.html")));
				})
				.toList()
				.map(objects -> {
					log.info("Listed {} objects", objects.size());
					return objects.stream()
							.collect(Collectors.toMap(o -> o.getUrl().getPath(), Function.identity()));
				});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.trace("Init MVStore");
			dataStore = mvStoreUtil.createTempStore("ref-data-data");
			fileStore = mvStoreUtil.createTempStore("ref-data-files");
			fileMap = mvStoreUtil.openJsonMap(fileStore, "files", String.class);
		});
	}

	public Completable closeMvStore() {
		return Completable.fromAction(() -> {
			log.trace("Closing MVStore");
			dataStore.close();
			fileStore.close();
		});
	}

	private Completable loadData() {
		return refDataUtil
				.parseReferenceDataArchive(refDataFile)
				.flatMapCompletable(entry -> Completable.fromAction(() -> {
					if (entry.getType().equals("meta")) {
						metaEntry = entry;
					} else {
						mvStoreUtil
								.openJsonMap(dataStore, entry.getType(), Long.class)
								.put(entry.getId(), objectMapper.readTree(entry.getContent()));
					}
				}));
	}

	private Completable renderFiles() {
		return Completable.defer(() -> Flowable.concatArray(
						Flowable.just(Pair.of(metaEntry.getPath(), objectMapper.readTree(metaEntry.getContent()))),
						basicFileRendererProvider.get().setDataStore(dataStore).render())
				.flatMapCompletable(entry -> Completable.fromAction(() -> {
					fileMap.put(entry.getLeft(), entry.getRight());
				})));
	}

	private Completable uploadFiles(Map<String, ListedS3Object> existing) {
		return Completable.defer(() -> {
			var skipped = new AtomicInteger();
			return Flowable.fromIterable(fileMap.entrySet())
					.map(entry -> refDataUtil.createEntryForPath(
							entry.getKey(), objectMapper.writeValueAsBytes(entry.getValue())))
					.filter(entry -> filterExisting(skipped, existing, entry))
					.flatMapCompletable(entry -> uploadFile(entry), false, UPLOAD_CONCURRENCY)
					.doOnComplete(() -> log.info("Skipped {} entries", skipped.get()))
					.andThen(Completable.defer(() -> deleteRemaining(new ArrayList<>(existing.keySet()))));
		});
	}

	private Completable uploadFile(@NonNull ReferenceEntry entry) {
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

	private boolean filterExisting(
			@NonNull AtomicInteger skipped,
			@NonNull Map<String, ListedS3Object> existing,
			@NonNull ReferenceEntry entry) {
		var existingHash =
				Optional.ofNullable(existing.get(entry.getPath())).flatMap(e -> Optional.ofNullable(e.getMd5Hex()));
		existing.remove(entry.getPath());
		if (existingHash.isPresent() && existingHash.get().equals(entry.getMd5Hex())) {
			skipped.incrementAndGet();
			return false;
		}
		return true;
	}

	public Completable deleteRemaining(@NonNull List<String> remaining) {
		return Completable.defer(() -> {
					log.info("Deleting {} entries", remaining.size());
					return Flowable.fromIterable(remaining)
							.flatMapCompletable(
									entry -> {
										var delete = s3Util.deleteObjectRequest(refDataUrl.toBuilder()
												.path(entry)
												.build());
										return s3Adapter
												.deleteObject(delete, s3Client)
												.ignoreElement();
									},
									false,
									UPLOAD_CONCURRENCY);
				})
				.doOnComplete(() -> log.info("Deleted {} entries", remaining.size()));
	}
}
