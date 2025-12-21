package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import javax.inject.Inject;
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

/**
 * This
 */
@SetEnvironmentVariable(key = "ESI_DATASOURCE", value = "tranquility")
public class EsiModuleTest {
	@Inject
	UniverseEsi universeEsi;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				return new MockResponse()
						.setResponseCode(200)
						.setBody("[10000001, 10000002, 10000003]")
						.addHeader("Content-Type", "application/json");
			}
		});
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@SneakyThrows
	private void runTest(String prefix) throws InterruptedException {
		universeEsi.getRegionIds().blockingSubscribe();
		var request = server.takeRequest();
		assertEquals(prefix + "/universe/regions", request.getPath());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
	void shouldGetRegionsWithoutTrailingSlashOnDomain() {
		runTest("");
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/")
	void shouldGetRegionsWithTrailingSlashOnDomain() {
		runTest("");
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/base")
	void shouldGetRegionsWithoutTrailingSlashOnBasePath() {
		runTest("/base");
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT + "/base/")
	void shouldGetRegionsWithTrailingSlashOnDBasePath() {
		runTest("/base");
	}
}
