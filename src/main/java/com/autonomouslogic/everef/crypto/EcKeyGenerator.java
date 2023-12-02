package com.autonomouslogic.everef.crypto;

import java.security.SecureRandom;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

/**
 * Generated Ed25519ctx key-pairs.
 */
@Singleton
public class EcKeyGenerator {
	private final Ed25519KeyPairGenerator generator;

	@Inject
	@SneakyThrows
	public EcKeyGenerator() {
		generator = new Ed25519KeyPairGenerator();
		generator.init(new Ed25519KeyGenerationParameters(SecureRandom.getInstanceStrong()));
	}

	/**
	 * Generates a new private key and returns it encoded as base64.
	 * @return
	 */
	public synchronized String generatePrivateKey() {
		var pair = generator.generateKeyPair();
		var key = (Ed25519PrivateKeyParameters) pair.getPrivate();
		return Base64.encodeBase64String(key.getEncoded());
	}

	/**
	 * Generates a new public key from the supplied private key and returns it encoded as base64.
	 * @return
	 */
	public String generatePublicKey(String privateKey) {
		var decoded = Base64.decodeBase64(privateKey);
		var key = new Ed25519PrivateKeyParameters(decoded, 0);
		return Base64.encodeBase64String(key.generatePublicKey().getEncoded());
	}
}
