package com.autonomouslogic.everef.cli;

public interface Command extends Runnable {
	void run();

	default String getName() {
		return this.getClass().getSimpleName();
	}
}
