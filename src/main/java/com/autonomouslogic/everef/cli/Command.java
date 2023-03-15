package com.autonomouslogic.everef.cli;

import io.reactivex.rxjava3.core.Completable;

public interface Command {
	Completable run();
}
