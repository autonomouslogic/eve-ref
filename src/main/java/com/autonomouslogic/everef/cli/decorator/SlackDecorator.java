package com.autonomouslogic.everef.cli.decorator;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackAttachment;
import net.gpedro.integrations.slack.SlackMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class SlackDecorator {
	private Optional<String> url;
	private boolean reportSuccess;
	private boolean reportFailure;
	private boolean reportStacktrace;
	private String channel;
	private String username;

	@Inject
	protected SlackDecorator() {
		url = Configs.SLACK_WEBHOOK_URL.get();
		if (url.isPresent()) {
			channel = Configs.SLACK_WEBHOOK_CHANNEL.getRequired();
			username = Configs.SLACK_WEBHOOK_USERNAME.getRequired();
			reportSuccess = Configs.SLACK_REPORT_SUCCESS.getRequired();
			reportFailure = Configs.SLACK_REPORT_FAILURE.getRequired();
			reportStacktrace = Configs.SLACK_REPORT_FULL_STACKTRACE.getRequired();
		}
	}

	public Command decorate(@NonNull Command command) {
		if (url.isEmpty()) {
			return command;
		}
		return new SlackCommand(command);
	}

	private void report(@NonNull SlackMessage message) {
		if (url.isEmpty()) {
			log.trace("Slack disabled, not reporting");
			return;
		}
		log.trace("Sending Slack message");
		int maxRetries = 2;
		Exception lastException = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				VirtualThreads.onVirtualThread(() -> new SlackApi(url.get()).call(message));
				return;
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					log.warn(String.format("Slack \"%s\" retrying: %s", url.get(), ExceptionUtils.getMessage(e)));
				}
			}
		}
		log.warn(String.format("Slack \"%s\" failed", url.get()), lastException);
	}

	private void reportSuccess(@NonNull String commandName, @NonNull Instant start) {
		if (!reportSuccess) {
			return;
		}
		report(successMessage(String.format(
				"%s completed in %s",
				commandName, Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS))));
	}

	private void reportFailure(@NonNull String commandName, @NonNull Instant start, Throwable error) {
		if (!reportFailure) {
			return;
		}
		report(errorMessage(
				String.format(
						"%s failed after %s",
						commandName, Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.MILLIS)),
				error));
	}

	private SlackMessage createMessage() {
		return new SlackMessage().setUsername(username).setChannel(channel);
	}

	private SlackMessage successMessage(@NonNull String message) {
		SlackMessage msg = createMessage();
		msg.setText(":large_green_circle: " + message);
		return msg;
	}

	private SlackMessage errorMessage(@NonNull String message, Throwable e) {
		var msg = createMessage();
		msg.setText(":large_red_square: " + message);
		var errorAttachment = new SlackAttachment()
				.setColor("danger")
				.setText(ExceptionUtils.getRootCauseMessage(e))
				.setFallback("");
		if (reportStacktrace) {
			errorAttachment.setText(ExceptionUtils.getStackTrace(e));
		}
		msg.addAttachments(errorAttachment);
		return msg;
	}

	@RequiredArgsConstructor
	private class SlackCommand implements Command {
		private final Command delegate;

		@Override
		public void run() {
			VirtualThreads.checkIsVirtual();
			var start = Instant.now();
			try {
				delegate.run();
				reportSuccess(delegate.getName(), start);
			} catch (Throwable e) {
				reportFailure(delegate.getName(), start, e);
				throw e;
			}
		}

		public String getName() {
			return delegate.getName();
		}
	}
}
