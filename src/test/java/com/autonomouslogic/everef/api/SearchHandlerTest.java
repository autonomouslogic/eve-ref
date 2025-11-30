package com.autonomouslogic.everef.api;

import static com.autonomouslogic.everef.api.SearchHandlerTest.API_TEST_PORT;
import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.cli.publishrefdata.PublishRefDataTest;
import com.autonomouslogic.everef.openapi.api.api.SearchApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import java.io.File;
import java.io.FileInputStream;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
@SetEnvironmentVariable(key = "REFERENCE_DATA_PATH", value = "s3://" + PublishRefDataTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "HTTP_PORT", value = "" + API_TEST_PORT)
@Log4j2
@Timeout(60)
public class SearchHandlerTest {
	public static final int API_TEST_PORT = 29217;

	@Inject
	ApiRunner apiRunner;

	@Inject
	MockScrapeBuilder mockScrapeBuilder;

	@Inject
	RefDataService refDataService;

	SearchApi searchApi;
	MockWebServer server;
	File refDataFile;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		refDataFile = mockScrapeBuilder.createTestRefdata();

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TEST_PORT);

		apiRunner.startServer();
		searchApi = new SearchApi(
				new ApiClient().setScheme("http").setHost("localhost").setPort(API_TEST_PORT));

		refDataService.init();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		apiRunner.stop();
		refDataService.stop();
		server.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldSearchByExactName() {
		var res = searchApi.searchWithHttpInfo("Tritanium");
		assertEquals(200, res.getStatusCode());
		assertEquals("application/json", res.getHeaders().get("Content-Type").getFirst());
		assertEquals(
				"public, max-age=600, immutable",
				res.getHeaders().get("Cache-Control").getFirst());
		assertEquals(
				"https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml",
				res.getHeaders().get("X-OpenAPI").getFirst());

		var result = res.getData();
		assertNotNull(result);
		assertEquals("Tritanium", result.getInput());
		assertNotNull(result.getInventoryType());
		assertEquals(1, result.getInventoryType().size());
		assertEquals("Tritanium", result.getInventoryType().get(0).getNameEn());
		assertEquals(34L, result.getInventoryType().get(0).getTypeId());
	}

	@Test
	@SneakyThrows
	void shouldSearchByPartialName() {
		var result = searchApi.search("Trit");
		assertNotNull(result);
		assertEquals("Trit", result.getInput());
		assertNotNull(result.getInventoryType());
		assertTrue(result.getInventoryType().size() > 0);
		assertTrue(result.getInventoryType().stream().anyMatch(t -> "Tritanium".equals(t.getNameEn())));
	}

	@Test
	@SneakyThrows
	void shouldSearchCaseInsensitive() {
		var result = searchApi.search("tritanium");
		assertNotNull(result);
		assertEquals("tritanium", result.getInput());
		assertNotNull(result.getInventoryType());
		assertEquals(1, result.getInventoryType().size());
		assertEquals("Tritanium", result.getInventoryType().get(0).getNameEn());
	}

	@Test
	@SneakyThrows
	void shouldReturnEmptyForShortQuery() {
		var result = searchApi.search("Tr");
		assertNotNull(result);
		assertEquals("Tr", result.getInput());
		assertNotNull(result.getInventoryType());
		assertEquals(0, result.getInventoryType().size());
	}

	@Test
	@SneakyThrows
	void shouldReturnEmptyForNull() {
		var result = searchApi.search(null);
		assertNotNull(result);
		assertEquals("", result.getInput());
		assertNotNull(result.getInventoryType());
		assertEquals(0, result.getInventoryType().size());
	}

	@Test
	@SneakyThrows
	void shouldSearchMultipleWords() {
		var result = searchApi.search("Mjolnir Fury");
		assertNotNull(result);
		assertEquals("Mjolnir Fury", result.getInput());
		assertNotNull(result.getInventoryType());
		assertTrue(result.getInventoryType().size() > 0);
		assertTrue(result.getInventoryType().stream()
				.anyMatch(t -> t.getNameEn() != null
						&& t.getNameEn().contains("Mjolnir")
						&& t.getNameEn().contains("Fury")));
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		@SneakyThrows
		public MockResponse dispatch(@NotNull RecordedRequest request) {
			var path = request.getPath();
			log.info("Path: {}", path);
			if (path.startsWith("/reference-data/reference-data-latest.tar.xz")) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody(new Buffer().readFrom(new FileInputStream(refDataFile)));
			}
			return new MockResponse().setResponseCode(404);
		}
	}
}
