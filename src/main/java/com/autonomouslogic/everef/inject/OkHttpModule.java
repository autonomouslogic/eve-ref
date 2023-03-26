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
			EsiLimitExceededInterceptor limitExceededInterceptor) {
		var builder = new OkHttpClient.Builder()
				.addInterceptor(userAgentInterceptor)
				// .addInterceptor(limitExceededInterceptor) // @todo
				.addInterceptor(rateLimitInterceptor) // @todo
				.retryOnConnectionFailure(true)
				.followRedirects(true)
				.followSslRedirects(true)
				.connectTimeout(Duration.ofSeconds(5))
				.readTimeout(Duration.ofSeconds(20))
				.writeTimeout(Duration.ofSeconds(5));
		if (cache != null) {
			builder.cache(cache);
		}
		return builder.build();
	}
}
