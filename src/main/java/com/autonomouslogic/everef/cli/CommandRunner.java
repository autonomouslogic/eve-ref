package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.cli.decorator.HealthcheckDecorator;
import io.reactivex.rxjava3.core.Completable;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class CommandRunner {
	@Inject
	protected Provider<PlaceholderCli> placeholderCliProvider;

	@Inject
	protected HealthcheckDecorator healthcheckDecorator;

	@Inject
	protected CommandRunner() {}

	public Completable runCommand(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("No command specified");
		}
		if (args.length > 1) {
			throw new IllegalArgumentException("More than one command specified");
		}
		var command = createCommand(args[0]);
		return runCommand(command);
	}

	private Completable runCommand(Command command) {
		return Completable.defer(() -> {
			var name = command.getName();
			log.info(String.format("Executing command: %s", name));
			var start = Instant.now();
			return command.run().andThen(Completable.fromAction(() -> {
				var time = Duration.between(start, Instant.now());
				log.info(String.format("Command %s completed in %s", name, time));
			}));
		});
	}

	private Command createCommand(String name) {
		switch (name) {
			case "placeholder":
				return placeholderCliProvider.get();
			default:
				throw new IllegalArgumentException("Unknown command: " + name);
		}
	}

	private Command decorateCommand(Command command) {
		command = healthcheckDecorator.decorate(command);
		return command;
	}
}
