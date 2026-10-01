package com.autonomouslogic.everef.http;

import com.autonomouslogic.everef.config.Configs;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Pauses ESI requests process-wide when {@code X-Esi-Error-Limit-Remain} drops to or below a configured
 * threshold, until {@code X-Esi-Error-Limit-Reset} has elapsed.
 */
@Singleton
@Log4j2
public class EsiErrorLimitBudgetInterceptor implements Interceptor {
	private static final Duration MAX_WAIT = Duration.ofSeconds(120);

	private final int minRemain;
	private final AtomicReference<Instant> blockedUntil = new AtomicReference<>(Instant.MIN);

	@Inject
	public EsiErrorLimitBudgetInterceptor() {
		this(Configs.ESI_ERROR_LIMIT_MIN_REMAIN.getRequired());
	}

	EsiErrorLimitBudgetInterceptor(int minRemain) {
		if (minRemain < 0 || minRemain > 99) {
			throw new IllegalArgumentException("ESI_ERROR_LIMIT_MIN_REMAIN must be between 0 and 99: " + minRemain);
		}
		this.minRemain = minRemain;
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		awaitBlock();
		var response = chain.proceed(chain.request());
		checkErrorLimit(response);
		return response;
	}

	private void awaitBlock() throws IOException {
		while (true) {
			var until = blockedUntil.get();
			var now = Instant.now();
			if (!now.isBefore(until)) {
				return;
			}
			try {
				Thread.sleep(Duration.between(now, until).toMillis());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("Interrupted while waiting for ESI error limit budget");
			}
		}
	}

	void checkErrorLimit(@NotNull Response response) {
		var networkResponse = response.networkResponse();
		if (networkResponse == null) {
			// Pure cache hit; headers would be stale.
			return;
		}
		var remainHeader = networkResponse.header(EsiErrorLimitHeaders.REMAIN_HEADER);
		var resetHeader = networkResponse.header(EsiErrorLimitHeaders.RESET_TIME_HEADER);
		if (remainHeader == null || resetHeader == null) {
			return;
		}
		var remain = EsiErrorLimitHeaders.parseRemain(remainHeader);
		var reset = EsiErrorLimitHeaders.parseReset(resetHeader);
		if (remain.isEmpty() || reset.isEmpty()) {
			log.debug(String.format(
					"Unparseable ESI error limit headers: %s=%s, %s=%s",
					EsiErrorLimitHeaders.REMAIN_HEADER,
					remainHeader,
					EsiErrorLimitHeaders.RESET_TIME_HEADER,
					resetHeader));
			return;
		}
		if (remain.get() > minRemain) {
			return;
		}
		var wait = capWait(reset.get().plusSeconds(1));
		var newBlockedUntil = Instant.now().plus(wait);
		var previous =
				blockedUntil.getAndUpdate(current -> newBlockedUntil.isAfter(current) ? newBlockedUntil : current);
		if (newBlockedUntil.isAfter(previous)) {
			log.warn(String.format(
					"ESI error limit remain is %s (<= %s), blocking ESI requests until %s",
					remain.get(), minRemain, newBlockedUntil));
		}
	}

	Instant currentBlockedUntil() {
		return blockedUntil.get();
	}

	private Duration capWait(@NotNull Duration wait) {
		return wait.compareTo(MAX_WAIT) > 0 ? MAX_WAIT : wait;
	}
}
