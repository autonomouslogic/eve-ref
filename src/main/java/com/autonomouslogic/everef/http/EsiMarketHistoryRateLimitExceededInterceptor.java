package com.autonomouslogic.everef.http;

import com.autonomouslogic.everef.config.Configs;
import java.io.IOException;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Global rate limiter for ESI requests.
 */
@Singleton
@Log4j2
public class EsiMarketHistoryRateLimitExceededInterceptor implements Interceptor {
	private final Duration pause = Configs.ESI_MARKET_HISTORY_RATE_LIMIT_WAIT_TIME.getRequired();
	private final int maxTrues = Configs.ESI_MARKET_HISTORY_RATE_LIMIT_TRIES.getRequired();

	@Inject
	protected EsiMarketHistoryRateLimitExceededInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var response = chain.proceed(chain.request());
		var tries = 0;
		while (response.code() == 500 && tries < maxTrues) {
			tries++;
			var body = response.body().string();
			log.warn("Received 500, waiting {}, try {}/{} - body: {}", pause, tries, maxTrues, body);
			response = chain.proceed(chain.request());
		}
		return response;
	}
}
