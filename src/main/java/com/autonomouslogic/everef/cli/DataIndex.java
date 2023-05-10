package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.pug.PugHelper;
import com.autonomouslogic.everef.s3.ListedS3Object;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.S3Util;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;

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
	protected PugHelper pugHelper;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected UrlParser urlParser;

	private S3Url dataUrl;
	private final String dataDomain = Configs.DATA_DOMAIN.getRequired();
	private final Duration indexCacheTime = Configs.DATA_INDEX_CACHE_CONTROL_MAX_AGE.getRequired();
	private final int indexConcurrency = Configs.DATA_INDEX_CONCURRENCY.getRequired();

	@Inject
	protected DataIndex() {}

	@Inject
	protected void init() {
		var dataPathUrl = urlParser.parse(Configs.DATA_PATH.getRequired());
		if (!dataPathUrl.getProtocol().equals("s3")) {
			throw new IllegalArgumentException("Data path must be an S3 path");
		}
		dataUrl = (S3Url) dataPathUrl;
	}

	@Override
	public Completable run() {
		return listAndIndex().flatMapCompletable(dirs -> {
			resolveDirectories(dirs);
			return createAndUploadIndexPages(dirs);
		});
	}

	private Completable createAndUploadIndexPages(@NonNull Map<String, Listing.Builder> dirs) {
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
	private void resolveDirectories(@NonNull Map<String, Listing.Builder> dirs) {
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
					if (!dataUrl.getPath().equals("")) {
						throw new RuntimeException("Data index must be run at the root of the bucket");
					}
					return s3Adapter
							.listObjects(dataUrl, s3)
							.filter(obj -> !(obj.getUrl().getPath().endsWith("index.html")))
							.map(obj -> createObjectListing(obj));
				})
				.doOnComplete(() -> log.info("Listing complete"));
	}

	private ObjectListing createObjectListing(@NonNull ListedS3Object listedObject) {
		var obj = listedObject.getS3Object();
		String key = obj.key();
		String dir = new File("/" + key).getParent().substring(1);
		String basename = new File(key).getName();
		return ObjectListing.builder()
				.listedObject(listedObject)
				.basename(basename)
				.dir(dir)
				.prefix(key)
				.build();
	}

	private Completable createIndexPage(@NonNull Listing listing) {
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
			var target = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path((listing.getPrefix().equals("") ? "" : listing.getPrefix() + "/") + "index.html")
					.build();
			log.debug(String.format("Uploading index page: %s", target));
			var putObjectRequest = s3Util.putPublicObjectRequest(rendered.length, target, "text/html", indexCacheTime);
			return s3Adapter
					.putObject(putObjectRequest, AsyncRequestBody.fromBytes(rendered), s3)
					.ignoreElement();
		});
	}

	private byte[] renderIndexPage(@NonNull Listing listing) {
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
				listing.objects.stream()
						.mapToLong(o -> o.getListedObject().getSize())
						.sum());
		// Render template.
		byte[] rendered = pugHelper.renderTemplate("data/index.pug", model);

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
		@NonNull
		String prefix;

		@NonNull
		String basename;
	}

	@Value
	@Builder
	public static class ObjectListing {
		@NonNull
		ListedS3Object listedObject;

		@NonNull
		String basename;

		@NonNull
		String dir;

		@NonNull
		String prefix;
	}

	private Map<String, String> splitDirectoryPath(@NonNull String path) {
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
