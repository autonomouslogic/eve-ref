package com.autonomouslogic.everef.api;

import static com.autonomouslogic.everef.test.TestDataUtil.TEST_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.cli.publishrefdata.PublishRefDataTest;
import com.autonomouslogic.everef.openapi.api.model.IndustryCost;
import com.autonomouslogic.everef.openapi.api.model.IndustryCostInput;
import com.autonomouslogic.everef.openapi.api.model.InventionCost;
import com.autonomouslogic.everef.openapi.api.model.ManufacturingCost;
import com.autonomouslogic.everef.openapi.api.api.IndustryApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.service.MarketPriceService;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.autonomouslogic.everef.util.MockScrapeBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SetEnvironmentVariable(key = "DATA_BASE_URL", value = "http://localhost:" + TEST_PORT)
@SetEnvironmentVariable(key = "REFERENCE_DATA_PATH", value = "s3://" + PublishRefDataTest.BUCKET_NAME + "/base/")
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "user-agent")
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + TestDataUtil.TEST_PORT)
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
	MarketPriceService marketPriceService;

	IndustryApi industryApi;
	MockWebServer server;
	File refDataFile;
	String esiMarketPrices;

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
	}

	@AfterEach
	@SneakyThrows
	void teardown() {
		apiRunner.stop();
		refDataService.stop();
		marketPriceService.stop();
		server.shutdown();
	}

	@ParameterizedTest
	@MethodSource("costTests")
	@SneakyThrows
	void shouldCalculateCosts(String name, IndustryCostInput input, IndustryCost expected, String esiMarketPrices) {
		this.esiMarketPrices = esiMarketPrices;
		marketPriceService.init();

		var res = industryApi.industryCostWithHttpInfo(input);
		assertEquals(200, res.getStatusCode());
		assertEquals("application/json", res.getHeaders().get("Content-Type").getFirst());
		assertEquals(
				"public, max-age=600, immutable",
				res.getHeaders().get("Cache-Control").getFirst());
		assertEquals(
				"https://github.com/autonomouslogic/eve-ref/blob/industry-api/spec/eve-ref-api.yaml",
				res.getHeaders().get("X-OpenAPI").getFirst());
		var actual = res.getData();

		System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual));

//		actual = assertEiv(expected, actual);
		assertEquals(expected, actual);
	}

//	/*
//	There are some slight differences in the EIV calculations.
//	This method asserts those values within some accepted tolerance.
//	*/
//	private IndustryCost assertEiv(IndustryCost expected, IndustryCost actual) {
//		var builder = actual.toBuilder();
//		if (actual.getInvention() != null) {
//			for (var product : actual.getInvention().keySet()) {
//				var expectedActivity =
//						Optional.ofNullable(expected.getInvention()).flatMap(m -> Optional.ofNullable(m.get(product)));
//				if (!expectedActivity.isEmpty()) {
//					builder.invention(product, (InventionCost) assertEiv(
//							"invention",
//							product,
//							expectedActivity.get(),
//							actual.getInvention().get(product)));
//				}
//			}
//		}
//		if (actual.getManufacturing() != null) {
//			for (var product : actual.getManufacturing().keySet()) {
//				var expectedActivity = Optional.ofNullable(expected.getManufacturing())
//						.flatMap(m -> Optional.ofNullable(m.get(product)));
//				if (!expectedActivity.isEmpty()) {
//					builder.manufacturing(product, (ManufacturingCost) assertEiv(
//							"manufacturing",
//							product,
//							expectedActivity.get(),
//							actual.getManufacturing().get(product)));
//				}
//			}
//		}
//		return builder.build();
//	}
//
//	private ActivityCost assertEiv(String type, String product, ActivityCost expected, ActivityCost actual) {
//		if (expected.getEstimatedItemValue() == null || actual.getEstimatedItemValue() == null) {
//			return actual;
//		}
//		var expectedEiv = expected.getEstimatedItemValue();
//		var actualEiv = actual.getEstimatedItemValue();
//		var margin =
//				expectedEiv.multiply(BigDecimal.valueOf(EIV_TOLERANCE_RATE)).max(EIV_TOLERANCE_ABS);
//		var diff = actualEiv.subtract(expectedEiv);
//		var msg = String.format("%s product %s, diff: %s, margin: %s", type, product, diff, margin);
//		if (diff.abs().compareTo(margin) > 0) {
//			assertEquals(expectedEiv, actualEiv, msg);
//		}
//		return actual.toBuilder().estimatedItemValue(expectedEiv).build();
//	}

	static Stream<Arguments> costTests() {
		var mapper = new ObjectMapper()
				.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(JsonParser.Feature.ALLOW_COMMENTS)
				.registerModule(new JavaTimeModule());
		return TEST_NAMES.stream().map(name -> {
			try {
				var input = mapper.readValue(openTestFile(name, "input"), IndustryCostInput.class);
				var output = mapper.readValue(openTestFile(name, "output"), IndustryCost.class)
//					.toBuilder()
//						.input(input)
//						.build();
					.input(input);
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
				}
				return new MockResponse().setResponseCode(404);
			} catch (Exception e) {
				log.error("Error in dispatcher", e);
				return new MockResponse().setResponseCode(500);
			}
		}
	}
}
