package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.VirtualThreads;
import dagger.Lazy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class HealthcheckDecorator {
	@Inject
	protected Lazy<OkHttpClient> okHttpClient;

	private final Optional<String> finishUrl;
	private final Optional<String> startUrl;
	private final Optional<String> failUrl;
	private final Optional<String> logUrl;

	@Inject
	protected HealthcheckDecorator() {
		finishUrl = Configs.HEALTH_CHECK_URL.get();
		startUrl = Configs.HEALTH_CHECK_START_URL.get();
		failUrl = Configs.HEALTH_CHECK_FAIL_URL.get();
		logUrl = Configs.HEALTH_CHECK_LOG_URL.get();
	}

	public Command decorate(@NonNull Command command) {
		if (!enabled()) {
			return command;
		}
		okHttpClient.get();
		return new HealthcheckCommand(command);
	}

	private boolean enabled() {
		return finishUrl.isPresent() || startUrl.isPresent() || failUrl.isPresent() || logUrl.isPresent();
	}

	private void ping(@NonNull Optional<String> url) {
		ping(url, Optional.empty());
	}

	private void ping(@NonNull Optional<String> url, @NonNull Optional<String> body) {
		if (url.isEmpty()) {
			return;
		}
		int maxRetries = 2;
		Exception lastException = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				executeCall(url.get(), body);
				return;
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					log.warn(String.format("Healthcheck \"%s\" retrying: %s", url.get(), ExceptionUtils.getMessage(e)));
				}
			}
		}
		log.warn(String.format("Healthcheck \"%s\" failed", url.get()), lastException);
	}

	@SneakyThrows
	private void executeCall(@NotNull String url, @NotNull Optional<String> body) {
		var client = okHttpClient.get();
		var call = client.newCall(new Request.Builder()
				.post(RequestBody.create(body.orElse("").getBytes(StandardCharsets.UTF_8)))
				.url(url)
				.build());
		try (var response = call.execute()) {
			var code = response.code();
			var msg = String.format("Healthcheck \"%s\" response: %d", url, code);
			if (code < 200 || code >= 300) {
				log.warn(msg);
			} else {
				log.debug(msg);
			}
		}
	}

	@RequiredArgsConstructor
	private class HealthcheckCommand implements Command {
		private final Command delegate;

		@Override
		public void run() {
			VirtualThreads.checkThread();
			ping(startUrl);
			try {
				delegate.run();
				ping(finishUrl);
			} catch (Exception e) {
				ping(logUrl, Optional.of(ExceptionUtils.getStackTrace(e)));
				ping(failUrl);
				throw e;
			}
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
