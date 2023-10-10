package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.data.FileEntry;
import com.autonomouslogic.everef.data.VirtualDirectory;
import com.autonomouslogic.everef.pug.PugHelper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.S3Util;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

	@Setter
	@Getter
	private boolean recursive = true;

	@Setter
	@Getter
	private String prefix;

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
		return listAndIndex()
				.flatMapCompletable(
						dirIndex -> {
							//			resolveDirectories(dirs);
							return createAndUploadIndexPage(dirIndex.getLeft(), dirIndex.getRight());
							// .andThen(Completable.fromAction(() -> log.info(String.format("Uploaded %s index pages",
							// dirs.size()))));
						},
						false,
						indexConcurrency);
	}

	private Flowable<Pair<String, List<FileEntry>>> listAndIndex() {
		return listBucketContents().flatMapPublisher(dir -> {
			var path = Optional.ofNullable(prefix).orElse("");
			var indexes = Flowable.fromStream(dir.list(path, recursive))
					.filter(d -> d.isDirectory())
					.map(d -> {
						var contents = dir.list(d.getPath(), false)
								.filter(entry -> entry != d)
								.toList();
						return Pair.of(d.getPath(), contents);
					});
			if (!recursive) {
				indexes = indexes.take(1);
			}
			return indexes;
		});
	}

	private Single<VirtualDirectory> listBucketContents() {
		return Single.defer(() -> {
					log.debug("Listing contents");
					if (!dataUrl.getPath().equals("")) {
						throw new RuntimeException("Data index must be run at the root of the bucket");
					}
					var url = dataUrl;
					if (!StringUtils.isEmpty(prefix)) {
						url = url.resolve(prefix);
					}
					var dir = new VirtualDirectory();
					return s3Adapter
							.listObjects(url, recursive, s3)
							.filter(obj -> !(obj.getUrl().getPath().endsWith("index.html")))
							.doOnNext(obj -> {
								if (obj.isDirectory()) {
									dir.add(FileEntry.directory(obj.getUrl().getPath()));
								} else {
									dir.add(FileEntry.file(
											obj.getUrl().getPath(),
											obj.getSize(),
											obj.getLastModified(),
											obj.getMd5Hex()));
								}
							})
							.ignoreElements()
							.andThen(Single.just(dir));
				})
				.doAfterSuccess(ignore -> log.debug("Listing complete"));
	}

	private Completable createAndUploadIndexPage(String prefix, List<FileEntry> index) {
		return Completable.defer(() -> {
			var rendered = renderIndexPage(prefix, index);
			return uploadIndexPage(prefix, rendered);
		});
	}

	private byte[] renderIndexPage(@NonNull String prefix, List<FileEntry> entries) {
		var directoryParts = splitDirectoryPath(prefix);
		// Prepare model.
		Map<String, Object> model = new HashMap<>();
		model.put("pageTitle", dataDomain + "/" + prefix);
		model.put("common", entries.stream().filter(e -> e.isDirectory()).toList());
		model.put("objects", entries.stream().filter(e -> !e.isDirectory()).toList());
		model.put("domain", dataDomain);
		model.put("directoryParts", directoryParts);
		// Render template.
		byte[] rendered = pugHelper.renderTemplate("data/index.pug", model);

		return rendered;
	}

	@NotNull
	private Completable uploadIndexPage(@NotNull String prefix, @NotNull byte[] rendered) {
		return Completable.defer(() -> {
			// Upload index page.
			var target = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path((prefix.equals("") ? "" : prefix + "/") + "index.html")
					.build();
			log.debug(String.format("Uploading index page: %s", target));
			var putObjectRequest = s3Util.putPublicObjectRequest(rendered.length, target, "text/html", indexCacheTime);
			return s3Adapter
					.putObject(putObjectRequest, AsyncRequestBody.fromBytes(rendered), s3)
					.ignoreElement();
		});
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
