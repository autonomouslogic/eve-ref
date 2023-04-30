package com.autonomouslogic.everef.cli.refdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransformUtilTest {
	@Inject
	ObjectMapper objectMapper;

	@Inject
	TransformUtil transformUtil;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldOrderKeys() {
		assertEquals(
				objectMapper
						.createObjectNode()
						.put("a", 1)
						.put("b", 1)
						.put("c", 1)
						.toPrettyString(),
				transformUtil
						.orderKeys(objectMapper
								.createObjectNode()
								.put("b", 1)
								.put("c", 1)
								.put("a", 1))
						.toPrettyString());
	}
}
