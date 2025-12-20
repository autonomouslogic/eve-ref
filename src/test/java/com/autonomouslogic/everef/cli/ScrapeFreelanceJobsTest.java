package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeFreelanceJobsTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
public class ScrapeFreelanceJobsTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	ScrapeFreelanceJobs scrapeFreelanceJobs;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	DataIndexHelper dataIndexHelper;

	MockWebServer server;

	List<ObjectNode> jobsIndex;
	Map<String, ObjectNode> jobsDetail;
	byte[] existingLatestFile;

	@Inject
	protected ScrapeFreelanceJobsTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		jobsIndex = new ArrayList<>();
		jobsDetail = new HashMap<>();
		existingLatestFile = null;

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		try {
			assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
		} finally {
			server.close();
		}
	}

	@Test
	@SneakyThrows
	void shouldFetchEmptyJobs() {
		scrapeFreelanceJobs.run();

		var latestRequest = server.takeRequest();
		assertEquals(
				"/freelance-jobs/freelance-jobs-latest.json.bz2",
				latestRequest.getRequestUrl().encodedPath());

		var indexRequest = server.takeRequest();
		assertEquals("/freelance-jobs", indexRequest.getRequestUrl().encodedPath());
	}

	@Test
	@SneakyThrows
	void shouldFetchJobsDetail() {
		createFreelanceJob(1, Instant.now());
		createFreelanceJob(2, Instant.now());

		scrapeFreelanceJobs.run();

		var latestRequest = server.takeRequest();
		assertEquals(
				"/freelance-jobs/freelance-jobs-latest.json.bz2",
				latestRequest.getRequestUrl().encodedPath());

		var indexRequest = server.takeRequest();
		assertEquals("/freelance-jobs", indexRequest.getRequestUrl().encodedPath());

		var job1request = server.takeRequest();
		assertEquals("/freelance-jobs/id-1", job1request.getRequestUrl().encodedPath());

		var job2request = server.takeRequest();
		assertEquals("/freelance-jobs/id-2", job2request.getRequestUrl().encodedPath());
	}

	@Test
	@SneakyThrows
	void shouldUploadArchiveFiles() {
		var job1 = createFreelanceJob(1, Instant.now());
		var job2 = createFreelanceJob(2, Instant.now());

		var time = ZonedDateTime.parse("2020-01-02T03:04:05Z");
		scrapeFreelanceJobs.setScrapeTime(time);
		scrapeFreelanceJobs.run();

		server.takeRequest(); // latest file download (404)
		server.takeRequest(); // index
		server.takeRequest(); // detail 1
		server.takeRequest(); // detail 2

		var archiveFile = "base/freelance-jobs/history/2020/2020-01-02/freelance-jobs-2020-01-02_03-04-05.json.bz2";
		var archiveBytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();

		var latestFile = "base/freelance-jobs/freelance-jobs-latest.json.bz2";
		var latestBytes =
				mockS3Adapter.getTestObject(BUCKET_NAME, latestFile, dataClient).orElseThrow();

		var expected = objectMapper.createObjectNode();
		expected.put(job1.job().get("id").asText(), job1.detail());
		expected.put(job2.job().get("id").asText(), job2.detail());

		var archiveJson = decompressArchive(archiveBytes);
		assertEquals(expected, archiveJson);

		var latestJson = decompressArchive(latestBytes);
		assertEquals(expected, latestJson);
	}

	@Test
	@SneakyThrows
	void shouldExecuteDataIndex() {
		createFreelanceJob(1, Instant.now());

		scrapeFreelanceJobs
				.setScrapeTime(ZonedDateTime.parse("2020-01-02T03:04:05Z"))
				.run();

		server.takeRequest(); // latest file download (404)
		server.takeRequest(); // index
		server.takeRequest(); // detail 1

		verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket("data-bucket")
								.path("base/freelance-jobs/freelance-jobs-latest.json.bz2")
								.build(),
						S3Url.builder()
								.bucket("data-bucket")
								.path(
										"base/freelance-jobs/history/2020/2020-01-02/freelance-jobs-2020-01-02_03-04-05.json.bz2")
								.build());
	}

	@Test
	@SneakyThrows
	void shouldMergeExistingJobs() {
		var existingJob = createFreelanceJob(1, Instant.parse("2020-01-01T00:00:00Z"), false);
		var existingJobs =
				objectMapper.createObjectNode().set(existingJob.job().get("id").asText(), existingJob.job());
		createExistingJobs(existingJobs);

		var job2 = createFreelanceJob(2, Instant.now());
		var job3 = createFreelanceJob(3, Instant.now());

		scrapeFreelanceJobs.run();

		server.takeRequest(); // latest file download (200)
		server.takeRequest(); // index
		server.takeRequest(); // detail 1
		server.takeRequest(); // detail 2

		var expected = objectMapper.createObjectNode();
		expected.set(existingJob.job().get("id").asText(), existingJob.job());
		expected.set(job2.job().get("id").asText(), existingJob.job());
		expected.set(job3.job().get("id").asText(), existingJob.job());

		var latestFile = "base/freelance-jobs/freelance-jobs-latest.json.bz2";
		var compressedBytes =
				mockS3Adapter.getTestObject(BUCKET_NAME, latestFile, dataClient).orElseThrow();
		var latestJson = decompressArchive(compressedBytes);
		assertEquals(expected, latestJson);
	}

	@Test
	@SneakyThrows
	void shouldSkipUnmodifiedJobs() {
		var lastModified = Instant.parse("2020-01-01T00:00:00Z");

		// Create an existing job with a specific last_modified
		var existingJobs = objectMapper.createObjectNode();
		existingJobs.set(
				"id-1",
				objectMapper
						.createObjectNode()
						.put("id", "id-1")
						.put("name", "Existing Job")
						.put("last_modified", lastModified.toString()));

		// Compress the existing jobs data
		createExistingJobs(existingJobs);

		// Create a job with the same last_modified (should be skipped)
		createFreelanceJob(1, lastModified);

		// Create a new job with a newer last_modified (should be fetched)
		createFreelanceJob(2, Instant.parse("2020-01-02T00:00:00Z"));

		scrapeFreelanceJobs.run();

		// Consume all requests
		server.takeRequest(); // latest file download (200)
		server.takeRequest(); // index
		// Note: No request for id-1 detail since it hasn't been modified
		server.takeRequest(); // detail for id-2 only

		// Verify no more requests were made
		assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));

		// Verify the latest file contains both jobs
		var latestFile = "base/freelance-jobs/freelance-jobs-latest.json.bz2";
		var compressedBytes =
				mockS3Adapter.getTestObject(BUCKET_NAME, latestFile, dataClient).orElseThrow();

		var latestJson = decompressArchive(compressedBytes);
		assertNotNull(latestJson.get("id-1"));
		assertNotNull(latestJson.get("id-2"));
		assertEquals("Existing Job", latestJson.get("id-1").get("name").asText());
		assertEquals("name-2", latestJson.get("id-2").get("name").asText());
	}

	private JobJson createFreelanceJob(int id, @NonNull Instant lastModified) {
		return createFreelanceJob(id, lastModified, true);
	}

	private JobJson createFreelanceJob(int id, @NonNull Instant lastModified, boolean add) {
		var jobId = "id-" + id;

		var job = objectMapper
				.createObjectNode()
				.put("id", jobId)
				.put("name", "name-" + id)
				.put("state", "Active")
				.put("last_modified", lastModified.toString());

		var progress = objectMapper.createObjectNode().put("current", "979900").put("desired", "999999999999999999");
		job.set("progress", progress);

		if (add) {
			jobsIndex.add(job);
		}

		var detail = job.deepCopy();
		detail.put("details", objectMapper.createObjectNode().put("description", "System Mining Boost"));

		if (add) {
			jobsDetail.put(jobId, detail);
		}

		return new JobJson(job, detail);
	}

	@SneakyThrows
	private String buildJobsIndexResponse() {
		var response = objectMapper.createObjectNode();

		var cursor = objectMapper.createObjectNode();
		cursor.put("after", "7RWpqiyrSw");
		response.set("cursor", cursor);

		var jobsArray = objectMapper.createArrayNode();
		for (var job : jobsIndex) {
			jobsArray.add(job);
		}
		response.set("freelance_jobs", jobsArray);

		return objectMapper.writeValueAsString(response);
	}

	private void createExistingJobs(@NonNull JsonNode existingJobs) throws IOException {
		var uncompressed = objectMapper.writeValueAsBytes(existingJobs);
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.write(uncompressed, out);
		}
		existingLatestFile = compressed.toByteArray();
	}

	private ObjectNode decompressArchive(byte[] archiveBytes) throws IOException {
		try (var decompressed = new BZip2CompressorInputStream(new ByteArrayInputStream(archiveBytes))) {
			return (ObjectNode) objectMapper.readTree(decompressed);
		}
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();

				if (path.equals("/freelance-jobs")) {
					return new MockResponse().setBody(buildJobsIndexResponse());
				}

				if (path.equals("/freelance-jobs/freelance-jobs-latest.json.bz2")) {
					if (existingLatestFile == null) {
						return new MockResponse().setResponseCode(404);
					}
					return new MockResponse().setBody(new Buffer().write(existingLatestFile));
				}

				if (path.startsWith("/freelance-jobs/")) {
					var jobId = path.substring("/freelance-jobs/".length());
					var job = jobsDetail.get(jobId);
					if (job != null) {
						return new MockResponse().setBody(objectMapper.writeValueAsString(job));
					}
					return new MockResponse().setResponseCode(404);
				}

				log.error("Unaccounted for URL: {}", path);
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	record JobJson(ObjectNode job, ObjectNode detail) {}
}
