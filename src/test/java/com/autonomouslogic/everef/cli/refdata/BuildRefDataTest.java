package com.autonomouslogic.everef.cli.refdata;

import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BuildRefDataTest {
	@Inject
	protected BuildRefData buildRefData;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	void shouldBuildRefData() {
		fail("todo");
		buildRefData.run().blockingAwait();
	}
}
