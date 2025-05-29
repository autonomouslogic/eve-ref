package com.autonomouslogic.everef.service;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "FUZZWORK_MARKET_API_BASE_PATH", value = "http://localhost:" + TEST_PORT + "/")
public class FuzzworkMarketServiceTest {
	@Inject
	FuzzworkMarketService marketService;

	MockWebServer server;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		server = new MockWebServer();
		//		server.setDispatcher(new TestDispatcher());
		server.start(TEST_PORT);
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		server.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldGetPrices() {
		enqueueSuccess(34, new BigDecimal("6.6"));

		var response = marketService.fetchAggregateStationPrices(1, List.of(34L, 35L, 36L));
		assertEquals(new BigDecimal("6.6"), response.get("34").getSell().getWeightedAverage());

		var req = server.takeRequest(1, TimeUnit.MILLISECONDS);
		assertEquals(
				"http://localhost:30150/aggregates/?station=1&types=34,35,36",
				req.getRequestUrl().url().toString());
	}

	@Test
	@SneakyThrows
	void shouldCachePrices() {
		enqueueSuccess(34, new BigDecimal("6.6"));
		enqueueSuccess(34, new BigDecimal("6.6"));

		var response1 = marketService.fetchAggregateStationPrices(1, List.of(34L));
		assertEquals(new BigDecimal("6.6"), response1.get("34").getSell().getWeightedAverage());

		var response2 = marketService.fetchAggregateStationPrices(1, List.of(34L));
		assertEquals(new BigDecimal("6.6"), response2.get("34").getSell().getWeightedAverage());

		var req = server.takeRequest(1, TimeUnit.MILLISECONDS);
		assertEquals(
				"http://localhost:30150/aggregates/?station=1&types=34",
				req.getRequestUrl().url().toString());

		assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
	}

	@Test
	@SneakyThrows
	void shouldPartiallyReconstructResponses() {
		enqueueSuccess(34, new BigDecimal("6.6"));
		enqueueSuccess(35, new BigDecimal("7.7"));
		enqueueSuccess(36, new BigDecimal("8.8"));

		var response1 = marketService.fetchAggregateStationPrices(1, List.of(34L));
		assertEquals(new BigDecimal("6.6"), response1.get("34").getSell().getWeightedAverage());

		var response2 = marketService.fetchAggregateStationPrices(1, List.of(35L));
		assertEquals(new BigDecimal("7.7"), response2.get("35").getSell().getWeightedAverage());

		var response3 = marketService.fetchAggregateStationPrices(1, List.of(34L, 35L, 36L));
		assertEquals(new BigDecimal("6.6"), response3.get("34").getSell().getWeightedAverage());
		assertEquals(new BigDecimal("7.7"), response3.get("35").getSell().getWeightedAverage());
		assertEquals(new BigDecimal("8.8"), response3.get("36").getSell().getWeightedAverage());

		assertEquals(
				"http://localhost:30150/aggregates/?station=1&types=34",
				server.takeRequest(1, TimeUnit.MILLISECONDS)
						.getRequestUrl()
						.url()
						.toString());

		assertEquals(
				"http://localhost:30150/aggregates/?station=1&types=35",
				server.takeRequest(1, TimeUnit.MILLISECONDS)
						.getRequestUrl()
						.url()
						.toString());

		assertEquals(
				"http://localhost:30150/aggregates/?station=1&types=36",
				server.takeRequest(1, TimeUnit.MILLISECONDS)
						.getRequestUrl()
						.url()
						.toString());

		assertNull(server.takeRequest(1, TimeUnit.MILLISECONDS));
	}

	private void enqueueSuccess(long typeId, BigDecimal sellAverage) {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody(String.format(
						"""
			{
			"%s": {
				"buy": {
				"weightedAverage": "4.02878502065",
				"max": "5.95",
				"min": "0.01",
				"stddev": "1.62036217159",
				"median": "5.0",
				"volume": "10024734026.0",
				"orderCount": "52",
				"percentile": "5.50168617928"
			},
			"sell": {
				"weightedAverage": "%s",
				"max": "2201571.0",
				"min": "5.01",
				"stddev": "177420.733866",
				"median": "6.38",
				"volume": "25573930856.0",
				"orderCount": "179",
				"percentile": "5.92257900667"
				}
			}
			}""",
						typeId, sellAverage)));
	}
}
