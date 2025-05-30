package com.autonomouslogic.everef;

import com.autonomouslogic.everef.cli.CommandRunner;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.inject.MainComponent;
import com.autonomouslogic.everef.util.VirtualThreads;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
	@Inject
	protected CommandRunner commandRunner;

	@Inject
	protected Main() {}

	public void start(String[] args) {
		commandRunner.runCommand(args);
	}

	public static void main(String[] args) {
		log.info(String.format("EVE Ref version %s", Configs.EVE_REF_VERSION.getRequired()));
		initSentry();
		RxJavaPlugins.setErrorHandler(e -> {
			log.fatal("RxJava error", e);
			Sentry.captureException(e, scope -> scope.setLevel(SentryLevel.FATAL));
			System.exit(1);
		});
		try {
			var main = MainComponent.create().createMain();
			VirtualThreads.EXECUTOR.submit(() -> main.start(args)).get();
		} catch (Throwable e) {
			log.fatal("Root error, exiting", e);
			Sentry.captureException(e, scope -> scope.setLevel(SentryLevel.FATAL));
			System.exit(1);
		}
		System.exit(0);
	}

	private static void initSentry() {
		var sentryDsn = Configs.SENTRY_DSN.get();
		if (sentryDsn.isPresent()) {
			log.info("Enabling Sentry");
			Sentry.init(options -> {
				options.setDsn(sentryDsn.get().toString());
				options.setRelease(Configs.EVE_REF_VERSION.getRequired());
			});
		}
	}
}
