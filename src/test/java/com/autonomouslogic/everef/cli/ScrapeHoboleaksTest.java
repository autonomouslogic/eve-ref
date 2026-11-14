package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapeHoboleaksTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/data/")
@SetEnvironmentVariable(
		key = "HOBOLEAKS_SDE_DATA_BASE_URL",
		value = "http://localhost:" + TestDataUtil.TEST_PORT + "/tq/")
public class ScrapeHoboleaksTest {
	static final String BUCKET_NAME = "data-bucket";
	static final String META = "{\"revision\":1,\"files\":{\"a.json\":{}}}";

	@Inject
	ScrapeHoboleaks scrapeHoboleaks;

	@Inject
	MockS3Adapter mockS3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient dataClient;

	MockWebServer server;
	List<String> requestPaths;

	@Inject
	protected ScrapeHoboleaksTest() {}

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		requestPaths = new ArrayList<>();
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest request) {
				requestPaths.add(request.getPath());
				return switch (request.getPath()) {
					case "/tq/meta.json" -> new MockResponse().setBody(META);
					case "/tq/a.json" -> new MockResponse().setBody("{}");
					default -> new MockResponse().setResponseCode(404);
				};
			}
		});
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFetchFromHoboleaksOutsideDataBasePath() {
		VirtualThreads.onVirtualThread(scrapeHoboleaks::run);

		assertTrue(requestPaths.contains("/tq/meta.json"));
		assertTrue(requestPaths.contains("/tq/a.json"));
		var uploaded = mockS3Adapter.getTestObject(
				BUCKET_NAME, "base/hoboleaks-sde/hoboleaks-sde-latest.tar.xz", dataClient);
		assertTrue(uploaded.isPresent());
	}
}
