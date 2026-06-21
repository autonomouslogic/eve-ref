package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.service.HealthcheckService;
import com.autonomouslogic.everef.util.VirtualThreads;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class HealthcheckDecorator {
	@Inject
	protected HealthcheckService healthcheckService;

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
		return new HealthcheckCommand(command);
	}

	private boolean enabled() {
		return finishUrl.isPresent() || startUrl.isPresent() || failUrl.isPresent() || logUrl.isPresent();
	}

	@RequiredArgsConstructor
	private class HealthcheckCommand implements Command {
		private final Command delegate;

		@Override
		public void run() {
			VirtualThreads.checkThread();
			healthcheckService.ping(startUrl);
			try {
				delegate.run();
				healthcheckService.ping(finishUrl);
			} catch (Exception e) {
				healthcheckService.ping(logUrl, Optional.of(ExceptionUtils.getStackTrace(e)));
				healthcheckService.ping(failUrl);
				throw e;
			}
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
