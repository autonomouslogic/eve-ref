package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.apis.AllianceApi;
import com.autonomouslogic.everef.openapi.esi.models.GetAlliancesAllianceIdOk;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class AllianceEsi {
	@Inject
	protected AllianceApi allianceApi;

	private final String datasource = Configs.ESI_DATASOURCE.getRequired();

	private final Map<Integer, Optional<GetAlliancesAllianceIdOk>> alliances = new ConcurrentHashMap<>();

	@Inject
	protected AllianceEsi() {}

	public Maybe<GetAlliancesAllianceIdOk> getAlliance(int allianceId) {
		return EsiHelper.getFromCacheOrFetch("alliance", GetAlliancesAllianceIdOk.class, alliances, allianceId, () -> {
			var source = AllianceApi.DatasourceGetAlliancesAllianceId.valueOf(datasource);
			return allianceApi.getAlliancesAllianceId(allianceId, source, null);
		});
	}
}
