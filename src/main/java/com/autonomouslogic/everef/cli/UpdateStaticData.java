package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.HOBOLEAKS;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import io.reactivex.rxjava3.core.Completable;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class UpdateStaticData implements Command {
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
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	private S3Url dataPath;
	//	private HttpUrl dataUrl;
	private final URI staticDataUrl = Configs.STATIC_DATA_URL.getRequired();

	@Inject
	protected UpdateStaticData() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		//		dataUrl = (HttpUrl) urlParser.parse(Configs.DATA_BASE_URL.getRequired());
	}

	@Override
	public void run() {
		var files = loadLatestFiles();
		for (var file : files) {
			syncFile(file);
		}
	}

	@SneakyThrows
	private List<URL> loadLatestFiles() {
		try (var response = okHttpWrapper.get(staticDataUrl.toString())) {
			if (response.code() != 200) {
				throw new RuntimeException("Failed to fetch " + staticDataUrl + ": " + response.code());
			}
			var latest = objectMapper.readValue(
					response.body().byteStream(), Map.class);
			if (latest == null || !latest.containsKey("_key") || !latest.get("_key").equals("sde")) {
				throw new RuntimeException("Unable to find latest static-data file");
			}
			return null;
		}
	}

	private void syncFile(URL file) {
		log.info("Syncing file: {}", file);
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
