package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
public class ScrapeFreelanceJobsTest {
	@Inject
	ScrapeFreelanceJobs scrapeFreelanceJobs;

	@Inject
	ObjectMapper objectMapper;

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

	record JobJson(ObjectNode job, ObjectNode jsonNodes) {}
}
