package com.autonomouslogic.everef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.junit.jupiter.api.Test;

/**
 * This is just some tests to play with the crypto required for a centralised shared market history cache.
 *
 * <a href="https://github.com/bcgit/bc-java/blob/main/core/src/test/java/org/bouncycastle/crypto/test/Ed25519Test.java">Ed25519Test.java</a>
 */
@Log4j2
public class MarketHistoryProxyCryptoTest {
	@Test
	@SneakyThrows
	public void test() {
		// Generate key.
		var gen = new Ed25519KeyPairGenerator();
		gen.init(new Ed25519KeyGenerationParameters(SecureRandom.getInstanceStrong()));
		var keyPair = gen.generateKeyPair();
		var privateKey = (Ed25519PrivateKeyParameters) keyPair.getPrivate();
		var publicKey = (Ed25519PublicKeyParameters) keyPair.getPublic();

		log.info("Private key: {}", Base64.encodeBase64String(privateKey.getEncoded()));
		log.info("Public key: {}", Base64.encodeBase64String(publicKey.getEncoded()));

		// Parse encoded keys.
		var rePrivateKey = new Ed25519PrivateKeyParameters(privateKey.getEncoded(), 0);
		assertEquals(
				Base64.encodeBase64String(privateKey.getEncoded()),
				Base64.encodeBase64String(rePrivateKey.getEncoded()));

		var rePublicKey = new Ed25519PublicKeyParameters(publicKey.getEncoded(), 0);
		assertEquals(
				Base64.encodeBase64String(publicKey.getEncoded()), Base64.encodeBase64String(rePublicKey.getEncoded()));

		// Create message
		byte[] plaintext = new byte[256];
		//		byte[] plaintext = new byte[65536];
		ThreadLocalRandom.current().nextBytes(plaintext);

		// Sign document.
		var signer = new Ed25519Signer();
		//		var signer = new Ed25519ctxSigner();
		//		var signer = new Ed25519phSigner();
		signer.init(true, privateKey);
		var signature = signer.generateSignature();
		log.info("Signature: {}", Base64.encodeBase64String(signature));

		// Verify signature.
		signer.init(false, publicKey);
		assertTrue(signer.verifySignature(signature));
	}
}
