package com.autonomouslogic.everef.inject;

import static com.autonomouslogic.everef.config.Configs.REF_DATA_BASE_URL;

import com.autonomouslogic.everef.openapi.esi.api.CharacterApi;
import com.autonomouslogic.everef.openapi.esi.api.CorporationApi;
import com.autonomouslogic.everef.openapi.esi.api.MarketApi;
import com.autonomouslogic.everef.openapi.esi.api.SovereigntyApi;
import com.autonomouslogic.everef.openapi.esi.api.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.api.WalletApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiClient;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class EsiModule {
	@Provides
	@Singleton
	public ApiClient apiClient() {
		var base = REF_DATA_BASE_URL.getRequired();
		var api = new ApiClient();
		api.setScheme(base.getScheme());
		api.setHost(base.getHost());
		api.setPort(base.getPort());
		api.setBasePath(base.getPath());
		return api;
	}

	@Provides
	@Singleton
	public UniverseApi universeApi(ApiClient apiClient) {
		return new UniverseApi(apiClient);
	}

	@Provides
	@Singleton
	public MarketApi marketApi(ApiClient apiClient) {
		return new MarketApi(apiClient);
	}

	@Provides
	@Singleton
	public SovereigntyApi sovereigntyApi(ApiClient apiClient) {
		return new SovereigntyApi(apiClient);
	}

	@Provides
	@Singleton
	public WalletApi walletApi(ApiClient apiClient) {
		return new WalletApi(apiClient);
	}

	@Provides
	@Singleton
	public CharacterApi characterApi(ApiClient apiClient) {
		return new CharacterApi(apiClient);
	}

	@Provides
	@Singleton
	public CorporationApi corporationApi(ApiClient apiClient) {
		return new CorporationApi(apiClient);
	}
}
