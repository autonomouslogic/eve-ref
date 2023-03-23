package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Global rate limiter for ESI requests.
 */
@Singleton
@Log4j2
public class EsiRateLimitInterceptor implements Interceptor {
	private final RateLimiter rateLimiter;
	private Instant lastRateLimitLog = Instant.MIN;
	private final Duration rateLimitLogInterval = Duration.ofSeconds(5);

	@Inject
	protected EsiRateLimitInterceptor() {
		var limit = Configs.ESI_RATE_LIMIT_PER_S.getRequired();
		if (limit < 1) {
			throw new IllegalArgumentException("ESI rate limit must be at least 1");
		}
		rateLimiter = RateLimiter.create(limit);
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		if (!rateLimiter.tryAcquire()) {
			logRateLimit();
			rateLimiter.acquire();
		}
		return chain.proceed(chain.request());
	}

	private void logRateLimit() {
		var now = Instant.now();
		var time = Duration.between(lastRateLimitLog, now);
		if (time.compareTo(rateLimitLogInterval) > 0) {
			lastRateLimitLog = now;
			log.debug("Limiting requests to the ESI");
		}
	}
}
