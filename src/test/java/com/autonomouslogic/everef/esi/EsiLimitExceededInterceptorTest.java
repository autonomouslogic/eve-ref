package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.autonomouslogic.everef.http.EsiLimitExceededInterceptor;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "10")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@Log4j2
@Timeout(30)
public class EsiLimitExceededInterceptorTest {
	@Inject
	TestDataUtil testDataUtil;

	@Inject
	EsiHelper esiHelper;

	MockWebServer server;

	volatile boolean isLimited;
	String limitBody;
	int limitStatus;
	Instant limitResetTime;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		isLimited = false;
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				if (isLimited) {
					var response = new MockResponse().setResponseCode(limitStatus);
					if (limitBody != null) {
						response.setBody(limitBody);
					}
					if (limitResetTime != null) {
						response.setHeader(
								EsiLimitExceededInterceptor.RESET_TIME_HEADER,
								Duration.between(Instant.now(), limitResetTime)
										.truncatedTo(ChronoUnit.SECONDS)
										.toSeconds());
					}
					return response;
				}
				return new MockResponse().setResponseCode(200);
			}
		});
		server.start(TestDataUtil.TEST_PORT);

		// testDataUtil.mockResponse("https://esi.evetech.net/latest/page?datasource=tranquility&language=en");
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@ParameterizedTest
	@ValueSource(strings = {"code", "text"})
	@SneakyThrows
	void shouldStopRequests(String type) {
		var count = new AtomicInteger(0);
		var threads = new ArrayList<Thread>();
		// Start threads to constantly query.
		for (int i = 0; i < 4; i++) {
			var thread = new Thread(() -> {
				while (true) {
					try {
						esiHelper
								.fetch(EsiUrl.builder().urlPath("/page").build())
								.blockingGet();
					} catch (Exception e) {
						log.warn("Fail", e);
					}
					count.incrementAndGet();
				}
			});
			threads.add(thread);
			thread.start();
		}
		// Ensure requests are running.
		log.info("Running");
		Thread.sleep(500);
		log.info("Count (1): " + count.get());
		assertNotEquals(0, count.get());
		// Execute 420 request, which should initiate a global stop.
		log.info("Returning 420");
		limitResetTime = Instant.now().plusSeconds(5);
		switch (type) {
			case "code":
				limitStatus = 420;
				limitBody = null;
				break;
			case "text":
				limitStatus = 200;
				limitBody = EsiLimitExceededInterceptor.ESI_420_TEXT;
				break;
		}
		isLimited = true;
		log.info("Count (2): " + count.get());
		Thread.sleep(1500);
		count.set(0);
		log.info("Count (3): " + count.get());
		Thread.sleep(3500);
		log.info("Count (4): " + count.get());
		assertEquals(0, count.get());
		isLimited = false;
		// Ensure requests are running again.
		log.info("Resuming");
		Thread.sleep(5000);
		log.info("Count (6): " + count.get());
		assertNotEquals(0, count.get());

		threads.forEach(Thread::interrupt);
	}
}
