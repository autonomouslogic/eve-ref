package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EsiHttpWrapperTest {
	@Inject
	@Named("esi")
	OkHttpWrapper wrapper;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldSetUserAgent() {
		server.enqueue(new MockResponse().setResponseCode(204));
		wrapper.get(String.format("http://localhost:%s/test", server.getPort()));
		var request = server.takeRequest(0, TimeUnit.SECONDS);
		assertEquals("everef.net/dev (+https://github.com/autonomouslogic/eve-ref/)", request.getHeader("User-Agent"));
	}
}
