package com.autonomouslogic.everef.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.url.UrlParser;
import java.util.List;
import javax.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@Timeout(10)
public class DataCrawlerTest {
	@Inject
	TestDataUtil testDataUtil;

	@Inject
	UrlParser urlParser;

	@Inject
	DataCrawler dataCrawler;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);

		server.enqueue(testDataUtil.mockResponse(ResourceUtil.loadContextual(DataCrawlerTest.class, "/page1.html")));
		server.enqueue(testDataUtil.mockResponse(ResourceUtil.loadContextual(DataCrawlerTest.class, "/page2.html")));
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldCrawlDataSite() {
		var urls = dataCrawler.crawl().toList().blockingGet();
		assertNotNull(urls);
		assertEquals(
				List.of(
						urlParser.parse("http://localhost:" + TestDataUtil.TEST_PORT + "/test.zip"),
						urlParser.parse("http://localhost:" + TestDataUtil.TEST_PORT
								+ "/esi-scrape/eve-ref-esi-scrape-latest.tar.xz")),
				urls);

		testDataUtil.assertRequest(server.takeRequest(), "/");
		testDataUtil.assertRequest(server.takeRequest(), "/esi-scrape/");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	void shouldCrawlDataSiteWithPrefix() {
		var urls = dataCrawler.setPrefix("/test").crawl().toList().blockingGet();
		assertNotNull(urls);
		assertEquals(List.of(urlParser.parse("http://localhost:" + TestDataUtil.TEST_PORT + "/test.zip")), urls);

		testDataUtil.assertRequest(server.takeRequest(), "/");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	void shouldCrawlDataSiteWithDeepPrefix() {
		var urls = dataCrawler
				.setPrefix("/esi-scrape/eve-ref-esi-scrape-")
				.crawl()
				.toList()
				.blockingGet();
		assertNotNull(urls);
		assertEquals(
				List.of(urlParser.parse(
						"http://localhost:" + TestDataUtil.TEST_PORT + "/esi-scrape/eve-ref-esi-scrape-latest.tar.xz")),
				urls);

		testDataUtil.assertRequest(server.takeRequest(), "/");
		testDataUtil.assertRequest(server.takeRequest(), "/esi-scrape/");
		testDataUtil.assertNoMoreRequests(server);
	}
}
