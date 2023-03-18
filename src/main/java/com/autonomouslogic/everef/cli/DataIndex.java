package com.autonomouslogic.everef.cli;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.pug.PugHelper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.util.S3Util;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Builds index pages for <code>data.everef.net</code>.
 */
@Log4j2
public class DataIndex implements Command {
	@Inject
	@Named("data")
	protected S3AsyncClient s3;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected Provider<DataIndex> dataIndexProvider;

	@Inject
	protected PugHelper pugHelper;

	@Inject
	protected S3Adapter s3Adapter;

	private final String dataBucket = Configs.DATA_S3_BUCKET.getRequired();
	private final String dataDomain = Configs.DATA_DOMAIN.getRequired();
	private final Duration indexCacheTime = Configs.DATA_INDEX_CACHE_TIME.getRequired();
	private final int indexConcurrency = Configs.DATA_INDEX_CONCURRENCY.getRequired();

	@Inject
	protected DataIndex() {}

	@Override
	public Completable run() {
		return listAndIndex().flatMapCompletable(dirs -> {
			resolveDirectories(dirs);
			return createAndUploadIndexPages(dirs);
		});
	}

	private Completable createAndUploadIndexPages(Map<String, Listing.Builder> dirs) {
		return Flowable.fromIterable(dirs.entrySet())
				.flatMapCompletable(
						entry -> {
							var listing =
									entry.getValue().prefix(entry.getKey()).build();
							return createIndexPage(listing);
						},
						false,
						indexConcurrency)
				.andThen(Completable.fromAction(() -> log.info(String.format("Uploaded %s index pages", dirs.size()))));
	}

	/**
	 * Iterates over all the directories and adds them to their parent directories.
	 * @param dirs
	 */
	private void resolveDirectories(Map<String, Listing.Builder> dirs) {
		var changesMade = new AtomicBoolean();
		do {
			changesMade.set(false);
			dirs.forEach((path, listing) -> {
				if (path.equals("")) {
					return;
				}
				var parentPath = new File("/" + path).getParent().substring(1);
				var parent = dirs.computeIfAbsent(parentPath, ignore -> {
					changesMade.set(true);
					return Listing.builder();
				});
				parent.common(CommonListing.builder()
						.prefix(path)
						.basename(new File(path).getName())
						.build());
			});
		} while (changesMade.get());
	}

	@NotNull
	private Single<Map<String, Listing.Builder>> listAndIndex() {
		final Map<String, Listing.Builder> dirs = new ConcurrentHashMap<>();
		return listBucketContents()
				.groupBy(ObjectListing::getDir)
				.flatMapCompletable(group -> {
					var listing = dirs.computeIfAbsent(group.getKey(), ignore -> Listing.builder());
					return group.toList()
							.doOnSuccess(objs -> listing.objects(objs))
							.ignoreElement();
				})
				.andThen(Single.just(dirs));
	}

	private Flowable<ObjectListing> listBucketContents() {
		return Flowable.defer(() -> {
					log.info("Listing contents");
					ListObjectsV2Request request =
							ListObjectsV2Request.builder().bucket(dataBucket).build();
					return s3Adapter
							.listObjects(request, s3)
							.flatMap(response -> Flowable.fromIterable(response.contents()))
							.filter(obj -> !(obj.key().endsWith("index.html")))
							.filter(obj -> obj.size() != null
									&& obj.size() > 0) // S3 doesn't list directories, but Backblaze does.
							.map(obj -> createObjectListing(obj));
				})
				.doOnComplete(() -> log.info("Listing complete"));
	}

	private ObjectListing createObjectListing(S3Object obj) {
		String prefix = obj.key();
		String dir = new File("/" + prefix).getParent().substring(1);
		String basename = new File(prefix).getName();
		long size = obj.size();
		Instant lastModified = Instant.ofEpochMilli(obj.lastModified().toEpochMilli());
		return ObjectListing.builder()
				.prefix(prefix)
				.basename(basename)
				.dir(dir)
				.size(size)
				.lastModified(lastModified)
				.build();
	}

	private Completable createIndexPage(Listing listing) {
		return Completable.defer(() -> {
			var sortedCommons = listing.getCommon().stream()
					.sorted(Ordering.natural().onResultOf(CommonListing::getBasename))
					.distinct() // resolveDirectories() adds duplicates.
					.collect(Collectors.toList());
			var sortedObjects = listing.getObjects().stream()
					.sorted(Ordering.natural().onResultOf(ObjectListing::getBasename))
					.collect(Collectors.toList());
			byte[] rendered = renderIndexPage(listing.toBuilder()
					.clearCommon()
					.common(sortedCommons)
					.clearObjects()
					.objects(sortedObjects)
					.build());
			// Upload index page.
			var target = new URL(String.format(
					"s3://%s/%sindex.html",
					dataBucket, listing.getPrefix().equals("") ? "" : listing.getPrefix() + "/"));
			log.debug(String.format("Uploading index page: %s", target));
			var putObjectRequest = s3Util
					.putObjectRequest(new ByteArrayInputStream(rendered), rendered.length, target, "text/html")
					.toBuilder()
					.acl(ObjectCannedACL.PUBLIC_READ)
					.cacheControl(s3Util.cacheControl(indexCacheTime))
					.build();
			return Rx3Util.toSingle(s3.putObject(putObjectRequest, AsyncRequestBody.fromBytes(rendered)))
					.ignoreElement()
					.timeout(5, TimeUnit.SECONDS)
					.retry(3)
					.observeOn(Schedulers.computation());
		});
	}

	private byte[] renderIndexPage(Listing listing) {
		var directoryParts = splitDirectoryPath(listing.getPrefix());
		// Prepare model.
		Map<String, Object> model = new HashMap<>();
		model.put("pageTitle", dataDomain + "/" + listing.getPrefix());
		model.put("common", listing.getCommon());
		model.put("objects", listing.getObjects());
		model.put("domain", dataDomain);
		model.put("directoryParts", directoryParts);
		model.put(
				"totalSize",
				listing.objects.stream().mapToLong(o -> o.getSize()).sum());
		// Render template.
		byte[] rendered = pugHelper.renderTemplate("data/index.jade", model);

		return rendered;
	}

	@Value
	@Builder(toBuilder = true)
	public static class Listing {
		String prefix;

		@Singular("common")
		List<CommonListing> common;

		@Singular
		List<ObjectListing> objects;
	}

	@Value
	@Builder
	public static class CommonListing {
		String prefix;
		String basename;
	}

	@Value
	@Builder
	public static class ObjectListing {
		String prefix;
		String basename;
		String dir;
		long size;
		Instant lastModified;
	}

	private Map<String, String> splitDirectoryPath(String path) {
		Map<String, String> paths = new LinkedHashMap<>();
		String[] parts = path.split("\\/");
		for (int i = 0; i < parts.length; i++) {
			StringJoiner pathUrl = new StringJoiner("/");
			for (int j = 0; j <= i; j++) {
				pathUrl.add(parts[j]);
				paths.put(parts[i], "/" + pathUrl.toString());
			}
		}
		return paths;
	}
}
