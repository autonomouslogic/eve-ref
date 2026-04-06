package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.StaticDataMeta;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.DiscordNotifier;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class SyncStaticData implements Command {
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

	@Inject
	protected DiscordNotifier discordNotifier;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

	private S3Url dataPath;
	private final URI staticDataUrl =
			URI.create("https://developers.eveonline.com/static-data/tranquility/latest.jsonl");
	private final URI schemaUrl =
			URI.create("https://developers.eveonline.com/static-data/tranquility/schema-changelog.yaml");

	@Inject
	protected SyncStaticData() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		//		dataUrl = (HttpUrl) urlParser.parse(Configs.DATA_BASE_URL.getRequired());
	}

	@Override
	public void run() {
		var latest = loadLatestFiles();
		var newFile = false;
		newFile |= syncFile(latest, ArchivePathFactory.SDE_V2_JSONL, "jsonl");
		newFile |= syncFile(latest, ArchivePathFactory.SDE_V2_YAML, "yaml");
		if (newFile) {
			discordNotifier.notifyDiscord(String.format("New SDE released: %s", latest.getBuildNumber()));
		}
		syncSchema();
	}

	@SneakyThrows
	private StaticDataMeta loadLatestFiles() {
		try (var response = okHttpWrapper.get(staticDataUrl.toString())) {
			if (response.code() != 200) {
				throw new RuntimeException("Failed to fetch " + staticDataUrl + ": " + response.code());
			}
			var latest = objectMapper.readValue(response.body().byteStream(), StaticDataMeta.class);
			if (latest == null || latest.getKey() == null || !latest.getKey().equals("sde")) {
				throw new RuntimeException("Unable to find latest static-data file");
			}
			return latest;
		}
	}

	@SneakyThrows
	private boolean syncFile(StaticDataMeta latest, ArchivePathFactory type, String variant) {
		var latestUri = dataUrl(latest.getBuildNumber(), variant);

		var latestPath = getArchivePath(latest.getReleaseDate(), type, latest.getBuildNumber());
		var existing =
				s3Adapter.listObjects(latestPath, false, s3Client).toList().blockingGet();
		if (!existing.isEmpty()) {
			log.info(String.format("Latest file already exists at %s", latestPath));
			return false;
		}

		var file = tempFiles.tempFile("sde-" + variant, ".zip").toFile();
		try (var response = okHttpWrapper.download(latestUri.toString(), file)) {
			if (response.code() != 200) {
				throw new RuntimeException("Failed to download " + latestUri + ": " + response.code());
			}
			Files.setLastModifiedTime(file.toPath(), FileTime.from(latest.getReleaseDate()));
			uploadFile(file, type, latest.getBuildNumber());
			return true;
		}
	}

	/**
	 * Uploads the final file to S3.
	 * @return
	 */
	@SneakyThrows
	private void uploadFile(File outputFile, ArchivePathFactory type, int buildNumber) {
		var latestPath = dataPath.resolve(type.createLatestPath());
		var filetime = Files.getLastModifiedTime(outputFile.toPath()).toInstant();
		var archivePath = getArchivePath(filetime, type, buildNumber);
		var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, latestCacheTime);
		var archivePut = s3Util.putPublicObjectRequest(outputFile.length(), archivePath, archiveCacheTime);
		log.info(String.format("Uploading latest file to %s", latestPath));
		log.info(String.format("Uploading archive file to %s", archivePath));
		s3Adapter.putObject(latestPut, outputFile, s3Client).blockingGet();
		s3Adapter.putObject(archivePut, outputFile, s3Client).blockingGet();
		dataIndexHelper.updateIndex(latestPath, archivePath).blockingAwait();
	}

	private S3Url getArchivePath(Instant filetime, ArchivePathFactory type, int buildNumber) throws IOException {
		var archivePathBase = type.createArchivePath(filetime);
		archivePathBase =
				archivePathBase.replace("eve-online-static-data-", "eve-online-static-data-" + buildNumber + "-");
		var archivePath = dataPath.resolve(archivePathBase);
		return archivePath;
	}

	private URI dataUrl(int buildNumber, String variant) {
		return URI.create(String.format(
				"https://developers.eveonline.com/static-data/tranquility/eve-online-static-data-%s-%s.zip",
				buildNumber, variant));
	}

	private void syncSchema() {
		var file = tempFiles.tempFile("schema", ".yaml").toFile();
		try (var response = okHttpWrapper.download(schemaUrl.toString(), file)) {
			if (response.code() != 200) {
				throw new RuntimeException("Failed to download " + schemaUrl + ": " + response.code());
			}
			var name = FilenameUtils.getName(schemaUrl.getPath());
			var folder = ArchivePathFactory.SDE_V2_JSONL.getFolder();
			var s3Url = dataPath.resolve(folder + "/" + name);
			var latestPut = s3Util.putPublicObjectRequest(file.length(), s3Url, latestCacheTime);
			s3Adapter.putObject(latestPut, file, s3Client).blockingGet();
			dataIndexHelper.updateIndex(s3Url, s3Url).blockingAwait();
		}
	}
}
