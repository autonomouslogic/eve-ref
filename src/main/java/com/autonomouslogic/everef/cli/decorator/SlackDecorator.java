package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import io.reactivex.rxjava3.core.Completable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class SlackDecorator {
	private Optional<String> url;
	private String channel;
	private String username;

	@Inject
	protected SlackDecorator() {
		url = Configs.SLACK_WEBHOOK_URL.get();
		if (url.isEmpty()) {
			channel = Configs.SLACK_WEBHOOK_CHANNEL.getRequired();
			username = Configs.SLACK_WEBHOOK_USERNAME.getRequired();
		}
	}

	public Command decorate(@NonNull Command command) {
		if (url.isEmpty()) {
			return command;
		}
		return new SlackCommand(command);
	}

	private void reportSuccess(@NonNull String commandName, Duration runtime) {
		log.info("SLACK SUCCESS");
	}

	private void reportFailure(@NonNull String commandName, Duration runtime, Throwable error) {
		log.info("SLACK FAILURE");
	}

	@RequiredArgsConstructor
	private class SlackCommand implements Command {
		private final Command delegate;

		public Completable run() {
			return Completable.defer(() -> {
				var start = Instant.now();
				return delegate.run()
						.doOnError(e -> {
							reportFailure(delegate.getName(), Duration.between(start, Instant.now()), e);
						})
						.andThen(Completable.fromAction(() -> {
							reportSuccess(delegate.getName(), Duration.between(start, Instant.now()));
						}));
			});
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
