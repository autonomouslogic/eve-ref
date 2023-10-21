package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
public class EsiHelperTest {
	@Inject
	TestDataUtil testDataUtil;

	@Inject
	EsiHelper esiHelper;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockDataIndexModule(new MockDataIndexModule().setDefaultMock(true))
				.build()
				.inject(this);
		server = new MockWebServer();
		server.start(TestDataUtil.TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFetchSinglePage() {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("page-1").addHeader("X-Pages", "1"));
		var responses = esiHelper
				.fetchPages(EsiUrl.builder().urlPath("/pages").build())
				.toList()
				.blockingGet();
		var bodies = getBodies(responses);
		assertEquals(List.of("page-1"), bodies);
		testDataUtil.assertRequest(server.takeRequest(), "/pages?datasource=tranquility&language=en");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	void shouldFetchSinglePageWithoutHeader() {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("page-1"));
		var responses = esiHelper
				.fetchPages(EsiUrl.builder().urlPath("/pages").build())
				.toList()
				.blockingGet();
		var bodies = getBodies(responses);
		assertEquals(List.of("page-1"), bodies);
		testDataUtil.assertRequest(server.takeRequest(), "/pages?datasource=tranquility&language=en");
		testDataUtil.assertNoMoreRequests(server);
	}

	@Test
	@SneakyThrows
	void shouldFetchMultiplePages() {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("page-1").addHeader("X-Pages", "3"));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("page-2").addHeader("X-Pages", "3"));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("page-3").addHeader("X-Pages", "3"));
		var responses = esiHelper
				.fetchPages(EsiUrl.builder().urlPath("/pages").build())
				.toList()
				.blockingGet();
		var bodies = getBodies(responses);
		assertEquals(Set.of("page-1", "page-2", "page-3"), new HashSet<>(bodies));
		var paths = new HashSet<String>();
		for (int i = 0; i < 3; i++) {
			paths.add(server.takeRequest().getPath());
		}
		assertEquals(
				Set.of(
						"/pages?datasource=tranquility&language=en",
						"/pages?datasource=tranquility&language=en&page=2",
						"/pages?datasource=tranquility&language=en&page=3"),
				paths);
		testDataUtil.assertNoMoreRequests(server);
	}

	@ParameterizedTest
	@ValueSource(ints = {204, 404, 503, 504})
	@SneakyThrows
	void shouldHandleNoResponseStatusCodes(int code) {
		server.enqueue(new MockResponse().setResponseCode(code));
		var url = EsiUrl.builder().urlPath("/codes").build();
		var responses = esiHelper
				.fetch(url)
				.toFlowable()
				.compose(esiHelper.standardErrorHandling(url))
				.toList()
				.blockingGet();
		assertEquals(List.of(), responses);
		testDataUtil.assertRequest(server.takeRequest(), "/codes?datasource=tranquility&language=en");
		testDataUtil.assertNoMoreRequests(server);
	}

	@ParameterizedTest
	@ValueSource(ints = {401, 403})
	@SneakyThrows
	void shouldErrorOnAuth(int code) {
		server.enqueue(new MockResponse().setResponseCode(code));
		var url = EsiUrl.builder().urlPath("/codes").build();
		var err = assertThrows(RuntimeException.class, () -> {
			esiHelper
					.fetch(url)
					.toFlowable()
					.compose(esiHelper.standardErrorHandling(url))
					.toList()
					.blockingGet();
		});
		assertEquals(
				String.format("Received %s for http://localhost:30150/codes?datasource=tranquility&language=en", code),
				err.getMessage());
		testDataUtil.assertRequest(server.takeRequest(), "/codes?datasource=tranquility&language=en");
		testDataUtil.assertNoMoreRequests(server);
	}

	@NotNull
	private static List<String> getBodies(List<Response> responses) {
		var bodies = responses.stream()
				.map(r -> {
					try {
						return r.body().string();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
		return bodies;
	}
}
