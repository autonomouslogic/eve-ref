package com.autonomouslogic.everef.cli.markethistory;

import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.openapi.esi.models.GetUniverseTypesTypeIdOk;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import okio.Buffer;
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

/**
 * <ul>
 *     <li><code>2023-01-01</code> and <code>2023-01-02</code> exist in the downloaded history.</li>
 *     <li><code>2023-01-01</code> will return from the ESI exactly what's in the file, so data for that date shouldn't be uploaded</li>
 *     <li><code>2023-01-02</code> and <code>2023-01-03</code> will have updated entries from the ESI. Only these two dates will be updated.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeMarketHistoryTest.BUCKET_NAME + "/data/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/esi")
// @Timeout(20)
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
		scrapeMarketHistory
				.setMinDate(LocalDate.parse("2023-01-01"))
				.setToday(LocalDate.parse("2023-01-03"))
				.run()
				.blockingAwait();
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				var typeId = request.getRequestUrl().queryParameter("type_id");
				var segments = request.getRequestUrl().pathSegments();
				if (path.equals("/data/")) {
					return mockResponse(ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/data.html"));
				}
				if (path.equals("/data/market-history/")) {
					return mockResponse(
							ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/market-history.html"));
				}
				if (path.equals("/data/market-history/2023/")) {
					return mockResponse(
							ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/market-history-2023.html"));
				}
				if (path.equals("/data/market-history/2023/market-history-2023-01-01.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2023-01-01"));
				}
				if (path.equals("/data/market-history/2023/market-history-2023-01-02.csv.bz2")) {
					return mockHistoricalDate(LocalDate.parse("2023-01-02"));
				}
				if (path.equals("/esi/markets/10000001/types/")) {
					return mockResponse("[20,21]");
				}
				if (path.equals("/esi/markets/10000002/types/")) {
					return mockResponse("[20,21]");
				}
				if (path.startsWith("/esi/markets/") && segments.get(3).equals("history")) {
					var regionId = segments.get(2);
					return mockHistory(regionId, typeId);
				}
				fail(String.format("Unaccounted for URL: %s", path));
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				fail("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}

	@NotNull
	private MockResponse mockResponse(String body) {
		return new MockResponse().setResponseCode(200).setBody(body).addHeader("last-modified", lastModified);
	}

	@NotNull
	private MockResponse mockResponse(byte[] body) {
		return new MockResponse()
				.setResponseCode(200)
				.setBody(new Buffer().write(body))
				.addHeader("last-modified", lastModified);
	}

	@NotNull
	@SneakyThrows
	private MockResponse mockResponse(InputStream in) {
		try (in) {
			var body = IOUtils.toByteArray(in);
			return mockResponse(body);
		}
	}

	@SneakyThrows
	private MockResponse mockHistoricalDate(LocalDate date) {
		var file = ResourceUtil.loadContextual(ScrapeMarketHistoryTest.class, "/" + date.toString() + ".csv");
		var compressed = new ByteArrayOutputStream();
		try (var out = new BZip2CompressorOutputStream(compressed)) {
			IOUtils.copy(file, out);
		}
		return mockResponse(new ByteArrayInputStream(compressed.toByteArray()));
	}

	@SneakyThrows
	private MockResponse mockHistory(String regionId, String typeId) {
		return mockResponse(ResourceUtil.loadContextual(
				ScrapeMarketHistoryTest.class, String.format("/%s-%s.json", regionId, typeId)));
	}
}
