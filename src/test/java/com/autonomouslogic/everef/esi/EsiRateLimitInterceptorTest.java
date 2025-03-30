package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.google.common.base.Stopwatch;
import java.time.Duration;
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
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "5")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@Log4j2
public class EsiRateLimitInterceptorTest {
	@Inject
	TestDataUtil testDataUtil;

	@Inject
	EsiHelper esiHelper;

	MockWebServer server;

	final Duration duration = Duration.ofSeconds(5);

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
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

	@Test
	@SneakyThrows
	void shouldLimitRequests() {
		var count = new AtomicInteger(0);
		var watch = Stopwatch.createStarted();
		while (watch.elapsed().compareTo(duration) < 0) {
			try (var response =
					esiHelper.fetch(EsiUrl.builder().urlPath("/page").build())) {}
			count.incrementAndGet();
		}
		var time = watch.elapsed().toMillis();
		var rate = count.get() / (time / 1000.0);
		log.info(String.format("Requests: %s, time: %s, rate: %.2f/s", count.get(), watch.elapsed(), rate));
		assertEquals(5.0, rate, 1.0);
	}

	@Test
	@SneakyThrows
	void shouldLimitMultithreadedRequests() {
		var count = new AtomicInteger(0);
		var watch = Stopwatch.createStarted();
		var threads = new ArrayList<Thread>();
		for (int i = 0; i < 4; i++) {
			var thread = new Thread(() -> {
				while (watch.elapsed().compareTo(duration) < 0) {
					try (var response =
							esiHelper.fetch(EsiUrl.builder().urlPath("/page").build())) {}
					count.incrementAndGet();
				}
			});
			threads.add(thread);
			thread.start();
		}
		while (threads.stream().anyMatch(Thread::isAlive)) {
			Thread.sleep(100);
		}
		var time = watch.elapsed().toMillis();
		var rate = count.get() / (time / 1000.0);
		log.info(String.format("Requests: %s, time: %s, rate: %.2f/s", count.get(), watch.elapsed(), rate));
		assertEquals(5.0, rate, 1.0);
	}
}
