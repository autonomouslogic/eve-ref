package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.HOBOLEAKS;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.S3Util;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class ScrapeHoboleaks implements Command {
	@Inject
	protected UrlParser urlParser;

	@Inject
	protected ObjectMapper objectMapper;

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
	protected DataIndexHelper dataIndexHelper;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	private S3Url dataPath;
	private HttpUrl dataUrl;
	private HttpUrl hoboUrl;

	@Inject
	protected ScrapeHoboleaks() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataUrl = (HttpUrl) urlParser.parse(Configs.DATA_BASE_URL.getRequired());
		hoboUrl = (HttpUrl) urlParser.parse(Configs.HOBOLEAKS_SDE_DATA_BASE_URL.getRequired());
	}

	public Completable run() {
		return loadMeta().flatMapCompletable(currentMeta -> {
			return loadArchiveMeta().flatMapCompletable(archiveMeta -> {
				if (Arrays.equals(currentMeta, archiveMeta)) {
					log.info("No update needed");
					return Completable.complete();
				}
				return buildArchive(currentMeta).flatMapCompletable(this::uploadFiles);
			});
		});
	}

	private Single<byte[]> loadMeta() {
		return download(hoboUrl.resolve("meta.json"));
	}

	private Single<byte[]> loadArchiveMeta() {
		return Single.defer(() -> {
			var file = tempFiles.tempFile("hoboleaks", ".tar.xz").toFile();
			var url = dataUrl.resolve(HOBOLEAKS.createLatestPath()).toString();
			return okHttpHelper.download(url, file, okHttpClient).flatMap(response -> {
				if (response.code() == 404) {
					return Single.just(new byte[0]);
				}
				if (response.code() != 200) {
					return Single.error(new RuntimeException("Failed to download " + url + ": " + response.code()));
				}
				return CompressUtil.loadArchive(file)
						.filter(entry -> entry.getLeft().getName().equals("meta.json"))
						.map(Pair::getRight)
						.first(new byte[0]);
			});
		});
	}

	private Single<File> buildArchive(byte[] metaBytes) {
		return Single.defer(() -> {
			var meta = objectMapper.readTree(metaBytes);
			var archive = tempFiles.tempFile("hoboleaks-scrape", ".tar").toFile();
			var out = new TarArchiveOutputStream(new FileOutputStream(archive));
			var metaEntry = new TarArchiveEntry("meta.json");
			metaEntry.setSize(metaBytes.length);
			out.putArchiveEntry(metaEntry);
			out.write(metaBytes);
			out.closeArchiveEntry();
			return Completable.defer(() -> {
						return Flowable.fromIterable(() -> meta.get("files").fields())
								.flatMap(entry -> {
									var filename = entry.getKey();
									return download(hoboUrl.resolve(filename))
											.map(bytes -> Pair.of(filename, bytes))
											.toFlowable();
								})
								.toList()
								.flatMapCompletable(files -> Completable.fromAction(() -> {
									for (var entry : files) {
										var archiveEntry = new TarArchiveEntry((String) entry.getLeft());
										archiveEntry.setSize(entry.getRight().length);
										out.putArchiveEntry(archiveEntry);
										out.write(entry.getRight());
										out.closeArchiveEntry();
									}
								}));
					})
					.andThen(Single.fromCallable(() -> {
						out.close();
						var compressed = CompressUtil.compressXz(archive);
						return compressed;
					}));
		});
	}

	private Single<byte[]> download(HttpUrl url) {
		return Single.defer(() -> {
			var file = tempFiles.tempFile("hoboleaks", ".json").toFile();
			return okHttpHelper.download(url.toString(), file, okHttpClient).map(response -> {
				if (response.code() != 200) {
					throw new RuntimeException("Failed to download " + url + ": " + response.code());
				}
				return IOUtils.toByteArray(new FileInputStream(file));
			});
		});
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	private Completable uploadFiles(File outputFile) {
		return Completable.defer(() -> {
			var latestPath = dataPath.resolve(HOBOLEAKS.createLatestPath());
			var archivePath = dataPath.resolve(HOBOLEAKS.createArchivePath(Instant.now()));
			var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, latestCacheTime);
			var archivePut = s3Util.putPublicObjectRequest(outputFile.length(), archivePath, archiveCacheTime);
			log.info(String.format("Uploading latest file to %s", latestPath));
			log.info(String.format("Uploading archive file to %s", archivePath));
			return Completable.mergeArray(
							s3Adapter.putObject(latestPut, outputFile, s3Client).ignoreElement(),
							s3Adapter
									.putObject(archivePut, outputFile, s3Client)
									.ignoreElement())
					.andThen(Completable.defer(() -> dataIndexHelper.updateIndex(latestPath, archivePath)));
		});
	}
}
