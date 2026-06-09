package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.autonomouslogic.everef.util.archive.StandardArchivePathFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZonedDateTime;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Generic scraper for fetching JSON from a URL, validating it, compressing to .bz2,
 * and uploading both uncompressed (latest) and compressed (archive) versions to S3.
 */
@Singleton
@Log4j2
public class GenericHistoryScraper {
	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected GenericHistoryScraper() {}

	private S3Url dataPath;

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	/**
	 * Fetches JSON from a URL, validates it, compresses to .bz2, and uploads both latest
	 * and archive versions to S3.
	 *
	 * @param url the URL to fetch JSON from
	 * @param pathFactory the factory for creating latest and archive paths
	 * @param scrapeTime the timestamp to use for the archive path
	 * @throws RuntimeException if any step fails (HTTP, JSON validation, or S3 upload)
	 */
	@SneakyThrows
	public void fetchAndUpload(
			@NonNull String url, @NonNull StandardArchivePathFactory pathFactory, @NonNull ZonedDateTime scrapeTime) {
		log.debug("Fetching JSON from {}", url);

		// Download JSON to temporary file
		var jsonFile = tempFiles.tempFile("scrape", ".json").toFile();
		try (var response = okHttpWrapper.get(url)) {
			if (response.code() != 200) {
				throw new RuntimeException(String.format("Failed to fetch %s: HTTP %d", url, response.code()));
			}
			try (var in = response.body().byteStream();
					var out = new java.io.FileOutputStream(jsonFile)) {
				org.apache.commons.io.IOUtils.copy(in, out);
			}
		}

		// Validate JSON is parseable
		try {
			objectMapper.readTree(jsonFile);
		} catch (Exception e) {
			jsonFile.delete();
			throw new RuntimeException(String.format("Invalid JSON from %s: %s", url, e.getMessage()), e);
		}

		// Compress to .bz2
		var compressedFile = CompressUtil.compressBzip2(jsonFile);

		try {
			// Upload both latest (uncompressed) and archive (compressed) versions
			s3Util.uploadLatestAndArchive(
					jsonFile,
					compressedFile,
					dataPath,
					pathFactory,
					scrapeTime,
					"application/json",
					"application/x-bzip2",
					s3Client);
			log.info("Successfully uploaded {} to latest and archive", pathFactory.getFolder());
		} finally {
			// Clean up temporary files
			jsonFile.delete();
			compressedFile.delete();
		}
	}
}
