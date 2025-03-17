package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import io.reactivex.rxjava3.core.Completable;
import io.sentry.Sentry;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class SentryDecorator {
	@Inject
	public SentryDecorator() {}

	public Command decorate(@NonNull Command command) {
		if (!Sentry.isEnabled()) {
			return command;
		}
		return new SentryCommand(command);
	}

	@RequiredArgsConstructor
	private class SentryCommand implements Command {
		private final Command delegate;

		public Completable runAsync() {
			return delegate.runAsync().onErrorResumeNext(e -> {
				captureException(e);
				return Completable.error(e);
			});
		}

		public String getName() {
			return delegate.getName();
		}
	}

	public static void captureException(Throwable e) {
		if (Sentry.isEnabled()) {
			Sentry.captureException(e);
		}
	}
}
