package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.EsiLimitExceededInterceptor;
import com.autonomouslogic.everef.http.EsiMarketHistoryRateLimitExceededInterceptor;
import com.autonomouslogic.everef.http.EsiRateLimitInterceptor;
import com.autonomouslogic.everef.http.EsiUserAgentInterceptor;
import com.autonomouslogic.everef.http.LoggingInterceptor;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.http.UserAgentInterceptor;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.time.Duration;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

@Module
@Log4j2
public class OkHttpModule {
	@Provides
	@Singleton
	@SneakyThrows
	public Cache cache() {
		// @todo add metrics for cache hits/misses
		var cacheDir = new File(Configs.HTTP_CACHE_DIR.getRequired());
		log.debug(String.format("Using cache directory: %s", cacheDir));
		if (!cacheDir.exists()) {
			log.info("Attempting to create cache directory");
			if (!cacheDir.mkdirs()) {
				throw new RuntimeException("Failed creating cache directory: " + cacheDir);
			}
			log.info("Cache directory created");
		}
		if (!cacheDir.isDirectory()) {
			throw new RuntimeException("Cache dir is not a directory: " + cacheDir);
		}
		if (!cacheDir.canWrite()) {
			throw new RuntimeException("Cache dir is not writable: " + cacheDir);
		}
		var maxSize = Configs.HTTP_CACHE_SIZE_MB.getRequired();
		log.debug(String.format("Cache directory max size: %s MiB", maxSize));
		if (maxSize < 1) {
			throw new RuntimeException("HTTP cache size must be at least 1 MiB");
		}
		var cache = new Cache(cacheDir, maxSize * 1024 * 1024);
		log.debug(String.format("Current cache directory size: %.1f MiB", cache.size() / 1024.0 / 1024.0));
		return cache;
	}

	@Provides
	@Singleton
	@Named("esi")
	public OkHttpClient esiHttpClient(
			Cache cache,
			EsiUserAgentInterceptor userAgentInterceptor,
			EsiRateLimitInterceptor rateLimitInterceptor,
			EsiLimitExceededInterceptor limitExceededInterceptor,
			LoggingInterceptor loggingInterceptor) {
		var builder = new OkHttpClient.Builder()
				.addInterceptor(userAgentInterceptor)
				.addInterceptor(limitExceededInterceptor)
				.addInterceptor(loggingInterceptor)
				.addNetworkInterceptor(rateLimitInterceptor);
		builder = configure(builder, cache);
		return builder.build();
	}

	@Provides
	@Singleton
	@Named("esi-market-history")
	public OkHttpClient esiMarketHistoryHttpClient(
			@Named("esi") OkHttpClient esiClient, EsiMarketHistoryRateLimitExceededInterceptor marketRateInterceptor) {
		return esiClient.newBuilder().addInterceptor(marketRateInterceptor).build();
	}

	@Provides
	@Singleton
	public OkHttpClient mainHttpClient(
			Cache cache, UserAgentInterceptor userAgentInterceptor, LoggingInterceptor loggingInterceptor) {
		var builder =
				new OkHttpClient.Builder().addInterceptor(userAgentInterceptor).addInterceptor(loggingInterceptor);
		builder = configure(builder, cache);
		return builder.build();
	}

	private OkHttpClient.Builder configure(OkHttpClient.Builder builder, Cache cache) {
		return builder.retryOnConnectionFailure(true)
				.followRedirects(true)
				.followSslRedirects(true)
				.connectTimeout(Duration.ofSeconds(5))
				.readTimeout(Duration.ofSeconds(60))
				.writeTimeout(Duration.ofSeconds(5))
				.callTimeout(Duration.ofSeconds(120))
				.cache(cache);
	}

	@Provides
	@Singleton
	public OkHttpWrapper mainWrapper(OkHttpClient client) {
		return new OkHttpWrapper(client);
	}

	@Provides
	@Singleton
	@Named("esi")
	public OkHttpWrapper esiMainWrapper(@Named("esi") OkHttpClient client) {
		return new OkHttpWrapper(client);
	}

	@Provides
	@Singleton
	@Named("esi-market-history")
	public OkHttpWrapper esiMarketHistoryWrapper(@Named("esi-market-history") OkHttpClient client) {
		return new OkHttpWrapper(client);
	}
}
