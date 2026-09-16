package com.autonomouslogic.everef.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
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
 * - IOException "Canceled" (OkHttp call timeout; cause chain not yet populated at interceptor time)
 *
 * Does NOT retry:
 * - Thread interruptions (preserves interrupt status for graceful shutdown)
 * - Intentional request cancellations
 * - Other IOExceptions
 */
@RequiredArgsConstructor
@Singleton
@Log4j2
public class SocketErrorRetryInterceptor implements Interceptor {
	private static final int MAX_RETRIES = 3;

	private final long retryDelayMs;
	private final long timeoutRetryInitialDelayMs;
	private final double timeoutRetryBackoffMultiplier;

	@Inject
	protected SocketErrorRetryInterceptor() {
		this(1000, 5000, 2.0);
	}

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
							e.getClass().getSimpleName(), retryCount, MAX_RETRIES, retryDelayMs));
					sleep(retryDelayMs);
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
		// OkHttp throws IOException("Canceled") when a call timeout fires — the timeout cause is only
		// added to the exception AFTER the interceptor chain exits (in RealCall.timeoutExit), so the
		// cause chain is empty here. We never cancel calls explicitly, so all "Canceled" exceptions
		// are timeout-triggered and safe to retry.
		return "Canceled".equals(e.getMessage());
	}

	private long calculateTimeoutRetryDelay(int retryCount) {
		return (long) (timeoutRetryInitialDelayMs * Math.pow(timeoutRetryBackoffMultiplier, retryCount - 1));
	}
}
