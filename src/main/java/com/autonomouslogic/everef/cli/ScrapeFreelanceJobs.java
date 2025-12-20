package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.FREELANCE_JOBS;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Scrapes freelance jobs from the ESI API.
 */
@Log4j2
public class ScrapeFreelanceJobs implements Command {
	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ObjectMapper objectMapper;

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
	protected TempFiles tempFiles;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Setter
	private ZonedDateTime scrapeTime;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private S3Url dataPath;

	@Inject
	protected ScrapeFreelanceJobs() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
	}

	@Override
	@SneakyThrows
	public void run() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		}

		log.info("Starting freelance jobs scrape");

		var existingJobs = downloadExistingJobs();
		log.info("Downloaded {} existing jobs", existingJobs.size());

		var jobs = fetchIndex();

		var detailedJobs = new HashMap<String, JsonNode>();
		for (var job : jobs) {
			fetchJobDetail(job, detailedJobs);
		}

		log.info("Retrieved {} jobs from ESI", detailedJobs.size());

		var mergedJobs = mergeJobs(existingJobs, detailedJobs);
		log.info("Total jobs after merge: {}", mergedJobs.size());

		var outputFile = buildOutput(mergedJobs);
		uploadFiles(outputFile);
	}

	private JsonNode fetchIndex() {
		var indexUrl = EsiUrl.modern().urlPath("/freelance-jobs").build();
		var indexResponse = esiHelper.fetch(indexUrl);
		var indexData = esiHelper.decodeResponse(indexResponse);

		var jobsArray = indexData.get("freelance_jobs");
		if (jobsArray == null || !jobsArray.isArray()) {
			throw new IllegalStateException("No freelance_jobs array found in response");
		}

		log.info("Retrieved {} jobs from index", jobsArray.size());
		return jobsArray;
	}

	private void fetchJobDetail(JsonNode job, Map<String, JsonNode> detailedJobs) {
		var jobId = job.get("id");
		if (jobId == null || jobId.isNull()) {
			log.warn("Job entry missing ID, skipping");
			return;
		}

		var jobIdString = jobId.asText();
		var detailUrl =
				EsiUrl.modern().urlPath("/freelance-jobs/" + jobIdString).build();
		var detailResponse = esiHelper.fetch(detailUrl);
		var detailData = esiHelper.decodeResponse(detailResponse);

		detailedJobs.put(jobIdString, detailData);
	}

	@SneakyThrows
	private File buildOutput(Map<String, JsonNode> jobs) {
		var file = tempFiles.tempFile("freelance-jobs", ".json").toFile();
		log.info("Writing output file to {}", file);
		objectMapper.writeValue(file, jobs);
		return file;
	}

	/**
	 * Uploads the final file to S3.
	 */
	@SneakyThrows
	private void uploadFiles(@NonNull File outputFile) {
		log.info("Uploading files");
		var archiveFile = CompressUtil.compressBzip2(outputFile);
		var latestPath = dataPath.resolve(FREELANCE_JOBS.createLatestPath());
		var archivePath = dataPath.resolve(FREELANCE_JOBS.createArchivePath(scrapeTime));
		var latestPut = s3Util.putPublicObjectRequest(outputFile.length(), latestPath, latestCacheTime);
		var archivePut = s3Util.putPublicObjectRequest(archiveFile.length(), archivePath, archiveCacheTime);
		log.info(String.format("Uploading latest file to %s", latestPath));
		log.info(String.format("Uploading archive file to %s", archivePath));

		s3Adapter.putObject(latestPut, outputFile, s3Client).blockingGet();
		s3Adapter.putObject(archivePut, archiveFile, s3Client).blockingGet();
		dataIndexHelper.updateIndex(latestPath, archivePath).blockingAwait();
	}

	/**
	 * Downloads the existing latest freelance jobs file from S3.
	 * Returns an empty map if the file doesn't exist yet.
	 */
	@SneakyThrows
	private Map<String, JsonNode> downloadExistingJobs() {
		try {
			var latestPath = dataPath.resolve(FREELANCE_JOBS.createLatestPath());
			var get = s3Util.getObjectRequest(latestPath);
			log.debug("Downloading existing jobs from {}", latestPath);
			var result = s3Adapter.getObject(get, s3Client).blockingGet();
			var bytes = IOUtils.toByteArray(new FileInputStream(result.getRight().toFile()));
			result.getRight().toFile().delete();
			return objectMapper.readValue(
					bytes, objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, JsonNode.class));
		} catch (CompletionException e) {
			if (e.getCause() instanceof NoSuchKeyException) {
				log.info("No existing freelance jobs file found, starting fresh");
				return new HashMap<>();
			}
			throw e;
		}
	}

	/**
	 * Merges existing jobs with newly fetched jobs.
	 * New jobs overwrite existing ones with the same ID.
	 */
	private Map<String, JsonNode> mergeJobs(
			Map<String, JsonNode> existingJobs, Map<String, JsonNode> newJobs) {
		var merged = new HashMap<>(existingJobs);
		merged.putAll(newJobs);
		return merged;
	}
}
