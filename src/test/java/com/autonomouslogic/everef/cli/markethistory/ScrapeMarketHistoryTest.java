package com.autonomouslogic.everef.cli.markethistory;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseTypesTypeIdOk;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * <ul>
 *     <li><code>2023-01-01</code> and <code>2023-01-02</code> exist in the downloaded history.</li>
 *     <li><code>2023-01-01</code> will return from the ESI exactly what's in the file.</li>
 *     <li><code>2023-01-02</code> and <code>2023-01-03</code> will have updated entries from the ESI.<br/>
 *     Only these two dates will be updated.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMarketHistoryTest.BUCKET_NAME + "/data/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/sata/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/esi/")
public class ScrapeMarketHistoryTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	ScrapeMarketHistory scrapeMarketHistory;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	ObjectMapper objectMapper;

	final String lastModified = "Mon, 03 Apr 2023 03:47:30 GMT";
	final Instant lastModifiedInstant = Instant.parse("2023-04-03T03:47:30Z");
	final GetUniverseTypesTypeIdOk type = new GetUniverseTypesTypeIdOk(
			"", 0, "", true, 0, 0.0f, List.of(), List.of(), 0, 0, 0, 0.0f, 0.0f, 0, 0.0f, 0.0f);

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldScrapeMarketHistory() {
		scrapeMarketHistory.setMinDate(LocalDate.parse("2023-01-01")).run().blockingAwait();
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var segments = request.getRequestUrl().pathSegments();

				switch (path) {
					case "/market/history/":
						return mockResponse(
								ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "market-history.html"));
					case "/market/history/2022/":
						return mockResponse(
								ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "market-history-2022.html"));
				}
				log.error(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	@NotNull
	private MockResponse mockResponse(String body) {
		return new MockResponse().setResponseCode(200).setBody(body).addHeader("last-modified", lastModified);
	}

	@NotNull
	@SneakyThrows
	private MockResponse mockResponse(InputStream in) {
		try (in) {
			var body = IOUtils.toString(in, StandardCharsets.UTF_8);
			return mockResponse(body);
		}
	}
}
