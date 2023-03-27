package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.ResponseBody;
import okhttp3.mock.MediaTypes;
import okhttp3.mock.MockInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "10")
@Log4j2
public class EsiLimitExceededInterceptorTest {
	@Inject
	TestDataUtil testDataUtil;

	@Inject
	EsiHelper esiHelper;

	@Inject
	MockInterceptor http;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		testDataUtil.mockResponse("https://esi.evetech.net/latest/page?datasource=tranquility&language=en");
		http.addRule()
				.get("https://esi.evetech.net/latest/420-code?datasource=tranquility&language=en")
				.times(1)
				.respond(420)
				.header(EsiLimitExceededInterceptor.RESET_TIME_HEADER, "5");
		http.addRule()
				.get("https://esi.evetech.net/latest/420-code?datasource=tranquility&language=en")
				.anyTimes()
				.respond(204)
				.header(EsiLimitExceededInterceptor.RESET_TIME_HEADER, "5");
		http.addRule()
				.get("https://esi.evetech.net/latest/420-text?datasource=tranquility&language=en")
				.times(1)
				.respond(204)
				.header(EsiLimitExceededInterceptor.RESET_TIME_HEADER, "5")
				.body(ResponseBody.create(EsiLimitExceededInterceptor.ESI_420_TEXT, MediaTypes.MEDIATYPE_TEXT));
		http.addRule()
				.get("https://esi.evetech.net/latest/420-text?datasource=tranquility&language=en")
				.anyTimes()
				.respond(204)
				.header(EsiLimitExceededInterceptor.RESET_TIME_HEADER, "5");
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
					var response = esiHelper
							.fetch(EsiUrl.builder().urlPath("/page").build())
							.blockingGet();
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
		log.info("Count (2): " + count.get());
		new Thread(() -> {
					esiHelper
							.fetch(EsiUrl.builder().urlPath("/420-" + type).build())
							.blockingGet();
				})
				.start();
		log.info("Count (3): " + count.get());
		Thread.sleep(1500);
		count.set(0);
		log.info("Count (4): " + count.get());
		Thread.sleep(2500);
		log.info("Count (5): " + count.get());
		assertEquals(0, count.get());
		// Ensure requests are running again.
		log.info("Resuming");
		Thread.sleep(5000);
		log.info("Count (6): " + count.get());
		assertNotEquals(0, count.get());

		threads.forEach(Thread::stop);
	}
}
