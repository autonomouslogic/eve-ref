package com.autonomouslogic.everef.db;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Log4j2
public class DbAdapterTest {
	@Inject
	DbAccess dbAccess;

	@Inject
	DbAdapter dbAdapter;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		dbAccess.flyway().clean();
		dbAccess.flyway().migrate();
	}

	@Test
	void shouldInsertAndSelectMarketHistory() {}
}
