package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.openapi.esi.apis.MarketApi;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;

@Module
public class EsiModule {
	@Provides
	@Singleton
	public UniverseApi universeApi(@Named("esi") OkHttpClient httpClient) {
		return new UniverseApi(Configs.ESI_BASE_URL.getRequired().toString(), httpClient);
	}

	@Provides
	@Singleton
	public MarketApi marketApi(@Named("esi") OkHttpClient httpClient) {
		return new MarketApi(Configs.ESI_BASE_URL.getRequired().toString(), httpClient);
	}
}
