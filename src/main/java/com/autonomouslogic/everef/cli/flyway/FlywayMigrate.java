package com.autonomouslogic.everef.cli.flyway;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
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
	public Completable run() {
		return Completable.fromAction(() -> {
					log.info("Migrating database");
					dbAccess.flyway().migrate();
				})
				.compose(Rx.offloadCompletable());
	}

	public Completable autoRun() {
		return Completable.defer(() -> {
			if (Configs.FLYWAY_AUTO_MIGRATE.getRequired()) {
				return run();
			}
			return Completable.complete();
		});
	}
}
