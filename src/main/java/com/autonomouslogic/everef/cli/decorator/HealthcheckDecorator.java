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
	@Inject
	protected HealthcheckDecorator() {}

	public Command decorate(@NonNull Command command) {
		return new HealthCheckCommand(command);
	}

	private void signalCheck(@NonNull Optional<String> url) {
		signalCheck(url, Optional.empty());
	}

	private void signalCheck(@NonNull Optional<String> url, @NonNull Optional<String> body) {
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
				var startUrl = Configs.HEALTH_CHECK_START_URL.get();
				signalCheck(startUrl);
				return delegate.run()
						.doOnError(e -> {
							var logUrl = Configs.HEALTH_CHECK_LOG_URL.get();
							var failUrl = Configs.HEALTH_CHECK_FAIL_URL.get();
							signalCheck(logUrl, Optional.of(e.getMessage()));
							signalCheck(failUrl);
						})
						.andThen(Completable.fromAction(() -> {
							var endUrl = Configs.HEALTH_CHECK_URL.get();
							signalCheck(endUrl);
						}));
			});
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
