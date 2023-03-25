package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiLimitExceededInterceptor;
import com.autonomouslogic.everef.esi.EsiRateLimitInterceptor;
import com.autonomouslogic.everef.esi.EsiUserAgentInterceptor;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.time.Duration;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

@Module
public class OkHttpModule {
	@Provides
	@Singleton
	public Cache cache() {
		// @todo add metrics for cache hits/misses
		var cacheDir = new File(Configs.HTTP_CACHE_DIR.getRequired());
		if (!cacheDir.exists()) {
			if (!cacheDir.mkdirs()) {
				throw new RuntimeException("Failed creating cache directory: " + cacheDir);
			}
		}
		if (!cacheDir.isDirectory()) {
			throw new RuntimeException("Cache dir is not a directory: " + cacheDir);
		}
		if (!cacheDir.canWrite()) {
			throw new RuntimeException("Cache dir is not writable: " + cacheDir);
		}
		var maxSize = Configs.HTTP_CACHE_SIZE_MB.getRequired();
		if (maxSize < 1) {
			throw new RuntimeException("HTTP cache size must be at least 1 MB");
		}
		return new Cache(cacheDir, maxSize * 1024 * 1024);
	}

	@Provides
	@Singleton
	@Named("esi")
	public OkHttpClient esiHttpClient(
			EsiUserAgentInterceptor userAgentInterceptor,
			EsiRateLimitInterceptor rateLimitInterceptor,
			EsiLimitExceededInterceptor limitExceededInterceptor) {
		return new OkHttpClient.Builder()
				.addInterceptor(userAgentInterceptor)
				// .addInterceptor(limitExceededInterceptor) // @todo
				// .addInterceptor(rateLimitInterceptor) // @todo
				.retryOnConnectionFailure(true)
				.followRedirects(true)
				.followSslRedirects(true)
				.connectTimeout(Duration.ofSeconds(5))
				.readTimeout(Duration.ofSeconds(5))
				.writeTimeout(Duration.ofSeconds(5))
				.build();
	}
}
