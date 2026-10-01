package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.http.EsiErrorLimitHeaders;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
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
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "50")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "ESI_ERROR_LIMIT_MIN_REMAIN", value = "10")
@Log4j2
@Timeout(30)
public class EsiErrorLimitBudgetIntegrationTest {
	@Inject
	EsiHelper esiHelper;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
				return new MockResponse().setResponseCode(200);
			}
		});
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	/**
	 * A 420 response carries Remain=0, which both the existing 420 handler and the error-limit budget
	 * interceptor react to. They must not each wait the full reset time independently; the total elapsed
	 * time should look like a single wait.
	 */
	@Test
	@SneakyThrows
	void interactionWith420Handler_doesNotWaitTwice() {
		var hit = new AtomicInteger(0);
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
				if (hit.getAndIncrement() == 0) {
					return new MockResponse()
							.setResponseCode(420)
							.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 0)
							.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, 0);
				}
				return new MockResponse()
						.setResponseCode(200)
						.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 5)
						.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, 50);
			}
		});
		var start = Instant.now();
		esiHelper.fetch(EsiUrl.builder().urlPath("/page").build()).close();
		var elapsed = Duration.between(start, Instant.now());
		assertTrue(
				elapsed.compareTo(Duration.ofMillis(800)) >= 0, "expected roughly a single ~1s wait, took " + elapsed);
		assertTrue(
				elapsed.compareTo(Duration.ofSeconds(3)) < 0,
				"expected a single wait, not two sequential waits, took " + elapsed);
	}

	/**
	 * A low-Remain response should delay the next EsiHelper call, end to end through the real ESI client.
	 */
	@Test
	@SneakyThrows
	void lowRemainResponseDelaysFollowingCall() {
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
				return new MockResponse()
						.setResponseCode(200)
						.setHeader(EsiErrorLimitHeaders.REMAIN_HEADER, 5)
						.setHeader(EsiErrorLimitHeaders.RESET_TIME_HEADER, 2);
			}
		});
		esiHelper.fetch(EsiUrl.builder().urlPath("/page").build()).close();

		var start = Instant.now();
		esiHelper.fetch(EsiUrl.builder().urlPath("/page").build()).close();
		var elapsed = Duration.between(start, Instant.now());
		assertTrue(elapsed.compareTo(Duration.ofMillis(2500)) >= 0, "expected a wait of roughly 3s, took " + elapsed);
	}
}
