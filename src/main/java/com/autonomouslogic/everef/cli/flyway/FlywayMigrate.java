package com.autonomouslogic.everef.cli.flyway;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.DbAccess;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class FlywayMigrate implements Command {
	@Inject
	protected DbAccess dbAccess;

	@Inject
	protected FlywayMigrate() {}

	@Override
	@SneakyThrows
	public void run() {
		VirtualThreads.checkIsVirtual();
		log.info("Migrating database");
		VirtualThreads.onVirtualThread(() -> dbAccess.flyway().migrate());
	}

	public Completable autoRun() {
		return Completable.defer(() -> {
			if (Configs.FLYWAY_AUTO_MIGRATE.getRequired()) {
				return Completable.fromAction(this::run);
			}
			return Completable.complete();
		});
	}
}
