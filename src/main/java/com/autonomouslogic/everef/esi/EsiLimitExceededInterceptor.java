package com.autonomouslogic.everef.esi;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Handles global stop if 420 responses are received.
 */
@Singleton
@Log4j2
public class EsiLimitExceededInterceptor implements Interceptor {
	private static final String ESI_420_TEXT = "This software has exceeded the error limit for ESI.";
	private static final String RESET_TIME_HEADER = "X-Esi-Error-Limit-Reset";

	private static final AtomicBoolean globalStop = new AtomicBoolean();

	@Inject
	protected EsiLimitExceededInterceptor() {}

	@NotNull
	@Override
	@SneakyThrows
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var success = false;
		Response response;
		do {
			respectGlobalStop();
			response = chain.proceed(chain.request());
			if (response.code() == 420 || response.body().string().contains(ESI_420_TEXT)) {
				// @todo there's a race condition here on concurrent requests
				globalStop.set(true);
				var resetTime = parseResetTime(
						Optional.ofNullable(response.header(RESET_TIME_HEADER)).orElse("10"));
				log.warn(String.format("ESI 420, waiting for %s", resetTime));
				Thread.sleep(resetTime.plusSeconds(1).toMillis());
				globalStop.set(false);
			}
		} while (!success);
		return response;
	}

	@SneakyThrows
	private void respectGlobalStop() {
		var start = Instant.now();
		while (globalStop.get()) {
			log.debug(String.format("Waiting for ESI 420: %s", Duration.between(start, Instant.now())));
			Thread.sleep(1000);
		}
	}

	private Duration parseResetTime(@NonNull String resetTime) {
		return Duration.ofSeconds(Long.parseLong(resetTime));
	}
}
