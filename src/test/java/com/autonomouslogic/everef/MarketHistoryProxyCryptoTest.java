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
import org.bouncycastle.crypto.signers.Ed25519ctxSigner;
import org.junit.jupiter.api.Test;

/**
 * This is just some tests to play with the crypto required for a centralised shared market history cache.
 *
 * <a href="https://datatracker.ietf.org/doc/html/rfc8032">Edwards-Curve Digital Signature Algorithm (EdDSA) - RFC 8032</a>
 * <a href="https://github.com/bcgit/bc-java/blob/main/core/src/test/java/org/bouncycastle/crypto/test/Ed25519Test.java">Ed25519Test.java</a>
 * <a href="https://cryptologie.net/article/497/eddsa-ed25519-ed25519-ietf-ed25519ph-ed25519ctx-hasheddsa-pureeddsa-wtf/">EdDSA, Ed25519, Ed25519-IETF, Ed25519ph, Ed25519ctx, HashEdDSA, PureEdDSA, WTF?</a>
 */
@Log4j2
public class MarketHistoryProxyCryptoTest {
	private static final byte[] ctx =
			Base64.decodeBase64("Ea+TJ1sBwlHFwWsPQt/f2hh8/+0jVd6ezLGKJwhuBmCTxEn2nZuHwWWnbVsZkagqtTZueU104u87"
					+ "+xqHYgCOqHBfLNzr4fDnxwacCID8g+1N9kzT3LKwcZ7X7LqaaOjmNnWElcv+yqbkHcanrw1o1/Fv"
					+ "oSMFNBh/sHPhn6WHM8nOG+ZjJqxRS4qk1tLSoCGrjacFTM26Ds11T33Ug0qrs2tx02iTbMn29G8k"
					+ "y5PWxMTMMXeZJ7c9l4i67JHRFLsaervJ0JaM1fmTh3nHNPs/TAjP4bwe2zoc669ZaaJgA7yvki73"
					+ "XVS9hsILlhwxsMHxvzUMZdSYHytHVSMJCqNY");

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
		byte[] msg = new byte[65536];
		ThreadLocalRandom.current().nextBytes(msg);

		// Sign document.
		var signer = new Ed25519ctxSigner(ctx);
		signer.init(true, privateKey);
		signer.update(msg, 0, msg.length);
		var signature = signer.generateSignature();
		log.info("Signature: {}", Base64.encodeBase64String(signature));

		// Verify signature.
		signer.init(false, publicKey);
		signer.update(msg, 0, msg.length);
		assertTrue(signer.verifySignature(signature));
	}
}
