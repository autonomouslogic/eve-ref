package com.autonomouslogic.everef;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.CommandRunner;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.inject.MainComponent;
import com.autonomouslogic.everef.util.MemoryStatsLogger;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Log4j2
public class Main {
	@Inject
	protected CommandRunner commandRunner;

	@Inject
	protected Main() {}

	@SneakyThrows
	public void start(String[] args) {
		VirtualThreads.onVirtualThread(() -> {
			commandRunner.runCommand(args);
		});
	}

	public static void main(String[] args) {
		log.info(String.format("EVE Ref version %s", Configs.EVE_REF_VERSION.getRequired()));
		okhttpFineLogging();
		initSentry();
		MemoryStatsLogger.startIfEnabled();
		RxJavaPlugins.setErrorHandler(e -> {
			// Check if this is a network error that slipped through retry logic
			var rootCause = ExceptionUtils.getRootCause(e);
			if (isNetworkError(rootCause)) {
				log.error("Unhandled network error in RxJava stream (should have been retried)", e);
				Sentry.captureException(e, scope -> scope.setLevel(SentryLevel.ERROR));
				// Don't exit - log and continue
				return;
			}

			// For other errors, this is likely a bug - exit
			log.fatal("RxJava error", e);
			Sentry.captureException(e, scope -> scope.setLevel(SentryLevel.FATAL));
			System.exit(1);
		});
		try {
			var main = MainComponent.create().createMain();
			main.start(args);
		} catch (Throwable e) {
			log.fatal("Root error, exiting", e);
			Sentry.captureException(e, scope -> scope.setLevel(SentryLevel.FATAL));
			System.exit(1);
		}
		System.exit(0);
	}

	private static boolean isNetworkError(Throwable e) {
		if (e instanceof java.net.SocketException) return true;
		if (e instanceof java.net.SocketTimeoutException) return true;
		if (e instanceof java.io.IOException) {
			var msg = e.getMessage();
			return msg != null
					&& (msg.contains("Socket") || msg.contains("timeout") || msg.contains("Connection reset"));
		}
		return false;
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

	private static void okhttpFineLogging() {
		if (Configs.OKHTTP_FINE_LOGGING_ENABLED.getRequired()) {
			Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
		}
	}
}
