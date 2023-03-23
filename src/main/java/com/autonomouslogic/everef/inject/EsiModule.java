package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiUserAgentInterceptor;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.infrastructure.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

@Module
public class EsiModule {
	@Provides
	@Singleton
	@Named("esi")
	public OkHttpClient apiClient(EsiUserAgentInterceptor userAgentInterceptor) {
		return ApiClient.getBuilder()
			.addInterceptor(userAgentInterceptor)
			.build();
	}

	@Provides
	@Singleton
	public UniverseApi universeApi(@Named("esi") OkHttpClient httpClient) {
		return new UniverseApi(null, httpClient);
	}
}
