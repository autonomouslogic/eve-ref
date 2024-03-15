package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import io.micronaut.runtime.Micronaut;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class BasicLogin implements Command {
	@Inject
	protected BasicLoginFactory basicLoginFactory;

	private final int micronautPort = Configs.MICRONAUT_PORT.getRequired();

	@Inject
	protected BasicLogin() {}

	public Completable run() {
		return Completable.fromAction(() -> {
			//			System.getenv().forEach((k, v) -> log.info(k + " = " + v));
			Configs.EVE_OAUTH_CLIENT_ID.getRequired();
			Configs.EVE_OAUTH_SECRET_KEY.getRequired();

			Micronaut.build(new String[] {})
					.banner(false)
					.classes(BasicLoginController.class)
					.singletons(basicLoginFactory)
					.start();
			while (true) {
				Thread.sleep(1000);
			}
		});
	}
}
