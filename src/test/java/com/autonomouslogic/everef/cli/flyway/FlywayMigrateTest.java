package com.autonomouslogic.everef.cli.flyway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.db.DbAccess;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

// @todo move to dbIntegrationTest configuration
@ExtendWith(MockitoExtension.class)
@Log4j2
public class FlywayMigrateTest {
	@Inject
	FlywayMigrate flywayMigrate;

	@Inject
	DbAccess dbAccess;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldMigrate() {
		flywayMigrate.run().blockingAwait();

		dbAccess.flyway().validate();
		assertEquals("1", dbAccess.flyway().info().current().getVersion().getVersion());
	}
}
