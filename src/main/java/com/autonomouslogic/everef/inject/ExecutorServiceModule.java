package com.autonomouslogic.everef.inject;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Singleton;

@Module
public class ExecutorServiceModule {
	@Provides
	@Singleton
	public ScheduledExecutorService scheduledExecutorService() {
		return Executors.newScheduledThreadPool(
				4, new ThreadFactoryBuilder().setNameFormat("scheduler-%d").build());
	}
}
