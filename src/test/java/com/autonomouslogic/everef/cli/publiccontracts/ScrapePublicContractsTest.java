package com.autonomouslogic.everef.cli.publiccontracts;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.esi.MetaGroupScraperTest;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import javax.inject.Inject;
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
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
@Timeout(5)
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + ScrapePublicContractsTest.BUCKET_NAME + "/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_PATH", value = "http://localhost:" + ScrapePublicContractsTest.PORT)
@SetEnvironmentVariable(key = "EVE_REF_BASE_PATH", value = "http://localhost:" + ScrapePublicContractsTest.PORT)
public class ScrapePublicContractsTest {
	static final String BUCKET_NAME = "data-bucket";
	static final int PORT = 30150;

	@Inject
	ScrapePublicContracts scrapePublicContracts;

	MockWebServer server;
	ZonedDateTime scrapeTime = ZonedDateTime.parse("2020-02-03T04:05:06.89Z");

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
					ResourceUtil.loadContextual(MetaGroupScraperTest.class, "/meta-groups-15.html"),
					StandardCharsets.UTF_8);
			return new MockResponse().setResponseCode(200).setBody(html);
		}
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	void shouldScrapePublicContracts() {
		scrapePublicContracts.setScrapeTime(scrapeTime).run().blockingAwait();
	}
}
