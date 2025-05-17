package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.util.RefDataUtil;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class RefDataService {
	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ScheduledExecutorService scheduler;

	@Getter
	private LoadedRefData loadedRefData;

	private ScheduledFuture<?> scheduledFuture;

	@Inject
	protected RefDataService() {}

	public void init() {
		update();
		scheduledFuture = scheduler.scheduleWithFixedDelay(
				() -> {
					try {
						update();
					} catch (Exception e) {
						log.warn("Failed updating reference data, ignoring", e);
					}
				},
				0,
				10,
				TimeUnit.MINUTES);
	}

	private void update() {
		log.debug("Updating reference data");
		var oldData = loadedRefData;
		loadedRefData = refDataUtil.loadLatestRefData().blockingGet();
		log.debug("Reference data updated");
		if (oldData != null) {
			oldData.close();
		}
	}

	public boolean isReady() {
		return loadedRefData != null;
	}

	public void stop() {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}
		if (loadedRefData != null) {
			loadedRefData.close();
			loadedRefData = null;
		}
	}
}
