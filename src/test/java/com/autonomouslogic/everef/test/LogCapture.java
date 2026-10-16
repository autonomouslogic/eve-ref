package com.autonomouslogic.everef.test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

/**
 * Captures the formatted messages written to a single named log4j logger, for asserting on log output in tests.
 */
public class LogCapture implements AutoCloseable {
	private final String loggerName;
	private final List<String> messages = new CopyOnWriteArrayList<>();
	private final AbstractAppender appender;

	public LogCapture(String loggerName) {
		this.loggerName = loggerName;
		appender = new AbstractAppender("capture-" + loggerName, null, null, true, Property.EMPTY_ARRAY) {
			@Override
			public void append(LogEvent event) {
				messages.add(event.getMessage().getFormattedMessage());
			}
		};
		appender.start();
		var ctx = (LoggerContext) LogManager.getContext(false);
		var config = ctx.getConfiguration();
		config.addAppender(appender);
		var loggerConfig = new LoggerConfig(loggerName, Level.ALL, true);
		loggerConfig.addAppender(appender, null, null);
		config.addLogger(loggerName, loggerConfig);
		ctx.updateLoggers();
	}

	public List<String> messages() {
		return List.copyOf(messages);
	}

	/**
	 * Waits for a message matching the predicate to be logged.
	 */
	@SneakyThrows
	public Optional<String> await(Predicate<String> predicate, Duration timeout) {
		var deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			var match = messages.stream().filter(predicate).findFirst();
			if (match.isPresent()) {
				return match;
			}
			Thread.sleep(20);
		}
		return messages.stream().filter(predicate).findFirst();
	}

	@Override
	public void close() {
		var ctx = (LoggerContext) LogManager.getContext(false);
		ctx.getConfiguration().removeLogger(loggerName);
		ctx.updateLoggers();
		appender.stop();
	}
}
