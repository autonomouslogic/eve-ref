package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.crypto.EcKeyGenerator;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Fetches donations to EVE Ref from the ESI.
 */
@Log4j2
public class GenerateKeyPair implements Command {
	@Inject
	protected EcKeyGenerator keyGenerator;

	@Inject
	protected GenerateKeyPair() {}

	public Completable runAsync() {
		return Completable.fromAction(() -> {
			var privateKey = keyGenerator.generatePrivateKey();
			var publicKey = keyGenerator.generatePublicKey(privateKey);
			var publicKeyHash = keyGenerator.createKeyHash(publicKey);
			log.info("Private key: {}", privateKey);
			log.info("Public key: {}", publicKey);
			log.info("Public key hash: {}", publicKeyHash);
		});
	}
}
