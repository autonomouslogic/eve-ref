package com.autonomouslogic.everef.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.autonomouslogic.everef.cli.api.ApiRunner;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.openapi.api.api.IndustryApi;
import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IndustryCostHandlerTest {
	@Inject
	ApiRunner apiRunner;

	IndustryApi industryApi;

	@BeforeEach
	public void setup() {
		DaggerTestComponent.builder().build().inject(this);
		apiRunner.start();
		industryApi = new IndustryApi(
				new ApiClient().setScheme("http").setHost("localhost").setPort(8080));
	}

	@AfterEach
	public void teardown() {
		apiRunner.stop();
	}

	@Test
	@SneakyThrows
	public void shouldCalculateCosts() {
		var res =
				industryApi.industryCostWithHttpInfo(IndustryCostInput.builder().build());
		assertEquals(200, res.getStatusCode());
		var cost = res.getData();
		assertNotNull(cost);
	}
}
