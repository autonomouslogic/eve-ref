package com.autonomouslogic.everef.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

@Log4j2
public class EcKeyGeneratorTest {
	final EcKeyGenerator gen = new EcKeyGenerator();

	@Test
	void shouldGeneratePrivateKey() {
		var privateKey = gen.generatePrivateKey();
		assertEquals(256 / 8, Base64.decodeBase64(privateKey).length);
	}

	@Test
	void shouldGeneratePublicKeyFromPrivateKey() {
		var privateKey = "AsFLu5zKG04NDEz52fFZTKDgTSM9bmA5UvY3+VucukA=";
		var publicKey = gen.generatePublicKey(privateKey);
		assertEquals("jiJFzeS5XupLo5KUGdbXqQMgTlTDbAJVssWQFi2zIUI=", publicKey);
	}

	@Test
	void shouldGenerateDifferentKeys() {
		int n = 1000;
		var privateKeys = new HashSet<String>();
		var publicKeys = new HashSet<String>();
		for (int i = 0; i < n; i++) {
			var privateKey = gen.generatePrivateKey();
			var publicKey = gen.generatePublicKey(privateKey);
			privateKeys.add(privateKey);
			publicKeys.add(publicKey);
		}
		assertEquals(n, privateKeys.size());
		assertEquals(n, publicKeys.size());
	}

	@ParameterizedTest
	@CsvFileSource(
			resources = "/com/autonomouslogic/everef/crypto/rfc8032-test-vectors.csv",
			useHeadersInDisplayName = true)
	@SneakyThrows
	void shouldGenerateRfc8032TestVectorPublicKeys(
			String name, String secretKey, String publicKey, String message, String context, String signature) {
		var b64 = gen.generatePublicKey(Base64.encodeBase64String(Hex.decodeHex(secretKey)));
		assertEquals(publicKey, Hex.encodeHexString(Base64.decodeBase64(b64)));
	}
}
