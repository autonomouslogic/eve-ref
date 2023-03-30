package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Completable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class HealthcheckDecorator {
	@Inject
	protected OkHttpClient okHttpClient;

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
		return new HealthCheckCommand(command);
	}

	private boolean enabled() {
		return finishUrl.isPresent() || startUrl.isPresent() || failUrl.isPresent() || logUrl.isPresent();
	}

	private Completable ping(@NonNull Optional<String> url) {
		return ping(url, Optional.empty());
	}

	private Completable ping(@NonNull Optional<String> url, @NonNull Optional<String> body) {
		return Completable.fromAction(() -> {
					if (url.isEmpty()) {
						return;
					}
					var call = okHttpClient.newCall(new Request.Builder()
							.post(RequestBody.create(body.orElse("").getBytes(StandardCharsets.UTF_8)))
							.url(url.get())
							.build());
					try (var response = call.execute()) {
						var code = response.code();
						var msg = String.format("Healthcheck \"%s\" response: %d", url.get(), code);
						if (code < 200 || code >= 300) {
							log.warn(msg);
						} else {
							log.debug(msg);
						}
					}
				})
				.retry(2, e -> {
					log.warn(String.format("Healthcheck \"%s\" retrying: %s", url.get(), ExceptionUtils.getMessage(e)));
					return true;
				})
				.onErrorResumeNext(e -> {
					log.warn(String.format("Healthcheck \"%s\" failed", url.get()), e);
					return Completable.complete();
				})
				.compose(Rx.offloadCompletable());
	}

	@RequiredArgsConstructor
	private class HealthCheckCommand implements Command {
		private final Command delegate;

		public Completable run() {
			return Completable.concatArray(ping(startUrl), delegate.run(), ping(finishUrl))
					.onErrorResumeNext(e -> Completable.concatArray(
									ping(logUrl, Optional.of(ExceptionUtils.getMessage(e))), ping(failUrl))
							.andThen(Completable.error(e)));
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
