package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;

/**
 * Utility for retrying ESI API calls with exponential backoff.
 */
@Log4j2
public class EsiRetryUtil {
	private EsiRetryUtil() {}

	/**
	 * Retry a supplier with exponential backoff for transient errors.
	 *
	 * <p>Retries on 5xx and 429 (rate limit) errors. Throws immediately on client errors (4xx except
	 * 429) and network errors.
	 *
	 * @param description description of what's being retried (for logging)
	 * @param supplier the supplier to retry
	 * @param maxRetries maximum number of retries
	 * @param delay delay between retries
	 * @param <T> return type
	 * @return the result of the supplier
	 * @throws ApiException if the supplier throws after max retries
	 */
	public static <T> T fetchWithRetry(String description, Supplier<T> supplier, int maxRetries, Duration delay)
			throws ApiException {
		int attempts = 0;
		while (true) {
			try {
				attempts++;
				return supplier.get();
			} catch (Exception e) {
				if (e instanceof ApiException) {
					var apiException = (ApiException) e;
					if (apiException.getCode() >= 500 || apiException.getCode() == 429) {
						if (attempts >= maxRetries) {
							log.error("Max retries exceeded for {}", description);
							throw apiException;
						}
						log.debug(
								"Retry {}/{} for {} (code: {})",
								attempts,
								maxRetries,
								description,
								apiException.getCode());
						try {
							Thread.sleep(delay.toMillis());
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							throw new RuntimeException(ie);
						}
					} else {
						throw apiException;
					}
				} else {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
