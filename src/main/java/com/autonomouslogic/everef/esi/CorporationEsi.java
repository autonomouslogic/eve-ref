package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.apis.CorporationApi;
import com.autonomouslogic.everef.openapi.esi.models.GetCorporationsCorporationIdOk;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class CorporationEsi {
	@Inject
	protected CorporationApi corporationApi;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	private final Map<Integer, Optional<GetCorporationsCorporationIdOk>> corporations = new ConcurrentHashMap<>();

	@Inject
	protected CorporationEsi() {}

	public Maybe<GetCorporationsCorporationIdOk> getCorporation(int corporationId) {
		return EsiHelper.getFromCacheOrFetch(
				"corporation", GetCorporationsCorporationIdOk.class, corporations, corporationId, () -> {
					var source = CorporationApi.DatasourceGetCorporationsCorporationId.valueOf(datasource);
					return corporationApi.getCorporationsCorporationId(corporationId, source, null);
				});
	}
}
