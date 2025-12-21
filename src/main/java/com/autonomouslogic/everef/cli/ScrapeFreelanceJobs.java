package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.FREELANCE_JOBS;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import software.amazon.awssdk.services.s3.S3AsyncClient;

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

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	@Setter
	private ZonedDateTime scrapeTime;

	private final Duration latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
	private final Duration archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();
	private S3Url dataPath;
	private URI dataBaseUrl;

	@Inject
	protected ScrapeFreelanceJobs() {}

	@Inject
	protected void init() {
		dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		dataBaseUrl = Configs.DATA_BASE_URL.getRequired();
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
			fetchJobDetail(job, existingJobs, detailedJobs);
		}

		log.info("Retrieved {} new/updated jobs from ESI", detailedJobs.size());

		var mergedJobs = mergeJobs(existingJobs, detailedJobs, jobs);
		log.info("Total jobs after merge: {}", mergedJobs.size());

		var outputFile = buildOutput(mergedJobs);
		uploadFiles(outputFile);
	}

	private JsonNode fetchIndex() {
		var indexUrl = EsiUrl.modern().urlPath("/freelance-jobs?limit=100").build();
		var indexResponse = esiHelper.fetch(indexUrl);
		var indexData = esiHelper.decodeResponse(indexResponse);

		var jobsArray = indexData.get("freelance_jobs");
		if (jobsArray == null || !jobsArray.isArray()) {
			throw new IllegalStateException("No freelance_jobs array found in response");
		}

		log.debug("Retrieved {} jobs from index", jobsArray.size());
		return jobsArray;
	}

	private void fetchJobDetail(JsonNode job, Map<String, JsonNode> existingJobs, Map<String, JsonNode> detailedJobs) {
		var jobId = job.get("id");
		if (jobId == null || jobId.isNull()) {
			log.warn("Job entry missing ID, skipping");
			return;
		}

		var jobIdString = jobId.asText();

		// Check if we already have this job and if it needs updating
		var existingJob = existingJobs.get(jobIdString);
		if (existingJob != null && !shouldUpdateJob(job, existingJob)) {
			return;
		}

		log.trace("Fetching details for job {}", jobIdString);
		var detailUrl =
				EsiUrl.modern().urlPath("/freelance-jobs/" + jobIdString).build();
		var detailResponse = esiHelper.fetch(detailUrl);
		var detailData = esiHelper.decodeResponse(detailResponse);

		detailedJobs.put(jobIdString, detailData);
	}

	private boolean shouldUpdateJob(JsonNode indexJob, JsonNode existingJob) {
		var indexLastModified = indexJob.get("last_modified");
		var existingLastModified = existingJob.get("last_modified");

		if (indexLastModified == null || indexLastModified.isNull()) {
			log.warn("Job in index missing last_modified, will update");
			return true;
		}

		if (existingLastModified == null || existingLastModified.isNull()) {
			log.warn("Existing job missing last_modified, will update");
			return true;
		}

		var indexTime = ZonedDateTime.parse(indexLastModified.asText());
		var existingTime = ZonedDateTime.parse(existingLastModified.asText());

		return existingTime.isBefore(indexTime);
	}

	@SneakyThrows
	private File buildOutput(Map<String, JsonNode> jobs) {
		var file = tempFiles.tempFile("freelance-jobs", ".json").toFile();
		log.debug("Writing output file to {}", file);
		objectMapper.writeValue(file, jobs);
		return file;
	}

	/**
	 * Uploads the final file to S3.
	 */
	@SneakyThrows
	private void uploadFiles(@NonNull File outputFile) {
		log.info("Uploading files");
		var compressedFile = CompressUtil.compressBzip2(outputFile);
		var latestPath = dataPath.resolve(FREELANCE_JOBS.createLatestPath());
		var archivePath = dataPath.resolve(FREELANCE_JOBS.createArchivePath(scrapeTime));
		var latestPut = s3Util.putPublicObjectRequest(compressedFile.length(), latestPath, latestCacheTime);
		var archivePut = s3Util.putPublicObjectRequest(compressedFile.length(), archivePath, archiveCacheTime);
		log.debug(String.format("Uploading latest file to %s", latestPath));
		log.debug(String.format("Uploading archive file to %s", archivePath));

		s3Adapter.putObject(latestPut, compressedFile, s3Client).blockingGet();
		s3Adapter.putObject(archivePut, compressedFile, s3Client).blockingGet();
		dataIndexHelper.updateIndex(latestPath, archivePath).blockingAwait();
	}

	/**
	 * Downloads the existing latest freelance jobs file from the data site over HTTP.
	 * Returns an empty map if the file doesn't exist yet.
	 */
	@SneakyThrows
	private Map<String, JsonNode> downloadExistingJobs() {
		var url = dataBaseUrl.resolve(FREELANCE_JOBS.createLatestPath()).toString();
		var file = tempFiles.tempFile("freelance-jobs-existing", ".json.bz2").toFile();
		log.debug("Downloading existing jobs from {}", url);

		try (var response = okHttpWrapper.download(url, file)) {
			if (response.code() == 404) {
				log.warn("No existing freelance jobs file found, starting fresh");
				return new HashMap<>();
			}
			if (response.code() != 200) {
				throw new RuntimeException(String.format("Failed downloading existing jobs: HTTP %d", response.code()));
			}

			// Decompress and parse the bz2 file
			try (var in = new BZip2CompressorInputStream(new FileInputStream(file))) {
				return objectMapper.readValue(
						in,
						objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, ObjectNode.class));
			} finally {
				file.delete();
			}
		}
	}

	/**
	 * Merges existing jobs with newly fetched jobs.
	 * New jobs overwrite existing ones with the same ID.
	 * Jobs not in the current index are excluded.
	 */
	private Map<String, JsonNode> mergeJobs(
			Map<String, JsonNode> existingJobs, Map<String, JsonNode> newJobs, JsonNode indexJobs) {
		var merged = new HashMap<String, JsonNode>();

		// Only include existing jobs that are still in the current index
		for (var indexJob : indexJobs) {
			var jobId = indexJob.get("id");
			if (jobId != null && !jobId.isNull()) {
				var jobIdString = jobId.asText();
				var existingJob = existingJobs.get(jobIdString);
				if (existingJob != null) {
					merged.put(jobIdString, existingJob);
				}
			}
		}

		// Overwrite with newly fetched jobs
		merged.putAll(newJobs);
		return merged;
	}
}
