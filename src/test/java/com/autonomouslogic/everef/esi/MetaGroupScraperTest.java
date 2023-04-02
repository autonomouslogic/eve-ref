package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
public class MetaGroupScraperTest {
	private static final int PORT = 30150;

	@Inject
	MetaGroupScraper metaGroupScraper;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.start(PORT);
		var html = IOUtils.toString(
				ResourceUtil.loadContextual(getClass(), "/meta-groups-15.html"), StandardCharsets.UTF_8);
		server.enqueue(new MockResponse().setResponseCode(200).setBody(html));
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SetEnvironmentVariable(key = "EVE_REF_BASE_PATH", value = "http://localhost:" + PORT)
	@SneakyThrows
	void shouldScrapeTypeIdsFromMetaGroups() {
		var ids = metaGroupScraper.scrapeTypeIds(15).toList().blockingGet();

		var request = server.takeRequest();
		assertEquals("/meta-groups/15", request.getPath());
		assertEquals("GET", request.getMethod());

		assertNotNull(ids);
		assertNotEquals(0, ids.size());
		assertTrue(ids.contains(47840));
		assertTrue(ids.contains(47846));
		assertTrue(ids.contains(48439));
		assertTrue(ids.contains(56305));
		assertTrue(ids.contains(56309));
		assertEquals(61, ids.size());
	}
}
