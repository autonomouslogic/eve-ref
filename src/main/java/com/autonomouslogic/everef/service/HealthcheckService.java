package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.config.Configs;
import dagger.Lazy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class HealthcheckService {
	@Inject
	protected Lazy<OkHttpClient> okHttpClient;

	@Inject
	protected ScheduledExecutorService scheduler;

	private final Optional<String> healthcheckUrl = Configs.HEALTH_CHECK_URL.get();
	private final Duration healthcheckInterval = Configs.HEALTH_CHECK_INTERVAL.getRequired();
	private ScheduledFuture<?> scheduledFuture;

	@Inject
	protected HealthcheckService() {}

	public void startPeriodicPing() {
		if (healthcheckUrl.isEmpty()) {
			log.debug("Periodic healthcheck not configured, skipping");
			return;
		}

		log.info("Starting periodic healthcheck to {} every {}", healthcheckUrl.get(), healthcheckInterval);

		scheduledFuture = scheduler.scheduleAtFixedRate(
				() -> {
					try {
						ping(healthcheckUrl);
					} catch (Exception e) {
						log.warn("Periodic healthcheck failed", e);
					}
				},
				healthcheckInterval.toMillis(),
				healthcheckInterval.toMillis(),
				TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (scheduledFuture != null) {
			log.info("Stopping periodic healthcheck");
			scheduledFuture.cancel(true);
		}
	}

	public void ping(@NonNull Optional<String> url) {
		ping(url, Optional.empty());
	}

	public void ping(@NonNull Optional<String> url, @NonNull Optional<String> body) {
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
}
