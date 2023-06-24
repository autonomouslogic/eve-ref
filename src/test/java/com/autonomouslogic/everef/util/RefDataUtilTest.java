package com.autonomouslogic.everef.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RefDataUtilTest {
	@Inject
	RefDataUtil refDataUtil;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldLoadReferenceDataConfig() {
		var config = refDataUtil.loadReferenceDataConfig().stream()
				.filter(c -> c.getId().equals("types"))
				.findFirst()
				.orElseThrow();
		assertEquals("types", config.getId());
		assertEquals("InventoryType", config.getModel());
	}
}
