package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.api.MarketApi;
import com.autonomouslogic.everef.util.VirtualThreads;
import io.reactivex.rxjava3.core.Flowable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class MarketEsi {
	@Inject
	protected MarketApi marketApi;

	@Inject
	protected EsiHelper esiHelper;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	@Inject
	protected MarketEsi() {}

	public Flowable<Integer> getActiveMarketOrderTypes(int regionId) {
		return Flowable.defer(() -> {
					var source = EsiConstants.Datasource.valueOf(datasource);
					return esiHelper.fetchPages(page -> VirtualThreads.offload(() ->
							marketApi.getMarketsRegionIdTypesWithHttpInfo(regionId, source.toString(), null, page)));
				})
				.onErrorResumeNext(e -> {
					// Had problems with UndeliverableExceptions being thrown.
					// This is a hacky workaround, as we should definitely error on errors instead of ignoring them.
					log.warn("Failed fetching active market order types", e);
					return Flowable.empty();
				});
	}
}
