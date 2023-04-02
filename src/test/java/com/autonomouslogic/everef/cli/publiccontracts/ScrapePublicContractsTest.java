package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
@Timeout(5)
public class ScrapePublicContractsTest {
	private static final int PORT = 30150;
	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(PORT);
	}

	private class TestDispatcher extends Dispatcher {
		@Override
		public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
			switch (request.getPath()) {
				case "/meta-groups/15":
					return metaGroup15();
			}
			return new MockResponse().setResponseCode(404);
		}

		@SneakyThrows
		MockResponse metaGroup15() {
			var html = IOUtils.toString(
					ResourceUtil.loadContextual(getClass(), "/meta-groups-15.html"), StandardCharsets.UTF_8);
			return new MockResponse().setResponseCode(200).setBody(html);
		}
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldSCrapePublicContracts() {}
}
