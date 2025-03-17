package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.util.VirtualThreads;
import io.reactivex.rxjava3.core.Completable;

public interface Command extends Runnable {
	@Deprecated
	Completable runAsync();

	default void run() {
		VirtualThreads.checkThread();
		runAsync().blockingAwait();
	}

	default String getName() {
		return this.getClass().getSimpleName();
	}
}
