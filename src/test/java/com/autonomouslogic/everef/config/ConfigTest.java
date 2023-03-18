package com.autonomouslogic.everef.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ConfigTest {
	@Test
	void shouldGetStringValues() {
		var config = Config.<String>builder()
				.name("TEST_ENV_VAR_STRING")
				.type(String.class)
				.build();
		assertEquals(Optional.of("test-value"), config.get());
		assertEquals("test-value", config.getRequired());
	}

	@Test
	void shouldGetIntegerValues() {
		var config = Config.<Integer>builder()
				.name("TEST_ENV_VAR_INTEGER")
				.type(Integer.class)
				.build();
		assertEquals(Optional.of(12345), config.get());
		assertEquals(12345, config.getRequired());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void shouldGetIntegerValues(boolean val) {
		var config = Config.<Boolean>builder()
				.name("TEST_ENV_VAR_BOOL_" + (val ? "TRUE" : "FALSE"))
				.type(Boolean.class)
				.build();
		assertEquals(Optional.of(val), config.get());
		assertEquals(val, config.getRequired());
	}

	@Test
	void shouldGetUnknownValues() {
		var config =
				Config.<String>builder().name("UNKNOWN_VAR").type(String.class).build();
		assertEquals(Optional.empty(), config.get());
		var e = assertThrows(IllegalArgumentException.class, config::getRequired);
		assertEquals("No value for UNKNOWN_VAR", e.getMessage());
	}

	@Test
	void shouldGetDefaultValues() {
		var config = Config.<String>builder()
				.name("UNKNOWN_VAR")
				.type(String.class)
				.defaultValue("default-value")
				.build();
		assertEquals(Optional.of("default-value"), config.get());
		assertEquals("default-value", config.getRequired());
	}

	@Test
	void shouldGetDefaultMethods() {
		var config = Config.<String>builder()
				.name("UNKNOWN_VAR")
				.type(String.class)
				.defaultMethod(() -> Optional.of("default-value"))
				.build();
		assertEquals(Optional.of("default-value"), config.get());
		assertEquals("default-value", config.getRequired());
	}
}
