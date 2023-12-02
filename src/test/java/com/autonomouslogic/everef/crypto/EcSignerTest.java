package com.autonomouslogic.everef.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

@Log4j2
public class EcSignerTest {
	String privateKey = "JXUry/m4e1eAzCHu/7C4ui1SvyloG/Q2fg2zaCCA9ro=";
	String publicKey = "c8kizuQIYny/H1Sc/HaDrwBFFjRb25odhHDJg1qSLpg=";

	@ParameterizedTest
	@MethodSource("signAndVerifyMessages")
	void shouldSignAndVerify(String message) {
		var signer = new EcSigner();
		var signature = signer.sign(privateKey, message);
		assertEquals(512 / 8, Base64.decodeBase64(signature).length);
		assertTrue(signer.verify(publicKey, message, signature));
	}

	static Stream<Arguments> signAndVerifyMessages() {
		return Stream.of(
				Arguments.of("Hello, world!"),
				Arguments.of(""),
				Arguments.of("The quick brown fox jumps over the lazy dog"),
				Arguments.of(
						"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam volutpat augue at erat consequat,\n"
								+ "et aliquet urna malesuada. Nam tempus tempor aliquam. Aliquam condimentum, risus sit amet\n"
								+ "venenatis finibus, quam tortor iaculis mi, quis venenatis justo risus quis nisi. Suspendisse id\n"
								+ "odio ipsum. Integer placerat lectus at tortor rutrum, non fermentum ipsum posuere. Integer\n"
								+ "sagittis dolor ut ligula rutrum blandit. Phasellus euismod dolor quam, non elementum erat lacinia\n"
								+ "nec. Sed sit amet aliquam diam. Integer nec augue sit amet nulla dapibus luctus. Curabitur eu nisi\n"
								+ "et mi ultrices egestas. Duis sollicitudin facilisis lacus ut finibus. Sed cursus tortor sed ipsum\n"
								+ "tempor, vel aliquet quam hendrerit. Vivamus arcu eros, maximus nec dolor in, rutrum convallis\n"
								+ "purus. Aliquam enim odio, vestibulum sit amet porta pharetra, luctus eget erat. Suspendisse\n"
								+ "potenti. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis\n"
								+ "egestas. Quisque dignissim urna vitae risus facilisis dictum. Nulla pharetra. "));
	}

	@Test
	void shouldSignAndVerifyHugeMessage() {
		var chars = new char[1 << 20];
		Arrays.fill(chars, 'a');
		var message = new String(chars);

		var signer = new EcSigner();
		var signature = signer.sign(privateKey, message);
		assertEquals(512 / 8, Base64.decodeBase64(signature).length);
		assertTrue(signer.verify(publicKey, message, signature));
	}

	@ParameterizedTest
	@CsvFileSource(
			resources = "/com/autonomouslogic/everef/crypto/rfc8032-test-vectors.csv",
			useHeadersInDisplayName = true)
	@SneakyThrows
	void shouldSignRfc8032TestVectors(
			String name, String secretKey, String publicKey, String message, String context, String signature) {
		var signer = new EcSigner(Hex.decodeHex(context));
		assertEquals(signature, Hex.encodeHexString(signer.sign(Hex.decodeHex(secretKey), Hex.decodeHex(message))));
	}

	@ParameterizedTest
	@CsvFileSource(
			resources = "/com/autonomouslogic/everef/crypto/rfc8032-test-vectors.csv",
			useHeadersInDisplayName = true)
	@SneakyThrows
	void shouldVerifyRfc8032TestVectors(
			String name, String secretKey, String publicKey, String message, String context, String signature) {
		var signer = new EcSigner(Hex.decodeHex(context));
		assertTrue(signer.verify(Hex.decodeHex(publicKey), Hex.decodeHex(message), Hex.decodeHex(signature)));
	}
}
