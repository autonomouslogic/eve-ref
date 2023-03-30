package com.autonomouslogic.everef.esi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
public class EsiHelperTest {
	@Inject
	TestDataUtil testDataUtil;

	@Inject
	EsiHelper esiHelper;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder()
				.mockDataIndexModule(new MockDataIndexModule().setDefaultMock(true))
				.build()
				.inject(this);
	}

	@Test
	@SneakyThrows
	void shouldFetchSinglePage() {
		testDataUtil
				.mockResponse("https://esi.evetech.net/latest/pages?datasource=tranquility&language=en", "page-1")
				.header("X-Pages", "1");
		var responses = esiHelper
				.fetchPages(EsiUrl.builder().urlPath("/pages").build())
				.toList()
				.blockingGet();
		var bodies = responses.stream()
				.map(r -> {
					try {
						return r.body().string();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
		assertEquals(List.of("page-1"), bodies);
	}

	@Test
	@SneakyThrows
	void shouldFetchSinglePageWithoutHeader() {
		testDataUtil.mockResponse("https://esi.evetech.net/latest/pages?datasource=tranquility&language=en", "page-1");
		var responses = esiHelper
				.fetchPages(EsiUrl.builder().urlPath("/pages").build())
				.toList()
				.blockingGet();
		var bodies = responses.stream()
				.map(r -> {
					try {
						return r.body().string();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
		assertEquals(List.of("page-1"), bodies);
	}

	@Test
	@SneakyThrows
	void shouldFetchMultiplePages() {
		testDataUtil
				.mockResponse("https://esi.evetech.net/latest/pages?datasource=tranquility&language=en", "page-1")
				.header("X-Pages", "3");
		testDataUtil
				.mockResponse(
						"https://esi.evetech.net/latest/pages?datasource=tranquility&language=en&page=2", "page-2")
				.header("X-Pages", "3");
		testDataUtil
				.mockResponse(
						"https://esi.evetech.net/latest/pages?datasource=tranquility&language=en&page=3", "page-3")
				.header("X-Pages", "3");
		var responses = esiHelper
				.fetchPages(EsiUrl.builder().urlPath("/pages").build())
				.toList()
				.blockingGet();
		var bodies = responses.stream()
				.map(r -> {
					try {
						return r.body().string();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
		assertEquals(Set.of("page-1", "page-2", "page-3"), new HashSet<>(bodies));
	}
}
