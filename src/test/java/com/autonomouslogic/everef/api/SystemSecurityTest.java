package com.autonomouslogic.everef.api;

import static com.autonomouslogic.everef.model.api.SystemSecurity.HIGH_SEC;
import static com.autonomouslogic.everef.model.api.SystemSecurity.LOW_SEC;
import static com.autonomouslogic.everef.model.api.SystemSecurity.NULL_SEC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.model.api.SystemSecurity;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SystemSecurityTest {

	@ParameterizedTest
	@MethodSource("securityTests")
	@SneakyThrows
	void shouldResolveSystem(double rating, SystemSecurity security) {
		assertEquals(security, SystemSecurity.forStatus(rating));
	}

	public static Stream<Arguments> securityTests() {
		return Stream.of(
				Arguments.of(1.0, HIGH_SEC),
				Arguments.of(0.45, HIGH_SEC),
				Arguments.of(0.4996122717857361, HIGH_SEC),
				Arguments.of(0.4501924216747284, HIGH_SEC),
				Arguments.of(0.44994357228279114, LOW_SEC),
				Arguments.of(0.449, LOW_SEC),
				Arguments.of(0.25, LOW_SEC),
				Arguments.of(0.029147488996386528, LOW_SEC),
				Arguments.of(-0.0000013443460602502455, NULL_SEC),
				Arguments.of(-0.1, NULL_SEC),
				Arguments.of(-1.0, NULL_SEC));
	}
}
