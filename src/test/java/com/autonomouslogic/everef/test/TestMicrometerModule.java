package com.autonomouslogic.everef.test;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Module
@Log4j2
public class TestMicrometerModule {
	@Provides
	@Singleton
	public MeterRegistry meterRegistry() {
		return new SimpleMeterRegistry();
	}
}
