package com.autonomouslogic.everef.crypto;

import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519ctxSigner;

/**
 * Wraps signature generation and verification using the Ed25519ctx algorithm.
 *
 * <a href="https://datatracker.ietf.org/doc/html/rfc8032">Edwards-Curve Digital Signature Algorithm (EdDSA) - RFC 8032</a>
 * <a href="https://github.com/bcgit/bc-java/blob/main/core/src/test/java/org/bouncycastle/crypto/test/Ed25519Test.java">Ed25519Test.java</a>
 * <a href="https://cryptologie.net/article/497/eddsa-ed25519-ed25519-ietf-ed25519ph-ed25519ctx-hasheddsa-pureeddsa-wtf/">EdDSA, Ed25519, Ed25519-IETF, Ed25519ph, Ed25519ctx, HashEdDSA, PureEdDSA, WTF?</a>
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class EcSigner {
	/**
	 * The global context for signatures.
	 * Recommended by RFC 8032. It's not secret, it doesn't need to be random or secure.
	 * It only needs to be unique to the application.
	 */
	private static final byte[] GLOBAL_CONTEXT = "everef.net".getBytes(StandardCharsets.UTF_8);

	private final byte[] ctx;

	@Inject
	public EcSigner() {
		this(GLOBAL_CONTEXT);
	}

	/**
	 * Sign a message using the given private key.
	 * @param privateKey base64 encoded private key.
	 * @param message message to sign.
	 * @return
	 */
	public String sign(String privateKey, String message) {
		return Base64.encodeBase64String(
				sign(Base64.decodeBase64(privateKey), message.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Sign a message using the given private key.
	 * @param privateKey private key.
	 * @param message message to sign.
	 * @return
	 */
	public byte[] sign(byte[] privateKey, byte[] message) {
		var key = new Ed25519PrivateKeyParameters(privateKey, 0);
		var signer = new Ed25519ctxSigner(ctx);
		signer.init(true, key);
		signer.update(message, 0, message.length);
		return signer.generateSignature();
	}

	/**
	 * Verify a signature using the given public key.
	 * @param publicKey base64 encoded public key.
	 * @param message message to verify.
	 * @param signature base64 encoded signature.
	 * @return
	 */
	public boolean verify(String publicKey, String message, String signature) {
		return verify(
				Base64.decodeBase64(publicKey),
				message.getBytes(StandardCharsets.UTF_8),
				Base64.decodeBase64(signature));
	}

	/**
	 * Verify a signature using the given public key.
	 * @param publicKey public key.
	 * @param message message to verify.
	 * @param signature signature.
	 * @return
	 */
	public boolean verify(byte[] publicKey, byte[] message, byte[] signature) {
		var key = new Ed25519PublicKeyParameters(publicKey, 0);
		var signer = new Ed25519ctxSigner(ctx);
		signer.init(false, key);
		signer.update(message, 0, message.length);
		return signer.verifySignature(signature);
	}
}
