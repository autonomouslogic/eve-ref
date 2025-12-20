package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeFreelanceJobsTest.BUCKET_NAME + "/base/")
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
	AtomicInteger counter = new AtomicInteger();

	List<ObjectNode> freelanceJobs;
	Map<String, ObjectNode> freelanceJobsById;

	@Inject
	protected ScrapeFreelanceJobsTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		freelanceJobs = new ArrayList<>();
		freelanceJobsById = new HashMap<>();

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

		var request = server.takeRequest();
		assertEquals("/freelance-jobs", request.getRequestUrl().encodedPath());
	}

	@Test
	@SneakyThrows
	void shouldFetchJobsDetail() {
		createFreelanceJob(Instant.now());
		createFreelanceJob(Instant.now());

		scrapeFreelanceJobs.run();

		var indexRequest = server.takeRequest();
		assertEquals("/freelance-jobs", indexRequest.getRequestUrl().encodedPath());

		var detail1Request = server.takeRequest();
		assertEquals("/freelance-jobs/id-1", detail1Request.getRequestUrl().encodedPath());

		var detail2Request = server.takeRequest();
		assertEquals("/freelance-jobs/id-2", detail2Request.getRequestUrl().encodedPath());
	}

	@Test
	@SneakyThrows
	void shouldUploadJsonFile() {
		createFreelanceJob(Instant.now());
		createFreelanceJob(Instant.now());

		scrapeFreelanceJobs.run();

		// Consume all requests
		server.takeRequest(); // index
		server.takeRequest(); // detail 1
		server.takeRequest(); // detail 2

		var latestFile = "base/freelance-jobs/freelance-jobs-latest.json";
		var latestBytes =
				mockS3Adapter.getTestObject(BUCKET_NAME, latestFile, dataClient).orElseThrow();

		var latestJson = (ObjectNode) objectMapper.readTree(latestBytes);
		assertNotNull(latestJson.get("id-1"));
		assertNotNull(latestJson.get("id-2"));
		assertEquals("name-1", latestJson.get("id-1").get("name").asText());
		assertEquals("name-2", latestJson.get("id-2").get("name").asText());
	}

	@Test
	@SneakyThrows
	void shouldUploadArchiveFile() {
		var job1 = createFreelanceJob(Instant.now());
		var job2 = createFreelanceJob(Instant.now());

		var time = ZonedDateTime.parse("2020-01-02T03:04:05Z");
		scrapeFreelanceJobs.setScrapeTime(time);
		scrapeFreelanceJobs.run();

		// Consume all requests
		server.takeRequest(); // index
		server.takeRequest(); // detail 1
		server.takeRequest(); // detail 2

		var archiveFile = "base/freelance-jobs/history/2020/2020-01-02/freelance-jobs-2020-01-02_03-04-05.json.bz2";
		var compressedBytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, archiveFile, dataClient)
				.orElseThrow();

		var latestFile = "base/freelance-jobs/freelance-jobs-latest.json";
		var latestBytes = mockS3Adapter
				.getTestObject(BUCKET_NAME, latestFile, dataClient)
				.orElseThrow();

		// Verify the latest file (uncompressed)
		var latestJson = (ObjectNode) objectMapper.readTree(latestBytes);
		var expected = objectMapper.createObjectNode();
		expected.put(job1.job().get("id").asText(), job1.detail());
		expected.put(job2.job().get("id").asText(), job2.detail());
		assertEquals(expected, latestJson);

		// Decompress the bz2 archive file and verify it has the same content as latest
		try (var decompressed = new BZip2CompressorInputStream(new ByteArrayInputStream(compressedBytes))) {
			var archiveJson = (ObjectNode) objectMapper.readTree(decompressed);
			assertEquals(expected, archiveJson);
		}
	}

	@Test
	@SneakyThrows
	void shouldExecuteDataIndex() {
		createFreelanceJob(Instant.now());

		scrapeFreelanceJobs
				.setScrapeTime(ZonedDateTime.parse("2020-01-02T03:04:05Z"))
				.run();

		// Consume all requests
		server.takeRequest(); // index
		server.takeRequest(); // detail 1

		Mockito.verify(dataIndexHelper)
				.updateIndex(
						S3Url.builder()
								.bucket("data-bucket")
								.path("base/freelance-jobs/freelance-jobs-latest.json")
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
		// Create an existing job and upload it to S3
		var existingJob = objectMapper
				.createObjectNode()
				.put("id", "existing-job-1")
				.put("name", "Existing Job");
		var existingJobs = objectMapper.createObjectNode();
		existingJobs.set("existing-job-1", existingJob);

		var latestFile = "base/freelance-jobs/freelance-jobs-latest.json";
		var existingBytes = objectMapper.writeValueAsBytes(existingJobs);
		mockS3Adapter.putTestObject(BUCKET_NAME, latestFile, existingBytes, dataClient);

		// Create new jobs from ESI
		createFreelanceJob(Instant.now());
		createFreelanceJob(Instant.now());

		scrapeFreelanceJobs.run();

		// Consume all requests
		server.takeRequest(); // index
		server.takeRequest(); // detail 1
		server.takeRequest(); // detail 2

		// Verify the latest file contains both existing and new jobs
		var latestBytes =
				mockS3Adapter.getTestObject(BUCKET_NAME, latestFile, dataClient).orElseThrow();
		var latestJson = (ObjectNode) objectMapper.readTree(latestBytes);

		assertNotNull(latestJson.get("existing-job-1"));
		assertNotNull(latestJson.get("id-1"));
		assertNotNull(latestJson.get("id-2"));
		assertEquals("Existing Job", latestJson.get("existing-job-1").get("name").asText());
		assertEquals("name-1", latestJson.get("id-1").get("name").asText());
		assertEquals("name-2", latestJson.get("id-2").get("name").asText());
	}

	private JobJson createFreelanceJob(@NonNull Instant lastModified) {
		var id = counter.incrementAndGet();
		var jobId = "id-" + id;

		var job = objectMapper
				.createObjectNode()
				.put("id", jobId)
				.put("name", "name-" + id)
				.put("state", "Active")
				.put("last_modified", lastModified.toString());

		var progress = objectMapper.createObjectNode().put("current", "979900").put("desired", "999999999999999999");
		job.set("progress", progress);

		freelanceJobs.add(job);

		var detail = job.deepCopy();
		detail.put("details", objectMapper.createObjectNode().put("description", "System Mining Boost"));

		freelanceJobsById.put(jobId, detail);

		return new JobJson(job, detail);
	}

	@SneakyThrows
	private String buildJobsIndexResponse() {
		var response = objectMapper.createObjectNode();

		var cursor = objectMapper.createObjectNode();
		cursor.put("after", "7RWpqiyrSw");
		response.set("cursor", cursor);

		var jobsArray = objectMapper.createArrayNode();
		for (var job : freelanceJobs) {
			jobsArray.add(job);
		}
		response.set("freelance_jobs", jobsArray);

		return objectMapper.writeValueAsString(response);
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

				if (path.startsWith("/freelance-jobs/")) {
					var jobId = path.substring("/freelance-jobs/".length());
					var job = freelanceJobsById.get(jobId);
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
