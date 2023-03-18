package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import io.reactivex.rxjava3.core.Completable;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class HealthcheckDecorator {
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

	private void ping(@NonNull Optional<String> url) {
		ping(url, Optional.empty());
	}

	private void ping(@NonNull Optional<String> url, @NonNull Optional<String> body) {
		if (url.isPresent()) {
			if (body.isEmpty()) {
				log.info("POST " + url.get());
			} else {
				log.info("POST " + url.get() + " - with body: " + body.get());
			}
		}
	}

	@RequiredArgsConstructor
	private class HealthCheckCommand implements Command {
		private final Command delegate;

		public Completable run() {
			return Completable.defer(() -> {
				ping(startUrl);
				return delegate.run()
						.doOnError(e -> {
							try {
								ping(logUrl, Optional.of(e.getMessage()));
								ping(failUrl);
							} finally {
								throw e;
							}
						})
						.andThen(Completable.fromAction(() -> {
							ping(finishUrl);
						}));
			});
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
