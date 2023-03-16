package com.autonomouslogic.everef;

import com.autonomouslogic.everef.cli.CommandRunner;
import com.autonomouslogic.everef.inject.MainComponent;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
	@Inject
	protected CommandRunner commandRunner;

	@Inject
	protected Main() {}

	public void start(String[] args) {
		commandRunner.runCommand(args).blockingAwait();
	}

	public static void main(String[] args) {
		try {
			MainComponent.create().createMain().start(args);
		} catch (Throwable e) {
			log.error("Root error, exiting", e);
			System.exit(1);
		}
		System.exit(0);
	}
}
