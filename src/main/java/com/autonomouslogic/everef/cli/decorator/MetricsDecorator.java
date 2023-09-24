package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.MetricNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Completable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class MetricsDecorator {
	@Inject
	protected MeterRegistry meterRegistry;

	@Inject
	protected MetricsDecorator() {}

	public Command decorate(@NonNull Command command) {
		return new MetricsCommand(command);
	}

	public Command postDecorate(@NonNull Command command) {
		return new MetricsPostCommand(command);
	}

	@RequiredArgsConstructor
	private class MetricsCommand implements Command {
		private final Command delegate;

		public Completable run() {
			return Completable.defer(() -> {
				var start = Instant.now();
				return Completable.concatArray(delegate.run(), Completable.fromAction(() -> {
					meterRegistry.timer(MetricNames.COMMAND_RUNTIME).record(Duration.between(start, Instant.now()));
				}));
			});
		}

		public String getName() {
			return delegate.getName();
		}
	}

	@RequiredArgsConstructor
	private class MetricsPostCommand implements Command {
		private final Command delegate;

		public Completable run() {
			var delay = Configs.PROMETHEUS_SCRAPE_DELAY.getRequired();
			if (delay.isNegative()) {
				return Completable.error(new IllegalArgumentException("Prometheus scrape delay must be positive"));
			}
			if (delay.isZero()) {
				return delegate.run();
			}
			return Completable.concatArray(delegate.run(), Completable.defer(() -> {
				log.debug("Pausing {} to allow Prometheus to scrape metrics", delay);
				return Completable.timer(delay.toMillis(), TimeUnit.MILLISECONDS);
			}));
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
