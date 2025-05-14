package com.autonomouslogic.everef.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.openapi.api.api.IndustryApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IndustryCostHandlerTest {
	static final List<Pair<String, Set<String>>> TEST_NAMES =
			List.of(Pair.of("basic-dominix", Set.of("manufacturing", "invention")));

	@Inject
	ApiRunner apiRunner;

	@Inject
	ObjectMapper objectMapper;

	IndustryApi industryApi;

	@BeforeEach
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		apiRunner.start();
		industryApi = new IndustryApi(
				new ApiClient().setScheme("http").setHost("localhost").setPort(8080));
	}

	@AfterEach
	void teardown() {
		apiRunner.stop();
	}

	@ParameterizedTest
	@MethodSource("costTests")
	@SneakyThrows
	void shouldCalculateCosts(String name, IndustryCostInput input, IndustryCost expected, Set<String> verified) {
		var res = industryApi.industryCostWithHttpInfo(input);
		assertEquals(200, res.getStatusCode());
		assertEquals(
				"public, max-age=600, immutable",
				res.getHeaders().get("Cache-Control").getFirst());
		assertEquals(
				"https://github.com/autonomouslogic/eve-ref/blob/industry-api/spec/eve-ref-api.yaml",
				res.getHeaders().get("X-OpenAPI").getFirst());
		var actual = res.getData();
		assertEquals(expected, actual);
	}

	static Stream<Arguments> costTests() {
		var mapper = new ObjectMapper()
				.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(JsonParser.Feature.ALLOW_COMMENTS)
				.registerModule(new JavaTimeModule());
		return TEST_NAMES.stream().map(pair -> {
			try {
				var input = mapper.readValue(openTestFile(pair.getLeft(), "input"), IndustryCostInput.class);
				var output = mapper.readValue(openTestFile(pair.getLeft(), "output"), IndustryCost.class).toBuilder()
						.input(input)
						.build();
				return Arguments.of(pair.getLeft(), input, output, pair.getRight());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static InputStream openTestFile(String name, String type) throws FileNotFoundException {
		return ResourceUtil.loadContextual(IndustryCostHandlerTest.class, "/" + name + "-" + type + ".json");
	}
}
