package com.autonomouslogic.everef.api;

import static com.autonomouslogic.everef.api.SearchHandlerTest.API_TEST_PORT;
import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.cli.publishrefdata.PublishRefDataTest;
import com.autonomouslogic.everef.openapi.api.api.SearchApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.openapi.api.invoker.ApiException;
import com.autonomouslogic.everef.openapi.api.model.SearchEntry;
import com.autonomouslogic.everef.openapi.api.model.SearchEntryUrls;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	@Inject
	ObjectMapper objectMapper;

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
	void shouldSearchForInventoryTypes() {
		var result = searchApi.search("Tritanium");
		log.info("Result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		var entries = result.getEntries();
		assertEquals(1, entries.size(), entries.toString());
		assertEquals(
				new SearchEntry()
						.id(34L)
						.title("Tritanium")
						.language("en")
						.type(SearchEntry.TypeEnum.INVENTORY_TYPE)
						.typeName("Inventory type")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/types/34")
								.referenceData("https://ref-data.everef.net/types/34")),
				entries.getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchForMarketGroups() {
		var result = searchApi.search("Battleships");
		log.info("Result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		var entries = result.getEntries();
		assertTrue(result.getEntries().size() > 0);
		assertEquals(
				new SearchEntry()
						.id(1376L)
						.title("Ships > Battleships")
						.language("en")
						.type(SearchEntry.TypeEnum.MARKET_GROUP)
						.typeName("Market group")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/market-groups/1376")
								.referenceData("https://ref-data.everef.net/market_groups/1376")),
				entries.getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchForCategories() {
		var result = searchApi.search("Starbase");
		log.info("Result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		var entries = result.getEntries();
		assertEquals(1, entries.size(), entries.toString());
		assertEquals(
				new SearchEntry()
						.id(23L)
						.title("Starbase")
						.language("en")
						.type(SearchEntry.TypeEnum.CATEGORY)
						.typeName("Inventory category")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/categories/23")
								.referenceData("https://ref-data.everef.net/categories/23")),
				entries.getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchForInventoryGroups() {
		var result = searchApi.search("Battleship");
		log.info("Result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		var entries = result.getEntries();
		assertTrue(result.getEntries().size() > 0);
		assertEquals(
				new SearchEntry()
						.id(27L)
						.title("Battleship")
						.language("en")
						.type(SearchEntry.TypeEnum.GROUP)
						.typeName("Inventory group")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/groups/27")
								.referenceData("https://ref-data.everef.net/groups/27")),
				entries.getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchByPartialName() {
		var result = searchApi.search("Trit");
		log.info("Result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		assertTrue(result.getEntries().size() > 0);
		assertEquals(
				new SearchEntry()
						.id(34L)
						.title("Tritanium")
						.language("en")
						.type(SearchEntry.TypeEnum.INVENTORY_TYPE)
						.typeName("Inventory type")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/types/34")
								.referenceData("https://ref-data.everef.net/types/34")),
				result.getEntries().getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchCaseInsensitive() {
		var result = searchApi.search("tritanium");
		assertEquals(1, result.getEntries().size());
		assertEquals(
				new SearchEntry()
						.id(34L)
						.title("Tritanium")
						.language("en")
						.type(SearchEntry.TypeEnum.INVENTORY_TYPE)
						.typeName("Inventory type")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/types/34")
								.referenceData("https://ref-data.everef.net/types/34")),
				result.getEntries().getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchMultipleWords() {
		var result = searchApi.search("Mjolnir Fury");
		assertTrue(
				result.getEntries().size() > 0,
				Long.toString(result.getEntries().size()));
		assertEquals(
				new SearchEntry()
						.id(24535L)
						.title("Mjolnir Fury Cruise Missile")
						.language("en")
						.type(SearchEntry.TypeEnum.INVENTORY_TYPE)
						.typeName("Inventory type")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/types/24535")
								.referenceData("https://ref-data.everef.net/types/24535")),
				result.getEntries().getFirst());
	}

	@Test
	@SneakyThrows
	void shouldSearchForIds() {
		var result = searchApi.search("34");
		var entries = result.getEntries();
		assertEquals(1, entries.size(), entries.toString());
		assertEquals(
				new SearchEntry()
						.id(34L)
						.title("Tritanium")
						.language("en")
						.type(SearchEntry.TypeEnum.INVENTORY_TYPE)
						.typeName("Inventory type")
						.urls(new SearchEntryUrls()
								.everef("https://everef.net/types/34")
								.referenceData("https://ref-data.everef.net/types/34")),
				entries.getFirst());
	}

	@Test
	@SneakyThrows
	void shouldThrowOnShortQuery() {
		var ex = assertThrows(ApiException.class, () -> searchApi.search("Tr"));
		assertEquals(400, ex.getCode());
	}

	@Test
	@SneakyThrows
	void shouldThrowOnShortQueryWithWhitespace() {
		var ex = assertThrows(ApiException.class, () -> searchApi.search("   T      r     \t\n\r"));
		assertEquals(400, ex.getCode());
	}

	@Test
	void shouldThrowForNull() {
		var ex = assertThrows(ApiException.class, () -> searchApi.search(null));
		assertEquals(400, ex.getCode());
	}

	@Test
	void shouldThrowForEmpty() {
		var ex = assertThrows(ApiException.class, () -> searchApi.search(""));
		assertEquals(400, ex.getCode());
	}

	@Test
	void shouldThrowForWhitespace() {
		var ex = assertThrows(ApiException.class, () -> searchApi.search("   "));
		assertEquals(400, ex.getCode());
	}

	@Test
	@SneakyThrows
	void shouldSetHeaders() {
		var res = searchApi.searchWithHttpInfo("Tritanium");
		assertEquals(200, res.getStatusCode());
		assertEquals("application/json", res.getHeaders().get("Content-Type").getFirst());
		assertEquals(
				"public, max-age=600, immutable",
				res.getHeaders().get("Cache-Control").getFirst());
		assertEquals(
				"https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml",
				res.getHeaders().get("X-OpenAPI").getFirst());
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
