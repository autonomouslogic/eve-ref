package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.openapi.esi.api.IndustryApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiResponse;
import com.autonomouslogic.everef.openapi.esi.model.IndustrySystemsGetInner;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class SystemCostIndexService {
	@Inject
	protected IndustryApi industryApi;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ScheduledExecutorService scheduler;

	private String etag;
	private ScheduledFuture<?> future;
	private Map<Long, SystemCostIndex> cached = new ConcurrentHashMap<>();

	@Inject
	protected SystemCostIndexService() {}

	public void init() {
		update();
		future = scheduler.scheduleAtFixedRate(
				() -> {
					try {
						update();
					} catch (Exception e) {
						log.warn("Failed to update market prices, ignoring", e);
					}
				},
				10,
				10,
				TimeUnit.MINUTES);
	}

	@SneakyThrows
	private void update() {
		log.info("Updating industry systems");
		ApiResponse<List<IndustrySystemsGetInner>> res;
		try {
			res = industryApi.getIndustrySystemsWithHttpInfo(esiHelper.getCompatibilityDate(), null, etag, null);
		} catch (ApiException e) {
			if (e.getCode() == 304) {
				log.debug("No industry systems update needed");
				return;
			} else {
				throw e;
			}
		}
		for (IndustrySystemsGetInner system : res.getData()) {
			cached.put(system.getSolarSystemId(), convert(system));
		}
		etag = res.getHeaders().get("ETag").getFirst();
		log.debug("Finished updating industry systems");
	}

	private SystemCostIndex convert(IndustrySystemsGetInner system) {
		var cost = SystemCostIndex.builder();
		for (var costIndex : system.getCostIndices()) {
			switch (costIndex.getActivity()) {
				case MANUFACTURING -> cost.manufacturing(costIndex.getCostIndex());
				case RESEARCHING_TIME_EFFICIENCY -> cost.researchingTimeEfficiency(costIndex.getCostIndex());
				case RESEARCHING_MATERIAL_EFFICIENCY -> cost.researchingMaterialEfficiency(costIndex.getCostIndex());
				case COPYING -> cost.copying(costIndex.getCostIndex());
				case INVENTION -> cost.invention(costIndex.getCostIndex());
				case REACTION -> cost.reaction(costIndex.getCostIndex());
			}
		}
		return cost.build();
	}

	public SystemCostIndex getSystem(long systemId) {
		return cached.get(systemId);
	}

	public boolean isReady() {
		return !cached.isEmpty();
	}

	public void stop() {
		if (future != null) {
			future.cancel(true);
			future = null;
		}
	}

	@Value
	@Builder
	public static class SystemCostIndex {
		double manufacturing;
		double researchingTimeEfficiency;
		double researchingMaterialEfficiency;
		double copying;
		double invention;
		double reaction;
	}
}
