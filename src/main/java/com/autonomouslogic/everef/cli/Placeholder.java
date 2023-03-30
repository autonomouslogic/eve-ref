package com.autonomouslogic.everef.cli;

import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Placeholder implements Command {
	@Inject
	protected Placeholder() {}

	@Override
	public Completable run() {
		return Completable.fromAction(() -> {
			log.info("Placeholder command");
		});
	}
}
