package com.autonomouslogic.everef.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.openapi.api.api.IndustryApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IndustryCostHandlerTest {
	static final List<String> TEST_NAMES = List.of("basic-dominix");

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
	void shouldCalculateCosts(IndustryCostInput input, IndustryCost expected) {
		var res = industryApi.industryCostWithHttpInfo(input);
		assertEquals(200, res.getStatusCode());
		var actual = res.getData();
		assertEquals(expected, actual);
	}

	static Stream<Arguments> costTests() {
		var mapper = new ObjectMapper();
		return TEST_NAMES.stream().map(name -> {
			try {
				var input = mapper.readValue(openTestFile(name, "input"), IndustryCostInput.class);
				var output = mapper.readValue(openTestFile(name, "output"), IndustryCost.class).toBuilder()
						.input(input)
						.build();
				return Arguments.of(name, input, output);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static InputStream openTestFile(String name, String type) throws FileNotFoundException {
		return ResourceUtil.loadContextual(IndustryCostHandlerTest.class, "/" + name + "-" + type + ".json");
	}
}
