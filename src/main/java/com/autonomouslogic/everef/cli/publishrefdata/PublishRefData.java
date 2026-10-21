package com.autonomouslogic.everef.cli.publishrefdata;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.cli.publishrefdata.bundle.CategoryBundleRenderer;
import com.autonomouslogic.everef.cli.publishrefdata.bundle.GroupBundleRenderer;
import com.autonomouslogic.everef.cli.publishrefdata.bundle.MarketGroupBundleRenderer;
import com.autonomouslogic.everef.cli.publishrefdata.bundle.RootCategoryBundleRenderer;
import com.autonomouslogic.everef.cli.publishrefdata.bundle.TypeBundleRenderer;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.openapi.refdata.api.RefdataApi;
import com.autonomouslogic.everef.refdata.RefDataMeta;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.DiscordNotifier;
import com.autonomouslogic.everef.util.ProgressReporter;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.autonomouslogic.everef.util.TempFiles;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes the reference data.
 */
@Log4j2
public class PublishRefData implements Command {
	private final int uploadConcurrency = Configs.REF_DATA_UPLOAD_CONCURRENCY.getRequired();

	@Inject
	@Named("refdata")
	protected S3AsyncClient s3Client;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected RefdataApi refdataApi;

	@Inject
	protected MVStoreUtil mvStoreUtil;

	@Inject
	protected Provider<BasicFileRenderer> basicFileRendererProvider;

	@Inject
	protected Provider<RootMarketGroupIndexRenderer> rootMarketGroupIndexRendererProvider;

	@Inject
	protected Provider<IndexRenderer> indexRendererProvider;

	@Inject
	protected Provider<TypeBundleRenderer> typeBundleRendererProvider;

	@Inject
	protected Provider<RootCategoryBundleRenderer> rootCategoryBundleRendererProvider;

	@Inject
	protected Provider<CategoryBundleRenderer> categoryBundleRendererProvider;

	@Inject
	protected Provider<GroupBundleRenderer> groupBundleRendererProvider;

	@Inject
	protected Provider<MarketGroupBundleRenderer> marketGroupBundleRendererProvider;

	@Inject
	protected DiscordNotifier discordNotifier;

	private S3Url refDataUrl;
	private URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
	private AtomicInteger uploadCounter = new AtomicInteger();
	private File refDataFile;
	private RefDataMeta latestMeta;
	private RefDataMeta currentMeta;
	private MVStore dataStore;
	private MVStore fileStore;
	private MVMap<String, byte[]> fileMap;
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
	public void run() {
		VirtualThreads.checkIsVirtual();
		Completable.mergeArray(loadLatestFile(), loadCurrentMeta())
				.andThen(Completable.defer(() -> {
					if (!shouldUpdate()) {
						log.info("No update needed");
						return Completable.complete();
					}
					return listBucketContents().flatMapCompletable(existing -> {
						return Completable.concatArray(
								initMvStore(),
								loadData(),
								renderFiles(),
								uploadFiles(existing),
								closeMvStore(),
								notifyDiscord());
					});
				}))
				.blockingAwait();
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
		return Completable.fromAction(() -> {
			currentMeta = VirtualThreads.onVirtualThread(() -> refdataApi.getMeta());
		});
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

	private Single<Map<String, String>> listBucketContents() {
		return Single.defer(() -> {
			log.info("Listing existing contents");
			// Only the path and ETag of each object are kept: holding the full ListedS3Object (and the SDK
			// response pages it's created from) for every object in the bucket is too expensive in memory
			// for a bucket this size.
			var existing = new HashMap<String, String>();
			s3Adapter.listObjects(refDataUrl, true, s3Client, obj -> {
				if (!obj.getUrl().getPath().endsWith("index.html")) {
					existing.put(obj.getUrl().getPath(), obj.getEtag());
				}
			});
			log.info("Listed {} objects", existing.size());
			return Single.just(existing);
		});
	}

	private Completable initMvStore() {
		return Completable.fromAction(() -> {
			log.trace("Init MVStore");
			dataStore = mvStoreUtil.createTempStore("ref-data-data");
			fileStore = mvStoreUtil.createTempStore("ref-data-files");
			// Stored as raw bytes, not via openJsonMap: the content is already serialized JSON bytes, and
			// going through a JsonNode round trip on the way in and out would just add another full copy of
			// every file's tree to the heap for no benefit.
			fileMap = fileStore.openMap("files");
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
						// entry.getContent() is already serialized JSON bytes (see RefDataUtil.createEntry),
						// so it's stored as-is rather than round-tripping through a JsonNode tree just to
						// re-serialize it straight back to the same bytes.
						dataStore.<Long, byte[]>openMap(entry.getType()).put(entry.getId(), entry.getContent());
					}
				}));
	}

	private Completable renderFiles() {
		return Completable.defer(() -> Flowable.concatArray(
						Flowable.just(Pair.of(metaEntry.getPath(), jsonMapper.readTree(metaEntry.getContent()))),
						indexRendererProvider.get().setDataStore(dataStore).render(),
						basicFileRendererProvider.get().setDataStore(dataStore).render(),
						rootMarketGroupIndexRendererProvider
								.get()
								.setDataStore(dataStore)
								.render(),
						typeBundleRendererProvider.get().setDataStore(dataStore).render(),
						rootCategoryBundleRendererProvider
								.get()
								.setDataStore(dataStore)
								.render(),
						categoryBundleRendererProvider
								.get()
								.setDataStore(dataStore)
								.render(),
						groupBundleRendererProvider
								.get()
								.setDataStore(dataStore)
								.render(),
						marketGroupBundleRendererProvider
								.get()
								.setDataStore(dataStore)
								.render())
				.flatMapCompletable(entry -> Completable.fromAction(() -> {
					// Serialized to bytes immediately: fileMap holds raw JSON bytes, not JsonNode trees, so
					// a rendered file's tree is garbage-collectable as soon as it's written here instead of
					// staying live (duplicated as both a tree and bytes) until upload time.
					fileMap.put(entry.getLeft(), jsonMapper.writeValueAsBytes(entry.getRight()));
				})));
	}

	private Completable uploadFiles(Map<String, String> existing) {
		return Completable.fromAction(() -> {
					log.info("Evaluating {} files for upload", fileMap.size());
					var reporter = new ProgressReporter(getName(), fileMap.size(), Duration.ofMinutes(1));
					reporter.start();
					var skipped = new AtomicInteger();
					var uploaded = new AtomicInteger();
					// The pipeline below is lazy end to end (an Iterator, not a List): entries are only
					// serialized into ReferenceEntry/byte[] form as VirtualThreads.runAll pulls them, one per
					// free upload slot, so at most uploadConcurrency files' worth of bytes are ever resident
					// at once instead of every changed file in the dataset.
					var entries = fileMap.entrySet().stream()
							.filter(entry -> !entry.getKey().equals(metaEntry.getPath()))
							.peek(entry -> reporter.increment())
							.map(entry -> refDataUtil.createEntryForPath(entry.getKey(), entry.getValue()))
							.filter(entry -> filterExisting(skipped, existing, entry))
							.iterator();
					VirtualThreads.checkIsVirtual();
					// commons-java < 1.11.3 drained this overload's iterator into an ArrayList<Runnable> up
					// front - one closure per file, each capturing its full ReferenceEntry/byte[] content -
					// before running anything, which defeated the laziness above just as completely as the
					// original .toList() did. 1.11.3 fixed it to pull lazily; see commons-java#111.
					VirtualThreads.runAll(
							entries,
							entry -> {
								uploaded.incrementAndGet();
								uploadFile(entry).blockingAwait();
							},
							uploadConcurrency);
					log.info("Uploaded {} entries", uploaded.get());
					log.info("Skipped {} entries", skipped.get());
				})
				.andThen(Completable.defer(() -> {
					// existing is keyed by full bucket path (e.g. "base/meta"), but metaEntry.getPath() is
					// the unprefixed render path (e.g. "meta"). Removing the latter from `existing` was a
					// no-op, which left the meta file in `remaining` and deleted it from the bucket whenever
					// its rendered bytes happened to still match the existing ETag (uploadMetaFile would
					// then skip re-uploading it, believing nothing changed) - most visible on a forced
					// republish of otherwise-unchanged data. The correct full path is excluded from the
					// *copy* passed to deleteRemaining, not from `existing` itself: uploadMetaFile still
					// needs to find it there to do its own ETag comparison afterwards.
					var metaPath = refDataUrl.getPath() + metaEntry.getPath();
					var remaining = new ArrayList<>(existing.keySet());
					remaining.remove(metaPath);
					return deleteRemaining(remaining);
				}))
				.andThen(Completable.defer(() -> uploadMetaFile(existing)));
	}

	private Completable uploadMetaFile(Map<String, String> existing) {
		return Completable.defer(() -> {
			var entry = refDataUtil.createEntryForPath(metaEntry.getPath(), fileMap.get(metaEntry.getPath()));
			if (!filterExisting(new AtomicInteger(), existing, entry)) {
				log.info("Meta file unchanged, skipping upload");
				return Completable.complete();
			}
			log.info("Uploading meta file");
			return uploadFile(entry);
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
			return Completable.fromAction(() -> s3Adapter.putObject(latestPut, entry.getContent(), s3Client))
					.andThen(Completable.fromAction(() -> {
						var uploads = uploadCounter.incrementAndGet();
						if (uploads % 10000 == 0) {
							log.info("Uploaded {} files", uploads);
						}
					}));
		});
	}

	private boolean filterExisting(
			@NonNull AtomicInteger skipped, @NonNull Map<String, String> existing, @NonNull ReferenceEntry entry) {
		var existingEtag = existing.remove(entry.getPath());
		if (existingEtag != null && existingEtag.equals(entry.getMd5Hex())) {
			skipped.incrementAndGet();
			return false;
		}
		return true;
	}

	private Completable notifyDiscord() {
		return Completable.fromAction(() -> {
			discordNotifier.notifyDiscord(String.format(
					"Reference data published - SDE: %s, Hoboleaks: %s)",
					latestMeta.getSde().getVersion(), latestMeta.getHoboleaks().getVersion()));
		});
	}

	public Completable deleteRemaining(@NonNull List<String> remaining) {
		return Completable.fromAction(() -> {
			log.info("Deleting {} entries", remaining.size());
			VirtualThreads.checkIsVirtual();
			VirtualThreads.callAll(
					remaining.iterator(),
					entry -> {
						var delete = s3Util.deleteObjectRequest(
								refDataUrl.toBuilder().path(entry).build());
						s3Adapter.deleteObject(delete, s3Client);
						return null;
					},
					uploadConcurrency);
			log.info("Deleted {} entries", remaining.size());
		});
	}
}
