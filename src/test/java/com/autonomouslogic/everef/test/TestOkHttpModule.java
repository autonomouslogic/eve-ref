package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.http.EsiLimitExceededInterceptor;
import com.autonomouslogic.everef.http.EsiMarketHistoryRateLimitExceededInterceptor;
import com.autonomouslogic.everef.http.EsiRateLimitInterceptor;
import com.autonomouslogic.everef.http.EsiUserAgentInterceptor;
import com.autonomouslogic.everef.http.LoggingInterceptor;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.http.UserAgentInterceptor;
import com.autonomouslogic.everef.inject.OkHttpModule;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

@Module
public class TestOkHttpModule {
	@Provides
	@Singleton
	@Named("esi")
	public OkHttpClient esiHttpClient(
			EsiUserAgentInterceptor userAgentInterceptor,
			EsiRateLimitInterceptor rateLimitInterceptor,
			EsiLimitExceededInterceptor limitExceededInterceptor,
			LoggingInterceptor loggingInterceptor) {
		return new OkHttpModule()
				.esiHttpClient(
						null, userAgentInterceptor, rateLimitInterceptor, limitExceededInterceptor, loggingInterceptor)
				.newBuilder()
				.addInterceptor(new NonLocalhostBlockingInterceptor())
				.build();
	}

	@Provides
	@Singleton
	@Named("esi-market-history")
	public OkHttpClient esiMarketHistoryHttpClient(
			@Named("esi") OkHttpClient esiClient,
			EsiMarketHistoryRateLimitExceededInterceptor esiMarketHistoryRateLimitExceededInterceptor) {
		return new OkHttpModule()
				.esiMarketHistoryHttpClient(esiClient, esiMarketHistoryRateLimitExceededInterceptor)
				.newBuilder()
				.addNetworkInterceptor(esiMarketHistoryRateLimitExceededInterceptor)
				.build();
	}

	@Provides
	@Singleton
	public OkHttpClient okHttpClient(UserAgentInterceptor userAgentInterceptor, LoggingInterceptor loggingInterceptor) {
		return new OkHttpModule()
				.mainHttpClient(null, userAgentInterceptor, loggingInterceptor)
				.newBuilder()
				.addInterceptor(new NonLocalhostBlockingInterceptor())
				.build();
	}

	private class NonLocalhostBlockingInterceptor implements Interceptor {
		@Override
		public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
			var server = chain.request().url().host();
			if (!server.equals("localhost")) {
				throw new RuntimeException(String.format(
						"Blocking outgoing request to %s", chain.request().url()));
			}
			return chain.proceed(chain.request());
		}
	}

	@Provides
	@Singleton
	public OkHttpWrapper mainWrapper(OkHttpClient client) {
		return new OkHttpModule().mainWrapper(client);
	}

	@Provides
	@Singleton
	@Named("esi")
	public OkHttpWrapper esiMainWrapper(@Named("esi") OkHttpClient client) {
		return new OkHttpModule().esiMainWrapper(client);
	}

	@Provides
	@Singleton
	@Named("esi-market-history")
	public OkHttpWrapper esiMarketHistoryWrapper(@Named("esi-market-history") OkHttpClient client) {
		return new OkHttpModule().esiMarketHistoryWrapper(client);
	}
}
