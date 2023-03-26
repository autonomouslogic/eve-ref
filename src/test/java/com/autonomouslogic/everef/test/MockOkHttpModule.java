package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.esi.EsiLimitExceededInterceptor;
import com.autonomouslogic.everef.esi.EsiRateLimitInterceptor;
import com.autonomouslogic.everef.esi.EsiUserAgentInterceptor;
import com.autonomouslogic.everef.inject.OkHttpModule;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;

@Module
public class MockOkHttpModule {
	@Provides
	@Singleton
	@Named("esi")
	public OkHttpClient esiHttpClient(
			EsiUserAgentInterceptor userAgentInterceptor,
			EsiRateLimitInterceptor rateLimitInterceptor,
			EsiLimitExceededInterceptor limitExceededInterceptor,
			MockInterceptor mockInterceptor) {
		return new OkHttpModule()
				.esiHttpClient(null, userAgentInterceptor, rateLimitInterceptor, limitExceededInterceptor)
				.newBuilder()
				.addInterceptor(mockInterceptor)
				.addInterceptor(chain -> {
					throw new RuntimeException(String.format(
							"Blocking outgoing request to %s", chain.request().url()));
				})
				.build();
	}

	@Provides
	@Singleton
	public MockInterceptor mockInterceptor() {
		return new MockInterceptor(Behavior.UNORDERED);
	}
}
