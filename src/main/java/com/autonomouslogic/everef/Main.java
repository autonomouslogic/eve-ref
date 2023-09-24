package com.autonomouslogic.everef;

import com.autonomouslogic.everef.cli.CommandRunner;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.inject.MainComponent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
	@Inject
	protected CommandRunner commandRunner;

	@Inject
	protected MeterRegistry meterRegistry;

	@Inject
	protected Main() {}

	public void start(String[] args) {
		try {
			meterRegistry
					.config()
					.commonTags(List.of(
							Tag.of("version", Configs.EVE_REF_VERSION.getRequired()),
							Tag.of("application", "eve-ref")));
			commandRunner.runCommand(args).blockingAwait();
		} finally {
			log.debug("Closing metrics");
			meterRegistry.close();
		}
	}

	public static void main(String[] args) {
		log.info(String.format("EVE Ref version %s", Configs.EVE_REF_VERSION.getRequired()));
		RxJavaPlugins.setErrorHandler(e -> {
			log.fatal("RxJava error", e);
			System.exit(1);
		});
		try {
			MainComponent.create().createMain().start(args);
		} catch (Throwable e) {
			log.fatal("Root error, exiting", e);
			System.exit(1);
		}
		System.exit(0);
	}
}
