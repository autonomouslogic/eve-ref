package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
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
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + SyncMerTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
@SetEnvironmentVariable(key = "MER_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
public class SyncMerTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	SyncMer syncMer;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	@Inject
	DataIndexHelper dataIndexHelper;

	MockWebServer server;
	Map<String, byte[]> availableMerFiles;

	@Inject
	protected SyncMerTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		availableMerFiles = new HashMap<>();
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
	void shouldDownloadMissingMerFiles() {
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202604.zip", "existing-202604".getBytes(), dataClient);

		availableMerFiles.put("202605", "content-202605".getBytes());

		syncMer.run();

		var uploaded202605 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202605.zip", dataClient);
		assertEquals("content-202605", new String(uploaded202605.orElseThrow()));
	}

	@Test
	@SneakyThrows
	void shouldSkipExistingMerFiles() {
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202604.zip", "existing-202604".getBytes(), dataClient);

		availableMerFiles.put("202602", "content-202602".getBytes());
		availableMerFiles.put("202603", "content-202603".getBytes());

		syncMer.run();

		var uploaded202602 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202602.zip", dataClient);
		var uploaded202603 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202603.zip", dataClient);

		assertEquals("content-202602", new String(uploaded202602.orElseThrow()));
		assertEquals("content-202603", new String(uploaded202603.orElseThrow()));
	}

	@Test
	@SneakyThrows
	void shouldHandle404ForMissingMonths() {
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202604.zip", "existing-202604".getBytes(), dataClient);

		// Latest files not available in the mock server - will return 404

		syncMer.run();

		var notFound =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202605.zip", dataClient);
		assertEquals(true, notFound.isEmpty());
	}

	@Test
	@SneakyThrows
	void shouldDownloadMultipleMissingMonths() {
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202601.zip", "existing-202601".getBytes(), dataClient);
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202602.zip", "existing-202602".getBytes(), dataClient);

		availableMerFiles.put("202603", "content-202603".getBytes());
		availableMerFiles.put("202604", "content-202604".getBytes());
		availableMerFiles.put("202605", "content-202605".getBytes());

		syncMer.run();

		var uploaded202603 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202603.zip", dataClient);
		var uploaded202604 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202604.zip", dataClient);
		var uploaded202605 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202605.zip", dataClient);

		assertEquals("content-202603", new String(uploaded202603.orElseThrow()));
		assertEquals("content-202604", new String(uploaded202604.orElseThrow()));
		assertEquals("content-202605", new String(uploaded202605.orElseThrow()));
	}

	@Test
	@SneakyThrows
	void shouldUploadFilesWhenNewMerAvailable() {
		// Seed with old files
		mockS3Adapter.putTestObject(
				BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202604.zip", "existing-202604".getBytes(), dataClient);

		availableMerFiles.put("202605", "content-202605".getBytes());

		syncMer.run();

		// Verify file was uploaded
		var uploaded202605 =
				mockS3Adapter.getTestObject(BUCKET_NAME, "base/ccp/mer/2026/EVEOnline_MER_202605.zip", dataClient);
		assertEquals("content-202605", new String(uploaded202605.orElseThrow()));
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var url = request.getRequestUrl().toString();
				var path = request.getRequestUrl().encodedPath();

				log.debug("Received request: {}", url);

				// Match MER file requests (both from hardcoded CDN URL and test URL)
				var pattern = java.util.regex.Pattern.compile(".*/EVEOnline_MER_(\\d{6})\\.zip");
				var matcher = pattern.matcher(url);

				if (matcher.find()) {
					var monthStr = matcher.group(1);
					log.debug("Found MER file request for month: {}", monthStr);
					if (availableMerFiles.containsKey(monthStr)) {
						return new MockResponse()
								.setBody(new Buffer().write(availableMerFiles.get(monthStr)))
								.setHeader("Last-Modified", "Thu, 01 Jan 2026 12:00:00 GMT")
								.setResponseCode(200);
					}
					log.debug("MER file not available in mock: {}", monthStr);
					return new MockResponse().setResponseCode(404);
				}

				log.error("Unaccounted for URL: {}", url);
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
