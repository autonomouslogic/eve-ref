package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.FUZZWORK_ORDERSET;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.DataCrawler;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.google.common.collect.Ordering;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Syncs ordersets from Fuzzwork.
 */
@Log4j2
public class SyncFuzzworkOrdersets implements Command {
	private static final int CONCURRENCY = 4;
	private static final Pattern ORDERSET_PATTERN = Pattern.compile("\\/orderbooks\\/orderset-([0-9]+).csv.gz");

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected S3Util s3Util;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected Provider<DataCrawler> dataCrawlerProvider;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	private S3Url dataPath;
	private final URI fuzzworkBaseUrl = Configs.FUZZWORK_BASE_URL.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	@Inject
	protected SyncFuzzworkOrdersets() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	@SneakyThrows
	@Override
	public Completable runAsync() {
		return getFilesToSync()
				.flatMapCompletable(
						file -> downloadFile(file)
								.flatMapCompletable(downloaded ->
										Completable.concatArray(uploadFile(downloaded), deleteFile(downloaded))),
						false,
						CONCURRENCY)
				.andThen(updateDataIndex());
	}

	@SneakyThrows
	private Flowable<FuzzworkFile> scanFuzzworkFiles() {
		return okHttpHelper
				.get(fuzzworkBaseUrl.resolve("/api/").toString(), okHttpClient)
				.flatMapPublisher(response -> {
					if (response.code() != 200) {
						return Flowable.error(new RuntimeException("Unexpected response code: " + response.code()));
					}
					var body =
							Optional.ofNullable(response.body()).orElseThrow().string();
					var doc = Jsoup.parse(body);
					var links = doc.select("a");
					var books = links.stream()
							.map(link -> link.attr("href"))
							.flatMap(href -> {
								var matcher = ORDERSET_PATTERN.matcher(href);
								if (!matcher.matches()) {
									return Stream.empty();
								}
								var id = Long.parseLong(matcher.group(1));
								return Stream.of(FuzzworkFile.builder()
										.id(id)
										.uri(fuzzworkBaseUrl.resolve(href))
										.build());
							})
							.toList();
					return Flowable.fromIterable(books);
				});
	}

	private Flowable<Long> scanExistingFiles() {
		return Flowable.defer(() -> {
			log.trace("Scanning existing files");
			return dataCrawlerProvider
					.get()
					.setPrefix(FUZZWORK_ORDERSET.getFolder() + "/")
					.crawl()
					.flatMap(url -> {
						var path = url.getPath();
						var basename = FilenameUtils.getBaseName(path);
						if (!basename.startsWith(FUZZWORK_ORDERSET.getFilename())
								|| !path.endsWith(FUZZWORK_ORDERSET.getSuffix())) {
							return Flowable.empty();
						}
						var removed = StringUtils.removeStart(basename, FUZZWORK_ORDERSET.getFilename());
						removed = removed.substring(0, removed.indexOf('-'));
						var id = Long.parseLong(removed);
						return Flowable.just(id);
					});
		});
	}

	private Flowable<FuzzworkFile> getFilesToSync() {
		return scanExistingFiles()
				.toList()
				.flatMapPublisher(existing -> {
					var set = new HashSet<>(existing);
					return scanFuzzworkFiles().filter(file -> !set.contains(file.getId()));
				})
				.sorted(Ordering.natural().onResultOf(FuzzworkFile::getId));
	}

	private Maybe<FuzzworkFile> downloadFile(FuzzworkFile fuzz) {
		return Maybe.defer(() -> {
					log.info("Downloading orderset {}", fuzz.getId());
					var file = tempFiles
							.tempFile("fuzzwork-orderset-" + fuzz.getId(), ".csv.gz")
							.toFile();
					return okHttpHelper
							.download(fuzz.getUri().toString(), file, okHttpClient)
							.flatMapMaybe(response -> {
								if (response.code() == 404) {
									log.warn(
											"Failed downloading orderset {} - status: {}",
											fuzz.getId(),
											response.code());
									return Maybe.empty();
								}
								if (response.code() != 200) {
									return Maybe.error(
											new RuntimeException("Unexpected response code: " + response.code()));
								}
								return Maybe.just(fuzz.toBuilder()
										.file(file)
										.lastModified(okHttpHelper
												.getLastModified(response)
												.orElseThrow()
												.toInstant())
										.build());
							});
				})
				.switchIfEmpty(Maybe.defer(() -> {
					log.info("No new data for orderset {}", fuzz.getId());
					return Maybe.empty();
				}));
	}

	private Completable uploadFile(FuzzworkFile fuzz) {
		return Completable.defer(() -> {
			var relativePath = FUZZWORK_ORDERSET.toBuilder()
					.filename(FUZZWORK_ORDERSET.getFilename() + fuzz.getId())
					.build()
					.createArchivePath(fuzz.getLastModified());
			var archivePath = dataPath.resolve(relativePath);
			var archivePut = s3Util.putPublicObjectRequest(fuzz.getFile().length(), archivePath, archiveCacheTime);
			log.info("Uploading orderset {} from {} to {}", fuzz.getId(), fuzz.getFile(), archivePath);
			return s3Adapter.putObject(archivePut, fuzz.getFile(), s3Client).ignoreElement();
		});
	}

	private Completable updateDataIndex() {
		return Completable.defer(() -> {
			var urls = List.of(0, 1).stream()
					.map(i -> Instant.now().minus(Duration.ofDays(i)))
					.map(FUZZWORK_ORDERSET::createArchivePath)
					.map(dataPath::resolve)
					.toList();
			return dataIndexHelper.updateIndex(urls);
		});
	}

	@NotNull
	private Completable deleteFile(FuzzworkFile downloaded) {
		return Completable.fromAction(() -> {
			if (!downloaded.getFile().delete()) {
				log.warn("Failed deleting file {}", downloaded.getFile());
			}
		});
	}

	@Value
	@Builder(toBuilder = true)
	private static class FuzzworkFile {
		long id;
		URI uri;
		Instant lastModified;
		File file;
	}
}
