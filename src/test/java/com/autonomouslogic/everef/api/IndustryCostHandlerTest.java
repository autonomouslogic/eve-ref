package com.autonomouslogic.everef.api;

import static com.autonomouslogic.everef.model.api.SystemSecurity.NULL_SEC;
import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.cli.publishrefdata.PublishRefDataTest;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.PriceSource;
import com.autonomouslogic.everef.model.api.SystemSecurity;
import com.autonomouslogic.everef.model.fuzzwork.FuzzworkAggregatedMarketSegment;
import com.autonomouslogic.everef.model.fuzzwork.FuzzworkAggregatedMarketType;
import com.autonomouslogic.everef.openapi.api.api.IndustryApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.openapi.api.invoker.ApiException;
import com.autonomouslogic.everef.openapi.esi.model.GetMarketsPrices200Ok;
import com.autonomouslogic.everef.service.EsiMarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.service.SystemCostIndexService;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.CaseFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Full test of all products against the API:
 * <code>
 * cat blueprints.json | jq '.[].activities | [.manufacturing, .invention][].products | select(. != null) | .[].type_id' | sort -n | xargs -I{} curl -s -o /dev/null -w "%{http_code} %{url.query}\n" "https://api.everef.net/v1/industry/cost?product_id={}"
 * </code>
 */
@ExtendWith(MockitoExtension.class)
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
@SetEnvironmentVariable(key = "REFERENCE_DATA_PATH", value = "s3://" + PublishRefDataTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
@SetEnvironmentVariable(key = "FUZZWORK_MARKET_BASE_PATH", value = "http://localhost:" + TEST_PORT + "/fuzzwork/")
@Log4j2
@Timeout(60)
public class IndustryCostHandlerTest {
	private static final double EIV_TOLERANCE_RATE = 1.0 / 1_000_000.0;
	private static final BigDecimal EIV_TOLERANCE_ABS = BigDecimal.valueOf(10);

	static final List<String> TEST_NAMES = List.of(
			"dominix",
			"sin",
			"sin-blueprint",
			"armor-energizing-charge-blueprint",
			"mjolnir-fury-cruise-missile",
			"mjolnir-fury-cruise-missile-blueprint",
			"mjolnir-fury-cruise-missile-blueprint-optimized-attainment-decryptor",
			"dominix-lowsec-sotiyo-rigs",
			"sin-blueprint-lowsec-sotiyo-rigs");

	@Inject
	ApiRunner apiRunner;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	MockScrapeBuilder mockScrapeBuilder;

	@Inject
	RefDataService refDataService;

	@Inject
	EsiMarketPriceService esiMarketPriceService;

	@Inject
	SystemCostIndexService systemCostIndexService;

	IndustryApi industryApi;
	MockWebServer server;
	File refDataFile;
	String esiMarketPrices;
	HttpClient httpClient;
	String fuzzworkPrices;

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		refDataFile = mockScrapeBuilder.createTestRefdata();

		server = new MockWebServer();
		server.setDispatcher(new TestDispatcher());
		server.start(TEST_PORT);

		apiRunner.startServer();
		industryApi = new IndustryApi(
				new ApiClient().setScheme("http").setHost("localhost").setPort(8080));

		refDataService.init();
		systemCostIndexService.init();

		httpClient = HttpClient.newHttpClient();
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		apiRunner.stop();
		refDataService.stop();
		esiMarketPriceService.stop();
		server.shutdown();
	}

	@ParameterizedTest
	@MethodSource("costTests")
	@SneakyThrows
	void shouldCalculateCosts(String name, IndustryCostInput input, IndustryCost expected, String esiMarketPrices) {
		this.esiMarketPrices = esiMarketPrices;
		esiMarketPriceService.init();

		var res = industryApi.industryCostWithHttpInfo(input);
		assertEquals(200, res.getStatusCode());
		assertEquals("application/json", res.getHeaders().get("Content-Type").getFirst());
		assertEquals(
				"public, max-age=600, immutable",
				res.getHeaders().get("Cache-Control").getFirst());
		assertEquals(
				"https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml",
				res.getHeaders().get("X-OpenAPI").getFirst());
		var actual = res.getData();

		System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual));

		if (!expected.equals(actual)) {
			assertEquals(
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected),
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual));
		}
	}

	static Stream<Arguments> costTests() {
		var mapper = new ObjectMapper()
				.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(JsonParser.Feature.ALLOW_COMMENTS)
				.registerModule(new JavaTimeModule());
		return TEST_NAMES.stream().map(name -> {
			try {
				var input = mapper.readValue(openTestFile(name, "input"), IndustryCostInput.class);
				var output = mapper.readValue(openTestFile(name, "output"), IndustryCost.class).toBuilder()
						.input(input)
						.build();
				var esiMarketPrices = IOUtils.toString(openTestFile(name, "esi-market-prices"), StandardCharsets.UTF_8);
				return Arguments.of(name, input, output, esiMarketPrices);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static InputStream openTestFile(String name, String type) throws FileNotFoundException {
		return ResourceUtil.loadContextual(IndustryCostHandlerTest.class, "/" + name + "/" + type + ".json");
	}

	@Test
	@SneakyThrows
	void shouldNotFailOnBlankParameters() {
		setupBasicPrices();

		var query = new ArrayList<String>();
		for (var field : IndustryCostInput.class.getDeclaredFields()) {
			var prop = field.getAnnotation(JsonProperty.class);
			if (prop != null) {
				var name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
				if (name.equals("product_id")) {
					continue;
				}
				query.add(name + "=");
			}
		}

		var uri = URI.create("http://localhost:8080/v1/industry/cost?product_id=645&" + String.join("&", query));
		log.info("URI: {}", uri);
		var req = HttpRequest.newBuilder().GET().uri(uri).build();
		var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
		log.info("Body: {}", res.body());
		assertEquals(200, res.statusCode());
		var output = objectMapper.readValue(res.body(), IndustryCost.class);
		assertNull(output.getInput().getStructureTypeId());
		assertNull(output.getInput().getRigId());
	}

	@Test
	@SneakyThrows
	void shouldDefaultEfficienciesForT1Products() {
		setupBasicPrices();
		var input = IndustryCostInput.builder().productId(645).build();
		assertNull(input.getMe());
		assertNull(input.getTe());
		var cost = industryApi.industryCost(input);
		var manufacturing = cost.getManufacturing().get("645");
		assertEquals(10, manufacturing.getMe());
		assertEquals(20, manufacturing.getTe());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "34202"})
	@SneakyThrows
	void shouldUseInventionEfficienciesForT2Products(String decryptorId) {
		setupBasicPrices();
		var builder = IndustryCostInput.builder().productId(22430);
		if (!decryptorId.isEmpty()) {
			builder.decryptorId(Long.valueOf(decryptorId));
		}
		var input = builder.build();
		assertNull(input.getMe());
		assertNull(input.getTe());
		var cost = industryApi.industryCost(input);

		var invention = cost.getInvention().get("22431");
		if (decryptorId.isEmpty()) {
			assertEquals(2, invention.getMe());
			assertEquals(4, invention.getTe());
		} else {
			assertEquals(1, invention.getMe());
			assertEquals(8, invention.getTe());
		}

		var manufacturing = cost.getManufacturing().get("22430");
		assertEquals(invention.getMe(), manufacturing.getMe());
		assertEquals(invention.getTe(), manufacturing.getTe());
	}

	@Test
	@SneakyThrows
	void shouldUseSuppliedEfficienciesForT2Products() {
		setupBasicPrices();
		var input = IndustryCostInput.builder().productId(22430).me(0).te(0).build();
		var cost = industryApi.industryCost(input);

		var invention = cost.getInvention().get("22431");
		assertEquals(2, invention.getMe());
		assertEquals(4, invention.getTe());

		var manufacturing = cost.getManufacturing().get("22430");
		assertEquals(0, manufacturing.getMe());
		assertEquals(0, manufacturing.getTe());
	}

	@ParameterizedTest
	@MethodSource("fuzzworkTests")
	@SneakyThrows
	void shouldLookupPricesViaFuzzwork(PriceSource source, int expected) {
		setupBasicPrices();
		var input = IndustryCostInput.builder()
				.productId(645)
				.materialPrices(source)
				.build();
		var cost = industryApi.industryCost(input);
		var price = cost.getManufacturing().get("645").getMaterials().get("34").getCostPerUnit();
		assertEquals(BigDecimal.valueOf(expected), price);
	}

	public static Stream<Arguments> fuzzworkTests() {
		return Stream.of(
				Arguments.of(PriceSource.FUZZWORK_JITA_SELL_AVG, 10),
				Arguments.of(PriceSource.FUZZWORK_JITA_SELL_MIN, 11),
				Arguments.of(PriceSource.FUZZWORK_JITA_BUY_AVG, 20),
				Arguments.of(PriceSource.FUZZWORK_JITA_BUY_MAX, 21));
	}

	@Test
	@SneakyThrows
	void shouldResolveSystemSecurityAndCost() {
		setupBasicPrices();
		var input =
				IndustryCostInput.builder().productId(645).systemId(30004839).build();
		var cost = industryApi.industryCost(input);

		assertEquals(30004839, cost.getInput().getSystemId());
		assertEquals(NULL_SEC, cost.getInput().getSecurity());
		assertEquals(BigDecimal.valueOf(0.0131), cost.getInput().getManufacturingCost());
		assertEquals(BigDecimal.valueOf(0.0142), cost.getInput().getResearchingTeCost());
		assertEquals(BigDecimal.valueOf(0.0044), cost.getInput().getResearchingMeCost());
		assertEquals(BigDecimal.valueOf(0.0055), cost.getInput().getCopyingCost());
		assertEquals(BigDecimal.valueOf(0.0324), cost.getInput().getInventionCost());
		assertEquals(BigDecimal.valueOf(0.0014), cost.getInput().getReactionCost());

		var product = cost.getManufacturing().get("645");
		assertNotEquals(BigDecimal.ZERO, product.getSystemCostIndex());
	}

	@Test
	@SneakyThrows
	void shouldDefaultToHighSecIfNeitherSecurityNorSystemAreSupplied() {
		setupBasicPrices();
		var input = IndustryCostInput.builder().productId(645).build();
		var cost = industryApi.industryCost(input);
		assertNull(cost.getInput().getSystemId());
		assertEquals(SystemSecurity.HIGH_SEC, cost.getInput().getSecurity());
	}

	@Test
	@SneakyThrows
	void shouldComplainAboutUnknownSystems() {
		setupBasicPrices();
		var input = IndustryCostInput.builder().productId(645).systemId(1).build();
		var e = assertThrows(ApiException.class, () -> industryApi.industryCost(input));
		assertEquals(400, e.getCode());
		assertTrue(e.getResponseBody().contains("System ID 1 not found"), e.getResponseBody());
	}

	// ===========

	@SneakyThrows
	void setupBasicPrices() {
		var esiPrices = new ArrayList<>();
		var fuzzworkPrices = new HashMap<String, FuzzworkAggregatedMarketType>();
		for (int i = 0; i < 100_000; i++) {
			esiPrices.add(
					new GetMarketsPrices200Ok().typeId(i).averagePrice(1.0).adjustedPrice(1.0));
			fuzzworkPrices.put(
					String.valueOf(i),
					FuzzworkAggregatedMarketType.builder()
							.sell(FuzzworkAggregatedMarketSegment.builder()
									.weightedAverage(BigDecimal.valueOf(10))
									.min(BigDecimal.valueOf(11))
									.build())
							.buy(FuzzworkAggregatedMarketSegment.builder()
									.weightedAverage(BigDecimal.valueOf(20))
									.max(BigDecimal.valueOf(21))
									.build())
							.build());
		}

		esiMarketPrices = objectMapper.writeValueAsString(esiPrices);
		esiMarketPriceService.init();

		this.fuzzworkPrices = objectMapper.writeValueAsString(fuzzworkPrices);
	}

	class TestDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
			try {
				var path = request.getRequestUrl().encodedPath();
				log.info("Path: {}", path);
				switch (path) {
					case "/reference-data/reference-data-latest.tar.xz":
						return new MockResponse()
								.setResponseCode(200)
								.setBody(new Buffer().write(IOUtils.toByteArray(new FileInputStream(refDataFile))));
					case "/markets/prices/":
						return new MockResponse()
								.setResponseCode(200)
								.setHeader("ETag", "test")
								.setBody(esiMarketPrices);
					case "/universe/systems/30004839/":
						return new MockResponse()
								.setResponseCode(200)
								.setBody("{\"security_status\": -0.22037343680858612}");
					case "/industry/systems/":
						return new MockResponse()
								.setResponseCode(200)
								.setHeader("ETag", "test")
								.setBody(
										"""
									[{
										"cost_indices": [
										{
											"activity": "manufacturing",
											"cost_index": 0.0131
										},
										{
											"activity": "researching_time_efficiency",
											"cost_index": 0.0142
										},
										{
											"activity": "researching_material_efficiency",
											"cost_index": 0.0044
										},
										{
											"activity": "copying",
											"cost_index": 0.0055
										},
										{
											"activity": "invention",
											"cost_index": 0.0324
										},
										{
											"activity": "reaction",
											"cost_index": 0.0014
										}
										],
										"solar_system_id": 30004839
									}]""");
				}
				if (path.startsWith("/fuzzwork/aggregates/")) {
					return new MockResponse().setResponseCode(200).setBody(fuzzworkPrices);
				}
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
