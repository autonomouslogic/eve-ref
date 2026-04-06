package com.autonomouslogic.everef.http;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Retries socket errors (SocketTimeoutException, SocketException) that occur during request/response
 * processing. OkHttp's retryOnConnectionFailure only handles connection establishment, not errors
 * during transmission.
 */
@Singleton
@Log4j2
public class SocketErrorRetryInterceptor implements Interceptor {
	private static final int MAX_RETRIES = 3;
	private static final long RETRY_DELAY_MS = 1000;

	@Inject
	protected SocketErrorRetryInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
		var retryCount = 0;
		var lastException = (IOException) null;

		while (retryCount < MAX_RETRIES) {
			try {
				return chain.proceed(chain.request());
			} catch (SocketTimeoutException | SocketException e) {
				lastException = e;
				retryCount++;
				if (retryCount < MAX_RETRIES) {
					log.debug(String.format(
							"%s, retrying (attempt %d/%d) after %dms",
							e.getClass().getSimpleName(), retryCount, MAX_RETRIES, RETRY_DELAY_MS));
					sleep(RETRY_DELAY_MS);
				}
			}
		}

		// All retries exhausted, throw the last exception
		if (lastException != null) {
			throw lastException;
		}

		// Shouldn't reach here, but proceed if no exception was caught
		return chain.proceed(chain.request());
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted during socket error retry delay", e);
		}
	}
}
