package com.autonomouslogic.everef.cli;

import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PlaceholderCli implements Command {
	@Inject
	protected PlaceholderCli() {}

	@Override
	public Completable run() {
		return Completable.fromAction(() -> {
			System.out.println("Tst ============================");
			log.info("Placeholder command");
		});
	}
}
