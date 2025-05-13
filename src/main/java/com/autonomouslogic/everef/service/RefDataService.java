package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.util.RefDataUtil;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Log4j2
public class RefDataService {
	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected ScheduledExecutorService scheduler;

	@Getter
	private LoadedRefData loadedRefData;

	@Inject
	protected RefDataService(){
	}

	@Inject
	protected void init(){
		update();
		scheduler.scheduleWithFixedDelay(this::update, 10, 10, TimeUnit.MINUTES);
	}

	private void update() {
		try {
			log.debug("Updating reference data");
			loadedRefData = refDataUtil.loadLatestRefData().blockingGet();
		}
		catch (Exception e) {
			log.warn("Failed updating reference data", e);
		}
	}

	public boolean isReady() {
		return loadedRefData != null;
	}
}
