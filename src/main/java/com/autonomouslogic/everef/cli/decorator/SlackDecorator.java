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
import net.gpedro.integrations.slack.SlackAttachment;
import net.gpedro.integrations.slack.SlackMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class SlackDecorator {
	private Optional<String> url;
	private String channel;
	private String username;

	@Inject
	protected SlackDecorator() {
		url = Configs.SLACK_WEBHOOK_URL.get();
		if (url.isPresent()) {
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

	private Completable reportSuccess(@NonNull String commandName, Duration runtime) {
		log.info("SLACK SUCCESS");
	}

	private Completable reportFailure(@NonNull String commandName, Duration runtime, Throwable error) {
		log.info("SLACK FAILURE");
	}

	private SlackMessage createMessage() {
		return new SlackMessage()
			.setUsername(username)
			.setChannel(channel);
	}

	private SlackMessage successMessage(String text, String title, String message) {
		SlackMessage msg = createMessage();
		if (message == null) message = "";
		msg.setText(text);
		msg.addAttachments(new SlackAttachment()
			.setColor("good")
			.setTitle(title)
			.setText(message)
			.setFallback(message)
		);
		return msg;
	}

	private SlackMessage errorMessage(String text, String title, String message, Throwable e) {
		SlackMessage msg = createMessage();
		if (message == null) message = "";
		if (e != null) {
			message += "\n" + ExceptionUtils.getMessage(e);
		}
		msg.setText(text);
		msg.addAttachments(new SlackAttachment()
			.setColor("danger")
			.setTitle(title)
			.setText(message)
			.setFallback(message)
		);
		return msg;
	}

	@RequiredArgsConstructor
	private class SlackCommand implements Command {
		private final Command delegate;

		public Completable run() {
			return Completable.defer(() -> {
				var start = Instant.now();
				return delegate.run()
						.doOnError(e -> {
							try {
								reportFailure(delegate.getName(), Duration.between(start, Instant.now()), e);
							} finally {
								throw e;
							}
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
