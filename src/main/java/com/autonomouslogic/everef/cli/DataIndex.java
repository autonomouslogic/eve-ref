package com.autonomouslogic.everef.cli;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.data.FileEntry;
import com.autonomouslogic.everef.data.IndexDirectoryEntry;
import com.autonomouslogic.everef.data.IndexFileEntry;
import com.autonomouslogic.everef.data.IndexPageData;
import com.autonomouslogic.everef.data.VirtualDirectory;
import com.autonomouslogic.everef.pug.PugHelper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.Rx;
import com.autonomouslogic.everef.util.archive.ArchivePathFactories;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.SneakyThrows;
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

	@Inject
	protected ObjectMapper objectMapper;

	private S3Url dataUrl;
	private final String dataDomain = Configs.DATA_DOMAIN.getRequired();
	private final Duration indexCacheTime = Configs.DATA_INDEX_CACHE_CONTROL_MAX_AGE.getRequired();
	private final int indexConcurrency = Configs.DATA_INDEX_CONCURRENCY.getRequired();

	@Setter
	@Getter
	private boolean recursive = true;

	@Setter
	@Getter
	private String prefix = Configs.DATA_INDEX_PREFIX.get().orElse(null);

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
	public void run() {
		VirtualThreads.checkIsVirtual();
		log.info("Starting data index for: {}", dataUrl);
		listAndIndex()
				.flatMapCompletable(
						dirIndex -> createAndUploadIndexPage(dirIndex.getLeft(), dirIndex.getRight())
								.subscribeOn(Rx.VIRTUAL),
						false,
						indexConcurrency)
				.blockingAwait();
	}

	private Flowable<Pair<String, List<FileEntry>>> listAndIndex() {
		return listBucketContents().flatMapPublisher(dir -> {
			log.debug("Building indexes");
			var path = Optional.ofNullable(prefix).orElse("");
			var indexes = dir.list(path, recursive).filter(d -> d.isDirectory()).map(d -> {
				var contents =
						dir.list(d.getPath(), false).filter(entry -> entry != d).toList();
				return Pair.of(d.getPath(), contents);
			});
			if (!recursive) {
				indexes = indexes.limit(1);
			}
			var indexList = indexes.toList();
			log.debug("Indexing complete");
			return Flowable.fromIterable(indexList);
		});
	}

	private Single<VirtualDirectory> listBucketContents() {
		return Single.defer(() -> {
					VirtualThreads.checkIsVirtual();
					if (!dataUrl.getPath().equals("")) {
						throw new RuntimeException("Data index must be run at the root of the bucket");
					}
					var url = dataUrl;
					if (!StringUtils.isEmpty(prefix)) {
						url = url.resolve(prefix);
					}
					if (!url.toString().endsWith("/")) {
						url = url.toBuilder().path(url.getPath() + "/").build();
					}
					log.debug("Listing contents at {}", url);
					var dir = new VirtualDirectory();
					var objects = s3Adapter.listObjects(url, recursive, s3);
					var filtered = objects.stream()
							.filter(obj -> !(obj.getUrl().getPath().endsWith("index.html")))
							.filter(obj -> !(obj.getUrl().getPath().endsWith("index.json")))
							.toList();
					log.debug("Listing complete");
					var withLastModified = s3Adapter.headLastModified(filtered, s3);
					log.debug("Heading complete");
					return Flowable.fromIterable(withLastModified)
							.doOnNext(obj -> {
								if (obj.isDirectory()) {
									dir.add(FileEntry.directory(obj.getUrl().getPath()));
								} else {
									dir.add(FileEntry.file(
											obj.getUrl().getPath(),
											obj.getSize(),
											obj.getLastModified(),
											obj.getEtag()));
								}
							})
							.ignoreElements()
							.andThen(Single.just(dir));
				})
				.doOnSuccess(ignore -> log.debug("Virtual directory complete"));
	}

	private Completable createAndUploadIndexPage(String prefix, List<FileEntry> index) {
		return Completable.defer(() -> {
			var htmlRendered = renderIndexPage(prefix, index);
			var jsonRendered = renderJsonIndexPage(prefix, index);
			return Completable.mergeArray(
					uploadIndexPage(prefix, htmlRendered).subscribeOn(Rx.VIRTUAL),
					uploadJsonIndexPage(prefix, jsonRendered).subscribeOn(Rx.VIRTUAL));
		});
	}

	private byte[] renderIndexPage(@NonNull String prefix, List<FileEntry> entries) {
		var directoryParts = splitDirectoryPath(prefix);
		var files = entries.stream().filter(e -> !e.isDirectory()).toList();
		var totalSize = files.stream().mapToLong(FileEntry::getSize).sum();
		// Prepare model.
		Map<String, Object> model = new HashMap<>();
		model.put("pageTitle", dataDomain + "/" + prefix);
		model.put("directories", entries.stream().filter(e -> e.isDirectory()).toList());
		model.put("files", files);
		model.put("totalSize", totalSize);
		model.put("domain", dataDomain);
		model.put("directoryParts", directoryParts);
		// Render template.
		byte[] rendered = pugHelper.renderTemplate("data/index.pug", model);

		return rendered;
	}

	@SneakyThrows
	private byte[] renderJsonIndexPage(@NonNull String prefix, List<FileEntry> entries) {
		var files = entries.stream()
				.filter(e -> !e.isDirectory())
				.map(entry -> buildFileEntry(entry))
				.toList();

		var directories = entries.stream()
				.filter(FileEntry::isDirectory)
				.map(entry -> buildDirectoryEntry(entry))
				.toList();

		var indexPage = IndexPageData.builder()
				.path(prefix)
				.files(files)
				.directories(directories)
				.build();

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(indexPage);
	}

	private IndexFileEntry buildFileEntry(FileEntry entry) {
		var filename = entry.getPath().substring(entry.getPath().lastIndexOf('/') + 1);
		var builder = IndexFileEntry.builder()
				.name(filename)
				.url("https://" + dataDomain + "/" + entry.getPath())
				.size(entry.getSize())
				.lastModified(entry.getLastModified())
				.etag(entry.getEtag());

		var match = ArchivePathFactories.tryMatch(entry.getPath());
		match.ifPresent(archiveMatch -> {
			builder.type(archiveMatch.getType());
			archiveMatch.getDate().ifPresent(date -> builder.fileTime(date));
			archiveMatch.getSequence().ifPresent(seq -> builder.sequenceNumber(seq));
		});

		return builder.build();
	}

	private IndexDirectoryEntry buildDirectoryEntry(FileEntry entry) {
		var dirName = entry.getPath()
				.replaceAll("/$", "")
				.substring(entry.getPath().replaceAll("/$", "").lastIndexOf('/') + 1);
		return IndexDirectoryEntry.builder()
				.name(dirName)
				.indexUrl("https://" + dataDomain + "/" + entry.getPath() + "/index.json")
				.build();
	}

	@NotNull
	private Completable uploadIndexPage(@NotNull String prefix, @NotNull byte[] rendered) {
		return Completable.fromAction(() -> {
			// Upload index page.
			var target = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path((prefix.equals("") ? "" : prefix + "/") + "index.html")
					.build();
			log.debug(String.format("Uploading index page: %s", target));
			var putObjectRequest = s3Util.putPublicObjectRequest(rendered.length, target, "text/html", indexCacheTime);
			s3Adapter.putObject(putObjectRequest, AsyncRequestBody.fromBytes(rendered), s3);
		});
	}

	@NotNull
	private Completable uploadJsonIndexPage(@NotNull String prefix, @NotNull byte[] rendered) {
		return Completable.fromAction(() -> {
			// Upload JSON index page.
			var target = S3Url.builder()
					.bucket(dataUrl.getBucket())
					.path((prefix.equals("") ? "" : prefix + "/") + "index.json")
					.build();
			log.debug(String.format("Uploading JSON index page: %s", target));
			var putObjectRequest =
					s3Util.putPublicObjectRequest(rendered.length, target, "application/json", indexCacheTime);
			s3Adapter.putObject(putObjectRequest, AsyncRequestBody.fromBytes(rendered), s3);
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
