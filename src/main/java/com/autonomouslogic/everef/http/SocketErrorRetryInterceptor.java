package com.autonomouslogic.everef.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Retries transient network errors that occur during HTTP request/response processing.
 * OkHttp's retryOnConnectionFailure only handles connection establishment errors.
 *
 * Retries with fixed delay (1s):
 * - SocketException (connection errors during transmission)
 * - SocketTimeoutException (read/write timeouts)
 *
 * Retries with exponential backoff (5s, 10s, 20s):
 * - InterruptedIOException containing "timeout" (call timeout exceeded)
 * - IOException "Canceled" caused by timeout
 *
 * Does NOT retry:
 * - Thread interruptions (preserves interrupt status for graceful shutdown)
 * - Intentional request cancellations
 * - Other IOExceptions
 */
@Singleton
@Log4j2
public class SocketErrorRetryInterceptor implements Interceptor {
	private static final int MAX_RETRIES = 3;
	private static final long RETRY_DELAY_MS = 1000;
	private static final long TIMEOUT_RETRY_INITIAL_DELAY_MS = 5000;
	private static final double TIMEOUT_RETRY_BACKOFF_MULTIPLIER = 2.0;

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
			} catch (InterruptedIOException e) {
				// Check for explicit thread interruption (should NOT retry)
				if (Thread.currentThread().isInterrupted()) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Thread interrupted during HTTP call", e);
				}

				// Check if this is a timeout (retryable)
				if (isTimeout(e)) {
					lastException = e;
					retryCount++;
					if (retryCount < MAX_RETRIES) {
						long delay = calculateTimeoutRetryDelay(retryCount);
						log.warn(String.format(
								"Call timeout (%s), retrying (attempt %d/%d) after %dms: %s",
								e.getMessage(),
								retryCount,
								MAX_RETRIES,
								delay,
								chain.request().url()));
						sleep(delay);
					} else {
						log.error(String.format(
								"Call timeout exhausted all retries (%d attempts): %s",
								MAX_RETRIES, chain.request().url()));
					}
				} else {
					// Unknown InterruptedIOException - don't retry
					throw e;
				}
			} catch (IOException e) {
				// Only handle timeout-triggered cancellations
				if (isCanceledFromTimeout(e)) {
					lastException = e;
					retryCount++;
					if (retryCount < MAX_RETRIES) {
						long delay = calculateTimeoutRetryDelay(retryCount);
						log.warn(String.format(
								"Call canceled due to timeout, retrying (attempt %d/%d) after %dms: %s",
								retryCount, MAX_RETRIES, delay, chain.request().url()));
						sleep(delay);
					} else {
						log.error(String.format(
								"Timeout-canceled call exhausted all retries (%d attempts): %s",
								MAX_RETRIES, chain.request().url()));
					}
				} else {
					// Other IOExceptions - don't catch, let them propagate
					throw e;
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

	private boolean isTimeout(InterruptedIOException e) {
		return e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout");
	}

	private boolean isCanceledFromTimeout(IOException e) {
		if (e.getMessage() == null || !e.getMessage().equals("Canceled")) {
			return false;
		}

		// Traverse cause chain looking for timeout
		Throwable cause = e.getCause();
		while (cause != null) {
			if (cause instanceof InterruptedIOException) {
				String msg = cause.getMessage();
				if (msg != null && msg.toLowerCase().contains("timeout")) {
					return true;
				}
			}
			cause = cause.getCause();
		}

		return false;
	}

	private long calculateTimeoutRetryDelay(int retryCount) {
		return (long) (TIMEOUT_RETRY_INITIAL_DELAY_MS * Math.pow(TIMEOUT_RETRY_BACKOFF_MULTIPLIER, retryCount - 1));
	}
}
